# Demo script — rehearse this three times, timed

Two people. **Shan talks. Chinmay drives the second phone.** Never both talking.

Total **3:30**. The final pitch window is 3–5 minutes, so this leaves room to be
interrupted and still finish.

---

## Before you stand up — 60 second checklist

- [ ] Airgap open once on the loaner. Status must read
      **`Engine: Rules + Gemma 3 1B (on-device)`**. If it says `Rules (stub)`, the
      model file is missing — stop and fix it.
- [ ] All three permissions **OK** on the status screen
- [ ] Chinmay's phone: WhatsApp chat with the loaner already open, both messages
      **pre-typed and ready to send** — do not type them on stage
- [ ] A UPI QR ready to show — PhonePe → *My QR*, on Chinmay's phone screen
- [ ] Loaner volume **up** (the warning is spoken)
- [ ] Loaner screen timeout set long, screen unlocked
- [ ] Battery > 40%

---

## The script

### 0:00 — The problem  *(30s, Shan, phone face down)*

> "In 2024 Indians lost over eleven thousand crore to UPI fraud. Almost all of
> it works the same way: a message arrives, and the person is talked into
> approving a payment they think is a refund.
>
> The apps that try to stop this send your messages to a server. Which means
> your bank SMS — your balance, your OTPs — leave your phone.
>
> We thought that trade was unnecessary. So Airgap does the whole thing on the
> handset. There is no backend. There is no server to send anything to."

**Then pick up the phone.**

---

### 0:30 — Scam caught  *(40s)*

**Shan:** "This is the phone you gave us this morning. Nothing else is running."

**Chinmay:** sends the scam on WhatsApp.

```
You have won Rs5000 cashback. Scan the QR to receive the amount in your account.
```

Block screen appears in about a tenth of a second, and **speaks**.

**Let the voice finish. Do not talk over it.** Then:

> "That is not a notification you can swipe away — it is a full screen, and it
> reads itself out, because the people losing this money are often first-time
> and older users."

Point at the bottom of the screen:

> "And it tells you who decided: Gemma 3 1B, on this device. No network was used."

---

### 1:10 — The clean message  *(35s)* ← **the beat that wins**

**Shan:** "Now the harder half. Anyone can flag everything."

**Chinmay:** sends, in the same chat:

```
Rs.199 debited from HDFC Bank Card xx7723 at NETFLIX. Avl limit Rs.48,801.
```

**Nothing happens. Hold the phone still and say nothing for two seconds.**

> "Nothing. That is correct, and it is half the product. Forty scams caught,
> fifteen genuine messages passed, zero false alarms. An app that cries wolf
> gets uninstalled, and then it protects nobody."

---

### 1:45 — Camera and aeroplane mode  *(50s)*

**Turn on aeroplane mode. Hold it up so they see the icon.**

> "Radios off."

Open Airgap → **Scan a QR code**. Chinmay holds up his PhonePe QR.

Block screen: *this QR only sends money*.

> "A UPI QR can only ever start a payment **from** you. There is no such thing
> as a QR that pays money **in**. So 'scan this to collect your cashback' isn't
> suspicious — it's impossible. We can tell the user that as a fact, not a guess.
>
> And that just ran with the radios off. The camera, the barcode model and the
> language model are all on the phone."

> **Why this beat is on aeroplane mode and the message beat isn't:** a message
> has to arrive over *some* radio. The QR is physically in front of the camera,
> so this part is genuinely end-to-end offline. Don't fake the other one.

---

### 2:35 — Why a model at all  *(35s)*

Expect: *"your rules already score 100% — what is the AI for?"* Answer before
they ask.

> "Fair question. Rules catch what we've already seen. So we wrote ten scams we
> deliberately built no rules for — electricity disconnection, courier
> redelivery, digital arrest, FASTag.
>
> Rules caught **zero of ten**. With the on-device model: **seven**.
>
> That is what the model is for — the scam invented next month. And it can only
> escalate a message that actually asks you to *do* something, which is why a
> real bank SMS can never be blocked by the model alone."

---

### 3:10 — Close  *(20s)*

> "Gemma 3 1B, int4, on the Snapdragon 8 Elite you handed us. Loads in about a
> second and a half, answers in under half a second, and the codebase has no
> network call in it anywhere.
>
> Your bank messages never leave your phone, because there is nowhere for them
> to go."

**Stop. Don't add anything.**

---

## If they ask about the NPU — tell the truth

> "It runs on the CPU, at about 370 ms a message. That is already below what a
> person notices, so we spent the time on the product instead of on the port."

**Never claim the NPU.** A Qualcomm judge will catch it in one question, and
then everything else you said is in doubt.

---

## If something breaks

| Breaks | Do this, without apologising |
|---|---|
| WhatsApp message doesn't arrive | "Let me trigger it directly" → **Fire a scam message** button |
| Block screen doesn't appear | Force-close Airgap, reopen, retry once. Then use the button |
| Voice doesn't speak | Carry on. Don't mention it |
| Model shows `Rules (stub)` | Say "the rules engine is running; the model loads on open" — then open the app |
| Everything fails | Show the numbers and `RUN ALL 55 TEST MESSAGES` live. That still works |

**Never say "it worked earlier".** Move to the next thing and keep going.

---

## Numbers — say them exactly like this

| Say | Not |
|---|---|
| "40 of 40 scams caught. 15 of 15 genuine messages passed." | "55 out of 55" |
| "Zero false alarms." | "very accurate" |
| "Rules caught zero of ten. With the model, seven." | "the AI helps" |
| "1.5 seconds to load, under half a second per message." | "it's fast" |

---

## Rehearsal — do this three times

1. **Run 1:** with the script open. Find where you stumble.
2. **Run 2:** script closed. Time it. Anything over 4:00, cut from section 0.
3. **Run 3:** Chinmay tries to break it — sends the clean message *first*, or an
   unexpected one. You have to keep talking.

**Then stop rehearsing and leave it alone.** Fiddling with the app after a good
run is how demos break.
