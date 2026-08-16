# Changelog

All notable changes to Vaultio are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project aims to follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.2.8] - 2026-08-09

### Added

- In-app updates for GitHub-distributed **release** and **nightly** builds (Settings → About;
  optional automatic check & download). Play Store installs continue to update through Play.
  Downloaded APKs are signature- and version-checked before install.
- Wishlist as a primary bottom-navigation tab with Add / Scan speed dial and clearer empty states.
- Shared Add / Scan FAB menu on Collection and Wishlist (animated + → × with staggered actions).
- Shared `EmptyState` and `ConfirmDestructiveDialog` components for consistent empty and destructive flows.
- Walkthrough coverage for scanner modes (Search, Price Check, Bulk, Page, Grading).
- Scanner Snackbars for permission / capture / error feedback; clearer camera-permission CTAs.
- Grading retake, disclaimer, and condition confirmation before destructive or irreversible steps.
- Page-scan safe confirms and candidate picker when matches are ambiguous.
- Scanner Metadata modal can add the scanned card to the wishlist.

### Changed

- Collection: Folders chip in sticky controls; Sort & Filter moved to the top bar; Select All applies to the filtered set; Mint filter removed; active filter count shown.
- Collection no longer uses a separate Collection FAB; add/scan lives in the shared speed dial.
- Bottom navigation is four tabs only (Collection, Wishlist, Stats, Settings); scanner is opened from Collection / Wishlist actions.
- Stats caption layout cleaned up; JustTCG usage is collapsible in Settings.
- Theme contrast, type, and dimens polish; CardBadge uses clearer semantic colors.
- Energy theme onPrimary / onSecondary contrast improved for light seeds.
- Collection grid and Pokédex detail thumbnails load `low.webp` instead of `high.webp` to cut scroll bandwidth and decode cost.
- Card Detail energy animations redraw at ~30fps instead of every display frame while prefs keep effects enabled.
- Wishlist, Stats, Card Detail, and Scanner UI state use `@Immutable` / kotlinx immutable collections (aligned with Collection) for safer Compose skipping.
- Pokédex grayscale filter is remembered once per screen; Stats set-completion and Dex detail grids use stable lazy keys.

### Fixed

- Scanner is full-bleed again under transparent system bars (edge-to-edge); status / navigation icons stay visible over the camera preview.
- Grading no longer hands off a blank bitmap when capture is missing; errors surface via Snackbar instead.
- Destructive Settings actions (clear cache, reset settings, delete all sets) require confirmation.
- Pokédex view now reflects ownership after importing a saved library ([#21](https://github.com/mFontecchio/vaultio/issues/21)). Collection import enriches catalog cards with missing `dexId` / `dexIds` (including multi-Pokémon Tag Team / LEGEND cards), backfills any remaining owned null-dex rows, and the Pokédex UI falls back to name→dex lookup when stored metadata is still missing.

## [1.2.7] - 2026-08-08

### Added

- Mode-first scanner redesign (Search, Price Check, Bulk, Page, Grading)
- Wishlist
- On-device AI grading (Gemini Nano / ML Kit GenAI)
- Dependabot, CodeQL, and updated security reporting

[Unreleased]: https://github.com/mFontecchio/vaultio/compare/v1.2.8...HEAD
[1.2.8]: https://github.com/mFontecchio/vaultio/compare/v1.2.7...v1.2.8
[1.2.7]: https://github.com/mFontecchio/vaultio/releases/tag/v1.2.7
