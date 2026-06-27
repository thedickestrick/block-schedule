# Block Schedule

A simple, calm Android block-scheduling app with a **home-screen widget** that shows
today's tasks at a glance. Built for daily executive-function support: you define tasks
once, and recurring ones are scheduled automatically every day.

## What it does

- **Recurring tasks** are scheduled automatically: **daily, weekly, every 2 weeks,
  monthly, yearly**. (Weekly/bi-weekly let you pick specific weekdays, e.g. gym on
  Mon/Wed/Fri.)
- **One-time tasks** (doctor appointments, meetings) for a specific date.
- **Two kinds of timing:**
  - **Fixed time** — has a set start and length (Work 9–5, Sleep 11pm–7am, Gym 6pm).
  - **Flexible** — no set time; the app auto-drops it into the first open gap inside a
    window you choose (e.g. "30 min reading, sometime between noon and 9pm"). If the day
    is too full it tells you instead of silently dropping it.
- **Block scheduling:** everything is laid out on the day's timeline. Blocks that cross
  midnight (sleep) are handled, and overlapping tasks are flagged with a ⚠.
- **Home-screen widget:** today's schedule with the **current task highlighted ("NOW")**.
  Tap it to open the app. There's an "add widget to home screen" button in the app's top bar.
- **All data stays on the phone** (Room/SQLite). No accounts, no internet, fully private.

## Install on a phone (once)

Grab the latest APK from the releases page:

```
https://github.com/thedickestrick/block-schedule/releases/latest
```

1. On the phone, open that link and download **app-release.apk**.
2. Tap the file. Android will ask to allow installing from this source — approve it.
3. Open **Block Schedule**, add a few tasks, then tap the **open-in-new icon** (top-right)
   to drop the widget on the home screen — or long-press the home screen → Widgets →
   Block Schedule → "Today's Schedule".

Minimum Android version: **8.0 (API 26)**. After this one install, the app keeps itself
up to date (see below).

## Auto-updates

The app only has to be installed once. After that:

- On launch it checks the GitHub releases for a newer version (toggle in **Settings →
  Check for updates automatically**, on by default; there's also a **Check now** button).
- If there's a newer version, a banner offers **Update now** → it downloads the new APK and
  Android shows **one** "Update this app?" confirmation. Tap it and you're on the new version.
- The first update will ask you to allow "install unknown apps" for Block Schedule — a
  one-time grant. After that, updates are a single tap.

> Note: a sideloaded app can't update *fully* silently — Android always shows that one
> confirmation. That's the most automatic an off-Play-Store app can be.

### Publishing a new version (you, on the PC)

```powershell
.\release.ps1 -VersionName 1.2 -Notes "Added reminders before each block."
```

This bumps `APP_VERSION_CODE`, builds a signed APK, writes `version.json`, and creates a
GitHub release (tag `vX.Y`) with both files attached. The app reads `releases/latest`, so
the newest release is offered automatically on the next check. **Every release must be
signed with the same `blockschedule-release.jks`** (the script handles this) or phones will
refuse the update.

## Building from source (Windows)

Requirements: JDK 17, Android SDK with `platforms;android-34` and `build-tools;34.0.0`.

```powershell
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
# See "Loopback workaround" below — REQUIRED on this machine.
$env:JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=C:\bstmp"

# Debug build (for testing on an emulator):
.\gradlew.bat assembleDebug
# Signed release build (for installing on a real phone):
.\gradlew.bat assembleRelease
```

### Loopback workaround (important)

On this machine the default `TEMP` path breaks the JDK's AF_UNIX NIO pipe, so Gradle
fails with **"Unable to establish loopback connection"**. The fix is to point the JDK's
unix-domain socket directory at a short, clean path:

- A clean dir `C:\bstmp` exists for this.
- `gradle.properties` sets `-Djdk.net.unixdomain.tmpdir=C:\bstmp` for the Gradle daemon.
- For the Gradle **launcher** and Kotlin worker JVMs, also export
  `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\bstmp` before running `gradlew`
  (shown above). Without this, builds fail before compiling anything.

### Run on the emulator

```powershell
$sdk="$env:LOCALAPPDATA\Android\Sdk"
& "$sdk\emulator\emulator.exe" -avd trader_test &
& "$sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
& "$sdk\platform-tools\adb.exe" shell am start -n com.blockschedule/.ui.MainActivity
```

## Project layout

```
app/src/main/java/com/blockschedule/
  data/         Room entities, DAO, repository (Task, Category, Frequency)
  schedule/     Scheduler — pure recurrence + block-placement logic (no Android deps)
  ui/           Compose screens: Today timeline, All tasks, Add/Edit task
  widget/       Glance home-screen widget + pin helper
```

The scheduling logic in `schedule/Scheduler.kt` is deliberately pure and side-effect free,
so it's easy to test and is shared by both the app UI and the widget.

## Signing

The release APK is signed with `blockschedule-release.jks` (config in `keystore.properties`).
These are kept local and out of version control. **Keep the keystore safe** — you need the
same key to ship updates the phone will accept as upgrades.

## Ideas for later

- Minute-accurate widget refresh at each block boundary (currently refreshes on app use and
  every ~30 min via the system).
- Don't flag a small task fully nested inside a big block (e.g. lunch during work) as a conflict.
- Drag-to-reschedule and per-task custom colors.
- Richer reminder actions (snooze / mark done) or a persistent "today" notification.
