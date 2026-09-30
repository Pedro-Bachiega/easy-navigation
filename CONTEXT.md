# Easy Navigation

This context defines the language used for navigation destinations and the results they return.

## Language

**Navigation result**:
An outcome returned by a destination that a caller opened, containing either a confirmed value or cancellation.
_Avoid_: Activity result, callback value

**Navigation result launcher**:
A caller-owned handle that opens a destination and receives only that destination's navigation result.
_Avoid_: Result callback, destination contract
