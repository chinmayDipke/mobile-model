# Model spike — RESULT: WORKS ✅

Run on **Shan's phone**, iQOO I2501 / SM8850 / Android 16, 5 Sept 13:09.

## The two numbers (Team-Handover.md section 5, point 5)

Model: `gemma3-1b-int4.task` (554,661,243 bytes) — the generic/safe build.

| | |
|---|---|
| **Cold load** | **1579 ms** (1.6 s) |
| **Inference** | **370 ms** (0.4 s) |

## It gave the right answer

Prompt was a real scam SMS from `testset/testset.json` (S01, fake KYC + link).

```
MODEL SAID: SCAM
```

So this is not just "the model speaks" — it is Airgap's actual job, working,
on-device, in under half a second.

## NPU build — TODO

`gemma3-1b-sm8850.litertlm` not yet timed. Do this and fill in:

| | generic `.task` | NPU `.litertlm` |
|---|---|---|
| Cold load | 1579 ms | ? |
| Inference | 370 ms | ? |

**Both numbers side by side is the technical-depth evidence.** The `.task` build
is portable and may run on CPU/GPU; the `.litertlm` is compiled for Hexagon
Tensor Processor v81 on SM8850 specifically. Ask Kartikey Rawat (Qualcomm) at
Mentor Round 1 how to *prove* the dispatch went to HTP and not CPU.

## How to run it

```
gradle assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Models must be at `/data/local/tmp/llm/` (both phones have them there):
- `gemma3-1b-int4.task`
- `gemma3-1b-sm8850.litertlm`

## Notes for Chinmay

- MediaPipe version is **0.10.29** (handover says 0.10.27 — 0.10.29 is newer and verified).
- Manifest theme must be a plain android theme, NOT `Theme.Material3.*` —
  that resource does not exist in a Compose-only project. Cost us one build.
- This is a SPIKE, not the real app. It exists to prove the model runs and to
  capture the timings.
