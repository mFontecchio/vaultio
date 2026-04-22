---
name: gradle-convention-plugin
description: Guide for creating, organizing, and maintaining Gradle Convention Plugins for Android multi-module projects. Use when setting up shared build configuration, adding a new convention plugin, or understanding how to centralize build logic across modules.
---

# Gradle Convention Plugin

## Overview

Convention Plugins are custom Gradle plugins that centralize build configuration so that individual
module `build.gradle.kts` files remain minimal. This is the recommended approach for multi-module
Android projects.

**Problem without them**: Each module duplicates 100+ lines of SDK versions, compiler options, and
test configuration.  
**Solution**: Write config once in a plugin, apply it everywhere.

---

## Project Structure

```
build-logic/
└── convention/
    ├── build.gradle.kts          ← dependencies for the plugin module
    └── src/main/kotlin/
        ├── AndroidLibraryConventionPlugin.kt
        ├── AndroidApplicationConventionPlugin.kt
        ├── ComposeConventionPlugin.kt
        ├── UnitTestConventionPlugin.kt
        ├── SnapshotTestConventionPlugin.kt
        ├── DetektConventionPlugin.kt
        └── KotlinAndroidConventionPlugin.kt
settings.gradle.kts               ← includes build-logic
gradle/
└── libs.versions.toml            ← version catalog
```

### `settings.gradle.kts` — Include build-logic

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

### `build-logic/convention/build.gradle.kts`

```kotlin
plugins {
    `kotlin-dsl`
}

dependencies {
    // Access AGP API from convention plugins
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}
```

---

## Creating a Convention Plugin

### 1. Write the Plugin Class

```kotlin
// build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<LibraryExtension> {
                compileSdk = 35

                defaultConfig {
                    minSdk = 26
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    consumerProguardFiles("consumer-rules.pro")
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }

            configureKotlinAndroid(this)
        }
    }
}
```

### 2. Shared Configuration via Extension Functions

Extract shared logic into top-level functions in the same source set:

```kotlin
// build-logic/convention/src/main/kotlin/KotlinAndroid.kt
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun configureKotlinAndroid(project: Project) {
    project.tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "17"
            allWarningsAsErrors = false
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-opt-in=kotlin.RequiresOptIn",
            )
        }
    }
}
```

### 3. Register the Plugin

```kotlin
// build-logic/convention/build.gradle.kts
gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "my.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "my.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidCompose") {
            id = "my.android.compose"
            implementationClass = "ComposeConventionPlugin"
        }
        register("androidUnitTest") {
            id = "my.android.unit.test"
            implementationClass = "UnitTestConventionPlugin"
        }
    }
}
```

---

## Plugin Examples

### Compose Convention Plugin

```kotlin
class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val extension = extensions.getByType<CommonExtension<*, *, *, *, *, *>>()

            extension.apply {
                buildFeatures { compose = true }

                composeOptions {
                    // Kotlin version → Compose compiler version must match
                    kotlinCompilerExtensionVersion = libs.findVersion("composeCompiler").get().toString()
                }
            }

            dependencies.add("implementation", libs.findLibrary("compose.ui").get())
            dependencies.add("implementation", libs.findLibrary("compose.material3").get())
            dependencies.add("debugImplementation", libs.findLibrary("compose.ui.tooling").get())
        }
    }
}
```

### Unit Test Convention Plugin

```kotlin
class UnitTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies.apply {
                add("testImplementation", libs.findLibrary("junit").get())
                add("testImplementation", libs.findLibrary("mockk").get())
                add("testImplementation", libs.findLibrary("kotlinx.coroutines.test").get())
                add("testImplementation", libs.findLibrary("turbine").get())
            }

            tasks.withType<Test> {
                useJUnitPlatform()
            }
        }
    }
}
```

---

## Applying Plugins in Modules

Once registered, applying a convention plugin is a single line:

```kotlin
// feature/my-feature/build.gradle.kts
plugins {
    id("my.android.library")
    id("my.android.compose")
    id("my.android.unit.test")
}

dependencies {
    implementation(project(":component:my-component"))
}
```

Versus the alternative without convention plugins (~80 lines of manual config).

---

## Accessing the Version Catalog in Plugins

```kotlin
// Access libs.versions.toml from inside a plugin
val Project.libs get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

// Usage inside plugin
dependencies.add("implementation", libs.findLibrary("retrofit").get())
val kotlinVersion = libs.findVersion("kotlin").get().toString()
```

---

## Rules

| Rule                                                           | Why                                            |
|----------------------------------------------------------------|------------------------------------------------|
| NEVER duplicate SDK/Kotlin config in module `build.gradle.kts` | Causes drift and inconsistency                 |
| NEVER hardcode versions in plugin code                         | Use version catalog via `libs`                 |
| One plugin per concern                                         | Compose, UnitTest, Detekt are separate plugins |
| Shared logic in extension functions                            | Reuse across multiple plugins                  |
| Apply `pluginManager.apply()` not `apply(plugin = "...")`      | Type-safe, composable                          |

---

## When to Create a New Plugin

- You find yourself copy-pasting 3+ lines across multiple `build.gradle.kts`
- A new tool is added project-wide (Detekt, Jacoco, Paparazzi)
- A new module type is introduced (e.g., `integration`, `common-ui`)

---

## Checklist: New Convention Plugin

- [ ] Plugin class in `build-logic/convention/src/main/kotlin/`
- [ ] Registered in `build-logic/convention/build.gradle.kts` with stable ID
- [ ] No hardcoded versions — all from `libs.*`
- [ ] Shared logic extracted to extension functions
- [ ] Applied in at least one module to verify it works
- [ ] CI build passes

---

## References

- [Gradle Convention Plugins Guide](https://docs.gradle.org/current/samples/sample_convention_plugins.html)
- [Now in Android — build-logic example](https://github.com/android/nowinandroid/tree/main/build-logic)
- [Gradle Plugin Development](https://docs.gradle.org/current/userguide/custom_plugins.html)
