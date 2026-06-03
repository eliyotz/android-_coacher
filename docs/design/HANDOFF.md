# Phone Coach — Design Handoff

> Reference designs for **eliyotz/android-_coacher** (branch `claude/android-screentime-coach-kU5Yv`).
> Maps the visual direction onto the existing Compose architecture in `com.coach.screentime.ui.*`
> and the resources in `app/src/main/res/`.

---

## 0 · Intent

The app is a coach, not a curfew. Visually that translates to:

- **Editorial, not utilitarian.** The weekly AI report is the headline feature and the rest of the app supports it. Serif headlines (Instrument Serif), generous whitespace, the coach speaks in first person.
- **Paper, ink, terracotta.** A warm desaturated cream / ink palette so the app feels like a notebook you keep, with one assertive accent (terracotta) reserved for coach interventions.
- **Honest data.** Every number in the app uses Geist Mono with tabular numerals. Stats are not hidden behind progress bars and friendly framing — the user is an adult.
- **Layered intervention is escalating temperature.** Mindfulness pause is cool green-black. Negotiation is warm paper. Hard lock is near-black with terracotta. The visual language signals where on the strictness ladder the user is.

---

## 1 · Design tokens

### Colors → `app/src/main/res/values/colors.xml` + Theme.kt

| Token              | Light hex   | Notes |
|--------------------|-------------|-------|
| `paper`            | `#EFE9DC`   | Default surface (= `background`)                |
| `paper_2`          | `#E8E1D0`   | Pressed / segmented track                       |
| `card`             | `#FBF7EE`   | `surface`                                       |
| `card_2`           | `#F5EFE0`   | `surfaceContainerLow`, secondary card           |
| `ink`              | `#1A1916`   | `onSurface`, primary text                       |
| `ink_2`            | `#34302A`   | Body copy                                       |
| `ink_mute`         | `#7A7466`   | `onSurfaceVariant`, captions                    |
| `rule`             | `#D8D1BD`   | Borders, dividers (hard)                        |
| `rule_soft`        | `#E5DECC`   | Borders within cards (soft)                     |
| `accent`           | `#C25A33`   | `primary` — coach moments, CTAs                 |
| `accent_2`         | `#A24923`   | Pressed primary                                 |
| `sage`             | `#6E7F5E`   | `tertiary` — "healthy / under cap"              |
| `gold`             | `#C58A2E`   | Warning (over cap, not yet locked)              |
| `alert`            | `#A52E1A`   | `error` — hard lock, overdue tasks              |
| `alert_soft`       | `#F1D6CC`   | Alert backgrounds                               |
| `ink_deep`         | `#14130F`   | Hard-lock + mindfulness backgrounds             |
| `ink_deep_2`       | `#1F1D17`   | Cards on deep surfaces                          |
| `paper_on_deep`    | `#F1ECDE`   | Text on deep surfaces                           |
| `paper_on_deep_mute`| `#A39C8B`  | Muted text on deep surfaces                     |

**Dark mode** mirrors with `paper`/`card` swapped for `ink_deep`/`ink_deep_2` and text inverted; accents stay the same hex. Defer dark mode to a second pass; this design is light-first.

### Type scale → new `Type.kt`

| Token        | Family            | Size / line height | Use |
|--------------|-------------------|--------------------|-----|
| `display`    | Instrument Serif italic | 52 / 56  | Cover, weekly letter opening |
| `headlineLg` | Instrument Serif  | 36 / 40            | "Morning, Eli.", focus mode title |
| `headlineMd` | Instrument Serif  | 26 / 30            | Screen headers (Today, Limits, Insights, Tasks) |
| `headlineSm` | Instrument Serif  | 22 / 28            | Coach quotes, intervention copy |
| `titleMd`    | Geist 600         | 17 / 22            | Card titles, app row label |
| `titleSm`    | Geist 600         | 14.5 / 20          | List headers |
| `bodyMd`     | Geist 400         | 14 / 22            | Default body |
| `bodySm`     | Geist 400         | 13 / 20            | Secondary body |
| `meta`       | Geist 400         | 12 / 17            | Captions, supporting text (`ink_mute`) |
| `eyebrow`    | Geist 500 caps    | 10 / 14, +0.18em   | Section labels |
| `mono`       | Geist Mono 400 tnum | 11–22            | All numbers, package names, timestamps |

