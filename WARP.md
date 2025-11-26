# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.
## Build, run, and verification commands
- Use the Gradle wrapper from the repo root: `./gradlew`.

### Full builds
- Build all modules (annotation, core, processors, sample):
  ```bash
  ./gradlew build
  ```

### Sample desktop application
- Run the Compose Desktop sample:
  ```bash
  ./gradlew :sample:run
  ```
  This triggers KSP processors in `processor:library` and `processor:application` and uses the generated navigation registries.

### Linting
Modules that use the custom build plugin automatically apply Detekt and KtLint.
- Run all lint checks:
  ```bash
  ./gradlew detekt ktlintCheck
  ```

### Tests
There are currently no tests in this repo, but standard Gradle test tasks are wired through the Kotlin Multiplatform/JVM tooling.
- Run all verification tasks (including tests where present):
  ```bash
  ./gradlew check
  ```
- Run tests for a single module (once tests exist). Example for `core` JVM tests:
  ```bash
  ./gradlew :core:jvmTest
  ```
- Run a single test method:
  ```bash
  ./gradlew :core:jvmTest --tests "com.pedrobneto.navigation.core.SomeTest.testCase"
  ```

## High-level architecture

### Overview
This project is a Kotlin Multiplatform navigation library built around JetBrains Compose Navigation (`navigation3`) with compile-time code generation via KSP. It is organized as:
- `annotation`: API annotations for navigation entries.
- `core`: runtime navigation primitives and Compose integration.
- `processor:library`: KSP processor that generates per-module navigation directions and registries.
- `processor:application`: KSP processor that aggregates all module registries into a global registry.
- `sample`: Compose Desktop sample app demonstrating how to declare routes, annotate destinations, and use the generated navigation layer.
- `kmp-build-plugin`: an internal Gradle plugin used to configure multiplatform targets, linting, and other build behavior.

### Runtime navigation core (`core` module)
Key types live in `core/src/commonMain/kotlin/com/pedrobneto/navigation/core`:
- `NavigationRoute`: marker interface implemented by all route types (e.g. `FirstScreenRoute`, `SecondScreenRoute`).
- `NavigationDirection`: abstract description of a destination, binding a `NavigationRoute` class to a `Draw(route: NavigationRoute)` composable plus an optional list of deeplinks.
- `DirectionRegistry`: simple container for a list of `NavigationDirection` instances belonging to a module.
- `NavigationController`:
  - Holds the back stack (`SnapshotStateList<NavigationRoute>`).
  - Exposes navigation operations:
    - `navigateTo(route: NavigationRoute, singleTop: Boolean = false)`.
    - `navigateTo(deeplink: String, singleTop: Boolean = false)` with URL-like normalization and query param parsing into route data classes via `kotlinx.serialization`.
    - `navigateUp()` and `popUpTo(direction: NavigationRoute, inclusive: Boolean = false)`.
  - Builds an `entryProvider` that maps each `NavigationRoute` class to the generated `NavigationDirection.Draw` function, which Compose Navigation uses to render scenes.
  - Is exposed via `LocalNavigationController` for use inside composables.
- `Navigation` composable:
  - Creates and provides a `NavigationController` (via `CompositionLocalProvider`).
  - Renders the current back stack using `NavDisplay` from `androidx.navigation3.ui`, configured with customizable transition specs and a `SceneStrategy`.
  - Accepts:
    - `initialRoute: NavigationRoute`.
    - `directionRegistries: List<DirectionRegistry>`.
    - Optional alignment, decorators, and transition configuration.

The runtime core is multiplatform and does not depend on Android-specific APIs; Android specifics (manifests, resources) are configured through the shared build plugin.

### Annotation API (`annotation` module)
`annotation/src/commonMain/kotlin/com/pedrobneto/navigation/annotation/NavigationEntry.kt` defines:
- `@NavigationEntry(route: KClass<*>, deeplinks: Array<String> = [])`:
  - Applied to composable functions that represent navigation destinations.
  - `route` points to a `NavigationRoute`-implementing type.
  - `deeplinks` lists URL-like strings (e.g. `"/first"`, `"/third?title=...&description=..."`) that map into route instances via query parameters.

This module is pure API and shared between library consumers and KSP processors.

### Code generation pipeline (`processor:library` and `processor:application`)
Navigation code is generated through two KSP processors, both run from the consuming module (in this repo, the `sample` module):
1. **Per-module generation (`processor:library`)**
   - `LibraryProcessor` scans for functions annotated with `@NavigationEntry`.
   - For each function:
     - Resolves the `route` class and any `deeplinks`.
     - Detects which parameter (if any) is of the route type to forward into the composable.
     - Generates:
       - A `*Direction` object in the route’s package extending `NavigationDirection`, wiring `Draw` to call the annotated composable.
       - A `<ModuleName>DirectionRegistry` in `com.pedrobneto.navigation.registry` listing all generated `*Direction` objects for that module.
   - Module naming is normalized (e.g., hyphens/underscores converted to camel case) to derive the registry class name.
