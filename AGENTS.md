# Agent Guidance

Use this file as the first stop for repository context. Keep future reads narrow: prefer the files listed here over broad scans, and update this file when module boundaries or verification commands change.

## Project Shape

Easy Navigation is a Kotlin Multiplatform navigation library for Compose Multiplatform, powered by KSP-generated navigation directions and registries.

Main modules:

- `core`: runtime navigation API, Compose integration, adaptive pane behavior, deeplink resolution, and controller/back stack logic.
- `processor`: JVM KSP processor that reads navigation annotations and generates `*Direction` and `*DirectionRegistry` code.
- `easy-navigation-gradle-plugin`: Gradle plugin that wires KSP options and processor dependencies into consumer modules.
- `sample:app`: shared Compose sample routes and screens.
- `sample:target:desktop`: Compose Desktop launcher for the sample app.
- `test`: shared test-only helpers, including coverage exclusions.
- `kmp-build-plugin`: included build that provides local convention plugins. In this checkout it is mostly consumed as an included build/built artifact; inspect it only when Gradle convention behavior is directly involved.

Key packages:

- `com.pedrobneto.easy.navigation.core`
- `com.pedrobneto.easy.navigation.core.annotation`
- `com.pedrobneto.easy.navigation.core.adaptive`
- `com.pedrobneto.easy.navigation.core.model`
- `com.pedrobneto.easy.navigation.processor.library`
- `com.pedrobneto.easy.navigation.plugin`
- `com.pedrobneto.easy.navigation.sample`

## Read These First

- Runtime behavior: `core/src/commonMain/kotlin/com/pedrobneto/easy/navigation/core/NavigationController.kt`, `Navigation.kt`, `model/*`, `adaptive/*`.
- Deeplink behavior: `core/src/commonMain/kotlin/com/pedrobneto/easy/navigation/core/model/NavigationDeeplink.kt` and its tests.
- KSP generation: `processor/src/main/kotlin/com/pedrobneto/easy/navigation/processor/library/LibraryProcessor.kt`, `_symbols.kt`, `_directionFile.kt`, `_registryFile.kt`, and `model/*`.
- Gradle plugin wiring: `easy-navigation-gradle-plugin/src/main/kotlin/com/pedrobneto/easy/navigation/plugin/BaseGradlePlugin.kt`.
- Sample usage: `sample/app/src/commonMain/kotlin/com/pedrobneto/easy/navigation/sample/model/_routes.kt` and `sample/app/src/commonMain/kotlin/com/pedrobneto/easy/navigation/sample/ui/SampleApp.kt`.
- Build setup: `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, and per-module `build.gradle.kts` files.

Avoid loading generated output under `build/`, Gradle caches, or the `kmp-build-plugin/build/` tree unless diagnosing generated code or build artifacts.

## Common Commands

Use the Gradle wrapper from the repo root.

```bash
./gradlew build
./gradlew check
./gradlew detekt ktlintCheck
./gradlew :core:jvmTest
./gradlew :processor:test
./gradlew :sample:target:desktop:run
```

For focused work, run the narrowest relevant task first:

- Core runtime change: `./gradlew :core:jvmTest`.
- Processor/code generation change: `./gradlew :processor:test`.
- Gradle plugin change: `./gradlew :easy-navigation-gradle-plugin:build`.
- Sample UI or integration change: `./gradlew :sample:target:desktop:run` or `./gradlew :sample:target:desktop:build`.
- Cross-module API change: `./gradlew check`.

## Coding Conventions

- Follow existing Kotlin style and package layout.
- Keep public runtime APIs in `core`; keep compile-time symbol processing in `processor`.
- Route types should implement `NavigationRoute` and normally be `@Serializable`.
- Destination composables are bound to routes with annotations from `core.annotation`.
- Preserve multiplatform boundaries: keep `commonMain` code platform-neutral unless the source set says otherwise.
- Prefer small, explicit helpers over broad abstractions in generated-code paths.
- Do not hand-edit generated KSP output; fix the generator or source annotations instead.

## Testing Notes

There are tests in this repo despite older docs suggesting otherwise:

- `core/src/commonTest/kotlin/...`
- `processor/src/test/kotlin/...`

Add or update tests near the affected behavior. For generated code, prefer processor tests that assert emitted source shape or generated registry/direction behavior. For navigation semantics, prefer `core` common tests.

## Context-Saving Rules

- Use `rg` and targeted file reads before opening whole directories.
- Read `README.md` for user-facing concepts, but treat source and tests as authoritative when docs drift.
- Do not inspect `build/` unless the task explicitly involves generated files, compiled artifacts, or Gradle output.
- Before touching Gradle convention behavior, check whether the behavior lives in this repo, the included `kmp-build-plugin`, or the published easy-navigation plugin.
- Keep edits scoped to the module that owns the behavior; sample changes should not be used to hide runtime or processor bugs.
