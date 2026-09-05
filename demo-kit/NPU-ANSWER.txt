# NPU investigation — what we found, and the mentor question

**Status: model WORKS. NPU dispatch does NOT. Not a blocker.**

## What works ✅

| | |
|---|---|
| Model | `gemma3-1b-int4.task` via MediaPipe `tasks-genai:0.10.29` |
| Cold load | **1579 ms** |
| Inference | **370 ms** |
| Answer | **SCAM** (correct — prompt was testset S01, fake KYC) |
| Phone | iQOO I2501 / SM8850 / Android 16 |

## What we tried, in order

**1. `.litertlm` through MediaPipe tasks-genai → FAILED**
```
FAILED_PRECONDITION: Calculator::Open() for node "odml.infra.LiteRTResourceCalculator"
failed: Unsupported model signature
llm_litert_xnnpack_executor.cc:183
```
`xnnpack` is the CPU executor. MediaPipe tasks-genai only ships that one, so it
cannot open an NPU-targeted `.litertlm` at all.

**This failure is itself evidence** that `Gemma3-1B-IT_q4_ekv1280_sm8850.litertlm`
really is chip-specific — a generic build would have loaded on CPU.

**2. `.litertlm` through LiteRT-LM with `Backend.NPU` → FAILED**

Added `com.google.ai.edge.litertlm:litertlm-android:0.11.0` (what Google's own
AI Edge Gallery uses) and asked for
`Backend.NPU(nativeLibraryDir = applicationInfo.nativeLibraryDir)`:
```
LiteRtLmJniException: Failed to create engine: INTERNAL:
llm_litert_npu_compiled_model_executor.cc:896
litert_compiled_model.h:1914 / :525
```

**Likely cause.** The APK ships only:
- `libLiteRt.so`
- `libLiteRtClGlAccelerator.so`  (GPU, not NPU)
- `liblitertlm_jni.so`

There is **no QNN / Hexagon dispatch library** in it. The device has the vendor
side (`/vendor/lib/rfsa/adsp/libQnnHtpV81.so`), but the app-side dispatch shim
appears to be missing. The Gallery declares no extra dependency for it, so it
probably fetches the accelerator at runtime.

Also note: `litertlm-android:0.11.0` is built with **Kotlin 2.3.0** while our
project is on 2.1.0 — we had to pass `-Xskip-metadata-version-check`. That
library is very new.

## 👉 Ask Kartikey Rawat (Qualcomm) at Mentor Round 1, 15:30

1. `Backend.NPU` in LiteRT-LM 0.11.0 fails with `llm_litert_npu_compiled_model_executor.cc:896`
   on SM8850. **Which QNN dispatch .so has to ship in the APK,** and where do we get it?
2. Is `Gemma3-1B-IT_q4_ekv1280_sm8850.litertlm` the right artefact for HTP v81,
   or does it need recompiling through AI Hub for this device?
3. **How do we prove** a run dispatched to Hexagon and not CPU? Is there a
   counter or log we can show a judge?

## What we say to judges either way

Do not pretend we are on the NPU. The honest version is stronger:

> "Gemma 3 1B runs entirely on the handset, 370 ms per verdict, no network.
> We also pulled the Hexagon-targeted build for this exact SoC and tried two
> runtimes to reach the NPU — here is where it stops, and here is the dispatch
> library that is missing. That is our next step."

That shows real hardware work. Claiming NPU without evidence is the thing a
Qualcomm judge would catch instantly.

## Decision

**Stop here.** 370 ms on CPU is already fast enough that a user perceives it as
instant. Integration with Chinmay's app is worth far more than the remaining NPU
work. Revisit only if the mentor hands us the answer.
