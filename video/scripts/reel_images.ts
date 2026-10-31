/**
 * The interview reel's pictures, made with Muse Image (muse-image-1.0, $0.01 each): Rosa interviewing candidates for
 * "bodyguard of her phone", the room, the two rejected candidates and Peri's new poses. Rosa's pictures are edits of
 * one another (so she stays the same woman) and Peri's are edits of peri_calm. Run with META_API_KEY set:
 *
 *   cd backend; bun ../video/scripts/reel_images.ts               all of them
 *   cd backend; bun ../video/scripts/reel_images.ts rosa_laugh    just one
 *
 * Every prompt is kept in video/public/reel/prompts.json.
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const pub = join(here, "..", "public");
const out = join(pub, "reel");
const log = join(out, "prompts.json");
const base = process.env.META_API_BASE ?? "https://api.meta.ai/v1";
const key = process.env.META_API_KEY;
if (!key) throw new Error("META_API_KEY is missing");

const CLAY = "Soft 3D clay, matte plasticine texture with visible fingerprints, rounded chunky shapes, warm pastel colours, soft studio light from the top left. No text, no letters, no numbers, no logos.";
const PERI = "Same Peri: same face, big round dark eyes, leaf, colours and clay style. Cream background.";
const ROSA = "Same woman, same desk, same office, same framing and the same soft 3D clay style. Only change:";

/** Each picture: an edit of another one (`from`, a path under video/public or assets/peri) or a new one. */
const PICTURES: Record<string, { from?: string; tall?: boolean; prompt: string }> = {
  rosa_desk: {
    from: "submission/lonely.png",
    tall: true,
    prompt:
      "Same elderly woman (silver hair in a bun, lilac knitted cardigan) in the same soft 3D clay style. New scene: she sits behind a small wooden desk in a " +
      "cosy little office, round reading glasses on the tip of her nose, holding a clipboard and a pen, looking towards the right side of the image with a " +
      "serious but playful interviewer face. Warm daylight from a window. No text, no letters.",
  },
  rosa_meh: { from: "reel/rosa_desk.png", tall: true, prompt: `${ROSA} she raises one eyebrow and purses her lips, unimpressed, pointing her pen to the right as if saying "next!".` },
  rosa_laugh: { from: "reel/rosa_desk.png", tall: true, prompt: `${ROSA} she laughs with joy, eyes squeezed happily shut, and gives a big thumbs up with one hand.` },
  rosa_phone: { from: "reel/rosa_desk.png", tall: true, prompt: `${ROSA} she holds her phone up proudly towards the viewer with a warm, satisfied smile, the glasses still on her nose.` },
  office: {
    tall: true,
    prompt:
      "Tall vertical shot of a cosy little interview room: a warm peach wall with framed pictures of pears and flowers, a potted plant, a small window with " +
      "daylight, a wooden floor with a soft round spotlight on it in the middle, and empty space in the centre where someone could stand. No people, no " +
      "characters. " + CLAY,
  },
  padlock: {
    prompt:
      "A funny grumpy clay character standing, full body, centred: a big shiny golden padlock with stubby arms and legs, thick angry eyebrows and a frowning " +
      "mouth, heavy chains over its shoulders, arms crossed. Plain cream background (#FFF8EC), nothing else. Like a stop-motion children's film. " + CLAY,
  },
  camera: {
    prompt:
      "A funny sneaky clay character standing, full body, centred: a grey security camera head on a little round body with stubby arms and legs, one huge " +
      "round lens eye looking sideways suspiciously, holding small binoculars, a sly grin. Plain cream background (#FFF8EC), nothing else. Like a " +
      "stop-motion children's film. " + CLAY,
  },
  peri_tie: { from: "../../assets/peri/peri_calm.webp", prompt: `Only change: Peri wears a small neat red tie and holds a tiny sheet of paper (her CV) in both hands in front of her, with a hopeful, slightly nervous smile. ${PERI}` },
  peri_guard: { from: "../../assets/peri/peri_calm.webp", prompt: `Only change: Peri wears cool black sunglasses and a tiny black earpiece with a curly wire, arms crossed over her chest, a confident proud smile, like a bodyguard. ${PERI}` },
  peri_stop: { from: "../../assets/peri/peri_calm.webp", prompt: `Only change: Peri holds one hand up firmly in front of her, palm out like a stop sign, with a serious, brave, protective face (eyebrows a little lowered, no smile); her other hand rests on her hip. ${PERI}` },
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
