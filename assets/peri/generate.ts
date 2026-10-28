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
 *   cd backend; bun ../assets/peri/generate.ts traps            "how the trap worked": 3 panels for 6 kinds of scam (18 images)
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
  calm_laugh: { from: "calm", prompt: "Only change: Peri laughs happily at a joke, her eyes squeezed shut into two happy upward curves, mouth open in a big joyful laugh, one hand on her tummy." },
  calm_love: { from: "calm", prompt: "Only change: Peri holds a small soft coral heart in both hands close to her chest, with a warm, tender smile and slightly rosier cheeks." },
  calm_dizzy: { from: "calm", prompt: "Only change: Peri is playfully dizzy, her eyes are two small dark spirals, a wobbly happy smile, three tiny yellow stars circling above her leaf, arms loose at her sides." },
  calm_yawn: { from: "calm", prompt: "Only change: Peri yawns sleepily, one hand politely covering her open mouth, eyes half closed, the other arm stretching up a little." },
  calm_letter: { from: "calm", prompt: "Only change: Peri holds an open white paper letter in both hands in front of her body and reads it with a friendly, attentive smile." },
  family: { from: "calm", prompt: "Peri hugging a big warm coral heart with both arms, happy and caring, two small leaves floating around." },
  report: { from: "calm", prompt: "Peri holding a clipboard with a big green check mark, proud and kind expression." },
  guardian: { from: "calm", prompt: "Peri holding a small phone in both hands in front of her, its screen showing a soft coral heart, looking up warmly and attentively, as if a loved one just asked her something. Same big round dark eyes." },
  guardian_alert: { from: "calm", prompt: "Peri holding a small phone in both hands in front of her, its screen showing a bold amber exclamation mark, with a serious but caring, alert expression and slightly raised eyebrows, ready to help a loved one. Same big round dark eyes." },
  shield: { from: "calm", prompt: "Peri holding a big green shield with a white leaf emblem in front of the body, confident and friendly smile." },
  // Round 2: more of her personality (docs/PERI_PERSONAJE.md, "Peri viva").
  calm_hero: { from: "calm", prompt: "Only change: Peri wears a small coral superhero cape flowing behind her and stands tall with both fists on her hips in a proud heroic pose, chin up, confident and kind smile." },
  calm_medal: { from: "calm", prompt: "Only change: Peri proudly holds up a shiny round gold medal on a green ribbon with both hands in front of her chest, a big proud smile." },
  calm_peekaboo: { from: "calm", prompt: "Only change: Peri plays peekaboo: both hands cover her eyes, and she peeks through her fingers with one eye, a big playful open-mouthed smile." },
  calm_clap: { from: "calm", prompt: "Only change: Peri claps her hands together in front of her chest, her eyes happily squeezed into two upward curves, a big cheerful open smile." },
  calm_detective: { from: "calm", prompt: "Only change: Peri wears a small brown detective hat and holds a big magnifying glass up in front of one eye, which looks big through the lens, a curious smile." },
  calm_surprised: { from: "calm", prompt: "Only change: Peri looks surprised and delighted: eyes wide open, eyebrows raised, mouth in a small round 'oh', both hands up beside her cheeks." },
  calm_party: { from: "calm", prompt: "Only change: Peri wears a small striped party hat and blows a curly party horn, small paper confetti in green, yellow and coral floating around her." },
  calm_glasses: { from: "calm", prompt: "Only change: Peri wears small round reading glasses and holds a little open notebook in both hands, reading it with a gentle smile." },
  calm_scarf: { from: "calm", prompt: "Only change: Peri wears a cosy knitted scarf with coral and cream stripes around her neck, rosy cheeks, a happy warm smile." },
  calm_bye: { from: "calm", prompt: "Only change: Peri waves goodbye with one hand held high and the other hand on her chest, a warm smile, a tiny coral heart floating near the waving hand." },
};

/**
 * "How the trap worked": three panels per kind of scam, each an edit of the calm Peri so she stays the same
 * character. The words go under each panel in the app (both languages), never in the picture.
 */
