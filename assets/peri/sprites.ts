/**
 * Peri in layers with Muse Image (muse-image-1.0, $0.01 per image): bodies with an empty face, eyes and mouths to
 * lay over them, new moves, Mini Peri, effects and section illustrations (docs/agentes/sprites list, "Peri por partes").
 * Writes assets/peri/sprites/<name>.webp and logs each prompt in assets/peri/sprites/prompts.json.
 * It never copies anything into android/: putting the layers together is the app's job.
 *
 *   cd backend; bun ../assets/peri/sprites.ts 1               lote 1 (skips images already made)
 *   cd backend; bun ../assets/peri/sprites.ts 3 body_happy_noface face_eyes_up   only those names (made again)
 *   cd backend; bun ../assets/peri/sprites.ts 3 --soft         the missing ones, with the softer "no face" wording
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
// Muse's filter sometimes turns down "no eyes, no mouth"; this wording asks for the same thing.
const SOFT_NOFACE = "the front of the pear is plain and smooth, like a blank clay toy before its features are painted on: the eyes, eyebrows and smile are left out, and that area is the same green clay with the same soft shading as the rest of her. Keep the two rosy cheeks exactly where they are.";
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
  mini("mini_peek", "only one single Peri, no second pear: her head is moved to the right so that its right half is cut off by the right edge of the image, as if she peeks in from outside, and one small green hand grips that edge beside her."),
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
  checkup: "wears a stethoscope around her neck and holds up a small green knight's shield with a white leaf on it.",
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

// Round 6 (docs/IMAGENES_RONDA6.md): every frame of a move is an edit of the same picture, never of another frame, and
// her feet stay on the ground line of peri_calm unless the frame says how high she is, so the app only swaps pictures.
const GROUND = "Her feet rest on the ground at exactly the same height in the image as in the original picture.";
const frame = (name: string, from: string, n: number, of: number, move: string, change: string) =>
  ({ name, from, prompt: `Exactly the same character, same size in the frame, same framing and same camera. This is frame ${n} of ${of} of ${move}. Change: ${change}${/above where they usually are/.test(change) ? "" : ` ${GROUND}`} ${STYLE}` });
const seq = (group: string, from: string, move: string, changes: string[]) =>
  changes.map((c, i) => frame(`anim_${group}_${String(i + 1).padStart(2, "0")}`, from, i + 1, changes.length, move, c));

LOTES["16"] = [
  ...seq("jump", "peri_calm", "a jump animation", [
    "anticipation. Knees bent deep, body squashed wide and low like soft clay, both arms swung back, eyes eager.",
    "the push. Legs straightening fast, body stretching upward, arms swinging forward and up. Feet still on the ground.",
    "take-off. Toes just leaving the ground, body very stretched tall and thin, arms straight up. Her feet a tenth of the image above where they usually are.",
    "rising. In the air, legs tucked under her, arms up, a big happy open smile. Her feet a fifth of the image above where they usually are.",
    "the top. Body round and relaxed, arms and legs spread out like a happy star, eyes happily closed in two curves. Her feet a quarter of the image above where they usually are.",
    "falling. Legs reaching down, body a little stretched downward, arms up, her leaf still firmly attached to the top of her stem but bent upward by the wind. Her feet a sixth of the image above where they usually are.",
    "impact. Feet on the ground, body squashed very wide and low like clay, knees bent, arms out, eyes squeezed for a moment.",
    "rebound. A small bounce: body slightly stretched up, arms out for balance, feet just on the ground.",
    "settle. Standing, a tiny happy wobble to one side, arms relaxed, big smile.",
  ]),
  ...seq("drop", "peri_calm", "an entrance from the sky", [
    "falling, body stretched long and thin, arms up, her leaf still firmly attached to the top of her stem but bent upward by the wind, surprised happy face. Her feet a third of the image above where they usually are.",
    "about to touch the ground, very stretched, legs reaching down. Her feet a tenth of the image above where they usually are.",
    "impact. Squashed flat and wide like a pancake of clay, eyes squeezed shut, arms flat out to the sides.",
    "bouncing back up, stretched tall, arms up, a little in the air. Her feet a tenth of the image above where they usually are.",
    "second small landing, slightly squashed, knees bent, arms out.",
    "\"ta-da!\": standing proud, both arms open wide to the sides, huge smile.",
  ]),
];

/** One picture of a move that has no sequence of its own. */
const still = (name: string, from: string, change: string) =>
  ({ name, from, prompt: `Exactly the same character, same size in the frame, same framing and same camera. Change: ${change}${/above where they usually are/.test(change) ? "" : ` ${GROUND}`} ${STYLE}` });

