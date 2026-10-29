# Autori e contributi

## Il gruppo

Ricostruiti dalla cronologia dei commit (`git log`).

### Bruno (`brunolen`)
- **Backend:** entità JPA, repository, service e controller per **Booking, Payment e Review**; endpoint per le recensioni (creazione, modifica, cancellazione, ricalcolo della media voti) e per le **risposte dell'organizzatore**; creazione delle tappe (`Stage`); tabella **FAVORITES** ed endpoint `/api/favorites`.
- **JavaFX:** pagina di dettaglio di attività e viaggi (descrizione, itinerario con mappa e tappe, galleria foto con vista a schermo intero, cuore dei preferiti); interfaccia delle recensioni con paginazione, modifica e risposte.
- **Android:** schermata di **dettaglio dell'esperienza** (tab, descrizione, itinerario e timeline delle tappe, foto, recensioni con modifica e risposte, prenotazione, preferiti e aggiunta alle liste).

### Matteo Marzano (`pxxyco`)
- **Backend:** entità JPA Notification, Stage e Trip con relativi service e controller; Log4j2; **raccomandazioni**, rate limiting e sicurezza JWT; scheletro di **chat** e **Swagger**; **pagamenti mock**; **Docker** del backend; Spring Boot Actuator.
- **JavaFX:** creazione del progetto; profilo viaggiatore e organizzatore, foto profilo, navbar, home e animazioni; **liste di preferiti** (creazione, modifica, condivisione, eliminazione); sistema di notifiche; **export `.ics`**; storico prenotazioni; interfaccia dell'organizzatore rifatta; riorganizzazione dei CSS e del codice.
- **Android:** profilo del viaggiatore.

### Antonio Franco (`Yorshkrum`)
- **Backend:** entità JPA Activity, ActivityImage e TripImage, con repository, service e controller (anche User); **archiviazione delle foto** di attività e viaggi con copertine e ordinamento; primo export calendario (`CalendarController`).
- **JavaFX:** **dashboard dell'organizzatore** (creazione, modifica ed eliminazione di attività e viaggi).
- **Android:** **dashboard dell'organizzatore** (foto, itinerari, coordinate, filtri, stati, posti, prenotazioni, incassi); gestione di liste e prenotazioni (eliminazione liste, rimozione elementi, annullamento prenotazioni); barra di navigazione inferiore; correzioni dopo i merge.

### Luca Santoro (`lucoilbasso`)
- **Backend:** creazione del progetto Spring; entità User, FavoriteList, ListTrips, ListActivities, ListSharing e Response; **JWT** e `SecurityConfig`; sicurezza del login (BCrypt, cache Caffeine, protezione da timing attack); **rate limiting**; salvataggio delle foto su disco; **HTTPS** con i certificati per backend, JavaFX e Android.
- **JavaFX:** login e home, `ApiService` (GET/POST/PUT/DELETE), DTO, barra di ricerca con filtri e categorie, card delle esperienze, cache delle immagini, gestione degli errori di connessione e dei messaggi di login.
- **Android:** creazione del progetto; `ApiService`, `UserSession`, JWT; login e profilo; **home** collegata al backend con suggerimenti, filtro per data e caricamento progressivo; preferiti.

