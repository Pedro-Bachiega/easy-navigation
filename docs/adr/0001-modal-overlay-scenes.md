# ADR 0001: Render modal destinations as Navigation 3 overlay scenes

## Status

Accepted

## Context

Easy Navigation currently represents destinations as routes in one `NavBackStack` and uses scene
strategies to choose between single-pane and adaptive layouts. Some flows need a destination to
appear above the current screen without replacing it or turning it into a side pane.

## Decision

Modal destinations are declared with `@Modal` and remain ordinary routes in the existing back stack.
The modal strategy runs before pane strategies and uses Navigation 3's `OverlayScene`. It calculates
the scene below the modal using the same adaptive strategy chain and includes that scene's entries
in the overlay's entry list, so the base is composed exactly once while the overlay remains in the
Navigation 3 transition pipeline. Multiple modals can be stacked recursively.

The library owns the transparent interaction layer and dismiss policy. The modal composable owns its
surface, dimensions, alignment, and visual styling. System back and outside taps respect the
destination's `dismissible` flag, while explicit `navigateUp()` calls are always allowed.

Modal declarations cannot be combined with pane annotations because pane layout and modal
presentation are separate, competing presentation modes.

## Consequences

- Existing adaptive scenes remain intact below the modal.
- Modal routes work with the existing back stack, serialization, and deeplink infrastructure.
- A modal cannot be the root route because an overlay requires a non-empty base scene.
- Custom modal animations are deferred until a dedicated transition API is designed.
