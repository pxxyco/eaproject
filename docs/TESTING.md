# Test e collaudo

Dalla radice della repository:

```bash
./ea-project/mvnw -B -f ea-project/pom.xml test
./ea-project/mvnw -B -f ea_project_javafx/pom.xml test
cd ea_project_application
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon --max-workers=1
```

I test backend usano H2 e configurazione di test, non il PostgreSQL reale. I test JavaFX comprendono contratti e architettura degli stili; non sostituiscono il controllo grafico. I test JVM Android non eseguono la fotocamera fisica.

Con un dispositivo o emulatore avviato:

```bash
cd ea_project_application
./gradlew :app:connectedDebugAndroidTest --no-daemon --max-workers=1
```

Non avviare più emulatori o build pesanti insieme su un computer con poca RAM. I report sono in `ea-project/target/surefire-reports`, `ea_project_javafx/target/surefire-reports` e `ea_project_application/app/build/reports`.

## Collaudo prima della release

- Registrazione, accesso, refresh e logout con entrambi i ruoli.
- Ricerca, apertura scheda e ritorno alla pagina precedente.
- Prenotazione multiposto, richieste concorrenti e rifiuto delle copie per la stessa occorrenza.
- Pagamento mock ripetuto senza doppio addebito logico, annullamento e ripristino dei posti una sola volta.
- Recensione dopo pagamento, gratuita confermata, rifiuto per prenotazione annullata e modifica della recensione personale.
- Messaggio nuovo/risposta, lettura, eliminazione personale e indicatore non letto.
- Proprietà e permessi delle raccolte e delle offerte: provare anche un altro utente.
- Android Light/Dark, caratteri grandi, rotazione, scelta foto, permesso camera negato/concesso, scatto, annullamento e upload.
- Avvio dei pacchetti su una macchina pulita, collegamento al backend e riavvio con dati persistenti.

La CI compila e testa i moduli senza credenziali di sviluppo. La pubblicazione è un passaggio separato e manuale. I numeri dei test vanno riportati nelle note della release effettiva, non fissati nel README.

## Smoke test delle API sulla configurazione RTM

Lo script usa per default `https://localhost:8443` e verifica il certificato con le CA di sistema più il certificato pubblico di sviluppo incluso. Il parametro `--ca-file` permette di indicare un certificato aggiuntivo diverso; non viene disabilitata la verifica TLS.

Su un’installazione di collaudo con database dedicato:

```bash
python3 scripts/verify-backend.py --url https://localhost:8443 --allow-write
```

Lo script crea utenti e offerte di prova RTM. Non eseguirlo sul database reale della consegna. L’eventuale uso di HTTP con `--url` serve soltanto a un backend di test esplicitamente configurato per HTTP.
