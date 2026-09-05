# Device facts — closes Team-Handover.md section 1

**Both loaner phones are the SAME model.** Checked by adb on both laptops, 5 Sept.

| | Chinmay's phone | **Shan's phone** |
|---|---|---|
| Model | iQOO `I2501` | iQOO `I2501` |
| Chip | `SM8850` | **`SM8850`** ✅ |
| Android | 16 (SDK 36) | **16 (SDK 36)** ✅ |
| RAM | 15.6 GB | **15.6 GB** ✅ |

**So both phones run the same build.** No per-phone branching.
Section 1's "check it" column is resolved.

We ship **`gemma3-1b-int4.task` on the CPU**. The chip-specific `.litertlm`
route was tried and dropped on 5 Sept - see the note at the end of this file.

## Storage — not a constraint

`/data` has **443 GB free**. Do not shrink the model for space.
Shrink only if it fails to load.

## Toolchain verified on Shan's laptop

| | |
|---|---|
| SDK | `C:\Users\LOQ\AppData\Local\Android\Sdk` |
| NDK | 30.0.16138531 |
| build-tools | 36.0.0 · platform android-36 · cmake 4.1.2 |
| JDK | 21.0.6 (Android Studio JBR) |
| Gradle | 8.13, dependency cache warmed (~834 MB) |
| adb | 37.0.1, phone authorised |

**MediaPipe: use `0.10.29`, not `0.10.27`.** Handover section 5 says 0.10.27;
0.10.29 is newer and is already resolved and cached locally.

## The NPU route: tried, dropped

We spent part of 5 Sept trying to reach the Hexagon NPU with a
chip-specific `.litertlm` build, through both MediaPipe and LiteRT-LM.
Neither reached it, and we removed that code rather than carry a half-
finished port into judging.

**370 ms per verdict on the CPU is already below what a person notices**,
so the port bought us nothing a judge could see. Do not restart it.

If asked: say it runs on the CPU. Never claim the NPU.
