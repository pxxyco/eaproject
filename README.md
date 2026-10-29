# EA Project – Prenotazione di viaggi e attività

Progetto del corso **Enterprise Applications 2025/2026** (Università della Calabria).

Applicazione per cercare, prenotare e gestire viaggi, escursioni e attività turistiche, con due tipi di utente:

- **Viaggiatore**: cerca viaggi e attività, prenota, paga (mock), recensisce, crea liste di preferiti private o condivise con altri utenti.
- **Organizzatore**: crea, modifica e cancella viaggi e attività (con tappe, foto e coordinate) e consulta prenotazioni, incassi e statistiche.

## Struttura del repository

| Cartella | Contenuto | README |
|---|---|---|
| [`ea-project/`](ea-project) | Backend Spring Boot (REST API, JPA, JWT) | [ea-project/README.md](ea-project/README.md) |
| [`ea_project_application/`](ea_project_application) | Client mobile Android (Kotlin, Jetpack Compose) | [ea_project_application/README.md](ea_project_application/README.md) |
| [`ea_project_javafx/`](ea_project_javafx) | Client desktop JavaFX | [ea_project_javafx/README.md](ea_project_javafx/README.md) |
| `compose.yaml` | Avvio di backend + PostgreSQL con Docker | |
| `scripts/` | Creazione del file `.env` e smoke test delle API | |

## Tecnologie

- **Backend:** Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA, PostgreSQL, JJWT, Log4j2, springdoc (Swagger), Caffeine
- **Android:** Kotlin, Jetpack Compose, Material 3, OkHttp, Gson, Coil, CameraX, ML Kit
- **Desktop:** Java 21, JavaFX 21
- **Infrastruttura:** Docker, Docker Compose, HTTPS con certificato del team

## Requisiti

- JDK 21
- Docker Desktop
- Android Studio con un emulatore Android (API 30 o superiore)
- Il **keystore del team** `keystore.p12` e il file `application-dev.properties`, condivisi sul canale Discord. Non sono su GitHub.

Maven e Gradle non vanno installati: ogni progetto ha il proprio wrapper (`mvnw`, `gradlew`).

## Avvio con Docker

Il backend in Docker risponde su **https://localhost:8443** con lo stesso certificato di cui si fidano i client Android e JavaFX.

### Con le immagini consegnate

Copia nella cartella principale del progetto i file consegnati a parte (`ea-project-images.tar`, `.env`, `certs/server.p12`), poi:

```bash
docker load -i ea-project-images.tar
docker compose up -d
```

### Dal codice

1. Copia `keystore.p12` in `ea-project/src/main/resources/` e `application-dev.properties` nella stessa cartella.
2. Crea il file `.env` e il keystore per Docker:

   ```bash
   python scripts/setup-local-env.py
   ```

   Lo script genera password casuali per database e JWT, copia il keystore in `certs/server.p12` e legge la password del keystore da `application-dev.properties`. Se `.env` esiste già non lo tocca.

3. Avvia database e backend:

   ```bash
   docker compose up -d --build
   ```

### Verifica

Controlla che risponda (`-k` perché il certificato è self-signed):

```bash
curl -k https://localhost:8443/api/health
```

Per fermare tutto: `docker compose down`. Per cancellare anche i dati: `docker compose down -v`.

## Avvio del backend senza Docker

Serve un PostgreSQL locale con i dati indicati in `application-dev.properties`.

```bash
cd ea-project
./mvnw spring-boot:run
```

Il backend parte su **https://localhost:8443**. Dettagli in [ea-project/README.md](ea-project/README.md).

## Avvio dell'app Android

1. Avvia il backend (con Docker o senza).
2. Avvia l'emulatore da Android Studio.
3. Inoltra la porta 8443 dell'emulatore al PC. Va rifatto **a ogni riavvio dell'emulatore o di adb**:

   ```bash
   adb reverse tcp:8443 tcp:8443
   ```

4. Apri `ea_project_application` in Android Studio ed esegui la configurazione `app` (variante **debug**).

Senza `adb reverse` l'app mostra l'errore di connessione ERR-101. Dettagli in [ea_project_application/README.md](ea_project_application/README.md).

## Avvio del client JavaFX

```bash
./ea-project/mvnw -f ea_project_javafx/pom.xml javafx:run
```

Per default si collega a `https://localhost:8443`. Dettagli in [ea_project_javafx/README.md](ea_project_javafx/README.md).

## Documentazione delle API

Con il backend avviato, Swagger UI è disponibile su **https://localhost:8443/swagger-ui.html**.

## Account demo

Password per tutti: `Demo2026!`

