# Easy Navigation

Easy Navigation is a Kotlin Multiplatform navigation library for Compose Multiplatform. It sits on top of JetBrains Navigation3 and uses a Kotlin compiler plugin to generate type-safe navigation directions and registries from annotated composable destinations.

The project currently focuses on:

- Type-safe routes modeled as `@Serializable` `NavigationRoute` types.
- Annotation-driven destination registration with `@Route`, `@Deeplink`, `@ParentRoute`, `@ParentDeeplink`, and `@Scope`.
- Generated `NavigationDirection` and `DirectionRegistry` objects, so app code does not need to hand-write entry providers.
- Adaptive single-pane and two-pane layouts through `@AdaptivePane`, `@ExtraPane`, `@SinglePane`, and `rememberAdaptiveSceneStrategies`.
- A small runtime API centered on `Navigation`, `NavigationController`, and `rememberNavigationController`.

## Repository layout

This repository is organized into these Gradle modules:

- `core`: runtime navigation API, Compose integration, Navigation3 entry wiring, adaptive pane behavior, deeplink resolution, and controller/back stack logic.
- `compiler-plugin`: isolated Kotlin compiler source generator and FIR validator that reads Easy Navigation annotations and generates `*Direction` and `*DirectionRegistry` code.
- `easy-navigation-gradle-plugin`: Gradle plugin published as `io.github.pedro-bachiega.easy-navigation-library`; it wires generation and FIR validation into Kotlin and Kotlin Multiplatform modules.
- `sample:app`: app shell aggregating feature registries without the Easy Navigation compiler plugin.
- `sample:feature`: sample routes, destinations and compiler verification fixtures; generates registries and is never published.
- `sample/published-consumer`: standalone consumer used to verify library artifacts staged by CI; never published.
- `sample:target:desktop`: Compose Desktop launcher for the sample app.
- `test`: shared test-only helpers, including coverage exclusions.
- `build-logic`: included build with local convention plugins used by this repository.

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

A destination may have at most one parameter matching the route type in `@Route`. Other parameters must have defaults or be varargs. Destinations must be accessible top-level composable functions without receivers, context parameters, or type parameters.

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

The compiler generator produces:

- One `*Direction` object per `@Route` destination.
- A module registry for unscoped destinations, named from the module, such as `FeatureDirectionRegistry`.
- Scope registries for destinations annotated with `@Scope("name")`, such as `NameDirectionRegistry`.

Generated registries live in `com.pedrobneto.easy.navigation.registry`. Pass one or more registries to `Navigation` or `rememberNavigationController`.

```kotlin
import com.pedrobneto.easy.navigation.registry.FeatureDirectionRegistry

val registries = remember { listOf(FeatureDirectionRegistry) }
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

### Modal destinations

Mark a destination with `@Modal` to render it above the complete previous scene. The route remains
in the same back stack. The modal component owns its scrim, animations, and dismissal behavior; it
can use `LocalModalScope` to navigate after its own exit animation completes.

```kotlin
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.core.modal.LocalModalScope
import com.pedrobneto.easy.navigation.core.modal.Modal

