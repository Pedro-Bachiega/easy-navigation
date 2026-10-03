# Agent Guidance

Use this file as the first stop for repository context. Keep future reads narrow: prefer the files listed here over broad scans, and update this file when module boundaries or verification commands change.

## Project Shape

Easy Navigation is a Kotlin Multiplatform navigation library for Compose Multiplatform, powered by Kotlin compiler-generated navigation directions and registries.

Main modules:

- `core`: runtime navigation API, Compose integration, adaptive pane behavior, deeplink resolution, and controller/back stack logic.
- `compiler-plugin`: isolated compiler PSI source generator plus FIR semantic validation; generates complete `*Direction` and `*DirectionRegistry` sources.
- `easy-navigation-gradle-plugin`: Gradle plugin that derives source ownership from the KGP graph, wires generation and compiler artifacts, and registers generated IDE source roots.
- `sample:app`: shared Compose sample routes and screens.
- `sample:feature` and `sample:consumer`: non-published multiplatform compiler verification fixtures.
- `sample/published-consumer`: standalone, non-published consumer of library artifacts staged by CI.
- `sample:target:desktop`: Compose Desktop launcher for the sample app.
- `test`: shared test-only helpers, including coverage exclusions.
- `build-logic`: included build that provides local convention plugins for this repository.

Key packages:

- `com.pedrobneto.easy.navigation.core`
- `com.pedrobneto.easy.navigation.core.annotation`
- `com.pedrobneto.easy.navigation.core.adaptive`
- `com.pedrobneto.easy.navigation.core.model`
- `com.pedrobneto.easy.navigation.compiler`
- `com.pedrobneto.easy.navigation.plugin`
- `com.pedrobneto.easy.navigation.sample`

## Read These First

- Runtime behavior: `core/src/commonMain/kotlin/com/pedrobneto/easy/navigation/core/NavigationController.kt`, `Navigation.kt`, `model/*`, `adaptive/*`.
- Deeplink behavior: `core/src/commonMain/kotlin/com/pedrobneto/easy/navigation/core/model/NavigationDeeplink.kt` and its tests.
- Compiler generation: `compiler-plugin/src/main/kotlin/com/pedrobneto/easy/navigation/compiler/SourceReader.kt`, `SourceWriter.kt`, `GenerationMain.kt`, and `NavigationCompilerPlugin.kt`.
- Gradle plugin wiring: `easy-navigation-gradle-plugin/src/main/kotlin/com/pedrobneto/easy/navigation/plugin/BaseGradlePlugin.kt`.
- Sample usage: `sample/app/src/commonMain/kotlin/com/pedrobneto/easy/navigation/sample/model/_routes.kt` and `sample/app/src/commonMain/kotlin/com/pedrobneto/easy/navigation/sample/ui/SampleApp.kt`.
- Build setup: `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, and per-module `build.gradle.kts` files.

Avoid loading generated output under `build/`, Gradle caches, or the `build-logic/build/` tree unless diagnosing generated code or build artifacts.

## Common Commands

Use the Gradle wrapper from the repo root.

```bash
./gradlew build
./gradlew check
./gradlew detekt ktlintCheck
./gradlew :core:jvmTest
./gradlew :compiler-plugin:test
./gradlew :sample:target:desktop:run
```

For focused work, run the narrowest relevant task first:

- Core runtime change: `./gradlew :core:jvmTest`.
- Processor/code generation change: `./gradlew :compiler-plugin:test`.
- Gradle plugin change: `./gradlew :easy-navigation-gradle-plugin:build`.
- Sample UI or integration change: `./gradlew :sample:target:desktop:run` or `./gradlew :sample:target:desktop:build`.
- Cross-module API change: `./gradlew check`.

## Coding Conventions

- Follow existing Kotlin style and package layout.
- Keep public runtime APIs in `core`; keep compile-time symbol processing in `compiler-plugin`.
- Route types should implement `NavigationRoute` and normally be `@Serializable`.
- Destination composables are bound to routes with annotations from `core.annotation`.
- Preserve multiplatform boundaries: keep `commonMain` code platform-neutral unless the source set says otherwise.
- Prefer small, explicit helpers over broad abstractions in generated-code paths.
- Do not hand-edit generated compiler output; fix the generator or source annotations instead.

## Testing Notes

There are tests in this repo despite older docs suggesting otherwise:

- `core/src/commonTest/kotlin/...`
- `compiler-plugin/src/test/kotlin/...`

Add or update tests near the affected behavior. For generated code, prefer compiler generation and consumer tests that assert emitted source shape or generated registry/direction behavior. For navigation semantics, prefer `core` common tests.

## Context-Saving Rules

- Use `rg` and targeted file reads before opening whole directories.
- Read `README.md` for user-facing concepts, but treat source and tests as authoritative when docs drift.
- Do not inspect `build/` unless the task explicitly involves generated files, compiled artifacts, or Gradle output.
- Before touching Gradle convention behavior, check whether the behavior lives in `build-logic` or the published easy-navigation plugin.
- Keep edits scoped to the module that owns the behavior; sample changes should not be used to hide runtime or processor bugs.

## Compiler migration verification

- Kotlin support is pinned to 2.4.20; upgrades require a compatibility CI run.
- `sample:feature` has shared and platform destinations in custom source directories and tests
  generated behavior; `sample:consumer` consumes its public registry without the compiler plugin.
- No sample module applies publishing plugins. CI stages only library artifacts in its temporary repository.
- Validate with CI only when instructed by the user; do not run local builds/tests in this migration.
- PR CI compiles common metadata/JVM/Android and links iOS frameworks on macOS.
- Code is generated before compilation using compiler PSI. FIR validates semantic route requirements
  in the normal compilation. No IR body generation is needed; Compose/Serialization compile the sources.
