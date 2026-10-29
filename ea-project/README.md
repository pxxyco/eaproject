# ea-project – Backend

Backend REST dell'applicazione, scritto con **Spring Boot 4.1** e **Java 21**.

## Stack

- Spring Web MVC, Spring Security, Spring Data JPA (Hibernate)
- PostgreSQL in esecuzione, H2 in memoria per i test
- JWT con JJWT (access token + refresh token)
- Log4j2 con log applicativo (`app.log`) e di audit (`audit.log`)
- springdoc-openapi (Swagger UI)
- Caffeine per cache e tentativi di login

## Configurazione

`application.properties` legge i valori da variabili d'ambiente oppure dal file `src/main/resources/application-dev.properties`, che **non è su GitHub** e va preso dal canale del team:

| Variabile | Uso |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PSW` | Connessione a PostgreSQL |
| `JWT_SECRET` | Chiave HMAC in Base64 per firmare i token |
| `SSL_KEYSTORE_PASSWORD` | Password di `keystore.p12` |
| `SERVER_PORT` | Porta (default `8443`) |
| `SERVER_SSL_ENABLED` | HTTPS attivo (default `true`) |

Anche il keystore `src/main/resources/keystore.p12` (alias `ea-server`, CN `localhost`) non è su GitHub. Contiene lo stesso certificato di cui si fidano i client Android (`ea_cert.cer`) e JavaFX (`dev-ca.crt`): se ne generate uno nuovo, i client non si collegano più.

## Avvio in locale

Con PostgreSQL in esecuzione e i due file sopra al loro posto:

```bash
./mvnw spring-boot:run
```

Il server parte su `https://localhost:8443`. Le tabelle vengono create o aggiornate da Hibernate (`ddl-auto=update`).

## Avvio con Docker

Dalla cartella principale del repository:

```bash
python scripts/setup-local-env.py
docker compose up -d --build
```

Il profilo `container` (`application-container.properties`) ascolta sulla porta 8080 del container, che Compose espone su `https://localhost:8443`. Il keystore viene montato da `certs/server.p12`. Il container gira come utente non-root con filesystem in sola lettura; foto e log stanno nei volumi `media` e `logs`.

## Test

```bash
./mvnw test
```

I test usano H2 in modalità PostgreSQL (`src/test/resources/application.properties`) e non richiedono database o keystore. Comprendono test di integrazione sui flussi obbligatori, sull'organizzatore (anche con richieste concorrenti), sulla sicurezza del login e sulle raccomandazioni.

## Documentazione delle API

- Swagger UI: `https://localhost:8443/swagger-ui.html`
- OpenAPI JSON: `https://localhost:8443/v3/api-docs`
- Health check: `https://localhost:8443/api/health`

## Struttura del codice

```
it.unical.ea_project
├── config        SecurityConfig, OpenAPI, risorse statiche
├── controller    Endpoint REST, divisi per dominio
├── domain        Entità JPA
├── dto, mapper   Oggetti di trasferimento e conversioni
├── repository    Repository Spring Data
├── service       Logica di business (interfacce + impl)
├── filter        JWT, rate limiting, tempi di risposta
├── security      Utente autenticato, policy password
├── event         Eventi della chat (SSE)
└── util          Export .ics, paginazione
```

## Endpoint principali

| Area | Prefisso | Accesso |
|---|---|---|
| Utenti e autenticazione | `/api/users` | Login e registrazione pubblici |
| Account (carte, impostazioni, ticket) | `/api/account` | Autenticato |
| Viaggi e tappe | `/api/trips`, `/api/trips/{id}/stages` | GET pubblico, scrittura per ORGANIZER |
| Attività | `/api/activities` | GET pubblico, scrittura per ORGANIZER |
| Foto | `/api/trip-images`, `/api/activity-images` | GET pubblico, scrittura per ORGANIZER |
| Prenotazioni | `/api/bookings` | TRAVELER; `/organizer/**` per ORGANIZER |
| Pagamenti mock e statistiche | `/api/payments` | Autenticato; `/organizer/**` per ORGANIZER |
| Recensioni | `/api/reviews` | GET pubblico, creazione per TRAVELER |
| Risposte alle recensioni | `/api/responses` | ORGANIZER |
| Preferiti e liste | `/api/favorites`, `/api/favorite-lists`, `/api/list-trips`, `/api/list-activities` | Autenticato |
| Condivisione liste | `/api/list-sharings` | Autenticato |
| Itinerari collaborativi | `/api/itineraries` | Autenticato |
| Notifiche | `/api/notifications` | Autenticato |
| Raccomandazioni | `/api/recommendations` | Autenticato |
| Chat (SSE) | `/api/chat` | Autenticato |

## Sicurezza

- Password salvate con BCrypt; blocco temporaneo dopo troppi tentativi di login falliti.
- Token JWT verificati da `JwtAuthenticationFilter`; ruoli `TRAVELER` e `ORGANIZER`.
- Rate limiting per IP e per utente (`RateLimitFilter`), con risposta 429 e header `Retry-After`.
- Logger `AUDIT` per login, accessi negati e operazioni sensibili.
