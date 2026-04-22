# AGENTS.md

## Start here

- This repo has **no existing agent-specific docs or README** from the standard search patterns; use
  this file plus `context-map.md` for orientation.
- `context-map.md` is high-signal but slightly stale: verify against code before editing. Current
  code has `ui/stats/`, `CollectionSnapshotWorker`, Room DB `version = 12`, and app
  `versionName = "1.0.6r11"`.

## Architecture that matters

- `Vaultio` is a **single-module Android app** (`:app`) with conceptual boundaries only. Main
  packages live under `app/src/main/java/com/mrhayami/vaultio/`:
    - `data/` = Room, Retrofit APIs, DataStore prefs, workers, shared utilities
    - `ui/collection`, `ui/scanner`, `ui/settings`, `ui/stats`, `ui/card_detail` = feature packages
    - `ui/common/MviViewModel.kt` = shared MVI base class
- `data/repository/VaultioRepository.kt` is the main service boundary. It owns set sync/download,
  collection CRUD, import/export, pricing, telemetry, and snapshots. Prefer extending it instead of
  bypassing it from UI code.
- There is **no DI framework**. `VaultioApplication.kt` manually creates Room, Retrofit,
  `UserPreferencesRepository`, and `VaultioRepository`; screens/ViewModels use manual
  `ViewModelProvider.Factory` creation.
- Navigation is centralized in `MainActivity.kt`. Important gotcha: `card_detail/{userCardId}` is
  declared inline there, not in `ui/navigation/Screen.kt`.

## Core data flows

- Offline catalog data (`sets`, `cards`) is foundational. `SetDownloadsViewModel`
  refreshes/downloads TCGDex data; scanner and collection features assume local catalog entries
  exist.
- Scanner flow (`ui/scanner/`): CameraX frame -> ROI crop in `CameraAnalyzer.kt` -> OCR + pHash ->
  local DB match by `localId`/set total -> remote TCGDex fallback -> save via
  `VaultioRepository.addUserCard()`.
- Scanner has **three modes in code**: normal scan, bulk scan, and page scan (
  `PageScanProcessor.kt`, `PageScanModels.kt`). Keep new scanner work compatible with all three.
- Pricing flow: TCGDex is primary, JustTCG is fallback for modern cards and primary for vintage
  routing (`VintageSets.kt`, `PricingUtils.kt`). Runtime API quota is stored in Room and surfaced in
  settings.
- Stats depend on daily collection snapshots: `VaultioApplication.kt` schedules
  `CollectionSnapshotWorker`; `ui/stats/` renders value history and distributions with Vico charts.

## Project-specific conventions

- Prefer the repo’s **MVI shape** for stateful screens: `UiState` + sealed `Event`/`Effect` + single
  `onEvent(...)` entry point. See `CollectionViewModel.kt`, `SettingsViewModel.kt`,
  `StatsViewModel.kt`.
- State is usually derived with large `combine(...)` pipelines from Room/DataStore flows; avoid
  pushing business logic into composables.
- Room changes usually touch **three files together**: `data/local/Entities.kt`,
  `data/local/Daos.kt`, and `data/local/VaultioDatabase.kt`. The DB uses
  `fallbackToDestructiveMigration()`, so schema bumps wipe local data.
- `PriceMetaEntity` exists in `Entities.kt` but currently has **no DAO or repository usage**; treat
  it as dormant until fully wired.
- `UserPreferencesRepository.kt` is more than UI prefs: it controls walkthrough gating, theme,
  layout, animation toggles, and scanner bulk defaults.
- `app/build.gradle.kts` reads `JUST_TCG_API_KEY` from `local.properties` into `BuildConfig`, but
  the pricing code actually reads the API key from DataStore/settings at runtime.
- `VaultioApplication.kt` enables `HttpLoggingInterceptor.Level.BODY` globally, so avoid adding code
  that logs secrets or assumes silent networking.

## Useful workflows

- Verified locally on Windows: `./gradlew.bat testDebugUnitTest --console=plain` succeeds.
- Common commands:
    - `./gradlew.bat assembleDebug --console=plain`
    - `./gradlew.bat testDebugUnitTest --console=plain`
    - `./gradlew.bat connectedDebugAndroidTest --console=plain`
    - `./gradlew.bat lintDebug --console=plain`
- Current automated test coverage is minimal: only `ExampleUnitTest.kt` and
  `ExampleInstrumentedTest.kt` exist. For risky changes, rely on targeted manual verification in
  addition to Gradle tasks.
- No `.github/workflows/` CI configuration was found, so local Gradle verification is the source of
  truth.

