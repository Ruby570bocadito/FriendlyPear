/**
 * The 3-minute submission video's new pictures, made with Muse Image (muse-image-1.0, $0.01 each): Peri's scenes as
 * edits of peri_calm (so she stays the same character) and places with people as new pictures in her clay style.
 * Run with META_API_KEY set (backend/.env holds one):
 *
 *   cd backend; bun ../video/scripts/submission_images.ts            all of them
 *   cd backend; bun ../video/scripts/submission_images.ts lonely     just one
 *
 * Every prompt is kept in video/public/submission/prompts.json.
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const out = join(here, "..", "public", "submission");
const log = join(out, "prompts.json");
const base = process.env.META_API_BASE ?? "https://api.meta.ai/v1";
const key = process.env.META_API_KEY;
if (!key) throw new Error("META_API_KEY is missing");

const STYLE =
  "Soft 3D clay diorama, matte plasticine texture with visible fingerprints, rounded chunky shapes, warm pastel colours with fresh greens, " +
  "soft studio light from the top left, gentle shadows, shallow depth of field. No text, no letters, no numbers, no logos.";

/** Peri's scenes: edits of her calm pose, on her cream background (cut out later with Adobe Photoshop). */
const EDITS: Record<string, string> = {
  conductor:
    "Change: Peri is a cheerful orchestra conductor holding a small wooden baton up in one hand. In front of her, on the ground, stand six small round clay helper robots " +
    "in different pastel colours (coral, sky blue, lilac, sun yellow, mint, peach), each holding one different item: a magnifying glass, a big ear trumpet, a paintbrush, " +
    "a notebook, a red-and-white target, a glowing light bulb. Same Peri: same face, big round dark eyes, leaf, colours and clay style. Cream background.",
  builder:
    "Change: Peri wears a small yellow hard hat and holds a little clay wrench, standing proudly next to a half-built big clay smartphone held by small wooden scaffolding, " +
    "a few clay bolts on the ground. Same Peri: same face, big round dark eyes, leaf, colours and clay style. Cream background.",
  padlock:
    "Change: Peri holds a big shiny golden clay padlock in both hands in front of her chest, the padlock has a small green leaf on its front, and Peri has a firm, kind, " +
    "confident smile. Same Peri: same face, big round dark eyes, leaf, colours and clay style. Cream background.",
  bridge:
    "Change: Peri stands in the middle holding hands with two smiling clay people: on the left an elderly woman with silver hair in a bun and a lilac cardigan, on the right " +
    "her adult daughter with brown hair and a sky blue jumper. Small coral hearts float above them. All three the same height scale as a family photo, Peri a little smaller. " +
    "Same Peri: same face, big round dark eyes, leaf, colours and clay style. Cream background.",
};

/** Rosa and Ana in new scenes, as edits of a picture they're already in, so they stay the same people. */
const RETAKES: Record<string, { from: string; prompt: string }> = {
  rosa_morning: {
    from: "lonely.png",
    prompt:
      "Same elderly woman, same cosy living room, same armchair and the same soft 3D clay style. Change: it is a bright sunny morning now, warm golden light " +
      "pours through the window, and she smiles, relaxed and happy, holding a small cup of tea in one hand and her phone in the other. No text.",
  },
  ana_city: {
    from: "bridge.png",
    prompt:
      "Keep only the young woman on the right (long brown hair, sky blue knitted jumper), in the same soft 3D clay style. New wide scene: she stands by a big " +
      "window in a small city flat at dusk, tall buildings with warm lit windows behind her, holding her phone and looking at it with a caring, slightly " +
      "worried face. No pear, no elderly woman, no text.",
  },
};

/** Places and people, without Peri, full frame. */
const SCENES: Record<string, string> = {
  lonely:
    "Wide shot. An elderly clay woman with silver hair in a bun and a lilac cardigan sits alone on a soft armchair in a cosy living room in the evening, " +
    "a standing lamp glowing warm, she looks worried at a phone in her hands whose screen casts a cold blue light on her face. " + STYLE,
  scammer:
    "Wide shot. A dark, moody clay room: a shadowy faceless figure in a hoodie sits at a desk with many phones and a laptop glowing purple, " +
    "a big clay fishing hook hangs from the ceiling. Mysterious but not scary, suitable for all ages. " + STYLE,
  parking:
    "Wide shot. A sunny clay street in a small Spanish town: a parking meter in the foreground on the pavement, with a small blue sticker stuck on it " +
    "showing an abstract black and white square pattern like a QR code, parked clay cars, balconies with flower pots, a pale blue sky. No people. " + STYLE,
  meadow:
    "Wide landscape. A sunny clay meadow on rolling green hills with small pear trees, little white clay houses with terracotta roofs far away, " +
    "fluffy clay clouds in a soft blue sky, flowers in the foreground, an empty grassy hill in the middle. No people, no animals. " + STYLE,
};

async function call(path: string, body: Record<string, unknown>): Promise<{ image: Buffer; size: string }> {
  const sizes = path === "/images/generations" ? ["1536x1024", "1024x1024"] : ["1024x1024"];
  for (const size of sizes) {
    const res = await fetch(`${base}${path}`, {
      method: "POST",
      headers: { authorization: `Bearer ${key}`, "content-type": "application/json" },
      body: JSON.stringify({ ...body, model: "muse-image-1.0", n: 1, size, output_format: "png" }),
    });
    if (res.status === 400 && size !== "1024x1024") continue;
    if (!res.ok) throw new Error(`${path} answered ${res.status}: ${(await res.text()).slice(0, 300)}`);
    const b64 = ((await res.json()) as { data: { b64_json?: string }[] }).data?.[0]?.b64_json;
    if (!b64) throw new Error(`${path} returned no image`);
    return { image: Buffer.from(b64, "base64"), size };
  }
  throw new Error("no size worked");
}

const wanted = process.argv.slice(2);
mkdirSync(out, { recursive: true });
const prompts: Record<string, { prompt: string; size: string }> = existsSync(log) ? JSON.parse(readFileSync(log, "utf8")) : {};
const calm = `data:image/webp;base64,${readFileSync(join(here, "..", "..", "assets", "peri", "peri_calm.webp")).toString("base64")}`;

for (const [name, prompt] of Object.entries(EDITS)) {
  if (wanted.length && !wanted.includes(name)) continue;
  const { image, size } = await call("/images/edits", { prompt, images: [{ image_url: calm }] });
  writeFileSync(join(out, `${name}.png`), image);
  prompts[name] = { prompt, size };
  console.log(`${name}: ${size}, ${Math.round(image.length / 1024)} KB`);
}
for (const [name, { from, prompt }] of Object.entries(RETAKES)) {
  if (wanted.length && !wanted.includes(name)) continue;
  const reference = `data:image/png;base64,${readFileSync(join(out, from)).toString("base64")}`;
  const { image, size } = await call("/images/edits", { prompt, images: [{ image_url: reference }] });
  writeFileSync(join(out, `${name}.png`), image);
  prompts[name] = { prompt, size };
  console.log(`${name}: ${size}, ${Math.round(image.length / 1024)} KB`);
}
for (const [name, prompt] of Object.entries(SCENES)) {
  if (wanted.length && !wanted.includes(name)) continue;
  const { image, size } = await call("/images/generations", { prompt });
  writeFileSync(join(out, `${name}.png`), image);
  prompts[name] = { prompt, size };
  console.log(`${name}: ${size}, ${Math.round(image.length / 1024)} KB`);
}
writeFileSync(log, JSON.stringify(prompts, null, 2));