LOTES["17"] = [
  ...seq("cheer", "peri_calm", "a celebration jump", [
    "crouched, both fists clenched at her chest, excited.",
    "jumping up with both fists high in the air, mouth open in a cheer. Her feet a fifth of the image above where they usually are.",
    "at the top, one fist up, the other arm pumping down, eyes closed happily. Her feet a quarter of the image above where they usually are.",
    "landed, knees bent, both fists still up, laughing.",
  ]),
  frame("anim_spin_01", "peri_calm", 1, 4, "a spinning jump", "crouched, turning a little towards the right side of the image."),
  frame("anim_spin_02", "sprites/turn_side_right", 2, 4, "a spinning jump", "in the air seen from the side, arms tucked in. Her feet a fifth of the image above where they usually are."),
  frame("anim_spin_03", "sprites/turn_back", 3, 4, "a spinning jump", "in the air seen from behind, arms out. Her feet a fifth of the image above where they usually are."),
  frame("anim_spin_04", "peri_calm", 4, 4, "a spinning jump", "landing facing the viewer, arms open, a little dizzy smile."),
  ...seq("sprout", "peri_calm", "an entrance from the ground", [
    "only her leaf and the top of her head show above the bottom edge of the image, as if coming out of the soil; the rest of her is hidden below the bottom edge.",
    "half of her body is out above the bottom edge of the image, the rest still hidden below it, eyes open, curious.",
    "jumping fully out, in the air, arms up. Her feet a fifth of the image above where they usually are.",
  ]).map((j) => ({ ...j, prompt: j.prompt.replace(` ${GROUND}`, "") })), // she comes up through the ground line
  ...seq("float", "peri_calm", "an entrance floating down", [
    "she floats down holding her small coral umbrella open above her head with one hand, legs dangling. Her feet a third of the image above where they usually are.",
    "lower, still holding the open coral umbrella above her, swaying to the left side of the image. Her feet a sixth of the image above where they usually are.",
    "landed softly on tiptoe, closing the coral umbrella, smiling.",
  ]),
  still("anim_star_01", "peri_calm", "a star jump: in the air with arms and legs spread wide, huge smile. Her feet a fifth of the image above where they usually are."),
];

LOTES["18"] = [
  ...seq("walk", "peri_calm", "a walk cycle in place, facing the viewer, arms swinging opposite to the feet", [
    "the foot on the left side of the image is forward, its heel touching the ground; the other foot behind.",
    "her weight comes down on the foot on the left side of the image, body a little lower, knee slightly bent.",
    "the foot on the right side of the image passes, lifted a little off the ground; the other foot flat.",
    "up on the foot on the left side of the image, body a little higher, the other foot lifted behind.",
    "the foot on the right side of the image is forward, its heel touching the ground; the other foot behind.",
    "her weight comes down on the foot on the right side of the image, body a little lower, knee slightly bent.",
    "the foot on the left side of the image passes, lifted a little off the ground; the other foot flat.",
    "up on the foot on the right side of the image, body a little higher, the other foot lifted behind.",
  ]),
  ...seq("run", "peri_calm", "a happy run in place, facing the viewer, arms pumping, big smile", [
    "the foot on the left side of the image pushes off the ground, the knee on the right side of the image raised high.",
    "both feet off the ground, legs apart mid-stride. Her feet a tenth of the image above where they usually are.",
    "the foot on the right side of the image lands on the ground.",
    "the foot on the right side of the image pushes off the ground, the knee on the left side of the image raised high.",
    "both feet off the ground, legs apart mid-stride the other way. Her feet a tenth of the image above where they usually are.",
    "the foot on the left side of the image lands on the ground.",
  ]),
  still("anim_skip_01", "peri_calm", "a happy skip: one knee up, the other foot on tiptoe, arms swinging, eyes closed happily."),
];

