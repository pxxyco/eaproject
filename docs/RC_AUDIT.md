# EA Project — verifica della release candidate

Data: 27 ottobre 2026. Riferimento: «Progetto formativo 2025/2026», gruppo di quattro persone.
Ambito: backend, frontend JavaFX, configurazione Docker e database. Android è escluso per
l'indicazione già concordata: la conformità dell'intera consegna resta quindi da chiudere su quel modulo.

## Esito

La candidata è utilizzabile per il collaudo del perimetro backend/JavaFX e dispone ora di un avvio
Docker riproducibile con PostgreSQL. Non va descritta come «tutti i bonus implementati» né come
consegna integralmente verificata: la tabella seguente distingue ciò che è presente, facoltativo o escluso.
I test non costituiscono una garanzia di assenza assoluta di bug o un penetration test completo.

## Requisiti della traccia

| Requisito | Esito ed evidenza |
| --- | --- |
| Ricerca viaggi e attività | Presente: ricerca, categorie, date, disponibilità e pagine nel backend e in JavaFX. |
| Prenotazione Traveler | Presente: controlli su ruolo, stato, date e posti; lock contro l'overselling; cancellazione idempotente. Suite di integrazione e collaudo HTTP. |
| Liste private | Presente: proprietario, raccolte nominate e preferiti rapidi; richieste di altri utenti negate. |
| Liste condivise | Presente: invito, accettazione, VIEW/EDIT e revoca; il ritorno a PRIVATE revoca gli accessi. |
| Creazione/modifica/cancellazione Organizer | Presente: viaggi, attività, tappe, immagini e disponibilità; isolamento fra organizzatori. |
| Prenotazioni ricevute | Presente: viste dell'Organizer e filtraggio lato server per proprietario. |
| REST con Spring | Presente: Spring Boot e controller REST. |
| JPA e database | Presente: entità, repository e transazioni; PostgreSQL reale nel collaudo Docker. |
| Autenticazione/autorizzazione | Presente: password BCrypt, JWT access/refresh distinti, scadenza, issuer, controlli di ruolo e proprietà, revoca mediante versione della sessione. |
| Android | Richiesto dalla traccia ma NON verificato in questo audit. Non modificato. |
| Dockerizzazione backend | Aggiunti Dockerfile a più stadi, Compose, profilo container, configurazione e controlli di salute. PostgreSQL usa la propria immagine ufficiale. |
| Repository GitHub | Remote configurato verso GitHub. Non è stato fatto un push né verificato online che tutte le modifiche locali siano pubblicate. |
| Sito web | Non obbligatorio per quattro persone; richiesto dalla traccia per gruppi di cinque/sei. Non implementato in questo intervento. |
| Link pubblici con capabilities | Non obbligatori per quattro persone; richiesti solo ai gruppi di sei. Non implementati. La vecchia visibilità PUBLIC non equivale a un capability link ed è protetta come privata; rimossa dalla scelta delle nuove raccolte JavaFX. |

## Bonus

| Bonus | Stato effettivo |
| --- | --- |
| Microservizi | Assente: backend monolitico modulare. I due container backend/database non costituiscono un'architettura a microservizi. |
| Audit logging | Presente: Log4j2, log applicativo/audit con rotazione, identificativo richiesta, eventi di login/logout, errori e rate limiting; volume persistente nel container. |
| Rate limiting | Presente: bucket limitati in memoria per peer, utente e famiglia di operazioni, prima e dopo JWT; 429 e Retry-After. Le intestazioni di forwarding non sono usate come identità attendibile. |
| Testing | Presente: suite Maven e script HTTP ripetibile su PostgreSQL. Non è stata eseguita una certificazione di carico o una scansione completa CVE. |
| Documentazione/Swagger | Presente: OpenAPI/Swagger con bearerAuth; aggiunti README, avvio Docker, limiti e rapporto di verifica. |
| HTTPS | Supporto già presente nel backend e validazione certificati nel client. Il Compose locale usa HTTP su localhost; per il deployment occorre il keystore reale del responsabile HTTPS. Non certificato come deployment pubblico HTTPS completato. |
| Authorization server/Keycloak | Non implementato per scelta concordata. L'autenticazione JWT applicativa soddisfa il requisito base, ma non viene conteggiata come bonus Keycloak/OAuth2 authorization server. |
| Itinerari collaborativi | Presente: tappe personali associate a una raccolta condivisa, permessi EDIT e conflitto 409 su versioni obsolete. Testati anche con due utenti sul database reale. |
| Raccomandazioni | Presente: segnali da prenotazioni, liste, recensioni e consenso alla personalizzazione; candidati limitati, diversità, filtro di esperienze già prenotate e non disponibili. Testato il contratto e il filtraggio. |
| Chat in tempo reale | Presente: SSE autenticato, eventi dopo commit, riconnessione, cronologia, notifiche, non letti, risposta, allegati privati e cancellazione personale. Verificata la consegna di un evento reale fra Traveler e Organizer. |

