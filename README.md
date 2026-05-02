# Easy Navigation

Easy Navigation is a Kotlin Multiplatform navigation library for Compose Multiplatform. It sits on top of JetBrains Navigation3 and uses KSP to generate type-safe navigation directions and registries from annotated composable destinations.

The project currently focuses on:

- Type-safe routes modeled as `@Serializable` `NavigationRoute` types.
- Annotation-driven destination registration with `@Route`, `@Deeplink`, `@ParentRoute`, `@ParentDeeplink`, and `@Scope`.
- Generated `NavigationDirection` and `DirectionRegistry` objects, so app code does not need to hand-write entry providers.
- Adaptive single-pane and two-pane layouts through `@AdaptivePane`, `@ExtraPane`, `@SinglePane`, and `rememberAdaptiveSceneStrategies`.
- A small runtime API centered on `Navigation`, `NavigationController`, and `rememberNavigationController`.

## Repository layout

This repository is organized into these Gradle modules:

- `core`: runtime navigation API, Compose integration, Navigation3 entry wiring, adaptive pane behavior, deeplink resolution, and controller/back stack logic.
- `processor`: JVM KSP processor that reads Easy Navigation annotations and generates `*Direction` and `*DirectionRegistry` code.
- `easy-navigation-gradle-plugin`: Gradle plugin published as `io.github.pedro-bachiega.easy-navigation-library`; it wires the KSP processor into Kotlin and Kotlin Multiplatform modules.
- `sample:app`: shared Compose sample routes, destinations, and app shell.
- `sample:target:desktop`: Compose Desktop launcher for the sample app.
- `test`: shared test-only helpers, including coverage exclusions.
- `kmp-build-plugin`: included build with local convention plugins used by this repository.

For consumers, the important published pieces are:

- `io.github.pedro-bachiega:easy-navigation-core`
- `io.github.pedro-bachiega.easy-navigation-library`

## Core concepts

### Routes

Every destination is represented by a Kotlin type that implements `NavigationRoute`. Route types must be `@Serializable`, because the runtime uses Kotlin serialization for saved state, deeplink arguments, and route reconstruction.

```kotlin
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavigationRoute

@Serializable
data class DetailsRoute(val id: Long) : NavigationRoute

@Serializable
data object SettingsRoute : NavigationRoute
```

Routes without arguments can be `data object`s. Routes with arguments should usually be `data class`es.

### Destinations

Composable destinations are connected to routes with `@Route`.

```kotlin
import androidx.compose.runtime.Composable
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.Route

@Route(HomeRoute::class)
@Deeplink("/home")
@Composable
fun HomeScreen() {
    // ...
}

@Route(DetailsRoute::class)
@Deeplink("/details/{id}")
@Composable
fun DetailsScreen(route: DetailsRoute) {
    // route.id comes from type-safe navigation or from the deeplink path
}
```

If a destination function has parameters, exactly one parameter must match the route type declared in `@Route`.

### Deeplinks and parent navigation

`@Deeplink` registers URI patterns for a destination. Placeholders such as `{id}` are resolved into route properties.

`@ParentRoute` and `@ParentDeeplink` define where "up" navigation should go when a destination is the root of its current back stack.

```kotlin
@Route(DetailsRoute::class)
@Deeplink("/details/{id}")
@ParentRoute(HomeRoute::class)
@Composable
fun DetailsScreen(route: DetailsRoute) {
    // ...
}
```

### Registries

The KSP processor generates:

- One `*Direction` object per `@Route` destination.
- A module registry for unscoped destinations, named from the module, such as `AppDirectionRegistry`.
- Scope registries for destinations annotated with `@Scope("name")`, such as `NameDirectionRegistry`.

Generated registries live in `com.pedrobneto.easy.navigation.registry`. Pass one or more registries to `Navigation` or `rememberNavigationController`.

```kotlin
import com.pedrobneto.easy.navigation.registry.AppDirectionRegistry

val registries = remember { listOf(AppDirectionRegistry) }
```

### Adaptive panes

Destinations default to `PaneStrategy.Adaptive`, which can participate in adaptive layouts. You can make that explicit, force full-screen behavior, or declare an extra pane hosted by another route.

```kotlin
import com.pedrobneto.easy.navigation.core.adaptive.AdaptivePane
import com.pedrobneto.easy.navigation.core.adaptive.ExtraPane
import com.pedrobneto.easy.navigation.core.adaptive.SinglePane

@AdaptivePane(ratio = 0.3f)
@Route(HomeRoute::class)
@Composable
fun HomeScreen() = Unit

@ExtraPane(host = HomeRoute::class, ratio = 0.7f)
@Route(DetailsRoute::class)
@Composable
fun DetailsScreen(route: DetailsRoute) = Unit

@SinglePane
@Route(SettingsRoute::class)
@Composable
fun SettingsScreen() = Unit
```

