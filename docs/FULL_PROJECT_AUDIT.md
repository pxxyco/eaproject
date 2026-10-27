# EA Project — ricontrollo completo della candidata

Data: 27 ottobre 2026. Ambito: backend Spring, client JavaFX, risorse, configurazioni,
script e Docker. Android resta escluso come concordato. Nessun commit o push eseguito.

## Esito e significato del controllo

Le verifiche eseguite sono superate dopo le correzioni riportate sotto. Questo rende la
candidata più solida per il collaudo del gruppo, ma non certifica che ogni comportamento
possibile sia privo di bug. La scansione automatica riguarda tutti i file sorgente del
perimetro; l'analisi manuale e le prove funzionali approfondiscono i flussi indicati.
Il confronto con requisiti e bonus della traccia rimane in [RC_AUDIT.md](RC_AUDIT.md).

## Controlli su tutti i file

- **262 Java**, inclusi i test: parsing AST, corrispondenza package/percorso, import
  apparentemente inutilizzati e corpi di metodi duplicati normalizzati oltre 180 caratteri.
  Nessuna segnalazione finale. Questo controllo non dimostra l'assenza di duplicazioni
  concettuali o di import utilizzati soltanto per casi particolari.
- **41 FXML**: XML, risorse relative, inclusioni, identificativi duplicati, esistenza degli
  handler e compatibilità fra tipi dichiarati e campi dei controller.
- Tutti i **41 FXML** caricati e impaginati nel runtime JavaFX con CSS applicato.
  Per isolare il layout ed evitare chiamate di rete, nella copia temporanea usata per
  questa prova sono stati rimossi controller e handler; i loro collegamenti sono stati
  verificati separatamente tramite riflessione sui controller compilati.
- **26 CSS**: risorse locali e bilanciamento delle parentesi, oltre alla verifica JavaFX.
  Nessun errore CSS finale. Il runtime di prova Java 25 segnala gli avvisi della libreria
  JavaFX 21 relativi al caricamento dal classpath e all'accesso nativo; non sono errori CSS.
- **56 documenti XML/FXML/IML e 2 JSON** validati sintatticamente; script Python compilati.
- Controllati riferimenti letterali alle risorse Java, integrità PNG/JPEG, percorsi,
  marcatori di conflitto e differenze con spazi problematici.
- Il certificato CA pubblico del client risulta temporalmente valido alla data di verifica;
  questo non sostituisce il collaudo HTTPS del deployment.

L'inventario [FILE_AUDIT.json](FILE_AUDIT.json) elenca i file presenti riconosciuti da Git,
compresi quelli nuovi non ignorati, con dimensioni e SHA-256. Non contiene Android,
output compilati, archivi runtime o configurazioni personali ignorate. È una fotografia
dei file, non un punteggio di qualità né una certificazione manuale riga per riga.
Il file dell'inventario stesso è escluso dal proprio hash.

## Problemi ulteriori corretti

### Backend

1. **Recensioni concorrenti:** serializzate le operazioni sull'offerta per evitare
   recensioni duplicate dello stesso autore e medie incoerenti. Modifica/cancellazione
   bloccano anche la recensione; una recensione inesistente restituisce 404.
2. **Risposte Organizer:** rifiutati testi vuoti o oltre 255 caratteri; inserimenti
   simultanei producono una sola risposta e conflitto 409 per i duplicati. Una risposta
   la cui recensione è stata eliminata non è più modificabile.
3. **Fotografie:** controllo delle dimensioni prima della decodifica, limite di due
   elaborazioni contemporanee e normalizzazione JPEG. Limiti: 8000 pixel per lato,
   25 milioni di pixel totali, 10 MB per foto offerta, 5 MB per avatar; lato finale
   massimo rispettivamente 2400 e 1200 pixel. Da 1 a 10 foto per richiesta, massimo
   50 foto per offerta. Superamento della capacità di elaborazione: 429 con invito
   a riprovare; il limitatore HTTP mantiene il proprio contratto Retry-After.
4. **Coerenza database/file:** scrittura mediante file temporaneo e spostamento atomico
   quando supportato. Un rollback rimuove il nuovo file; una rimozione elimina il file
   precedente soltanto dopo commit. Gli avatar hanno URL immutabili con UUID: un
   aggiornamento fallito non sovrascrive più la foto precedente.
5. **Gallerie delle bozze:** l'elenco delle immagini è accessibile al proprietario;
   altri utenti ricevono 404. Gli URL delle foto di catalogo e degli avatar restano
   risorse pubbliche: il controllo sull'elenco non li rende allegati privati.
6. **Identità e maiuscole:** username/email nuovi o aggiornati vengono normalizzati
   con trim e minuscole, così i vincoli univoci del database impediscono nuove
   registrazioni concorrenti con sole differenze di maiuscole. Nessuna migrazione
   dei dati già presenti nel database di sviluppo è stata eseguita.

### JavaFX

1. La galleria azzera immagini e indice quando viene riutilizzata e nasconde il comando
   per altri elementi al termine. Verificati 41 elementi in blocchi 20/20/1, poi reset
   a due immagini e a una lista vuota.
2. Le fotografie a schermo intero vengono caricate in background con dimensioni limitate,
   evitando la decodifica sincrona a risoluzione originale sul thread dell'interfaccia.
3. Lo sfondo di ripiego usa il JPEG già presente; rimosso l'AVIF non supportato da JavaFX.
   Aggiunti timeout di connessione/lettura per il servizio degli sfondi e recupero dei
   file di cache vuoti.
