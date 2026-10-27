/**
 * Prueba 20/20: rules alone, Muse Spark Open alone, and rules + model vote, over 40 synthetic messages.
 * Run from backend/ so Bun loads backend/.env:
 *
 *   cd backend; bun ../eval/prueba-20-20/run.ts            estimate only, no requests
 *   cd backend; bun ../eval/prueba-20-20/run.ts --run      one request per message, in order, no retries
 *   cd backend; bun ../eval/prueba-20-20/run.ts --report   rebuild the report from saved results, no requests
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { buildMessages, parseOpinion, type OpinionInput } from "../../backend/src/investigator/opinion";
import { sparkOpenFromEnv } from "../../backend/src/models/spark-open";

const dir = dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, "$1"));
const out = join(dir, "resultados");
const PRICE_IN = 1.25 / 1e6;
const PRICE_OUT = 4.25 / 1e6;
const MAX_OUT = 900;
const DOUBT_AT = 0;
const SCAM_AT = 2;

type Row = Record<string, string>;
const tsv = (path: string): Row[] => {
  const [head, ...lines] = readFileSync(path, "utf8").trim().split(/\r?\n/);
  const keys = head.split("\t");
  return lines.map((l) => Object.fromEntries(l.split("\t").map((v, i) => [keys[i], v])));
};

const messages = tsv(join(dir, "mensajes.tsv"));
const rules = new Map(tsv(join(out, "reglas.tsv")).map((r) => [r.id, r]));
const input = (m: Row): OpinionInput => {
  const r = rules.get(m.id)!;
  return {
    channel: m.channel as OpinionInput["channel"],
    senderKnown: m.known === "1",
    callActive: m.call === "1",
    signals: r.signals ? r.signals.split(",") : [],
    text: m.text,
    lang: m.lang === "en" ? "en" : "es",
  };
};

const mode = process.argv.includes("--run") ? "run" : process.argv.includes("--report") ? "report" : "estimate";

if (mode === "estimate") {
  const promptTokens = messages.reduce((n, m) => n + Math.ceil(JSON.stringify(buildMessages(input(m))).length / 3.5), 0);
  const typical = promptTokens * PRICE_IN + messages.length * 550 * PRICE_OUT;
  const worst = promptTokens * PRICE_IN + messages.length * MAX_OUT * PRICE_OUT;
  console.log(`${messages.length} requests to Muse Spark Open (Baseten), one per message, no retries.`);
  console.log(`Input ≈ ${promptTokens} tokens; output ≈ 550 per request (reasoning included), at most ${MAX_OUT}.`);
  console.log(`Estimated cost ≈ $${typical.toFixed(3)} (worst case $${worst.toFixed(3)}), paid from Baseten credits.`);
  process.exit(0);
}

const modelFile = join(out, "modelo.tsv");
if (mode === "run") {
  const client = sparkOpenFromEnv();
  if (!client) throw new Error("BASETEN_API_KEY is missing in backend/.env");
  mkdirSync(out, { recursive: true });
  const lines = ["id\tverdict\tconfidence\tdelta\tfinish\tms\tpromptTokens\tcompletionTokens\treasoningTokens"];
  let cost = 0;
  for (const m of messages) {
    const t = performance.now();
    try {
      const r = await client.chat({ messages: buildMessages(input(m)), maxTokens: MAX_OUT });
      const o = parseOpinion(r.content);
      const ms = Math.round(performance.now() - t);
      cost += r.usage.promptTokens * PRICE_IN + r.usage.completionTokens * PRICE_OUT;
      lines.push([m.id, o.verdict, o.confidence, o.delta, r.finishReason, ms, r.usage.promptTokens, r.usage.completionTokens, r.usage.reasoningTokens].join("\t"));
      console.log(`${m.id} ${m.label.padEnd(6)} → ${o.verdict.padEnd(5)} ${String(o.delta).padStart(5)}  ${ms} ms`);
    } catch (e) {
      lines.push([m.id, "error", 0, 0, "error", Math.round(performance.now() - t), 0, 0, 0].join("\t"));
      console.log(`${m.id} error: ${(e as Error).message.slice(0, 120)}`);
    }
  }
  writeFileSync(modelFile, lines.join("\n") + "\n");
  console.log(`Spent ≈ $${cost.toFixed(4)}`);
}

if (!existsSync(modelFile)) throw new Error("no model results yet: run with --run first");
const model = new Map(tsv(modelFile).map((r) => [r.id, r]));

/** Wilson 95% interval for k successes out of n. */
const wilson = (k: number, n: number) => {
  if (n === 0) return [0, 0];
  const z = 1.96, p = k / n, d = 1 + (z * z) / n;
  const c = (p + (z * z) / (2 * n)) / d, h = (z * Math.sqrt((p * (1 - p)) / n + (z * z) / (4 * n * n))) / d;
  return [Math.max(0, c - h), Math.min(1, c + h)];
};
const pct = (x: number) => `${Math.round(x * 100)} %`;
const interval = (k: number, n: number) => `${pct(k / n)} (${pct(wilson(k, n)[0])}–${pct(wilson(k, n)[1])})`;

