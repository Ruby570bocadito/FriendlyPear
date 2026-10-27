# Shield evaluation and learned weights

How well Peri's on-phone rules catch scams, measured on messages the rules were never tuned on, and whether
weights learned from data beat the hand-set ones. Everything here is reproducible from the repo.

## Data

| Set | Size | Made how | Used for |
|---|---|---|---|
| `eval/reservado-200/` | 200 (102 scams, 98 normal) | Written by hand across the 15 scam types of [`docs/ENTRENAMIENTO.md`](../docs/ENTRENAMIENTO.md), plus hard legitimate cases: real bank notices, one-time codes, genuine delivery and payment reminders, a niece with a new number. Spanish and English, SMS, WhatsApp, email and browser notifications | **Evaluation only** |
| `eval/prueba-20-20/` | 40 (20 / 20) | The earlier 20/20 test | Second evaluation set |
| `training/data/` (not in the repo) | 1,306 (719 / 587) | Generated with **Muse Spark 1.3 contributor** (`training/generate.ts`): 5 messages per call, 26 categories, ~180 k tokens. Contributor prompts may be used by Meta for training, which is fine here because nothing private is sent | **Fitting only**, never evaluated on |

Model-written messages are never used to evaluate, because a model rewards its own style.

## Method

The rules turn each message into signals (unknown sender, lookalike domain, urgency, money request, "tell us the
code", new-number claim…), and the shield adds a log-odds weight per signal. Two hard rules (a code arriving
during a call, a brand on a lookalike domain) make it a scam whatever the score.

`training/fit.py` fits a logistic regression over those signals:

- weights constrained non-negative, so each signal can only add risk and the verdict stays explainable;
- shrunk towards the hand-set weights (a prior), with the strength picked by 5-fold cross-validation on the training
  set only;
- the hard-rule signals and the "an insecure link always reaches doubt" promise are kept as floors.

## Results (held-out sets)

Levels as the app uses them: score ≥ 0 is **doubt** (Peri asks the model, or says "let's check"); ≥ 2 is **scam**
(Peri warns firmly). Intervals are 95 % Wilson.

**`reservado-200`**

| Weights | AUC | Firm warning on scams | Firm false alarms | Any warning on scams | Any false alarm |
|---|---|---|---|---|---|
| Hand-set (`PROVISIONAL`, in use) | 0.957 | 52.9 % [43–62] | 1.0 % [0.2–5.6] | 83.3 % [75–89] | 4.1 % [1.6–10] |
| Learned from scratch | 0.942 | 41.2 % | 0.0 % | 77.5 % | 3.1 % |
| **Learned with hand prior (`LEARNED`)** | **0.965** | **66.7 % [57–75]** | 1.0 % [0.2–5.6] | 78.4 % [70–85] | **2.0 %** [0.6–7.1] |

**`prueba-20-20`**

| Weights | AUC | Firm warning on scams | Firm false alarms | Any warning on scams |
|---|---|---|---|---|
| Hand-set | 0.998 | 75 % | 0 of 20 | 100 % |
| Learned with prior | 0.986 | 80 % | **1 of 20** | 100 % |

## Decision

**The shield keeps the hand-set weights.** The learned ones rank scams better and warn firmly on 14 more scams in
100, with fewer doubtful false alarms, but they make one genuine message a firm scam: a gym's "your membership
expires tomorrow, pay at reception" (urgency + money from an unsaved number). For an older person a confident
false "it's a scam" costs more trust than a doubtful message that goes on to the model.

Error analysis traced it to the training data: the synthetic legitimate messages rarely combine a deadline with a
payment. Adding a category of genuine payment reminders (club fees, school trips, rent, invoices) lowered the
doubtful false alarms from 4.1 % to 2.0 %, but this one case still sits exactly on the threshold, and
cross-validation chose the edge of its grid. The next step is **real legitimate messages** from families, with
permission and anonymized, as `docs/ENTRENAMIENTO.md` plans. `ShieldWeights.LEARNED` ships in `core` and a test
checks that it keeps the app's promises, so switching is a one-line change once the data is there.

## Errors found on the way

- Lloyds' real domain `lloydsbank.co.uk` was missing from the brand list, so a genuine Lloyds email tripped the
  lookalike hard rule. Fixed.
- The rules miss romance scams with no link, and some fake-subscription emails. Those go to the model only if
  another signal fires, which is a gap for the next round of rules.

## Reproduce

```
cd backend; bun ../training/generate.ts --run           # training data (Spark contributor, cents)
cd core; ./gradlew evalShield --args="../training/data/train.tsv ../training/data/senales.tsv"
cd core; ./gradlew evalShield --args="../eval/reservado-200/mensajes.tsv ../eval/reservado-200/resultados/reglas.tsv"
python training/fit.py                                  # writes training/out/pesos.json
```
