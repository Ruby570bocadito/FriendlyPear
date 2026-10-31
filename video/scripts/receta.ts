/**
 * The edit's recipe: from what Whisper heard (each word and when) and where Silero found a voice, decides which piece
 * of the recording is each scene's line (the last good take, if a line was read twice), which silences go (a short
 * breath stays between phrases), and when each word of the line will be said after the cuts and the speed-up.
 * Everything after this step only reads receta.json. Run by editar.ps1:
 *
 *   node video/scripts/receta.ts <work dir> <speed>
 */
import { readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const [work, speedArg] = process.argv.slice(2);
const speed = Number(speedArg);
const video = join(import.meta.dirname, "..");

/** What each scene says on screen at its start (the jury reads the story at a glance). Scene 8 ends on the logo. */
export const HEADLINES: Record<string, string> = {
  1: "Meet Carmen & Peri",
  2: "Scam #1 · The new number",
  3: "Scam #2 · The fake bank",
  4: "Scam #3 · The QR trap",
  5: "The family decides",
  6: "\"I've fallen\"",
  7: "How Peri works",
};

/** A pause no longer than this stays as it is; longer ones shrink to it (s). Longer after a full stop. */
const BREATH = 0.22;
const BREATH_SENTENCE = 0.38;
/** A line counts as read when this share of its words is heard, in order. */
const ENOUGH = 0.6;

type Heard = { text: string; from: number; to: number };
type Word = { w: string; from: number; to: number };

const norm = (word: string) => word.toLowerCase().replace(/[^a-z0-9]/g, "");

/** The script's words as shown: "Q R" is written apart for the draft voice, but it's one word on screen. */
const tokens = (line: string) => {
  const out: string[] = [];
  for (const t of line.split(/\s+/).filter(Boolean)) {
    const last = out[out.length - 1];
    if (last && /^[A-Z]$/.test(last) && /^[A-Z][^a-z]*$/.test(t)) out[out.length - 1] = last + t;
    else out.push(t);
  }
  return out;
};

/**
 * Where the line [a] was said in [b], from [from] on: all of [a], against any stretch of [b], a heard word that isn't in
 * the line costing as much as a line word that wasn't heard. Of the best stretches, the last (a line read twice).
 * The pairs (i in a, j in b) that match.
 */
const align = (a: string[], b: string[], from: number) => {
  const m = a.length, n = b.length - from;
  const dp = Array.from({ length: m + 1 }, (_, i) => Float64Array.from({ length: n + 1 }, (_, j) => (j === 0 ? -i : i === 0 ? 0 : -Infinity)));
  for (let i = 1; i <= m; i++)
    for (let j = 1; j <= n; j++)
      dp[i][j] = Math.max(dp[i - 1][j - 1] + (a[i - 1] === b[from + j - 1] ? 2 : -1), dp[i - 1][j] - 1, dp[i][j - 1] - 1);
  const best = Math.max(...dp[m]);
  let end = 0;
  for (let j = 1; j <= n; j++) if (dp[m][j] >= best - 2) end = j;
  const pairs: [number, number][] = [];
  for (let i = m, j = end; i > 0 && j > 0; ) {
    if (dp[i][j] === dp[i - 1][j - 1] + (a[i - 1] === b[from + j - 1] ? 2 : -1)) {
      if (a[i - 1] === b[from + j - 1]) pairs.unshift([i - 1, from + j - 1]);
      i--, j--;
    } else if (dp[i][j] === dp[i - 1][j] - 1) i--;
    else j--;
  }
  return pairs;
};

const heard: Heard[] = JSON.parse(readFileSync(join(work, "transcripcion.json"), "utf8"))
  .transcription.map((s: any) => ({ text: s.text.trim(), from: s.offsets.from / 1000, to: s.offsets.to / 1000 }))
  .filter((h: Heard) => norm(h.text));
const voiced: { from: number; to: number }[] = JSON.parse(readFileSync(join(work, "tramos.json"), "utf8"));
const lines: Record<string, string> = JSON.parse(readFileSync(join(video, "public/voice/lines.json"), "utf8"));
const heardNorm = heard.map((h) => norm(h.text));

/** How long each scene leaves for its voice: from its voice's start to 0.5 s before it ends (src/Demo.tsx). */
const room: Record<string, number> = {};
for (const m of readFileSync(join(video, "src/Demo.tsx"), "utf8").matchAll(/\{ n: (\d+), from: ([\d.]+), to: ([\d.]+),[^}]*voiceAt: ([\d.]+)/g))
  room[m[1]] = Number(m[3]) - Number(m[2]) - Number(m[4]) - 0.5;

const notes: string[] = [];
const scenes: any[] = [];
let after = 0;

for (const [n, line] of Object.entries(lines)) {
  const shown = tokens(line);
  const want = shown.map(norm);
  const pairs = after < heard.length ? align(want, heardNorm, after) : [];
  const take = { score: pairs.length / want.length, pairs };
  if (take.score < ENOUGH) {
    notes.push(`Escena ${n}: no encontré esta frase en la grabación (solo el ${Math.round(take.score * 100)} %), se queda la voz provisional.`);
    continue;
  }
  const first = take.pairs[0][1], last = take.pairs[take.pairs.length - 1][1];
  after = last + 1;

  // Whisper stretches each word up to the next one, silences included, so the voice itself comes from Silero: its
  // stretches with a voice, between this line's first word and the next word heard, which may be the next take.
  const floor = first > 0 ? (heard[first - 1].from + heard[first].from) / 2 : 0;
  const ceil = last + 1 < heard.length ? heard[last + 1].from : Infinity;
  const lastEnd = heard[last].to;
  const runs = voiced
    .filter((v) => v.to > Math.max(floor, heard[first].from - 0.4) && v.from < Math.min(ceil, lastEnd + 0.1))
    .map((v) => {
      const to = Math.min(v.to, ceil);
      // A full stop if a line word that ends one starts in this stretch's last half second.
      const ends = take.pairs.some(([i, j]) => /[.?!:]$/.test(shown[i]) && heard[j].from >= v.from && heard[j].from < to && heard[j].from > to - 0.8);
      return { from: Math.max(v.from, floor), to, ends };
    });
  if (runs.length === 0) runs.push({ from: heard[first].from, to: lastEnd, ends: true });

  // Each cut keeps a breath of the room's own sound: part after one run, the rest before the next.
  const cuts: { from: number; to: number }[] = [];
  runs.forEach((run, i) => {
    const prev = runs[i - 1], next = runs[i + 1];
    const before = prev ? Math.min(run.from - prev.to, prev.ends ? BREATH_SENTENCE : BREATH) * 0.4 : 0.06;
    const tail = next ? Math.min(next.from - run.to, run.ends ? BREATH_SENTENCE : BREATH) * 0.6 : 0.18;
    cuts.push({ from: +Math.max(0, run.from - before).toFixed(3), to: +(run.to + tail).toFixed(3) });
  });

  // When a moment of the recording is heard in the finished line (s), after the cuts and the speed-up.
  const at = (t: number) => {
    let done = 0;
    for (const c of cuts) {
      if (t < c.from) return done / speed;
      if (t <= c.to) return (done + t - c.from) / speed;
      done += c.to - c.from;
    }
    return done / speed;
  };

  // Each word of the script, with the time it is heard; words Whisper missed take their place between their neighbours.
  const matched = new Map(take.pairs);
  const words: Word[] = shown.map((w, i) => {
    const j = matched.get(i);
    return { w, from: j === undefined ? NaN : at(heard[j].from), to: j === undefined ? NaN : at(heard[j].to) };
  });
  for (let i = 0; i < words.length; i++) {
    if (!isNaN(words[i].from)) continue;
    let k = i;
    while (k < words.length && isNaN(words[k].from)) k++;
    const start = i > 0 ? words[i - 1].to : 0;
    const end = k < words.length ? words[k].from : at(cuts[cuts.length - 1].to);
    for (let x = i; x < k; x++) {
      words[x].from = start + ((end - start) * (x - i)) / (k - i);
      words[x].to = start + ((end - start) * (x - i + 1)) / (k - i);
    }
  }

  const length = +(cuts.reduce((s, c) => s + c.to - c.from, 0) / speed).toFixed(2);
  if (room[n] && length > room[n])
    notes.push(`Escena ${n}: tu voz dura ${length} s y la escena deja ${room[n].toFixed(1)} s; se cortaría el final. Léela algo más rápido o alargamos la escena.`);
  if (take.score < 0.85) notes.push(`Escena ${n}: solo oí el ${Math.round(take.score * 100)} % de la frase; revisa que dijiste el guion.`);
  scenes.push({ n, line, heard: +take.score.toFixed(2), cuts, length, words: words.map((w) => ({ ...w, from: +w.from.toFixed(3), to: +w.to.toFixed(3) })) });
}

const recipe = { speed, face: null, headlines: HEADLINES, scenes, notes };
writeFileSync(join(work, "receta.json"), JSON.stringify(recipe, null, 1));
console.log(`Escenas con tu voz: ${scenes.map((s) => s.n).join(", ") || "ninguna"}`);
for (const note of notes) console.log(`Aviso · ${note}`);
