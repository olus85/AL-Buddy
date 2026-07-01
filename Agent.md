# AI Context & Identity
You are an expert Senior Android Developer specializing in Kotlin, Jetpack Compose, Coroutines, and local Machine Learning on Android.

# Project Context
We are building an Android app meant to run on an old device plugged into the wall. It acts as a local Voice-Assistant-Hub for Home Assistant. It continuously listens to the microphone in the background, transcribes audio locally using either Vosk or Whisper.cpp (toggleable), and matches the transcribed text against a local Room Database of trigger phrases. If a phrase matches, it triggers a REST API call to Home Assistant.

# Implementation Rules & Priorities
1. **Modern Android:** Use Jetpack Compose for all UI. Use Kotlin Coroutines/Flow for asynchronous work and Room for local persistence.
2. **Foreground Service:** The most critical component is the `SpeechRecognitionService`. It must be implemented as a robust Foreground Service (Type: microphone) with a persistent notification to prevent Android from killing it. It must handle `RECORD_AUDIO` continuous streams without memory leaks.
3. **STT Integration Abstracted:** Create an interface `STTEngine` with methods like `startListening()`, `stopListening()`, and a Flow/Callback for `onTranscriptionReady(text: String)`. Implement two classes: `VoskEngine` and `WhisperEngine`. The service should instantiate the engine based on user preferences.
4. **Network Layer:** Use Retrofit. The app needs to hit `/api/states` (GET) to list entities and `/api/services/{domain}/{service}` (POST) to execute commands. Always include the Bearer token in the headers.
5. **Robustness:** Implement auto-restart logic inside the Foreground Service in case the STT engine throws an OutOfMemoryError or crashes.

Let's start by scaffolding the Android project structure and setting up the Room Database and Retrofit interfaces. Please output the initial Data classes and Room DAO first.