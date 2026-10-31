// Pictures for the website, made with Muse Image (muse-image-1.0) as edits of Peri's base so she stays the same
// character. Prompts live here; the images go to web/public/img/.
//   META_API_KEY=... node web/scripts/generate-images.mjs [name ...]
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, "../..");
const out = join(here, "../public/img");
const key = process.env.META_API_KEY;
if (!key) throw new Error("META_API_KEY is missing");

const STYLE =
  "Soft 3D clay render, rounded shapes, gentle studio lighting from the top left, tactile matte plasticine material, " +
  "no text, no letters, no numbers, plain flat warm cream background (#FFF8EC), soft contact shadow only.";
const PERI =
  "Peri, the same cute round green pear mascot made of clay as in the reference: one leaf on a short brown stem, " +
  "big round dark eyes with a white shine, rosy cheeks, small arms and legs.";

export const IMAGES = {
  grandma: ["1536x1024", `${PERI} Wide cozy scene: a kind elderly woman made of clay, grey hair in a bun, round glasses, cardigan, sits in a soft green armchair holding a smartphone; Peri sits on the armrest beside her, pointing gently at the phone screen and smiling. A small potted plant and a warm lamp.`],
  check: ["1024x1024", `${PERI} Peri looks through a big magnifying glass at a clay envelope with a small fishing hook attached to it, curious and focused.`],
  family: ["1536x1024", `${PERI} Wide scene: Peri on the left holds a clay phone to her ear; on the right a younger clay woman with brown hair holds her own phone and smiles with relief; between them a gentle dotted arc of small pink clay hearts connects the two phones.`],
  privacy: ["1024x1024", `${PERI} Peri hugs a big rounded clay padlock, coloured forest green, with a tiny leaf on it, calm and trustworthy smile.`],
  code: ["1024x1024", `${PERI} Peri holds a big coral umbrella over a small clay phone that shows six round dots like a secret code, brave and caring, standing firm.`],
};

const reference = `data:image/webp;base64,${readFileSync(join(root, "assets/peri/peri_calm.webp")).toString("base64")}`;
mkdirSync(out, { recursive: true });
const only = process.argv.slice(2);
await Promise.all(Object.entries(IMAGES).filter(([n]) => !only.length || only.includes(n)).map(async ([name, [size, scene]]) => {
  const prompt = `${scene} ${STYLE}`;
  const res = await fetch("https://api.meta.ai/v1/images/edits", {
    method: "POST",
    headers: { authorization: `Bearer ${key}`, "content-type": "application/json" },
    body: JSON.stringify({ model: "muse-image-1.0", prompt, images: [{ image_url: reference }], n: 1, size, output_format: "webp" }),
  });
  if (!res.ok) { console.error(name, res.status, (await res.text()).slice(0, 300)); return; }
  const json = await res.json();
  writeFileSync(join(out, `${name}.webp`), Buffer.from(json.data[0].b64_json, "base64"));
  console.log(name, "ok");
}));