**Fonts to add:** `app/src/main/res/font/`
- `instrument_serif_regular.ttf`
- `instrument_serif_italic.ttf`
- `geist_regular.ttf`, `geist_medium.ttf`, `geist_semibold.ttf`
- `geist_mono_regular.ttf`

All four families are SIL OFL on Google Fonts — drop into `font/` and reference via `FontFamily` in `Type.kt`.

### Geometry

- Radii: `r-sm 8 · r-md 14 · r-lg 18 · r-xl 24 · pill 999`
- Spacing scale: 4 / 8 / 12 / 16 / 20 / 24 / 32 / 48 (multiples of 4)
- Stroke: 1px default rule, 1.5px on emphasised borders
- Adherence ring stroke: 10px
- App glyph: 34dp w/ 10dp radius, 36dp w/ 12dp radius for limits screen
- Card padding: `20 / 18` (hero), `16 / 14` (standard), `16 / 12` (list rows)
- Min hit target: 44dp (you already follow this — keep it)

---

## 2 · Components → `ui/components/`

Components below map onto the existing folder. Where a file exists, replace its body; where it doesn't, create it.

| Existing file                          | Action |
|----------------------------------------|--------|
| `AdherenceRing.kt`                     | Replace color logic (see §3.1). Use 10dp stroke, draw label as two `Text` rows (`%`, eyebrow). |
| `UsageRow.kt`                          | Repaint to match the row in §3.1 — app glyph + name (semibold) + monospace stats + optional delta chip + bar below. |
| `PunishmentBanner.kt`                  | Restyle as the dark intervention card in §3.4 (Tasks). |
| **NEW** `AppGlyph.kt`                  | 34dp rounded square, brand-mapped color from `Categories.kt`, two-letter monogram in Instrument Serif. |
| **NEW** `Pill.kt`                      | Bordered pill with optional leading icon. 6×10 padding. |
| **NEW** `SegmentedTab.kt`              | Replaces M3 `TabRow` on Limits — see §3.3. |
| **NEW** `UsageBar.kt`                  | 6dp height, `rule_soft` track, sage fill, terracotta when over, alert when hard-lock. |
| **NEW** `CoachAvatar.kt`               | 22 / 28dp ink circle w/ eye icon. Used wherever the coach "speaks." |
| **NEW** `Eyebrow.kt`                   | One-line composable for the all-caps section labels. |

### Iconography

Replace `Icons.Filled.*` with a small custom set drawn at 24×24, 1.6 stroke. Place as vector drawables in `res/drawable/`:

```
ic_today.xml      ic_tasks.xml      ic_insights.xml   ic_limits.xml
ic_settings.xml   ic_eye.xml        ic_lock.xml       ic_focus.xml
ic_streak.xml     ic_overdue.xml    ic_send.xml       ic_spark.xml
```

The bottom-nav icons are intentionally lightweight (1.6 stroke, no fill) — match the rest. Filled material icons clash with the editorial type.

---

## 3 · Screen-by-screen Compose notes

### 3.1 Today → `ui/today/TodayScreen.kt`

The current screen has the right structure (ring → category list → app list). Visual repaint only; the `TodayViewModel` and state object don't need to change.

**Layout:**

