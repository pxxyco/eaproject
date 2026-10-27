# EA Project

Gestione di viaggi e attività turistiche: API Spring/JPA con PostgreSQL e client JavaFX.
Il client Android è un modulo separato, escluso dall'attuale verifica della candidata.

## Avvio del backend con Docker

Servono Docker Engine/Desktop con Compose e Python 3 soltanto per generare `.env`.
Eseguire dalla cartella principale del progetto:

```sh
python3 scripts/setup-local-env.py
docker compose up -d --build --wait
```

Il primo comando crea password e chiave JWT casuali senza stamparle e senza sovrascrivere
un `.env` esistente. In alternativa copiare `.env.example` in `.env` e compilare i valori.
Il database viene creato automaticamente dal container PostgreSQL: non occorre un suo Dockerfile.
Il database non pubblica porte sull'host. Il backend attende il controllo di disponibilità del database;
questa dipendenza usa `service_healthy` di [Docker Compose](https://docs.docker.com/compose/how-tos/startup-order/).

- API e disponibilità: `http://127.0.0.1:8080/api/health`
- Swagger: `http://127.0.0.1:8080/swagger-ui/index.html`
- Specifica OpenAPI: `http://127.0.0.1:8080/v3/api-docs`

L'avvio locale usa HTTP e pubblica il servizio solo su localhost. Per il client JavaFX impostare
la proprietà JVM `-Deaproject.api.url=http://127.0.0.1:8080`; non disabilitare la verifica TLS globale.
Per raggiungere il backend da un altro dispositivo configurare deliberatamente `BACKEND_HOST`
e usare HTTPS con un certificato valido per il nome host di destinazione.

I volumi `database`, `media` e `logs` mantengono database, immagini pubbliche, allegati chat privati
e log. Gli allegati chat non sono serviti dalla cartella pubblica delle foto. Il backend usa un utente
non root, filesystem in sola lettura, `/tmp` temporaneo e limiti di memoria.
L'immagine usa una [build a più stadi](https://docs.docker.com/build/building/multi-stage/): Maven
compila con Java 21; l'immagine finale contiene il JRE, non i sorgenti o gli strumenti di compilazione.
Configurazioni personali, password, keystore, test e foto locali sono esclusi dal contesto di build.

```sh
docker compose ps
docker compose logs --tail=100 backend
docker compose down
```

`down` ferma i servizi mantenendo i volumi. `down -v` elimina i dati: usarlo solo per ambienti sacrificabili.
Non cambiare `DB_USER`, `DB_NAME` o `DB_PASSWORD` su un volume già inizializzato aspettandosi che
PostgreSQL modifichi automaticamente gli account: queste variabili inizializzano un database vuoto.
Vedi la [documentazione dell'immagine PostgreSQL](https://hub.docker.com/_/postgres).

## HTTPS nel container

Il backend ha già supporto TLS. Per utilizzarlo anche con Compose:

1. Mettere il certificato PKCS12 in `certs/server.p12`, leggibile dall'UID 10001. L'alias atteso è `ea-server`.
2. Impostare `SERVER_SSL_ENABLED=true` e `SSL_KEYSTORE_PASSWORD` in `.env`.
3. Riavviare il backend con `docker compose up -d` e collegarsi a `https://HOST:8080`.

Il keystore non va committato né copiato nell'immagine. Il solo controllo interno di salute accetta
il certificato locale; i client mantengono la normale verifica di certificato e nome host.
La validazione del certificato del deployment finale rimane a carico del responsabile HTTPS.

## Autenticazione e API

Registrazione e login restituiscono i dati pubblici dell'utente. Il login restituisce anche
`Authorization: Bearer ...` e `X-Refresh-Token`. Gli endpoint protetti richiedono il primo header.
Il refresh invia il secondo header con `POST /api/users/refresh`. Il logout revoca tutte le sessioni
dell'account, inclusi access token e refresh token precedenti.
Swagger descrive i percorsi e permette l'autenticazione tramite il pulsante Authorize.
I limiti di frequenza restituiscono HTTP 429, `Retry-After` e codice applicativo `ERR-104`.
La chat usa SSE autenticato tramite header, con aggiornamenti dopo il commit e cronologia persistita.

Il pagamento è dimostrativo: non addebita denaro e non raccoglie dati di carte. Gli importi vengono
ricavati dalla prenotazione; i tentativi ripetuti non generano un secondo pagamento.
Le informazioni di payout salvate non eseguono bonifici.

## Collaudo

```sh
mvn -f ea-project/pom.xml clean verify
mvn -f ea_project_javafx/pom.xml clean compile
```

Su un database Docker dedicato, il seguente script esegue un collaudo HTTP con PostgreSQL reale:

```sh
python3 scripts/verify-backend.py --url http://127.0.0.1:8080 --allow-write
```

Lo script crea utenti, offerte, raccolte, messaggi e prenotazioni di prova: non eseguirlo su dati reali.
Le fixture hanno un prefisso casuale `rc_...`. La suite Maven usa H2 e copre anche i casi di concorrenza;
lo script aggiunge i controlli delle query PostgreSQL e della consegna SSE effettiva.
Il Dockerfile non rilancia i test Maven: vanno eseguiti prima del build o nella pipeline CI.

## Stato della candidata

Vedere [il confronto completo con la traccia](docs/RC_AUDIT.md), con requisiti obbligatori,
bonus verificati e limiti che restano prima di considerare conclusa la consegna.