type Level = "OK" | "DOUBT" | "SCAM";
const levelOf = (score: number): Level => (score >= SCAM_AT ? "SCAM" : score >= DOUBT_AT ? "DOUBT" : "OK");
const rulesLevel = (m: Row) => rules.get(m.id)!.level as Level;
const modelLevel = (m: Row): Level => {
  const v = model.get(m.id)?.verdict;
  return v === "scam" ? "SCAM" : v === "ok" ? "OK" : "DOUBT";
};
const combinedLevel = (m: Row): Level => {
  const r = rules.get(m.id)!;
  if (r.level !== "DOUBT") return r.level as Level;
  return levelOf(Number(r.score) + Number(model.get(m.id)?.delta ?? 0));
};

const scams = messages.filter((m) => m.label === "scam");
const normals = messages.filter((m) => m.label === "normal");
const row = (name: string, level: (m: Row) => Level) => {
  const caught = scams.filter((m) => level(m) === "SCAM").length;
  const warned = scams.filter((m) => level(m) !== "OK").length;
  const falseScam = normals.filter((m) => level(m) === "SCAM").length;
  const falseWarn = normals.filter((m) => level(m) !== "OK").length;
  return `| ${name} | ${interval(caught, scams.length)} | ${interval(warned, scams.length)} | ${interval(falseScam, normals.length)} | ${interval(falseWarn, normals.length)} |`;
};

const modelWrong = messages.filter((m) => (m.label === "scam" ? modelLevel(m) !== "SCAM" : modelLevel(m) !== "OK")).length;
const timings = [...model.values()].map((r) => Number(r.ms)).sort((a, b) => a - b);
const median = timings[Math.floor(timings.length / 2)] ?? 0;
const spent = [...model.values()].reduce((s, r) => s + Number(r.promptTokens) * PRICE_IN + Number(r.completionTokens) * PRICE_OUT, 0);
const misses = messages
  .filter((m) => (m.label === "scam" ? combinedLevel(m) === "OK" : combinedLevel(m) !== "OK"))
  .map((m) => `- \`${m.id}\` (${m.label}): rules ${rulesLevel(m)}, model ${modelLevel(m)}, together ${combinedLevel(m)}`);

const report = `# Prueba 20/20 · resultados

Preliminary check with **40 synthetic messages** (20 scams, 20 normal; half Spanish, half English), written for this test.
It is not the held-out evaluation (\`eval/data/\`, real messages); numbers in brackets are 95 % Wilson intervals.

| Layer | Scams caught as scam | Scams with any warning | Normal flagged as scam | Normal with any warning |
|---|---|---|---|---|
${row("Rules on the phone", rulesLevel)}
${row("Muse Spark Open alone", modelLevel)}
${row("Rules + model vote (±2, doubtful only)", combinedLevel)}

- Pivot criterion (model wrong in more than a third): model alone wrong in **${modelWrong} of ${messages.length}** → ${modelWrong * 3 > messages.length ? "**pivot**: lean on the rules" : "no pivot"}.
- Median latency of the model: ${median} ms. Cost of this run ≈ $${spent.toFixed(4)} (Baseten credits).

## Where it still fails
${misses.length ? misses.join("\n") : "- Nothing: every scam got a warning and every normal message stayed quiet."}
`;
writeFileSync(join(out, "informe.md"), report);
console.log(report);
