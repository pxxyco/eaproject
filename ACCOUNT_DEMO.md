# EA Project — Account demo e guida alle prove

Prima build ufficiale · **RTM 1.0.0**

## Credenziali di accesso

**Password per tutti gli account: `Demo2026!`**

| Nome utente | Ruolo |
|---|---|
| `calabria.tour` | Organizzatore |
| `sud.experience` | Organizzatore |
| `mario.rossi` | Viaggiatore |
| `giulia.bianchi` | Viaggiatore |

Gli account devono essere presenti nel database demo fornito dal gruppo: un database PostgreSQL nuovo e vuoto non li contiene automaticamente.

## Prove come Viaggiatore

Accedere con `mario.rossi` oppure `giulia.bianchi`.

1. **Esplorare il catalogo:** cercare un viaggio o un’attività e aprirne il dettaglio per consultare descrizione, foto, itinerario e recensioni disponibili.
2. **Prenotare:** scegliere un’esperienza pubblicata, con posti disponibili e data valida, quindi selezionare il numero di partecipanti e confermare.
3. **Provare il pagamento mock:** per una prenotazione a pagamento, completare il checkout. Su Android si può usare il popup subito dopo la prenotazione oppure riprendere il pagamento dalle prenotazioni o dal dettaglio dell’esperienza. Non vengono effettuati addebiti reali e non servono dati di carte reali.
4. **Scrivere una recensione:** dopo il pagamento, aprire le recensioni dell’esperienza e aggiungere una valutazione. Per un’esperienza gratuita è sufficiente la prenotazione confermata. Una recensione già presente può essere modificata.
5. **Usare i preferiti:** salvare un’esperienza e creare una lista personale.
6. **Provare la chat:** dal dettaglio scegliere “Contatta organizzatore” e inviare un messaggio. Su Android le conversazioni si trovano nella voce “Messaggi” della barra inferiore.
7. **Gestire il profilo:** consultare e modificare i dati del proprio account.
8. **Annullare una prenotazione:** utilizzare una prenotazione ancora annullabile e verificare il nuovo stato.

## Prove come Organizzatore

Accedere con `calabria.tour` oppure `sud.experience`.

1. **Consultare la dashboard:** verificare le proprie offerte e le prenotazioni ricevute.
2. **Creare un’offerta:** aggiungere un’attività o un viaggio, compilare i campi richiesti e pubblicarlo con data valida e posti disponibili.
3. **Modificare un’offerta:** aggiornare una propria attività o un proprio viaggio e verificare le modifiche dal catalogo.
4. **Consultare i partecipanti:** aprire le prenotazioni ricevute per le proprie esperienze.
5. **Rispondere in chat:** aprire i messaggi e rispondere al Viaggiatore.
6. **Consultare gli incassi mock:** verificare i pagamenti simulati delle proprie prenotazioni.
7. **Rispondere alle recensioni:** aprire una propria esperienza con una recensione e aggiungere una risposta.

## Prova consigliata con due account

Per verificare la comunicazione tra i client, usare **JavaFX e Android contemporaneamente**, connessi allo stesso backend:

1. Accedere come Organizzatore in un client e pubblicare un’esperienza riconoscibile, ad esempio “Prova docente”.
2. Accedere come Viaggiatore nell’altro client e cercare l’esperienza.
3. Prenotare e completare il pagamento mock, se previsto.
4. Controllare la prenotazione e l’incasso dal client dell’Organizzatore.
5. Dal client del Viaggiatore contattare l’Organizzatore, quindi scambiarsi alcuni messaggi per verificare l’aggiornamento in tempo reale.
6. Aggiungere una recensione come Viaggiatore e rispondere come Organizzatore.

## Prima della dimostrazione

- Avviare il backend e il database contenente gli account demo.
- Per Android in configurazione locale, collegare il dispositivo o avviare l’emulatore ed eseguire `adb reverse tcp:8443 tcp:8443`.
- Per il certificato locale fornito dal gruppo, usare la variante Android **debug**.
- Verificare che entrambi i client utilizzino lo stesso backend.
- Usare esperienze pubblicate e con date valide: i dati demo con date trascorse possono non essere più prenotabili.
- Le prove modificano i dati: prenotazioni, recensioni, messaggi e offerte rimangono salvati.

Istruzioni complete di avvio: [Guida all’avvio](docs/GETTING_STARTED.md).
