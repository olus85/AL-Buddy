# Product Requirements Document (PRD): "Always-Listening HA Hub"
## 1. Project Overview
**High-Level Zusammenfassung:** Die App verwandelt ein altes, dauerhaft am Stromnetz hängendes Android-Smartphone (z. B. Pixel 7 Pro) in einen lokalen Smart-Home-Voice-Hub. Die App lauscht permanent im Hintergrund auf das Mikrofon, transkribiert die Audiodaten lokal (ohne Cloud) und gleicht das Transkript in Echtzeit mit einer Liste benutzerdefinierter Phrasen ab. Bei einem Treffer wird über die Home Assistant (HA) REST API ein Befehl ausgeführt (z. B. "TV aus"). Es wird **kein** Wake-Word ("Ok Google") benötigt.
**Ziele & Erfolgskriterien:**
 * **100% Lokale Verarbeitung:** Keine Audiodaten verlassen das Gerät.
 * **Zuverlässigkeit:** Dauerhafter Hintergrundbetrieb als Foreground Service. Keine Unterbrechungen durch Android-Batterie-Optimierungen.
 * **Nahtlose HA-Integration:** Direkter API-Zugriff, um Entitäten direkt in der App auszuwählen.
 * **Resilienz:** Automatischer Neustart der Services bei Abstürzen.
## 2. User Stories & Features
 * **Als User** möchte ich beliebige Text-Phrasen (z.B. "licht an") definieren, damit ich Aktionen ohne ein spezifisches Trigger-Wort auslösen kann.
 * **Als User** möchte ich meine Home Assistant Instanz anbinden (URL + Token) und die Verbindung per Button testen, um sicherzustellen, dass die API erreichbar ist.
 * **Als User** möchte ich in der App eine Liste aller meiner Home Assistant Entitäten sehen, um sie bei der Befehlserstellung einfach per Dropdown/Suche auszuwählen, anstatt IDs abtippen zu müssen.
 * **Als User** möchte ich in den Einstellungen zwischen zwei Speech-to-Text (STT) Engines (Vosk und Whisper) wechseln können, um je nach Situation Geschwindigkeit gegen Genauigkeit abzuwägen.
 * **Als User** möchte ich, dass die App unsichtbar als Foreground Service im Hintergrund läuft, damit mein OLED-Display keinen Burn-in erleidet, die App aber trotzdem vom Betriebssystem nicht beendet wird.
## 3. User Flow & UX
Die App hat drei Hauptbereiche, gesteuert über eine simple Bottom-Navigation oder Tabs:
 1. **Main Screen (Dashboard):**
 * Großer Toggle-Button: "Service Start / Stop".
 * Liste der konfigurierten Befehle (Phrase -> Aktion).
 * Floating Action Button (FAB) zum Hinzufügen eines neuen Befehls.
 2. **Command Creation Screen:**
 * Textfeld: "Phrase" (z.B. "fernseher aus").
 * Dropdown/Suchfeld: "Home Assistant Entity" (wird live über die HA-API geladen).
 * Dropdown: "Action/Service" (z.B. turn_on, turn_off, toggle).
 * Button: "Speichern".
 3. **Settings Screen:**
 * Sektion HA-Config: Eingabefeld "HA Base URL" (z.B. http://192.168.1.x:8123) und "Long-Lived Access Token".
 * Button: "Test Connection" (zeigt bei Erfolg einen grünen Haken oder Toast).
 * Sektion STT-Engine: Radio-Buttons zur Auswahl zwischen Vosk und Whisper.cpp.
 * Button: "Modelle herunterladen/verwalten" (falls die STT-Modelle nachgeladen werden müssen).
## 4. Technical Specifications
### Tech Stack
 * **Plattform:** Android Native (Kotlin).
 * **Architektur:** MVVM (Model-View-ViewModel) mit Kotlin Coroutines & Flow.
 * **UI:** Jetpack Compose (für schnelle, moderne UI-Entwicklung).
 * **Netzwerk:** Retrofit + OkHttp.
 * **Lokale Datenbank:** Room Database.
 * **STT Engines:**
   * Vosk Android SDK (com.alphacep:vosk-android).
   * Whisper.cpp (JNI/Android Bindings für lokale Ausführung).
### API Definitionen (Home Assistant REST API)
**Connection Test & Fetch Entities:**
 * `GET /api/states`
 * Header: `Authorization: Bearer <token>`
 * Zweck: Prüft Verbindung und lädt alle entity_ids.

**Execute Command:**
 * `POST /api/services/{domain}/{service}` (z.B. `/api/services/media_player/turn_off`)
 * Header: `Authorization: Bearer <token>`
 * Body: `{"entity_id": "media_player.tv"}`
### Datenmodelle / Schema-Entwurf (Room DB)
**Tabelle: Command**
| Spalte | Typ | Beispiel |
|---|---|---|
| id | Int (PK, Auto) | 1 |
| trigger_phrase | String | "tv aus" |
| entity_id | String | "media_player.lg_tv" |
| domain | String | "media_player" |
| service | String | "turn_off" |

**Tabelle: Settings** (Key-Value Store oder DataStore Preferences)
 * `ha_url` (String)
 * `ha_token` (String)
 * `active_stt_engine` (Enum: VOSK / WHISPER)
## 5. Non-Functional Requirements
 * **Background Processing (Kritisch):** Die STT-Engine und der Mikrofon-Zugriff müssen in einem Android Foreground Service (Typ: microphone) laufen. Eine permanente, lautlose Benachrichtigung in der Statusleiste ist obligatorisch, damit das OS die App nicht tötet.
 * **Permissions:** RECORD_AUDIO, INTERNET, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MICROPHONE.
 * **Error Handling:** Fällt die STT-Engine mit einer Exception aus, muss der Service die Exception abfangen (try/catch), loggen und sich selbst neu initialisieren. Bei Netzwerkfehlern (HA nicht erreichbar) wird die Aktion einfach verworfen.
 * **Performance:** Audiodaten werden in Chunks gelesen und nicht im Arbeitsspeicher kumuliert.