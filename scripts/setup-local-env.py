#!/usr/bin/env python3
"""Create fresh local Docker credentials once, without overwriting an existing setup."""
import base64
import os
from pathlib import Path
import secrets

path = Path(__file__).resolve().parents[1] / '.env'
content = ('DB_NAME=eaproject\nDB_USER=eaproject\nDB_PASSWORD=' + secrets.token_hex(24)
           + '\nJWT_SECRET=' + base64.b64encode(secrets.token_bytes(32)).decode()
           + '\nBACKEND_HOST=127.0.0.1\nBACKEND_PORT=8080\nSERVER_SSL_ENABLED=false\n')
try:
    descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
except FileExistsError:
    raise SystemExit('.env esiste già: configurazione mantenuta.')
with os.fdopen(descriptor, 'w') as output:
    output.write(content)
print('Creato .env con credenziali casuali. I valori non vengono stampati.')
