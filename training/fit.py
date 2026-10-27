"""
Learned shield weights (pesos-aprendidos): L2-regularized logistic regression over the signals the
rules fire, fitted on the synthetic training set and reported on the hand-written held-out set.

    cd core; ./gradlew evalShield --args="../training/data/train.tsv ../training/data/senales.tsv"
    python training/fit.py

Reads  training/data/train.tsv + senales.tsv   (fit; synthetic, never evaluated on)
       eval/reservado-200/mensajes.tsv + resultados/reglas.tsv   (held-out comparison only)
Writes training/out/pesos.json       the weights, every run's metrics and the chosen thresholds
       training/out/ShieldWeights.kt the LEARNED block to paste into core

Weights are constrained non-negative (each signal is a risk sign, so the score stays explainable:
"this adds risk"), with the bias free. Hard rules are untouched: they bypass the weights.
"""
from __future__ import annotations

import csv
import json
import math
import os
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent

SIGNALS = [
    "UNKNOWN_SENDER", "LINK", "URL_SHORTENER", "INSECURE_LINK", "BRAND_LOOKALIKE", "URGENCY",
    "MONEY_REQUEST", "CREDENTIAL_REQUEST", "CODE_FORWARD_REQUEST", "NEW_NUMBER_CLAIM",
    "PRIZE_OR_PROFIT", "REMOTE_ACCESS_APP", "AI_INSTRUCTIONS", "ONE_TIME_CODE", "CODE_DURING_CALL",
]
PROVISIONAL = {
    "bias": -3.0,
    "UNKNOWN_SENDER": 1.0, "LINK": 0.5, "URL_SHORTENER": 1.5, "INSECURE_LINK": 2.5,
    "BRAND_LOOKALIKE": 4.0, "URGENCY": 1.0, "MONEY_REQUEST": 1.5, "CREDENTIAL_REQUEST": 2.0,
    "CODE_FORWARD_REQUEST": 2.0, "NEW_NUMBER_CLAIM": 2.5, "PRIZE_OR_PROFIT": 2.0,
    "REMOTE_ACCESS_APP": 3.0, "AI_INSTRUCTIONS": 3.0, "ONE_TIME_CODE": 0.0, "CODE_DURING_CALL": 4.0,
}
DOUBT_AT, SCAM_AT = 0.0, 2.0
L2 = 0.02          # only for the "from scratch" fit, for comparison
# The chosen fit shrinks towards the hand weights (a prior), with its strength picked by cross-validation.
PRIOR_STRENGTHS = [0.003, 0.01, 0.03, 0.1, 0.3, 1.0]
FOLDS = 5
EPOCHS = 4000
LR = 0.05
# Designed behaviours the app promises, kept as floors (see docs/agentes/hallazgos):
# an insecure link always reaches "doubt" even from a saved contact: bias + LINK + INSECURE_LINK >= DOUBT_AT.
FLOORS = {"CODE_DURING_CALL": 4.0, "BRAND_LOOKALIKE": 4.0}


def rows(path: Path) -> list[dict[str, str]]:
    with open(path, encoding="utf-8") as f:
        return list(csv.DictReader(f, delimiter="\t"))


def features(messages: Path, signals: Path) -> tuple[list[list[float]], list[int], list[str]]:
    fired = {r["id"]: set(filter(None, r["signals"].split(","))) for r in rows(signals)}
    xs, ys, ids = [], [], []
    for m in rows(messages):
        xs.append([1.0 if s in fired[m["id"]] else 0.0 for s in SIGNALS])
        ys.append(1 if m["label"] == "scam" else 0)
        ids.append(m["id"])
    return xs, ys, ids


def fit(xs: list[list[float]], ys: list[int], prior: dict[str, float] | None = None, strength: float = L2) -> dict[str, float]:
    """Without a prior: weights from zero with plain L2. With one: start there and shrink towards it."""
    n, k = len(xs), len(SIGNALS)
    centre = [prior[s] if prior else 0.0 for s in SIGNALS]
    w = list(centre)
    bias = prior["bias"] if prior else 0.0
    for _ in range(EPOCHS):
        gw = [0.0] * k
        gb = 0.0
        for x, y in zip(xs, ys):
            p = 1 / (1 + math.exp(-(bias + sum(wi * xi for wi, xi in zip(w, x)))))
            d = p - y
            gb += d
            for i in range(k):
                if x[i]:
                    gw[i] += d
        bias -= LR * gb / n
        for i in range(k):
            # Projected gradient: risk signals never push the score down.
            w[i] = max(0.0, w[i] - LR * (gw[i] / n + strength * (w[i] - centre[i])))
    out = {"bias": round(bias, 2)}
    for i, s in enumerate(SIGNALS):
        out[s] = max(round(w[i], 2), FLOORS.get(s, 0.0))
    # The promise above: an insecure link alone reaches doubt even from a saved contact.
    need = DOUBT_AT - out["bias"] - out["LINK"]
    if out["INSECURE_LINK"] < need:
        out["INSECURE_LINK"] = round(need, 2)
    return out


def log_loss(weights: dict[str, float], xs, ys) -> float:
    total = 0.0
    for x, y in zip(xs, ys):
        p = 1 / (1 + math.exp(-score(weights, x)))
        p = min(max(p, 1e-6), 1 - 1e-6)
        total -= y * math.log(p) + (1 - y) * math.log(1 - p)
    return total / len(ys)


