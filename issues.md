### #Issue 1: [BUG] Vosk-Grammatik-Generierung unterstützt keine kommagetrennten Trigger-Phrasen

 * **Priorität:** Hoch
 * **Komponente:** data/repository/VoskDictionaryRepository.kt
 * **Problembeschreibung:**
 In der Benutzeroberfläche (CommandCreatorScreen.kt) und in der Matching-Logik des SpeechRecognitionService ist es vorgesehen, dass ein Smart-Home-Kommando mehrere, durch Komma getrennte Trigger-Phrasen besitzen kann (z. B. "stehlampe an, licht oben an").
 Beim Erstellen des JSON-Grammatik-Arrays für die Vosk-Engine in VoskDictionaryRepository.getGrammarJson() wird die Eigenschaft command.triggerPhrase jedoch als ein einziger, ungesplitteter String in das wordsSet eingefügt. Vosk erhält dadurch ungültige Tokens wie ["stehlampe an, licht oben an", "[unk]"] anstelle der einzelnen Wörter, was die Offline-Erkennung im Grammar-Mode für diese Befehle komplett unbrauchbar macht.
 * **Soll-Zustand:**
 Die Grammatik-Generierung muss kommagetrennte Strings sauber aufteilen, trimmen und die enthaltenen Wörter einzeln in das Vokabular-Set einspeisen.
 * **Technischer Lösungsansatz:**
 In getGrammarJson() muss die Iteration über die Commands angepasst werden:
 1. command.triggerPhrase am Komma splitten: .split(",")
 2. Jedes Element trimmen, in Kleinbuchstaben umwandeln und Whitespaces bereinigen.
 3. Die resultierenden Einzelwörter in wordsSet per addAll() einfügen.

### #Issue 2: [BUG] Race Condition und doppelte Restart-Schleife bei Fehlern der nativen STT-Engine

 * **Priorität:** Kritisch
 * **Komponenten:** stt/android/NativeSpeechEngine.kt und service/SpeechRecognitionService.kt
 * **Problembeschreibung:**
 Es existiert ein kritischer Konflikt im Lifecycle-Management bei Auftreten eines Erkennungsfehlers im nativen SpeechRecognizer.
 Tritt in der NativeSpeechEngine ein Fehler in onError auf, emittiert die Engine diesen über den _errorFlow und ruft nach einem delay(500) eigenständig intern wieder startRecognizer() auf.
 Gleichzeitig fängt der SpeechRecognitionService diesen errorFlow ab und triggert parallel restartPipeline(), was nach 3000 ms die Methode stopListening() und anschließend startListeningPipeline() aufruft.
 Da beide Routinen asynchron und ohne gegenseitige Sperren laufen, kollidieren sie beim Zugriff auf die native Singleton-Instanz. Das führt reproduzierbar zu ERROR_CLIENT oder ERROR_RECOGNIZER_BUSY und blockiert den Service dauerhaft.
 * **Soll-Zustand:**
 Die Zuständigkeit für den Restart-Loop muss zentralisiert werden. Eine Engine sollte ihren eigenen Zustand verwalten, aber keine globalen Pipeline-Neustarts parallel zum steuernden Service erzwingen.
 * **Technischer Lösungsansatz:**
 1. Entfernen des automatischen startRecognizer()-Aufrufs aus dem Fehler-Scope innerhalb der NativeSpeechEngine. Die Engine meldet den Fehler rein deklarativ an den Flow.
 2. Der SpeechRecognitionService übernimmt die alleinige Kontrolle über das Timing und das saubere Zurücksetzen (cancel/destroy) der Engine vor dem erneuten Instanziieren.

### #Issue 3: [BUG] Fehlende Fortschrittsanzeige (verbleibt bei 0%) beim Download von Vosk-ZIP-Modellen

 * **Priorität:** Medium
 * **Komponente:** utils/ModelDownloader.kt
 * **Problembeschreibung:**
 Wenn ein Vosk-Sprachmodell als ZIP-Archiv heruntergeladen wird (isZip = true), wird der body.byteStream() direkt in einen ZipInputStream gewrappt und sequentiell entpackt. Die Berechnung und Emission des prozentualen Fortschritts (emit(progress)) findet jedoch ausschließlich im else-Block für reguläre Einzeldateien statt. Bei großen Model-Downloads verweilt die Anzeige in den Einstellungen permanent bei 0 % und springt nach dem Entpacken abrupt auf 100 %. Zudem fehlt beim Entpacken eine Validierung der Zielpfade gegen "Zip-Slip"-Schachzüge (../).
 * **Soll-Zustand:**
 Auch während des Streamings und Entpackens einer ZIP-Datei muss der Fortschritt basierend auf den bereits vom Netzwerk gelesenen Bytes im Verhältnis zur contentLength kontinuierlich an die UI gemeldet werden.
 * **Technischer Lösungsansatz:**
 1. Vorschalten eines Custom- oder CountingInputStream vor den ZipInputStream, der die gelesenen Roh-Bytes akkumuliert.
 2. Berechnung des Fortschritts innerhalb der Lese-Schleife und regelmäßige Emission via flow.
 3. Einbau einer Sicherheitsprüfung (canonicalPath), um sicherzustellen, dass entpackte Dateien das Zielverzeichnis nicht verlassen dürfen.

