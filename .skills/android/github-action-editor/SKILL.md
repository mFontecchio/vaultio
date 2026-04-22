---
name: github-action-editor
description: Guide for writing and maintaining GitHub Actions workflows for Android projects. Use when creating new CI pipelines, adding workflow steps, debugging failing actions, or optimizing build times with caching and parallelization.
---

# GitHub Action Editor

## Overview

Best practices for GitHub Actions in Android projects: workflow structure, job dependencies, Gradle
caching, secrets management, matrix builds, and CI optimization.

---

## Workflow Anatomy

```yaml
# .github/workflows/ci.yml
name: CI

on:
  pull_request:
    branches: [main, develop]
  push:
    branches: [main, develop]

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true   # Cancel previous runs on new push

jobs:
  build:
    name: Build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build
        run: ./gradlew assembleDebug
```

---

## Reusable Workflow (Callable)

Split large workflows into reusable units:

```yaml
# .github/workflows/workflow_build.yml
name: Build (Reusable)

on:
  workflow_call:
    inputs:
      build_type:
        type: string
        required: true
        default: debug
    secrets:
      KEYSTORE_FILE:
        required: false

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: ./.github/actions/setup-android   # composite action (see below)
      - name: Build
        run: ./gradlew assemble${{ inputs.build_type }}
```

```yaml
# .github/workflows/on_pull_request.yml
name: Pull Request

on:
  pull_request:
    types: [opened, reopened, synchronize]

concurrency:
  group: pr-${{ github.event.pull_request.number }}
  cancel-in-progress: true

jobs:
  build:
    uses: ./.github/workflows/workflow_build.yml
    with:
      build_type: Debug

  test:
    uses: ./.github/workflows/workflow_test.yml

  lint:
    uses: ./.github/workflows/workflow_lint.yml
```

---

## Composite Action (Setup Android)

Extract repeated setup steps:

```yaml
# .github/actions/setup-android/action.yml
name: Setup Android
description: Setup Java, Android SDK, and Gradle cache

inputs:
  java-version:
    description: Java version
    default: '17'

runs:
  using: composite
  steps:
    - uses: actions/setup-java@v4
      with:
        java-version: ${{ inputs.java-version }}
        distribution: temurin

    - name: Setup Gradle
      uses: gradle/actions/setup-gradle@v3
      with:
        gradle-home-cache-cleanup: true

    - name: Cache Gradle dependencies
      uses: actions/cache@v4
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: gradle-${{ runner.os }}-${{ hashFiles('**/*.toml', '**/*.gradle.kts', 'gradle/wrapper/gradle-wrapper.properties') }}
        restore-keys: |
          gradle-${{ runner.os }}-
```

---

## Gradle Caching Strategy

```yaml
- name: Cache Gradle and Android SDK
  uses: actions/cache@v4
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
      ~/.android/build-cache
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.versions.toml', '**/*.gradle.kts', 'gradle/wrapper/gradle-wrapper.properties') }}
    restore-keys: |
      ${{ runner.os }}-gradle-
```

### Gradle Build Cache for CI

```yaml
- name: Build with cache
  run: ./gradlew assembleDebug
  env:
    GRADLE_BUILD_ACTION_CACHE_DEBUG_ENABLED: true
```

---

## Common Workflow Jobs

### Lint

```yaml
lint:
  name: Lint
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: ./.github/actions/setup-android
    - name: Run Lint
      run: ./gradlew lint
    - name: Upload lint reports
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: lint-reports
        path: '**/build/reports/lint-results*.html'
```

### Unit Tests

```yaml
test:
  name: Unit Tests
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: ./.github/actions/setup-android
    - name: Run Tests
      run: ./gradlew test
    - name: Upload test reports
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: test-reports
        path: '**/build/reports/tests/'
    - name: Publish test results
      uses: dorny/test-reporter@v1
      if: always()
      with:
        name: Unit Test Results
        path: '**/build/test-results/**/*.xml'
        reporter: java-junit
```

### Snapshot Tests

```yaml
snapshot:
  name: Snapshot Tests
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: ./.github/actions/setup-android
    - name: Verify Snapshots
      run: ./gradlew verifyPaparazziDebug
    - name: Upload snapshot diffs on failure
      if: failure()
      uses: actions/upload-artifact@v4
      with:
        name: snapshot-diffs
        path: '**/build/paparazzi/failures/'
```

