# Device facts — closes Team-Handover.md section 1

**Both loaner phones are the SAME model.** Checked by adb on both laptops, 5 Sept.

| | Chinmay's phone | **Shan's phone** |
|---|---|---|
| Model | iQOO `I2501` | iQOO `I2501` |
| Chip | `SM8850` | **`SM8850`** ✅ |
| Android | 16 (SDK 36) | **16 (SDK 36)** ✅ |
| RAM | 15.6 GB | **15.6 GB** ✅ |

**So both phones use the same file:** `Gemma3-1B-IT_q4_ekv1280_sm8850.litertlm`.
No per-phone branching. Section 1's "check it" column is resolved.

## NPU evidence — for the technical-depth 15%

Read off the device, not guessed:

- `/vendor/lib/rfsa/adsp/libQnnHtpV81.so` → **Hexagon Tensor Processor v81**
- platform feature `com.google.android.feature.AICORE_QC_SM8850`
- `ro.soc.manufacturer=QTI`, `ro.board.platform=canoe`, abi `arm64-v8a`

**Say to judges: "Hexagon Tensor Processor v81 on SM8850."** Not "the NPU".
The `sm8850.litertlm` build is compiled to target that specific HTP.

Ask Kartikey Rawat (Qualcomm) how to *prove* it dispatched to HTP and not CPU.

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
