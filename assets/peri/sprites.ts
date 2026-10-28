/**
 * Peri in layers with Muse Image (muse-image-1.0, $0.01 per image): bodies with an empty face, eyes and mouths to
 * lay over them, new moves, Mini Peri, effects and section illustrations (docs/agentes/sprites list, "Peri por partes").
 * Writes assets/peri/sprites/<name>.webp and logs each prompt in assets/peri/sprites/prompts.json.
 * It never copies anything into android/: putting the layers together is the app's job.
 *
 *   cd backend; bun ../assets/peri/sprites.ts 1               lote 1 (skips images already made)
 *   cd backend; bun ../assets/peri/sprites.ts 3 body_happy_noface face_eyes_up   only those names (made again)
 * Then `python assets/peri/sprites_sheet.py <lote>` draws the contact sheet in assets/peri/sprites/hojas/.
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";

const here = dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, "$1"));
const out = join(here, "sprites");
const base = process.env.META_API_BASE ?? "https://api.meta.ai/v1";
const key = process.env.META_API_KEY;
if (!key) throw new Error("META_API_KEY is missing in backend/.env");

const STYLE =
  "Soft 3D clay render, rounded shapes, gentle studio lighting, tactile matte plasticine material, no text, no letters, " +
  "centered character with space around it, plain flat warm cream background (#FFF8EC), no floor shadow.";
const SAME = "Exactly the same image: same character, same pose, same framing, same size and position, same background and props. Only change: ";
const KEEP_EYES = "The mouth, the cheeks and everything else stay exactly the same.";
const KEEP_MOUTH = "The eyes, the cheeks and everything else stay exactly the same.";
const NOFACE = "Peri's face is empty: no eyes, no eyebrows and no mouth; that part of the pear is smooth green clay with the same soft shading as the rest of her body. Keep the two rosy cheeks exactly where they are.";
const MOVE = "Same character, same style, same size in the frame. Change: ";
const SCENE = "Same character, same style. Change: Peri ";

/** One image: its reference ("peri_x" in assets/peri/, or "sprites/x" made earlier; none for a generation) and prompt. */
type Job = { name: string; from?: string; prompt: string };
const LOTES: Record<string, Job[]> = {};

const edit = (name: string, from: string, change: string, keep = "") =>
  ({ name, from, prompt: `${SAME}${change}${keep ? ` ${keep}` : ""} ${STYLE}` });
const eyes = (name: string, change: string) => edit(`face_eyes_${name}`, "peri_calm", change, KEEP_EYES);
const mouth = (name: string, change: string) => edit(`face_mouth_${name}`, "peri_calm", change, KEEP_MOUTH);

LOTES["1"] = [
  edit("body_calm_noface", "peri_calm", NOFACE),
  eyes("happy", "the eyes are two happy upward curves, as when smiling with the eyes."),
  eyes("wide", "the eyes are open extra wide and round, with bigger white highlights, and the eyebrows are raised: amazed."),
  eyes("laugh", "the eyes are squeezed tightly shut into two thin curved lines pressed hard together, like \">\" and \"<\", as when laughing hard; not the soft smiling arches."),
  eyes("worried", "the eyebrows tilt up in the middle and the eyes look a little worried, still kind."),
  eyes("love", "each eye is a glossy coral heart."),
  eyes("stars", "each eye is a shiny golden five-pointed star with a small white highlight."),
  eyes("up", "both pupils look up, as if at something above her."),
  eyes("down", "both eyes keep the same round shape and size, with no eyelid line; the dark eyes and their white highlights shift towards the bottom, as if she looks down at her feet."),
  mouth("m", "the lips are pressed together in a small closed line, as when saying \"m\"."),
  mouth("ee", "the mouth is a wide open smile showing the top teeth, as when saying \"ee\"."),
  mouth("u", "the lips are pushed forward in a small round pucker, as when saying \"oo\"."),
  mouth("laugh", "the mouth is wide open in a big laugh, with a little pink tongue."),
  mouth("grin", "a big closed-teeth grin from cheek to cheek."),
  mouth("surprised", "the mouth is a big round open \"oh!\", about as wide as one of her eyes, dark inside with a little pink tongue."),
];

LOTES["2"] = [
  eyes("wink_left", "the eye on the left side of the image is closed in a happy curve; the other eye stays open."),
  eyes("sleepy", "heavy, half-closed eyelids: sleepy and content."),
  eyes("brave", "the eyebrows are lowered and determined, the eyes bright and focused: brave, ready to protect."),
  eyes("teary", "the eyes are glossy with a small tear at the corner of each: moved, not sad."),
  mouth("f", "the top teeth rest on the lower lip, as when saying \"f\"."),
  mouth("l", "the mouth is open with the tip of the tongue touching the top teeth, as when saying \"l\"."),
  mouth("worried", "a small wavy, worried mouth."),
  mouth("sad", "a small, gentle frown."),
  mouth("tongue", "a playful smile with the tongue sticking out a little."),
  mouth("kiss", "the lips puckered as if blowing a kiss."),
  mouth("whistle", "small round lips, as if whistling a tune."),
  mouth("smirk", "a cheeky smile, one corner higher than the other."),
  mouth("yawn", "a big, tall open yawn."),
  edit("face_blush_strong", "peri_calm", "the cheeks are much rosier and a little bigger, as if shy.", "The eyes, the mouth and everything else stay exactly the same."),
];