Audit e limitatori SSE/rate sono locali alla singola istanza: un deployment con più repliche richiederebbe
un coordinamento condiviso. La configurazione fornita avvia una singola istanza del backend.

## Funzioni ulteriori

- Recensioni e risposte Organizer: presenti con controlli di proprietà ed eleggibilità; non confondere
  la notifica in-app con un servizio email/push mobile.
- Notifiche in-app per prenotazioni, inviti e chat: presenti. Invio email, push Android e promemoria
  programmati non sono stati certificati né aggiunti in questo audit; sono idee facoltative della traccia.
- Pagamenti mock: presenti e protetti da retry concorrenti. Nessun circuito bancario o bonifico di payout.
- Calendari `.ics`: esportazione personale della prenotazione e del viaggio; UTF-8, escaping,
  fine esclusiva dei giorni interi e folding delle righe. Corretto un vecchio endpoint ancora incompleto.
- Tappe personalizzate, mappe e fotografie: presenti. Le tile della mappa dipendono dalla rete;
  il precaricamento non rende istantaneo un primo download senza connessione.

## Problemi trovati e corretti in questa verifica

1. `/api/health` era autorizzato ma non implementato; il client considerava raggiungibile anche una
   risposta HTTP di errore. Ora il controllo verifica il database, risponde UP/200 o DOWN/503 e il
   client accetta soltanto 200.
2. `/api/v1/trips/{id}/export/ics` richiamava un metodo non implementato. Ora esporta davvero e
   impedisce la lettura di bozze altrui. La scrittura del calendario è condivisa con l'esportazione
   della prenotazione e rispetta i limiti delle righe UTF-8.
3. Un arresto durante la copia della foto predefinita poteva lasciare un file vuoto, mai riparato
   ai riavvii successivi. Ora la copia è atomica e un file vuoto viene rigenerato; aggiunto un test.
4. Il salvataggio delle recensioni poteva fallire senza feedback e modificava anticipatamente il DTO
   della recensione mostrata. Ora segnala errore/valutazione mancante, riabilita il retry e conserva
   il DTO originale se il server rifiuta la modifica; verificato con un test JavaFX a risposta 400.
5. JavaFX offriva la voce «Tutti» per PUBLIC senza un corrispondente link pubblico funzionante.
   Le nuove raccolte offrono soltanto le due modalità effettivamente supportate: privata e condivisa.

Il successivo ricontrollo completo, le correzioni aggiuntive e i limiti delle verifiche sono descritti in
[FULL_PROJECT_AUDIT.md](FULL_PROJECT_AUDIT.md).

## Evidenze del collaudo

- Suite Maven backend: **55 test, zero errori/fallimenti**.
- Compilazione frontend JavaFX riuscita; controllo sintattico di tutti i **41 FXML**.
- Build reale del Dockerfile con Java 21, avvio con database PostgreSQL 17, backend non root,
  filesystem in sola lettura e controlli di salute. Dati di prova isolati dal database di sviluppo.
- Script `scripts/verify-backend.py`: ruoli, proprietà, prenotazione, calendari, pagamento mock
  idempotente, statistiche, raccomandazioni, itinerario condiviso e versioni obsolete, chat SSE,
  notifiche, query dello storico su PostgreSQL, logout/revoca e rate limiting.
- Prova di ripristino: health 503 a database fermo e 200 alla ripartenza, dati conservati;
  foto vuota rigenerata dopo ricreazione del backend.
- JAR estratto dall’immagine finale verificato: nessun `application-dev.properties` o keystore privato.
- I container e volumi temporanei di audit vengono rimossi a fine collaudo; l’immagine RC resta disponibile.

## Prima della consegna finale

Verificare Android, collaudare il certificato e il nome host HTTPS del deployment, pubblicare le
modifiche approvate su GitHub e fare una prova manuale finale sui computer dei quattro componenti.
Per dati reali definire backup/ripristino e migrazioni versionate: la candidata usa ancora Hibernate
`ddl-auto=update`. Il volume Docker conserva i dati ma non sostituisce un backup.
Le configurazioni personali di sviluppo sono escluse dall'immagine Docker: distribuire l'immagine
verificata, evitando di distribuire un JAR costruito localmente con risorse personali non controllate.