Enable adaptive behavior by passing adaptive scene strategies to `Navigation`.

```kotlin
import com.pedrobneto.easy.navigation.core.adaptive.rememberAdaptiveSceneStrategies

Navigation(
    modifier = Modifier.fillMaxSize(),
    initialRoute = HomeRoute,
    directionRegistries = registries,
    sceneStrategies = rememberAdaptiveSceneStrategies()
)
```

`rememberAdaptiveSceneStrategies` also accepts options such as `isUsingAdaptiveLayout` and `orientation`, which the sample app derives from the current window state.

### Navigation controller

`NavigationController` owns the current back stack and exposes route and deeplink navigation.

```kotlin
val navigation = LocalNavigationController.current

navigation.navigateTo(DetailsRoute(id = 123L))
navigation.safeNavigateTo("/details/123")
navigation.safeNavigateUp()
navigation.popUpTo(HomeRoute)
```

Unsafe APIs throw when navigation cannot be completed. Safe APIs wrap the operation and return `Boolean`.

Available launch strategies are:

- `LaunchStrategy.Default`: push a new route.
- `LaunchStrategy.SingleTop(clearTop = true)`: reuse or replace existing destinations of the same route class.
- `LaunchStrategy.NewStack`: clear the current stack and make the new route the root.

## Installation

Replace `<latest_version>` with the version you want to use.

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    id("io.github.pedro-bachiega.easy-navigation-library") version "<latest_version>"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.pedro-bachiega:easy-navigation-core:<latest_version>")
        }
    }
}
```

The Easy Navigation Gradle plugin expects the KSP plugin to be applied in the module. It adds the Easy Navigation KSP processor to the appropriate KSP configurations and exposes generated common metadata sources for multiplatform projects.

## Minimal app setup

```kotlin
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.pedrobneto.easy.navigation.core.Navigation
import com.pedrobneto.easy.navigation.core.adaptive.rememberAdaptiveSceneStrategies
import com.pedrobneto.easy.navigation.registry.AppDirectionRegistry

@Composable
fun App() {
    val registries = remember { listOf(AppDirectionRegistry) }

    MaterialTheme {
        Navigation(
            modifier = Modifier.fillMaxSize(),
            initialRoute = HomeRoute,
            directionRegistries = registries,
            sceneStrategies = rememberAdaptiveSceneStrategies()
        )
    }
}
```

For apps that own the back stack outside `Navigation`, create it explicitly and pass a controller:

```kotlin
val backStack = rememberNavBackStack(
    initialRoute = HomeRoute,
    registries = registries
)
val controller = rememberNavigationController(
    initialRoute = HomeRoute,
    directionRegistries = registries,
    backStack = backStack
)

Navigation(
    modifier = Modifier.fillMaxSize(),
    initialRoute = HomeRoute,
    directionRegistries = registries,
    controller = controller
)
```

Nested graphs use the same pattern: create a child controller with its own initial route and pass it to a nested `Navigation` composable.

## Code generation

During KSP processing, Easy Navigation:

1. Finds functions annotated with `@Route`, `@Deeplink`, `@ParentRoute`, or `@ParentDeeplink`.
2. Validates that each destination is `@Composable`.
3. Validates that the declared route implements `NavigationRoute` and is `@Serializable`.
4. Reads deeplinks, parent route/deeplink metadata, scopes, and pane annotations.
5. Generates an internal `*Direction` object beside the route package.
6. Generates unscoped module registries and scoped registries in `com.pedrobneto.easy.navigation.registry`.

Do not edit generated KSP output directly. Change annotations, route types, or the processor instead.

## Running the sample

From the repository root:

```bash
./gradlew :sample:target:desktop:run
```

The sample demonstrates:

- Module registry usage through `AppDirectionRegistry`.
- Route and deeplink navigation.
- Adaptive list/detail panes.
- A nested detail graph.
- A controller-owned app shell with saveable back stack state.

## Development commands

Use the Gradle wrapper from the repository root.

```bash
./gradlew build
./gradlew check
./gradlew detekt ktlintCheck
./gradlew :core:jvmTest
./gradlew :processor:test
./gradlew :easy-navigation-gradle-plugin:build
./gradlew :sample:target:desktop:build
```

For focused work, prefer the narrowest relevant task first:

- Runtime/navigation behavior: `./gradlew :core:jvmTest`
- KSP generation: `./gradlew :processor:test`
- Gradle plugin wiring: `./gradlew :easy-navigation-gradle-plugin:build`
- Sample integration: `./gradlew :sample:target:desktop:build`
