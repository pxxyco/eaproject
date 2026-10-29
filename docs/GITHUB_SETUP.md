# Configurazione della repository GitHub

Questi valori sono pronti da copiare; i file non cambiano automaticamente le impostazioni online.

## About

Descrizione suggerita:

> Travel booking platform with Spring Boot APIs, JavaFX desktop and Android clients. JWT security, shared itineraries, recommendations and real-time chat.

La repository è `https://github.com/pxxyco/untitled_ea_project`. Mantieni come nome visibile del prodotto **EA Project**; rinominare la repository è una scelta separata e richiede aggiornare URL e riferimenti.

Nella sezione About della pagina principale apri l’ingranaggio, inserisci descrizione e topic e salva. Lascia vuoto Website finché non esiste una pagina pubblicata; non inserire `localhost`.

## Topic suggeriti

```text
java kotlin spring-boot javafx android jetpack-compose postgresql docker jwt rest-api travel booking-system collaborative-itineraries recommendation-system real-time-chat enterprise-applications university-project material3 tourism gpl-3-0
```

Sono 20 topic, il massimo previsto da GitHub. Usano minuscole, numeri e trattini e descrivono le funzioni presenti, senza dichiarare microservizi o pagamenti reali.

## Issue, PR e discussioni

I modelli in `.github/ISSUE_TEMPLATE` e `.github/PULL_REQUEST_TEMPLATE.md` diventano disponibili dopo averli integrati nel ramo predefinito. La repository ha attualmente `main` come riferimento remoto predefinito; verifica che l’impostazione online sia ancora quella concordata dal team.

Crea o conserva queste label:

| Label | Uso |
|---|---|
| `bug` | Errori riproducibili |
| `enhancement` | Nuove funzioni e miglioramenti |
| `documentation` | Documentazione |
| `question` | Richieste di aiuto |
| `backend`, `javafx`, `android` | Area interessata |
| `security` | Attività di correzione pubblicabile, non dettagli riservati |
| `release` | Preparazione di una versione |
| `skip-changelog` | Esclusione volontaria dalle note automatiche |

Le label dei modelli vengono assegnate soltanto se esistono. `.github/release.yml` organizza le note generate per categoria.

Puoi attivare Discussions e creare categorie Annunci, Domande e Idee; fino a quel momento le domande hanno un modello issue funzionante. Non sono necessari sponsor, funding o contatti inventati.

## Revisione e protezione

Dopo il primo esito positivo della CI, configura un ruleset per il ramo stabile: pull request, revisione di un altro membro, conversazioni risolte e controlli CI richiesti. Separa il ramo di integrazione da quello pubblicato secondo il flusso del gruppo.

`.github/CODEOWNERS.example` è intenzionalmente un esempio: sostituisci i nomi con i responsabili concordati, verifica che abbiano accesso di scrittura e rinominalo `CODEOWNERS` per attivarlo. Gli autori dei commit non vengono trasformati automaticamente in responsabili delle revisioni.

## Sicurezza e release

Attiva le funzioni di sicurezza disponibili per il piano e la visibilità della repository. Il canale **Private vulnerability reporting** va abilitato esplicitamente dove supportato; `SECURITY.md` non lo attiva da solo. Custodisci chiavi di firma e configurazioni fuori dal repository.

La CI ha permessi di lettura, non pubblica release e non usa credenziali TLS del team. La prima esecuzione remota resta da verificare dopo il push; i collaudi locali non equivalgono a un esito GitHub Actions.

Per pubblicare gli asset segui [RELEASING.md](RELEASING.md).

## Riferimenti ufficiali

- [Topic](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/classifying-your-repository-with-topics)
- [Modelli issue](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/syntax-for-issue-forms)
- [Segnalazioni private](https://docs.github.com/en/code-security/how-tos/report-and-fix-vulnerabilities/configure-vulnerability-reporting/configure-for-a-repository)
