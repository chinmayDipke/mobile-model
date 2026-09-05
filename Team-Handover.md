# Airgap — Team Handover

**Team:** StackUnderFlow · Shan Solanki (leader) · Chinmay Dipke
**Event:** iQOO Hackathon 2026, Pune. Sat 5 to Sun 6 September 2026.
**Track:** FinTech and Commerce. This is locked. Do not change it.
**Updated:** Saturday 5 September, during the first Green Light.

This file is the one place with everything. It replaces `PLAN.md` and section 6
of `Need-list.md`. Both people should read all of it once.

---

## 1. The two phones are NOT the same

We have two loaner phones. They are different models. **This matters, because the
fast model file is built for one exact chip.**

| | Chinmay's phone | Shan's phone |
|---|---|---|
| Model | iQOO `I2501` | **check it** |
| Chip | `SM8850` (Snapdragon 8 Elite Gen 5) | **check it** |
| Android | 16 (SDK 36) | **check it** |
| RAM | 15.6 GB | |

### How Shan checks his chip

```powershell
$adb = "$env:USERPROFILE\iqoo-models\tools\platform-tools\adb.exe"
& $adb shell getprop ro.soc.model
& $adb shell getprop ro.product.model
```

### Then pick your file from this table

| If the chip says | Use this file |
|---|---|
| `SM8850` | `Gemma3-1B-IT_q4_ekv1280_sm8850.litertlm` |
| `SM8750` | `Gemma3-1B-IT_q4_ekv1280_sm8750.litertlm` |
| `SM8650` | `Gemma3-1B-IT_q4_ekv1280_sm8650.litertlm` |
| **anything else** | `gemma3-1b-it-int4.task` |

**The `.task` file works on every chip.** It is the safe one. The `.litertlm`
files are faster but only on their own chip. If Shan's chip is not in the list,
that is fine, just use the `.task` file.

**Start with the `.task` file anyway, on both phones.** Get it working first.
Try the faster chip file later.

---

## 2. What is already done

### Chinmay's laptop and phone: DONE

- [x] Android Studio installed (Quail 4, version 2026.1.4)
- [x] `adb` installed on its own (Android Studio did not include it)
- [x] Phone connected, USB debugging on, shows in `adb devices`
- [x] All model files downloaded and size-checked
- [x] Two models copied onto the phone:

```
/data/local/tmp/llm/gemma3-1b-sm8850.litertlm    693,747,712 bytes
/data/local/tmp/llm/gemma3-1b-int4.task          554,661,243 bytes
```

Files on Chinmay's laptop, in `C:\Users\chinm\iqoo-models\`:

| File | Bytes | For |
|---|---|---|
| `gemma3-1b-it-int4.task` | 554,661,243 | any chip, start here |
| `Gemma3-1B-IT_q4_ekv1280_sm8850.litertlm` | 693,747,712 | SM8850 |
| `Gemma3-1B-IT_q4_ekv1280_sm8750.litertlm` | 689,291,264 | SM8750 |
| `Gemma3-1B-IT_q4_ekv1280_sm8650.litertlm` | 690,094,080 | SM8650 |
| `Qwen2.5-1.5B-Instruct_seq128_q8_ekv1280.task` | 1,567,364,648 | backup, slower |

### Shan's laptop: downloading now

Shan has started his downloads. Good.

**If the download is slow, stop it and copy from Chinmay's USB drive instead.**
Everything above is already on that drive. Venue wifi is shared. A USB copy of
5.3 GB takes a few minutes and always works.

---

## 3. Setup steps for Shan

Use **PowerShell**, not Git Bash. Reason is in section 8.

### Step 1. Android Studio

```
https://edgedl.me.gvt1.com/android/studio/install/2026.1.4.7/android-studio-quail4-windows.exe
```

1.5 GB. Install on **C:**. Pick **Standard**. **Untick the emulator.** We have real
phones, so the emulator is wasted time and disk.

