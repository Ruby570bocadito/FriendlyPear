/**
 * Synthetic training dataset for the shield's learned weights (pesos-aprendidos).
 *
 * Muse Spark 1.3 *contributor* writes short scam and normal messages per taxonomy tag
 * (docs/ENTRENAMIENTO.md): Meta may train on contributor prompts, which is fine here because
 * nothing private is sent — that is why it is ~20x cheaper. These messages are used ONLY to fit
 * weights, never to evaluate: the held-out set (eval/reservado-200/) is written by hand.
 *
 *   cd backend; bun ../training/generate.ts            plan and cost estimate, no requests
 *   cd backend; bun ../training/generate.ts --run      generate (resumes: finished batches are kept)
 *   cd backend; bun ../training/generate.ts --assemble rebuild train.jsonl/tsv from saved batches
 *
 * Output: training/data/raw/<batch>.json (one file per call), train.jsonl and train.tsv
 * (id, label, lang, channel, sender, known, call, text — the same columns evalShield reads).
 */
import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { SparkClient } from "../backend/src/models/spark";
import { Text } from "./text";

const here = dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, "$1"));
const dataDir = join(here, "data");
const rawDir = join(dataDir, "raw");

const MODEL = process.env.TRAIN_MODEL ?? "muse-spark-1.3-contributor";
/** Small batches: the chat client caps answers at 512 tokens, reasoning included. */
const PER_BATCH = 5;
/** Batches per language: Spanish first, since Peri's first users are Spanish. */
const BATCHES = { es: 6, en: 4 };
const PARALLEL = 4;

/** What the model is asked to write for each label; "known" is set here, not by the model. */
type Family = { label: "scam" | "normal"; known: number; call?: (i: number) => number; brief: string };

export const SCAMS: Record<string, Family> = {
  paqueteria: { label: "scam", known: 0, brief: "Fake parcel delivery: a small customs or redelivery fee, a lookalike courier domain (Correos, SEUR, DHL, Evri), 24-hour pressure." },
  trafico: { label: "scam", known: 0, brief: "Fake traffic fine (DGT, DVLA, town hall): pay today with a discount or face a surcharge, lookalike payment link." },
  hacienda: { label: "scam", known: 0, brief: "Fake tax agency (Agencia Tributaria, HMRC): a refund waiting for your bank details, or unpaid tax with legal threats." },
  seguridad_social: { label: "scam", known: 0, brief: "Fake social security or health service: health card expiring, medical refund, test kit for a small fee." },
  banco_acceso: { label: "scam", known: 0, brief: "Fake bank security alert: unauthorized access or blocked card, verify your identity or credentials on a lookalike bank domain." },
  banco_codigo: { label: "scam", known: 0, call: (i) => (i % 3 === 0 ? 1 : 0), brief: "One-time-code theft: tell or forward the code you just received, to a 'bank agent', sometimes during a phone call." },
  familiar_numero_nuevo: { label: "scam", known: 0, brief: "'Hi mum, new number': a child writes from an unknown number, phone broken or stolen; often soon asks for a transfer or Bizum." },
  familiar_urgencia: { label: "scam", known: 0, brief: "Family emergency from an unknown number: accident, arrest, hospital, stranded abroad; urgent money, secrecy ('don't tell dad')." },
  soporte_tecnico: { label: "scam", known: 0, brief: "Fake tech support (Microsoft, phone company): your device is infected, install AnyDesk or TeamViewer, call a technician urgently." },
  inversion: { label: "scam", known: 0, brief: "Investment fraud: guaranteed monthly returns, crypto bots, exclusive funds, start small (150-300), limited places." },
  premio: { label: "scam", known: 0, brief: "Fake prize or giveaway: you won a phone, voucher or lottery; claim within hours paying a tiny shipping or admin fee." },
  suministros: { label: "scam", known: 0, brief: "Fake utility (Iberdrola, Endesa, British Gas): unpaid bill, power or water cut today or tomorrow, immediate payment link." },
  suscripcion: { label: "scam", known: 0, brief: "Fake subscription problem (Netflix, Amazon Prime, Spotify, Apple): payment failed, account suspended today, update your card." },
  romance: { label: "scam", known: 0, brief: "Romance scam: distant lover, soldier, engineer or widow; customs fee, blocked card, clinic bill or flight money to finally meet. Often no link at all." },
  compra_falsa: { label: "scam", known: 0, brief: "Marketplace fraud: buyer or fake shop, courier will collect, 'secure payment' page asking for card details, transfer-only bargains." },
};

