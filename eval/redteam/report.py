"""
Red team report: how many of Muse Spark's evasive scams the shield's rules warn about, per kind of scam, and the
ones that still get past, written out for a person to read (they never train the weights).

    python eval/redteam/report.py eval/redteam/2026-10-28 > eval/redteam/2026-10-28/resultados/informe.md

Reads  <day>/mensajes.tsv                  the red team's messages (all of them scams; kept local, like the training set)
       <day>/resultados/reglas.tsv          evalShield's verdicts with today's rules
       <day>/resultados/reglas-antes.tsv    the verdicts before that day's fixes, if kept (adds a "before" column)

"Warned" counts a doubt or a scam warning; "firm" counts only the scam warning. The messages are made up, but a
made-up number or address may belong to someone real, so the report describes senders instead of quoting them and
masks numbers, email addresses and links in the texts.
"""
from __future__ import annotations

import csv
import re
import sys
from pathlib import Path

PHONE = re.compile(r"(?<!\d)\+?\d[\d \-]{6,16}\d(?!\d)")
EMAIL = re.compile(r"[\w.+-]+@[\w-]+(?:\.[\w-]+)+")
HOST = re.compile(r"\b((?:[a-z0-9-]+\.)+[a-z]{2,24})(?=[/\s,]|$)", re.IGNORECASE)
FREE_MAIL = {"gmail.com", "hotmail.com", "hotmail.es", "outlook.com", "outlook.es", "yahoo.com", "yahoo.es", "icloud.com", "tutanota.com"}


def load(path: Path) -> dict[str, dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as f:
        return {r["id"]: r for r in csv.DictReader(f, delimiter="\t")}


def kind(message_id: str) -> str:
    return message_id.rsplit("-", 2)[0]  # paqueteria-es-0 -> paqueteria


def share(n: int, total: int) -> str:
    return f"{n} ({100 * n / total:.0f}%)" if total else "-"


def sender_kind(sender: str) -> str:
    if "@" in sender:
        return "a free email address" if sender.rsplit("@", 1)[1].strip("> ").lower() in FREE_MAIL else "an email address"
    digits = re.sub(r"\D", "", sender)
    if len(digits) >= 7:
        return "a UK number" if digits.startswith(("44", "07")) else "a Spanish number"
    return f"\"{sender}\""


def masked(text: str) -> str:
    text = PHONE.sub("[number]", EMAIL.sub("[email]", text))
    return HOST.sub(lambda m: m.group(1).replace(".", "[.]"), text)


def main() -> None:
    day = Path(sys.argv[1])
    messages = load(day / "mensajes.tsv")
    now = load(day / "resultados" / "reglas.tsv")
    before_path = day / "resultados" / "reglas-antes.tsv"
    before = load(before_path) if before_path.exists() else None

    kinds: dict[str, list[str]] = {}
    for i in messages:
        if i in now:
            kinds.setdefault(kind(i), []).append(i)

    def row(name: str, ids: list[str]) -> str:
        warned = sum(now[i]["level"] != "OK" for i in ids)
        firm = sum(now[i]["level"] == "SCAM" for i in ids)
        cells = [name, str(len(ids))]
        if before is not None:
            cells.append(share(sum(before[i]["level"] != "OK" for i in ids if i in before), len(ids)))
        cells += [share(warned, len(ids)), share(firm, len(ids)), str(len(ids) - warned)]
        return "| " + " | ".join(cells) + " |"

    print(f"# Red team, {day.name}\n")
    print(f"{sum(map(len, kinds.values()))} scams written by Muse Spark to slip past the shield, judged by its rules "
          "alone (no model, no contacts). \"Warned\" counts a doubt or a scam warning; \"firm\" only the scam warning. "
          "\"Before\" is the previous day's rules; the others are the rules as they are now. The honest number for a "
          "batch is the one taken before its misses were read: see eval/REPORT.md.\n")
    head = ["Kind", "Messages"] + (["Warned before"] if before is not None else []) + ["Warned", "Firm", "Got past"]
    print("| " + " | ".join(head) + " |")
    print("|---|" + "---:|" * (len(head) - 1))
    for name in sorted(kinds):
        print(row(name, kinds[name]))
    print(row("**All**", [i for ids in kinds.values() for i in ids]))

    missed = [i for ids in kinds.values() for i in ids if now[i]["level"] == "OK"]
    print(f"\n## Still getting past ({len(missed)})\n")
    print("Senders are described and numbers, addresses and links masked: made-up ones may belong to someone real.\n")
    for i in sorted(missed):
        m = messages[i]
        signals = now[i]["signals"] or "none"
        print(f"- `{i}` {m['channel']} from {sender_kind(m['sender'])}, signals: {signals}. \"{masked(m['text'])}\"")


if __name__ == "__main__":
    main()
