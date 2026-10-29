# Sicurezza

## Segnalazione riservata

Non pubblicare nelle issue exploit, token, credenziali, chiavi private, foto personali o dati di altri utenti.

Se la repository offre **Security → Report a vulnerability**, usa quel canale privato. La funzione richiede l’attivazione da parte di un amministratore; questo file da solo non la abilita. Se il canale non è disponibile, contatta privatamente un manutentore tramite il canale concordato del team. Non usare una issue pubblica per i dettagli sensibili.

Indica versione/commit, componente, condizioni necessarie, riproduzione minima, impatto e dati fittizi utilizzati. Il team valuterà la segnalazione e concorderà la divulgazione; non è promesso un tempo di risposta garantito.

## Versioni

Il riferimento è l’ultima release pubblicata e il ramo di integrazione attivo. Eventuali versioni supportate e correzioni retroportate sono dichiarate nelle rispettive note di release; non esiste un impegno LTS implicito.

## Configurazione

- Le chiavi di firma Android e TLS restano fuori da Git e dagli asset pubblici.
- Le credenziali demo sono pubbliche e servono solo al dataset dimostrativo.
- I pagamenti sono mock. Non usare dati di pagamento reali per prove o segnalazioni.
- Non disabilitare la verifica TLS per risolvere un errore di certificato.
- Per un segreto esposto revoca/ruota la credenziale: cancellare il file dall’ultimo commit non lo rimuove dalla cronologia.

Vedi [configurazione](docs/CONFIGURATION.md) e [gestione operativa](docs/OPERATIONS.md).
