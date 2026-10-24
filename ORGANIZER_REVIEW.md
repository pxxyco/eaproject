# Verifica dashboard organizzatore - 24 ottobre 2026

## Esito

Sono stati corretti problemi di logica e di integrazione nelle dashboard Android e JavaFX e nei servizi condivisi che usano. Non è corretto considerare già completo l'intero percorso Android viaggiatore -> prenotazione -> organizzatore: alcune schermate del viaggiatore sono ancora dimostrative.

La revisione confronta il codice con il documento `Progetto formativo 2025_2026.pdf`: per l'organizzatore sono richieste creazione, modifica, cancellazione di viaggi/attività e visualizzazione delle prenotazioni ricevute. Pagamenti, foto, mappe e itinerari sono funzionalità aggiuntive. Il PDF è stato usato come specifica del progetto.

## Problemi corretti

| Area | Problema rilevato | Correzione |
| --- | --- | --- |
| Posti | La modifica desktop conservava i vecchi posti disponibili anche cambiando la capienza. Entrambi i client potevano inviare disponibilità non aggiornata rispetto a prenotazioni recenti. | Il backend ricalcola la disponibilità dai valori persistiti; impedisce capienze inferiori ai posti occupati. Il desktop anticipa la stessa verifica. |
| Concorrenza | Prenotazioni simultanee e aggiornamenti dell'organizzatore potevano sovrascrivere i conteggi. | Lock sulle offerte durante prenotazione, modifica e cancellazione; lock sulla prenotazione in annullamento. L'annullamento ripetuto non restituisce due volte i posti. |
| Prenotabilità | La creazione di prenotazioni non controllava lo stato dell'offerta. | Viaggi in bozza e offerte annullate/concluse non accettano nuove prenotazioni. |
| Storico | Eliminare logicamente un'offerta la faceva scomparire dalle prenotazioni dell'organizzatore e dal totale dei pagamenti. | Lo storico delle prenotazioni e i pagamenti rimangono consultabili anche quando l'offerta è stata eliminata. Le prenotazioni eliminate logicamente e i pagamenti non completati restano esclusi dalle rispettive query. |
| Incassi | Il registro desktop sommava i prezzi delle prenotazioni, inclusi importi non pagati. | Registro e dashboard interrogano lo stesso servizio dei pagamenti completati/confermati. Il totale viene ricaricato alla chiusura del modulo offerta. |
| Errori desktop | Le risposte HTTP diverse da successo, refresh e rate limit non chiamavano il gestore di errore. Un salvataggio rifiutato poteva lasciare il pulsante disabilitato senza spiegazione. | Gli errori HTTP attivano il gestore locale; aggiunto il comando Aggiorna nella dashboard. |
| Offerte desktop | La voce della barra Offerte ed Eventi non caricava dati; il suo modulo sovrapposto poteva chiudere la finestra principale al salvataggio. | La voce riusa la dashboard e il suo flusso CRUD in finestra modale. Rimossi i filtri che non erano collegati ad alcun dato. |
| Tappe | I DTO dei viaggi restituivano sempre un elenco vuoto; il modulo desktop non caricava le tappe esistenti e il backend ignorava le modifiche all'itinerario. | I DTO includono le tappe ordinate; il modulo le carica e conserva gli attributi non modificabili. L'aggiornamento distingue itinerario omesso, modificato o svuotato e rifiuta ID di tappe estranee/duplicati. Android omette le tappe quando modifica solo i dati del viaggio. |
| Date Android | L'accesso diretto a stringhe JSON poteva fallire con le date restituite da Jackson come array numerici. | Lettura compatibile con stringhe ISO e array; rimozione dei campi di sola lettura dal payload di modifica; gestione delle risposte JSON invalide. Anche i dettagli desktop usano il lettore condiviso. |
| Dati delle attività | Modificare da desktop un'attività creata altrove azzerava gli orari e poteva cancellare le coordinate. | Conservazione di orari e coordinate già presenti. Allineati prezzo gratuito e descrizione facoltativa fra i client. |
| Validazione server | I controlli dei moduli non proteggevano le API da titoli, date, prezzi o capienze non validi. | Validazione condivisa nel backend, indipendente dal client utilizzato. |
| Accesso ai dati | L'elenco completo dei viaggi di un organizzatore era pubblico; l'aggiunta di tappe e la cancellazione di foto non controllavano il proprietario. | Controllo di ruolo e proprietario sulle operazioni; gli elenchi gestionali non sono consultabili da altri organizzatori. |
| Foto | Le anteprime locali venivano trattate come percorsi HTTP; annullare la selezione poteva perdere la selezione precedente; eliminare la copertina lasciava un URL obsoleto. | Corrette le anteprime locali, preservata la selezione annullata, riassegnata la copertina alla foto successiva. In un upload parziale il nuovo tentativo non reinvia la copertina già caricata con successo. |
| Archiviazione foto | La cartella usata per esporre le immagini ignorava la configurazione usata dal servizio di caricamento. | Entrambi usano app.storage.path; provato upload e successivo download tramite API. |
| Mappa | Tappe/attività senza coordinate potevano generare eccezioni nei dettagli. | Le coordinate facoltative vengono controllate prima di centrare la mappa o creare marcatori. |

