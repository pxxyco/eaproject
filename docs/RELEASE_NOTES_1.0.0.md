# EA Project v1.0.0 — RTM

Prima build ufficiale stabile di EA Project per il corso Enterprise Applications 2025/2026.

## Funzionalità

- Backend condiviso con API REST Spring Boot, PostgreSQL/JPA, JWT e ruoli Traveler/Organizer.
- Client desktop JavaFX e client Android Kotlin/Compose.
- Catalogo con ricerca, categorie, foto e raccomandazioni personalizzate.
- Prenotazioni con disponibilità controllata, protezione dai duplicati e gestione delle richieste concorrenti.
- Offerte per il giorno corrente visibili e prenotabili fino alle 23:59 del giorno di inizio.
- Profili, foto, recensioni, preferiti e strumenti Organizer.
- In JavaFX: pagamento mock, chat in tempo reale, notifiche, raccolte condivise, itinerari collaborativi, export calendario e mappa.
- Logging applicativo e audit, rate limiting, Swagger e test automatici.

La matrice delle funzioni dei due client è nel [README](../README.md).

## Distribuzione

- Tag Git previsto per la prima release ufficiale: `v1.0.0`.
- Versione backend e JavaFX: `1.0.0`.
- Versione Android: `versionName=1.0.0`, `versionCode=1`.
- Immagine backend: `ea-project-backend:1.0.0`.
- Database standard: `postgres:17-alpine`, vuoto su un volume nuovo.
- Archivio Docker: `ea-project-images-1.0.0.tar`, con checksum SHA-256 e piattaforma da riportare negli asset pubblicati.

I pacchetti Desktop + Backend e Android + Backend includono sorgenti e guide. Le librerie native del desktop dipendono dal sistema e dall’architettura di build. Java 21 è richiesto per il desktop e per l’avvio del backend JAR; i container includono il runtime del server.

Le immagini e gli ZIP devono essere generati dal commit finale approvato. I comandi sono nella [guida release](RELEASING.md); la preparazione locale non pubblica automaticamente un tag o una GitHub Release.

## Configurazione e limiti

- I pagamenti sono mock, senza addebiti reali.
- Keystore TLS, chiavi di firma e credenziali sono esterni agli asset pubblici.
- L’APK demo/debug usa l’endpoint e la CA di sviluppo; l’APK release richiede firma e configurazione di rete adeguate. RTM è la versione del software, non una sostituzione della firma Android.
- I dati demo non fanno parte dell’immagine PostgreSQL standard.
- Prima di aggiornare un’installazione esistente, salva database e media. Il progetto usa Hibernate `ddl-auto=update`; non viene applicato un reset automatico dei dati.

## Verifica prima della pubblicazione

Allega gli esiti dei test e dei collaudi relativi al commit distribuito, indicando piattaforme e dispositivi realmente provati. Il collaudo della fotocamera su hardware reale e la prima esecuzione remota di GitHub Actions vanno riportati soltanto dopo l’esecuzione effettiva.

GNU GPL v3.0 per il codice originale. Vedi [LICENSE](../LICENSE), [autori](../AUTHORS.md) e [dipendenze/asset di terzi](../THIRD_PARTY_NOTICES.md).