4. Definito il colore accent predefinito anche per le schermate aperte prima
   dell'applicazione della palette dinamica, eliminando un avviso CSS reale.

## Secondo passaggio richiesto: priorità Bing e revisione trasversale

- Eliminata la visualizzazione anticipata del fallback in `BackgroundHelper`.
  Lo sfondo viene preparato durante lo splash in parallelo al controllo del backend;
  la home viene aperta quando l'immagine scelta è pronta, senza bloccare il thread grafico.
- Ordine: **cache Bing valida del giorno → massimo tre tentativi Bing → fallback locale**.
  Tutte le viste condividono lo stesso caricamento del giorno, evitando richieste duplicate.
  Timeout per connessione/lettura: rispettivamente 2 e 3 secondi. Si tratta di timeout
  delle operazioni di rete, non di una durata totale garantita dell'avvio.
- Risposte vuote, JSON malformato, file vuoti o danneggiati contano come tentativi falliti.
  Download limitato a 8 MB e immagini a 16 milioni di pixel; file temporanei eliminati
  anche in caso di errore. Rimossa la dipendenza da una seconda immagine remota come
  ultimo fallback: il ripiego è una risorsa locale inclusa nell'applicazione.
- Upload multipart del client trasmessi mediante publisher di file: non viene più
  costruita in RAM una copia dell'intera richiesta con tutte le foto. Il contratto
  multipart e la sua lunghezza sono verificati da test; la costruzione della richiesta
  è provata anche con un file sparso da 128 MB, senza inviarlo né leggerlo integralmente.
  I limiti di accettazione del server rimangono quelli già indicati.
- Corretto `Retry-After` quando il decoder delle foto è occupato: 2 secondi invece dei
  900 suggeriti in precedenza per foto offerta/profilo. Per il limite delle connessioni
  chat restano 30 secondi e per il blocco dei login 900. Aggiunto `Cache-Control: no-store`.
- Corretto anche il parametro della richiesta di controllo email nello script di collaudo: ora usa
  `email` e verifica gli stati 200/404 prima del 429, evitando richieste errate con stato 400.
- Ripetute scansione AST di tutti i 262 Java, validazione risorse/XML/JSON/CSS/immagini,
  controllo dei collegamenti FXML/controller e caricamento dei 41 layout.
- Prova JavaFX con rete simulata: nessuna immagine di fallback prima dell'esito Bing;
  primo download riuscito scelto correttamente; tre errori consecutivi selezionano il
  fallback. Il test non certifica la disponibilità attuale del servizio Bing su ogni rete.
- Alcune regole duplicate già presenti in `SecurityConfig` sono state individuate ma
  lasciate intatte. Il controllo automatico di approvazione ha rifiutato la loro rimozione
  perché interpretata come possibile indebolimento dei permessi. Nessuna regola è stata
  rimossa né è stato aggirato il blocco; la configurazione esistente supera i test di ruolo.

## Prove funzionali e build

| Verifica | Risultato |
| --- | --- |
| Maven backend | **55 test, 0 errori, 0 fallimenti, 0 saltati** |
| Maven JavaFX | **6 test, 0 errori/fallimenti**, build riuscita |
| FXML/controller | 41 contratti coerenti |
| Layout e CSS JavaFX | 41 caricamenti riusciti |
| Galleria riutilizzata | Paginazione e reset superati nel runtime JavaFX |
| Dockerfile | Immagine ricostruita con il codice aggiornato, Java 21 |
| Compose | Backend e PostgreSQL 17 avviati e healthy |
| HTTP su PostgreSQL reale | Script esteso superato |

La suite aggiuntiva verifica concorrenza delle recensioni, unicità delle risposte,
registrazioni con varianti di maiuscole, visibilità delle gallerie, immagini con header
eccessivi, normalizzazione e cleanup delle foto con commit/rollback.

Lo script `scripts/verify-backend.py` verifica health, Swagger, ruoli e proprietà,
foto offerta, sostituzione/rimozione avatar, bozze, prenotazione, calendario, pagamento
mock idempotente, statistiche, raccomandazioni, collaborazione con versioni obsolete,
chat SSE realmente consegnata, notifiche, storico, revoca JWT e rate limiting.

Le prove Docker usano un progetto e un database temporanei separati. I relativi container,
volumi e rete sono stati rimossi; resta l'immagine `ea-project-backend:rc`.
Non sono stati modificati i dati di sviluppo né la configurazione Docker personale.

## Limiti e attività prima della release definitiva

- Android non analizzato; HTTPS pubblico resta da collaudare con chi se ne occupa.
- Non eseguiti penetration test completo, scansione CVE completa, prova di carico o
  collaudo manuale di ogni sequenza possibile su tutti i sistemi operativi.
- Le prove di concorrenza aggiunte sono nella suite di integrazione; lo smoke PostgreSQL
  verifica i flussi reali ma non è un test di carico concorrente prolungato.
- Il backend usa ancora `ddl-auto=update`: per dati reali servono migrazioni e backup.
- Rate limiting ed eventi in tempo reale restano legati alla singola istanza.
- Per la consegna rimangono validi gli elementi esplicitamente esclusi o non completati
  nella tabella bonus del rapporto RC; non vengono conteggiati come bonus presenti.
- Fare l'ultimo collaudo manuale con i quattro membri del gruppo e pubblicare le modifiche
  approvate prima di distribuire la release.
