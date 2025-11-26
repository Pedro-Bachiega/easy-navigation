# easy-navigation

A Kotlin Multiplatform navigation library built on top of JetBrains Compose Navigation (`navigation3`) with compile-time code generation via KSP.

The goal is to make navigation in Compose-based apps type-safe and declarative by:
- Representing each destination as a `NavigationRoute` Kotlin type.
- Generating navigation directions and registries from simple `@NavigationEntry` annotations.
- Providing a small runtime (`NavigationController` + `Navigation` composable) to drive the back stack.

## Modules overview

This repository is organized into the following Gradle modules:
- `annotation` – Defines the `@NavigationEntry` annotation used to mark composable destinations.
- `core` – Runtime navigation primitives and the `Navigation` composable used at app runtime.
- `processor:library` – KSP processor that generates per-module `*Direction` classes and `*DirectionRegistry` objects from `@NavigationEntry` usage.
- `processor:application` – KSP processor that aggregates all module registries into a single `GlobalDirectionRegistry`.
- `sample` – Compose Desktop sample that demonstrates how to define routes, annotate destinations, and wire navigation.
- `kmp-build-plugin` – Internal Gradle plugin used to configure multiplatform targets, linting, and other build behavior.

If you just want to use the library in another project, the important pieces are:
- `annotation`
- `core`
- `processor:library`
- `processor:application`

The other modules exist to support development and demonstration.

## Concepts

### NavigationRoute

Every destination in your app is represented by a type that implements `NavigationRoute`:

- Routes can be `data object` singletons (no arguments).
- Or `@Serializable` data classes (arguments become deep link query parameters).

Example (from the sample app):

```kotlin
import com.pedrobneto.navigation.core.NavigationRoute
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
    val title: String,
    val description: String,
) : NavigationRoute
```

These types are used both in your UI code (for type-safe navigation calls) and by the generated code that wires screens to the navigation runtime.

### NavigationDirection and DirectionRegistry

The library never asks you to write navigation graph code by hand. Instead, it **generates**:

- A `*Direction` object for each annotated destination.
- A `*DirectionRegistry` object per module that lists all directions in that module.

Conceptually, a `NavigationDirection`:
- Knows the `routeClass: KClass<out NavigationRoute>`.
- Holds the list of deeplink patterns associated with that destination.
- Implements `@Composable fun Draw(route: NavigationRoute)` which calls your screen.

A `DirectionRegistry` is just:

```kotlin
abstract class DirectionRegistry(
    val directions: List<NavigationDirection>
)
```

The runtime combines registries into a single flat list of directions that it uses to:
- Map route types to composable content.
- Resolve deeplink strings to `NavigationRoute` instances.

### NavigationController and Navigation composable

The runtime lives in `core`:

- `NavigationController` keeps a `SnapshotStateList<NavigationRoute>` as the back stack.
- It exposes operations:
  - `navigateTo(route: NavigationRoute, singleTop: Boolean = false)`
  - `navigateTo(deeplink: String, singleTop: Boolean = false)`
  - `navigateUp()`
  - `popUpTo(direction: NavigationRoute, inclusive: Boolean = false)`
- It builds an internal `entryProvider` using the list of `NavigationDirection`s.
- It is provided via `LocalNavigationController` composition local.

The `Navigation` composable is the root of the system:

```kotlin
@Composable
fun Navigation(
    modifier: Modifier,
    initialRoute: NavigationRoute,
    directionRegistries: List<DirectionRegistry>,
    contentAlignment: Alignment = Alignment.TopStart,
    /* optional transition / decorator parameters omitted */
)
```

It:
- Creates a `NavigationController` with the given `initialRoute` and `directionRegistries`.
- Exposes it through `LocalNavigationController`.
- Renders the current back stack using `NavDisplay` from `androidx.navigation3.ui`.

## How code generation works

Navigation code is generated in two stages.

### 1. Per-module generation (processor:library)

You mark composable destinations with `@NavigationEntry`:

