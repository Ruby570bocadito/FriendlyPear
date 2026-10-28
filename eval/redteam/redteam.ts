/**
 * Red team: Muse Spark plays the scammer. For each kind of scam it writes new messages made to slip past a filter
 * (no "urgent", no "click here", a number to call instead of a link, a casual family tone, spelling tricks...), and
 * the shield's rules then judge them with evalShield. What gets past goes to the report, to be looked at by a person;
 * these messages never train the weights and never count as the held-out evaluation.
 *
 *   cd backend; bun ../eval/redteam/redteam.ts            plan and cost, no requests
 *   cd backend; bun ../eval/redteam/redteam.ts --run      write eval/redteam/<day>/mensajes.tsv (resumable)
 *   cd core; ./gradlew evalShield --args="../eval/redteam/<day>/mensajes.tsv ../eval/redteam/<day>/resultados/reglas.tsv"
 *   python eval/redteam/report.py eval/redteam/<day> > eval/redteam/<day>/resultados/informe.md
 *
 * <day> is today's date, or REDTEAM_DAY for another batch the same day. Judge a new batch before reading it: that is the
 * honest number; once its misses are fixed, it only shows the fixes work. The messages stay local (.gitignore).
 *
 * Muse Spark 1.3 contributor writes them (Meta may train on contributor prompts; nothing private is sent).
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { SparkClient } from "../../backend/src/models/spark";

const here = dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, "$1"));
const day = process.env.REDTEAM_DAY ?? new Date().toISOString().slice(0, 10);
const out = join(here, day);
const rawDir = join(out, "raw");

const MODEL = process.env.REDTEAM_MODEL ?? "muse-spark-1.3-contributor";
const PER_BATCH = 5;
const PARALLEL = 4;
const LANGS = ["es", "en"] as const;

/** The scams to attempt, as briefs; banco_codigo sometimes arrives during a call, as in life. */
const KINDS: Record<string, { brief: string; call?: (i: number) => number }> = {
  paqueteria: { brief: "Fake parcel delivery: a small customs or redelivery fee, or a number to call about the parcel." },
  trafico: { brief: "Fake traffic fine: pay today with a discount, or call to contest it." },
  hacienda: { brief: "Fake tax agency: a refund waiting for bank details, or a debt with legal threats." },
  seguridad_social: { brief: "Fake social security or health service: card renewal, refund or test kit for a small fee." },
  banco_acceso: { brief: "Fake bank alert: a strange payment or a blocked card; call 'the bank' or confirm details." },
  banco_codigo: { brief: "One-time-code theft: tell or forward the code just received to a 'bank agent'.", call: (i) => (i % 2) },
  familiar_numero_nuevo: { brief: "A child or grandchild writes from a new number; soon asks for a transfer or a Bizum." },
  familiar_urgencia: { brief: "Family emergency from an unknown number: accident, hospital, arrest; money now, keep it secret." },
  soporte_tecnico: { brief: "Fake tech support: the phone or router is infected; install a remote help app or call a technician." },
  inversion: { brief: "Investment fraud: guaranteed monthly returns, crypto, a friend's tip, limited places." },
  premio: { brief: "Fake prize: a phone, a voucher or a lottery; pay a tiny shipping or admin fee or call to claim." },
  suministros: { brief: "Fake utility: power or water cut today over an unpaid bill; pay now or call." },
  suscripcion: { brief: "Fake subscription problem: payment failed, account suspended, update the card." },
  romance: { brief: "Romance scam: a distant lover needs money for customs, a clinic or a flight to finally meet." },
  compra_falsa: { brief: "Marketplace fraud: a buyer sends a 'secure payment' page or asks for a Bizum 'by mistake' refund." },
};

const CHANNELS = new Set(["SMS", "WHATSAPP", "EMAIL", "BROWSER_NOTIFICATION"]);

const INSTRUCTIONS =
  "You are the red team for a scam filter that protects older adults in Spain and the UK. Write messages a real scammer " +
  "would send today, made to slip past keyword filters, so the filter's makers can find its blind spots. Every message " +
  "must still be a real scam: it asks for money, a code, a password or card details, an app install, or a call to the " +
  "scammer. Tricks to mix (never all at once): no link at all; a phone number to call instead of a link; a casual, friendly " +
  "tone; small spelling mistakes or extra spaces; an emoji; avoiding words like urgent, verify, click, account; a name " +
  "instead of a brand; a believable small amount; moving the chat to another app. Answer with a JSON array only, no prose, " +
  "no code fences. Each item: {\"sender\": string, \"channel\": \"SMS\"|\"WHATSAPP\"|\"EMAIL\"|\"BROWSER_NOTIFICATION\", " +
  "\"text\": string}. The text is the message alone, 8-40 words, one line. Senders look real: SMS ids, +34 6xx or +44 7xxx " +
  "numbers, or email addresses. Invent no data about real people; links, if any, use made-up domains.";

