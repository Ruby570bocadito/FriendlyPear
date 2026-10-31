/**
 * The heist video's character frames, made with Muse Image (muse-image-1.0, $0.01 each), so the people move like
 * Peri does: new poses (the angry crook with his moustache, Rosa on the phone, Ana calling and leaving, the family's
 * pears) and, for every pose that speaks, a talking frame and a blink, each an edit of the pose itself so only the
 * mouth or the eyes change. Run from backend/, whose .env holds the API key:
 *
 *   cd backend; bun ../video/scripts/heist_frames.ts poses      the new poses
 *   cd backend; bun ../video/scripts/heist_frames.ts frames     talking and blinking frames of every pose
 *   cd backend; bun ../video/scripts/heist_frames.ts <name>...  just those
 *
 * Pictures go to video/public/heist/; every prompt is kept in its prompts.json.
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const pub = join(here, "..", "public");
const out = join(pub, "heist");
const log = join(out, "prompts.json");
const base = process.env.META_API_BASE ?? "https://api.meta.ai/v1";
const key = process.env.META_API_KEY;
if (!key) throw new Error("META_API_KEY is missing (run from backend/, whose .env holds it)");

const SAME = "Same character, same pose, same framing, same size and position, same background, same lighting, same clay style.";
const PERI = "Same Peri: same face, big round dark eyes, leaf, colours and clay style. Cream background.";

/** New poses: an edit of an existing picture (a path under video/public, or assets/peri for Peri). */
const POSES: Record<string, { from: string; prompt: string }> = {
  crook_angry: {
    from: "short/crook_desk.png",
    prompt:
      "Same man, same desk, same room, same framing and the same soft 3D clay style, keeping his thin curly black moustache, his black beanie and his " +
      "black and white striped jumper. Only change: he looks very cross and grumpy, frowning hard with his cheeks a little red, two small cartoon puffs of steam " +
      "rising from his ears, and his arms crossed.",
  },
  rosa_call: {
    from: "submission/rosa_morning.png",
    prompt:
      "Same elderly woman, same armchair, same room, same framing and the same soft 3D clay style. Only change: she holds her phone to her ear with " +
      "one hand, listening to someone with a worried face; her other hand rests on her lap, no cup.",
  },
  ana_call: {
    from: "submission/ana_city.png",
    prompt:
      "Same young woman, same flat at dusk, same framing and the same soft 3D clay style. Only change: she holds her phone to her ear, listening " +
      "with a surprised, alarmed face.",
  },
  ana_go: {
    from: "submission/ana_city.png",
    prompt:
      "Same young woman, same flat at dusk, same framing and the same soft 3D clay style. Only change: she is pulling on a coat in a hurry with " +
      "her phone held to her ear, worried but determined.",
  },
  // Rosa somewhere new in every round, and Ana with her mouth closed between words.
  rosa_reading: {
    from: "submission/rosa_morning.png",
    prompt:
      "Same elderly woman, same armchair, same room, same framing and the same soft 3D clay style. Only change: she holds her phone a little closer and reads a message " +
      "on it with a puzzled, slightly worried face, her eyebrows raised and her mouth closed; the cup rests on her lap.",
  },
  rosa_yes: {
    from: "submission/rosa_morning.png",
    prompt:
      "Same elderly woman, same armchair, same room, same framing and the same soft 3D clay style. Only change: she looks at her phone hopeful and relieved, her free " +
      "hand resting on her chest, her mouth closed in a small grateful smile; no cup.",
  },
  rosa_qr: {
    from: "submission/rosa_morning.png",
    prompt:
      "Same elderly woman with the same grey hair bun, the same lilac knitted cardigan and the same soft 3D clay style. She now stands outdoors on a sunny little town " +
      "street beside a parking meter with a small square sticker on it, holding her phone up in front of it as if about to scan it, with a curious face and her mouth " +
      "closed. Upper body, similar framing.",
  },
  rosa_kitchen: {
    from: "heist/rosa_call.png",
    prompt:
      "Same elderly woman with the same grey hair bun, the same lilac knitted cardigan and the same soft 3D clay style. She now sits at a small cosy kitchen table with " +
      "a cup of tea and a notepad, holding her phone to her ear and listening trustingly, her mouth closed. Similar framing.",
  },
  rosa_garden_call: {
    from: "short/rosa_garden.png",
    prompt:
      "Same elderly woman, same garden, same wicker chair, same framing and the same soft 3D clay style. Only change: she has put her knitting down on her lap and holds " +
      "her phone to her ear with one hand, listening to someone with a polite, slightly unsure face, her mouth closed.",
  },
  rosa_fallen: {
    from: "submission/rosa_morning.png",
    prompt:
      "Same elderly woman, same living room, same lilac cardigan and the same soft 3D clay style. Only change: she has slipped and now sits on the rug beside her " +
      "armchair, one hand on her hip, holding her phone in the other hand to ask for help, calm but a little shaken, her mouth closed.",
  },
  ana_call_rest: {
    from: "heist/ana_call.png",
    prompt: `Exactly the same image. ${SAME} Only change: her mouth is closed, lips gently together, with the same surprised, concerned look.`,
  },
  ana_go_rest: {
    from: "heist/ana_go.png",
    prompt: `Exactly the same image. ${SAME} Only change: her mouth is closed, lips gently together, worried but calm and determined.`,
  },
  pear_rosa: { from: "../../assets/peri/peri_calm.webp", prompt: `Only change: Peri wears a small red bow on top of her head, next to her leaf, with a warm smile. ${PERI}` },
  pear_ana: { from: "../../assets/peri/peri_calm.webp", prompt: `Only change: Peri wears small round red glasses and a red bow tie, with a kind, attentive smile. ${PERI}` },
  pear_pablo: { from: "../../assets/peri/peri_calm.webp", prompt: `Only change: Peri wears a cosy blue woolly beanie, with a cheerful smile. ${PERI}` },
};