### Step 2. adb (10 MB, quick)

```powershell
New-Item -ItemType Directory -Force "$env:USERPROFILE\iqoo-models\tools" | Out-Null
Invoke-WebRequest "https://dl.google.com/android/repository/platform-tools-latest-windows.zip" `
  -OutFile "$env:USERPROFILE\iqoo-models\tools\platform-tools.zip"
Expand-Archive "$env:USERPROFILE\iqoo-models\tools\platform-tools.zip" `
  -DestinationPath "$env:USERPROFILE\iqoo-models\tools" -Force
& "$env:USERPROFILE\iqoo-models\tools\platform-tools\adb.exe" version
```

Expect `Android Debug Bridge version 1.0.41`.

### Step 3. Phone setup

1. Settings, About phone, tap **Build number** 7 times.
2. Developer options, turn on **USB debugging**.
3. Plug in the phone, tap **Allow** on the popup.
4. Run the chip check from section 1.

### Step 4. Models

**Best:** copy the `iqoo-models` folder from Chinmay's USB drive.

**If downloading yourself,** the Gemma model is licence locked. Two separate things
are needed, not one:

1. Make a Hugging Face account.
2. Open `https://huggingface.co/litert-community/Gemma3-1B-IT` and **click to accept
   the Gemma licence.** A token alone does not work.
3. Make a **read** token at `https://huggingface.co/settings/tokens`.
4. Log in. Do not paste the token into any chat.

```powershell
pip install huggingface_hub
huggingface-cli login
```

5. Download. Change the `.litertlm` name if your chip is different.

```powershell
huggingface-cli download litert-community/Gemma3-1B-IT `
  gemma3-1b-it-int4.task `
  Gemma3-1B-IT_q4_ekv1280_sm8850.litertlm `
  --local-dir "$env:USERPROFILE\iqoo-models"
```

### Step 5. Put the models on the phone

```powershell
$adb = "$env:USERPROFILE\iqoo-models\tools\platform-tools\adb.exe"
$m   = "$env:USERPROFILE\iqoo-models"
& $adb shell mkdir -p /data/local/tmp/llm
& $adb push "$m\gemma3-1b-it-int4.task" /data/local/tmp/llm/gemma3-1b-int4.task
& $adb shell ls -l /data/local/tmp/llm/
```

About 15 seconds. Check the size matches.

### Step 6. Office Kit

Install on the laptop from `pc.vivoglobal.com`. The phone side is already there.

**Test all four before 14:00:** screen mirror, clipboard, file transfer, remote
control. Do not find out at 14:01 that it does not work.

---

## 4. Do this in the next 10 minutes

Both of you agree on **one thing**, write it in a file, then you can never block
each other:

```kotlin
data class Verdict(
    val isScam: Boolean,
    val pattern: String,     // "FAKE_KYC" | "CASHBACK_QR" | "FAKE_REFUND" | "CLEAN"
    val confidence: Float,
    val reason: String       // shown on screen and read out loud
)

