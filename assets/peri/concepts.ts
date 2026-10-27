/**
 * Eight alternative designs for Peri, with Muse Image (muse-image-1.0, $0.01 each), to choose a direction.
 * Writes assets/peri/concepts/concept_<n>.webp and prompts.json; join them with concepts_sheet.py.
 *
 *   cd backend; bun ../assets/peri/concepts.ts
 */
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";

const here = dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, "$1"));
const out = join(here, "concepts");
const base = process.env.META_API_BASE ?? "https://api.meta.ai/v1";
const key = process.env.META_API_KEY;
if (!key) throw new Error("META_API_KEY is missing in backend/.env");

const CHARACTER =
  "Peri, a friendly round green pear mascot with a leaf on top, big kind eyes, rosy cheeks, small arms and legs, " +
  "standing and smiling, full body, front view, centered, plain warm cream background, no text, no letters.";

export const CONCEPTS: Record<string, string> = {
  clay: "Soft 3D clay render, rounded shapes, gentle studio lighting, tactile matte material.",
  watercolor: "Warm watercolor storybook illustration, soft brush edges, gentle paper texture.",
  sticker: "Kawaii chibi sticker style, oversized head, thick white outline around the character, glossy highlights.",
  grandma: "Cozy grandparent-friendly look: round reading glasses and a knitted scarf, warm and wise, flat vector illustration.",
  hero: "Little protector hero: a small coral cape and a leaf-shaped shield badge on the chest, brave and kind, bold cartoon style.",
  geometric: "Minimal geometric flat design made of simple circles and soft gradients, modern app mascot.",
  plush: "Plush toy made of felt fabric with visible stitches and button details, soft and huggable.",
  anime: "Anime-inspired style with sparkling eyes, soft cel shading and a cheerful expression.",
};

mkdirSync(out, { recursive: true });
const prompts: Record<string, string> = {};
for (const [name, style] of Object.entries(CONCEPTS)) {
  const prompt = `${CHARACTER} ${style}`;
  const res = await fetch(`${base}/images/generations`, {
    method: "POST",
    headers: { authorization: `Bearer ${key}`, "content-type": "application/json" },
    body: JSON.stringify({ model: "muse-image-1.0", prompt, n: 1, size: "1024x1024", output_format: "webp" }),
  });
  if (!res.ok) throw new Error(`${name}: ${res.status} ${(await res.text()).slice(0, 200)}`);
  const b64 = ((await res.json()) as { data: { b64_json?: string }[] }).data?.[0]?.b64_json;
  if (!b64) throw new Error(`${name}: no image`);
  writeFileSync(join(out, `concept_${name}.webp`), Buffer.from(b64, "base64"));
  prompts[name] = prompt;
  console.log(`${name}: ok`);
}
writeFileSync(join(out, "prompts.json"), JSON.stringify(prompts, null, 2) + "\n");
console.log(`${Object.keys(prompts).length} concepts (about $${(Object.keys(prompts).length * 0.01).toFixed(2)})`);
