# Easy Navigation

A type-safe, declarative, and boilerplate-free navigation library for Compose Multiplatform, built on top of JetBrains' `navigation-compose` and powered by KSP.

The goal is to make navigation in Compose-based apps simple and robust by:
- Representing each destination as a `@Serializable` `NavigationRoute` Kotlin type.
- Generating navigation logic from simple annotations like `@Route`, `@Deeplink`, and `@Scope`.
- Providing built-in support for adaptive master-detail layouts using `@AdaptivePane`, `@ExtraPane`, and `@SinglePane` annotations.
- Offering a concise runtime (`NavigationController` + `Navigation` composable) to manage the back stack and navigation lifecycle.

## Modules overview

This repository is organized into the following Gradle modules:
- `core` – Contains the runtime navigation primitives, including `NavigationController`, `LaunchStrategy`, adaptive layout strategies, and the `Navigation` composable.
- `easy-navigation-gradle-plugin` – Contains the `easy-navigation-application` and `easy-navigation-library` plugins, which simplify the integration of the KSP processors.
- `processor:library` – A KSP processor that generates per-module `*Direction` classes and `*DirectionRegistry` objects from your annotations.
- `processor:application` – A KSP processor that aggregates all module registries into a single `GlobalDirectionRegistry` for the application.
- `sample` – A Compose for Desktop sample app that demonstrates how to define routes, annotate destinations, and wire up navigation.
- `kmp-build-plugin` – An internal Gradle plugin for configuring multiplatform targets, linting, and other common build behaviors.

For consumers of the library, the key modules are:
- `core`
- `easy-navigation-gradle-plugin`

## Concepts

### NavigationRoute

Every destination in your app is represented by a type that implements the `NavigationRoute` marker interface.

- Routes can be `data object` singletons for destinations without arguments.
- For destinations with arguments, use a `@Serializable data class`. The properties of the data class become the arguments for the destination and can be populated from deep link query or path parameters.

Example (from the sample app):

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

These types provide compile-time safety for navigation calls and are used by the generated code to link destinations to their composable implementations.

### Scoping Destinations

The library supports organizing destinations into scopes, which are essentially independent navigation graphs. This is useful for creating nested navigation flows or separating features.

- **@Scope("scopeName")**: Assigns a destination to a specific scope. The application processor will generate a `ScopeNameDirectionRegistry` containing all destinations with that scope.
- **@GlobalScope**: A convenience annotation that is equivalent to `@Scope("global")`. It assigns a destination to the global navigation graph.

By default, if no scope is specified, a destination is only added to its local module registry. To make it accessible from other modules, you must assign it to a scope.

### Adaptive Layouts and Panes

Easy Navigation provides built-in support for creating adaptive UIs that can display one or two panes of content, which is ideal for tablets, foldables, and desktop applications. This is controlled by the `PaneStrategy` and a set of annotations.

#### Pane Strategy

The layout behavior of a destination is determined by its `PaneStrategy`, which is generated based on the annotations you use:
- **`PanelStrategy.Adaptive`**: The default behavior. The destination can host an extra pane alongside it.
- **`PanelStrategy.Single`**: The destination will always be displayed in a single panel, occupying the full width.
- **`PanelStrategy.Extra`**: The destination will be displayed in an extra panel alongside a primary one, creating a two-pane layout. You must specify the `host` route that the extra pane is associated with.

You declare the desired strategy using the following annotations on your route composables.

#### Pane Annotations

-   **`@AdaptivePane`**: Marks a destination that can host an extra pane alongside it. This is the default if no other pane annotation is used.
-   **`@SinglePane`**: Forces a destination to always be displayed as a single, full-width pane.
-   **`@ExtraPane(host = HomeRoute::class, ratio = 0.5f)`**: Marks a destination as an extra pane that should appear alongside its `host`. The `ratio` determines the width fraction it occupies.

#### AdaptiveSceneStrategy

To enable this functionality, you must provide the `Navigation` composable with an `AdaptiveSceneStrategy`. This strategy automatically calculates whether to show a single pane or a dual-pane layout based on the annotations of the current and previous destinations.

```kotlin
import com.pedrobneto.easy.navigation.core.adaptive.AdaptiveSceneStrategy

Navigation(
    // ...
    sceneStrategy = AdaptiveSceneStrategy(),
)
```

### Safe vs. Unsafe Navigation

The navigation controller offers two styles for invoking navigation actions that can potentially fail:
- **Unsafe API**: Functions like `navigateTo(deeplink: String)` or `navigateUp()`. These methods will throw an exception if the navigation action cannot be performed.
- **Safe API**: Functions like `safeNavigateTo(deeplink: String)` or `safeNavigateUp()`. These methods wrap the unsafe calls in a `runCatching` block and return a `Boolean` indicating success or failure.

### NavigationDirection and DirectionRegistry

You never have to write navigation graph code by hand. Instead, the library **generates** it:

- A `*Direction` object for each composable annotated with `@Route`.
- A `*DirectionRegistry` object for each module and for each scope, which lists all the directions within that module or scope.

A `NavigationDirection` knows the route's class, its deeplinks, its pane strategy, and how to draw its composable content. A `DirectionRegistry` is a simple container for a list of these directions. The `NavigationController` uses these registries to build a complete map of the app's navigation graph.

### NavigationController and Navigation composable

- `NavigationController` maintains the back stack and is provided to the composable tree via `LocalNavigationController`.
- It exposes powerful operations for navigation, available in two styles: `unsafe` (throws on error) and `safe` (returns `Boolean`).

