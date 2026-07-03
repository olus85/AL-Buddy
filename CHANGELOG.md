# Changelog

## [1.1.0] - 2026-07-04
### Added
- **Native Android STT Engine**: Added support for Android's built-in continuous speech recognition as the new default offline engine.
- **Vosk Dictionary Management**: You can now manually add custom words or phrases (even comma-separated lists) to the Vosk STT grammar.
- **Auto-Sync Trigger Phrases**: Command trigger phrases are now automatically synchronized with the Vosk Dictionary.
- **Modernized Settings UI**: Completely overhauled the settings screen with Material 3 expandable cards, live connection testing icons, and auto-download progress bars.

### Changed
- **Auto-Save Settings**: Settings now save automatically as soon as they are modified.
- **Smart Connection Test**: Home Assistant connection is tested automatically when typing the URL or Token, removing the need for manual testing buttons.
- **Token Masking**: The Home Assistant Token is now hidden by default like a password to prevent shoulder-surfing.
- **STT Engine Default**: Changed the default STT engine from Vosk to Native Android.

### Fixed
- Fixed Kotlin JVM signature clashes in Room DAOs related to suspend functions and KSP.
