# Prueba 20/20 · resultados

Preliminary check with **40 synthetic messages** (20 scams, 20 normal; half Spanish, half English), written for this test.
It is not the held-out evaluation (`eval/data/`, real messages); numbers in brackets are 95 % Wilson intervals.

| Layer | Scams caught as scam | Scams with any warning | Normal flagged as scam | Normal with any warning |
|---|---|---|---|---|
| Rules on the phone | 60 % (39 %–78 %) | 100 % (84 %–100 %) | 0 % (0 %–16 %) | 5 % (1 %–24 %) |
| Muse Spark Open alone | 80 % (58 %–92 %) | 100 % (84 %–100 %) | 0 % (0 %–16 %) | 5 % (1 %–24 %) |
| Rules + model vote (±2, doubtful only) | 95 % (76 %–99 %) | 100 % (84 %–100 %) | 0 % (0 %–16 %) | 0 % (0 %–16 %) |

- Pivot criterion (model wrong in more than a third): model alone wrong in **5 of 40** → no pivot.
- Median latency of the model: 3108 ms. Cost of this run ≈ $0.1288 (Baseten credits).

## Where it still fails
- `s16` (scam, warned only as doubtful): rules DOUBT, model DOUBT, together DOUBT

## Where the model hesitates
The model answered "doubt" (no vote) on `s01` (scam, rules SCAM), `s02` (scam, rules SCAM), `s12` (scam, rules SCAM), `s16` (scam, rules DOUBT), `n12` (normal, rules OK).
