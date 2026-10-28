#!/usr/bin/env python3
"""API release smoke test. Creates disposable fixture users/offers; use a test database."""
import argparse
from concurrent.futures import ThreadPoolExecutor
from datetime import date, timedelta
import json
import struct
import zlib
import threading
import uuid
from urllib.error import HTTPError
from urllib.request import Request, urlopen

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--url', default='http://127.0.0.1:8080')
parser.add_argument('--allow-write', action='store_true', help='Required: creates RC test data')
args = parser.parse_args()
if not args.allow_write:
    parser.error('Usa --allow-write soltanto su un database di collaudo.')
base = args.url.rstrip('/')
suffix = uuid.uuid4().hex[:10]


def request(method, path, data=None, auth=None, expected=200, extra=None):
    headers = dict(extra or {})
    if auth:
        headers['Authorization'] = auth
    raw = None
    if data is not None:
        headers['Content-Type'] = 'application/json'
        raw = json.dumps(data).encode()
    req = Request(base + path, raw, headers, method=method)
    try:
        response = urlopen(req, timeout=15)
    except HTTPError as error:
        response = error
    with response:
        payload = response.read()
        assert response.status == expected, f'{method} {path}: {response.status}, atteso {expected}: {payload[:300]!r}'
        parsed = json.loads(payload) if 'json' in response.headers.get('Content-Type', '') and payload else payload
        return parsed, response.headers


def upload(path, field, auth):
    def chunk(kind, data):
        return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', zlib.crc32(kind + data))
    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', 1, 1, 8, 2, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(b'\0\xff\0\0')) + chunk(b'IEND', b'')
    boundary = uuid.uuid4().hex
    body = (f'--{boundary}\r\nContent-Disposition: form-data; name="{field}"; filename="rc.png"'
            '\r\nContent-Type: image/png\r\n\r\n').encode() + png + f'\r\n--{boundary}--\r\n'.encode()
    headers = {'Authorization': auth, 'Content-Type': 'multipart/form-data; boundary=' + boundary}
    with urlopen(Request(base + path, body, headers, method='POST'), timeout=15) as result:
        assert result.status == 200
        return json.loads(result.read())


def account(role, number):
    name = f'rc_{suffix}_{number}'
    password = uuid.uuid4().hex + '!Rc9'
    user, _ = request('POST', '/api/users', {'username': name, 'email': name + '@example.test',
                    'fullName': 'Collaudo RC', 'password': password, 'role': role}, expected=201)
    _, headers = request('POST', '/api/users/login', {'identifier': name, 'password': password})
    return user, headers['Authorization'], headers['X-Refresh-Token']


request('GET', '/api/health')
document, _ = request('GET', '/v3/api-docs')
assert '/api/chat/events' in document['paths'] and '/api/health' in document['paths']
image, _ = request('GET', '/photo/default/experience.jpg')
assert len(image) > 100
request('GET', '/api/bookings/mine', expected=401)
organizer, org, _ = account('ORGANIZER', 1)
_, foreign_org, _ = account('ORGANIZER', 2)
traveler, token, refresh = account('TRAVELER', 3)
collaborator, other, _ = account('TRAVELER', 4)
avatar = upload('/api/users/me/profile-photo', 'file', token)['profilePhotoUrl']
request('GET', avatar)
replacement = upload('/api/users/me/profile-photo', 'file', token)['profilePhotoUrl']
assert avatar != replacement
request('GET', avatar, expected=404)
request('GET', replacement)
removed, _ = request('DELETE', '/api/users/me/profile-photo', auth=token)
assert removed['profilePhotoUrl'] is None
request('GET', replacement, expected=404)
start = date.today() + timedelta(days=10)
trip_body = {'title': f'RC {suffix}', 'description': 'Collaudo riproducibile', 'destinationCity': 'Roma',
             'destinationCountry': 'Italia', 'startDate': str(start), 'endDate': str(start + timedelta(days=1)),
             'maxSeats': 5, 'totalPrice': 20, 'status': 'PUBLISHED', 'stages': []}
