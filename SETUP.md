# Airgap — build and run

**This file is the build truth. If another doc disagrees with this one, this one wins.**

Last merged: 5 Sept, Chinmay pulled Shan's 8 commits and added the Gradle wrapper.

---

## The repo is already a Gradle project

Do **not** create a new Android Studio project. Do **not** copy files into
`app/src/main`. An older version of this file said to do that — it was true for
about two hours and is now wrong.

The app module points straight at our source:

```kotlin
// app/build.gradle.kts
sourceSets["main"].apply {
    java.srcDirs("../airgap-src/java")
    res.srcDirs("../airgap-src/res")
    assets.srcDirs("../airgap-src/assets")
    manifest.srcFile("../airgap-src/AndroidManifest.xml")
}
```

So: **edit `airgap-src/`, then build.** Nothing gets copied anywhere.

---

## Build it

```powershell
$env:JAVA_HOME = "C:\Users\chinm\jdk21\jdk-21.0.12.1+1"
cd "D:\PROJECTS\IQoo Hackthon\mobile-model"
.\gradlew.bat assembleDebug
```

APK lands at `app\build\outputs\apk\debug\app-debug.apk` — about **71 MB**.

If it is ~13 MB, MediaPipe did not get packaged and the model will never load.

### Two version traps, both real, both cost an hour

| Trap | Symptom | Why |
|---|---|---|
| **Gradle 9.x** | `InternalProblems` class not found | Gradle 9 deleted an internal API that AGP 8.13 calls. The wrapper is pinned to **8.13** — use `gradlew`, not a system `gradle`. |
| **JDK 25** | error is just the text `25.0.3` | Gradle 8.13 does not know JDK 25. Android Studio Quail ships JBR 25, so its default JDK will fail. Point `JAVA_HOME` at **JDK 21**. |

Versions that work together: **JDK 21 · Gradle 8.13 · AGP 8.13.0 · Kotlin 2.1.0 · compileSdk 36 · minSdk 31**.

---

## Install it — Office Kit, not adb

**10% of the score is Office Kit usage, and it is read off the phone, not from
what we say in the pitch.** There is also a "Most iQOO Usage" prize on top.

1. Drag `app-debug.apk` into **Office Kit file transfer**
2. Tap the file on the phone to install
3. Use **Screen Mirror** when testing the block screen
4. Use **Super Clipboard** to get logcat text back to the laptop

`adb install` works and earns us nothing. Use it only when Office Kit is broken.

### The model file

`GemmaDetector` looks for exactly this path on the phone:

```
/data/local/tmp/llm/gemma3-1b-int4.task
```

If it is missing the app does not crash — it drops to rules only. That is on
purpose.

---

## Turn on three permissions

Open the app, tap the three buttons in order. The status box shows `OK` or
`MISSING` for each.

1. **Grant SMS access** — normal popup
2. **Allow draw over other apps** — sends you to Settings
3. **Allow notification access** — sends you to Settings

---

## Check it works

The status box at the top of the app tells you which engine is live:

| Shown | Meaning |
|---|---|
| `Rules + Gemma 3 1B (on-device)` | Model loaded. This is what we demo. |
| `Rules only` | Model file missing or failed to load. Still works, still blocks. |

It also prints `Engine ready in <n> ms` — a real number to give a judge who asks
whether the model is genuinely on the phone.

Then:

- **Run all 55 test messages** → `Scams caught 40/40   Clean passed 15/15`
- **Fire a scam message** → full-screen block, read aloud
- **Fire a genuine bank message** → nothing happens, and the status says so.
  **This is the important one.** It proves a detector, not a word filter.

### Running the suite headless, from the laptop

Useful while the phone is busy mirroring or rehearsing:

```
adb shell am broadcast -a com.stackunderflow.airgap.RUN_TESTS -n com.stackunderflow.airgap/.DevTestReceiver
adb logcat -s AIRGAP_TEST
```

---

## What is in the code

| File | What it does | Owner |
|---|---|---|
| `Verdict.kt` | The shared contract: `Detector.check() -> Verdict` | Chinmay |
| `Normaliser.kt` | Pulls out amount, links, phone numbers. Keeps 1800 numbers separate from personal mobiles. | Chinmay |
| `StubDetector.kt` | Five structural rules. **40/40 scams, 15/15 clean.** Our fallback — do not delete. | Chinmay |
| `GemmaDetector.kt` | Gemma 3 1B through MediaPipe. One-word output, reasons come from a written table. | **Shan — do not edit** |
| `HybridDetector.kt` | Rules first, then a "does this ask you to act?" gate, then the model. **55/55, ~1 ms per message.** | Shan |
| `Airgap.kt` | Single entry point. Serialises detection on one worker thread. | Shan |
| `DetectionService.kt` | Foreground service so Android cannot kill the process mid-detection. | Shan |
| `SmsReceiver.kt` | Catches SMS, hands straight to the service. | both |
| `AirgapNotificationListener.kt` | Catches UPI collect requests from GPay, PhonePe, Paytm, BHIM. | Chinmay |
| `BlockActivity.kt` | Full-screen block. Reads the reason aloud. | Chinmay |
| `TestSetRunner.kt` | Runs all 55 messages, gives judges real numbers. | Chinmay |
| `DevTestReceiver.kt` | Headless suite trigger. Dev only. | Shan |
| `MainActivity.kt` | Permissions, demo buttons, engine name and load time. | both |

---

## The one rule when adding a feature

### ❌ never `Airgap.handleMessage(...)`
### ✅ always `Airgap.handleMessageAsync(...)`

```kotlin
Airgap.handleMessageAsync(context, sender, body, onResult = { verdict ->
    // runs on the MAIN thread
})
```

`handleMessage` blocks the calling thread. With the rules stub that was instant;
with Gemma it is ~400 ms, and ~1.5 s on the first call. On the UI thread or a
camera callback that is a visible freeze. From a `BroadcastReceiver` it is an ANR.

If you detect from anywhere that is **not** a foreground activity, route it
through `DetectionService.check(...)` instead, or the process gets reclaimed
mid-detection and the block screen silently never appears.

---

## Other docs, and which are still current

| File | Status |
|---|---|
| `SETUP.md` | **current** — build and run |
| `CHINMAY-READ-NOW.md` | **current** — Shan's threading rules, read before adding features |
| `CHINMAY-TODO.md` | **current** — job list and demo order |
| `SHAN-READ-NOW.md` | **current** — Chinmay's reply: what got merged |
| `Device-Facts.md` | current — chips, NPU findings |
| `testset/README.md` | current — and it flags that the 55 messages are synthetic |
| `Team-Handover.md` | **historical.** Written before the repo was a Gradle project. Its setup steps are stale; keep it for the event plan and timeline only. |