```kotlin
import androidx.compose.runtime.Composable
import com.pedrobneto.navigation.annotation.NavigationEntry
import com.pedrobneto.navigation.core.LocalNavigationController
import com.pedrobneto.navigation.model.FirstScreenRoute
import com.pedrobneto.navigation.model.SecondScreenRoute

@Composable
@NavigationEntry(route = FirstScreenRoute::class, deeplinks = ["/first"])
fun FirstScreenComposable(modifier: Modifier = Modifier) {
    val navigation = LocalNavigationController.current
    // ...
}

@Composable
@NavigationEntry(route = SecondScreenRoute::class)
fun SecondScreenComposable(
    modifier: Modifier = Modifier,
    secondScreenRoute: SecondScreenRoute,
) {
    val navigation = LocalNavigationController.current
    // use secondScreenRoute.title, etc.
}
```

The **library processor** does the following for each annotated function:

1. Reads the `route` parameter (`FirstScreenRoute::class`, `SecondScreenRoute::class`, ...).
2. Extracts any `deeplinks`.
3. Looks for a function parameter whose type matches the route (`secondScreenRoute: SecondScreenRoute` in the example).
4. Generates a `*Direction` object in the **route’s package**, e.g.
   - `FirstScreenRouteDirection`
   - `SecondScreenRouteDirection`
5. Generates a `*DirectionRegistry` object for the module, e.g. `SampleDirectionRegistry`, inside `com.pedrobneto.navigation.registry`.

You never reference the generated `*Direction` types directly, but you do reference the generated registries.

### 2. Global aggregation (processor:application)

The **application processor** runs once per consuming application:

- It expects a KSP option `navigation.rootDir` pointing to the project root.
- It walks `build/generated/ksp/**/navigation/registry/` looking for files named `*DirectionRegistry.kt`.
- It parses their package and class names (e.g. `com.pedrobneto.navigation.registry.SampleDirectionRegistry`).
- It generates a single `GlobalDirectionRegistry` object:

```kotlin
// Conceptually

data object GlobalDirectionRegistry : DirectionRegistry(
    directions = listOf( /* all module directions */ )
)
```

In practice, this means your app can just depend on `GlobalDirectionRegistry` and let the processors keep it in sync.

## Using easy-navigation in your project

This section shows how you would typically consume the library in an application (Desktop or Android).

> Note: The exact Maven coordinates and versions depend on how you publish the artifacts. Replace `<version>` with the appropriate version string.

### 1. Add dependencies

In your app module `build.gradle.kts`:

```kotlin
dependencies {
    // Public API
    implementation("io.github.pedro-bachiega:easy-navigation-annotation:<version>")
    implementation("io.github.pedro-bachiega:easy-navigation-core:<version>")

    // KSP processors
    ksp("io.github.pedro-bachiega:easy-navigation-application-processor:<version>")
    ksp("io.github.pedro-bachiega:easy-navigation-library-processor:<version>")
}
```

If you are using multiplatform with a dedicated desktop module (like the sample):

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.github.pedro-bachiega:easy-navigation-annotation:<version>")
                implementation("io.github.pedro-bachiega:easy-navigation-core:<version>")
            }
        }
    }
}

dependencies {
    kspJvm(project(":processor:library"))
    kspJvm(project(":processor:application"))
}
```

### 2. Configure KSP root directory

In the **application** module that runs the app, pass the root directory to the application processor:

```kotlin
ksp {
    arg("navigation.rootDir", rootDir.path)
}
```

This allows the application processor to scan the full project tree for generated `*DirectionRegistry` files.

### 3. Define your routes

Create route types implementing `NavigationRoute`:

```kotlin
import com.pedrobneto.navigation.core.NavigationRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavigationRoute

