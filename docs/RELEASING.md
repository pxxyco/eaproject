# Preparare e pubblicare una release

La distribuzione comprende **due pacchetti integrati** e un archivio sorgenti dello stesso checkout:

| Asset | Contenuto |
|---|---|
| `EA-Project-VERSION-desktop-backend-OS-ARCH.zip` | JavaFX, librerie native per OS/architettura di build, backend JAR, Compose, sorgenti e guide |
| `EA-Project-VERSION-android-backend-demo.zip` oppure `...-release.zip` | APK, stesso backend, Compose, sorgenti e guide |
| `EA-Project-VERSION-source.zip` | Sorgenti e documentazione corrispondenti, senza directory Git o build |
| `SHA256SUMS.txt` | Impronte SHA-256 dei tre archivi |

`VERSION`, `OS` e `ARCH` nella tabella sono segnaposto. Il comando di packaging genera i nomi effettivi e un `release-manifest.json` nei due pacchetti, con commit, versione, variante e hash dei binari. Il runtime Java e le immagini Docker **non** sono inclusi automaticamente: servono Java 21 e Docker oppure la consegna offline opzionale descritta sotto.

## 1. Scegliere commit e versione

Lavora su un checkout pulito del commit approvato. Allinea `versionName` e `versionCode` Android, versioni dei moduli e note prima della pubblicazione. Un tag come `v1.0.0` identifica una release stabile; `v1.0.0-rc.1` una candidata. Non pubblicare un tag riutilizzato per binari diversi.

Lo script rifiuta modifiche non committate. `--allow-dirty` è disponibile soltanto per prove locali e marca i pacchetti come anteprima: non considerarle release definitive.

## 2. Backend senza configurazioni private

Dalla radice del checkout:

```bash
./ea-project/mvnw -B -f ea-project/pom.xml -Prelease-dist verify
```

Il JAR viene prodotto in `ea-project/target/release-dist/`, separato dalla build locale. Il profilo esclude `application-dev.properties`, configurazioni locali e keystore. Non include i file privati del team o il keystore del server: configura database e TLS esternamente. Lo script controlla i nomi di file privati nei JAR; questo non sostituisce una revisione dei contenuti da pubblicare.

## 3. JavaFX con librerie

Esegui la build sul sistema operativo e architettura per cui vuoi distribuire il desktop:

```bash
./ea-project/mvnw -B -f ea_project_javafx/pom.xml verify
./ea-project/mvnw -B -f ea_project_javafx/pom.xml dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/release-lib
```

Usa una directory `release-lib` nuova per ogni build/platform: non mescolare librerie native Windows, Linux e macOS. Il launcher incluso usa il JAR e la cartella `lib`, evitando di richiedere Maven all’utente finale. Serve Java 21; non si tratta di un installer nativo con runtime incorporato.

Per offrire più sistemi prepara un pacchetto distinto su ciascun sistema e ripeti il collaudo lì. Per eventuali installer con runtime incluso, configura separatamente `jpackage`, firma e requisiti del sistema; non rinominare lo ZIP Linux come pacchetto Windows.

## 4. Android: scegliere la variante

### Consegna dimostrativa locale

```bash
cd ea_project_application
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon --max-workers=1
cd ..
```

Questa variante è firmata con chiave debug e si fida della CA di sviluppo. Nel pacchetto verrà chiamata **demo**. Usa il backend del team e `adb reverse tcp:8443 tcp:8443`.

### Distribuzione release

Configura prima l’endpoint Android, attualmente `https://localhost:8443`, e una catena TLS attendibile per la variante release. Poi genera un APK firmato da Android Studio (**Build → Generate Signed App Bundle or APK**), conservando la chiave fuori dal repository. L’APK deve essere firmato con la chiave release; un AAB non è direttamente installabile e non è accettato dal packager.