### #Issue 4: [BUG] Verbindungsabsturz gegen Localhost bei fehlendem HTTP(S)-Schema in der HA-URL

 * **Priorität:** Hoch
 * **Komponenten:** di/NetworkModule.kt und ui/screens/SettingsScreen.kt
 * **Problembeschreibung:**
 Im AuthOkHttpClient versucht der Interceptor, die vom Nutzer hinterlegte Basis-URL dynamisch via url.toHttpUrlOrNull() zu parsen. Gibt der Nutzer in den Einstellungen eine reine IP-Adresse oder Domain ohne Protokoll-Präfix ein (z. B. 192.168.1.100:8123), schlägt das Parsen fehl und die Methode liefert null. Der Interceptor bricht das Umschreiben der URL daraufhin stillschweigend ab. Retrofit feuert den Request folglich gegen die hartcodierte Fallback-Standard-URL (http://localhost/api/...), was zu verwirrenden Verbindungsfehlern führt.
 * **Soll-Zustand:**
 Eingegebene URLs müssen entweder bei der Eingabe validiert oder spätestens vor der Verarbeitung repariert werden. Ein stillschweigender Fallback auf Localhost darf nicht vorkommen.
 * **Technischer Lösungsansatz:**
 1. Anpassung der Speicher-Logik im SettingsRepository oder direkt in der UI: Wenn die URL nicht mit http:// oder https:// beginnt, wird standardmäßig http:// vorangestellt.
 2. Alternativ: Einbau einer visuellen Validierung (Error-State am Textfeld) im SettingsScreen, die das Speichern unvollständiger URLs verhindert.

### #Issue 5: [FEATURE] Erweiterung um erweiterte Konfigurationsoptionen für die native Android STT-Engine

 * **Priorität:** Medium (Erweiterung)
 * **Komponenten:** ui/screens/SettingsScreen.kt, data/repository/SettingsRepository.kt, stt/android/NativeSpeechEngine.kt
 * **Problemstellung / Zielsetzung:**
 Der native SpeechRecognizer läuft aktuell mit minimalen Standardeinstellungen. Um die Erkennung für kurze Smart-Home-Befehle im Offline-Betrieb drastisch zu beschleunigen und fehlerfreier zu gestalten, sollen 5 spezifische Parameter des Android-SDKs konfigurierbar gemacht und persistiert werden.
 * **Soll-Zustand (Die 5 optionalen Einstellungen im Settings-Screen):**
 1. **Sprachcode erzwingen (EXTRA_LANGUAGE):** Ein Dropdown/Textfeld, um die Erkennung explizit festzulegen (z. B. "de-DE" oder "en-US"), statt sich blind auf die Systemsprache zu verlassen.
 2. **Sprachmodell-Modus (EXTRA_LANGUAGE_MODEL):** Ein Switch/Toggle zwischen LANGUAGE_MODEL_FREE_FORM (Diktat) und LANGUAGE_MODEL_WEB_SEARCH (kurze, prägnante Suchphrasen – ideal für Smart-Home-Kommandos).
 3. **Ruhe-Länge nach dem Sprechen (EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS):** Ein Slider (z. B. von 300 ms bis 2000 ms). Ein niedrigerer Wert (z. B. 600 ms) sorgt dafür, dass die Engine den Befehl nach dem Sprechen spürbar schneller abschließt und ausführt.
 4. **Zwischenergebnisse aktivieren (EXTRA_PARTIAL_RESULTS):** Ein optionaler Boolean-Switch, der steuert, ob die native Engine während des Sprechens Zwischenergebnisse liefert (verbessert die visuelle Latenz im Dashboard).
 5. **Maximale Ergebnishypothesen (EXTRA_MAX_RESULTS):** Ein Nummern-Eingabefeld (Standard: 1, Max: 5), um festzulegen, wie viele Text-Alternativen Android für das Matching zurückgeben soll.
 * **Umsetzungsplan:**
 1. Erweiterung der DataStore/SharedPreferences-Struktur im SettingsRepository um die 5 neuen Keys inklusive sinnvoller Defaults.
 2. Erstellung einer neuen Sektion "Native STT Feineinstellungen" im SettingsScreen (nur sichtbar oder aktiv, wenn Native STT als Engine gewählt ist).
 3. Auslesen dieser Werte in der NativeSpeechEngine.kt und dynamisches Hinzufügen via intent.putExtra(...) vor dem Start des Listeners.
