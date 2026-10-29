# Dipendenze e contenuti di terzi

La GPL v3.0 di EA Project copre il codice originale del progetto. Non cambia le licenze delle librerie, dei runtime o dei contenuti di terzi.

## Inventario di riferimento

| Componente | Dove verificare versioni e informazioni |
|---|---|
| Spring Boot, Spring Security, JPA, Hibernate e librerie backend | [pom backend](ea-project/pom.xml), POM e file LICENSE/NOTICE degli artefatti |
| PostgreSQL e immagini Java/Maven | [Compose](compose.yaml), [Dockerfile](ea-project/Dockerfile) e relativi progetti upstream |
| JavaFX/OpenJFX, Gluon Maps, Gson e Lombok | [pom desktop](ea_project_javafx/pom.xml), POM e avvisi distribuiti nelle librerie |
| Kotlin, AndroidX/Compose, CameraX, OkHttp, Coil, ML Kit e lettura NFC | [Gradle Android](ea_project_application/app/build.gradle.kts) e [catalogo versioni](ea_project_application/gradle/libs.versions.toml) |
| Wrapper Maven e Gradle | File e avvisi dei rispettivi wrapper inclusi nel repository |

Prima di pubblicare binari conserva i file LICENSE/NOTICE delle dipendenze e del runtime effettivamente redistribuiti. Questo inventario non è un elenco automatico completo delle dipendenze transitive né una dichiarazione che abbiano tutte la stessa licenza.

## Immagini, marchi e dati

Gli sfondi giornalieri provengono da Bing e possono essere soggetti a diritti e condizioni specifici dell’immagine. Non vengono dichiarati come opera originale GPL del team. Lo stesso vale per marchi dei circuiti di pagamento, font e altri asset di terzi.

Verifica provenienza e autorizzazioni delle foto demo prima di allegarle a una release pubblica. Non distribuire fotografie, account o conversazioni personali. Se un asset non può essere ridistribuito, sostituiscilo con un contenuto autorizzato o documenta come procurarlo separatamente.

Il codice originale della release va reso disponibile insieme ai binari tramite l’archivio sorgenti dello stesso commit e il relativo testo di licenza.
