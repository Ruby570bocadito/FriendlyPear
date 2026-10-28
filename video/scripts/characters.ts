/**
 * The story's people, in Peri's own clay style, made with Muse Image (muse-image-1.0, $0.01 each) on a cream
 * background that cut.py then removes. The first picture of a person is drawn from words; every other pose is an
 * edit of it, so they stay the same person (as Peri's moods are edits of hers). Run from backend/:
 *
 *   cd backend; bun ../video/scripts/characters.ts carmen
 *   cd backend; bun ../video/scripts/characters.ts carmen_relieved
 *
 * Every prompt is kept in video/prompts.json. Fictional people only, never real ones.
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const out = join(here, "..", "public", "characters");
const log = join(here, "..", "prompts.json");
const base = process.env.META_API_BASE ?? "https://api.meta.ai/v1";
const key = process.env.META_API_KEY;
if (!key) throw new Error("META_API_KEY is missing in backend/.env");

const STYLE =
  "Soft 3D clay character like a stop-motion film, matte plasticine texture, rounded friendly shapes, warm pastel colours, " +
  "gentle studio light. Full body, centred, the whole figure inside the frame. Plain flat cream background (#FFF8EC), no floor, " +
  "no shadow, no text, no logos.";

/** A person drawn from words, or [from] (one of these) with one thing changed. */
const PEOPLE: Record<string, { prompt: string; from?: string }> = {
  carmen: {
    prompt:
      "Carmen, a kind Spanish grandmother in her late seventies, short wavy grey hair, round glasses, a soft lilac cardigan over a " +
      "flowered blouse, a long grey skirt and comfortable shoes. She stands holding a smartphone in both hands and looks at it, " +
      "a little worried. Front three-quarter view. " + STYLE,
  },
  carmen_relieved: {
    from: "carmen",
    prompt: "Only change: she now smiles with relief, holding the phone against her chest. Same person, same clothes, same style, same cream background. " + STYLE,
  },
};

async function post(path: string, body: unknown): Promise<Buffer> {
  const res = await fetch(`${base}${path}`, {
    method: "POST",
    headers: { authorization: `Bearer ${key}`, "content-type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`${path} answered ${res.status}: ${(await res.text()).slice(0, 300)}`);
  const b64 = ((await res.json()) as { data: { b64_json?: string }[] }).data?.[0]?.b64_json;
  if (!b64) throw new Error(`${path} returned no image`);
  return Buffer.from(b64, "base64");
}

const names = process.argv.slice(2);
if (!names.length) throw new Error(`which person? ${Object.keys(PEOPLE).join(", ")}`);
mkdirSync(out, { recursive: true });
const prompts: Record<string, unknown> = existsSync(log) ? JSON.parse(readFileSync(log, "utf8")) : {};
for (const name of names) {
  const person = PEOPLE[name];
  if (!person) throw new Error(`no person called ${name}`);
  const body = { model: "muse-image-1.0", prompt: person.prompt, n: 1, size: "1024x1536", output_format: "webp" };
  const image = person.from
    ? await post("/images/edits", { ...body, images: [{ image_url: `data:image/webp;base64,${readFileSync(join(out, `${person.from}.webp`)).toString("base64")}` }] })
    : await post("/images/generations", body);
  writeFileSync(join(out, `${name}.webp`), image);
  prompts[`character_${name}`] = { prompt: person.prompt, from: person.from ?? null, size: "1024x1536" };
  console.log(`${name}: ${Math.round(image.length / 1024)} KB`);
}
writeFileSync(log, JSON.stringify(prompts, null, 2) + "\n");