```
ScreenHeader(eyebrow = "Tuesday · May 14", title = "Morning, $name.",
             right = focus pill that opens FocusActivity)

HeroCard {
  Row {
    AdherenceRing(state.adherence)      // 148dp
    Column {
      Eyebrow("So far today")
      DisplayText(formatMin(state.totalMinutes))   // Instrument Serif 40
      MetaRow("${state.totalOpens} opens · goal ${formatMin(state.goalMinutes)}")
      Row {
        if (state.streak > 0) Pill(icon = ic_streak) { "$streak-day streak" }
        state.worstHourLabel?.let { Pill { "Worst hour 22:00" } }
      }
    }
  }
}

if (state.coachLine != null) CoachLine(state.coachLine)
   // e.g. "You're 17 minutes over on Social, and it's only 11am."
   // Bind to a one-line LLM call (existing prompt has the data) — see §6.

EyebrowHeader("Categories")
Card { state.categories.map { CategoryRow(it) } separated by Divider }

EyebrowHeader("Apps · top today  ${apps.size}")
Card { state.apps.map { AppRow(it) } separated by Divider }
```

**Adherence ring color thresholds:**

```kotlin
val color = when {
  adherence >= 0.8f -> SageGreen      // #6E7F5E
  adherence >= 0.5f -> WarnGold        // #C58A2E
  else              -> AccentTerracotta // #C25A33
}
```

(Replaces the current `#34D399 / #FBBF24 / #F87171` triad.)

**Delta chip in AppRow:** replace `#EF4444 / #10B981` with `accent / sage`. The arrow glyph stays.

### 3.2 Insights → `ui/insights/InsightsScreen.kt`

Three sections in this order:

1. **7-day chart card** — title eyebrow "Last 7 days", display number for week total (Instrument Serif 30), `↑ 12% vs last week` chip on right, then the bar chart. Goal line is a horizontal terracotta dashed line at `goalMinutes`. Bars sage when under goal, terracotta when over.
2. **Hour-of-day heatmap card** — eyebrow + meta + the existing heatmap, recoloured to a terracotta alpha ramp on `rule_soft` empty cells. Mono day labels and hour ticks.
3. **Weekly letter card** — *this is the headline UI of the entire app*:

```kotlin
WeeklyLetterCard(report) {
  Row {
    CoachAvatar()
    Column {
      Eyebrow("Week ${report.weekNumber} · letter from your coach")
      MetaText("${report.dateRange} · written ${report.timestamp}")
    }
  }
  Spacer(18.dp)
  Text(report.openingSentence, style = headlineSm, fontStyle = Italic)  // pull-quote
  Spacer(14.dp)
  MarkdownBody(report.markdownBody, style = bodyMd, color = ink_2)      // generous leading
  Spacer(18.dp)
  Divider()
  Row { MetaText(report.derivedFrom); Spacer(weight = 1f); GhostButton("Mark as read") }
}
```

Behind this UI is the existing `WeeklyReportPrompt`. **One prompt change is required** — see §6.

After the latest letter, list older letters as compact rows (title "Week N · letter", meta = the pull-quote of that week) tapping to expand inline.

### 3.3 Limits → `ui/limits/LimitsScreen.kt`

Replace the `TabRow` with a single segmented control (see `SegmentedTab.kt`). The two existing tabs (Apps / Categories) stay, but the visual treatment changes.

**Above the list (Apps tab only):** a "Coach has a suggestion" recommendation card — dashed terracotta border, sparkle icon. Bind to `LimitRecommenderPrompt` (Phase 5 in README). When dismissed, persist a `limitsRecommendationDismissedAt` in `SettingsStore`.

**App row card layout:**

```
Row {
  AppGlyph(app.name)
  Column { TitleMd(app.displayName); MetaMono(app.packageName) }
  Switch(checked = app.isFlagged, accent = AccentTerracotta)
}

if (app.isFlagged) {
  DashedDivider()
  Eyebrow("Daily limit")
  Row {
    NumberPill(app.perAppDailyMinutesCap)   // mono 22pt + " minutes / day"
    Column(weight = 1f) { Meta("Used today"); UsageBar; MonoMeta("47m / 30m") }
  }
  HardLockTile(app)
}
```

**HardLockTile**: tinted background (`alert_soft` 0.35α when enabled, `paper_2` otherwise), lock icon + label + supporting copy:
- Enabled: *"Can't be turned off for 22h 14m. Ulysses contract."*
- Cool-off pending: *"Lock still active. Disables in 3h 41m."*
- Default: *"No negotiation. Sends you home when the cap hits."*