Il progetto non ha una chiave release condivisa pubblicamente. `assembleRelease` senza configurazione di firma non produce da solo un APK distribuibile firmato. Per Play Store crea anche l’AAB firmato e allegalo separatamente se previsto dal team; lo ZIP integrato usa l’APK installabile.

Verifica la firma con `apksigner verify --print-certs percorso/app-release.apk`. Il packager esegue questa verifica quando scegli `--android-kind release` e rifiuta il certificato debug. Non distribuire keystore o password insieme all’APK.

## 5. Creare i pacchetti

Esempio per una consegna locale (versione da sostituire con quella approvata):

```bash
python3 scripts/package-release.py --version 1.0.0-rc.1 --android-kind demo --android-apk ea_project_application/app/build/outputs/apk/debug/app-debug.apk
```

Per un APK release firmato:

```bash
python3 scripts/package-release.py --version 1.0.0 --android-kind release --android-apk /percorso/app-release.apk --apksigner /percorso/android-sdk/build-tools/VERSION/apksigner
```

`--backend-jar`, `--desktop-jar` e `--desktop-lib` consentono di indicare percorsi diversi. L’output è in `dist/VERSION/`, ignorato da Git. Gli artefatti esistenti non vengono sovrascritti.

Ogni ZIP integrato contiene sorgenti e guide, `backend/app.jar` e i file del client sotto `client/`. **START_HERE.md** spiega l’avvio. Dalla radice del pacchetto si può buildare il backend con il Compose incluso; keystore e credenziali restano esterni. Con `java -jar backend/app.jar` occorre esportare le variabili documentate: Java non legge `.env` automaticamente.

Controlla gli hash prima e dopo il trasferimento:

```bash
cd dist/1.0.0-rc.1
sha256sum -c SHA256SUMS.txt
```

Su macOS usa `shasum -a 256 -c SHA256SUMS.txt`; su Windows puoi confrontare `Get-FileHash -Algorithm SHA256` con il file pubblicato.

## 6. Collaudo della distribuzione

Estrai ogni ZIP in una cartella nuova e segui START_HERE, senza dipendere dalle directory di build del computer dello sviluppatore. Verifica il [collaudo funzionale](TESTING.md), la piattaforma desktop indicata, l’APK scelto e la persistenza dopo un riavvio. Rivedi [dipendenze e asset](../THIRD_PARTY_NOTICES.md). Non dichiarare prove su piattaforme o hardware che non hai eseguito.

## 7. GitHub Release

1. Integra i file approvati, esegui i controlli e individua il commit finale.
2. Vai in **Releases → Draft a new release**.
3. Crea/seleziona il tag scelto e verifica il commit/ramo di destinazione.
4. Compila il [modello di note](RELEASE_NOTES_TEMPLATE.md), includendo limiti, configurazione e collaudi effettivi.
5. Allega i due ZIP integrati, il sorgente e SHA256SUMS. Gli archivi “Source code” generati da GitHub non contengono automaticamente JAR o APK.
6. Per una candidata abilita **pre-release**. Salva come bozza e controlla i download prima di pubblicare.
7. Pubblica soltanto dopo la revisione finale del team.

Non caricare `.env`, keystore TLS/firma, database personali o log. La CI inclusa non esegue questo passaggio di pubblicazione.

## Consegna offline opzionale

Dopo una build Docker verificata puoi esportare le immagini già disponibili, per esempio:

```bash
docker image save ea-project-backend:rc postgres:17-alpine -o ea-project-images.tar
```

Allega il TAR separatamente, indicandone piattaforma, tag e checksum. Il Compose corrente usa il tag backend `rc`: se scegli un tag diverso, adatta anche il Compose consegnato e prova `docker load` su una macchina pulita. Un’immagine PostgreSQL standard non contiene i dati del database; eventuali seed demo autorizzati richiedono una consegna distinta e documentata. Non esportare il database reale come immagine demo.

[Procedura ufficiale GitHub](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository).