@Modal
@Route(CheckoutConfirmationRoute::class)
@Composable
fun CheckoutConfirmationScreen() {
    val modal = LocalModalScope.current

    ConfirmationCard(
        onConfirm = { modal.navigateUp() },
        onCancel = { modal.navigateUp() },
    )
}
```

The modal composable owns its surface, sizing, alignment, scrim, entry and exit animations, and
dismissal behavior. If it animates out before closing, call `modal.navigateUp()` after its exit
animation completes. Register the component's system-back behavior with
`modal.setSystemBackRequestHandler { ... }` so it can use that same animated close path. The library
provides the overlay layer and keeps the scene below intact. Modal destinations cannot be combined
with `@AdaptivePane`, `@SinglePane`, or `@ExtraPane`.

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

Easy Navigation supports Kotlin **2.4.20**. Use the same version for Kotlin, Compose Compiler,
and the Kotlin Serialization compiler plugin. The existing Easy Navigation plugin automatically
resolves `io.github.pedro-bachiega:easy-navigation-compiler-plugin` at its own version; consumers
do not declare a compiler dependency or configure a generation DSL.

### Migrating from KSP

Update Kotlin and Easy Navigation, then remove the Easy Navigation KSP processor dependency,
KSP arguments, and `build/generated/ksp` source-directory wiring. Remove the KSP plugin only
if no other library needs it. Keep route annotations, generated imports, and `listOf(...)`
registry composition unchanged. Recompile dependent libraries with the supported Kotlin version.

The `generateEasyNavigation` task writes complete Kotlin files into
`build/generated/easyNavigation/kotlin/<source-set>`. Compilation and Kotlin IDE import preparation
depend on generation; you can also run the task explicitly after editing annotations. IntelliJ IDEA
and Android Studio index these real Kotlin source roots without an Easy Navigation IDE extension.
This does not promise live updates while typing or a custom generated-body preview.

### Platform registries

The plugin follows the configured Kotlin `dependsOn` graph and source directories, including
intermediate and custom source sets. A shared registry is an automatically generated `expect object`;
its platform `actual data object` combines shared and platform-specific destinations from that
module. Shared directions remain in their declaring source set. Scope registries follow the same
composition rules. Registries do not discover destinations from dependencies: applications still
combine the public registries of their feature modules explicitly.

Android, JVM, iOS ARM64, and iOS Simulator ARM64 are the supported initial targets. Other Kotlin
versions and backends are outside the initial compatibility guarantee.

## Minimal app setup

```kotlin
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.pedrobneto.easy.navigation.core.Navigation
import com.pedrobneto.easy.navigation.core.adaptive.rememberAdaptiveSceneStrategies
import com.pedrobneto.easy.navigation.registry.FeatureDirectionRegistry

@Composable
fun App() {
    val registries = remember { listOf(FeatureDirectionRegistry) }

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
    directionRegistries = registries,
    backStack = backStack
)

Navigation(
    modifier = Modifier.fillMaxSize(),
    controller = controller
)
```

Nested graphs use the same pattern: create a child controller with its own initial route and pass it to a nested `Navigation` composable.

### Initial deeplinks

`Navigation`, `rememberNavigationController`, and `rememberNavBackStack` accept either a
`NavigationRoute` or a deeplink `String` as `initialRoute`. Deeplinks use the same resolution,
JSON configuration, and serializable payload support as `NavigationController.navigateTo`.

```kotlin
Navigation(
    modifier = Modifier.fillMaxSize(),
    initialRoute = "/details/123",
    directionRegistries = registries,
)

val controller = rememberNavigationController(
    initialRoute = "/details/123",
    payload = DetailsPayload(source = "notification"),
    directionRegistries = registries,
)

val backStack = rememberNavBackStack(
    initialRoute = "/details/123",
    registries = registries,
)
```

The optional `payload` must serialize to a JSON object, just as for controller navigation.
Payload arguments override query and path arguments. Deeplink overloads accept `json` for
custom deserialization; controller initialization uses that same instance for later navigation.
Invalid deeplinks, unmatched destinations, and incompatible arguments propagate resolution errors.
Initial routes and payloads apply only when creating a new stack. Recomposition does not navigate
or reset the stack, and restored state takes precedence without resolving the initial deeplink.
Use `controller.navigateTo(...)` for deeplinks received after initialization.

When passing a controller to `Navigation`, omit `initialRoute` and `directionRegistries`.
When passing a back stack to `rememberNavigationController`, omit `initialRoute` and keep
`directionRegistries` for destination registration. The previous signatures accepting both
initial state and an existing controller or back stack have been removed.

### Navigation transitions

Regular scene animations are configured through `NavigationTransitions`. Modal animations belong
to each modal component because different components can use different scrims, transitions, and
dismissal rules.

```kotlin
Navigation(
    modifier = Modifier.fillMaxSize(),
    initialRoute = HomeRoute,
    directionRegistries = registries,
    transitions = NavigationTransitions(
        regular = DefaultRegularSceneTransitions,
    )
)
```

Each route can provide its own policy without being referenced by the module that configures
`Navigation`. Shared interfaces extending `NavigationRoute` can identify flows across features:

```kotlin
interface AuthenticatedRoute : NavigationRoute

