# API REST

La fonte interattiva è Swagger, generato dal backend avviato:

- `https://localhost:8443/swagger-ui.html`
- `https://localhost:8443/v3/api-docs`
- `https://localhost:8443/api/health`

Non usare un token reale negli screenshot delle segnalazioni. Per chiamate protette usa `Authorization: Bearer <access-token>` e rinnova la sessione tramite il flusso refresh del client.

| Area | Prefisso |
|---|---|
| Utenti e autenticazione | `/api/users` |
| Account, preferenze e assistenza | `/api/account` |
| Viaggi e attività | `/api/trips`, `/api/activities` |
| Prenotazioni | `/api/bookings` |
| Pagamenti mock e dati Organizer | `/api/payments` |
| Recensioni e risposte | `/api/reviews` e API delle risposte |
| Raccolte e condivisioni | `/api/favorite-lists`, `/api/list-sharings` |
| Raccomandazioni | `/api/recommendations` |
| Notifiche | `/api/notifications` |
| Chat | `/api/chat` |

## Flussi da integrare

- Prenotazione: il client invia occorrenza e posti, il backend determina utente, disponibilità e prezzo.
- Scheda personale: `GET /api/bookings/mine/experience` richiede **un solo** parametro tra `tripId` e `activityId`.
- Storico: `GET /api/bookings/mine/history` pagina i dati sul backend; non scaricare tutta la cronologia.
- Pagamento mock: `POST /api/payments/mock/bookings/{bookingId}` aggiorna una prenotazione propria.
- Messaggi: `GET /api/chat/unread-count` restituisce il conteggio dell’utente autenticato, non quello delle notifiche.

## Risposte

`400` indica input o regole del flusso non rispettate; `401` sessione assente/scaduta; `403` risorsa non autorizzata; `404` risorsa non disponibile; `409` conflitto, per esempio una prenotazione già attiva; `429` richiede attesa prima di riprovare. Leggi anche il messaggio della risposta: non ripetere automaticamente una mutazione dopo un errore.

I dettagli dei payload, le rotte complete e i requisiti di ogni operazione sono in Swagger e nei controller della versione installata. Mantieni backend e client della stessa release.
