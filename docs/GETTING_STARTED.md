# Primi passi

## Requisiti

- Git, JDK 21 e Python 3. I progetti Maven usano Java 21; Gradle richiede la toolchain 21 configurata dal progetto.
- Docker Engine/Desktop con il plugin Compose, oppure PostgreSQL locale.
- Android Studio con Android SDK 37 per compilare; dispositivo Android 11/API 30 o successivo.
- Per JavaFX, un ambiente grafico e le librerie native del sistema. Il pacchetto desktop va preparato sul sistema operativo e architettura di destinazione.

```bash
git clone https://github.com/pxxyco/untitled_ea_project.git
cd untitled_ea_project
```

## Avvio con Docker

Il backend in Docker risponde su **https://localhost:8443** con lo stesso certificato di sviluppo di cui si fidano i client Android debug e JavaFX. Esegui i comandi dalla cartella principale del progetto, dove si trova `compose.yaml`.

### Con il pacchetto Docker RTM

La prima build ufficiale è **RTM 1.0.0**, identificata dal tag Git `v1.0.0`. Il pacchetto `ea-project-images-1.0.0.tar` va generato dal codice RTM e allegato alla release come descritto in [RELEASING.md](RELEASING.md#consegna-offline-con-le-immagini-docker).

| Immagine | Uso |
|---|---|
| `ea-project-backend:1.0.0` | Backend della prima build ufficiale |
| `postgres:17-alpine` | PostgreSQL standard, con database vuoto su un volume nuovo |

Per avviare queste immagini sul server servono Docker/Compose e i file della consegna, senza JDK, Maven o Python sul computer host. La piattaforma dell’archivio deve corrispondere a quella dichiarata nelle note della release. Il TAR è escluso da Git e viene consegnato separatamente dal codice.

Disponi i file così:

```text
untitled_ea_project/
├── compose.yaml
├── ea-project-images-1.0.0.tar
├── .env
└── certs/
    └── server.p12
```

Nel `.env` della consegna configura i riferimenti RTM, conservando le credenziali del database e del keystore:

```dotenv
BACKEND_IMAGE=ea-project-backend:1.0.0
DB_IMAGE=postgres:17-alpine
```

Importa le immagini e avvia i servizi:

```bash
docker load -i ea-project-images-1.0.0.tar
docker compose up -d --no-build --pull never
docker compose ps
```

`--no-build --pull never` usa soltanto le immagini caricate. Se un tag richiesto manca, l’avvio fallisce invece di compilare o scaricare un’altra immagine. Il comando `docker load` importa le immagini, ma non configura certificati, credenziali o volumi esistenti.

Il database standard non contiene gli account e i dati dimostrativi del team. Se ricevi una consegna con un’immagine demo dedicata, imposta `DB_IMAGE` al tag esatto dichiarato in quella consegna e importa anche quell’immagine; non sostituire automaticamente il dataset con uno nuovo.

Un volume `database` già inizializzato conserva i dati: cambiare l’immagine o importare un TAR non li ricrea. Per provare una nuova installazione senza usare il volume esistente puoi usare un progetto separato, ad esempio `docker compose -p ea-project-rtm-test up -d --no-build --pull never`, dopo aver liberato la porta 8443.

### Passaggio dalla precedente consegna

Le immagini con tag `:rc` appartengono alla precedente consegna e non identificano la RTM. Il backend ufficiale va ricostruito dal codice 1.0.0; cambiare soltanto il tag della vecchia immagine non aggiorna il software. I vecchi TAR non diventano pacchetti RTM rinominando il file.

Se riusi un `.env` esistente, verifica `BACKEND_IMAGE` e `DB_IMAGE`: lo script di setup non sovrascrive i valori locali. Per il dataset demo conserva l’immagine prevista dal team; prima di cambiare installazione salva database e media secondo [OPERATIONS.md](OPERATIONS.md).

### Dal codice

1. Copia i file del team `keystore.p12` e `application-dev.properties` in `ea-project/src/main/resources/`. Il certificato deve corrispondere alle CA di sviluppo dei client; un certificato diverso richiede aggiornare anche la loro fiducia.
2. Crea la configurazione locale e copia il keystore per Docker:

   ```bash
   python3 scripts/setup-local-env.py
   ```

   Lo script genera credenziali casuali per database e JWT in `.env`, copia il keystore in `certs/server.p12` e legge la password da `application-dev.properties`. Se `.env` esiste già, non lo sovrascrive. Questa procedura serve a una nuova configurazione locale; quando riusi una consegna o un database esistente conserva le relative credenziali.
3. Avvia database e backend:

   ```bash
   docker compose up -d --build
   docker compose ps
   ```

   In una configurazione locale nuova il database standard è `postgres:17-alpine`. Se `.env` seleziona un’immagine demo dedicata, carica prima il TAR corrispondente per renderla disponibile; `--build` ricompila il backend, non ricrea i dati PostgreSQL.

### Verifica e arresto

Controlla che il backend risponda (`-k` accetta il certificato self-signed soltanto per questa verifica locale):

```bash
curl -k https://localhost:8443/api/health
```

Per i log:

```bash
docker compose logs --tail=100 backend
docker compose logs --tail=100 db
```

Ferma i servizi mantenendo i dati:

```bash
docker compose down
```

Per un reset volontario completo, `docker compose down -v` elimina anche i volumi con database, media e log. Eseguilo soltanto se vuoi cancellare quei dati. Se hai usato un nome progetto con `-p`, usa lo stesso nome anche nei comandi di controllo e arresto.

## Avvio del backend senza Docker

Servono JDK 21 e un PostgreSQL locale con i dati indicati in `application-dev.properties`, oltre al keystore TLS del team. In alternativa esporta le variabili descritte in [CONFIGURATION.md](CONFIGURATION.md).

Dalla cartella principale:

```bash
cd ea-project
./mvnw spring-boot:run
```

Il backend parte su **https://localhost:8443**. Vedi anche [la guida del backend](../ea-project/README.md). Per un backend pubblico configura un certificato attendibile senza disabilitare la verifica TLS nei client.

## JavaFX

```bash
./ea-project/mvnw -f ea_project_javafx/pom.xml javafx:run
```

Il client usa `https://localhost:8443`. La variabile `EA_PROJECT_API_URL` permette di selezionare un backend diverso. Occorrono comunque un certificato attendibile e una versione API compatibile.

## Android

Apri `ea_project_application` in Android Studio, avvia un emulatore e seleziona la variante `debug`. Collega la porta al computer:

```bash
adb reverse tcp:8443 tcp:8443
cd ea_project_application
./gradlew :app:installDebug
```

Ripeti `adb reverse` dopo il riavvio del dispositivo/ADB. Funziona anche con un telefono collegato via USB con debug autorizzato. Per l’uso autonomo sul telefono serve un endpoint di rete raggiungibile: `localhost` sul telefono non è il PC.

## Dati iniziali

Un database nuovo non equivale al database dimostrativo del team. Registrati oppure usa il dataset demo fornito separatamente. Gli account descritti nel [manuale](USER_GUIDE.md) funzionano soltanto se quel dataset è stato caricato.
