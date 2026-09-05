# Chinmay — read this before you write more QR code

**14:55, Red Light.** `git pull` first. Three bugs found and fixed since your
last pull. **None of them were your fault** — your code was correct with the
stub. All three appeared the moment Gemma replaced it. But two of them affect
code you are writing right now.

---

## The one rule that affects your QR feature

### ❌ NEVER call `Airgap.handleMessage(...)`
### ✅ ALWAYS call `Airgap.handleMessageAsync(...)`

```kotlin
Airgap.handleMessageAsync(
    context, sender, body,
    onResult = { verdict -> /* runs on the MAIN thread */ }
)
```

`handleMessage` runs the model on whatever thread you call it from. With the
stub that was instant. With Gemma it is ~400 ms and the first call is ~1.5 s —
on the camera thread or the UI thread that is a freeze.

**For the QR flow specifically:** ML Kit's barcode callback is on the main
thread. Do not run detection there.

---

## The three bugs, so you know what changed

### 1. Threading — would have broken the live demo
`SmsReceiver.onReceive` runs on the main thread and Android kills a receiver
that blocks ~10 s. Gemma blocks. **A real incoming SMS would have ANR'd the app
in front of the judges.**

Fixed: `Airgap.handleMessageAsync()` on a single worker thread. Single on
purpose — `LlmInference` is **not thread safe**, so two messages arriving
together could have corrupted each other.

### 2. Speed — 12 seconds per message, measured
Generation time scales with **output length**, not model size. Asking the model
to write a reason sentence cost ~12 s each.

Fixed: model now emits **one word** (`maxTokens = 8`). The warning text comes
from a written table in `GemmaDetector.REASONS`.

**This is better, not a shortcut.** Those strings get **read aloud** by your TTS
to first-time and older users. A 1B model writes clumsy English. Ours is plain
and calm, and there is nothing to parse on stage.

**If you add a QR pattern, add its reason string to that map.** Suggested:

```kotlin
"qr_send_only" to "Scanning this code can only SEND your money, never receive it."
```

### 3. Process death — silent failure, the worst kind
The app process was being **killed mid-detection**. A receiver's process has no
foreground component once `onReceive` returns, and ours holds a 529 MB model, so
it is first in line to be reclaimed. Measured: the 55-message run was killed
twice, with no error anywhere.

In a demo this looks like **the app simply doing nothing**.

Fixed: `DetectionService`, a foreground service. `SmsReceiver` now hands off to
it instead of doing the work itself.

**If your QR scan happens while the app is in the foreground you are fine** —
the activity keeps the process alive. But if you ever detect from a receiver or
a background callback, route it through `DetectionService.check(...)`.

---

## Your jobs, unchanged

1. **Camera → QR warning** ← biggest scoring gap, 15% and measured by the phone
2. **Voice** — TTS reads `verdict.reason` on the block screen
3. **Show `EXTRA_ENGINE` on the block screen** — `Gemma 3 1B (on-device)` printed
   on screen is proof to a judge, not a claim

## Do not touch

- `GemmaDetector.kt` — Shan is tuning the prompt
- `StubDetector` — our fallback if the model dies on stage
- `Airgap.kt` — just call `handleMessageAsync`

---

## Still unknown

The 55-message accuracy run has **not completed yet** — it kept getting killed.
Re-running now on the foreground service. Numbers to follow.

Your stub's own numbers (40/40, 15/15) still stand and are still our safety net.
