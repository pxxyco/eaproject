# FAQ

### Il backend non parte: cosa manca?

Controlla variabili database, `JWT_SECRET`, keystore e password TLS. In Docker consulta `docker compose ps` e `docker compose logs --tail=100 backend`. La build per la distribuzione non contiene la configurazione privata del team.

### Android mostra ERR-101 o non raggiunge il server

Avvia il backend, verifica l’emulatore e ripeti `adb reverse tcp:8443 tcp:8443`. Su un telefono `localhost` indica il telefono: per usare il PC serve il ponte USB oppure un endpoint di rete configurato nell’app.

### Perché debug si collega e release no?

La fiducia per il certificato locale è nella variante debug. La release richiede una catena TLS attendibile e un endpoint appropriato. Non copiare alla cieca la configurazione debug e non disabilitare TLS.

### Posso recensire subito dopo aver pagato?

Sì, con prenotazione non annullata. Per una gratuita basta la conferma. Una recensione già presente si modifica. Sono ammesse anche le partecipazioni già concluse e non annullate.

### Perché non posso prenotare due volte la stessa attività?

Per la stessa occorrenza è ammessa una sola prenotazione attiva: indica più posti nella stessa prenotazione. Un’occorrenza in una data diversa è distinta. Se annulli entro le 23:59 del giorno di inizio, puoi riprenotare se restano posti e l’offerta è ancora prenotabile.

### Pagamento e conclusione sono la stessa cosa?

No. Il pagamento mock conferma la prenotazione; “Conclusa” riguarda l’esperienza. Una prenotazione non pagata risulta scaduta dopo le 23:59 del giorno di inizio.

### Perché vedo ancora vecchie prenotazioni duplicate?

Il blocco impedisce nuove copie. Le registrazioni precedenti non sono cancellate automaticamente: gestiscile dalla scheda o dalle prenotazioni, preservando la cronologia dei pagamenti.

### Il pallino Messaggi resta dopo aver letto una notifica

Notifiche e messaggi non letti hanno conteggi distinti. Apri la conversazione o usa “Segna come letto”.

### La fotocamera del profilo non si apre

Verifica il permesso Camera nelle impostazioni dell’app e la presenza di una camera sul dispositivo/emulatore. Puoi sempre scegliere un’immagine dalla galleria. Dopo lo scatto conferma con “Carica foto”.

### Dove sono salvati database e foto?

Compose usa i volumi `database`, `media` e `logs`. Fermare i container non li elimina. `docker compose down -v` elimina invece i volumi: non usarlo come normale comando di aggiornamento.

### Sono supportate tutte le funzioni su Android?

No: la matrice del [README](../README.md) e la [guida Android](../ea_project_application/README.md) distinguono le funzioni dei due client.

### Come segnalo un problema?

Usa [i modelli delle issue](https://github.com/pxxyco/untitled_ea_project/issues/new/choose). Per una vulnerabilità segui [SECURITY.md](../SECURITY.md), evitando dettagli sensibili nelle issue pubbliche.

### Posso pubblicare un’offerta per oggi?

Sì. Home, ricerca e consigli includono anche viaggi e attività previsti per oggi, fino alle 23:59 del giorno di inizio, purché pubblicati e con posti disponibili. Dal giorno successivo non sono più prenotabili. Gli orari del programma restano quelli inseriti dall’organizzatore; per le esperienze su più giorni resta valida la data finale prevista.