@Serializable
data class DetailsRoute(
    val id: String,
    val title: String?
) : NavigationRoute
```

Guidelines:
- Use `data object` / `object` when there is no associated state.
- Use `@Serializable data class` when you need arguments.
- Property names double as deep-link query parameter keys.

### 4. Annotate your composable destinations

Mark your screens with `@NavigationEntry` and optionally declare deeplinks.

**Example 1 – simple route with deeplink:**

```kotlin
@Composable
@NavigationEntry(route = HomeRoute::class, deeplinks = ["/home"])
fun HomeScreen() {
    val navigation = LocalNavigationController.current

    Button(onClick = { navigation.navigateTo(DetailsRoute(id = "42", title = "The answer")) }) {
        Text("Go to details via route")
    }

    Button(onClick = { navigation.navigateTo("/details?id=42&title=The%20answer") }) {
        Text("Go to details via deeplink")
    }
}
```

**Example 2 – composable receiving the route instance:**

```kotlin
@Composable
@NavigationEntry(route = DetailsRoute::class, deeplinks = ["/details"])
fun DetailsScreen(detailsRoute: DetailsRoute) {
    val navigation = LocalNavigationController.current

    Text(text = "ID: ${detailsRoute.id}")
    detailsRoute.title?.let { Text("Title: $it") }

    Button(onClick = { navigation.navigateUp() }) {
        Text("Back")
    }
}
```

The processor will:
- Detect that `detailsRoute: DetailsRoute` matches `DetailsRoute::class`.
- Generate a `DetailsRouteDirection` that casts `route` to `DetailsRoute` and calls `DetailsScreen(detailsRoute = route as DetailsRoute)`.

### 5. Set up the Navigation root

Wrap your UI in the `Navigation` composable and provide the generated registries.

In the sample, this is done in `App`:

```kotlin
@Composable
fun App() {
    MaterialTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val globalRegistry = remember { listOf(GlobalDirectionRegistry) }

            Navigation(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                initialRoute = FirstScreenRoute,
                directionRegistries = globalRegistry,
            )
        }
    }
}
```

In your own app you would:

1. Import your initial route and generated registry:
   - `import com.yourpkg.navigation.model.HomeRoute`
   - `import com.yourpkg.navigation.registry.GlobalDirectionRegistry`
2. Call `Navigation(initialRoute = HomeRoute, directionRegistries = listOf(GlobalDirectionRegistry))` near the top of your composable tree.

### 6. Navigating between screens

Inside any annotated screen, get the `NavigationController` from the composition local:

```kotlin
val navigation = LocalNavigationController.current
```

Available operations:

#### Navigate using a route instance

```kotlin
navigation.navigateTo(DetailsRoute(id = "123", title = "Example"))
```

#### Navigate using a deeplink string

```kotlin
navigation.navigateTo("/details?id=123&title=Example")
```

The deeplink handling:
- Normalizes the URI (accepts `/path`, `nav:/path`, or standard `scheme://host/path`).
- Strips scheme and query before matching against known deeplinks.
- Parses query parameters into a map and feeds them to `kotlinx.serialization` to construct the `NavigationRoute` instance.

If the deeplink does not match any known direction, navigation is a no-op.

#### Single-top behavior

Avoid pushing duplicates by using `singleTop = true`:

```kotlin
navigation.navigateTo(HomeRoute, singleTop = true)
```

If `HomeRoute` is already in the back stack, this will pop up to it instead of adding a new instance.

#### Navigate up

```kotlin
navigation.navigateUp()
```

Removes the top entry from the back stack if possible.

#### Pop up to a route

```kotlin
navigation.popUpTo(direction = HomeRoute, inclusive = false)
```

- If `inclusive = false`, everything **above** `HomeRoute` is removed.
- If `inclusive = true`, `HomeRoute` itself is removed as well.

From the sample:

```kotlin
// SecondScreen -> back to FirstScreen using route
navigation.popUpTo(direction = FirstScreenRoute)

// SecondScreen -> back to FirstScreen using deeplink
navigation.navigateTo(deeplink = "/first", singleTop = true)
```

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
- Navigate back using both route-based and deeplink-based operations.

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

The `kmp-build-plugin` module configures multiplatform targets and applies the lint and optimization plugins. Library modules use `module-config.json` to declare which targets (Android, JVM, JS, etc.) should be built.

## Summary

At a high level, easy-navigation works by:

1. Letting you define strongly-typed routes as Kotlin types.
2. Letting you annotate composable functions with `@NavigationEntry` to declare destinations and deeplinks.
3. Generating navigation directions and registries at compile time via KSP.
4. Providing a small runtime (`NavigationController` + `Navigation` composable) that:
   - Keeps a stateful back stack of routes.
   - Resolves routes and deeplinks to composable content.
   - Exposes simple APIs for navigation and back stack manipulation.

The included `sample` module is the best place to see all of this in a working application.