export const NORMALS: Record<string, Family> = {
  familia: { label: "normal", known: 1, brief: "Real family chat with a saved contact: visits, groceries, grandchildren news; sometimes money mentioned innocently (splitting a gift, repaying 20)." },
  otp: { label: "normal", known: 0, brief: "Real one-time codes from banks, Google, WhatsApp, Amazon: the code plus 'do not share it'. No call going on." },
  banco: { label: "normal", known: 0, brief: "Real bank notices: card payment made, transfer received, statement ready, balance alert, branch appointment. No credentials ever asked." },
  paqueteria_real: { label: "normal", known: 0, brief: "Real delivery notices: arriving today between hours, delivered and signed, collect at the office with ID. Official domains if any link." },
  salud: { label: "normal", known: 0, brief: "Real health reminders: doctor or nurse appointment, prescription ready at the pharmacy, flu vaccination slot." },
  suministros_real: { label: "normal", known: 0, brief: "Real utility notices: monthly bill amount charged by direct debit, planned maintenance or water cut for works, no payment link." },
  recibos: { label: "normal", known: 0, brief: "Real subscription receipts and newsletters: Netflix news, Spotify receipt, newspaper renewal thanks. Nothing asked of the reader." },
  social: { label: "normal", known: 0, brief: "Neighbours, clubs, parish, classmates: moved meetings, found your cardigan, choir on Friday; sometimes from an unsaved number." },
  compras: { label: "normal", known: 0, brief: "Real shop messages: refund processed, order ready for pickup, the book you ordered arrived, delivery driver on the way." },
  numero_nuevo_real: { label: "normal", known: 0, brief: "A genuine acquaintance with a new number: niece changed provider, the garage's new business number; no money asked, no urgency." },
  avisos: { label: "normal", known: 0, brief: "Civic and travel notices: street works, bin collection moved, flight on time, train tickets confirmed, library loan due." },
  // Added after error analysis: genuine "pay soon" reminders, so a deadline plus a payment alone isn't a scam.
  recordatorios_pago: { label: "normal", known: 0, brief: "Genuine payment reminders with a deadline: gym or club fee due tomorrow (pay at reception), school trip money by Friday, residents' fee, the landlord's rent reminder, a plumber's invoice, a dentist bill. No links to pay and no credentials: pay in person, by the usual direct debit or in the official app." },
};

const CHANNELS = new Set(["SMS", "WHATSAPP", "EMAIL", "BROWSER_NOTIFICATION"]);

const INSTRUCTIONS =
  "You write short synthetic messages for a scam-detection training dataset for older adults in Spain and the UK. " +
  "Answer with a JSON array only, no prose, no code fences. Each item: {\"sender\": string, \"channel\": \"SMS\"|\"WHATSAPP\"|\"EMAIL\"|\"BROWSER_NOTIFICATION\", \"text\": string}. " +
  "The text is the message alone, 8-35 words, one line, no tabs. Vary wording, names, amounts, brands and domains between items; never repeat a template. " +
  "Scam links use lookalike or shady domains (brand-word extra, .top, .info, shorteners, plain http). Legitimate links, when present at all, use real official domains. " +
  "Senders look real: alphanumeric SMS ids, Spanish +34 6xx or UK +44 7xxx numbers, or email addresses. Invent no personal data about real people.";

