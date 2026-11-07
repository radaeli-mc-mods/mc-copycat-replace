"""Run exported local test launches; no Gradle processes remain alive during the test.

Usage: python scripts/run_multiplayer_validation.py forge1201/build/multiplayer-copycats
Requires :<loader>:exportValidationLaunches first. Results and process logs stay in build/.
"""
import json
import os
import re
import shlex
from pathlib import Path
import subprocess
import sys
import time


def java_arg(value):
    return '"' + str(value).replace('\\', '\\\\').replace('"', '\\"') + '"'


def expand_vm_files(command, cwd):
    # Java does not recursively expand @files inside another argument file.
    result = []
    for arg in command:
        if arg.startswith('@') and arg.endswith(('VmArgs.txt', 'win_args.txt', 'unix_args.txt')):
            source = Path(arg[1:])
            if not source.is_absolute():
                source = Path(cwd) / source
            result.extend(shlex.split(source.read_text(), comments=True, posix=True))
        else:
            result.append(arg)
    return result


def main():
    directory = Path(sys.argv[1]).resolve()
    launches = json.loads((directory / 'launches.json').read_text())
    server_launch = launches['runServerMulti']
    default_config = Path(server_launch['cwd']) / 'world/serverconfig/copycat_replace-server.toml'
    config = Path(server_launch.get('serverConfig', default_config))
    config.parent.mkdir(parents=True, exist_ok=True)
    config.write_text('max_connected_blocks = 3\n')
    for name in ('phase', 'actor.state', 'observer.state', 'result.txt'):
        (directory / name).write_text('')
    processes = []
    logs = []
    try:
        for name in ('runServerMulti', 'runClientActor', 'runClientObserver'):
            launch = launches[name]
            Path(launch['cwd']).mkdir(parents=True, exist_ok=True)
            command = expand_vm_files(launch['command'], launch['cwd'])
            args = directory / (name + '.args')
            args.write_text('\n'.join(java_arg(arg) for arg in command[1:]), encoding='utf-8')
            log = (directory / (name + '.log')).open('w', encoding='utf-8')
            logs.append(log)
            env = os.environ.copy()
            env.update({key: str(value) for key, value in launch['environment'].items()})
            proc = subprocess.Popen([command[0], '@' + str(args)], cwd=launch['cwd'], env=env,
                                    stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT,
                                    creationflags=subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0)
            processes.append(proc)
            print(f'Started {name}, pid={proc.pid}', flush=True)
            if name == 'runServerMulti':
                deadline = time.monotonic() + 180
                while time.monotonic() < deadline:
                    if proc.poll() is not None:
                        raise RuntimeError('Server exited before accepting clients; inspect runServerMulti.log')
                    path = directory / (name + '.log')
                    if path.exists() and 'For help, type "help"' in path.read_text(errors='replace'):
                        break
                    time.sleep(1)
                else:
                    raise TimeoutError('Server startup timed out')
        deadline = time.monotonic() + 600
        previous = ''
        while time.monotonic() < deadline:
            result = (directory / 'result.txt').read_text()
            phase = (directory / 'phase').read_text()
            if phase != previous:
                print('Phase: ' + phase, flush=True)
                previous = phase
                if phase == 'reload':
                    config.write_text(re.sub(r'max_connected_blocks\s*=\s*\d+', 'max_connected_blocks = 1', config.read_text()))
            if result:
                print(result, flush=True)
                return 0 if result.startswith('PASS') else 1
            if any(proc.poll() is not None for proc in processes):
                raise RuntimeError('A process exited early; inspect process logs')
            time.sleep(1)
        raise TimeoutError('Multiplayer validation timed out')
    finally:
        for proc in processes:
            if proc.poll() is None:
                try:
                    proc.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    proc.terminate()
                    try:
                        proc.wait(timeout=10)
                    except subprocess.TimeoutExpired:
                        proc.kill()
        for log in logs:
            log.close()


if __name__ == '__main__':
    sys.exit(main())
