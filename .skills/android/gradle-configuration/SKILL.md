---
name: gradle-configuration
description: Guide for configuring Gradle builds for Android projects using Version Catalogs, Kotlin DSL, and best practices for dependency management. Use when adding dependencies, updating versions, resolving conflicts, or optimizing build performance.
---

# Gradle Configuration

## Overview

Best practices for configuring Android Gradle builds: Version Catalogs as single source of truth,
Kotlin DSL, dependency management, build performance, and common troubleshooting.

---

## Version Catalog (libs.versions.toml)

**Single source of truth** for all dependency versions. Located at `gradle/libs.versions.toml`.

### Structure

```toml
[versions]
kotlin = "2.1.21"
agp = "8.5.0"
compose-bom = "2024.12.01"
retrofit = "2.11.0"
mockk = "1.13.12"
coroutines = "1.8.1"

[libraries]
# Pattern: group-artifact = { module = "group:artifact", version.ref = "key" }
retrofit-core = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-converter-gson = { module = "com.squareup.retrofit2:converter-gson", version.ref = "retrofit" }

# BOM — no version here
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
# BOM children — no version
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }

# Testing
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }

[bundles]
# Group libraries used together
retrofit = ["retrofit-core", "retrofit-converter-gson"]
compose-core = ["compose-ui", "compose-material3"]
```

### Usage in build.gradle.kts

```kotlin
// Type-safe accessors
dependencies {
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    // or via bundle
    implementation(libs.bundles.retrofit)

    // BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

---

## Module build.gradle.kts

### Application Module

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // your convention plugins
    id("my.android.application")
}

android {
    namespace = "com.example.myapp"
    // SDK versions come from convention plugin
}

dependencies {
    implementation(project(":feature:home"))
    implementation(project(":feature:profile"))
    implementation(libs.androidx.core.ktx)
}
```

### Library Module

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("my.android.library")    // convention plugin handles the rest
    id("my.android.unit.test")
}

android {
    namespace = "com.example.feature.home"
}

dependencies {
    implementation(project(":component:user"))
    implementation(libs.androidx.lifecycle.viewmodel)
}
```

---

## Dependency Scopes

| Scope                       | When to Use                                           |
|-----------------------------|-------------------------------------------------------|
| `implementation`            | Internal dependency, not leaked to consumers          |
| `api`                       | Dependency exposed to consumers (use sparingly)       |
| `compileOnly`               | Needed only at compile time (annotations, Lint rules) |
| `runtimeOnly`               | Needed only at runtime                                |
| `testImplementation`        | Unit test only                                        |
| `androidTestImplementation` | Instrumented test only                                |
| `debugImplementation`       | Debug build only (e.g., LeakCanary, Compose tooling)  |

> **Prefer `implementation` over `api`**. `api` leaks transitive dependencies, increases coupling,
> and slows compilation.

---

## Dependency Conflict Resolution

### Force a Version

```kotlin
// In root build.gradle.kts
configurations.all {
    resolutionStrategy {
        force(libs.kotlin.stdlib)
        // or for a specific module
        eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(libs.versions.kotlin.get())
                because("All Kotlin modules must use the same version")
            }
        }
    }
}
```

### Exclude a Transitive Dependency

```kotlin
implementation(libs.someLibrary) {
    exclude(group = "com.google.guava", module = "guava")
}
```

### View Dependency Tree

```bash
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
# or specific module
./gradlew :feature:home:dependencies
```

---

## Build Performance

### gradle.properties

```properties
# Parallel execution
org.gradle.parallel=true

# Build cache
org.gradle.caching=true

# Configuration cache (Gradle 8+)
org.gradle.configuration-cache=true

# Increase heap for large projects
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC

# AndroidX
android.useAndroidX=true

# R8 in debug (optional — faster debug builds)
android.enableR8.fullMode=false
```

### Configuration Cache

```bash
# First run: configures + executes
./gradlew assembleDebug

# Subsequent runs: skips configuration phase
./gradlew assembleDebug  # ~50% faster
```

Incompatible tasks must be excluded:

```properties
org.gradle.configuration-cache.problems=warn
```

---

## Compatibility Matrix (Critical)

Certain dependencies must align or builds fail:

| Dependency                | Constraint                             |
|---------------------------|----------------------------------------|
| Kotlin ↔ Compose Compiler | Must match exactly per release         |
| AGP ↔ Gradle              | AGP requires minimum Gradle version    |
| AGP ↔ Kotlin              | Minimum Kotlin version per AGP version |

**Check before upgrading**:

- [AGP ↔ Gradle compatibility](https://developer.android.com/build/releases/gradle-plugin#updating-gradle)
- [Kotlin ↔ Compose Compiler](https://developer.android.com/jetpack/androidx/releases/compose-kotlin)

---

## Common Gradle Commands

```bash
# Build
./gradlew assembleDebug
./gradlew assembleRelease

# Test
./gradlew test                          # all unit tests
./gradlew :feature:home:test            # specific module

# Lint
./gradlew lint
./gradlew :feature:home:lint

# Clean
./gradlew clean

# Dependency insight
./gradlew dependencyInsight --dependency retrofit --configuration runtimeClasspath

# Check outdated dependencies
./gradlew dependencyUpdates              # requires com.github.ben-manes.versions plugin

# Build scan (requires agreement)
./gradlew build --scan
```

---

## Kotlin DSL Tips

```kotlin
// Access version catalog in build script
val kotlinVersion = libs.versions.kotlin.get()

// Configure build type
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }
}

// Product flavors
android {
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            buildConfigField("String", "BASE_URL", "\"https://dev.api.example.com\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://api.example.com\"")
        }
    }
}
```

---

## Checklist: Adding a Dependency

- [ ] Version declared in `[versions]` section of `libs.versions.toml`
- [ ] Library declared in `[libraries]` section with `version.ref`
- [ ] Added to module via `libs.my.library` (never hardcoded)
- [ ] Correct scope used (`implementation` vs `testImplementation`)
- [ ] Compatibility with existing versions verified
- [ ] No version conflicts introduced (run `./gradlew dependencies`)

---

## References

- [Version Catalogs](https://docs.gradle.org/current/userguide/version_catalogs.html)
- [Android Gradle Plugin](https://developer.android.com/build/releases/gradle-plugin)
- [Gradle Build Performance](https://docs.gradle.org/current/userguide/performance.html)
- [Kotlin DSL for Gradle](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