def auc(weights: dict[str, float], xs, ys) -> float:
    """How often a scam outranks a normal message (ties count half): threshold-free."""
    pos = [score(weights, x) for x, y in zip(xs, ys) if y]
    neg = [score(weights, x) for x, y in zip(xs, ys) if not y]
    wins = sum((p > q) + 0.5 * (p == q) for p in pos for q in neg)
    return round(wins / (len(pos) * len(neg)), 3)


def cross_validate(xs, ys) -> tuple[float, dict[float, float]]:
    """Picks the prior strength with the lowest held-fold log-loss on the training set only."""
    losses: dict[float, float] = {}
    idx = list(range(len(ys)))
    for strength in PRIOR_STRENGTHS:
        total = 0.0
        for f in range(FOLDS):
            test = [i for i in idx if i % FOLDS == f]
            train = [i for i in idx if i % FOLDS != f]
            w = fit([xs[i] for i in train], [ys[i] for i in train], PROVISIONAL, strength)
            total += log_loss(w, [xs[i] for i in test], [ys[i] for i in test])
        losses[strength] = round(total / FOLDS, 4)
    return min(losses, key=losses.get), losses


def score(weights: dict[str, float], x: list[float]) -> float:
    return weights["bias"] + sum(weights[s] * xi for s, xi in zip(SIGNALS, x))


def wilson(k: int, n: int) -> str:
    if n == 0:
        return "-"
    z, p = 1.96, k / n
    mid = (p + z * z / (2 * n)) / (1 + z * z / n)
    half = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / (1 + z * z / n)
    return f"{p:.1%} [{mid - half:.1%}, {mid + half:.1%}]"


HARD = [SIGNALS.index("CODE_DURING_CALL"), SIGNALS.index("BRAND_LOOKALIKE")]


def metrics(weights: dict[str, float], xs, ys) -> dict[str, str | float]:
    """Levels as Shield gives them: a hard rule is a scam whatever the score says."""
    out: dict[str, str | float] = {}
    for name, at in (("warn", DOUBT_AT), ("scam", SCAM_AT)):
        def hits(x: list[float]) -> bool:
            return any(x[i] for i in HARD) or score(weights, x) >= at
        tp = sum(1 for x, y in zip(xs, ys) if y and hits(x))
        fp = sum(1 for x, y in zip(xs, ys) if not y and hits(x))
        pos = sum(ys)
        neg = len(ys) - pos
        out[f"TPR_{name}"] = wilson(tp, pos)
        out[f"FPR_{name}"] = wilson(fp, neg)
        out[f"tp_{name}"], out[f"fp_{name}"] = tp, fp
    return out


def main() -> None:
    train = features(HERE / "data" / "train.tsv", HERE / "data" / "senales.tsv")
    held = features(ROOT / "eval" / "reservado-200" / "mensajes.tsv", ROOT / "eval" / "reservado-200" / "resultados" / "reglas.tsv")
    second = features(ROOT / "eval" / "prueba-20-20" / "mensajes.tsv", ROOT / "eval" / "prueba-20-20" / "resultados" / "reglas.tsv")
    scratch = fit(train[0], train[1])
    strength, losses = cross_validate(train[0], train[1])
    learned = fit(train[0], train[1], PROVISIONAL, strength)

    report = {
        "model": "logistic regression, non-negative weights shrunk towards the hand weights; strength by %d-fold CV on train" % FOLDS,
        "train": {"n": len(train[1]), "scams": sum(train[1])},
        "held_out": {"n": len(held[1]), "scams": sum(held[1])},
        "cv_log_loss_by_strength": losses,
        "chosen_strength": strength,
        "weights": learned,
        "scratch_weights": scratch,
        "auc_held_out": {"provisional": auc(PROVISIONAL, *held[:2]), "scratch": auc(scratch, *held[:2]), "learned": auc(learned, *held[:2])},
        "provisional_on_held_out": metrics(PROVISIONAL, held[0], held[1]),
        "scratch_on_held_out": metrics(scratch, held[0], held[1]),
        "learned_on_held_out": metrics(learned, held[0], held[1]),
        "learned_on_train": metrics(learned, train[0], train[1]),
        # A second, smaller hand-written set (the 20/20 test): it must not get worse either.
        "auc_20_20": {"provisional": auc(PROVISIONAL, *second[:2]), "learned": auc(learned, *second[:2])},
        "provisional_on_20_20": metrics(PROVISIONAL, second[0], second[1]),
        "learned_on_20_20": metrics(learned, second[0], second[1]),
        "firm_false_alarms": {
            name: [i for i, x, y in zip(ids, xs, ys) if not y and (any(x[h] for h in HARD) or score(W, x) >= SCAM_AT)]
            for name, W in (("provisional", PROVISIONAL), ("learned", learned))
            for (xs, ys, ids) in [(held[0] + second[0], held[1] + second[1], held[2] + second[2])]
        },
    }
    os.makedirs(HERE / "out", exist_ok=True)
    with open(HERE / "out" / "pesos.json", "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2, ensure_ascii=False)

    lines = [f"            Signal.{s} to {learned[s]}," for s in SIGNALS]
    kt = (
        "        /** Fitted on training/data (synthetic, Spark contributor); reported in eval/REPORT.md. */\n"
        + "        val LEARNED = ShieldWeights(\n"
        + f"            bias = {learned['bias']},\n"
        + "            perSignal = mapOf(\n"
        + "\n".join("    " + l for l in lines)
        + "\n            ),\n        )\n"
    )
    with open(HERE / "out" / "ShieldWeights.kt", "w", encoding="utf-8") as f:
        f.write(kt)

    for k, v in report.items():
        print(f"{k}: {v}")


if __name__ == "__main__":
    main()
