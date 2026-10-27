/**
 * Peri's illustrated states with Muse Image (muse-image-1.0, $0.01 per image).
 * 1. One base Peri: the clay design the person chose (concepts/concept_clay.webp, copied to peri_base.webp).
 * 2. Each state as an edit of that base, so it stays the same character.
 * 3. Animation frames: small edits of each state's own image (a blink, talking, the leaf swaying), so the
 *    app can move between them smoothly.
 * Writes assets/peri/peri_<state>[_<frame>].webp and copies them to android/app/src/main/res/drawable-nodpi/.
 *
 *   cd backend; bun ../assets/peri/generate.ts                  all states (10 images, about $0.10)
 *   cd backend; bun ../assets/peri/generate.ts happy            only the named states, from the saved base
 *   cd backend; bun ../assets/peri/generate.ts frames           7 frames per state (63 images, about $0.63)
 *   cd backend; bun ../assets/peri/generate.ts frames calm      only the named states' frames
 *   cd backend; bun ../assets/peri/generate.ts frames --frame=blinkhalf,talkwide   only those frames
 *   cd backend; bun ../assets/peri/generate.ts extras           the app icon and the illustrations for screens
 * Then `python assets/peri/cutout.py` cuts the cream background out for the app.
 */
import { copyFileSync, existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";

const here = dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, "$1"));
const drawables = join(here, "../../android/app/src/main/res/drawable-nodpi");
const base = process.env.META_API_BASE ?? "https://api.meta.ai/v1";
const key = process.env.META_API_KEY;
if (!key) throw new Error("META_API_KEY is missing in backend/.env");

// Clay 3D, chosen by the person among eight designs (assets/peri/concepts/); the base is concepts/concept_clay.webp.
const STYLE =
  "Soft 3D clay render, rounded shapes, gentle studio lighting, tactile matte plasticine material, no text, no letters, " +
  "centered character with space around it, plain flat warm cream background (#FFF8EC), no floor shadow.";

const CHARACTER =
  "Peri, a cute round green pear mascot (#7CC242) made of clay, one leaf on top of a short brown stem, " +
  "big friendly dark eyes, rosy cheeks, small arms and legs, kind and gentle look, suitable for older adults.";

export const STATES: Record<string, string> = {
  calm: "Peri standing relaxed with a warm, gentle smile, big round eyes fully open and bright, welcoming and kind.",
  listening: "Peri listening closely and kindly: she leans slightly forward with her head tilted, both hands clasped together in front of her chest, big round dark eyes wide open and attentive, a soft warm smile, three small soft green sound-wave arcs floating beside her head. Her eyes stay big, round, solid dark brown-black with a small white shine.",
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
  blinkhalf: "Only change: the eyes are half closed, halfway through a blink, the upper eyelids covering the top half of each eye. If the eyes are already closed, keep them closed and make the smile a little softer.",
  talkwide: "Only change: the mouth is open wider in a happy rounded 'ah' shape, as if saying a vowel, with a little pink tongue showing. If sleeping, the mouth is a small relaxed oval.",
  glanceleft: "Only change: both pupils look towards the left side of the image, as if glancing at something there; head, body, mouth and everything else stay exactly the same. If the eyes are closed, keep them closed.",
  glanceright: "Only change: both pupils look towards the right side of the image, as if glancing at something there; head, body, mouth and everything else stay exactly the same. If the eyes are closed, keep them closed.",
  talko: "Only change: the mouth is a small round 'o' shape, as if saying 'oh', keeping the same eyes and expression. If sleeping, the mouth is a tiny relaxed line.",
  gesturea: "Change: Peri is talking with a friendly open mouth and raises her free hand (the one not holding anything) to shoulder height, palm open, as if kindly explaining something. Same face style, eyes, props, framing, size and position.",
  gestureb: "Change: Peri is talking with a friendly open mouth and opens her arms a little outwards at the sides, palms up, as if saying 'you see?'. If a hand holds something, it keeps holding it and only the free arm opens wider. Same face style, eyes, props, framing, size and position.",
  wink: "Only change: Peri winks: the eye on the right side of the image is closed in a happy curve, the other eye stays open, and the smile is a little bigger. Everything else stays exactly the same.",
  stepleft: "Only change: Peri lifts the foot on the left side of the image a little off the ground, that leg slightly bent at the knee, as if taking a small cheerful step; the other foot stays flat on the ground. Head, body, arms, face, props and position stay exactly the same.",
  stepright: "Only change: Peri lifts the foot on the right side of the image a little off the ground, that leg slightly bent at the knee, as if taking a small cheerful step; the other foot stays flat on the ground. Head, body, arms, face, props and position stay exactly the same.",
};