@Serializable
data object LoginRoute : NavigationRoute {
    override fun transitions(): SceneTransitions = LoginTransitions
}

object LoginTransitions : SceneTransitions {
    override val transitionSpec: DefaultTransitionSpec = { context ->
        if (context.from is AuthenticatedRoute &&
            context.operation == NavigationOperation.NewStack
        ) {
            fadeIn() togetherWith fadeOut()
        } else {
            null // Use the app's configured global policy.
        }
    }
}

controller.navigateTo(LoginRoute, strategy = LaunchStrategy.NewStack)
```

Callbacks receive a `RouteTransitionContext` with non-null `from` and `to` routes, original
`fromScene` and `toScene` instances (after consumer decorators), and `operation`. Routes are the
logical top of the stack at each end, not the last element of `Scene.entries`. This works with
multi-pane and custom scenes, a replaced stack, and multi-entry pops. Predictive back uses the
projected destination before the gesture is committed, and retains its policy and swipe edge
while completing or cancelling the gesture.

Forward navigation, `SingleTop`, and `NewStack` use the destination route's policy. Pop and
predictive pop use the departing route's policy. Each callback independently falls back from
the route to `NavigationTransitions.regular`, then to `DefaultRegularSceneTransitions` when
absent or returning null. Predictive pop never inherits a custom regular pop callback.
Returning `DefaultRegularSceneTransitions.none()` explicitly disables animation instead of
requesting a fallback.

The operation distinguishes `Forward`, `SingleTop`, `NewStack`, `Pop`, `PredictivePop`, and
`SceneChange`. Layout changes can have `from == to` and still run the callback. External stack
edits or mutations coalesced before display use `Unknown`; they do not guess controller intent.
Initial presentation and restoration do not invoke route or global transition callbacks.
Modal overlays retain their own animations. Navigation 3 scene metadata transition overrides
retain their existing precedence over the display's regular callbacks.

The built-in policy is now a stateless object with public helpers. Use `fullSlideIn()` or
`fullSlideOut()` to slide the entire scene. Use `transitionTo` or `popTo` to preserve the adaptive
pane behavior:

```kotlin
override val transitionSpec: DefaultTransitionSpec = { context ->
    when (context.from) {
        is AuthenticatedRoute -> fadeIn() togetherWith fadeOut()
        else -> with(DefaultRegularSceneTransitions) {
            context.fromScene transitionTo context.toScene
        }
    }
}
```

This is a breaking API change: remove `()` from `DefaultRegularSceneTransitions()` and update
custom callback signatures to accept context. Predictive callbacks receive `(context, swipeEdge)`.
All three callback properties are optional and callback results are now `ContentTransform?`.

## Code generation

During source generation and Kotlin compilation, Easy Navigation:

1. Finds functions annotated with `@Route`, `@Deeplink`, `@ParentRoute`, or `@ParentDeeplink`.
2. Validates that each destination is `@Composable`.
3. Validates that the declared route implements `NavigationRoute` and is `@Serializable`.
4. Reads deeplinks, parent route/deeplink metadata, scopes, and pane annotations.
5. Generates an internal `*Direction` object beside the route package.
6. Generates unscoped module registries and scoped registries in `com.pedrobneto.easy.navigation.registry`.

Do not edit generated output directly. Change annotations, route types, or the compiler generator instead.

## Running the sample

From the repository root:

```bash
./gradlew :sample:target:desktop:run
```

The sample demonstrates:

- Module registry usage through `FeatureDirectionRegistry`.
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
./gradlew :compiler-plugin:test
./gradlew :easy-navigation-gradle-plugin:build
./gradlew :sample:target:desktop:build
```

For focused work, prefer the narrowest relevant task first:

- Runtime/navigation behavior: `./gradlew :core:jvmTest`
- Compiler generation: `./gradlew :compiler-plugin:test`
- Gradle plugin wiring: `./gradlew :easy-navigation-gradle-plugin:build`
- Sample integration: `./gradlew :sample:target:desktop:build`
