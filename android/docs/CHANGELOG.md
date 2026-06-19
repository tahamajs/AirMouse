# Changelog

All notable changes for v1.0.0

## [1.0.0] - 2026-05-29
### Added
- Initial stable release build (APK) produced
- Redesigned and modernized main UI: home, profiles, network discovery, gestures, settings, calibration
- Added reusable UI drawable `bg_input.xml` for consistent input fields
- Material 3 theme consolidation and namespace fixes across layouts

### Fixed
- Resolved resource linking and layout namespace issues preventing builds
- Removed external JSON dependency to allow offline builds
- Fixed various malformed XML fragments and consolidated layout styles

### Notes
- Build completed successfully: `assembleDebug` produced debug APK
- Some Kotlin deprecation warnings remain (non-blocking)

---

Release prepared by automation on the development branch. Please push the tag and create a GitHub release to publish artifacts.