/** Illustrations made from one state, for the app icon and the screens around Peri. */
export const EXTRAS: Record<string, { from: string; prompt: string }> = {
  icon: { from: "calm", prompt: "Close-up of Peri's head only: the face large and centred, filling most of the image, warm smile, leaf on top, no arms, no legs, no props." },
  calm_wave1: { from: "calm", prompt: "Only change: Peri raises the right hand high above the shoulder, waving hello, palm open." },
  calm_wave2: { from: "calm", prompt: "Only change: Peri raises the right hand high above the shoulder, waving hello, the hand tilted the other way, mid-wave." },
  calm_idea: { from: "calm", prompt: "Only change: Peri holds one hand up with a small glowing yellow lightbulb floating above the head, a bright happy 'I have an idea' face." },
  calm_sorry: { from: "calm", prompt: "Only change: Peri shrugs gently with both hands open at the sides and a kind, apologetic little smile, eyebrows slightly raised." },
  calm_thumbsup: { from: "calm", prompt: "Only change: Peri gives a big thumbs up with one hand and a proud, warm smile." },
  family: { from: "calm", prompt: "Peri hugging a big warm coral heart with both arms, happy and caring, two small leaves floating around." },
  report: { from: "calm", prompt: "Peri holding a clipboard with a big green check mark, proud and kind expression." },
  shield: { from: "calm", prompt: "Peri holding a big green shield with a white leaf emblem in front of the body, confident and friendly smile." },
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

const args = process.argv.slice(2).filter((a) => !a.startsWith("--"));
const framesMode = args[0] === "frames";
const extrasMode = args[0] === "extras";
const only = framesMode || extrasMode ? args.slice(1) : args;
const pickedFrames = process.argv.find((a) => a.startsWith("--frame="))?.slice(8).split(",");
const prompts: { name: string; prompt: string }[] = [];
const baseFile = join(here, "peri_base.webp");

if (framesMode) {
  for (const state of Object.keys(STATES)) {
    if (only.length && !only.includes(state)) continue;
    const reference = `data:image/webp;base64,${readFileSync(join(here, `peri_${state}.webp`)).toString("base64")}`;
    for (const [frame, plain] of Object.entries(FRAMES)) {
      if (pickedFrames && !pickedFrames.includes(frame)) continue;
      // A worried Peri talks worried: the wide mouth otherwise comes out as a happy grin.
      const change = state === "worried" ? `${plain.replace("happy rounded", "concerned rounded")} Keep the worried eyebrows and the concerned look; she is not smiling.` : plain;
      // Gestures change the arms on purpose: they keep everything but the pose.
      const keep = frame.startsWith("gesture") ? "Same image: same character, same framing, same size and position, same background and props." : "Exactly the same image: same character, same pose, same framing, same size and position, same background and props.";
      const prompt = `${keep} ${change} ${STYLE}`;
      const image = await call("/images/edits", { model: "muse-image-1.0", prompt, images: [{ image_url: reference }], n: 1, size: "1024x1024", output_format: "webp" });
      prompts.push(save(`${state}_${frame}`, image, prompt));
    }
  }
} else if (extrasMode) {
  for (const [name, { from, prompt: change }] of Object.entries(EXTRAS)) {
    if (only.length && !only.includes(name)) continue;
    const reference = `data:image/webp;base64,${readFileSync(join(here, `peri_${from}.webp`)).toString("base64")}`;
    const prompt = `Same character as in the image, same style, same colours and proportions. ${change} ${STYLE}`;
    const image = await call("/images/edits", { model: "muse-image-1.0", prompt, images: [{ image_url: reference }], n: 1, size: "1024x1024", output_format: "webp" });
    prompts.push(save(name, image, prompt));
  }
} else if (only.length === 0 || !existsSync(baseFile)) {
  const prompt = `${STYLE} ${CHARACTER} Peri standing and smiling, full body, front view.`;
  prompts.push(save("base", await call("/images/generations", { model: "muse-image-1.0", prompt, n: 1, size: "1024x1024", output_format: "webp" }), prompt));
}

const reference = framesMode || extrasMode ? "" : `data:image/webp;base64,${readFileSync(baseFile).toString("base64")}`;
for (const [name, pose] of Object.entries(STATES)) {
  if (framesMode || extrasMode || (only.length && !only.includes(name))) continue;
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