This is also the place to wire the **24-hour cool-off enforcement** from README Phase 6 — the toggle should refuse to flip off (snackbar: "Cool-off active. 22h 14m remaining.") rather than just *display* the cool-off; toast/snackbar wording in `strings.xml`.

### 3.4 Tasks → `ui/tasks/TasksScreen.kt`

Group tasks into four sections by escalation state: **Overdue**, **Today**, **Upcoming**, **Closed today**. Each section preceded by an `Eyebrow` with a small count.

`PunishmentBanner` should be rendered at the top, *before* the sections, when `state.activePunishments` is non-empty. Use the dark intervention card style: `ink_deep_2` background, terracotta lock icon, eyebrow "Coach intervention", title, body in `paper_on_deep_mute`.

`TaskCard`:

```
Row {
  Checkbox(state == "judged_done", border = if (overdue) alert else rule)
  Column {
    TitleMd(task.title, strikethrough if judged_done)
    Row {
      MetaMono(overdueLabel, color = if (overdue) alert else ink_mute)  // "2d overdue" / "due today"
      MetaText("· ${stateLabel}", color = stateColor)
    }
    if (notes) MetaText(notes, color = ink_2)
    if (grantedDelayCount > 0) Row { ic_eye; MetaText("Coach granted N delays.") }
  }
}
```

Connection chip in header (right slot): a pill with a sage dot + the connected email. Loading/disconnected/not-configured states use a centered helper — keep the current text but typeset it `headlineSm` italic so it doesn't feel like an error.

### 3.5 Focus mode → `focus/FocusActivity.kt`

Currently a column of Buttons. Repaint to the layout in the screen:
- Back chevron + eyebrow "Focus mode"
- Large headline (Instrument Serif 38): "Hard-lock every flagged app."
- Body explaining the rules
- Three `FocusDurationRow` cards (30 / 60 / 120 min, with sub-copy)
- "Custom" mono input + start button
- Footer card explaining the persistent notification

Active state (`activeMins > 0`): replace the picker with a single big "X minutes left" card and an `OutlinedButton("End focus now")`.

### 3.6 Intervention overlays

These are not in a screen file today — they're rendered by `InterventionEngine` over `WindowManager`. Treat each as a separate `@Composable` mounted via the existing overlay infra:

**Mindfulness pause** → `intervention/overlay/MindfulnessOverlay.kt`
- Background `#1A2421` (cool green-black, intentionally *not* `ink_deep`)
- Concentric breathing rings: 4 circles, opacity ramp 0.4 → 0.15
- Center counter: Instrument Serif 52, current second remaining
- Tagline: italic serif 26, max width 280dp, line-height 1.3 — *"Breathe in, then ask: what am I actually looking for here?"* The line should rotate from a small set (5–8) tied to the time of day; deterministic by `hour % N` so the user sees the same line at the same hour and starts to associate it.
- Bottom: thin progress bar (60% sage) + "auto-continues / tap & hold to skip"

**AI Negotiation** → `intervention/overlay/NegotiationOverlay.kt`
- Bottom sheet, not full-screen. The user should still see the offending app (greyed/dimmed) above. That preserves accountability — they can see what they're about to negotiate to keep doing.
- 24dp top radius, paper card surface
- Drag handle, coach avatar + "The coach is listening" title
- Verdict block above input renders only when `state.phase == ACCEPT || REJECT`
  - Reject: sage-on-cream is wrong; use alert_soft background, alert eyebrow, italic serif quote
  - Accept: pale sage background `#E5ECDB`, sage eyebrow "Verdict · X more minutes", italic serif quote
- Input field is a single `OutlinedTextField` repainted: `card_2` fill, `rule` border, body 14.5sp. Character counter bottom-left, "Send to coach" terracotta primary button bottom-right (renders "Thinking…" while `phase == JUDGING`).
- Footer meta: bell icon + "Coach has memory. It's seen your last 5 reasons today."