### Detekt (Static Analysis)

```yaml
detekt:
  name: Detekt
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: ./.github/actions/setup-android
    - name: Run Detekt
      run: ./gradlew detekt
    - name: Upload SARIF to GitHub Security tab
      uses: github/codeql-action/upload-sarif@v3
      if: always()
      with:
        sarif_file: '**/build/reports/detekt/detekt.sarif'
```

---

## Matrix Builds

```yaml
test:
  strategy:
    matrix:
      api-level: [26, 31, 34]
      include:
        - api-level: 34
          target: google_apis
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - name: Run Instrumented Tests
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: ${{ matrix.api-level }}
        target: ${{ matrix.target || 'default' }}
        arch: x86_64
        script: ./gradlew connectedAndroidTest
```

---

## Secrets and Environment Variables

```yaml
# Reference secrets
- name: Build release APK
  run: ./gradlew assembleRelease
  env:
    KEYSTORE_FILE: ${{ secrets.KEYSTORE_FILE }}
    KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
    KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
    KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}

# Decode base64 keystore secret
- name: Decode keystore
  run: echo "${{ secrets.KEYSTORE_FILE }}" | base64 -d > keystore.jks
```

---

## Conditional Steps

```yaml
# Run only on main branch
- name: Deploy to Play Store
  if: github.ref == 'refs/heads/main' && github.event_name == 'push'
  run: ./gradlew publishRelease

# Run only when specific files change
on:
  push:
    paths:
      - '**/*.kt'
      - '**/*.gradle.kts'
      - 'gradle/**'
      # Ignore documentation changes
      - '!**/*.md'

# Skip CI (commit message)
# Add [skip ci] to commit message
```

---

## Job Dependencies and Artifacts

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      apk_path: ${{ steps.build.outputs.apk_path }}
    steps:
      - id: build
        run: |
          ./gradlew assembleDebug
          echo "apk_path=app/build/outputs/apk/debug/app-debug.apk" >> $GITHUB_OUTPUT
      - uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: app/build/outputs/apk/debug/

  distribute:
    needs: build    # only runs if build succeeds
    runs-on: ubuntu-latest
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: debug-apk
          path: apk/
```

---

## Build Time Optimization

```yaml
# Use larger runner for faster builds (GitHub-hosted)
runs-on: ubuntu-latest-4-cores

# Enable configuration cache
- name: Build with config cache
  run: ./gradlew assembleDebug --configuration-cache

# Parallel execution
- name: Run tests in parallel
  run: ./gradlew test --parallel --max-workers=4

# Only build affected modules (requires tooling like Affected Module Detector)
- name: Detect affected modules
  run: ./gradlew :tools:affected-module-detector:run
```

---

## Workflow Best Practices

| ✅ Do                                       | ❌ Don't                                 |
|--------------------------------------------|-----------------------------------------|
| Use `concurrency` to cancel stale runs     | Let stale PRs queue up indefinitely     |
| Cache Gradle dependencies with hash key    | Use static cache key (stale cache)      |
| Use composite actions for repeated setup   | Copy-paste setup steps across workflows |
| Upload artifacts on failure                | Lose test/lint output after failure     |
| Use `secrets.*` for sensitive values       | Hardcode credentials in workflow files  |
| Pin action versions (`@v4`)                | Use `@main` or `@latest` (unpinned)     |
| Fail fast with `fail-fast: true` in matrix | Let all matrix jobs run when one fails  |

---

## Checklist: New Workflow

- [ ] `concurrency` block prevents redundant runs
- [ ] Gradle cache configured with hash-based key
- [ ] Secrets referenced via `${{ secrets.NAME }}`
- [ ] Artifacts uploaded for test/lint reports
- [ ] `actions/checkout@v4` used (not older versions)
- [ ] `setup-java@v4` with `distribution: temurin`
- [ ] Workflow tested on a feature branch before merging
- [ ] Reusable workflows used for shared job logic

---

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [gradle/actions/setup-gradle](https://github.com/gradle/actions)
- [android-emulator-runner](https://github.com/ReactiveCircus/android-emulator-runner)
- [Workflow syntax reference](https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions)
