/**
 * The short's and the final video's pictures, made with Muse Image (muse-image-1.0, $0.01 each): a clumsy clay crook in
 * several moods (edits of one another, so he stays the same man), Ana saying no, Rosa in her garden and three new poses
 * of Peri (edits of peri_calm). Run with META_API_KEY set:
 *
 *   cd backend; bun ../video/scripts/short_images.ts                all of them
 *   cd backend; bun ../video/scripts/short_images.ts crook_angry    just one
 *
 * Every prompt is kept in video/public/short/prompts.json.
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const pub = join(here, "..", "public");
const out = join(pub, "short");
const log = join(out, "prompts.json");
const base = process.env.META_API_BASE ?? "https://api.meta.ai/v1";
const key = process.env.META_API_KEY;
if (!key) throw new Error("META_API_KEY is missing");

const CLAY = "Soft 3D clay, matte plasticine texture with visible fingerprints, rounded chunky shapes, warm pastel colours, soft studio light from the top left. No text, no letters, no numbers, no logos.";
const PERI = "Same Peri: same face, big round dark eyes, leaf, colours and clay style. Cream background.";

/** Each picture: an edit of another one (`from`, a path under video/public or assets/peri) or a new one. */
const VILLAIN = "Same man, same desk, same room, same framing and the same soft 3D clay style. Only change:";
const PICTURES: Record<string, { from?: string; tall?: boolean; prompt: string }> = {
  crook_desk: {
    tall: true,
    prompt:
      "Tall vertical shot. A funny, clumsy clay villain, harmless like in a children's stop-motion film: a skinny man with a thin curly moustache, a black " +
      "beanie and a black and white striped jumper, sitting at a messy desk covered with old phones, sticky notes and a laptop, in a dim room lit by a " +
      "purple desk lamp. He rubs his hands together with a smug, sneaky grin. " + CLAY,
  },
  crook_shock: { from: "short/crook_desk.png", tall: true, prompt: `${VILLAIN} he jumps back in shock, eyes huge, jaw dropped, moustache standing up, hands up.` },
  crook_angry: { from: "short/crook_desk.png", tall: true, prompt: `${VILLAIN} he is furious, red-faced, little puffs of steam coming out of his ears, pulling his beanie down with both hands.` },
  crook_quit: { from: "short/crook_desk.png", tall: true, prompt: `${VILLAIN} he slumps defeated with his head on the desk, waving a small white flag in one hand.` },
  crook_phone: { from: "short/crook_desk.png", tall: true, prompt: `${VILLAIN} he holds an old phone to his ear and puts on a posh fake voice, little finger raised, eyebrows up, very pleased with himself.` },
  crook_stickers: { from: "short/crook_desk.png", tall: true, prompt: `${VILLAIN} he proudly holds up a sheet of small square stickers with black and white QR-like patterns, with a sneaky grin.` },
  crook_plan: { from: "short/crook_desk.png", tall: true, prompt: `${VILLAIN} he points proudly over his shoulder at a cork board on the wall behind him, with four papers pinned on it joined by red strings, a smug grin.` },
  ana_no: {
    from: "submission/ana_city.png",
    prompt: "Same young woman (long brown hair, sky blue knitted jumper), same city flat at dusk, same framing and the same soft 3D clay style. Only change: she taps her phone firmly with one finger, with a determined, protective face.",
  },
  rosa_garden: {
    from: "submission/lonely.png",
    tall: true,
    prompt:
      "Same elderly woman (silver hair in a bun, lilac knitted cardigan) in the same soft 3D clay style. New scene: she sits relaxed in a wicker chair in a " +
      "sunny little garden full of flowers, knitting a long green scarf with a calm smile, her phone on a small round table beside her. No text.",
  },
  peri_wag: { from: "../../assets/peri/peri_calm.webp", prompt: `Only change: Peri wags one raised finger from side to side, as if saying "no, no", with a calm, confident face. ${PERI}` },
  peri_handset: { from: "../../assets/peri/peri_calm.webp", prompt: `Only change: Peri holds a big old-fashioned green clay telephone handset to the side of her head with both hands, determined and kind, as if calling someone important. ${PERI}` },
  peri_trophy: { from: "../../assets/peri/peri_calm.webp", prompt: `Only change: Peri holds a small shiny golden trophy high above her head with both hands, jumping a little with joy, eyes squeezed happily. ${PERI}` },
};

async function call(path: string, body: Record<string, unknown>, tall: boolean): Promise<{ image: Buffer; size: string }> {
  for (const size of tall ? ["1024x1536", "1024x1024"] : ["1024x1024"]) {
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
for (const [name, { from, tall = false, prompt }] of Object.entries(PICTURES)) {
  if (wanted.length ? !wanted.includes(name) : existsSync(join(out, `${name}.png`))) continue;
  const result = from
    ? await call("/images/edits", { prompt, images: [{ image_url: `data:image/${from.endsWith(".webp") ? "webp" : "png"};base64,${readFileSync(join(pub, from)).toString("base64")}` }] }, tall)
    : await call("/images/generations", { prompt }, tall);
  writeFileSync(join(out, `${name}.png`), result.image);
  prompts[name] = { prompt, size: result.size };
  writeFileSync(log, JSON.stringify(prompts, null, 2));
  console.log(`${name}: ${result.size}, ${Math.round(result.image.length / 1024)} KB`);
}
