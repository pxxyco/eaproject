# Configurazione

## Backend

| Variabile | Significato |
|---|---|
| `DB_URL` | URL JDBC PostgreSQL; Compose lo imposta verso il servizio `db` |
| `DB_USER`, `DB_PSW` | Credenziali lette da Spring |
| `DB_NAME`, `DB_PASSWORD` | Nome e password usati dal file Compose; `DB_PASSWORD` viene passato come `DB_PSW` |
| `DB_IMAGE` | Immagine PostgreSQL opzionale; default `postgres:17-alpine` |
| `JWT_SECRET` | Segreto HMAC Base64, generato localmente; non condividere tra installazioni |
| `SERVER_SSL_ENABLED` | HTTPS; nel setup Compose locale è attivo |
| `SSL_KEYSTORE`, `SSL_KEYSTORE_PASSWORD` | Percorso e password del keystore TLS |
| `SERVER_PORT` | Porta del processo Java, default 8443; il profilo container usa 8080 |
| `BACKEND_HOST`, `BACKEND_PORT` | Bind e porta esposta da Compose; default loopback e 8443 |
| `LOG_DIR` | Directory di log; Compose usa `/app/logs` |
| `DB_POOL_SIZE` | Dimensione del pool nel profilo container, default 10 |

[.env.example](../.env.example) contiene i nomi senza segreti. Compose legge `.env`; un normale `java -jar` non carica automaticamente quel file. Esporta esplicitamente le variabili quando avvii il JAR senza Docker.

Il profilo Maven `release-dist` produce il backend sotto `ea-project/target/release-dist/` escludendo configurazioni private e keystore. In quella distribuzione TLS e database vanno configurati esternamente. La build locale ordinaria resta compatibile con i file privati del team.

## JavaFX

`EA_PROJECT_API_URL` o la proprietà JVM `eaproject.api.url` scelgono l’endpoint. La CA di sviluppo è in `security/TlsSupport` e nelle risorse `certs/dev-ca.crt`. Il client non deve ignorare errori TLS.

## Android

L’endpoint è definito in `network/ApiService.kt`; attualmente è `https://localhost:8443`. Per una distribuzione autonoma cambia l’endpoint nel codice e ricompila. `adb reverse` è un ponte di sviluppo, non una configurazione per utenti finali.

La variante debug include la fiducia per il certificato locale; la variante release usa la configurazione di rete principale e non eredita quella debug. Prima di distribuire una release verifica un backend raggiungibile, hostname del certificato, catena di fiducia e assenza di HTTP non necessario.

Il progetto non contiene una chiave di firma Android release né una configurazione `signingConfig` pubblica. Firma il pacchetto in Android Studio con **Generate Signed App Bundle or APK**, custodisci la chiave fuori dal repository e mantienila per gli aggiornamenti futuri. Incrementa `versionCode` e allinea `versionName` per ogni aggiornamento distribuito.

## Dati locali esclusi

Non versionare `.env`, `application-dev.properties`, chiavi private, keystore di firma, database, esportazioni con utenti reali, log con token o foto personali. I certificati pubblici dei client non sono chiavi private.