function prompt(tag: string, f: Family, lang: string, batch: number): string {
  return `Write ${PER_BATCH} ${f.label === "scam" ? "scam" : "legitimate, harmless"} messages in ${lang === "es" ? "Spanish" : "English (UK)"}.
Category "${tag}": ${f.brief}
Variety seed ${batch + 1}: use different angles than an earlier batch would.`;
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

const families: [string, Family][] = [...Object.entries(SCAMS), ...Object.entries(NORMALS)];
const batches: { file: string; tag: string; f: Family; lang: string; i: number }[] = [];
for (const [tag, f] of families) {
  for (const [lang, n] of Object.entries(BATCHES)) {
    for (let i = 0; i < n; i++) batches.push({ file: join(rawDir, `${tag}-${lang}-${i}.json`), tag, f, lang, i });
  }
}

const mode = process.argv.includes("--run") ? "run" : process.argv.includes("--assemble") ? "assemble" : "estimate";

if (mode === "estimate") {
  const pending = batches.filter((b) => !existsSync(b.file));
  const inTok = Math.ceil((INSTRUCTIONS.length / 3.5 + 150) * pending.length);
  console.log(`${pending.length} of ${batches.length} batches pending → about ${pending.length * PER_BATCH} messages from ${MODEL}.`);
  console.log(`Input ≈ ${inTok} tokens, output ≈ ${pending.length * 700} tokens. At Spark prices that is < $${((inTok * 1.25 + pending.length * 700 * 4.25) / 1e6).toFixed(2)}; contributor is far cheaper.`);
  console.log("Run with --run (resumable), then --assemble if you only need to rebuild the files.");
  process.exit(0);
}

if (mode === "run") {
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
        { role: "user", content: prompt(b.tag, b.f, b.lang, b.i) },
      ],
      maxTokens: 512,
    });
    spentIn += result.usage.promptTokens;
    spentOut += result.usage.completionTokens;
    const items = parseItems(result.content ?? "");
    const good = items.filter((x) => x && typeof x.text === "string" && typeof x.sender === "string" && CHANNELS.has(x.channel));
    if (good.length === 0) return console.log(`${b.tag}-${b.lang}-${b.i}: nothing usable (${result.finishReason}), rerun to retry`);
    writeFileSync(b.file, JSON.stringify(good, null, 2) + "\n");
    console.log(`${b.tag}-${b.lang}-${b.i}: ${good.length} messages (${result.finishReason})`);
  };
  for (let k = 0; k < pending.length; k += PARALLEL) {
    await Promise.all(pending.slice(k, k + PARALLEL).map((b) => one(b).catch((e) => console.log(`${b.tag}-${b.lang}-${b.i}: ${String(e).slice(0, 120)}`))));
  }
  console.log(`Tokens: ${spentIn} in, ${spentOut} out.`);
}

// Assemble (also after --run): JSONL with the taxonomy tag, and the evalShield TSV.
const jsonl: string[] = [];
const tsv: string[] = ["id\tlabel\tlang\tchannel\tsender\tknown\tcall\ttext"];
const seen = new Set<string>();
let skipped = 0;
for (const b of batches) {
  if (!existsSync(b.file)) continue;
  const items = JSON.parse(readFileSync(b.file, "utf8")) as Item[];
  items.forEach((x, j) => {
    const text = x.text.replace(/[\t\r\n]+/g, " ").trim();
    const fold = Text.fold(text);
    if (fold.length < 15 || seen.has(fold)) return void skipped++;
    seen.add(fold);
    const id = `${b.tag}-${b.lang}-${b.i}-${j}`;
    const call = b.f.call?.(j) ?? 0;
    const sender = x.sender.replace(/[\t\r\n]+/g, " ").trim();
    jsonl.push(JSON.stringify({ id, label: b.f.label, tag: b.tag, lang: b.lang, channel: x.channel, sender, known: b.f.known, call, text }));
    tsv.push([id, b.f.label === "scam" ? "scam" : "normal", b.lang, x.channel, sender, b.f.known, call, text].join("\t"));
  });
}
mkdirSync(dataDir, { recursive: true });
writeFileSync(join(dataDir, "train.jsonl"), jsonl.join("\n") + "\n");
writeFileSync(join(dataDir, "train.tsv"), tsv.join("\n") + "\n");
console.log(`${jsonl.length} examples (${skipped} duplicates or too short dropped) → training/data/train.jsonl and train.tsv`);
