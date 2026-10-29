# ea_project_application – Client Android

Client mobile dell'applicazione, scritto in **Kotlin** con **Jetpack Compose** e Material 3.

## Requisiti

- Android Studio (versione recente, con Gradle wrapper incluso nel progetto)
- Emulatore o dispositivo con **Android 11 (API 30)** o superiore
- Backend avviato su `https://localhost:8443` (vedi il [README principale](../README.md))

## Avvio

1. Avvia il backend, con Docker o con `./mvnw spring-boot:run`.
2. Avvia l'emulatore.
3. Inoltra la porta dell'emulatore al PC:

   ```bash
   adb reverse tcp:8443 tcp:8443
   ```

   Va ripetuto a ogni riavvio dell'emulatore o di adb. Senza questo passaggio l'app mostra l'errore di connessione **ERR-101**.

4. Apri questa cartella in Android Studio ed esegui la configurazione `app` in variante **debug**.

Da riga di comando:

```bash
./gradlew :app:installDebug
```

## HTTPS

L'app si collega a `https://localhost:8443` (`network/ApiService.kt`).

Il backend usa un certificato self-signed. La variante **debug** si fida di quel certificato tramite `src/debug/res/xml/network_security_config.xml` e `src/debug/res/raw/ea_cert.cer`. Il certificato è valido per `localhost`, `127.0.0.1` e `10.0.2.2`. La variante release non si fida del certificato di sviluppo.

## Funzionalità

**Viaggiatore**
- Registrazione guidata e login, con refresh automatico del token
- Home con viaggi e attività, ricerca con suggerimenti, filtro per data, categorie e caricamento progressivo
- Raccomandazioni personalizzate
- Dettaglio di viaggi e attività: descrizione, itinerario con timeline delle tappe, galleria foto, recensioni
- Prenotazione e annullamento
- Recensioni (creazione, modifica, cancellazione)
- Preferiti e liste (creazione, rinomina, eliminazione, aggiunta e rimozione di elementi)
- Account: profilo, foto profilo, cambio password, carte salvate (anche lette con fotocamera o NFC), centro assistenza

**Organizzatore**
- Dashboard con offerte, prenotazioni ricevute e incassi
- Creazione e modifica di viaggi e attività, con tappe, coordinate, date e foto
- Risposte alle recensioni

## Funzioni disponibili solo nel client JavaFX

Pagamento mock delle prenotazioni, condivisione delle liste con utenti specifici, notifiche, chat, itinerari collaborativi, export `.ics`, mappa.

## Struttura del codice

```
com.example.application
├── network     ApiService (OkHttp + JWT), repository per ogni area
├── dto         Modelli delle risposte del backend
├── screens     Schermate Compose (home, login, dettaglio, preferiti, account, organizzatore)
├── device      Fotocamera, NFC, calendario, notifiche
├── service     Sfondo giornaliero
└── ui          Tema, colori, navigazione
```
