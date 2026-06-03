# Phone Coach — Design Handoff

This folder is a self-contained package to land the new visual direction on
top of the existing `eliyotz/android-_coacher` codebase
(branch `claude/android-screentime-coach-kU5Yv`).

## Contents

```
handoff/
├── README.md                ← you are here
├── HANDOFF.md               ← full design spec + screen-by-screen Compose notes
├── screens/                 ← high-res reference renders (PNG)
└── compose-stubs/           ← drop-in code:
    ├── Color.kt             →  app/src/main/java/com/coach/screentime/ui/theme/Color.kt
    ├── Type.kt              →  app/src/main/java/com/coach/screentime/ui/theme/Type.kt
    ├── Theme.kt             →  app/src/main/java/com/coach/screentime/ui/theme/Theme.kt (replaces existing)
    ├── Atoms.kt             →  app/src/main/java/com/coach/screentime/ui/components/Atoms.kt
    └── res/
        ├── drawable/
        │   ├── ic_launcher_background.xml
        │   ├── ic_launcher_foreground.xml
        │   ├── ic_launcher_monochrome.xml
        │   └── ic_notification.xml
        └── mipmap-anydpi-v26/
            ├── ic_launcher.xml
            └── ic_launcher_round.xml
```

## How to apply

> **Read `HANDOFF.md` first.** It has the screen-by-screen Compose breakdown and a
> suggested order of operations. Below is the abbreviated version.

1. **Fonts.** Download the four SIL-OFL families used and drop into `app/src/main/res/font/`:
   - Instrument Serif (Regular, Italic)
   - Geist (Regular, Medium, SemiBold)
   - Geist Mono (Regular)

   Each font file must be lowercase, e.g. `instrument_serif_regular.ttf`,
   `geist_mono_regular.ttf`, etc. The `Type.kt` stub references them by these
   names — keep them in sync.

2. **Tokens.** Copy the four files from `compose-stubs/` into the matching
   paths under `app/src/main/java/com/coach/screentime/`. `Theme.kt` replaces
   the existing file; the others are new.

3. **App icon.** Copy `compose-stubs/res/` over `app/src/main/res/`. Delete
   the old `ic_launcher_background.xml` / `ic_launcher_foreground.xml` first.

4. **Notification icon.** `ic_notification.xml` is referenced from
   `ForegroundTrackerService` (and any worker that fires a notification) — update
   `setSmallIcon(R.drawable.ic_notification)`. Today the project uses the
   launcher icon as a fallback; this gives a proper monochrome glyph for the
   status bar.

5. **Screens.** Work through `HANDOFF.md` §3 in order. The viewmodels and state
   objects don't need to change — every recommendation here is a layout/style
   repaint. The one place a data shape needs to grow is
   `WeeklyReportPrompt` → see §6 of the spec.

## Verification

The README's verification matrix still applies. After applying, sanity-check:

- Today screen ring colour matches the screenshot in `screens/today.png`
- Bottom nav uses thin outline icons, not Material filled
- Weekly letter card renders Instrument Serif italic
- App icon previews with a paper-cream background and terracotta iris on
  the launcher across Pixel/Samsung/OnePlus masks (use Android Studio's
  Image Asset preview)
- Notification icon is white-on-transparent (Android tints it)

## Open questions / decisions left to Claude Code

These are flagged in `HANDOFF.md` but worth surfacing here too:

1. **Markdown rendering for the weekly letter.** Use `compose-markdown` (Cyril
   Findeling's library) or hand-roll? Hand-rolling gives you tighter control of
   the editorial typography. Either is acceptable.

2. **Today coach-line source.** Spec says compute from `state` when a template
   matches, only call Gemini otherwise. Templates I'd start with:
   - over Social category by >10m before noon
   - 50% over weekly average by Wednesday
   - first day of a new streak
   - first day after breaking a streak

3. **Dark mode.** Token palette is light-first. Dark scheme is wired in
   `Theme.kt` but not visually proofed — defer to a second pass.

4. **Onboarding + Settings.** Not redesigned. They'll pick up tokens / typography
   automatically from the new `Theme.kt`; if they need a layout pass, ask.