## Verifiche eseguite

- Backend: `mvn -o -f ea-project/pom.xml test` -> **15 test passati**, nessun errore o fallimento.
- Fra questi: chiamate HTTP con JWT, isolamento fra proprietari/ruoli, creazione e modifica, prenotazione viaggiatore visibile all'organizzatore, cancellazione, incassi pagati contro non pagati, conservazione dello storico, CRUD delle tappe, proprietà delle foto, upload e download immagini, due prenotazioni contemporanee senza sovravendita.
- Test eseguiti su **H2 temporaneo**, in modalità compatibile con PostgreSQL; non sono stati creati utenti o offerte nel database di sviluppo. Questo non sostituisce una prova di concorrenza sul PostgreSQL effettivamente usato dal gruppo.
- Desktop: compilazione Maven riuscita. Caricate effettivamente tramite JavaFX le viste dashboard, creazione offerta, offerte/eventi, iscritti e contenitore dashboard; verificati anche caricamento delle tappe esistenti, conservazione dei loro attributi, ricalcolo posti e date ad array. Il controllo di caricamento non equivale a un collaudo manuale completo di tutti i dialoghi.
- Android: compilazione APK e test locali riusciti; **6 test mirati** sui dati organizzatore passati, oltre al test di esempio già esistente.
- Preparati e compilati 2 test dell'interfaccia Android: accesso diretto alla dashboard, posizione del menu a destra, passaggio alle impostazioni e ritorno; separazione dal profilo viaggiatore. **Esecuzione non conclusa**: l'emulatore è diventato offline durante l'installazione. Il messaggio finale BUILD SUCCESSFUL di Gradle non è stato considerato una prova di superamento di questi test.
- Corretti due problemi nei test preesistenti: chiamata attesa a un metodo di ricerca ormai sostituito e uso di un nanosecondo di scarto che veniva arrotondato alla precisione del database. Aggiunto H2 come dipendenza di test e configurazione separata dal database del gruppo.
- Controllo degli spazi e dei conflitti con `git diff --check`: superato.

## Integrazioni ancora incomplete

1. **Android Esplora e Viaggi.** HomeScreen contiene offerte dimostrative e callback vuote; MyTrip mostra un segnaposto. Le offerte create dall'organizzatore sono persistite e leggibili dalle API e dal desktop, ma non possono ancora comparire in un percorso Android viaggiatore completo. File: `ea_project_application/app/src/main/java/com/example/application/screens/HomeScreen.kt` e `MyTrip.kt`.
2. **Pagamento non completato come flusso applicativo.** BookingServiceImpl crea prenotazioni PENDING e PaymentController espone la lettura degli incassi; non è presente un percorso API/client che generi e confermi un pagamento. Gli incassi funzionano sui record di pagamento esistenti; una semplice prenotazione non deve aumentare il totale. Il PDF considera il pagamento opzionale.
3. **Parità delle funzioni aggiuntive su mobile.** Il modulo Android gestisce i dati principali delle offerte; non offre ancora caricamento foto o modifica delle tappe. Le immagini e le tappe inserite dal desktop vengono preservate. Queste funzioni non vanno considerate implementate su Android.
4. **Collaudo sul dispositivo da completare.** Riportare online l'emulatore e rieseguire i test di interfaccia; controllare inoltre il risultato visivo su telefono e su schermo ampio.

## Come provare le correzioni

Riavviare il backend e compilare/avviare i client aggiornati. Le correzioni ai servizi condivisi non entrano in funzione con il solo Run di Android. Per Android con il backend sul computer, ripristinare `adb reverse tcp:8080 tcp:8080` sul dispositivo selezionato dopo eventuali riavvii.
