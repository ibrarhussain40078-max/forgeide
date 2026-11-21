# ForgeIDE

An on-device, AI-assisted IDE for Android. Build **websites** (HTML/CSS/JS) or **Android apps**
(Kotlin + Jetpack Compose) from a prompt, edit multi-file projects, preview them live, and export.

## Features
- Multi-file editor with syntax highlighting, file explorer, autosave (Room)
- Gemini code generation and "fix this error" using **your own Gemini API key** (free at aistudio.google.com), entered in Settings and stored encrypted with Android Keystore. No Firebase/App Check needed.
- Safe AI output handling: file names are sanitized (no path traversal), markdown fences are stripped, and every AI rewrite can be **undone**
- Website mode: sandboxed WebView preview, viewport presets, live console capture
- Android mode: interactive JSON-driven UI simulator, plus real debug APKs via GitHub Actions (the simulator is an approximate preview, not the compiled app)
- ZIP export, starter templates, QR sharing

## Architecture
| Layer | Path | Notes |
|---|---|---|
| UI (Compose) | `ui/` | `IdeViewModel` holds a single `StateFlow<IdeUiState>` |
| AI | `data/ai/` | `GeminiCodeGenerator` (Gemini REST API) + `PayloadParser` (pure, tested) |
| Persistence | `data/local/` | Room workspace per mode; GitHub PAT encrypted with Android Keystore (AES-GCM) |
| Build/export | `data/remote/`, `data/export/` | GitHub Actions build pipeline; ZIP packager |

## Building APKs (real, no simulation)
Tap Build and ForgeIDE:
1. Runs quick pre-flight checks (reports only real findings).
2. Creates **one commit** in your GitHub repo with a complete Gradle project: ForgeIDE-owned build scripts
   (AGP 8.7.3, Kotlin 2.0.21, Compose BOM, Gradle 8.9 via `setup-gradle`, so no wrapper is needed) plus your code.
   Your Kotlin is normalized to package `com.forgeide.app` and the manifest to match; stale generated files are removed.
3. Dispatches the workflow and finds **its own run by commit SHA**.
4. Shows the run's **real step progress**, then downloads the real debug APK.
5. On failure, shows the **real compiler/Gradle errors** from the job log, and "Fix with AI" uses those errors.

Without GitHub settings the build fails with a clear message; nothing is faked. Token requirements: repo contents write,
Actions read/write, and workflow write (to install the workflow file). It is encrypted with Android Keystore, or not saved at all.

## Run locally
1. Open in Android Studio (JDK 17).
2. Run on a device/emulator, open Settings (gear) and paste your Gemini API key.

## Test
```
./gradlew :app:testDebugUnitTest
```
CI runs unit tests and `assembleDebug` on every push/PR (`.github/workflows/ci.yml`).

## Known limitations / roadmap
- One workspace per mode (no multi-project list yet)
- Android generation depends on a fixed 4-file layout and the simulator JSON schema
- Generated code is not compiled before being applied; use Undo or the AI fixer
