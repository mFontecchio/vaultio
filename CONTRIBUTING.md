# Contributing to Vaultio

Thanks for helping improve Vaultio. This guide covers how to report issues, propose changes, and
open pull requests.

## Code of conduct

Be respectful and constructive in issues, PRs, and discussions. Harassment or bad-faith behavior
will not be tolerated.

## Ways to contribute

- **Bug reports** — use the Bug report issue form
- **Feature ideas** — use the Feature request issue form
- **Code / docs** — open a PR against the active development branch
- **Questions** — prefer GitHub Discussions (if enabled) or a clearly titled issue; check the
  [README](README.md) first

## Development setup

1. Fork and clone the repo
2. Open in Android Studio (JDK 11+) or sync with Gradle from the CLI
3. Optionally paste a JustTCG API key in **Settings** (DataStore). Never commit API keys,
   `local.properties`, keystores, or `keystore.properties`
4. Run a debug build and, before PR, unit tests:

   ```bat
   gradlew.bat assembleDebug --console=plain
   gradlew.bat testDebugUnitTest --console=plain
   ```

Camera scanning needs a physical device. Emulators are fine for most other UI flows.

CI must pass on your PR (`test` + `build-debug` workflows).

## Project conventions

- **Kotlin + Jetpack Compose**, Material 3
- **MVI**: `UiState` + `Event` + `Effect` (see existing feature ViewModels)
- **Manual DI** in `VaultioApplication` — no Hilt/Koin unless agreed in an issue first
- **Focused diffs** — one concern per PR; avoid drive-by refactors
- **Room**: bump `VaultioDatabase` version when changing entities/DAOs. Migrations are destructive
  (`fallbackToDestructiveMigration()`) — call this out in the PR
- **Secrets**: never commit API keys, `local.properties`, keystores (`*.jks` / `*.keystore`), or
  `keystore.properties`

## Pull request process

1. Open an issue first for larger features or breaking changes (optional for small fixes)
2. Create a branch from the latest default / feature branch you were asked to target
3. Keep commits readable; squash is fine if the history is noisy
4. Fill out the pull request template
5. Ensure the app builds and relevant tests pass; wait for CI
6. Expect review feedback on UX, MVI shape, and Android lifecycle / concurrency

### PR checklist (summary)

- [ ] Builds (`assembleDebug` at minimum)
- [ ] `testDebugUnitTest` passes when code under test changed
- [ ] CI green on the PR
- [ ] Room / prefs / nav changes documented in the PR body
- [ ] No secrets or generated junk in the diff

## Issue guidelines

**Bugs:** include app version (or build type), device / Android version, steps to reproduce, expected
vs actual behavior, and logs/screenshots when useful.

**Features:** describe the problem, proposed solution, and alternatives you considered. Scanner,
pricing, and grading ideas should note offline vs online expectations.

## License

By contributing, you agree that your contributions are licensed under the
[Apache License 2.0](LICENSE).