LOTES["19"] = [
  ...seq("sidewalk", "sprites/turn_side_right", "a walk cycle seen from the side, walking towards the right side of the image, arms swinging, a friendly smile", [
    "contact: the front leg stretched forward with its heel touching the ground, the back leg behind on its toes.",
    "down: her weight on the front leg, knee bent, body a little lower, the back foot lifting.",
    "passing: the back leg swings forward, lifted, passing under her body; the other foot flat.",
    "up: up on the standing leg, body a little higher, the swinging leg reaching forward.",
    "contact with the other foot: the other leg now stretched forward with its heel touching the ground.",
    "down on the other foot: weight on it, knee bent, body a little lower.",
    "passing the other way: the first leg swings forward, lifted, under her body.",
    "up on the other leg, body a little higher, the swinging leg reaching forward.",
  ]),
  ...seq("siderun", "sprites/turn_side_right", "a run seen from the side, running towards the right side of the image, arms pumping", [
    "one foot pushes off the ground behind her, the other knee high in front.",
    "both feet off the ground, legs stretched apart. Her feet a tenth of the image above where they usually are.",
    "the front foot lands on the ground, knee bent.",
    "the other foot pushes off behind her, the first knee high in front.",
    "both feet off the ground, legs stretched apart the other way. Her feet a tenth of the image above where they usually are.",
    "the other front foot lands on the ground, knee bent.",
  ]),
  still("anim_sidejump_01", "sprites/turn_side_right", "a jump forward seen from the side, towards the right side of the image, both legs stretched out in front, arms back. Her feet a fifth of the image above where they usually are."),
];

LOTES["20"] = [
  still("turn_threequarter_right", "peri_calm", "she is turned three quarters towards the right side of the image: one eye and one cheek clearly visible, the other almost hidden."),
  still("turn_threequarter_back_right", "sprites/turn_back", "she is turned three quarters away from the viewer, towards the back and the right side of the image: only the edge of one cheek visible."),
  still("turn_threequarter_back_left", "sprites/turn_back", "she is turned three quarters away from the viewer, towards the back and the left side of the image: only the edge of one cheek visible."),
  still("turn_threequarter_left", "peri_calm", "she is turned three quarters towards the left side of the image: one eye and one cheek clearly visible, the other almost hidden."),
  ...seq("nod", "peri_calm", "a nod \"yes\"", [
    "her head and upper body tilted forward a little, eyes half closed.",
    "back up straight, smiling.",
    "tilted forward again, smiling.",
  ]),
  ...seq("shake", "peri_calm", "a gentle \"no\" shake, kind and a little serious", [
    "her body turned a little towards the left side of the image.",
    "her body turned a little towards the right side of the image.",
    "her body turned a little towards the left side of the image again.",
  ]),
  still("anim_tilt_left", "peri_calm", "a curious head tilt: the top of her body leans towards the left side of the image."),
  still("anim_tilt_right", "peri_calm", "a curious head tilt: the top of her body leans towards the right side of the image."),
  still("anim_lookup_body", "peri_calm", "she leans back a little to look up at the sky, hands behind her back."),
  still("anim_lookdown_body", "peri_calm", "she leans forward a little to look down at her feet, curious."),
  still("anim_shrug", "peri_calm", "shrugging: both hands up with open palms, shoulders up, \"I don't know\" with a smile."),
];

