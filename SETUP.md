# Airgap app — how to build it

Written by Chinmay's side. Shan, you do not need this to work on the model.

The Kotlin lives in `airgap-src/`. It is staged there on purpose: Gradle files
change with every Android Studio version, so we let Android Studio make those
and we only supply our own code.

---

## 1. Make the project (2 minutes, once)

In Android Studio:

1. **New Project** → **Empty Views Activity**
   ⚠️ **Views**, not Compose. Our screens are XML layouts.
2. Name: `AirgapApp`
3. Package name: `com.stackunderflow.airgap`  ← must match exactly
4. Language: **Kotlin**
5. Minimum SDK: **API 29**
6. Save location: `D:\PROJECTS\IQoo Hackthon\mobile-model\AirgapApp`
7. Finish, and wait for the first Gradle sync to end.

## 2. Drop our code in

Run this in **PowerShell**:

```powershell
$repo = "D:\PROJECTS\IQoo Hackthon\mobile-model"
$app  = "$repo\AirgapApp\app\src\main"
$src  = "$repo\airgap-src"

Copy-Item "$src\java\*"             "$app\java\"  -Recurse -Force
Copy-Item "$src\res\*"              "$app\res\"   -Recurse -Force
Copy-Item "$src\assets"             "$app\"       -Recurse -Force
Copy-Item "$src\AndroidManifest.xml" "$app\AndroidManifest.xml" -Force

Remove-Item "$app\java\com\stackunderflow\airgap\MainActivity.kt.orig" -EA SilentlyContinue
"copied. now Sync Gradle in Android Studio."
```

If Android Studio made its own `MainActivity.kt`, ours overwrites it. That is fine.

## 3. One dependency

Open `app/build.gradle.kts`, and inside `dependencies { }` make sure this is there.
The Views template normally adds it already:

```kotlin
implementation("androidx.appcompat:appcompat:1.7.0")
```

Then **Sync Now**.

## 4. Build and install — through Office Kit, not adb

**This part matters for our score.** Office Kit usage is 10% of the marks and it
is read off the phone, not from what we claim.

1. In Android Studio: **Build → Build APK(s)**
2. The file appears at `AirgapApp\app\build\outputs\apk\debug\app-debug.apk`
3. **Drag that APK into Office Kit file transfer** to send it to the phone
4. On the phone, tap the file to install it
5. Use **Screen Mirror** whenever testing the block screen
6. Use **Super Clipboard** to copy logcat text back to the laptop

Do not use `adb install`. It works, but it earns us nothing.

## 5. Turn on the three permissions

Open the app, and tap the three buttons in order:

1. **Grant SMS access** — normal popup
2. **Allow draw over other apps** — sends you to Settings, turn Airgap on
3. **Allow notification access** — sends you to Settings, turn Airgap on

The status box at the top shows `OK` or `MISSING` for each one.

## 6. Check it works

- **Run all 55 test messages** → should print
  `Scams caught 40/40   Clean passed 15/15`
- **Fire a scam message** → the black and gold block screen appears and reads the
  warning out loud
- **Fire a genuine bank message** → nothing happens, and the status says so.
  **This is the important one.** It proves we built a detector, not a word filter.

---

## What is in the code

| File | What it does |
|---|---|
| `Verdict.kt` | The shared contract. Shan plugs the model in behind `Detector`. |
| `Normaliser.kt` | Pulls out amount, links, and phone numbers. Keeps 1800 numbers separate from personal mobiles. |
| `StubDetector.kt` | Rules that work today. **Verified 40/40 scams, 15/15 clean.** |
| `Airgap.kt` | Ties it together. **Swap one line here for Shan's model.** |
| `SmsReceiver.kt` | Catches SMS as it arrives. |
| `AirgapNotificationListener.kt` | Catches UPI collect requests from GPay, PhonePe, Paytm, BHIM. |
| `BlockActivity.kt` | Full screen block. Reads the reason aloud with text to speech. |
| `TestSetRunner.kt` | Runs all 55 messages and gives real numbers for the judges. |
| `MainActivity.kt` | Permissions, demo buttons, test runner. |

## For Shan, when the model is ready

Write a class that implements `Detector`, then change one line in `Airgap.kt`:

```kotlin
var detector: Detector = GemmaDetector(context)   // was StubDetector()
```

Nothing else in the app changes. The block screen, the listeners and the test
runner all keep working. Run the 55 tests against your model and compare the
numbers with the stub.

## Notes

- The block screen already does the **voice** half of the creative-phone-use 15%.
- The **camera QR** half is not built yet. That is the next feature, and it is the
  single highest value thing left.
- The stub is a fallback, not the product. If the model is late, we still have a
  working demo. That is the whole point of the split.
