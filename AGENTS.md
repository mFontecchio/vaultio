# AGENTS.md

## Start here

- This repo uses `context-map.md` as the primary architectural reference. It is kept current (last
  update: 2026-05-19).
- Current version: `1.1.1` (versionCode 3).
- Room DB: `version = 14`.

## Architecture that matters

- `Vaultio` is a **single-module Android app** (`:app`). Main packages under
  `app/src/main/java/com/mrhayami/vaultio/`:
    - `data/` = Room, Retrofit APIs, DataStore prefs, WorkManager workers, shared utilities.
    - `ui/` = Feature packages (collection, scanner, settings, stats, card_detail, grading).
    - `ui/common/MviViewModel.kt` = Shared MVI base class.
- `data/repository/VaultioRepository.kt` is the main integration hub. It handles set sync,
  collection CRUD, pricing, telemetry, and collection snapshots.
- **No DI framework**: Dependencies are manually created in `VaultioApplication.kt`.
- **Navigation**: Centralized in `MainActivity.kt`. Note that `card_detail/{userCardId}` is declared
  inline.

## Core data flows

- **Scanner Flow** (`ui/scanner/`): CameraX -> ROI crop -> Prioritized OCR matching (
  `number/total` > `prefix+number` > standalone) -> pHash computation.
- **Disambiguation**: If OCR yields multiple candidates, the system fetches remote images
  on-the-fly, computes pHashes, and compares Hamming distance (< 12) for an auto-lock.
- **Consensus**: Requires 3 stable frames for a lock to prevent noise from attack/ability text.
- **Scanner Modes**: Normal, Bulk (auto-save), and Page (3x3 grid digitization).
- **Pricing**: TCGDex is primary; JustTCG is fallback/vintage. Quota is tracked in Room.
- **Stats**: Daily snapshots are stored in `CollectionSnapshotEntity` and visualized with Vico.
- **AI Grading**: Uses on-device Gemini Nano via `GradingRepository` and `GeminiNanoClient`.

## Project-specific conventions

- **MVI Pattern**: `UiState` + `Event` + `Effect`. See `ScannerViewModel.kt` or
  `CollectionViewModel.kt`.
- **Room Changes**: Update `Entities.kt`, `Daos.kt`, and `VaultioDatabase.kt`. Version bumps wipe
  data due to `fallbackToDestructiveMigration()`.
- **User Preferences**: Managed via `UserPreferencesRepository.kt` using DataStore.
- **Logging**: Global `HttpLoggingInterceptor.Level.BODY` is enabled.

## Useful workflows

- **Testing**: `./gradlew.bat testDebugUnitTest --console=plain`
- **Building**: `./gradlew.bat assembleDebug --console=plain`
- **Unit Tests**: Scanner matching logic is verified in `ScannerMatchingTest.kt`.

