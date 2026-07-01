# AL Buddy

**AL Buddy** is an Android application that provides an offline, privacy-first voice assistant for controlling your Home Assistant setup.

## Features

- **Offline Speech-to-Text (STT)**: Uses the **Vosk** engine for fast, accurate, and completely offline voice recognition.
- **Home Assistant Integration**: Connects locally to your Home Assistant instance via the REST API.
- **Custom Voice Commands**: Create custom trigger phrases mapped to specific Home Assistant entities and services. Multiple trigger phrases per command are supported (separated by commas).
- **Background Listening**: Can run as a foreground service, listening for commands even when the app is in the background or the screen is off (requires microphone permissions and disabled battery optimization).
- **Haptic & Audio Feedback**: Receive a satisfying click sound and vibration immediately upon a successful voice command match.
- **Backup & Restore**: Easily export and import your settings, HA URL, Long-Lived Access Token, and created commands to a JSON file.

## Requirements

- Android Device running Android 10+ (Tested extensively on Pixel 7 Pro with Android 13/14).
- A running instance of [Home Assistant](https://www.home-assistant.io/).
- A Long-Lived Access Token from Home Assistant.

## Setup Instructions

1. **Install the APK** on your Android device.
2. **Grant Microphone Permissions** when prompted.
3. Open the **Settings** within the app:
   - Enter your Home Assistant Local IP/URL (e.g., `http://192.168.1.x:8123`).
   - Enter your Long-Lived Access Token.
   - Click **Save & Test Connection**.
   - Click **Download STT Model (Vosk)** to download the offline German speech recognition model.
4. **Create Commands**: Go to the Command Creator, select an entity and service, and type the trigger phrase(s) you want to use.
5. Go to the **Dashboard** and press **Start Listening**.

## Troubleshooting

- **App stops listening in the background**: Ensure that Battery Optimization is disabled for AL Buddy in your Android settings.
- **Vibration/Sound not working**: Ensure your phone isn't in silent mode or Do Not Disturb mode, and verify the settings are toggled on.

## License

This project is licensed under the MIT License.