| Utente | Ruolo |
|---|---|
| `calabria.tour` | Organizzatore |
| `sud.experience` | Organizzatore |
| `mario.rossi` | Viaggiatore |
| `giulia.bianchi` | Viaggiatore |

## Funzionalità implementate

**Requisiti obbligatori**
- API REST con Spring, mapping JPA su PostgreSQL
- Autenticazione e autorizzazione con JWT (access token + refresh token, ruoli TRAVELER e ORGANIZER)
- Client mobile Android
- Backend dockerizzato
- Liste di preferiti private e condivise con utenti specifici (permesso di sola lettura o modifica)
- Gestione di viaggi e attività da parte dell'organizzatore, con elenco delle prenotazioni ricevute

**Funzionalità facoltative**
- Recensioni e valutazioni, con risposta dell'organizzatore
- Pagamento mock con carte salvate
- Export delle prenotazioni in formato calendario `.ics`
- Itinerari con più tappe
- Visualizzazione delle tappe su mappa (client JavaFX)
- Notifiche in-app (prenotazioni, pagamenti, inviti alle liste)

**Bonus**
- Audit logging, rate limiting, test automatici, documentazione Swagger, HTTPS
- Itinerari collaborativi sulle liste condivise
- Raccomandazioni basate su preferiti e prenotazioni precedenti
- Chat in tempo reale tra viaggiatore e organizzatore (Server-Sent Events)

## Contributi del gruppo

Ricostruiti dalla cronologia dei commit (`git log`).

### Bruno (`brunolen`)
- **Backend:** entità JPA, repository, service e controller per **Booking, Payment e Review**; endpoint per le recensioni (creazione, modifica, cancellazione, ricalcolo della media voti) e per le **risposte dell'organizzatore**; creazione delle tappe (`Stage`); tabella **FAVORITES** ed endpoint `/api/favorites`.
- **JavaFX:** pagina di dettaglio di attività e viaggi (descrizione, itinerario con mappa e tappe, galleria foto con vista a schermo intero, cuore dei preferiti); interfaccia delle recensioni con paginazione, modifica e risposte.
- **Android:** schermata di **dettaglio dell'esperienza** (tab, descrizione, itinerario e timeline delle tappe, foto, recensioni con modifica e risposte, prenotazione, preferiti e aggiunta alle liste).

### Matteo Marzano (`pxxyco`)
- **Backend:** entità JPA Notification, Stage e Trip con relativi service e controller; Log4j2; **raccomandazioni**, rate limiting e sicurezza JWT; scheletro di **chat** e **Swagger**; **pagamenti mock**; **Docker** del backend; Spring Boot Actuator.
- **JavaFX:** creazione del progetto; profilo viaggiatore e organizzatore, foto profilo, navbar, home e animazioni; **liste di preferiti** (creazione, modifica, condivisione, eliminazione); sistema di notifiche; **export `.ics`**; storico prenotazioni; interfaccia dell'organizzatore rifatta; riorganizzazione dei CSS e del codice.
- **Android:** profilo del viaggiatore.

### Antonio Franco (`Yorshkrum`)
- **Backend:** entità JPA Activity, ActivityImage e TripImage, con repository, service e controller (anche User); **archiviazione delle foto** di attività e viaggi con copertine e ordinamento; primo export calendario (`CalendarController`).
- **JavaFX:** **dashboard dell'organizzatore** (creazione, modifica ed eliminazione di attività e viaggi).
- **Android:** **dashboard dell'organizzatore** (foto, itinerari, coordinate, filtri, stati, posti, prenotazioni, incassi); gestione di liste e prenotazioni (eliminazione liste, rimozione elementi, annullamento prenotazioni); barra di navigazione inferiore; correzioni dopo i merge.

### Luca Santoro (`lucoilbasso`)
- **Backend:** creazione del progetto Spring; entità User, FavoriteList, ListTrips, ListActivities, ListSharing e Response; **JWT** e `SecurityConfig`; sicurezza del login (BCrypt, cache Caffeine, protezione da timing attack); **rate limiting**; salvataggio delle foto su disco; **HTTPS** con i certificati per backend, JavaFX e Android.
- **JavaFX:** login e home, `ApiService` (GET/POST/PUT/DELETE), DTO, barra di ricerca con filtri e categorie, card delle esperienze, cache delle immagini, gestione degli errori di connessione e dei messaggi di login.
- **Android:** creazione del progetto; `ApiService`, `UserSession`, JWT; login e profilo; **home** collegata al backend con suggerimenti, filtro per data e caricamento progressivo; preferiti.
