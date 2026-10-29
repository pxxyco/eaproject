# Gestione operativa

## Avvio e diagnostica

```bash
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 backend
docker compose logs --tail=100 db
```

Il bind predefinito è `127.0.0.1:8443`. Esporre il servizio su rete richiede configurazione esplicita di bind, firewall, endpoint dei client e TLS. I container usano i volumi `database`, `media` e `logs`; il database non espone una porta host nel Compose corrente.

## Backup

Prima di un aggiornamento salva un dump PostgreSQL e una copia coerente dei media. Esempio, su una macchina autorizzata:

```bash
mkdir -p backups
docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' > backups/database.sql
docker compose cp backend:/app/storage backups/storage
```

Per una copia coerente pianifica una finestra senza scritture applicative. I backup contengono dati personali: conservarli fuori dal repository, con accessi limitati, e verificare un ripristino prima di considerarli affidabili. Non allegarli alle release.

## Aggiornamento e rollback

1. Registra versione/commit e configurazione attuale.
2. Esegui e verifica il backup di database e media.
3. Ferma le scritture, carica/builda l’immagine della versione scelta e avvia Compose.
4. Verifica health, login, lettura dei dati, una prenotazione controllata e le foto.
5. In caso di rollback ripristina la versione compatibile e, se necessario, il backup coerente. Tornare soltanto al vecchio JAR non annulla modifiche allo schema.

Il progetto usa attualmente Hibernate `ddl-auto=update`, non una sequenza versionata di migrazioni. Valuta ogni modifica allo schema prima dell’aggiornamento.

`docker compose down` ferma l’installazione mantenendo i volumi. `docker compose down -v` li elimina ed è destinato solo a un reset volontario dei dati.
