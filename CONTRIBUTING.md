# Contribuire a EA Project

Leggi [installazione](docs/GETTING_STARTED.md), [architettura](docs/ARCHITECTURE.md) e [codice di condotta](CODE_OF_CONDUCT.md).

## Flusso di lavoro

1. Cerca una issue esistente; per un cambiamento ampio descrivi prima problema e comportamento atteso.
2. Crea un branch dal ramo di integrazione concordato dal team. Usa nomi descrittivi, per esempio `fix/profile-camera` o `feat/shared-itinerary`.
3. Mantieni separati backend, componenti UI riutilizzabili e logica delle schermate. Prima di aggiungere un helper cerca quello già presente.
4. Aggiorna DTO, API, import e risorse quando cambi un contratto. Usa i token del tema, non colori isolati nelle schermate.
5. Esegui i [controlli pertinenti](docs/TESTING.md), aggiorna documentazione e sezione Unreleased del changelog.
6. Apri una pull request usando il modello incluso. Indica problema, comportamento finale, prove eseguite e limiti noti; aggiungi schermate per modifiche UI.

Evita di includere file dell’IDE personali, output di build, keystore, `.env`, dati reali, immagini private o log con token. Non applicare reformat o refactor estranei a una correzione.

## Revisioni

Un altro membro del team verifica il cambiamento prima dell’unione. Risolvi le conversazioni aperte e conserva una cronologia leggibile. I controlli CI non sostituiscono la revisione funzionale o il collaudo su dispositivo.

## Licenza dei contributi

I contributi originali al progetto sono proposti sotto GNU GPL v3.0 (`GPL-3.0-only`), come il codice del repository. Dichiara provenienza e licenza di codice o asset di terzi; non aggiungere materiale che non puoi condividere.