/** Every pose that speaks gets a talking frame; the ones on screen for long also blink. */
const TALK = [
  "short/crook_plan", "short/crook_desk", "short/crook_phone", "short/crook_stickers", "short/crook_shock", "heist/crook_angry", "short/crook_quit",
  "submission/rosa_morning", "heist/rosa_call", "heist/ana_call", "short/ana_no", "heist/ana_go",
  "heist/rosa_yes", "heist/rosa_kitchen", "heist/rosa_garden_call", "heist/rosa_fallen", "heist/ana_call_rest", "heist/ana_go_rest",
];
const BLINK = [
  "short/crook_plan", "short/crook_desk", "short/crook_phone", "short/crook_stickers", "submission/rosa_morning", "heist/rosa_call",
  "heist/rosa_reading", "heist/rosa_yes", "heist/rosa_qr", "heist/rosa_kitchen", "heist/rosa_garden_call", "heist/rosa_fallen", "heist/ana_call_rest", "heist/ana_go_rest",
];
const moustache = (from: string) => (from.includes("crook") ? " He keeps his thin curly black moustache exactly as it is." : "");
const FRAMES: Record<string, { from: string; prompt: string }> = {};
for (const from of TALK) {
  FRAMES[`${from.split("/")[1]}_talk`] = {
    from: `${from}.png`,
    prompt: `Exactly the same image. ${SAME} Only change: the mouth is open in a natural talking shape, as if in the middle of saying a word.${moustache(from)}`,
  };
}
for (const from of BLINK) {
  FRAMES[`${from.split("/")[1]}_blink`] = {
    from: `${from}.png`,
    prompt: `Exactly the same image. ${SAME} Only change: the eyes are gently closed, as in the middle of a blink; the mouth stays exactly as it is.${moustache(from)}`,
  };
}

/** A PNG's width and height, from its header. */
const dimensions = (file: string) => {
  const b = readFileSync(file);
  return b.toString("ascii", 1, 4) === "PNG" ? [b.readUInt32BE(16), b.readUInt32BE(20)] : [1, 1];
};

async function edit(from: string, prompt: string): Promise<{ image: Buffer; size: string }> {
  const file = join(pub, from);
  const [w, h] = dimensions(file);
  const sizes = h > w * 1.2 ? ["1024x1536", "1024x1024"] : ["1024x1024"];
  const mime = from.endsWith(".webp") ? "webp" : "png";
  for (const size of sizes) {
    const res = await fetch(`${base}/images/edits`, {
      method: "POST",
      headers: { authorization: `Bearer ${key}`, "content-type": "application/json" },
      body: JSON.stringify({ model: "muse-image-1.0", prompt, images: [{ image_url: `data:image/${mime};base64,${readFileSync(file).toString("base64")}` }], n: 1, size, output_format: "png" }),
    });
    if (!res.ok) {
      const text = await res.text();
      // A 400 for the tall size means the model only makes squares; a content-policy refusal is final.
      if (res.status === 400 && size !== "1024x1024" && !text.includes("content_policy")) continue;
      throw new Error(`images/edits answered ${res.status}: ${text.slice(0, 300)}`);
    }
    const b64 = ((await res.json()) as { data: { b64_json?: string }[] }).data?.[0]?.b64_json;
    if (!b64) throw new Error("images/edits returned no image");
    return { image: Buffer.from(b64, "base64"), size };
  }
  throw new Error("no size worked");
}

const args = process.argv.slice(2);
const chosen = args[0] === "poses" ? POSES : args[0] === "frames" ? FRAMES : Object.fromEntries(Object.entries({ ...POSES, ...FRAMES }).filter(([n]) => args.includes(n)));
mkdirSync(out, { recursive: true });
const prompts: Record<string, { from: string; prompt: string; size: string }> = existsSync(log) ? JSON.parse(readFileSync(log, "utf8")) : {};
for (const [name, { from, prompt }] of Object.entries(chosen)) {
  if (args[0] === "poses" || args[0] === "frames") {
    if (existsSync(join(out, `${name}.png`))) continue;
  }
  const { image, size } = await edit(from, prompt);
  writeFileSync(join(out, `${name}.png`), image);
  prompts[name] = { from, prompt, size };
  writeFileSync(log, JSON.stringify(prompts, null, 2));
  console.log(`${name}: ${size}, ${Math.round(image.length / 1024)} KB`);
}
