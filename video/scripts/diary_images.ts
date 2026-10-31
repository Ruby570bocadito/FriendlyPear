/**
 * The pictures of "Peri's diary", the second 3-minute video, made with Muse Image (muse-image-1.0, $0.01 each), each an
 * edit of a picture already in the video's clay style so the world stays the same: Rosa's town at five moments of the
 * day (wide, 16:9), Peri writing her diary at night, yawning and asleep, her hill at sunrise, Rosa fallen at night and
 * Ana's hug, and Rosa's relief after each trick. Run from backend/, whose .env holds the API key:
 *
 *   cd backend; bun ../video/scripts/diary_images.ts           every picture still missing, in order
 *   cd backend; bun ../video/scripts/diary_images.ts <name>... just those (made again)
 *
 * Pictures go to video/public/diary/; every prompt is kept in its prompts.json. Their mouth and eye frames come from
 * heist_visemes.ts (group "diary").
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const pub = join(here, "..", "public");
const out = join(pub, "diary");
const log = join(out, "prompts.json");
const base = process.env.META_API_BASE ?? "https://api.meta.ai/v1";
const key = process.env.META_API_KEY;
if (!key) throw new Error("META_API_KEY is missing (run from backend/, whose .env holds it)");

const CLAY = "the same soft 3D clay style, warm and cosy";
const WIDE = "1792x1024";
const PERI = "Peri, the same small green clay pear with the same face, big round dark eyes, rosy cheeks and leaf";

/** Each picture: what it's an edit of (a path under video/public), its prompt and its size. Order matters: some build on others. */
const PICTURES: Record<string, { from: string; prompt: string; size: string }> = {
  town_morning: {
    from: "heist/rosa_garden_call.png",
    prompt: `A cosy little clay cottage with a lilac front door, round windows and a small flower garden behind a low picket fence, seen from the garden path, early morning just after sunrise: soft golden light, long gentle shadows, a bird on the fence, no people. Wide view, ${CLAY}.`,
    size: WIDE,
  },
  kitchen_noon: {
    from: "heist/rosa_kitchen.png",
    prompt: `The same cosy kitchen, wider view, with nobody in it: bright late-morning sun through the window, a cup of tea and a notepad on the table, and a phone on the table lighting up with an incoming call. ${CLAY}.`,
    size: WIDE,
  },
  street_afternoon: {
    from: "heist/rosa_qr.png",
    prompt: "Same clay town street and the same soft 3D clay style, wider view, no people, warm afternoon light.",
    size: WIDE,
  },
  garden_sunset: {
    from: "heist/rosa_garden_call.png",
    prompt: `The same garden, wider view, with nobody in it: the empty wicker chair with the green knitting left on it, the little round table, flowers everywhere, a warm pink and orange sunset sky and a string of little lights starting to glow. ${CLAY}.`,
    size: WIDE,
  },
  cottage_night: {
    from: "diary/town_morning.png",
    prompt: `The same cottage and garden, same framing, now late at night: a deep blue sky full of stars and a round moon, warm golden light glowing in the windows, a little lamp by the door, peaceful and cosy, not dark. No people. ${CLAY}.`,
    size: WIDE,
  },
  hill_morning: {
    from: "diary/street_afternoon.png",
    prompt: `A small round clay hill covered in soft grass and tiny flowers, in the middle of a cosy little world, under a sunrise sky in pastel yellow, peach and pink with a few puffy clay clouds; the top of the hill is empty and flat, ready for someone to stand on it. No buildings, no people. Wide view, ${CLAY}.`,
    size: WIDE,
  },
  peri_desk: {
    from: "../../assets/peri/sprites/scene_night.webp",
    prompt: `${PERI}, sits at a small wooden desk in her cosy little room at night, writing in an open diary with a pencil and looking up with a gentle smile; a warm desk lamp lights the room, a round window shows stars and a crescent moon, a mug and a little plant on the desk. Wide view with Peri in the middle, ${CLAY}, warm light, not dark.`,
    size: WIDE,
  },
  peri_desk_yawn: {
    from: "diary/peri_desk.png",
    prompt: `Exactly the same room, desk, lamp and window, same framing and the same clay style. Only change: Peri has closed the diary and stretches both arms up in a big sleepy yawn, eyes closed.`,
    size: WIDE,
  },
  peri_desk_sleep: {
    from: "diary/peri_desk.png",
    prompt: `Exactly the same room, desk, lamp and window, same framing and the same clay style. Only change: Peri has fallen asleep with her head resting on the closed diary, a little smile, and three small letters z floating above her; the lamp is still on.`,
    size: WIDE,
  },
  cover: {
    from: "diary/peri_desk.png",
    prompt: `A closed cosy notebook lying flat on the same wooden desk, seen from straight above and filling most of the picture: a soft green fabric cover with rounded corners and stitched edges, a cute clay pear sticker with a little leaf in the middle, a red ribbon bookmark hanging out at the bottom, a pencil beside it, warm lamp light. No text, no letters. ${CLAY}.`,
    size: WIDE,
  },
  rosa_fallen_night: {
    from: "heist/rosa_fallen.png",
    prompt: `Exactly the same elderly woman, same pose, same living room, same framing and the same soft 3D clay style. Only change: it is now night: the floor lamp glows warm and golden and the window shows a deep blue night sky with stars; the room stays warm and cosy, not dark. Her mouth is closed.`,
    size: "1024x1024",
  },
  ana_hug: {
    from: "diary/rosa_fallen_night.png",
    prompt: `The same living room at night, same lamp light and the same soft 3D clay style. The same elderly woman still sits on the rug, and her daughter Ana, a young woman with long wavy brown hair, a blue knitted jumper and a beige coat, kneels beside her and hugs her warmly; both have their eyes closed and smile with relief.`,
    size: "1024x1024",
  },
  rosa_kitchen_hangup: {
    from: "heist/rosa_kitchen.png",
    prompt: `Exactly the same elderly woman, same kitchen, same framing and the same soft 3D clay style. Only change: she has put the phone down on the table and smiles with relief, one hand on her chest, her mouth closed.`,
    size: "1024x1024",
  },
  rosa_qr_away: {
    from: "heist/rosa_qr.png",
    prompt: `Exactly the same elderly woman, same street, same framing and the same soft 3D clay style. Only change: she has lowered her phone and smiles knowingly, shaking her head at the sticker on the parking meter, her mouth closed.`,
    size: "1024x1024",
  },
  rosa_garden_smile: {
    from: "heist/rosa_garden_call.png",
    prompt: `Exactly the same elderly woman, same garden, same wicker chair, same framing and the same soft 3D clay style. Only change: she has lowered the phone to her lap and smiles warmly at its screen, relieved, her mouth closed.`,
    size: "1024x1536",
  },
};

