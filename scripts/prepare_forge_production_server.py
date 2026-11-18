"""Prepare the normal Forge server using final reobfuscated addon and validation JARs.

Usage: python scripts/prepare_forge_production_server.py <installed Forge 47.4.0 folder>
       [--without-copycats]
First build :forge1201:validationJar and export the matching development launches.
Clients retain their exported development runtime; the server uses production artifacts.
"""
import json
from pathlib import Path
import shutil
import sys

root = Path(__file__).resolve().parents[1]
mod_version = next(line.split('=', 1)[1].strip() for line in (root / 'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
installation = Path(sys.argv[1]).resolve()
without = '--without-copycats' in sys.argv[2:]
suffix = 'create' if without else 'copycats'
source = root / f'forge1201/build/multiplayer-{suffix}'
destination = root / f'forge1201/build/multiplayer-production-{suffix}'
destination.mkdir(parents=True, exist_ok=True)
launches = json.loads((source / 'launches.json').read_text())
server = destination / 'server'
server.mkdir(exist_ok=True)
shutil.copytree(installation / 'libraries', server / 'libraries', dirs_exist_ok=True)
for filename in ('eula.txt', 'server.properties'):
    shutil.copy2(source / 'server' / filename, server / filename)
mods = server / 'mods'
mods.mkdir(exist_ok=True)
for jar in [root / f'forge1201/build/libs/copycat_replace-forge-1.20.1-{mod_version}.jar',
            root / f'forge1201/build/validation/copycat_replace_tests-{mod_version}.jar',
            root / 'libs/forge1201/create-1.20.1-6.0.8.jar']:
    shutil.copy2(jar, mods / jar.name)
if not without:
    jar = root / 'libs/forge1201/copycats-3.0.10+mc.1.20.1-forge.jar'
    shutil.copy2(jar, mods / jar.name)
java = launches['runServerMulti']['command'][0]
launches['runServerMulti'] = {
    'command': [java, '-Xmx2G', '-Dcopycat_replace.multiplayerServer=true',
                '-Dcopycat_replace.coordinator=' + str(destination),
                '@libraries/net/minecraftforge/forge/1.20.1-47.4.0/win_args.txt', '--nogui'],
    'cwd': str(server), 'environment': {},
}
for name in ('runClientActor', 'runClientObserver'):
    launches[name]['command'] = [arg.replace(str(source), str(destination)) for arg in launches[name]['command']]
    launches[name]['cwd'] = str(destination / ('actor' if name.endswith('Actor') else 'observer'))
    Path(launches[name]['cwd']).mkdir(exist_ok=True)
(destination / 'launches.json').write_text(json.dumps(launches, indent=2))
print(destination)
