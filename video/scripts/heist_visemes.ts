/**
 * Mouth shapes, half blinks and glances for the heist video's people, made with Muse Image (muse-image-1.0, $0.01
 * each), so their faces move smoothly instead of just opening and closing: for each pose, the lips just parted, half
 * open, wide open, a round "o" and a talking grin, plus eye frames. Every frame is an edit of the pose itself; later
 * heist_stabilize.py keeps only the mouth or the eyes from it. Run from backend/, whose .env holds the API key:
 *
 *   cd backend; bun ../video/scripts/heist_visemes.ts crook1     one group (crook1, crook2, crook3, people)
 *   cd backend; bun ../video/scripts/heist_visemes.ts all        every group, one after another
 *
 * Frames go to video/public/heist/v/<pose>_<shape>.png, each with its prompt in <pose>_<shape>.json.
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const pub = join(here, "..", "public");
const out = join(pub, "heist", "v");
const base = process.env.META_API_BASE ?? "https://api.meta.ai/v1";
const key = process.env.META_API_KEY;
if (!key) throw new Error("META_API_KEY is missing (run from backend/, whose .env holds it)");

const SAME = "Exactly the same image: same character, same pose, same framing, same size and position, same background, same lighting, same clay style.";
const MOUTH: Record<string, string> = {
  open: "Only change: the mouth is open in a natural talking shape, as if in the middle of saying a word.",
  slight: "Only change: the lips are just slightly parted, a small opening, as at the very start of a word.",
  half: "Only change: the mouth is half open in a relaxed talking shape, as if softly saying 'ah'.",
  wide: "Only change: the mouth is wide open, as if exclaiming a word loudly.",
  round: "Only change: the lips are pushed into a small round 'o' shape, as if saying 'oh' or 'who'.",
  smile: "Only change: the mouth is open in a wide talking grin that shows the teeth, as if saying 'ee'.",
};
const EYES: Record<string, string> = {
  blink: "Only change: the eyes are gently closed, as in the middle of a blink; the mouth stays exactly as it is.",
  blinkhalf: "Only change: the eyes are half closed, halfway through a blink; the mouth stays exactly as it is.",
  lookleft: "Only change: both pupils look towards the left side of the image, as if glancing at something there; the head and mouth stay exactly as they are.",
  lookright: "Only change: both pupils look towards the right side of the image, as if glancing at something there; the head and mouth stay exactly as they are.",
};

/** Each pose (a path under video/public, without .png) and the frames it needs. */
const ALL_MOUTHS = ["slight", "half", "wide", "round", "smile"];
const GROUPS: Record<string, { pose: string; shapes: string[] }[]> = {
  crook1: [
    { pose: "short/crook_plan", shapes: [...ALL_MOUTHS, "blinkhalf", "lookleft", "lookright"] },
    { pose: "short/crook_desk", shapes: [...ALL_MOUTHS, "blinkhalf", "lookleft", "lookright"] },
  ],
  crook2: [
    { pose: "short/crook_phone", shapes: [...ALL_MOUTHS, "blinkhalf", "lookleft", "lookright"] },
    { pose: "short/crook_stickers", shapes: [...ALL_MOUTHS, "blinkhalf", "lookleft", "lookright"] },
  ],
  crook3: [
    { pose: "short/crook_shock", shapes: [...ALL_MOUTHS, "blink", "blinkhalf"] },
    { pose: "heist/crook_angry", shapes: [...ALL_MOUTHS, "blink", "blinkhalf"] },
    { pose: "short/crook_quit", shapes: [...ALL_MOUTHS, "blink", "blinkhalf"] },
  ],
  people: [
    { pose: "submission/rosa_morning", shapes: ["slight", "half", "wide", "round", "blinkhalf"] },
    { pose: "heist/rosa_call", shapes: ["slight", "half", "wide", "round", "blinkhalf"] },
    { pose: "heist/ana_call", shapes: ["slight", "half", "wide", "round", "blink", "blinkhalf"] },
    { pose: "short/ana_no", shapes: ["slight", "half", "wide", "round", "blink", "blinkhalf"] },
    { pose: "heist/ana_go", shapes: ["slight", "half", "wide", "round", "blink", "blinkhalf"] },
  ],
  // Rosa somewhere new in every round (heist_frames.ts), and Ana with her mouth closed between words: no wide mouths,
  // they never shout.
  places: [
    { pose: "heist/rosa_reading", shapes: ["blinkhalf", "lookleft", "lookright"] },
    { pose: "heist/rosa_yes", shapes: ["slight", "half", "round", "blinkhalf"] },
    { pose: "heist/rosa_kitchen", shapes: ["blinkhalf", "lookleft", "lookright"] },
    { pose: "heist/rosa_qr", shapes: ["blinkhalf", "lookleft", "lookright"] },
    { pose: "heist/rosa_garden_call", shapes: ["slight", "half", "round", "blinkhalf", "lookleft", "lookright"] },
    { pose: "heist/rosa_fallen", shapes: ["slight", "half", "round", "blinkhalf"] },
    { pose: "heist/ana_call_rest", shapes: ["slight", "half", "round", "blinkhalf"] },
    { pose: "heist/ana_go_rest", shapes: ["slight", "half", "round", "blinkhalf"] },
  ],
  // "Peri's diary" (diary_images.ts): Peri writing at her desk, Rosa fallen at night, and Rosa's relief after each trick.
  diary: [
    { pose: "diary/peri_desk", shapes: ["slight", "half", "open", "round", "blink", "blinkhalf", "lookleft", "lookright"] },
    { pose: "diary/rosa_fallen_night", shapes: ["slight", "half", "open", "round", "blink", "blinkhalf"] },
    { pose: "diary/rosa_kitchen_hangup", shapes: ["blink"] },
    { pose: "diary/rosa_qr_away", shapes: ["blink"] },
    { pose: "diary/rosa_garden_smile", shapes: ["blink"] },
  ],
};