async function edit(from: string, prompt: string, size: string): Promise<{ image: Buffer; size: string }> {
  const file = join(pub, from);
  const mime = from.endsWith(".webp") ? "webp" : "png";
  for (const tried of [size, "1024x1024"]) {
    for (let attempt = 0; attempt < 3; attempt++) {
      const res = await fetch(`${base}/images/edits`, {
        method: "POST",
        headers: { authorization: `Bearer ${key}`, "content-type": "application/json" },
        body: JSON.stringify({ model: "muse-image-1.0", prompt, images: [{ image_url: `data:image/${mime};base64,${readFileSync(file).toString("base64")}` }], n: 1, size: tried, output_format: "png" }),
      });
      if (res.ok) {
        const b64 = ((await res.json()) as { data: { b64_json?: string }[] }).data?.[0]?.b64_json;
        if (b64) return { image: Buffer.from(b64, "base64"), size: tried };
      } else {
        const text = await res.text();
        // A 400 for this size means the model won't make it; a content-policy refusal is final.
        if (res.status === 400 && tried !== "1024x1024" && !text.includes("content_policy")) break;
        if (res.status !== 429 && res.status < 500) throw new Error(`images/edits answered ${res.status}: ${text.slice(0, 300)}`);
      }
      await new Promise((r) => setTimeout(r, 4000 * (attempt + 1)));
    }
  }
  throw new Error(`no picture for ${from}`);
}

const names = process.argv.slice(2);
mkdirSync(out, { recursive: true });
const prompts: Record<string, { from: string; prompt: string; size: string }> = existsSync(log) ? JSON.parse(readFileSync(log, "utf8")) : {};
for (const [name, { from, prompt, size }] of Object.entries(PICTURES)) {
  if (names.length ? !names.includes(name) : existsSync(join(out, `${name}.png`))) continue;
  const made = await edit(from, prompt, size);
  writeFileSync(join(out, `${name}.png`), made.image);
  prompts[name] = { from, prompt, size: made.size };
  writeFileSync(log, JSON.stringify(prompts, null, 2));
  console.log(`${name}: ${made.size}, ${Math.round(made.image.length / 1024)} KB`);
}
