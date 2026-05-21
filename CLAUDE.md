# CLAUDE.md

Project-specific guidance for Claude Code working in this repo.

## What this is
Personal-use Android screen-time coach. Kotlin + Jetpack Compose + Hilt + Room + WorkManager. AI is **Google Gemini 2.5 Flash** (REST, free tier). Sideload-only; no Play Store, no telemetry. See `README.md` for the user-facing description.

## Build & install
```
.\gradlew installDebug        # debug variant has applicationId suffix .debug
```
There is a connected Samsung A54 (`R5CWB2F3HJN`) at `C:\Users\User\AppData\Local\Android\Sdk\platform-tools\adb.exe`. UI verification via screenshot + `adb shell uiautomator dump` to locate bounds.

Package on device: `com.coach.screentime.debug`; main activity: `com.coach.screentime.ui.MainActivity`.

## Things that have bitten us — check before assuming
- **Gemini model**: use `gemini-2.5-flash` with `thinkingBudget = 0`. Earlier code used `gemini-flash-latest` (404) or `gemini-2.0-flash` (chronic 429 on this account). Models share quota *per family*, so switch families to escape rate limits.
- **`responseSchema` is per-call**, not global. The single `GeminiClient.generate(systemPrompt, userPrompt, responseSchema?)` is shared by `NegotiationViewModel` (verdict/extensionMinutes/explanation shape) and `TaskEscalationEngine` (decision/delay/punishment shape). A hardcoded schema in the client silently corrupts the other caller's parse path — every fallback fires.
- **OAuth needs `client_secret`** for the Web-client server-auth-code flow. It must be in `local.properties` AND surfaced via `buildConfigField` in `app/build.gradle.kts`. Without it, Google returns `invalid_client` and tokens are never saved (UI shows nothing happening).
- **Activity lookup in Compose**: `LocalContext.current` is a `ContextThemeWrapper`, not the Activity. Use a `findActivity()` walk over `ContextWrapper.baseContext` — a raw `as? Activity` cast returns null and silently disables the connect button.
- **Light mode is forced** in `ui/theme/Theme.kt` (`darkTheme = false`). The design is paper-only; don't gate on system theme.
- **`enableEdgeToEdge()` is required** in `MainActivity` so the bottom nav isn't clipped under the system bar. Without it the Tasks tab is half-visible.
- **`Log.d()` returns `Int`** — if it's the last expression in a `runCatching { … }`, Kotlin infers `Result<Int>` and the surrounding `Result<Unit>` signature stops compiling. Add an explicit `Unit` trailing line.
- **`pm clear` wipes EncryptedSharedPreferences** including the OAuth tokens. The user must reconnect Google Tasks via Settings after a reset.

## Where things live
| Concern | File |
|---|---|
| Gemini REST client + retry/backoff | `ai/GeminiRestClient.kt` |
| App-block negotiation overlay | `ai/NegotiationViewModel.kt` + `intervention/Overlays.kt` |
| Task escalation state machine, AI judge, fallback | `enforcement/TaskEscalationEngine.kt` |
| OAuth (refresh + access token, encrypted store) | `auth/GoogleAuthRepository.kt`, `auth/TokenStore.kt`, `auth/OAuthApi.kt` |
| Room DB, migrations, DAOs | `data/db/AppDatabase.kt`, `data/db/dao/*`, `data/db/entities/Entities.kt` |
| Per-screen ViewModels | `ui/<screen>/<Screen>ViewModel.kt` |
| Prompts (judge, weekly report, task judge, nudge, etc.) | `ai/Prompts.kt` |
| Build-config wiring for API keys | `app/build.gradle.kts` (top) |

## Task escalation states (in `TasksRepository.STATE_*`)
`untouched → prompted → working | delayed | dismissed_pending → judged_done`
- `prompted`: the engine fired a notification; waiting on the user.
- `working`: user said yes — follow-up worker re-prompts after 30 min if still incomplete.
- `delayed`: AI granted a delay until `delayedUntilMs`. Card shows "delay ends in Xh Ym".
- `dismissed_pending`: AI punished. Re-prompts up to `MAX_REPROMPTS_PER_DAY` (3) once the punishment expires.
- `judged_done`: terminal locally. The next Google sync may not reflect this — scope is `tasks.readonly` so we can't write back.

## Tasks bottom sheet (current UX)
Tapping a task card opens a `ModalBottomSheet`:
1. **Yes, I'm doing it now** → `engine.markWorking`
2. **Mark as completed** → `engine.markCompleted` (local only)
3. **Can't right now — ask coach** → reason field → `engine.submitReason` → Gemini → Snackbar with the verdict

If no state row exists (task never prompted by the worker), `submitReason`/`markWorking` synthesize a default state instead of returning early.

## Fallback behavior
If Gemini fails all 3 retries, `deterministicFallback()` produces a punishment based on `severityFloor` (light/medium/harsh from prior delay count). The user's reason is **not** saved to `task_delays` in this case (the AI never saw it). The fallback message is neutral: "Entering focus mode for X min…", not "Coach unreachable…".

## Style
- No unsolicited comments. Single-line `//` only when the *why* is non-obvious.
- Don't create planning/analysis markdown files; use the conversation.
- Prefer editing over creating new files.
- Don't add backwards-compat shims, `_unused` renames, or "removed in v…" comments.
