# What proof to show — and in what order

For Eval Round 1 (19:00), Round 2 (09:00) and the final pitch. Same every time.

**The principle: show, don't claim.** Every line below is something a judge can
watch happen or read off the screen. Anything you can only assert, cut.

---

## 1. Airplane mode — your strongest proof, and it costs nothing

Turn it on. Hold the phone up. **Say nothing for two seconds.** Let them see the
aeroplane icon.

Then run the whole demo with it on.

This is unfakeable. Every other team's AI app dies the moment you do this. Yours
does not care. **Do not explain it first — do it, then explain.**

At the end: *"The radios have been off since before I started."*

---

## 2. On the screen, not from your mouth

Chinmay put these on screen so a judge reads them instead of trusting you:

| On screen | What it proves |
|---|---|
| `Engine: Rules + Gemma 3 1B (on-device)` | which engine actually ran |
| `Engine ready in 1579 ms` | the model really loaded, here, now |
| engine name on the **block screen** | the warning came from that engine |

A number beats an adjective. "It's fast" is a claim; `1579 ms` is evidence.

---

## 3. The demo order — step 3 is the one that wins

1. **Airplane mode on.** Visible. Silent.
2. **Scam SMS arrives** → full-screen block → **voice reads the warning aloud**
3. **A genuine bank SMS arrives → NOTHING HAPPENS** ← *the beat nobody else shows*
4. **Camera at a QR** → "this code can only send money, never receive it"
5. *"Radios off the whole time. There is no server. There never was."*

**Why step 3 wins:** every team can flag a scam — flag everything and you score
100% on scams. Almost nobody demonstrates a clean message passing silently. A
judge **will** try to make your app cry wolf. Get there first, on purpose.

Say it out loud as it happens: *"Nothing happened. That is the correct
behaviour, and it is half the product."*

---

## 4. The numbers — say them as two, never as one

**Say:** "40 of 40 scams caught. 15 of 15 genuine messages passed."
**Don't say:** "55 out of 55."

Same fact. But `55/55` sounds like one number about scams, and it hides the half
that impresses. Two numbers force the judge to notice the second one.

---

## 5. The proof that the AI earns its place

This is the question a sharp judge asks: **"Rules already score 100%. Why is
there an AI at all?"**

Have the answer ready as a measurement, not an opinion:

| Suite | Rules only | Rules + Gemma |
|---|---|---|
| 55 known messages | 40/40 · 15/15 | 40/40 · 15/15 |
| **10 scams the rules never saw** | **0 / 10** | **6 / 10** |

**Measured on the handset, 5 Sept.** The rules score zero on these - they were
never written for them. The model catches six. That difference is the entire
argument for putting an LLM on the phone.

The second row is the whole argument. Those 10 are real scam shapes we
deliberately did not write rules for — electricity disconnection, courier
redelivery fee, SIM-upgrade APK, digital arrest, loan processing fee, FASTag
blacklist.

Say: *"Our rules catch what we have seen. They caught zero of these ten. The
model is what catches the scam that gets invented next month — and it can only
escalate a message that actually asks you to act, which is why a real bank SMS
can never be blocked by the model alone."*

---

## 6. If they ask how detection works

Two layers, thirty seconds:

> "A normaliser pulls out the structure — amount, links, personal 10-digit
> number versus a 1800 number. Then Gemma 3 1B, running on this phone, judges
> the meaning: is this message **telling** me something, or **asking** me to do
> something? Scams always ask. Real bank SMS only inform."

Then the honest part, which lands better than polish:

> "The rules are precise on shapes we know. The 1B model on its own was biased
> towards flagging — it blocked OTP alerts. So the model isn't allowed to block
> alone. That's a deliberate design, and it's why our false-alarm count is zero."

---

## 7. Technical depth — what to actually claim

**Do claim:**
- Gemma 3 1B, int4, running fully on the handset — `/data/local/tmp/llm/`
- 1579 ms cold load, ~370 ms single inference, measured on this SM8850
- Zero network calls. No backend exists in the codebase at all.
- We pulled the Hexagon-targeted `.litertlm` build for this exact SoC and tried
  two runtimes to reach the NPU

**Do NOT claim:** that it runs on the NPU. It does not — yet. See
`spike-model-load/NPU-FINDINGS.md` for exactly where it stops.

Saying *"here is where we got blocked and here is the dispatch library that's
missing"* reads as real hardware work. Claiming NPU without evidence is the one
thing a Qualcomm judge would catch instantly, and it would cost you more than
the NPU was ever worth.

---

## 8. Proof the code was written during the event

The Build rules say original work only, written inside the event window.

`git log` is your evidence — every commit is timestamped inside Saturday. If
anyone asks, show it. Do not hide that the model weights and SDK were downloaded
beforehand; that is explicitly allowed, and `Airgap-Prep/` contains no Airgap
source.

---

## 9. Two things to fix before you stand up

- **Slide 10 says the test set is real messages from our phones. It is
  synthetic.** Reword it. A judge who asks "which of these are real?" and gets a
  wrong answer costs you more than the slide was worth.
- **Rehearse steps 1-5 three times, timed.** The final pitch is 3-5 minutes and
  the demo is most of it.
