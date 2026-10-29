#!/usr/bin/env python3
"""Create fresh local Docker credentials once, without overwriting an existing setup."""
import base64
import os
from pathlib import Path
import secrets
import shutil

root = Path(__file__).resolve().parents[1]
path = root / '.env'
resources = root / 'ea-project' / 'src' / 'main' / 'resources'
keystore = resources / 'keystore.p12'
server_keystore = root / 'certs' / 'server.p12'


def keystore_password():
    dev = resources / 'application-dev.properties'
    if not dev.exists():
        return ''
    for line in dev.read_text(encoding='utf-8').splitlines():
        if line.startswith('SSL_KEYSTORE_PASSWORD='):
            return line.split('=', 1)[1].strip()
    return ''


if keystore.exists() and not server_keystore.exists():
    server_keystore.parent.mkdir(exist_ok=True)
    shutil.copyfile(keystore, server_keystore)
    print('Copiato keystore.p12 in certs/server.p12.')
elif not server_keystore.exists():
    print('ATTENZIONE: manca certs/server.p12. Copia il keystore del team prima di avviare Docker.')

content = ('BACKEND_IMAGE=ea-project-backend:1.0.0\nDB_IMAGE=postgres:17-alpine\nDB_NAME=eaproject\nDB_USER=eaproject\nDB_PASSWORD=' + secrets.token_hex(24)
           + '\nJWT_SECRET=' + base64.b64encode(secrets.token_bytes(32)).decode()
           + '\nBACKEND_HOST=127.0.0.1\nBACKEND_PORT=8443\nSERVER_SSL_ENABLED=true'
           + '\nSSL_KEYSTORE=file:/app/certs/server.p12\nSSL_KEYSTORE_PASSWORD=' + keystore_password() + '\n')
try:
    descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
except FileExistsError:
    raise SystemExit('.env esiste già: configurazione mantenuta.')
with os.fdopen(descriptor, 'w') as output:
    output.write(content)
print('Creato .env con credenziali casuali. I valori non vengono stampati.')