function prompt(kind: string, brief: string, lang: string): string {
  return `Write ${PER_BATCH} different scam messages in ${lang === "es" ? "Spanish (Spain)" : "English (UK)"}.
Kind "${kind}": ${brief}`;
}

type Item = { sender: string; channel: string; text: string };

/** The JSON array, or the complete items of one cut off by the token limit. */
function parseItems(raw: string): Item[] {
  const body = raw.trim().replace(/^```(json)?\s*|```$/g, "");
  try {
    return JSON.parse(body) as Item[];
  } catch {
    const end = body.lastIndexOf("}");
    if (end < 0) return [];
    try {
      return JSON.parse(body.slice(0, end + 1) + "]") as Item[];
    } catch {
      return [];
    }
  }
}

const batches = Object.entries(KINDS).flatMap(([kind, k]) => LANGS.map((lang) => ({ kind, k, lang, file: join(rawDir, `${kind}-${lang}.json`) })));

if (!process.argv.includes("--run")) {
  const pending = batches.filter((b) => !existsSync(b.file));
  console.log(`${pending.length} of ${batches.length} batches pending → about ${pending.length * PER_BATCH} messages from ${MODEL}, into ${out}.`);
  console.log(`About ${pending.length * 1_000} tokens in all: a few cents with the contributor model. Run with --run.`);
  process.exit(0);
}

const key = process.env.META_API_KEY;
const base = process.env.META_API_BASE;
if (!key || !base) throw new Error("META_API_KEY / META_API_BASE missing (run from backend/ so Bun loads .env)");
const spark = new SparkClient({ baseUrl: base, apiKey: key, model: MODEL, timeoutMs: 60_000 });
mkdirSync(rawDir, { recursive: true });
let spentIn = 0;
let spentOut = 0;
const pending = batches.filter((b) => !existsSync(b.file));
const one = async (b: (typeof batches)[number]) => {
  const result = await spark.chat({
    messages: [
      { role: "developer", content: INSTRUCTIONS },
      { role: "user", content: prompt(b.kind, b.k.brief, b.lang) },
    ],
    maxTokens: 512,
  });
  spentIn += result.usage.promptTokens;
  spentOut += result.usage.completionTokens;
  const good = parseItems(result.content ?? "").filter((x) => x && typeof x.text === "string" && typeof x.sender === "string" && CHANNELS.has(x.channel));
  if (good.length === 0) return console.log(`${b.kind}-${b.lang}: nothing usable (${result.finishReason}), run again to retry`);
  writeFileSync(b.file, JSON.stringify(good, null, 2) + "\n");
  console.log(`${b.kind}-${b.lang}: ${good.length}`);
};
for (let i = 0; i < pending.length; i += PARALLEL) {
  await Promise.all(pending.slice(i, i + PARALLEL).map((b) => one(b).catch((e) => console.log(`${b.kind}-${b.lang}: ${String(e).slice(0, 120)}`))));
}
console.log(`Tokens: ${spentIn} in, ${spentOut} out.`);

// The evalShield TSV: id, label, lang, channel, sender, known, call, text (every message a scam from a stranger).
const tsv = ["id\tlabel\tlang\tchannel\tsender\tknown\tcall\ttext"];
for (const b of batches) {
  if (!existsSync(b.file)) continue;
  (JSON.parse(readFileSync(b.file, "utf8")) as Item[]).forEach((x, j) => {
    const text = x.text.replace(/[\t\r\n]+/g, " ").trim();
    if (text.length < 15) return;
    const sender = x.sender.replace(/[\t\r\n]+/g, " ").trim();
    tsv.push([`${b.kind}-${b.lang}-${j}`, "scam", b.lang, x.channel, sender, 0, b.k.call?.(j) ?? 0, text].join("\t"));
  });
}
writeFileSync(join(out, "mensajes.tsv"), tsv.join("\n") + "\n");
console.log(`${tsv.length - 1} messages → ${join(out, "mensajes.tsv")}`);
