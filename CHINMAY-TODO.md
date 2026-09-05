# Chinmay — what to do next

**Written 5 Sept, ~14:25, during Red Light.** Shan is on the model side.
Read section 0 first, it will break your build if you skip it.

---

## 0. FIRST: pull before you touch anything ⚠️

```
cd <your repo>
git pull
```

**I edited two of your files.** Pull first or you get a merge conflict, and
solving one over Remote PC at 3 a.m. is horrible.

| Your file | What changed | Why |
|---|---|---|
| `airgap-src/.../Airgap.kt` | added `initDetector(context)` | picks Gemma if the model file is on the phone, else your StubDetector |
| `airgap-src/.../MainActivity.kt` | 3 lines in `onCreate` | calls `initDetector` on a **background thread** (model load is ~1.5 s, it would hang the UI on main) |

Also new since your last push:
- **The repo is now a real Gradle project.** `gradle assembleDebug` works.
  Your `airgap-src/` did NOT move - the app module points at it via `sourceSets`,
  so nothing of yours was rearranged.
- `GemmaDetector.kt` - implements your `Detector` interface. Your `StubDetector`
  stays as the fallback, I did not delete it.
- Launcher icons in `res/` (the build was failing without them).

**Your `Detector` interface was exactly the right call.** The model dropped in
behind it with zero changes to your app code.

---

## 1. Status right now

| | |
|---|---|
| Gemma on the phone | ✅ 1579 ms load, **370 ms** per verdict |
| Correct answer on testset S01 | ✅ said SCAM |
| App shows `Engine: Gemma 3 1B (on-device)` | ✅ |
| Your stub | ✅ 40/40 scam, 15/15 clean |
| 55-message run with Gemma | ⏳ running, looks slow - Shan is on it |

---

## 2. Your jobs, in priority order

### 🥇 JOB 1 — Camera → QR scam warning

**This is the single biggest scoring gap we have.** "Creative phone use" is 15%
and it is measured by the phone itself, not by what we say in the pitch. Right
now we use no camera at all, so that part of the 15% is a zero.

It is also the exact scam on slide 2 of the deck, so it is not bolted on.

What it does:
1. Point the camera at a QR code
2. Decode it
3. If it is a UPI payment intent (`upi://pay?...`), show the block screen with
   **"This QR can only SEND money, not receive it."**

Dependencies are already cached locally, so this will resolve even with bad wifi:
```kotlin
implementation("com.google.mlkit:barcode-scanning:17.3.0")
implementation("androidx.camera:camera-camera2:1.4.1")
implementation("androidx.camera:camera-lifecycle:1.4.1")
implementation("androidx.camera:camera-view:1.4.1")
```

Reuse your `BlockActivity` - do not build a second warning screen. Add a
`pattern` value of `qr_send_only` and pass it the same way.

**Do the heavy typing in Green Light (15:30-16:30, or 19:00+).** Camera preview
plus permissions is a lot of keystrokes and Remote PC makes that painful.

### 🥈 JOB 2 — Voice: read the warning aloud

Small job, closes the voice half of the same 15%.

In `BlockActivity.onCreate`, speak `verdict.reason` with Android's built-in
`TextToSpeech`. No dependency needed.

```kotlin
private var tts: TextToSpeech? = null
// onCreate
tts = TextToSpeech(this) { if (it == TextToSpeech.SUCCESS) {
    tts?.language = Locale("en", "IN")
    tts?.speak(reason, TextToSpeech.QUEUE_FLUSH, null, "airgap")
}}
// onDestroy
tts?.stop(); tts?.shutdown()
```

Genuinely better for our stated users - first-time and older users - not a gimmick.
Add a mute toggle so we can silence it if the room is loud during judging.

### 🥉 JOB 3 — Make the block screen judge-proof

You already pass `EXTRA_ENGINE` to `BlockActivity`. **Show it on screen.**
When a judge sees `Gemma 3 1B (on-device)` printed on the block screen, that is
proof, not a claim. Put it small at the bottom.

Same for confidence, and a line like `No network used`.

---

## 3. What NOT to do

- ❌ Don't touch `GemmaDetector.kt` - Shan is tuning the prompt in it right now
- ❌ Don't delete `StubDetector` - it is our fallback if the model dies in the demo
- ❌ Don't start a big refactor. We are 8 hours from a scored round.

---

## 4. Office Kit - please actually use it

**10% of the score, read off the phone, not from what we say.**

Push every APK to the phone with Office Kit file transfer instead of
`adb install`. Use the clipboard for logs. Mirror the screen when you test.
There is a "Most iQOO Usage" prize on top.

---

## 5. Timeline

| Time | | What |
|---|---|---|
| now-15:30 | 🔴 | Low typing. Plan the camera flow, read CameraX docs, prep permissions |
| 15:30-16:30 | 🟢 | **Mentor Round.** Then heavy typing - start camera |
| 16:30-19:00 | 🔴 | Finish camera logic, test |
| **19:00-22:00** | 🟢 | **EVAL ROUND 1 - SCORED.** Demo in airplane mode. Then voice + polish |

**18:00 hard gate:** whatever is broken at 18:00, we demo without it at 19:00.
Something must run. Marks are being given.

---

## 6. The demo order (memorise it)

1. Airplane mode ON, hold the phone up, say nothing
2. Scam SMS → full-screen block → **voice reads it**
3. **Genuine bank SMS → nothing happens** ← the beat that wins
4. Camera at a QR → "this can only send money"
5. "Radios were off the whole time. There is no server. There never was."

**Step 3 is what separates us from a keyword filter.** A judge will try to make
it flag everything. Get there first.

---

## 7. Still open, needs a decision

- **Slide 10 says "about 30 real scam messages from our own phones."** The
  `testset.json` in the repo is **synthetic** - I wrote it as fixtures. Either
  add real ones from your phone, or reword the slide. Don't leave a claim a
  judge could catch.
- Rotate the Hugging Face tokens after the event - both got pasted into chats.