const WALK = ["gesturea", "gestureb", "stepleft", "steplefthalf", "stepright", "steprighthalf"];
const noface = (frame: string) => edit(`body_${frame}_noface`, `peri_${frame}`, NOFACE);
LOTES["3"] = [
  ...WALK.map((f) => noface(`calm_${f}`)),
  ...["listening", "listening_gesturea", "listening_gestureb", "listening_stepleft", "listening_stepright"].map(noface),
  ...["thinking", "worried", "protective", "calling", "waiting", "happy"].flatMap((s) => [s, ...WALK.map((f) => `${s}_${f}`)].map(noface)),
  noface("sleeping"),
  ...["bye", "wave1", "wave2", "hero", "medal", "clap", "party", "thumbsup", "love", "idea", "sorry", "letter", "scarf", "butterfly", "tea", "flower", "phone"]
    .map((p) => noface(`calm_${p}`)),
];

const MOVES: Record<string, string> = {
  wave3: "the arm on the right side of the image is raised to shoulder height with the hand open: the third step of a friendly wave.",
  jump_crouch: "knees bent and body squashed a little, both arms back, about to jump.",
  jump_air: "in the air, both feet tucked off the ground, arms up, body stretched with joy.",
  jump_land: "landing: knees bent, body squashed, arms out for balance.",
  dance1: "dancing: the arm on the left side of the image up, hips swaying to the right.",
  dance2: "dancing: both arms down at her sides, hips swaying to the left.",
  dance3: "dancing: the arm on the right side of the image up, hips swaying to the left.",
  dance4: "dancing: both arms up in a happy \"yay\".",
  point: "pointing straight at the viewer with one finger, friendly.",
  point_up: "one finger raised, as if saying \"listen!\" or \"I know!\".",
  think: "one hand on her chin, thinking; the other hand on her tummy.",
  hips: "both hands on her hips, proud.",
  heart: "both hands together in front of her chest, making a heart shape.",
  hug: "both arms wide open, inviting a hug.",
  bow1: "a small polite bow, leaning a little forward.",
  bow2: "a deeper bow.",
  stretch_up: "both arms stretched up high.",
  stretch_side: "both arms stretched out to the sides.",
  run1: "running on the spot: the foot on the left side of the image forward, arms swinging.",
  run2: "running on the spot: the foot on the right side of the image forward, arms swinging the other way.",
  sit: "sitting on the ground with her legs forward, relaxed, hands resting on the ground.",
};
const move = (name: string, change: string) => ({ name, from: "peri_calm", prompt: `${MOVE}${change} ${STYLE}` });
LOTES["4"] = [
  ...Object.entries(MOVES).flatMap(([n, c]) => [move(`move_${n}`, c), edit(`move_${n}_noface`, `sprites/move_${n}`, NOFACE)]),
  move("turn_side_left", "Peri seen from the side, facing the left side of the image."),
  move("turn_back", "Peri seen from behind: the back of the pear, the leaf and the stem, no face."),
  move("turn_side_right", "Peri seen from the side, facing the right side of the image."),
];

const mini = (name: string, change: string) =>
  ({ name, from: "peri_icon", prompt: `Same character, same style. Change: ${change} Her face is empty: no eyes, no eyebrows and no mouth, smooth green clay; keep the two rosy cheeks. ${STYLE}` });
LOTES["5"] = [
  edit("mini_noface", "peri_icon", NOFACE),
  edit("mini_leaf_left", "sprites/mini_noface", "the leaf on top bends towards the left side of the image, as in a breeze."),
  edit("mini_leaf_right", "sprites/mini_noface", "the leaf on top bends towards the right side of the image, as in a breeze."),
  mini("mini_peek", "Peri peeks in from the right edge of the image: only her head and one small hand holding the edge show."),
  mini("mini_wave", "her head with one small hand waving beside it."),
  mini("mini_umbrella", "her head under a tiny coral umbrella held by one small hand."),
  mini("mini_sleep", "her head wearing a tiny night cap."),
];

