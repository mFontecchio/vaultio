# Changelog

All notable changes to Vaultio are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project aims to follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.2.8] - 2026-08-09

### Changed

- Collection grid and Pokédex detail thumbnails load `low.webp` instead of `high.webp` to cut scroll bandwidth and decode cost.
- Card Detail energy animations redraw at ~30fps instead of every display frame while prefs keep effects enabled.
- Wishlist, Stats, Card Detail, and Scanner UI state use `@Immutable` / kotlinx immutable collections (aligned with Collection) for safer Compose skipping.
- Pokédex grayscale filter is remembered once per screen; Stats set-completion and Dex detail grids use stable lazy keys.

### Fixed

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
