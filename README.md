# Easy Navigation

A type-safe, declarative, and boilerplate-free navigation library for Compose Multiplatform, built on top of JetBrains' `navigation3` and powered by KSP.

The goal is to make navigation in Compose-based apps simple and robust by:
- Representing each destination as a `@Serializable` `NavigationRoute` Kotlin type.
- Generating navigation logic and registries from simple `@NavigationEntry` annotations.
- Providing a concise runtime (`NavigationController` + `Navigation` composable) to manage the back stack and navigation lifecycle.

## Modules overview

This repository is organized into the following Gradle modules:
- `annotation` – Defines the `@NavigationEntry` annotation used to mark composable destinations.
- `core` – Contains the runtime navigation primitives, including `NavigationController`, `LaunchStrategy`, and the `Navigation` composable.
- `processor:library` – A KSP processor that generates per-module `*Direction` classes and `*DirectionRegistry` objects from `@NavigationEntry` usage.
- `processor:application` – A KSP processor that aggregates all module registries into a single `GlobalDirectionRegistry` for the application.
- `sample` – A Compose for Desktop sample app that demonstrates how to define routes, annotate destinations, and wire up navigation.
- `kmp-build-plugin` – An internal Gradle plugin for configuring multiplatform targets, linting, and other common build behaviors.

For consumers of the library, the key modules are:
- `annotation`
- `core`
- `processor:library`
- `processor:application`

## Concepts

### NavigationRoute

Every destination in your app is represented by a type that implements the `NavigationRoute` marker interface.

- Routes can be `data object` singletons for destinations without arguments.
- For destinations with arguments, use a `@Serializable data class`. The properties of the data class become the arguments for the destination and can be populated from deep link query or path parameters.

Example (from the sample app):

```kotlin
import com.pedrobneto.navigation.core.model.NavigationRoute
import kotlinx.serialization.Serializable

@Serializable
data object FirstScreenRoute : NavigationRoute

@Serializable
data class SecondScreenRoute(
    val title: String,
    val description: String? = null,
) : NavigationRoute

@Serializable
data class ThirdScreenRoute(
    val id: Long,
    val source: String,
) : NavigationRoute
```

These types provide compile-time safety for navigation calls and are used by the generated code to link destinations to their composable implementations.

### NavigationDirection and DirectionRegistry

You never have to write navigation graph code by hand. Instead, the library **generates** it:

- A `*Direction` object for each composable annotated with `@NavigationEntry`.
- A `*DirectionRegistry` object for each module, which lists all the directions within that module.

Conceptually, a `NavigationDirection`:
- Knows the `routeClass: KClass<out NavigationRoute>`.
- Holds the list of deeplink patterns associated with that destination.
- Provides a `@Composable fun Draw(route: NavigationRoute)` function that calls your screen composable with the correct arguments.

A `DirectionRegistry` is a simple container:

```kotlin
abstract class DirectionRegistry(
    val directions: List<NavigationDirection>
)
```

The `NavigationController` uses these registries to build a complete map of the app's navigation graph, allowing it to resolve route types and deeplinks to their corresponding composable content.

### NavigationController and Navigation composable

The core runtime lives in the `core` module:

- `NavigationController` maintains the back stack as a `SnapshotStateList<NavigationRoute>`.
- It is provided to the composable tree via `LocalNavigationController`.
- It exposes simple, powerful operations for navigation:
  - `navigateTo(route: NavigationRoute, strategy: LaunchStrategy = ...)`
  - `navigateTo(deeplink: String, strategy: LaunchStrategy = ...)`
  - `navigateUp()`: Pops the back stack. Returns `true` on success.
  - `popUpTo(direction: NavigationRoute, inclusive: Boolean = false)`: Pops the back stack to a specific destination.

The `Navigation` composable is the root of the navigation system:

```kotlin
@Composable
fun Navigation(
    modifier: Modifier,
    initialRoute: NavigationRoute,
    directionRegistries: List<DirectionRegistry>,
    // Optional parameters for transitions, alignment, etc.
    contentAlignment: Alignment = Alignment.TopStart,
    sceneStrategy: SceneStrategy<NavigationRoute> = ...,
    transitionSpec: ... = defaultTransitionSpec(),
    // ... and more
)
```