const FX: Record<string, string> = {
  heart_coral: "A small chubby coral heart.",
  heart_pink: "A small chubby soft pink heart.",
  heart_broken: "A small coral heart cracked in two halves with a little gap.",
  sparkle: "A small four-pointed twinkle, white with a pale yellow glow.",
  music_note: "A small green music note (an eighth note).",
  sweat_drop: "A small light blue drop of sweat.",
  tear: "A small clear light blue teardrop.",
  question: "A chunky question mark symbol in soft yellow clay.",
  exclamation: "A chunky exclamation mark symbol in soft amber clay.",
  bulb: "A small glowing yellow lightbulb.",
  confetti_strip: "A small curly strip of confetti in lime green.",
  confetti_dot: "A small round flat confetti dot in sunny yellow.",
  dust_puff: "A small soft round puff of cream dust.",
  leaf: "A single green pear leaf, like the one on Peri's head.",
  snowflake: "A small chubby white snowflake.",
  petal: "A single soft pink flower petal.",
};
LOTES["6"] = Object.entries(FX).map(([n, item]) => ({
  name: `fx_${n}`,
  prompt: `${item} Soft 3D clay render of one single small object, isolated and centered, straight front view, symmetrical, no character, no face, no text, plain flat warm cream background (#FFF8EC), no floor shadow, tactile matte plasticine material, gentle studio lighting.`,
}));

const SCENES: Record<string, string> = {
  diary: "writes in a small diary with a pencil, little coral hearts floating around.",
  learn: "wears a small graduation cap and holds an open book, small golden stars around.",
  family: "hugs a big coral heart, small hearts floating around.",
  settings: "holds a small wrench, a little gear floating beside her, cheerful.",
  chat: "waves beside two small speech bubbles with tiny hearts in them.",
  picture: "holds a small camera, with a little flash sparkle.",
  checkup: "wears a stethoscope around her neck and holds a small green shield.",
  trophy: "holds up a golden trophy, confetti around.",
  cheer: "gives a thumbs up with an encouraging smile, a small star beside her.",
  hello: "waves hello with both arms, a big warm smile.",
  hearts: "is surrounded by small floating hearts and two tiny leaves, eyes smiling.",
  relax: "relaxes in a small hammock, eyes happily closed, little hearts floating.",
  bubble: "is tiny and floats inside a shiny soap bubble.",
  night: "reads a book under a small warm lamp, cosy.",
  morning: "stretches happily holding a small cup of tea, a little sun beside her.",
};
LOTES["7"] = Object.entries(SCENES).map(([n, c]) => ({ name: `scene_${n}`, from: "peri_calm", prompt: `${SCENE}${c} ${STYLE}` }));

async function call(path: string, body: unknown): Promise<Buffer> {
  const res = await fetch(`${base}${path}`, {
    method: "POST",
    headers: { authorization: `Bearer ${key}`, "content-type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`${path} answered ${res.status}: ${(await res.text()).slice(0, 300)}`);
  const json = (await res.json()) as { data: { b64_json?: string }[] };
  const b64 = json.data?.[0]?.b64_json;
  if (!b64) throw new Error(`${path} returned no image`);
  return Buffer.from(b64, "base64");
}

function reference(from: string) {
  const file = from.startsWith("sprites/") ? join(out, `${from.slice(8)}.webp`) : join(here, `${from}.webp`);
  return `data:image/webp;base64,${readFileSync(file).toString("base64")}`;
}

async function make(job: Job) {
  const body = { model: "muse-image-1.0", prompt: job.prompt, n: 1, size: "1024x1024", output_format: "webp" };
  const image = job.from
    ? await call("/images/edits", { ...body, images: [{ image_url: reference(job.from) }] })
    : await call("/images/generations", body);
  writeFileSync(join(out, `${job.name}.webp`), image);
  console.log(`${job.name}: ${Math.round(image.length / 1024)} KB`);
}

const [lote, ...names] = process.argv.slice(2);
const jobs = LOTES[lote];
if (!jobs) throw new Error(`lote must be one of ${Object.keys(LOTES).join(", ")}`);
if (names[0] === "--names") {
  console.log(jobs.map((j) => j.name).join(" "));
  process.exit(0);
}
mkdirSync(out, { recursive: true });
const todo = names.length ? jobs.filter((j) => names.includes(j.name)) : jobs.filter((j) => !existsSync(join(out, `${j.name}.webp`)));

// A few at a time; a "_noface" made from another sprite of the same lote waits for it.
const done = new Set<string>();
const failed: string[] = [];
const pending = [...todo];
async function worker() {
  while (pending.length) {
    const i = pending.findIndex((j) => !j.from?.startsWith("sprites/") || !todo.some((t) => `sprites/${t.name}` === j.from) || done.has(j.from.slice(8)));
    if (i < 0) { await new Promise((r) => setTimeout(r, 500)); continue; }
    const [job] = pending.splice(i, 1);
    try { await make(job); } catch (e) { failed.push(job.name); console.error(`${job.name}: ${(e as Error).message}`); }
    done.add(job.name);
  }
}
await Promise.all(Array.from({ length: 4 }, worker));

const log = join(out, "prompts.json");
const previous = existsSync(log) ? (JSON.parse(readFileSync(log, "utf8")) as Record<string, string>) : {};
for (const j of todo) if (!failed.includes(j.name)) previous[j.name] = j.prompt;
writeFileSync(log, JSON.stringify(previous, null, 2) + "\n");
const made = todo.length - failed.length;
console.log(`lote ${lote}: ${made} images (about $${(made * 0.01).toFixed(2)})${failed.length ? `, failed: ${failed.join(" ")}` : ""}`);
