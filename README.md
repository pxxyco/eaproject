<p align="center"><img src="ea_project_javafx/src/main/resources/it/unical/ea_project_javafx/images/monogram_icon.png" alt="EA Project" width="88"></p>

# EA Project

**Viaggi, attività e itinerari condivisi, dal desktop al telefono.**

**Prima build ufficiale: RTM 1.0.0** · Tag di release: `v1.0.0`.

Piattaforma realizzata per il corso **Enterprise Applications 2025/2026 dell’Università della Calabria**, con API Spring Boot, client JavaFX e app Android. Traveler e Organizer condividono lo stesso backend e gli stessi dati.

[Guida iniziale](docs/GETTING_STARTED.md) · [Documentazione](docs/README.md) · [FAQ](docs/FAQ.md) · [Release](docs/RELEASING.md) · [Contribuire](CONTRIBUTING.md)

## Cosa offre

| Area | JavaFX | Android |
|---|---|---|
| Ricerca, filtri, dettagli e raccomandazioni | Sì | Sì |
| Profilo, foto, sicurezza e account Traveler | Sì | Sì |
| Prenotazioni, annullamento e recensioni | Sì | Sì |
| Preferiti e raccolte personali | Sì | Sì |
| Gestione offerte e prenotazioni dell’Organizer | Sì | Sì |
| Pagamento mock delle prenotazioni | Sì | Sì |
| Condivisione delle raccolte e itinerari collaborativi | Sì | Gestione personale delle raccolte |
| Chat in tempo reale con risposte e foto | Sì | Sì |
| Centro notifiche, export `.ics` e mappa | Sì | Vedi [funzioni mobile](ea_project_application/README.md) |

Le funzioni comuni sono protette da JWT, ruoli e controlli di proprietà sul backend. I pagamenti sono **mock**, senza addebiti reali. I dettagli operativi sono nel [manuale utente](docs/USER_GUIDE.md).

## Struttura

| Percorso | Contenuto |
|---|---|
| [ea-project](ea-project/README.md) | Backend Java 21, Spring Boot 4.1, JPA, PostgreSQL, JWT, Log4j2 e Swagger |
| [ea_project_javafx](ea_project_javafx/README.md) | Desktop Java 21 / JavaFX 21, FXML e CSS modulari |
| [ea_project_application](ea_project_application/README.md) | Android 11+, Kotlin, Compose, Material 3 e CameraX |
| [compose.yaml](compose.yaml) | Backend e database PostgreSQL in Docker |
| [scripts](scripts) | Configurazione locale, verifica API e preparazione delle distribuzioni |
| [docs](docs/README.md) | Installazione, architettura, test, sicurezza operativa e release |

## Avvio rapido

Occorrono JDK 21, Python 3 e Docker con Compose. Per Android servono Android Studio e SDK 37; il dispositivo può usare Android 11/API 30 o superiore. I wrapper Maven e Gradle sono inclusi.

### Backend con le immagini consegnate

Metti `ea-project-images-1.0.0.tar`, il `.env` della consegna e `certs/server.p12` accanto a `compose.yaml`. Nel `.env` deve esserci `BACKEND_IMAGE=ea-project-backend:1.0.0` e `DB_IMAGE=postgres:17-alpine`, per usare le immagini della consegna RTM:

```bash
docker load -i ea-project-images-1.0.0.tar
docker compose up -d --no-build --pull never
```

Il pacchetto Docker RTM viene esportato con `ea-project-backend:1.0.0` e `postgres:17-alpine`; il database standard parte vuoto su un volume nuovo. [File necessari, verifica e gestione dei dati](docs/GETTING_STARTED.md#con-il-pacchetto-docker-rtm).

### Backend dal codice

Copia `keystore.p12` e `application-dev.properties` del team in `ea-project/src/main/resources/`, poi:

```bash
python3 scripts/setup-local-env.py
docker compose up -d --build
```

Il setup non sovrascrive un `.env` esistente. [Guida completa Docker e avvio senza Docker](docs/GETTING_STARTED.md#avvio-con-docker).

### Client

JavaFX, da un altro terminale nella cartella principale:

```bash
./ea-project/mvnw -f ea_project_javafx/pom.xml javafx:run
```

Android, con emulatore avviato e backend sullo stesso computer:

```bash
adb reverse tcp:8443 tcp:8443
cd ea_project_application
./gradlew :app:installDebug
```

Backend: `https://localhost:8443` · Swagger: `https://localhost:8443/swagger-ui.html` · Health: `https://localhost:8443/api/health`.

Il certificato locale è di sviluppo: per una distribuzione pubblica servono un endpoint raggiungibile e una configurazione TLS adatta ai client. Vedi [configurazione](docs/CONFIGURATION.md) e [release](docs/RELEASING.md).

## Qualità e collaborazione

- [Test e collaudi](docs/TESTING.md): API, concorrenza, componenti desktop, JVM Android e prove su dispositivo.
- [Segnalare un problema](https://github.com/pxxyco/untitled_ea_project/issues/new/choose): modelli per bug, richieste e domande.
- [Policy di sicurezza](SECURITY.md): segnalazioni riservate e dati da non pubblicare.
- [Versioni ufficiali](https://github.com/pxxyco/untitled_ea_project/releases), [autori](AUTHORS.md) e [dipendenze/asset](THIRD_PARTY_NOTICES.md).

## Licenza

Il codice originale di EA Project è distribuito sotto **GNU General Public License v3.0**, identificatore SPDX `GPL-3.0-only`. Il testo completo è in [LICENSE](LICENSE). Dipendenze e contenuti di terzi mantengono le rispettive licenze; vedi [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
