"""Prepare normal Forge clients from official launcher metadata, with the final addon JAR.

The runtime folder must already contain an isolated Forge 47.4.0 client installation.
Usage: python scripts/prepare_forge_production_clients.py <runtime> <coordinator>
       <Gradle user home> [--without-copycats]
This is a local offline-server test; no account/session credentials are read or stored.
"""
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import sys
import urllib.request
import uuid
import zipfile

root = Path(__file__).resolve().parents[1]
mod_version = next(line.split('=', 1)[1].strip() for line in (root / 'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
runtime, coordinator, cache = (Path(p).resolve() for p in sys.argv[1:4])
without = '--without-copycats' in sys.argv[4:]
vanilla = json.loads((runtime / 'versions/1.20.1/1.20.1.json').read_text())
forge = json.loads((runtime / 'versions/1.20.1-forge-47.4.0/1.20.1-forge-47.4.0.json').read_text())
features = {'has_custom_resolution': True}


def allowed(item):
    rules = item.get('rules')
    if not rules:
        return True
    result = False
    for rule in rules:
        system = rule.get('os', {})
        if system.get('name', 'windows') != 'windows':
            continue
        if system.get('arch', 'x86_64') not in ('x86_64', 'amd64'):
            continue
        if any(features.get(key, False) != value for key, value in rule.get('features', {}).items()):
            continue
        if 'version' in system and not re.match(system['version'], '10.0'):
            continue
        result = rule['action'] == 'allow'
    return result


libraries = {}
for library in vanilla['libraries'] + forge['libraries']:
    if allowed(library):
        coordinate = library['name'].split(':')
        libraries[tuple(coordinate[:2] + coordinate[3:])] = library


def obtain(library):
    artifact = library['downloads']['artifact']
    path = runtime / 'libraries' / artifact['path']
    expected = artifact.get('sha1')
    if path.exists() and (not expected or hashlib.sha1(path.read_bytes()).hexdigest() == expected):
        return path
    path.parent.mkdir(parents=True, exist_ok=True)
    group, name, version, *_ = library['name'].split(':')
    cached = cache / f'caches/modules-2/files-2.1/{group}/{name}/{version}'
    for candidate in cached.glob('*/' + path.name):
        if not expected or hashlib.sha1(candidate.read_bytes()).hexdigest() == expected:
            shutil.copy2(candidate, path)
            return path
    if not artifact.get('url'):
        raise RuntimeError('Missing installer-generated library: ' + str(path))
    request = urllib.request.Request(artifact['url'], headers={'User-Agent': 'CopycatReplace-local-validation/1'})
    with urllib.request.urlopen(request, timeout=60) as response:
        path.write_bytes(response.read())
    if expected and hashlib.sha1(path.read_bytes()).hexdigest() != expected:
        raise RuntimeError('Library hash mismatch: ' + str(path))
    return path


with ThreadPoolExecutor(max_workers=6) as executor:
    paths = list(executor.map(obtain, libraries.values()))
natives = runtime / 'natives'
natives.mkdir(exist_ok=True)
for path in paths:
    if 'natives-windows' in path.name:
        with zipfile.ZipFile(path) as archive:
            for name in archive.namelist():
                if name.endswith('.dll'):
                    (natives / Path(name).name).write_bytes(archive.read(name))
classpath = os.pathsep.join(map(str, paths + [runtime / 'versions/1.20.1/1.20.1.jar']))
launches_path = coordinator / 'launches.json'
launches = json.loads(launches_path.read_text())
java = launches['runServerMulti']['command'][0]


def expand_arguments(items, substitutions):
    result = []
    for item in items:
        if isinstance(item, dict):
            if not allowed(item):
                continue
            item = item['value']
        values = item if isinstance(item, list) else [item]
        for value in values:
            value = re.sub(r'\$\{([^}]+)\}', lambda match: substitutions[match.group(1)], value)
            result.append(value)
    return result


for role in ('Actor', 'Observer'):
    directory = coordinator / role.lower()
    mods = directory / 'mods'
    mods.mkdir(parents=True, exist_ok=True)
    for jar in [root / f'forge1201/build/libs/copycat_replace-forge-1.20.1-{mod_version}.jar',
                root / f'forge1201/build/validation/copycat_replace_tests-{mod_version}.jar',
                root / 'libs/forge1201/create-1.20.1-6.0.8.jar']:
        shutil.copy2(jar, mods / jar.name)
    if not without:
        jar = root / 'libs/forge1201/copycats-3.0.10+mc.1.20.1-forge.jar'
        shutil.copy2(jar, mods / jar.name)
    player = 'Copycat' + role
    offline = uuid.UUID(bytes=hashlib.md5(('OfflinePlayer:' + player).encode()).digest(), version=3)
    substitutions = {
        'natives_directory': str(natives), 'launcher_name': 'CopycatReplaceValidation', 'launcher_version': '1',
        'classpath': classpath, 'library_directory': str(runtime / 'libraries'), 'classpath_separator': os.pathsep,
        'version_name': forge['id'], 'version_type': 'release', 'game_directory': str(directory),
        'assets_root': str(cache / 'caches/forge_gradle/assets'), 'assets_index_name': vanilla['assetIndex']['id'],
        'auth_player_name': player, 'auth_uuid': offline.hex, 'auth_access_token': '0',
        'clientid': '', 'auth_xuid': '', 'user_type': 'legacy', 'resolution_width': '960', 'resolution_height': '640',
    }
    command = [java, '-Xmx2G', '-Dcopycat_replace.multiplayer=' + role,
               '-Dcopycat_replace.coordinator=' + str(coordinator), '-Dcopycat_replace.address=127.0.0.1:25585']
    jvm = expand_arguments(vanilla['arguments']['jvm'] + forge['arguments']['jvm'], substitutions)
    # The inherited vanilla JAR must be excluded from Forge's additional module discovery.
    command += [arg + ',1.20.1.jar' if arg.startswith('-DignoreList=') else arg for arg in jvm]
    command += [forge['mainClass']]
    command += expand_arguments(vanilla['arguments']['game'] + forge['arguments']['game'], substitutions)
    launches['runClient' + role] = {'command': command, 'cwd': str(directory), 'environment': {}}
launches_path.write_text(json.dumps(launches, indent=2))
print(f'Prepared normal Forge clients using {len(paths)} verified libraries: {coordinator}')
