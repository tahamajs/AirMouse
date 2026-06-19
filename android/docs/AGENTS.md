# AirMouse Android workspace guidelines

## Goal

Keep the Android app modern, modular, and buildable while preserving the current app behavior until a replacement is ready.

## Project structure

- `:app` is the UI shell and feature entry point.
- `:network` owns reusable socket/client code and shared network DTOs.
- New reusable code should go into the smallest module that can own it.
- Do not duplicate the same class in both `:app` and `:network`.

## Android architecture rules

- Prefer Kotlin, coroutines, Flow, and ViewModel-friendly APIs.
- Keep networking off the main thread.
- Use `DataSender`, `AutoReconnect`, and discovery helpers from `:network` instead of re-implementing sockets in `:app`.
- `PreferencesManager` in `:app` implements `com.airmouse.network.ConnectionStore` so the network layer stays app-agnostic.

## Build and validation

- Keep Gradle files clean and declarative.
- If a change touches build scripts or shared dependencies, validate the affected module before moving on.
- Fix duplicate dependencies, stray snippets, and dead code before adding new work.

## File placement

- Put new Android source files in the module that owns the responsibility.
- Keep package paths aligned with module ownership.
- If a file is moved, remove the old copy instead of leaving a duplicate behind.

## Workflow expectations

- Make small, testable changes.
- If more than 6 files are changed in one stretch, commit before continuing with the next batch.
- Finish a batch by validating compile or lint errors for the touched area.
- Prefer a single source of truth over compatibility wrappers unless the wrapper is intentionally temporary.

## Notes

- The modernization target is in `MODERNIZATION_SPEC.md`.
- Preserve existing app behavior unless the task explicitly asks for a rewrite.