fun check(messageText: String): Verdict
```

- Chinmay writes a fake version that always returns the same answer.
- Shan later swaps in the real model behind it.
- Neither of you waits for the other. This is the whole point.

---

## 5. Who does what

### Shan — the model. This is the risky half.

Your job: make the model return a `Verdict`.

1. New Android project. Kotlin. Empty Activity.
2. In `app/build.gradle.kts`:

```
implementation("com.google.mediapipe:tasks-genai:0.10.27")
```

3. Load `/data/local/tmp/llm/gemma3-1b-int4.task` first. It is the safe one.
4. Send one prompt. Print the answer. That is the first goal, nothing more.
5. **Write down the cold load time and one inference time.** We need these numbers
   for the judges. Technical depth is 15% of the score.
6. Only after that works, try the chip-specific `.litertlm` file.

Docs: `https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android`

**By 15:00 you must have:** a real answer out of the model, on the phone.

### Chinmay — the app. This must exist no matter what.

Your job: catch the message, block the screen.

1. Hello world app installed on the phone by `adb`. Prove the loop works.
2. SMS receiver plus `NotificationListenerService`.
3. Normaliser. Pull out amount, sender, links. Keep the text short so the model is fast.
4. Full screen block. Needs `SYSTEM_ALERT_WINDOW` and a foreground service.
   The permission popups are fiddly. Practise them.

**By 15:00 you must have:** a fake verdict showing a real full screen block.

### Do not both debug the model

If Shan is stuck at 15:00, that is a question for the Qualcomm mentor, not a
second person. Chinmay keeps building the app.

---

## 6. The test set

We need messages to test with. Two kinds, and we need both.

- **Scam messages.** About 30. Five kinds: fake KYC, cashback QR, refund that is
  really a debit, fake customer care, wrong number transfer.
- **Normal bank messages.** About 10. **This is not optional.** If our app flags
  everything, it is worthless, and a judge will test exactly that.

**Get real ones from your own phones first.** You both have your personal phones
with you. Search your SMS for these.

If you need more, use the OpenRouter credits to write extra ones. That is test
data, not product code, so it is allowed.

⚠️ **If we use made-up messages, slide 10 of the deck must change.** Right now it
says "about 30 real scam messages from our own phones." Only say that if it is
true. Otherwise change it to say some are real and some are written for testing.
Do not claim something a judge could catch.

---

## 7. The time plan

Green = laptop allowed. Red = phone only, through Office Kit.

**Green total 11.5 hours. Red total 9.5 hours.**

Red Light does not stop you. Office Kit Remote PC lets the phone drive the laptop.
It just makes you slow. So plan by how much typing a job needs.

- 🟢 **Green = heavy typing.** New features, Gradle builds, model pushes.
- 🔴 **Red = light typing.** Running tests, tuning text, QA on the phone, writing
  and practising the pitch.

**Never start a new feature 15 minutes before Red Light. Finish it or park it.**

### Saturday 5 September

| Time | Light | What we do |
|---|---|---|
| 11:00–14:00 | 🟢 | Model loading. App skeleton. **The most valuable hours you have.** |
| 14:00–15:30 | 🔴 | Model work over Remote PC. Phone QA. Log the timing numbers. |
| 15:30–16:30 | 🟢 | Build and fix. **Mentor Round 1. Find the Qualcomm mentor.** |
| 16:30–19:00 | 🔴 | Join the two halves. Real verdict replaces the fake one. |
| **19:00–22:00** | 🟢 | **Evaluation Round 1. Scored.** Then build the camera QR feature. |
| 22:00–01:00 | 🔴 | Voice warning. Run the test set. Tune. |

**18:00 hard gate.** If the model is not joined up by 18:00, show the fake verdict
version at 19:00. Something must run. Marks are being given.

### Sunday 6 September

| Time | Light | What we do |
|---|---|---|
| 01:00–06:30 | 🟢 | **Biggest block of the event.** Everything unfinished lands here. |
| | | **Stop adding features at 05:00.** Then test in airplane mode. |
| 06:30–09:00 | 🔴 | Code freeze. Testing and rehearsal only. |
| **09:00** | | **Evaluation Round 2. Scored.** Demo at the table. |
| 09:00–12:00 | | Polish. **Upload the repo to the Reskilll site before 12:00.** |
| 12:00 | | Building stops. Repos lock. |
| 13:30 | | Top 10 announced. Pitches 13:45, 3 to 5 minutes. |
| 16:15 | | Awards. |

**If the model will not load, use a smaller one. Never move a deadline to save a model.**

### Mentor Round 1, what to ask

Find **Kartikey Rawat, Qualcomm**. Go even if you are not stuck. Ask him:

1. Which path actually runs on the NPU on this chip, not the CPU?
2. How do we **prove** it ran on the NPU? Evidence is worth marks.

---

## 8. How we win

| Part | Weight | Judged by | Where we stand |
|---|---|---|---|
| End product | 30% | People | Good |
| Novelty | 20% | People | **Our best asset** |
| Creative phone use | 15% | **The phone** | **Weak. Fix it.** |
| Technical depth | 15% | People | Good, have the timing numbers ready |
| Office Kit use | 10% | **The phone** | **Weak. Fix it.** |
| Demo | 10% | People | Good |

**25% is measured by the phone itself.** We cannot talk our way into those marks.
They only count if we actually do the thing.

### The two features that fix the 15%

1. **Camera.** Point it at a QR code. App says "this QR can only send money, not
   receive it." This is the exact scam on slide 2. It fits perfectly.
2. **Voice.** Read the warning out loud. Our users are older and first time users.
   Speaking it is genuinely better for them.

### The 10% for Office Kit

Use it on purpose in **every** window, including Green. Push each new APK with
file transfer. Copy logs with the clipboard. Mirror the screen to rehearse.
There is also a **Most iQOO Usage** prize.

### Our biggest advantage

The organisers' own tip is: build apps that run on the phone, including the
backend. **Airgap has no backend at all.** Most teams will build
`app → cloud AI → app`. We build `app → the chip`. Say this every single time.

---

## 9. The demo. Same every time, all three judged moments.

1. Turn on **airplane mode**. Hold the phone up. Let them see it. Say nothing yet.
2. Scam SMS arrives. Full screen block appears. **Voice reads the warning.**
3. **A normal bank SMS arrives. Nothing happens.**
4. Point the camera at a QR code. "This one can only send money."
5. "Radios have been off the whole time. There is no server. There never was."

**Step 3 is the one that wins.** It proves we built a real detector and not a word
filter. A judge will try to make it flag everything. Get there first.

---

## 10. Things that already broke. Do not repeat them.

**Git Bash breaks adb.** It turned `/data/local/tmp` into
`C:/Program Files/Git/data/local/tmp` and the push failed. Use **PowerShell**.
If you must use Git Bash, put `MSYS_NO_PATHCONV=1` in front of the command.

**Android Studio did not install adb.** Get platform-tools separately.

**Hugging Face needs two things.** The licence click AND a token.
No token gives error 401. Token without the licence gives 403.

---

## 11. Rules we must not break

- **Only code written during the event counts.** Model files and tools downloaded
  earlier are fine, they are not our code. Credit open source in the README.
- **Do not touch HackTracker.** Tampering and crashes are logged and punished.
- **Upload the repo before 12:00 Sunday.** Late means a penalty or removal.
- **No cloud AI inside the app.** Not one call. It would destroy our whole story
  and cost us the on-device marks. OpenRouter is for writing test data and helping
  us code, nothing else.
- **One track only.** FinTech. Do not let the pitch drift into health or education.

---

## 12. Still open

- [ ] Shan: check your phone's chip and pick the right model file (section 1)
- [ ] Are we both in the same group, students or working professionals?
      A mixed team is not allowed.
- [ ] Chinmay: delete the old Hugging Face token, it was pasted into a chat
- [ ] Fix slide 10 if the test messages are not all real (section 6)
- [ ] Cut the pitch to 4 minutes, practise out loud 3 times

---

## 13. Our files

In `D:\PROJECTS\IQoo Hackthon\`:

| File | What it is |
|---|---|
| `AIRGAP-FULL-BRIEF.md` | The full idea. Problem, numbers, architecture, scam types. |
| `Airgap-Deck.pptx` | 10 slide deck, iQOO colours, no blanks. |
| `Form-Answers.md` | Application form answers. |
| `Need-list.md` | Prep list. Section 6 is replaced by section 7 here. |
| `Team-Handover.md` | This file. The one that matters now. |