**Unsafe API**
  - `navigateTo(route: NavigationRoute, strategy: LaunchStrategy = ...)`
  - `navigateTo(deeplink: String, strategy: LaunchStrategy = ...)`
  - `navigateUp()`
  - `popUpTo(direction: NavigationRoute, inclusive: Boolean = false)`

**Safe API**
  - `safeNavigateTo(deeplink: String, strategy: LaunchStrategy = ...)`
  - `safeNavigateUp()`
  - `safePopUpTo(direction: NavigationRoute, inclusive: Boolean = false)`

The `Navigation` composable is the root of the navigation system:

```kotlin
@Composable
fun Navigation(
    modifier: Modifier,
    initialRoute: NavigationRoute,
    directionRegistries: List<DirectionRegistry>,
    // Optional parameters for adaptive layouts, transitions, etc.
    sceneStrategy: SceneStrategy<NavigationRoute> = ...,
    // ... and more
)
```

It creates the `NavigationController`, provides it to the UI tree, and renders the current destination using `NavHost`.

### LaunchStrategy

The `LaunchStrategy` sealed class controls how `navigateTo` operations affect the back stack:
- `LaunchStrategy.NewTask(clearStack: Boolean)`: Pushes a new task, optionally clearing the existing stack.
- `LaunchStrategy.SingleTop(clearTop: Boolean)`: If the destination is already on top, it's replaced. If it's in the back stack, pops everything above it.

## How code generation works

Navigation code is generated in two stages using KSP.

### 1. Per-module generation (processor:library)

You annotate your destination composables with `@Route` and other optional annotations:

```kotlin
@Composable
@Route(DetailsRoute::class)
@Deeplink("/details/{id}")
@GlobalScope
@ExtraPane(host = HomeRoute::class, ratio = 0.4f) // Appears next to HomeRoute
fun DetailsScreen(route: DetailsRoute) {
    /* ... */
}
```

For each annotated function, the **library processor**:

1. Reads the `route` KClass (`DetailsRoute::class`).
2. Extracts any `@Deeplink` annotations.
3. Extracts any scopes (`@GlobalScope`, `@Scope("...")`).
4. Extracts any pane strategy annotations (`@AdaptivePane`, `@SinglePane`, `@ExtraPane`).
5. Generates a `*Direction` object and adds it to the appropriate module and scope registries.

### 2. Global aggregation (processor:application)

The **application processor** runs in your main app module, scanning for all generated registries and aggregating them into a single `GlobalDirectionRegistry` and other scope-specific registries. This allows your app to reference a single registry without needing to know about individual feature modules.

## Using easy-navigation in your project

> Note: Replace `<latest_version>` with the appropriate library version.

### 1. Add the Gradle plugin

In your `settings.gradle.kts`, add the repository to `pluginManagement`:

```kotlin
pluginManagement {
    repositories {
        // ...
        maven("https://oss.sonatype.org/content/repositories/snapshots") // or Maven Central for stable releases
    }
}
```

Then, apply the appropriate plugin in your module-level `build.gradle.kts`:

**For library modules:**
```kotlin
plugins {
    id("io.github.pedro-bachiega.easy-navigation-library")
}
```

**For your application module:**
```kotlin
plugins {
    id("io.github.pedro-bachiega.easy-navigation-application")
}
```

### 2. Add dependencies

In your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.pedro-bachiega:easy-navigation-core:<latest_version>")
}
```

### 3. Define your routes

Create your `@Serializable` route types that implement `NavigationRoute`.

### 4. Annotate your composable destinations

Mark each screen with `@Route` and other annotations to define its behavior.

**Example 1: A standard, adaptive screen**

```kotlin
@Composable
@Route(HomeRoute::class)
@Deeplink("/home")
@GlobalScope
@AdaptivePane // Can host an extra pane
fun HomeScreen() { /* ... */ }
```

**Example 2: A screen as an extra pane for a two-pane layout**

```kotlin
@Composable
@Route(DetailsRoute::class)
@Deeplink("/details/{id}")
@GlobalScope
@ExtraPane(host = HomeRoute::class, ratio = 0.4f) // Appears next to HomeRoute
fun DetailsScreen(route: DetailsRoute) {
    // This screen will take 40% of the width when shown next to HomeRoute
}
```

### 5. Set up the Navigation root

In your main `App` composable, wrap your UI in the `Navigation` composable. To enable adaptive layouts, provide the `AdaptiveSceneStrategy`.

```kotlin
import com.pedrobneto.easy.navigation.core.adaptive.AdaptiveSceneStrategy
import com.pedrobneto.easy.navigation.registry.GlobalDirectionRegistry // Generated

@Composable
fun App() {
    MaterialTheme {
        Navigation(
            modifier = Modifier.fillMaxSize(),
            initialRoute = HomeRoute,
            directionRegistries = remember { listOf(GlobalDirectionRegistry) },
            sceneStrategy = AdaptiveSceneStrategy() // Enable adaptive layouts
        )
    }
}
```

### 6. Navigating between screens

Access the `NavigationController` from any composable within the `Navigation` hierarchy and use its methods to navigate.

```kotlin
val navigation = LocalNavigationController.current

// Navigate with a type-safe route
navigation.navigateTo(DetailsRoute(id = 123L))

// Navigate with a deeplink string
navigation.safeNavigateTo("/details/123")

// Navigate up
navigation.safeNavigateUp()
```

## Running the sample app

From the repository root:
```bash
./gradlew :sample:run
```

## Building and linting
```bash
# Build all modules
./gradlew build

# Run verification tasks (including tests)
./gradlew check
```
""