const dimensions = (file: string) => {
  const b = readFileSync(file);
  return b.toString("ascii", 1, 4) === "PNG" ? [b.readUInt32BE(16), b.readUInt32BE(20)] : [1, 1];
};

async function edit(from: string, prompt: string): Promise<{ image: Buffer; size: string }> {
  const file = join(pub, `${from}.png`);
  const [w, h] = dimensions(file);
  for (const size of h > w * 1.2 ? ["1024x1536", "1024x1024"] : w > h * 1.2 ? ["1792x1024", "1024x1024"] : ["1024x1024"]) {
    for (let attempt = 0; attempt < 3; attempt++) {
      const res = await fetch(`${base}/images/edits`, {
        method: "POST",
        headers: { authorization: `Bearer ${key}`, "content-type": "application/json" },
        body: JSON.stringify({ model: "muse-image-1.0", prompt, images: [{ image_url: `data:image/png;base64,${readFileSync(file).toString("base64")}` }], n: 1, size, output_format: "png" }),
      });
      if (res.ok) {
        const b64 = ((await res.json()) as { data: { b64_json?: string }[] }).data?.[0]?.b64_json;
        if (b64) return { image: Buffer.from(b64, "base64"), size };
      } else {
        const text = await res.text();
        // A 400 for the tall size means the model only makes squares; a content-policy refusal is final.
        if (res.status === 400 && size !== "1024x1024" && !text.includes("content_policy")) break;
        if (res.status !== 429 && res.status < 500) throw new Error(`images/edits answered ${res.status}: ${text.slice(0, 300)}`);
      }
      await new Promise((r) => setTimeout(r, 4000 * (attempt + 1)));
    }
  }
  throw new Error(`no image for ${from}`);
}

const wanted = process.argv[2] === "all" ? Object.keys(GROUPS) : [process.argv[2]];
mkdirSync(out, { recursive: true });
for (const group of wanted) {
  for (const { pose, shapes } of GROUPS[group]) {
    const name = pose.split("/")[1];
    for (const shape of shapes) {
      const file = join(out, `${name}_${shape}.png`);
      if (existsSync(file)) continue;
      const moustache = name.startsWith("crook") ? " He keeps his thin curly black moustache exactly as it is." : "";
      const prompt = `${SAME} ${MOUTH[shape] ?? EYES[shape]}${moustache}`;
      const { image, size } = await edit(pose, prompt);
      writeFileSync(file, image);
      writeFileSync(join(out, `${name}_${shape}.json`), JSON.stringify({ from: `${pose}.png`, prompt, size }, null, 2));
      console.log(`${name}_${shape}: ${size}`);
    }
  }
}