2. **Global aggregation (`processor:application`)**
   - `ApplicationProcessor` expects a `navigation.rootDir` KSP option (set in `sample/build.gradle.kts` to the repo root).
   - It walks the file tree under that root looking for generated `*DirectionRegistry.kt` files in `build/generated/ksp/**/navigation/registry/`.
   - It parses their package and class names and generates a single `GlobalDirectionRegistry` object in `com.pedrobneto.navigation.registry` that:
     - Extends `DirectionRegistry`.
     - Flattens all module registries into one combined `directions` list.

The typical consumer pattern (shown in the `sample` app) is:
- Include both processors via KSP (`kspJvm(project(":processor:library"))` and `kspJvm(project(":processor:application"))`).
- Pass `navigation.rootDir` so the application processor can discover generated registries.

### Sample application (`sample` module)
The `sample` module is a Compose Desktop app that demonstrates the intended usage:
- Entry point (`sample/src/jvmMain/kotlin/Main.kt`):
  - Sets up logging (`Lumber.plant(DebugTree())`).
  - Launches a `Window` titled "Easy Navigation" and renders the `App()` composable.
- Routes (`sample/src/jvmMain/kotlin/com/pedrobneto/navigation/model`):
  - `FirstScreenRoute`: `data object` implementing `NavigationRoute`.
  - `SecondScreenRoute` and `ThirdScreenRoute`: `@Serializable` data classes implementing `NavigationRoute`, carrying the screen arguments that can be reconstructed from deeplink query parameters.
- UI and navigation usage (`sample/src/jvmMain/kotlin/com/pedrobneto/navigation/ui`):
  - `App`:
    - Uses `MaterialTheme` + `Scaffold`.
    - Obtains:
      - A module-specific `SampleDirectionRegistry` (generated by the library processor).
      - The global `GlobalDirectionRegistry` (generated by the application processor).
    - Calls `Navigation` with:
      - `initialRoute = FirstScreenRoute`.
      - `directionRegistries = listOf(GlobalDirectionRegistry)`.
  - Screen composables (`FirstScreenComposable`, `SecondScreenComposable`, `ThirdScreenComposable`):
    - Annotated with `@NavigationEntry`, each bound to a specific route and optional deeplinks.
    - Use `LocalNavigationController.current` to:
      - Navigate via route instances (e.g., `navigateTo(SecondScreenRoute(...))`).
      - Navigate via deeplink strings (e.g., `navigateTo("/third?title=...&description=...")`).
      - Pop the back stack either by route (`popUpTo(FirstScreenRoute)`) or by deeplink (`navigateTo("/first", singleTop = true)`).

The sample is the best reference for how library consumers should define routes, annotate destinations, and interact with the navigation controller.

### Custom build plugin (`kmp-build-plugin`)
The `kmp-build-plugin` composite project provides reusable Gradle plugins used throughout this repo:
- `plugin-multiplatform-library`:
  - Applies `com.android.library`, `org.jetbrains.kotlin.multiplatform`, and serialization plugins.
  - Configures KMP targets based on JSON configuration:
    - Reads `project-config.json` at the repo root and/or `module-config.json` in each module (e.g., `annotation/module-config.json`, `core/module-config.json`).
    - Fails the build with a clear error if no targets are configured.
  - For Android targets, standardizes `compileSdk`, `minSdk`, `buildToolsVersion`, and source sets.
  - Applies `plugin-lint` and `plugin-optimize` to enforce linting and dependency hygiene.
- `plugin-desktop-application`:
  - Sets up a JVM target with Compose Desktop (`org.jetbrains.compose` and Kotlin Compose compiler plugins).
  - Applies `plugin-lint` and `plugin-optimize`.
  - Used by the `sample` module, which then relies on Compose Desktop tasks such as `:sample:run`.
- `plugin-lint`:
  - Applies Detekt and KtLint plugins and wires them to shared configuration under the repo root (`tools/...` paths are referenced, even if not yet present).
  - Ensures Detekt runs on `src/main` sources, provides HTML/XML/TXT/SARIF/MD reports under each module’s `build/reports/detekt.*`.
- `plugin-test` (not currently applied in this repo’s modules):
  - Wires Jacoco and Kover coverage configuration and standard Android test options for applications/libraries.

Understanding these custom plugins is important when adding new modules: they encapsulate most of the Gradle boilerplate, and new modules are expected to opt into them and declare their targets via JSON configuration.

### Versioning and publishing
- `versioning.gradle.kts` derives the artifact `group` and `version`:
  - Prefers Gradle properties `GROUP` and `VERSION_NAME` when set.
  - Otherwise runs `git describe` once and caches the result in `build/version-name.txt`.
- Processor modules (`processor:application` and `processor:library`) use the Vanniktech Maven Publish plugin and are configured to publish to `mavenLocal()`. This is primarily relevant if you start consuming them from other projects.
