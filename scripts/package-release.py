#!/usr/bin/env python3
"""Package already-built EA Project clients + backend. Never publishes or reads local secrets."""
import argparse
import hashlib
import json
import platform
import re
import shutil
import subprocess
import tempfile
import zipfile
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def git(*args):
    return subprocess.check_output(['git', '-C', str(ROOT), *args])


def private_path(name):
    path = Path(name)
    return (path.name in {'application-dev.properties', '.env', '.env.local'}
            or path.name.startswith('application-local.')
            or path.suffix.lower() in {'.p12', '.jks', '.key', '.keystore'}
            or path.name.startswith('.env.') and path.name != '.env.example')


def inspect_jar(path):
    with zipfile.ZipFile(path) as jar:
        forbidden = [name for name in jar.namelist() if private_path(name)]
        if forbidden:
            raise ValueError('Il JAR contiene configurazioni private o keystore: usa il profilo release-dist da una directory di build pulita.')


def one_jar(directory):
    jars = [p for p in directory.glob('*.jar') if not p.name.endswith(('-sources.jar', '-javadoc.jar', '.original.jar'))]
    if len(jars) != 1:
        raise ValueError(f'Atteso un JAR in {directory}; specifica il percorso con l’opzione dedicata.')
    return jars[0]


def checksum(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def write_archive(directory, output):
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(directory.rglob('*')):
            if path.is_file():
                archive.write(path, Path(directory.name) / path.relative_to(directory))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--version', required=True, help='Per esempio 1.0.0 o 1.0.0-rc.1')
    parser.add_argument('--android-apk', type=Path, required=True)
    parser.add_argument('--android-kind', choices=['demo', 'release'], required=True)
    parser.add_argument('--backend-jar', type=Path)
    parser.add_argument('--desktop-jar', type=Path)
    parser.add_argument('--desktop-lib', type=Path, default=ROOT / 'ea_project_javafx/target/release-lib')
    parser.add_argument('--apksigner', help='Richiesto per release se apksigner non è nel PATH')
    parser.add_argument('--allow-dirty', action='store_true', help='Solo anteprima locale, marcata dirty nel manifest')
    args = parser.parse_args()
    if not re.fullmatch(r'\d+\.\d+\.\d+(?:-[A-Za-z0-9.-]+)?', args.version):
        parser.error('Versione non valida.')
    dirty = bool(git('status', '--porcelain').strip())
    if dirty and not args.allow_dirty:
        parser.error('Ci sono modifiche non committate. Rilascia da un checkout pulito o usa --allow-dirty solo per una prova locale.')
    try:
        backend = args.backend_jar or one_jar(ROOT / 'ea-project/target/release-dist')
        desktop = args.desktop_jar or one_jar(ROOT / 'ea_project_javafx/target')
        for jar in [backend, desktop]:
            inspect_jar(jar)
        with zipfile.ZipFile(desktop) as archive:
            if 'it/unical/ea_project_javafx/Launcher.class' not in archive.namelist():
                raise ValueError('Ricompila JavaFX: manca il Launcher della distribuzione.')
        libraries = sorted(args.desktop_lib.glob('*.jar'))
        if not libraries or not any('javafx-graphics' in p.name for p in libraries):
            raise ValueError('Mancano le dipendenze desktop: esegui dependency:copy-dependencies come descritto nella guida.')
        with zipfile.ZipFile(args.android_apk) as apk:
            if 'AndroidManifest.xml' not in apk.namelist():
                raise ValueError('Il file indicato non è un APK Android.')
        if args.android_kind == 'release':
            signer = args.apksigner or shutil.which('apksigner')
            if not signer:
                raise ValueError('Indica --apksigner con il percorso del tool Android SDK.')
            result = subprocess.run([signer, 'verify', '--print-certs', str(args.android_apk)], capture_output=True, text=True)
            if result.returncode or 'Android Debug' in result.stdout:
                raise ValueError('La release richiede un APK con firma valida, diversa dalla chiave debug.')
    except (OSError, ValueError, zipfile.BadZipFile) as error:
        parser.error(str(error))

    target_os = {'darwin': 'macos'}.get(platform.system().lower(), platform.system().lower())
    target = target_os + '-' + platform.machine().lower()
    output = ROOT / 'dist' / args.version
    output.mkdir(parents=True, exist_ok=True)
    names = [f'EA-Project-{args.version}-source.zip', f'EA-Project-{args.version}-desktop-backend-{target}.zip',
             f'EA-Project-{args.version}-android-backend-{args.android_kind}.zip', 'SHA256SUMS.txt']
    if any((output / name).exists() for name in names):
        parser.error('La directory contiene già una distribuzione con questi nomi: non sovrascrivo gli artefatti.')
    files = sorted(set(git('ls-files', '--cached', '--others', '--exclude-standard', '-z').decode().split('\0')) - {''})
    files = [name for name in files if not set(Path(name).parts).intersection(
        {'.idea', '.git', '.gradle', 'target', 'build', '__pycache__'})]
    if any(private_path(name) for name in files):
        parser.error('Un file privato è tra i sorgenti pubblicabili: controlla Git e .gitignore.')
    source = output / names[0]
    # The exact reviewed working files are included; dirty previews are marked in the manifest.
    with zipfile.ZipFile(source, 'w', zipfile.ZIP_DEFLATED) as archive:
        for name in files:
            path = ROOT / name
            if path.is_symlink():
                raise ValueError('Link simbolico nei sorgenti: verificare manualmente prima del packaging.')
            if path.is_file():
                archive.write(path, name)
    metadata = {'version': args.version, 'commit': git('rev-parse', 'HEAD').decode().strip(), 'dirty': dirty,
                'created_at': datetime.now(timezone.utc).isoformat(), 'desktop_target': target,
                'android_kind': args.android_kind, 'backend_sha256': checksum(backend),
                'desktop_sha256': checksum(desktop), 'android_sha256': checksum(args.android_apk),
                'source_sha256': checksum(source)}
    with tempfile.TemporaryDirectory(prefix='ea-release-') as temporary:
        for kind, name in [('desktop', names[1]), ('android', names[2])]:
            bundle = Path(temporary) / name.removesuffix('.zip')
            backend_dir = bundle / 'backend'; backend_dir.mkdir(parents=True)
            client = bundle / 'client'; client.mkdir()
            for filename in files:
                origin = ROOT / filename
                if origin.is_file():
                    destination = bundle / filename
                    destination.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(origin, destination)
            (bundle / 'release-manifest.json').write_text(json.dumps(metadata, indent=2) + '\n')
            shutil.copy2(backend, backend_dir / 'app.jar')
            if kind == 'desktop':
                shutil.copy2(desktop, client / 'ea-project-desktop.jar')
                (client / 'lib').mkdir()
                for library in libraries:
                    shutil.copy2(library, client / 'lib' / library.name)
                launcher = client / 'start.sh'
                launcher.write_text('''#!/bin/sh
set -eu
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -n "${JAVA_HOME:-}" ]; then JAVA="$JAVA_HOME/bin/java"; else JAVA=java; fi
exec "$JAVA" -cp "$HERE/ea-project-desktop.jar:$HERE/lib/*" it.unical.ea_project_javafx.Launcher "$@"
''')
                launcher.chmod(0o755)
                (client / 'start.cmd').write_text('''@echo off
setlocal
set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\\bin\\java.exe"
"%JAVA%" -cp "%~dp0ea-project-desktop.jar;%~dp0lib\\*" it.unical.ea_project_javafx.Launcher %*
''')
            else:
                shutil.copy2(args.android_apk, client / f'EA-Project-{args.version}-{args.android_kind}.apk')
            (bundle / 'START_HERE.md').write_text(f'''# EA Project {args.version}

Pacchetto {kind}. Commit e impronte degli artefatti: release-manifest.json.
Questa build è {'un’anteprima locale con modifiche non committate' if dirty else 'associata al commit indicato nel manifest'}.

1. Configura il backend seguendo docs/GETTING_STARTED.md, dalla cartella principale del pacchetto.
   Il keystore TLS e le credenziali vanno forniti separatamente; non sono inclusi.
2. Avvia backend + PostgreSQL con il Compose incluso, oppure usa backend/app.jar con Java 21 e variabili esterne.
3. {'Con Java 21 avvia client/start.sh (Linux/macOS) o client/start.cmd (Windows). Le librerie native sono per ' + target + '.' if kind == 'desktop' else 'Installa l’APK da client/. La variante demo usa il collegamento locale adb reverse e il certificato di sviluppo.'}
4. Consulta docs/RELEASING.md e docs/CONFIGURATION.md per endpoint, certificati e differenze debug/release.

Il backend usa pagamenti mock. Il codice sorgente corrispondente è incluso nelle cartelle del progetto con LICENSE; la release offre anche un archivio source separato.
''')
            write_archive(bundle, output / name)
    (output / names[3]).write_text(''.join(f'{checksum(output / name)}  {name}\n' for name in names[:3]))
    print('Pacchetti preparati in', output)
    if dirty:
        print('ANTEPRIMA LOCALE: dirty=true. Non pubblicare come release definitiva.')


if __name__ == '__main__':
    main()