export const TRAPS: Record<string, string> = {
  trap_parcel_1: "Peri holds a smartphone in both hands; on its screen a small cardboard parcel with a red exclamation mark. She looks a little puzzled.",
  trap_parcel_2: "A sneaky clay fishing hook, baited with a tiny cardboard parcel, comes out of a smartphone screen trying to hook a bank card. Peri watches with wide, surprised eyes.",
  trap_parcel_3: "Peri smiles under a big coral umbrella, holding a real cardboard parcel safely in one arm and giving a thumbs up with the other hand.",
  trap_link_1: "Peri holds a smartphone; on its screen a small bank building with a red warning sign. She looks worried.",
  trap_link_2: "A sneaky clay fishing hook, baited with a small golden key, comes out of a smartphone screen trying to catch a padlock. Peri watches with wide, surprised eyes.",
  trap_link_3: "Peri points proudly at a big closed padlock with a green check mark beside her, confident and kind.",
  trap_code_1: "Peri holds a phone to her ear and looks unsure, while a shadowy grey silhouette whispers into a second phone on the other side of the image.",
  trap_code_2: "A small golden key flies out of Peri's open hand towards a shadowy grey hand reaching from the edge of the image. Peri looks alarmed.",
  trap_code_3: "Peri holds the small golden key safely against her chest and kindly shakes her head, with a big green shield standing beside her.",
  trap_family_1: "Peri holds a smartphone showing a coral heart and a question mark, looking moved and a little worried.",
  trap_family_2: "A shadowy grey figure hides behind a cardboard mask with a drawn smiley face and holds out an empty open hand, asking for coins. Peri watches, surprised.",
  trap_family_3: "Peri holds a phone to her ear, smiling warmly, with a soft coral heart floating beside her.",
  trap_remote_1: "Peri looks at a smartphone whose screen shows a big red warning triangle and a cartoon bug. She looks alarmed.",
  trap_remote_2: "A shadowy grey hand reaches out of a smartphone screen holding a small remote control. Peri leans back, surprised.",
  trap_remote_3: "Peri calmly puts a smartphone face down on a small table and smiles, with a big green shield beside her.",
  trap_prize_1: "Peri holds a smartphone with a shiny golden trophy and golden coins bursting out of the screen. Her eyes are wide with excitement.",
  trap_prize_2: "Golden coins turn out to be painted cardboard, peeling at the edges, while a shadowy grey hand behind them takes away real coins. Peri looks surprised.",
  trap_prize_3: "Peri kindly shakes her head with a calm smile, holding a small coin purse safely closed in both hands.",
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
const trapsMode = args[0] === "traps";
const only = framesMode || extrasMode || trapsMode ? args.slice(1) : args;
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
} else if (trapsMode) {
  const reference = `data:image/webp;base64,${readFileSync(join(here, "peri_calm.webp")).toString("base64")}`;
  for (const [name, scene] of Object.entries(TRAPS)) {
    if (only.length && !only.includes(name)) continue;
    // A small scene: the cream background stays, so each panel sits in its own card.
    const prompt = `Same character as in the image, same style, same colours and proportions. A simple, friendly comic panel: ${scene} Soft 3D clay render, rounded shapes, gentle studio lighting, tactile matte plasticine material, no text, no letters, no numbers, plain flat warm cream background (#FFF8EC).`;
    const image = await call("/images/edits", { model: "muse-image-1.0", prompt, images: [{ image_url: reference }], n: 1, size: "1024x1024", output_format: "webp" });
    prompts.push(save(name, image, prompt));
  }
} else if (only.length === 0 || !existsSync(baseFile)) {
  const prompt = `${STYLE} ${CHARACTER} Peri standing and smiling, full body, front view.`;
  prompts.push(save("base", await call("/images/generations", { model: "muse-image-1.0", prompt, n: 1, size: "1024x1024", output_format: "webp" }), prompt));
}

const reference = framesMode || extrasMode || trapsMode ? "" : `data:image/webp;base64,${readFileSync(baseFile).toString("base64")}`;
for (const [name, pose] of Object.entries(STATES)) {
  if (framesMode || extrasMode || trapsMode || (only.length && !only.includes(name))) continue;
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