trip, _ = request('POST', '/api/trips', trip_body, org)
trip_id = trip['tripId']
photos = upload(f'/api/trip-images/trip/{trip_id}/photos', 'files', org)
photo = photos[0]
content, _ = request('GET', photo['imageUrl'])
assert content.startswith(b'\xff\xd8') and len(content) > 100
request('PUT', f'/api/trips/{trip_id}', dict(trip_body, status='DRAFT'), org)
request('GET', f'/api/trip-images/trip/{trip_id}', expected=404)
request('GET', f'/api/trip-images/trip/{trip_id}', auth=org)
request('PUT', f'/api/trips/{trip_id}', trip_body, org)
request('DELETE', f'/api/trip-images/{photo["imageId"]}', auth=org, expected=204)
request('GET', photo['imageUrl'], expected=404)
request('DELETE', f'/api/trips/{trip_id}', auth=foreign_org, expected=403)
request('PUT', f'/api/trips/{trip_id}', trip_body, foreign_org, expected=403)
booking, _ = request('POST', f'/api/bookings/trips/{trip_id}?seats=2', auth=token)
booking_id = booking['bookingId']
received, _ = request('GET', '/api/bookings/organizer/mine', auth=org)
assert any(row['bookingId'] == booking_id for row in received)
calendar, _ = request('GET', f'/api/bookings/{booking_id}/export/ics', auth=token)
assert b'BEGIN:VCALENDAR' in calendar
request('GET', f'/api/bookings/{booking_id}/export/ics', auth=other, expected=403)
request('GET', f'/api/v1/trips/{trip_id}/export/ics', auth=token)
receipt, _ = request('POST', f'/api/payments/mock/bookings/{booking_id}', {}, token)
retry, _ = request('POST', f'/api/payments/mock/bookings/{booking_id}', {}, token)
assert receipt['reference'] == retry['reference'] and receipt['amount'] == 40
statistics, _ = request('GET', '/api/payments/organizer/statistics', auth=org)
assert statistics['periodPaid'] == 40 and len(statistics['months']) == 12
request('GET', '/api/recommendations/mine', auth=token)
request('GET', '/api/recommendations/mine', auth=org, expected=403)
collection, _ = request('POST', '/api/favorite-lists', {'name': 'Piano RC', 'visibility': 'SHARED'}, token, 201)
list_id = collection['id']
request('GET', f'/api/itineraries/{list_id}', auth=other, expected=403)
request('POST', '/api/list-sharings', {'listId': list_id, 'identifier': collaborator['username'], 'permission': 'EDIT'}, token, 201)
notifications, _ = request('GET', '/api/notifications', auth=other)
invitation = next(n for n in notifications if n['type'] == 'LIST_INVITATION')
request('POST', f'/api/notifications/{invitation["notificationId"]}/accept', auth=other)
stop = {'title': 'Museo', 'day': 1, 'position': 1, 'version': 0}
created, _ = request('POST', f'/api/itineraries/{list_id}/stops', stop, token, 201)
path = f'/api/itineraries/{list_id}/stops/{created["id"]}'
request('PUT', path, dict(created, title='Parco'), other)
request('PUT', path, created, token, 409)
conversation, _ = request('POST', '/api/chat/conversations', {'organizerId': organizer['id']}, token)
chat_id = conversation['id']
ready = threading.Event()


def live_event():
    req = Request(base + '/api/chat/events', headers={'Authorization': org, 'Accept': 'text/event-stream'})
    with urlopen(req, timeout=12) as response:
        for raw in response:
            line = raw.decode().strip()
            if line == 'event:ready':
                ready.set()
            if line == 'event:changed':
                return True
    return False


with ThreadPoolExecutor(max_workers=1) as pool:
    future = pool.submit(live_event)
    assert ready.wait(6), 'Connessione SSE non pronta'
    message, _ = request('POST', f'/api/chat/conversations/{chat_id}/messages',
                         {'clientId': str(uuid.uuid4()), 'body': 'Collaudo in tempo reale'}, token)
    assert future.result(timeout=12), 'Messaggio SSE non ricevuto'
request('GET', f'/api/chat/conversations/{chat_id}/messages', auth=other, expected=403)
notifications, _ = request('GET', '/api/notifications', auth=org)
assert any(n['relatedEntityType'] == 'CHAT' for n in notifications)
request('DELETE', f'/api/bookings/{booking_id}', auth=token, expected=204)
history, _ = request('GET', '/api/bookings/mine/history?page=0&size=6', auth=token)
assert any(b['bookingId'] == booking_id for b in history['items'])
request('POST', '/api/users/logout', auth=token, expected=204)
request('GET', '/api/users/me', auth=token, expected=401)
request('POST', '/api/users/refresh', extra={'X-Refresh-Token': refresh}, expected=401)
# Changing untrusted forwarding headers must not evade the socket-peer auth bucket.
limited = False
for attempt in range(25):
    probe = Request(base + '/api/users/exists?email=rc_probe%40example.test', headers={'X-Forwarded-For': f'192.0.2.{attempt + 1}'})
    try:
        response = urlopen(probe, timeout=10)
    except HTTPError as error:
        response = error
    with response:
        response.read()
        assert response.status in (200, 404, 429), f'Risposta inattesa alla verifica email: {response.status}'
        if response.status == 429:
            assert int(response.headers['Retry-After']) >= 1
            assert response.headers.get('Cache-Control') == 'no-store'
            limited = True
            break
assert limited, 'Rate limiting non intervenuto'
request('GET', '/api/health')
print('PASS: PostgreSQL, Swagger, ruoli, CRUD, foto normalizzate/rimosse, gallerie bozze protette, prenotazione, calendario, pagamento idempotente, statistiche, '
      'raccomandazioni, collaborazione, chat SSE, notifiche, storico, revoca JWT e rate limiting.')
print(f'Fixture di collaudo: prefisso rc_{suffix}. Esegui solo su database dedicati.')