It performs three key tasks:
1. Creates and remembers a `NavigationController` with the provided `initialRoute` and `directionRegistries`.
2. Provides this controller to the rest of the UI tree via `LocalNavigationController`.
3. Renders the current destination using `NavDisplay` from `androidx.navigation3.ui`, handling transitions and state restoration automatically.

### LaunchStrategy

The `LaunchStrategy` sealed class controls how `navigateTo` operations affect the back stack, similar to Android's launch modes.

- `LaunchStrategy.NewTask(clearStack: Boolean)`:
  - If `clearStack` is `true`, the entire back stack is cleared, and the new destination becomes the root. Ideal for post-login or other "reset" flows.
  - If `false` (default), the new destination is pushed onto the stack.

- `LaunchStrategy.SingleTop(clearTop: Boolean)`:
  - If the destination is already at the top of the stack, it is replaced (to update arguments) instead of adding a new instance.
  - If `clearTop` is `true` (default) and the destination exists elsewhere in the stack, pops everything above it and pushes the new destination.

## How code generation works

Navigation code is generated in two stages using KSP.

### 1. Per-module generation (processor:library)

You annotate your destination composables with `@NavigationEntry`:

```kotlin
@Composable
@NavigationEntry(route = SecondScreenRoute::class, deeplinks = ["/second"])
fun SecondScreenComposable(
    modifier: Modifier = Modifier,
    // The processor injects the type-safe route object
    route: SecondScreenRoute,
) {
    val navigation = LocalNavigationController.current
    // use route.title, etc.
}
```

For each annotated function, the **library processor**:

1. Reads the `route` KClass (`SecondScreenRoute::class`).
2. Extracts any `deeplinks` (`"/second"`).
3. Looks for a function parameter whose type matches the route (`route: SecondScreenRoute`).
4. Generates a `*Direction` object (e.g., `SecondScreenRouteDirection`) in the **route’s package**.
5. Generates a `*DirectionRegistry` object for the module (e.g., `SampleDirectionRegistry`) in the `com.pedrobneto.navigation.registry` package.

### 2. Global aggregation (processor:application)

The **application processor** runs once in your main application module:

- It requires a KSP argument `navigation.rootDir` pointing to your project's root directory.
- It scans the project's `build/generated/ksp/**/navigation/registry/` directories to find all `*DirectionRegistry.kt` files.
- It generates a single `GlobalDirectionRegistry` object that aggregates all directions from all modules.

```kotlin
// Generated code (conceptual)
object GlobalDirectionRegistry : DirectionRegistry(
    directions = SampleDirectionRegistry.directions +
                 FeatureADirectionRegistry.directions +
                 ...
)
```

This automated aggregation means your app can simply reference `GlobalDirectionRegistry` without needing to know about individual feature module registries.

## Using easy-navigation in your project

This section outlines the steps to integrate and use the library in a Compose Multiplatform project.

> Note: Replace `<version>` with the appropriate library version.

### 1. Add dependencies

In your app module's `build.gradle.kts`:

```kotlin
dependencies {
    // Public API (needed by any module defining routes or screens)
    implementation("io.github.pedro-bachiega:easy-navigation-annotation:<version>")
    implementation("io.github.pedro-bachiega:easy-navigation-core:<version>")

    // KSP Processors (typically in the main app module)
    ksp("io.github.pedro-bachiega:easy-navigation-application-processor:<version>")
    ksp("io.github.pedro-bachiega:easy-navigation-library-processor:<version>")
}
```

For a typical multi-module setup, you'll apply `library-processor` to feature modules and both processors to the main app module.

### 2. Configure KSP root directory

In the **application module** that consumes other feature modules, pass the root directory path to the application processor. This allows it to scan the entire project for generated registries.

```kotlin
// In your app's build.gradle.kts
ksp {
    arg("navigation.rootDir", rootDir.path)
}
```

### 3. Define your routes

Create your `@Serializable` route types that implement `NavigationRoute`.

```kotlin
@Serializable
data object HomeRoute : NavigationRoute

@Serializable
data class DetailsRoute(
    val id: String,
    val title: String? = null
) : NavigationRoute
```

### 4. Annotate your composable destinations

Mark each screen with `@NavigationEntry`, providing its route type and any deeplinks.

**Example 1: Simple screen**

