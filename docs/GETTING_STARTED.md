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

## Backend HTTPS locale

Il team condivide separatamente `keystore.p12` e `application-dev.properties`. Inseriscili in `ea-project/src/main/resources/`. Il certificato deve corrispondere alle CA di sviluppo già configurate nei client; creare un certificato diverso richiede aggiornare anche la fiducia dei client.

```bash
python3 scripts/setup-local-env.py
docker compose up -d --build
docker compose ps
```

Lo script genera credenziali casuali in `.env` senza sovrascrivere una configurazione esistente e copia il keystore in `certs/server.p12`. Verifica il servizio:

```bash
curl -k https://localhost:8443/api/health
```

`-k` serve solo alla verifica del certificato locale. Per usare un backend pubblico adotta un certificato attendibile, non disabilitare la verifica TLS.

In alternativa configura PostgreSQL e le variabili descritte in [CONFIGURATION.md](CONFIGURATION.md), poi:

```bash
./ea-project/mvnw -f ea-project/pom.xml spring-boot:run
```

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
