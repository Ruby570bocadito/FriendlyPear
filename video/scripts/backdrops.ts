/**
 * The ad's backdrops: places of Peri's world in her soft clay style, made with Muse Image (muse-image-1.0, $0.01
 * each). No people and no text in them: the real app goes in front. Run from backend/, which holds the API key:
 *
 *   cd backend; bun ../video/scripts/backdrops.ts           all of them
 *   cd backend; bun ../video/scripts/backdrops.ts street    just one
 *
 * Every prompt is kept in video/prompts.json.
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const out = join(here, "..", "public", "backdrops");
const log = join(here, "..", "prompts.json");
const base = process.env.META_API_BASE ?? "https://api.meta.ai/v1";
const key = process.env.META_API_KEY;
if (!key) throw new Error("META_API_KEY is missing in backend/.env");

const STYLE =
  "Soft 3D clay diorama, matte plasticine texture, rounded shapes, warm pastel colours with fresh greens and a soft sky blue, " +
  "gentle afternoon light, slightly blurred like a shallow depth of field, tall vertical composition with calm empty space in the middle. " +
  "No people, no animals, no characters, no text, no letters, no numbers, no logos.";

const BACKDROPS: Record<string, string> = {
  home:
    "A cosy living room in a Spanish flat: a soft armchair with a knitted blanket, a small side table with a cup of tea, " +
    "a standing lamp, plants on the window sill, a window with a pale blue sky. " + STYLE,
  street:
    "A quiet sunny street in a Spanish town: a pavement with a parking meter at one side, small parked cars, balconies with " +
    "flower pots, a tree, a pale blue sky. " + STYLE,
  sky:
    "An open evening sky above gentle green hills and a small village with warm lit windows, soft pink and peach clouds, " +
    "the first stars. " + STYLE,
};

async function generate(prompt: string): Promise<{ image: Buffer; size: string }> {
  // Tall first; a square one is cropped to fit if the model only makes squares.
  for (const size of ["1024x1536", "1024x1024"]) {
    const res = await fetch(`${base}/images/generations`, {
      method: "POST",
      headers: { authorization: `Bearer ${key}`, "content-type": "application/json" },
      body: JSON.stringify({ model: "muse-image-1.0", prompt, n: 1, size, output_format: "webp" }),
    });
    if (res.status === 400 && size !== "1024x1024") continue;
    if (!res.ok) throw new Error(`images/generations answered ${res.status}: ${(await res.text()).slice(0, 300)}`);
    const b64 = ((await res.json()) as { data: { b64_json?: string }[] }).data?.[0]?.b64_json;
    if (!b64) throw new Error("images/generations returned no image");
    return { image: Buffer.from(b64, "base64"), size };
  }
  throw new Error("no size worked");
}

const wanted = process.argv.slice(2);
const names = wanted.length ? wanted : Object.keys(BACKDROPS);
mkdirSync(out, { recursive: true });
const prompts: Record<string, { prompt: string; size: string }> = existsSync(log) ? JSON.parse(readFileSync(log, "utf8")) : {};
for (const name of names) {
  const prompt = BACKDROPS[name];
  if (!prompt) throw new Error(`no backdrop called ${name}`);
  const { image, size } = await generate(prompt);
  writeFileSync(join(out, `${name}.webp`), image);
  prompts[name] = { prompt, size };
  console.log(`${name}: ${size}, ${Math.round(image.length / 1024)} KB`);
}
writeFileSync(log, JSON.stringify(prompts, null, 2) + "\n");