```kotlin
@Composable
@NavigationEntry(route = HomeRoute::class, deeplinks = ["/home"])
fun HomeScreen() {
    val navigation = LocalNavigationController.current

    Button(onClick = {
        navigation.navigateTo(DetailsRoute(id = "42", title = "The answer"))
    }) {
        Text("Go to details via route")
    }

    Button(onClick = {
        navigation.navigateTo("/details/42?title=The%20answer")
    }) {
        Text("Go to details via deeplink")
    }
}
```

**Example 2: Screen with arguments**

The processor automatically injects the route object as a parameter.

```kotlin
@Composable
@NavigationEntry(route = DetailsRoute::class, deeplinks = ["/details/{id}"])
fun DetailsScreen(route: DetailsRoute) {
    val navigation = LocalNavigationController.current

    Text("ID from path: ${route.id}")
    route.title?.let { Text("Title from query: $it") }

    Button(onClick = { navigation.navigateUp() }) {
        Text("Back")
    }
}
```

### 5. Set up the Navigation root

In your main `App` composable, wrap your UI in the `Navigation` composable.

```kotlin
import com.pedrobneto.navigation.registry.GlobalDirectionRegistry // Generated

@Composable
fun App() {
    MaterialTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            // Remember the list of registries
            val registries = remember { listOf(GlobalDirectionRegistry) }

            Navigation(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                initialRoute = FirstScreenRoute,
                directionRegistries = registries,
            )
        }
    }
}
```

### 6. Navigating between screens

Access the `NavigationController` from any composable within the `Navigation` hierarchy.

```kotlin
val navigation = LocalNavigationController.current
```

**Navigate with a type-safe route:**

```kotlin
// Push a new screen
navigation.navigateTo(DetailsRoute(id = "123", title = "Example"))

// Clear the stack and make DetailsRoute the new root
navigation.navigateTo(
    route = DetailsRoute(id = "123"),
    strategy = LaunchStrategy.NewTask(clearStack = true)
)
```

**Navigate with a deeplink string:**

The library parses path parameters (`/details/{id}`), query parameters (`?title=...`), and constructs the `NavigationRoute` instance using `kotlinx.serialization`.

```kotlin
navigation.navigateTo("/details/123?title=Example")
```

**Navigate up:**

Pops the top entry from the back stack.

```kotlin
val didNavigateUp = navigation.navigateUp()
```

**Pop up to a specific destination:**

```kotlin
// Pops everything above HomeRoute
navigation.popUpTo(direction = HomeRoute)

// Pops everything including HomeRoute
navigation.popUpTo(direction = HomeRoute, inclusive = true)
```

**Using `SingleTop` strategy:**

To bring an existing destination to the top instead of creating a duplicate:

```kotlin
// If another instance of HomeRoute exists in the stack, pop up to it.
// If it's already on top, this call will replace it to update its arguments.
navigation.navigateTo(HomeRoute, strategy = LaunchStrategy.SingleTop())
```

This is useful for handling notifications or other events that should navigate to a screen without creating a deep back stack.

## Running the sample app

From the repository root:

```bash
./gradlew :sample:run
```

This will:
- Build the `annotation`, `core`, and processor modules.
- Run KSP for the `sample` module, generating directions and registries.
- Launch the Compose Desktop window titled **"Easy Navigation"**.

You can then:
- Navigate from the first screen to the second via a route instance.
- Navigate from the first to the third via a deeplink string.
- Navigate back using both route-based and pop operations.

## Building and linting

Useful Gradle commands from the repo root:

```bash
# Build all modules
./gradlew build

# Run linting (Detekt + KtLint) for modules that apply the lint plugin
./gradlew detekt ktlintCheck

# Run verification tasks (including tests where present)
./gradlew check
```

## Summary

At a high level, easy-navigation works by:

1. Letting you define strongly-typed routes as Kotlin types.
2. Letting you annotate composable functions with `@NavigationEntry` to declare destinations and deeplinks.
3. Generating navigation directions and registries at compile time via KSP.
4. Providing a small runtime (`NavigationController` + `Navigation` composable) that:
   - Keeps a stateful back stack of routes.
   - Resolves routes and deeplinks to composable content.
   - Exposes simple APIs for navigation and back stack manipulation using different `LaunchStrategy` options.

The included `sample` module is the best place to see all of this in a working application.
