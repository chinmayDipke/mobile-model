# Test set

`testset.json` — 55 labelled messages for the detector.

| | count | |
|---|---|---|
| scam | 40 | 8 each across the 5 patterns from the brief |
| genuine | 15 | realistic SBI / HDFC / ICICI / Axis formats |

Patterns: `kyc_link`, `qr_cashback`, `collect_refund`, `fake_care`, `wrong_transfer`.

## The 15 genuine ones are the important half

Several deliberately contain the exact words a naive keyword filter trips on —
"refund", "credited", "OTP", "blocked", "debited", "subsidy". If Airgap passes all
15 clean, the demo beat in Team-Handover.md section 9 step 3 lands, and we survive
a judge trying to make it flag everything.

## ⚠️ These are SYNTHETIC — read Team-Handover.md section 6

These were written as test fixtures, they are **not** harvested from our phones.
Phone numbers are masked (`9835xxxx21`) and non-dialable.

Per Team-Handover.md section 6, **slide 10 of the deck currently claims "about 30
real scam messages from our own phones."** That claim is not true of this file.
Either:

- add real messages from our own phones and say "some real, some written for
  testing", or
- change slide 10 to describe the set accurately.

Do not claim something a judge could catch. This is the honest-disclosure item
already flagged in section 12.
