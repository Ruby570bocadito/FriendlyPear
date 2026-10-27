/**
 * Peri's illustrated states with Muse Image (muse-image-1.0, $0.01 per image).
 * 1. One base Peri from the description in docs/DISENO.md.
 * 2. Each state as an edit of that base, so it stays the same character.
 * 3. Animation frames: small edits of each state's own image (a blink, talking, the leaf swaying), so the
 *    app can move between them smoothly.
 * Writes assets/peri/peri_<state>[_<frame>].webp and copies them to android/app/src/main/res/drawable-nodpi/.
 *
 *   cd backend; bun ../assets/peri/generate.ts                  all states (10 images, about $0.10)
 *   cd backend; bun ../assets/peri/generate.ts happy            only the named states, from the saved base
 *   cd backend; bun ../assets/peri/generate.ts frames           4 frames per state (36 images, about $0.36)
 *   cd backend; bun ../assets/peri/generate.ts frames calm      only the named states' frames
 */
import { copyFileSync, existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";

const here = dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, "$1"));
const drawables = join(here, "../../android/app/src/main/res/drawable-nodpi");
const base = process.env.META_API_BASE ?? "https://api.meta.ai/v1";
const key = process.env.META_API_KEY;
if (!key) throw new Error("META_API_KEY is missing in backend/.env");

const STYLE =
  "Flat vector illustration for a friendly phone app, clean shapes, soft shading, thick rounded outlines, no text, no letters, " +
  "centered character with space around it, plain warm cream background (#FFF8EC).";

const CHARACTER =
  "Peri, a cute round green pear mascot (#7CC242) with a small yellow highlight, one leaf on top of a short brown stem, " +
  "big friendly dark eyes, rosy cheeks, small arms and legs, kind and gentle look, suitable for older adults.";

export const STATES: Record<string, string> = {
  calm: "Peri standing relaxed with a warm, gentle smile, big round eyes fully open and bright, welcoming and kind.",
  listening: "Peri leaning forward attentively with wide open eyes and one hand cupped behind where an ear would be, soft green sound waves around.",
  thinking: "Peri looking through a big magnifying glass with a curious face, three small yellow dots rising above the head.",
  worried: "Peri with a gentle worried frown, head tilted, pointing at a small floating message card with a soft coral border.",
  protective: "Peri holding open a big coral umbrella over a small message card, brave and caring expression, standing in front of it.",
  calling: "Peri holding a blue phone to the side of the head, talking kindly, soft blue rings around the phone.",
  waiting: "Peri looking at a round pocket watch with patient hope, small lilac hearts floating around.",
  happy: "Peri jumping with joy, arms up, big smile, green and yellow confetti around.",
  sleeping: "Peri sleeping peacefully with closed eyes, a tiny night cap, small floating 'z' shapes, on a dark night background (#141B12) instead of cream.",
};

/** Each frame changes one small thing and keeps everything else, so frames line up when they cross-fade. */
export const FRAMES: Record<string, string> = {
  blink: "Only change: the eyes are gently closed, as in the middle of a blink. If the eyes are already closed, make the chest rise slightly as if breathing in.",
  talk: "Only change: the mouth is open in a friendly rounded shape, as if saying a word. If sleeping, the mouth makes a tiny soft 'o'.",
  swayleft: "Only change: the leaf on top bends a little to the left and the whole body tilts very slightly (about 3 degrees) to the left.",
  swayright: "Only change: the leaf on top bends a little to the right and the whole body tilts very slightly (about 3 degrees) to the right.",
};

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

function save(name: string, image: Buffer, prompt: string) {
  const file = join(here, `peri_${name}.webp`);
  writeFileSync(file, image);
  mkdirSync(drawables, { recursive: true });
  copyFileSync(file, join(drawables, `peri_${name}.webp`));
  console.log(`${name}: ${Math.round(image.length / 1024)} KB`);
  return { name, prompt };
}

const args = process.argv.slice(2);
const framesMode = args[0] === "frames";
const only = framesMode ? args.slice(1) : args;
const prompts: { name: string; prompt: string }[] = [];
const baseFile = join(here, "peri_base.webp");

if (framesMode) {
  for (const state of Object.keys(STATES)) {
    if (only.length && !only.includes(state)) continue;
    const reference = `data:image/webp;base64,${readFileSync(join(here, `peri_${state}.webp`)).toString("base64")}`;
    for (const [frame, change] of Object.entries(FRAMES)) {
      const prompt = `Exactly the same image: same character, same pose, same framing, same size and position, same background and props. ${change} ${STYLE}`;
      const image = await call("/images/edits", { model: "muse-image-1.0", prompt, images: [{ image_url: reference }], n: 1, size: "1024x1024", output_format: "webp" });
      prompts.push(save(`${state}_${frame}`, image, prompt));
    }
  }
} else if (only.length === 0 || !existsSync(baseFile)) {
  const prompt = `${STYLE} ${CHARACTER} Peri standing and smiling, full body, front view.`;
  prompts.push(save("base", await call("/images/generations", { model: "muse-image-1.0", prompt, n: 1, size: "1024x1024", output_format: "webp" }), prompt));
}

const reference = framesMode ? "" : `data:image/webp;base64,${readFileSync(baseFile).toString("base64")}`;
for (const [name, pose] of Object.entries(STATES)) {
  if (framesMode || (only.length && !only.includes(name))) continue;
  const prompt = `Same character as in the image, same style, same colours and proportions. ${pose} ${STYLE}`;
  const image = await call("/images/edits", {
    model: "muse-image-1.0",
    prompt,
    images: [{ image_url: reference }],
    n: 1,
    size: "1024x1024",
    output_format: "webp",
  });
  prompts.push(save(name, image, prompt));
}

const log = join(here, "prompts.json");
const previous = existsSync(log) ? (JSON.parse(readFileSync(log, "utf8")) as Record<string, string>) : {};
for (const p of prompts) previous[p.name] = p.prompt;
writeFileSync(log, JSON.stringify(previous, null, 2) + "\n");
console.log(`${prompts.length} images generated (about $${(prompts.length * 0.01).toFixed(2)})`);
