---

# Epic: STT-Engine Optimierung (Vosk Grammar Mode & Native SpeechRecognizer)

**Kontext:**
Die aktuelle Standard-Implementierung von Vosk (Full-Vocabulary) weist für die primäre Use-Case-Anforderung (kurze Smart-Home-Befehle) eine zu geringe Trefferquote und Performance auf. Whisper ist zu langsam (hohe Latenz). Dieses Epic umfasst zwei voneinander unabhängige, in den Einstellungen umschaltbare Lösungswege, um die Zuverlässigkeit und Geschwindigkeit der Spracherkennung im Foreground Service drastisch zu erhöhen.

---

## Issue 1: Vosk Grammar Mode & Dynamische Dictionary-Verwaltung

### Beschreibung

Vosk soll nicht mehr versuchen, jedes beliebige deutsche Wort zu erkennen, sondern algorithmisch auf eine vordefinierte Liste von Phrasen beschränkt werden. Dies reduziert die Fehlerquote gegen Null und senkt die CPU-Last. Die Liste dieser Wörter (Dictionary/Grammar) muss vom User über die App-Einstellungen verwaltet und zur Laufzeit an Vosk übergeben werden.

### Akzeptanzkriterien (Acceptance Criteria)

* **UI/UX:** In den Einstellungen gibt es einen neuen Menüpunkt "Vosk Dictionary verwalten".
* **Dictionary-Verwaltung:** Der User kann in einer Listenansicht Wörter/Phrasen hinzufügen, bearbeiten und löschen.
* **Auto-Sync mit Befehlen:** Alle Phrasen, die im Hauptbildschirm (Dashboard) als Trigger-Satz für einen Home Assistant Befehl hinterlegt sind, werden *automatisch* im Dictionary geführt.
* **Vosk-Initialisierung:** Die App übergibt bei der Initialisierung des Vosk-Recognizers das generierte Dictionary als JSON-Array an den Konstruktor.
* **Sicherheits-Tag:** Das Array enthält zwingend als letztes Element den String `"[unk]"`, um Abstürze bei unbekannten Geräuschen zu verhindern.
* **Live-Reload:** Wird das Dictionary geändert, startet sich der Vosk-Service automatisch neu, um das neue Grammar-Modell zu laden.

### Technische Umsetzung (Anweisungen für Entwickler/KI)

1. **Datenbank (Room):** Erweitere die Room-DB um eine Tabelle/Entity `VoskWord(id, word)`.
2. **ViewModel / Repository:** Schreibe eine Flow-Query, die alle `trigger_phrase` aus der Tabelle `Command` mit den Wörtern aus `VoskWord` kombiniert, Duplikate entfernt und daraus ein JSON-Array baut (Format: `["wort 1", "satz 2", "[unk]"]`).
3. **STT-Engine (Vosk):** Passe die Klasse `VoskEngine` an. Der Konstruktor oder die `start()`-Methode muss nun den Grammar-String akzeptieren: `Recognizer(model, 16000.0f, grammarJsonString)`.

---

## Issue 2: Native Android SpeechRecognizer (Continuous Loop)

### Beschreibung

Auf Pixel-Geräten (und modernen Android-Systemen) soll das nativ integrierte, lokale Speech-to-Text-Modell (über Android AI Core / Tensor) angezapft werden. Da dieser Service standardmäßig nach einem erkannten Satz stoppt, muss ein "Continuous Listening Loop" (Dauerschleife) implementiert werden, um ihn als Always-On-Assistenten zu nutzen.

### Akzeptanzkriterien (Acceptance Criteria)

* **UI/UX:** In den Einstellungen (STT-Engine-Auswahl) gibt es nun eine dritte Option: "Native Android (Offline)".
* **Lokale Ausführung:** Der Intent für den SpeechRecognizer wird explizit gezwungen, offline zu arbeiten (`EXTRA_PREFER_OFFLINE`).
* **Continuous Loop:** Wenn der Spracherkenner stoppt (durch Fehler, Stille oder ein erfolgreiches Ergebnis), wird er sofort und automatisch neu gestartet.
* **Stabilität:** Endlos-Crash-Loops (z. B. wenn das Mikrofon blockiert ist) werden durch einen kurzen Delay (z.B. 500ms) beim Neustart nach einem Fehler abgefangen.
* **Thread-Sicherheit:** Die Engine beachtet, dass der native `SpeechRecognizer` zwingend auf dem Main Thread (UI Thread) laufen muss.

### Technische Umsetzung (Anweisungen für Entwickler/KI)

1. **Neue STT-Engine-Klasse:** Erstelle die Klasse `NativeSpeechEngine`, die das Interface `STTEngine` implementiert.
2. **Intent-Konfiguration:**
```kotlin
val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
}

```


3. **Loop-Logik im RecognitionListener:**
* `onResults(results: Bundle)`: Extrahiere den besten Treffer (`RESULTS_RECOGNITION`), triggere den Flow `onTranscriptionReady` und rufe danach sofort `speechRecognizer.startListening(intent)` auf.
* `onError(error: Int)`: Logge den Fehler. Egal ob `ERROR_NO_MATCH`, `ERROR_SPEECH_TIMEOUT` oder andere – starte die Erkennung mit einem leichten Delay (z.B. per Coroutine `delay(500)`) neu auf dem Main Thread.


4. **Lifecycle Management:** Stelle sicher, dass bei Beenden des Foreground Services `speechRecognizer.destroy()` aufgerufen wird, um Memory Leaks zu vermeiden.
