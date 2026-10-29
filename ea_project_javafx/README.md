# ea_project_javafx – Client desktop

Client desktop dell'applicazione, scritto in **Java 21** con **JavaFX 21** (FXML + CSS).

## Requisiti

- JDK 21
- Backend avviato su `https://localhost:8443` (vedi il [README principale](../README.md))

## Avvio

Il progetto non ha un proprio Maven wrapper; si usa quello del backend. Dalla cartella principale del repository:

```bash
./ea-project/mvnw -f ea_project_javafx/pom.xml javafx:run
```

In alternativa, apri la cartella in IntelliJ IDEA ed esegui `it.unical.ea_project_javafx.App`.

## Indirizzo del backend

Per default il client usa `https://localhost:8443`. Per cambiarlo:

- proprietà di sistema `-Deaproject.api.url=https://...`
- oppure variabile d'ambiente `EA_PROJECT_API_URL`

Il certificato self-signed del backend è incluso come `src/main/resources/it/unical/ea_project_javafx/certs/dev-ca.crt` e caricato da `security/TlsSupport`.

## Funzionalità

**Viaggiatore**
- Login e registrazione, profilo con foto
- Home con ricerca, filtri, categorie e raccomandazioni
- Dettaglio di viaggi e attività: descrizione, itinerario con **mappa** delle tappe, galleria foto, recensioni
- Prenotazione, **pagamento mock** con carte salvate, storico, annullamento, export **`.ics`**
- Preferiti e liste: creazione, modifica, **condivisione con utenti specifici** (lettura o modifica), inviti tramite notifica
- **Itinerari collaborativi** sulle liste condivise
- **Notifiche** e **chat** in tempo reale con l'organizzatore

**Organizzatore**
- Dashboard con offerte, prenotazioni ricevute, incassi e statistiche
- Creazione, modifica ed eliminazione di viaggi e attività, con tappe, coordinate e foto
- Risposte alle recensioni, chat con i viaggiatori

## Struttura del codice

```
it.unical.ea_project_javafx
├── App.java      Punto di ingresso
├── controller    Controller FXML (home, login, attività, organizzatore, profilo, chat, ...)
├── service       Chiamate al backend (api), chat, preferiti, notifiche, recensioni
├── dto           Modelli delle risposte del backend
├── navigation    Cambio di schermata
├── security      TLS e sessione
├── ui            Componenti grafici (mappa, grafici, dialoghi, animazioni, pagamento)
└── util          Utility varie
```

FXML e CSS si trovano in `src/main/resources/it/unical/ea_project_javafx/`.