**Hard lock** → `intervention/overlay/HardLockOverlay.kt`
- Full-screen `ink_deep`
- Eyebrow "Hard lock · $appName" terracotta
- Display copy (Instrument Serif 36 white): *"You signed a contract with yourself."*
- Body explains when/why they signed it, citing the actual timestamp from `hardLockToggleAt`
- "Lock ends" + "Streak protected" two-up footer
- "Take me home" terracotta button as the only action; pressing back is a no-op that also goes home (current behavior)

The dark + terracotta combination is deliberately the same as the Punishment banner on Tasks — same visual language wherever the coach is *enforcing*, never negotiating.

---

## 4 · Bottom nav

`ui/Navigation.kt` currently shows five tabs with `Icons.Filled.*`. Replace icons with the custom 1.6-stroke set (§2). Hide the M3 indicator pill (`NavigationBarItemDefaults.colors(indicatorColor = Transparent)`), use a 6dp terracotta dot under the active tab label instead.

The active tab's label and icon are `accent`; inactive are `ink_mute`. Tab bar background is `card` with a 1px `rule` top border.

---

## 5 · App icon → `res/mipmap-anydpi-v26/ic_launcher.xml`

The mark is an eye drawn as a Buddhist almond shape with three meditation arcs above it — the existing `ic_launcher_foreground.xml` should be replaced with the SVG below (converted to AVD), and the background swapped to a radial paper gradient (or a flat `#EFE9DC` if AVD doesn't support radial gradients on min SDK).

```
foreground (108×108 viewBox):
  <vector ...>
    <!-- almond eye -->
    <path android:fillColor="#1A1916"
      android:pathData="M26,50 C32,34 76,34 82,50 C76,66 32,66 26,50 z"/>
    <!-- iris terracotta gradient -->
    <path android:pathData="M54,38 A12,12 0 1 1 54,62 A12,12 0 1 1 54,38 z">
      <aapt:attr name="android:fillColor">
        <gradient android:type="linear"
          android:startX="42" android:startY="38" android:endX="66" android:endY="62">
          <item android:offset="0"  android:color="#C25A33"/>
          <item android:offset="1"  android:color="#7A2E14"/>
        </gradient>
      </aapt:attr>
    </path>
    <!-- pupil -->
    <path android:fillColor="#14130F"
      android:pathData="M54,46 A4.2,4.2 0 1 1 54,54.4 A4.2,4.2 0 1 1 54,46 z"/>
    <!-- catchlight -->
    <path android:fillColor="#F5EFE0"
      android:pathData="M50.6,46.6 A1.7,1.7 0 1 1 50.6,50 A1.7,1.7 0 1 1 50.6,46.6 z"/>
    <!-- upper lash -->
    <path android:strokeColor="#1A1916" android:strokeWidth="2.4"
      android:strokeLineCap="round" android:fillColor="#00000000"
      android:pathData="M27,49 C32,34 76,34 81,49"/>
    <!-- three meditation arcs -->
    <path android:strokeColor="#C25A33" android:strokeWidth="1.2"
      android:fillColor="#00000000"
      android:pathData="M32,32 C42,22 66,22 76,32"/>
    <path android:strokeColor="#C25A33" android:strokeWidth="1.0"
      android:strokeAlpha="0.55" android:fillColor="#00000000"
      android:pathData="M38,26 C46,18 62,18 70,26"/>
    <path android:strokeColor="#C25A33" android:strokeWidth="0.8"
      android:strokeAlpha="0.30" android:fillColor="#00000000"
      android:pathData="M44,21 C50,15 58,15 64,21"/>
  </vector>

background:
  <vector ...><path android:fillColor="#EFE9DC" android:pathData="M0,0h108v108h-108z"/></vector>
```

**Monochrome themed icon** (Android 13+): single white version of the almond + iris + arcs, no fill. Add at `res/drawable/ic_launcher_monochrome.xml` and reference from `ic_launcher.xml`:

```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>
</adaptive-icon>
```

**Notification icon** (`R.drawable.ic_notification`): a flat 24×24 vector of the eye glyph at single color (Android tints it). Used by `ForegroundTrackerService` and the persistent notification.

---

## 6 · Prompt change required (Insights → weekly letter)

The current `WeeklyReportPrompt` in `ai/Prompts.kt` returns 200–400 words of markdown. The design needs **two extra fields**:

```json
{
  "openingSentence": "Eli — last week you spent 9h 14m on Social, and three of those hours arrived after 10pm.",
  "pullQuote":       "The hardest moments aren't the limits. They're the ten seconds before.",
  "body":            "<200-400 word markdown body>",
  "derivedFromText": "Drawn from 47 sessions, 6 negotiations, 1 hard-lock."
}
```

`openingSentence` is rendered as the big italic-serif hook above the body. `derivedFromText` is the meta line under the divider. Keep `body` as markdown — render via a small markdown parser (the existing `MarkdownBody` placeholder).

Pull-quote stays optional for now (only shown when present, e.g. in the older-letters list).

Similar small extension for `TodayScreen`: a single coach line above the Categories list. Reuse the JudgePrompt's coach voice — no new model call needed if you can compute it client-side from `state` (e.g. "You're 17 minutes over on Social, and it's only 11am.") — call Gemini only if no template matches.

---

## 7 · Strings

Add to `res/values/strings.xml` (keep existing keys):

```xml
<string name="today_greeting_morning">Morning, %1$s.</string>
<string name="today_greeting_afternoon">Afternoon, %1$s.</string>
<string name="today_greeting_evening">Evening, %1$s.</string>

<string name="insights_title">Patterns</string>
<string name="limits_title">What you cap.</string>
<string name="tasks_title">Open loops.</string>
<string name="settings_title">Setup &amp; rules.</string>

<string name="focus_title">Hard-lock every flagged app.</string>
<string name="focus_body">No mindfulness pause. No AI negotiation. Until the timer ends, the answer is no.</string>

<string name="mindfulness_default_prompt">Breathe in, then ask: what am I actually looking for here?</string>
<string name="negotiation_listening">The coach is listening.</string>
<string name="negotiation_subtitle">Tell it why you need more time.</string>
<string name="negotiation_memory_hint">Coach has memory. It\'s seen your last %1$d reasons today.</string>
<string name="negotiation_send">Send to coach</string>
<string name="negotiation_thinking">Thinking…</string>

<string name="hardlock_title">You signed a contract with yourself.</string>
<string name="hardlock_take_me_home">Take me home</string>

<string name="coach_intervention_eyebrow">Coach intervention</string>
```

The voice rule: short sentences, present tense, the coach refers to itself as "I" or "the coach" never "we" or "the system."

---

## 8 · Implementation order (suggestion)

1. **Tokens.** Add `Color.kt`, `Type.kt`, replace `Theme.kt`. Drop the four font families into `res/font/`. Run the app — everything will already look 70% there because Material's defaults pick up the new scheme.
2. **App icon.** One-file change to the launcher mark + monochrome variant. Visible win, builds intuition for the brand.
3. **Components.** `AppGlyph`, `Pill`, `Eyebrow`, `UsageBar`, `CoachAvatar` — the atoms behind every screen.
4. **Today + Limits.** Highest visual mileage from token + component changes.
5. **Insights weekly letter.** Requires the small prompt change in §6.
6. **Tasks + Focus.** Straight repaints.
7. **Overlays.** Last, because they're the hardest to test in isolation. Use the verification matrix in README to manually trigger each.

---

## 9 · Out of scope for this pass

- Onboarding (3-permission flow) — already exists, not in this design pass.
- Settings — exists, not redesigned here. Apply tokens only.
- Goal revision flow — not yet built; design when Phase 5 lands.
- Gemini Nano fallback UI — defer with Phase 7.

Keep `OnboardingScreen.kt` and `SettingsScreen.kt` token-compatible (use the new colors, type, components) but their layout doesn't need to change.
