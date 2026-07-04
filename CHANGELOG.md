# Changelog

## [1.2.0] - 2026-07-05
### Added
- **Native Android STT Advanced Settings**: Added fine-grained control over the Native Android SpeechRecognizer (language selection, Web-Search-Model toggle, partial results, silence length, max results).
- **Settings Dialog**: The advanced Native STT settings are now grouped in a clean dialog.
- **Language Dropdown**: Added a dropdown to easily select standard languages for Native STT (System default, German, English).

### Changed
- **Home Assistant URL Fallback**: The app now automatically prepends `http://` to Home Assistant URLs if no protocol is specified.
- **Vosk Download Safety**: The Vosk ZIP model is now downloaded to a temporary file first, fixing the missing download progress indicator.
- **Vosk ZIP Extraction**: Hardened the extraction logic to prevent Zip-Slip attacks (directory traversal).

### Fixed
- **Native STT Race Conditions**: Fixed infinite loops and race conditions in Native STT error handling by centralizing lifecycle management in `SpeechRecognitionService`.
- **Vosk Grammar Bug**: Fixed a bug where comma-separated command triggers were not properly parsed and added to the Vosk grammar JSON, preventing them from being recognized.
- **Interpolation Bug**: Fixed an issue in the Settings UI where the silence length was displayed as raw template string `${nativeSttSilenceLength}ms`.

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