LOTES["21"] = [
  ...seq("laugh", "peri_calm", "a belly laugh", [
    "both hands on her tummy, eyes squeezed shut, mouth wide open.",
    "leaning back laughing, eyes shut, mouth wide open.",
    "wiping a happy tear with one hand, still smiling widely.",
  ]),
  ...seq("cry", "peri_calm", "a sad moment", [
    "sad, big shiny eyes about to cry, hands together.",
    "wiping a tear with the back of one hand.",
  ]),
  still("anim_startle", "peri_calm", "startled: a small jump back, both arms up, eyes wide, mouth a small \"o\". Her feet a tenth of the image above where they usually are."),
  still("anim_determined", "peri_calm", "determined: fists on her hips, chest out, eyebrows firm, a brave little smile."),
  ...seq("shy", "peri_calm", "a shy moment", [
    "hands together in front of her, cheeks extra rosy, eyes looking down.",
    "swaying to the left side of the image, hands together, cheeks extra rosy, eyes down.",
  ]),
  still("anim_relieved", "peri_calm", "relieved: one hand on her chest, breathing out, eyes closed, soft smile."),
  ...seq("dizzy", "peri_calm", "a dizzy wobble", [
    "spiral eyes, wobbling towards the left side of the image, arms out.",
    "spiral eyes, wobbling towards the right side of the image, arms out.",
  ]),
  ...seq("sneeze", "peri_calm", "a sneeze", [
    "about to sneeze: head back, eyes squeezed, mouth open.",
    "\"achoo!\": head forward, eyes shut, her leaf shaking (still attached to her stem).",
  ]),
  still("anim_oops", "peri_calm", "tripping a little: one foot caught behind, arms flailing, surprised face, still smiling."),
];

LOTES["22"] = [
  ...seq("listen", "peri_listening", "listening", [
    "leaning towards the viewer, one hand cupped beside her head.",
    "nodding slightly, eyes attentive.",
  ]),
  ...seq("think", "peri_thinking", "thinking", [
    "tapping her chin with one finger, eyes looking up.",
    "the finger away from her chin, eyes looking to the side.",
  ]),
  ...seq("fidget", "peri_worried", "worried fidgeting", [
    "wringing her hands together.",
    "both hands at her mouth, eyebrows up.",
  ]),
  ...seq("umbrella_open", "peri_protective", "opening her umbrella to protect", [
    "the coral umbrella is closed, held down beside her.",
    "lifting the closed coral umbrella up.",
    "the coral umbrella half open above her head.",
    "the coral umbrella fully open above her, brave face.",
  ]),
  ...seq("umbrella_close", "peri_protective", "closing her umbrella when the danger is over", [
    "lowering the coral umbrella, half closed, relieved.",
    "the coral umbrella closed, held down beside her, smiling.",
  ]),
  still("anim_dial_01", "peri_calling", "tapping on her small phone with one finger, concentrating."),
  ...seq("watch", "peri_waiting", "waiting", [
    "looking at a small watch on her wrist.",
    "tapping one foot, looking up patiently.",
  ]),
];

LOTES["23"] = [
  ...seq("doze", "peri_calm", "falling asleep standing", [
    "eyes half closed, head nodding forward.",
    "eyes closed, head down, a tiny smile.",
  ]),
  ...seq("sleep", "peri_sleeping", "sleeping", [
    "breathing in: her body a little taller, peaceful.",
    "breathing out: her body a little lower, peaceful.",
  ]),
  ...seq("yawn", "peri_calm", "a yawn", [
    "mouth starting to open, eyes closing.",
    "a big yawn, both arms stretched up.",
    "rubbing one eye with a hand.",
  ]),
  ...seq("wake", "peri_sleeping", "waking up", [
    "one eye opening.",
    "stretching with both arms up.",
    "waving good morning with a big smile.",
  ]),
];

// Muse's filter turned these down with both wordings; a third one avoids the words it seems to dislike.
const ERASE = "the front of the pear is smoothed over with the same green clay and the same soft shading as the rest of her body, a plain blank front like an unfinished clay toy. Keep the two rosy cheeks exactly where they are.";
LOTES["24"] = [
  ...["calm_stepleft", "worried", "worried_stepright", "worried_steprighthalf", "happy", "happy_gesturea", "happy_gestureb", "happy_steplefthalf"]
    .map((f) => edit(`body_${f}_noface`, `peri_${f}`, ERASE)),
  ...["listen_01", "think_01", "fidget_01", "umbrella_open_04", "watch_01", "shy_01", "relieved"]
    .map((f) => edit(`anim_${f}_noface`, `sprites/anim_${f}`, NOFACE)),
];

