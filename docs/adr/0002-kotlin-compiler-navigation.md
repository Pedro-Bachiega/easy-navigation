# Kotlin compiler navigation generation

Status: accepted for the initial Kotlin 2.4.20 implementation.

## Decision

Replace KSP with complete generated Kotlin sources and a K2 compiler plugin for semantic
validation. Retain the Gradle plugin ID, annotations, direction names/packages and public
registry API. Applications continue composing feature registries explicitly with `listOf(...)`.
The compiler artifact and generation dependencies are selected automatically by the Gradle plugin.

The Gradle plugin reads the KGP source-set graph after `AfterFinaliseRefinesEdges`, when default
hierarchy edges have been created, and uses KGP source-set-tree classifiers for compilations.
The lifecycle/classifier bridge uses internal KGP APIs isolated behind the exact version pin.
It reads configured source directories independently of directory names.
A cacheable task runs compiler PSI discovery in a separate JVM, reads dependency metadata for
class names, type aliases and constants, and emits a complete source snapshot. Successful
regeneration replaces the snapshot, removing obsolete declarations. Discovery precedes normal
compilation because consumers already import the generated registries in their original sources.

Shared directions stay in their declaring source set. Registries have an automatically generated
`expect object` in the common root and an `actual data object` for each platform compilation.
An actual registry includes only destinations visible through that compilation's `dependsOn`
graph. Scope registries use the same rule, including an empty actual when a scope has no
visible destinations on that platform. Registries aggregate the current module's destinations;
they do not discover destinations from dependency modules.

The normal compilation loads `FirAdditionalCheckersExtension` through a K2 registrar. FIR
checks resolved route identity, inheritance from `NavigationRoute`, `@Serializable`, and supported
function signatures. Syntax discovery reports malformed destination annotations and collisions.
Generated `Draw` and serializer-registration implementations are regular Kotlin, so Compose and
Serialization transform them normally. No custom IR generation is needed: generating an IR class
alone would not make a registry import resolvable in the consumer's earlier frontend analysis.

## Compatibility and risks

Only Kotlin 2.4.20 is supported initially. The Gradle integration rejects other versions;
compiler upgrades require the compatibility CI suite before changing the pin. Compiler PSI,
metadata and FIR APIs are unstable and belong in the compiler artifact, isolated from Gradle's
classloader. Initial guaranteed targets are Android, JVM, iOS ARM64 and iOS Simulator ARM64,
including common and intermediate metadata compilation.

Source discovery has its own import/alias/annotation-constant resolver and does not replace
Kotlin's semantic analysis. CI covers aliases and constant expressions as well as ordinary
annotations; further compiler-language constructs need explicit compatibility coverage. Generation
uses complete snapshots rather than a per-file incremental algorithm because platform registries
aggregate declarations from multiple source sets. Gradle can reuse the task output for unchanged
inputs and classpaths.

Expect/actual classifiers remain a Kotlin Beta feature. Generated files expose ordinary Kotlin
source roots to IntelliJ IDEA and Android Studio after generation/Gradle import preparation.
This requires no Easy Navigation IDE extension, but does not provide live regeneration while
typing or a custom generated-body preview. IDE UI behavior requires separate manual verification.

## Migration and acceptance

Keep the Easy Navigation Gradle plugin and generated imports. Update Kotlin and the library,
remove this library's KSP processor/arguments and manual KSP source-directory wiring, and
recompile dependent feature libraries. Keep KSP itself if another library still uses it.

The incremental delivery and its acceptance gates are:

1. Replace the processor and source-set wiring: generation tests cover custom directories,
   intermediate sets, aliases/constants, destination metadata and stale output removal.
2. Preserve source-visible APIs: compile sample and cross-module consumers for common metadata,
   JVM and Android, including a consumer that does not apply the compiler plugin.
3. Verify runtime and plugin interaction: generated `Draw` receives its route through Compose;
   polymorphic serialization and pane/modal/parent metadata match the annotation contract.
4. Verify Native: compile consumers and link sample frameworks for both supported iOS targets.
5. Verify distribution and build behavior: compile a separate consumer using artifacts published
   to a temporary CI repository, reject invalid routes with source diagnostics, and reuse the
   configuration cache after deleting a destination.
6. Run the repository checks and present the PR for review. Validation runs in CI only; no merge
   or public release publication is part of this migration.
