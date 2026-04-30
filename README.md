# Screen Time Coach (Android)

An AI-powered digital wellbeing app for **personal sideload** use. Designed around three ideas the typical "Digital Wellbeing" app gets wrong:

1. **Insights, not just blocks.** A weekly AI-generated behavior report (running on Sunday morning) explaining patterns in your week — when control breaks down, what rationalizations you give yourself, and one concrete suggestion for next week.
2. **Layered intervention.** A 5-second mindfulness pause on every open of a flagged app → AI negotiation when limits are crossed → opt-in Ulysses-contract hard lock for apps you don't trust yourself to negotiate over.
3. **Per-app + per-category limits.** Stops "whack-a-mole" — capping Instagram alone just pushes you to TikTok. A 90-minute social-media cap covers both.

The AI uses **Google Gemini Flash** (free tier) and is reachable via REST.

> Status: greenfield v1. Builds and runs end-to-end. Phases 1–4 of the plan are implemented (tracking, limits, mindfulness pause, AI negotiation, weekly insights). Phases 5–7 (observe-only onboarding with AI-recommended limits, Ulysses 24h cool-off enforcement, Gemini Nano fallback) are stubbed and noted in the code.

---

## Building

Requires Android Studio Iguana or newer (AGP 8.5, Kotlin 2.0).

```bash
cp local.properties.example local.properties
# Open local.properties and fill in:
#   GEMINI_API_KEY=ya29...    # get one at https://aistudio.google.com — free tier
#   sdk.dir=/path/to/Android/sdk

./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

The first launch walks you through three permissions:
1. **Usage access** (`ACTION_USAGE_ACCESS_SETTINGS`)
2. **Display over other apps** (`ACTION_MANAGE_OVERLAY_PERMISSION`)
3. **Notifications** (Android 13+)

It also offers a deep-link to system battery settings — strongly recommended on Samsung/Xiaomi/Oppo/OnePlus/Realme, where unwhitelisted apps get killed within minutes.

The app starts in **Observe mode**. No overlays fire until you switch to Enforce in Settings.

---

## Architecture

```
Foreground tracker (always running)
  ├── AccessibilityService       — real-time WINDOW_STATE_CHANGED (fast path)
  ├── UsageStatsManager poller   — 2s polling fallback / reboot backfill
  └── SessionAggregator          — coalesces window events into sessions + opens

Repository layer
  ├── AppRegistry      — installed-app scan + app→category mapping
  ├── Room DB          — sessions, rollups, interventions, verdicts, reports, goals
  ├── SettingsStore    — Datastore (mode, strictness, pause length, goal)
  └── GeminiClient     — Retrofit-backed model-agnostic interface

InterventionEngine (event-driven)
  ├── Layer 1: MindfulnessPauseOverlay   (5s, every open of flagged app)
  ├── Layer 2: NegotiationOverlay        (AI judge after limit crossed)
  └── Layer 3: HardLockOverlay           (opt-in, sends to home)

Workers
  ├── WeeklyReportWorker   — Sundays 8am, generates the AI weekly report
  └── DailyRollupWorker    — prunes session rows older than 90 days

UI (Compose + Hilt + Navigation)
  ├── Today      — minutes, opens, adherence ring, streak
  ├── Insights   — 7-day chart + weekly AI reports (the headline screen)
  ├── Limits     — flag apps, set per-app / per-category caps, opt into hard lock
  └── Settings   — strictness, mode, mindfulness pause, long-term goal
```

---

## The four AI surfaces

The Gemini key powers four distinct prompts (see `app/src/main/java/com/coach/screentime/ai/Prompts.kt`):

1. **JudgePrompt** — per-event accept/reject when the user requests an extension. Includes app, category, today's usage, prior reasons given today, time of day, the user's stated long-term goal, and strictness setting. Returns JSON `{verdict, extensionMinutes, explanation}` with malformed-response fallback per strictness level.
2. **WeeklyReportPrompt** — Sunday morning: takes the week's rollups + interventions + verdicts and writes 200–400 words of personalized observation.
3. **LimitRecommenderPrompt** — after observe-only mode, proposes initial per-app and per-category caps based on actual usage. (Wired but onboarding flow doesn't auto-trigger it yet — Phase 5.)
4. **In-the-moment context** — every prompt above includes the user's goal and same-day history so the coach has memory across the conversation, not just one event at a time.

---

## Privacy

- **App names + your reasons go to Google.** The app is sideload only, so this is your call. Reasons can be deeply personal — be aware.
- **GeminiClient is an interface.** Swapping to on-device Gemini Nano (ML Kit GenAI) is a one-file change once you decide to enable it.
- **No analytics, no Firebase, no telemetry.** All other data stays in Room on-device.

---

## Verification

| What                         | How                                                                                       |
|------------------------------|-------------------------------------------------------------------------------------------|
| Tracking accuracy            | Use the device for 30 min, lock screen, reboot. Today screen should match Android's built-in Digital Wellbeing within ~10%. |
| Mindfulness overlay          | Flag an app in Limits, set Mode = Enforce in Settings, open the app — overlay shows for the configured seconds. |
| AI negotiation               | Set a 1-min cap on a flagged app, exceed it, type a reason, observe verdict.              |
| Hard lock                    | Enable hard lock on an app, exceed limit — overlay sends you home and stays in front when you reopen. |
| Weekly report                | Tap "Generate now" on the Insights screen after using the app for a few days.             |
| Boot persistence             | Reboot phone, wait 30 sec, observe the persistent tracker notification reappears.         |

---

## Known limitations / road map

- **Onboarding's "observe-only with AI-recommended limits" flow** (plan Phase 5) — observe-mode is the default, but the limit-recommendation prompt isn't yet auto-fired at day 3. Planned.
- **Ulysses cool-off enforcement** (plan Phase 6) — the toggle is stored with `hardLockToggleAt`, but the disable-after-24h enforcement is not yet read at toggle time. Easy fix, intentionally deferred to keep Phase 1–4 shippable.
- **Gemini Nano fallback** (plan Phase 7) — the `GeminiClient` interface is ready; the Nano implementation is not.
- **Overlay over fullscreen apps** — some video players and games block overlays. Notifications fall back automatically when an overlay can't be shown.
- **OEM battery killers** — onboarding deep-links to battery settings, but each OEM has its own special-case settings page; users on Samsung/Xiaomi/Oppo/OnePlus may need to dig deeper.

---

## License

Personal use. Not for redistribution — the API key is bundled in the APK.