LOTES["25"] = [
  mouth("a", "the mouth wide open in an \"ah\", tongue visible at the bottom."),
  mouth("i", "a narrow smile with the teeth just showing, as when saying \"i\"."),
  mouth("w", "the lips small, round and pushed forward, as when saying \"w\"."),
  mouth("rest", "the mouth relaxed and closed, a soft neutral line."),
  eyes("left", "both eyes keep the same round shape and size, with no eyelid line; the dark eyes and their white highlights shift clearly towards the left side of each eye socket, as if she looks at something on the left side of the image."),
  eyes("right", "both eyes keep the same round shape and size, with no eyelid line; the dark eyes and their white highlights shift clearly towards the right side of each eye socket, as if she looks at something on the right side of the image."),
  eyes("upleft", "both eyes keep the same round shape and size, with no eyelid line; the dark eyes and their white highlights shift clearly towards the top left of each eye socket, as if she looks up at something at the top left of the image."),
  eyes("upright", "both eyes keep the same round shape and size, with no eyelid line; the dark eyes and their white highlights shift clearly towards the top right of each eye socket, as if she looks up at something at the top right of the image."),
  eyes("downleft", "both eyes keep the same round shape and size, with no eyelid line; the dark eyes and their white highlights shift clearly towards the bottom left of each eye socket, as if she looks down at something at the bottom left of the image."),
  eyes("downright", "both eyes keep the same round shape and size, with no eyelid line; the dark eyes and their white highlights shift clearly towards the bottom right of each eye socket, as if she looks down at something at the bottom right of the image."),
  eyes("squint", "the eyes narrowed, a little suspicious, like a detective."),
  eyes("dizzy", "each eye is a small dark spiral."),
  eyes("cry", "big shiny eyes with tears running down below them."),
  eyes("sparkle", "the eyes have extra white sparkles in them, delighted."),
  eyes("quarter", "the eyelids are a quarter closed: between fully open and half closed, as in the start of a blink."),
];

const minihead = (name: string, change: string) =>
  ({ name, from: "sprites/mini_noface", prompt: `Same character, same style, same size. Change: ${change} No eyes, no eyebrows and no mouth; both rosy cheeks stay clearly visible. ${STYLE}` });
LOTES["26"] = [
  minihead("mini_tilt_left", "the head is tilted towards the left side of the image, curious."),
  minihead("mini_tilt_right", "the head is tilted towards the right side of the image, curious."),
  minihead("mini_nod", "the head is tilted forward a little, as in a nod."),
  minihead("mini_lookup", "the head is tilted back a little, as when looking up."),
  minihead("mini_peek_left", "only one single pear, no second one: the pear is moved to the left so that its left quarter is cut off by the left edge of the picture, as if peeking in."),
  minihead("mini_peek_right", "only one single pear, no second one: the pear is moved to the right so that its right quarter is cut off by the right edge of the picture, as if peeking in."),
  minihead("mini_peek_bottom", "the pear is lower in the image, as if rising up from below: its lower third is cut off by the bottom edge of the picture."),
  minihead("mini_umbrella_01", "a small closed coral umbrella is held up beside the head by one small green hand."),
  minihead("mini_umbrella_02", "a small coral umbrella, half open, is held above the head by one small green hand."),
  minihead("mini_umbrella_03", "a small coral umbrella, fully open, is held above the head by one small green hand."),
  minihead("mini_wave_02", "a small green hand is raised high beside the pear, saying hello."),
  minihead("mini_wave_03", "one small green hand waves beside the head, tilted the other way, palm open."),
  minihead("mini_leaf_up", "the leaf on top is blown straight up by the wind, still attached to the stem."),
  minihead("mini_sleep_02", "a small knitted night cap sits on the head, slipping to one side, sleepy."),
  minihead("mini_party", "a tiny striped party hat sits on top of the head."),
];

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

const soft = process.argv.includes("--soft");
const [lote, ...names] = process.argv.slice(2).filter((a) => a !== "--soft");
const jobs = LOTES[lote];
if (!jobs) throw new Error(`lote must be one of ${Object.keys(LOTES).join(", ")}`);
if (names[0] === "--names") {
  console.log(jobs.map((j) => j.name).join(" "));
  process.exit(0);
}
mkdirSync(out, { recursive: true });
const picked = names.length ? jobs.filter((j) => names.includes(j.name)) : jobs.filter((j) => !existsSync(join(out, `${j.name}.webp`)));
const todo = soft ? picked.map((j) => ({ ...j, prompt: j.prompt.replace(NOFACE, SOFT_NOFACE) })) : picked;

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
