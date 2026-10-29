/**
 * The video's and the interface's pictures for round 5 (docs/IMAGENES_VIDEO_UI.md, lotes 8 to 15), made with Muse
 * Image (muse-image-1.0, $0.01 each) in Peri's clay style. Each person and place starts from one picture and the rest
 * are edits of it, so they stay the same. No text, numbers or real brands in any of them; fictional people only.
 * Writes video/public/<folder>/<name>.webp (assets/ui/ for the interface) and each prompt in that folder's prompts.json.
 *
 *   cd backend; bun ../video/scripts/images.ts 12                       lote 12 (skips pictures already made)
 *   cd backend; bun ../video/scripts/images.ts 8 carmen_wave ana_alert  only those (made again)
 *   cd backend; bun ../video/scripts/images.ts 8 --paths                where each picture of the lote goes
 * Then `python video/scripts/sheet.py <sheet.jpg> <paths…>` draws a contact sheet to look at.
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
const base = process.env.META_API_BASE ?? "https://api.meta.ai/v1";
const key = process.env.META_API_KEY;
if (!key) throw new Error("META_API_KEY is missing in backend/.env");

const PALETTE = "bright cheerful colours from pear green, coral, sky blue, lilac and sunny yellow";
const NO_TEXT = "no text, no letters, no numbers, no logos, no brands";
const CREAM = "plain flat warm cream background (#FFF8EC), no floor shadow";
// As in characters.ts: the people of the story.
const PERSON =
  "Soft 3D clay character like a stop-motion film, matte plasticine texture, rounded friendly shapes, warm pastel colours, " +
  `gentle studio light. Full body, centred, the whole figure inside the frame. ${CREAM}, no floor, ${NO_TEXT}.`;
// As in assets/peri/sprites.ts: Peri herself.
const PERI =
  `Soft 3D clay render, rounded shapes, gentle studio lighting, tactile matte plasticine material, ${NO_TEXT}, ` +
  `centered character with space around it, ${CREAM}.`;
const OBJECT =
  `Soft 3D clay render of one single object, isolated and centered, straight front view, no character, no face, ${NO_TEXT}, ` +
  `${CREAM}, tactile matte plasticine material, gentle studio lighting, ${PALETTE}.`;
const PLACE =
  `Soft 3D clay diorama, matte plasticine texture, rounded shapes, ${PALETTE}, gentle warm light, slightly blurred like a ` +
  "shallow depth of field, tall vertical composition with calm empty space at the bottom in the middle for two characters to stand. " +
  `No people, no animals, no characters, ${NO_TEXT}; any screen is plain and blank.`;
const CARD = `Soft 3D clay, matte plasticine texture, rounded shapes, ${PALETTE}, gentle warm light, ${NO_TEXT}.`;

type Job = { name: string; dir: string; from?: string; prompt: string; size?: string };
const TALL = "1024x1536";
const WIDE = "1536x1024";
const LOTES: Record<string, Job[]> = {};

const v = (dir: string) => `video/public/${dir}`;
const carmen = (name: string, change: string, from = "carmen") =>
  ({ name, dir: v("characters"), from: `${v("characters")}/${from}`, size: TALL,
     prompt: `The same person: same face, same short wavy grey hair, same round glasses, same lilac cardigan, flowered blouse, grey skirt and shoes, same style. Change: ${change} ${PERSON}` });
const person = (name: string, who: string) => ({ name, dir: v("characters"), size: TALL, prompt: `${who} ${PERSON}` });
const pose = (name: string, from: string, change: string) =>
  ({ name, dir: v("characters"), from: `${v("characters")}/${from}`, size: TALL,
     prompt: `The same person: same face, same hair, same clothes, same style. Change: ${change} ${PERSON}` });

LOTES["8"] = [
  carmen("carmen_phone_doubt", "she looks at her smartphone with one eyebrow raised, curious and a little suspicious: something odd has arrived."),
  carmen("carmen_worried", "she looks concerned, one hand on her chest, the other holding her phone low."),
  carmen("carmen_laugh", "she laughs heartily, head tilted back a little, eyes squeezed with joy, holding her phone at her side."),
  carmen("carmen_wave", "she waves hello with one hand, a big cheerful smile, the phone in her other hand."),
  carmen("carmen_hug_phone", "she hugs her phone against her chest with both hands, relieved, eyes softly closed with a smile."),
  carmen("carmen_calling", "she holds her phone to her ear and talks calmly, a relaxed smile."),
  carmen("carmen_sofa", "she sits comfortably on a small round coral clay sofa with a knitted blanket over her knees, holding her phone, smiling."),
  carmen("carmen_walk", "she walks briskly, mid-step, with a small handbag on her arm and her phone in her hand, lively and confident."),
  carmen("carmen_point", "she points firmly at something on the right side of the image with one finger, determined and sharp."),
  carmen("carmen_thumbs", "she gives a big thumbs up with a clever, humorous smile."),
  person("ana_base", "Ana, a friendly Spanish woman around forty, shoulder-length dark brown hair, a comfortable teal jumper over a white shirt, dark trousers and trainers, a lanyard-free relaxed office look; she stands smiling, holding a smartphone. Front three-quarter view, fictional person."),
  pose("ana_alert", "ana_base", "she looks at her smartphone seriously and attentively, eyebrows drawn together: an alert has just arrived."),
  pose("ana_calling", "ana_base", "she holds her phone to her ear and smiles with relief, her other hand on her chest."),
  person("pablo_base", "Pablo, a cheerful Spanish teenage boy about sixteen, messy dark curly hair, a mustard yellow hoodie, jeans and white trainers, holding a smartphone, a friendly grin. Front three-quarter view, fictional person."),
  person("papa_base", "Papá, a kind Spanish grandfather in his late seventies, white moustache, a flat cap, a green knitted waistcoat over a checked shirt, brown trousers, a warm smile, hands behind his back. Front three-quarter view, fictional person."),
];

const place = (name: string, scene: string) => ({ name, dir: v("backdrops"), size: TALL, prompt: `${scene} ${PLACE}` });
const sameplace = (name: string, from: string, change: string) =>
  ({ name, dir: v("backdrops"), from: `${v("backdrops")}/${from}`, size: TALL, prompt: `Exactly the same place, same composition, same objects, same style. Change: ${change} ${PLACE}` });
LOTES["9"] = [
  place("village_sunset", "Carmen's Spanish village at sunset: little white clay houses with terracotta roofs, a church bell tower, rolling green hills, a peach and pink sky, warm lit windows."),
  sameplace("village_morning", "village_sunset", "it is a fresh sunny morning: a clear light blue sky with a few white clouds, the windows unlit, bright soft light."),
  sameplace("living_day", "home", "it is a bright day, a cosy coral sofa with cushions now stands in the room, a small coffee table with a fruit bowl, a plant and a window with sunlight."),
  sameplace("living_evening", "living_day", "it is evening: the window shows a deep blue dusk, the standing lamp is lit with a warm golden glow, the room is cosy and warmly lit."),
  place("kitchen", "Carmen's cosy kitchen: a small old-fashioned clay radio on the counter, a fruit bowl with pears and oranges, tiled walls, a window with plants, hanging pans."),
  place("street_parking", "A quiet sunny street in a Spanish town with a big, clearly visible grey and blue clay parking meter on a post standing on the pavement in the front left of the image, small parked cars, balconies with flowers, a tree."),
  place("ana_home", "Ana's modern city flat living room: a grey sofa with teal cushions, a big window with city buildings outside, a shelf with books and plants, a laptop on a desk."),
  place("peri_workshop", "Peri's magical workshop: chunky clay gears, curvy pipes, round blank screens glowing softly, little lamps, warm golden light, cosy and inventive."),
  place("plaza", "The village square: a round stone fountain with water, a wooden bench under a tree, little houses around, bunting between balconies, sunny day."),
  place("night_window", "Carmen's window at night seen from inside: a big round moon and stars outside, a plant on the window sill, soft blue moonlight and a warm curtain."),
  { name: "phone_hands", dir: v("backdrops"), size: TALL, prompt: `Two friendly clay hands of an older woman, with a lilac cardigan cuff on each wrist, holding a smartphone straight towards the viewer; the phone fills most of the image and its screen is perfectly plain flat white and blank. ${CARD} ${CREAM}.` },
  sameplace("phone_table", "living_day", "a large smartphone, clearly visible, lies face up on the small coffee table, which is now big and in the front centre of the image; the phone's screen is plain flat white and blank."),
];

const prop = (name: string, what: string) => ({ name, dir: v("props"), prompt: `${what} Friendly and playful, not scary. ${OBJECT}` });
LOTES["10"] = [
  prop("scam_hook_message", "A white chat speech bubble hanging from a shiny clay fishing hook on a line."),
  prop("scam_mask_phone", "An old-style clay telephone handset wearing a small black eye mask, like a masquerade mask."),
  prop("scam_qr_sticker", "A square white paper sticker with a scannable black square code printed on it (three black corner squares and small random black blocks), one corner slightly peeling; a small shiny fishing hook peeks out from under the peeling corner."),
  prop("scam_parcel", "A cardboard parcel box tied with string, with a big yellow question mark shape on top."),
  prop("scam_prize_box", "A coral gift box with a big ribbon bow, with a fishing hook and line coming out of the bow."),
  prop("scam_heart_hook", "A coral heart caught on a shiny clay fishing hook on a line."),
  prop("scam_remote_hand", "A green clay hand reaching out of a plain blank tablet screen towards a small pile of golden coins."),
  prop("scam_safe_account", "A pink piggy bank with a padlock on its front; seen from a slight angle, its back is open with an empty hole."),
];

const fx = (name: string, what: string) => ({ name, dir: v("fx"), prompt: `${what} ${OBJECT}` });
LOTES["11"] = [
  fx("fx_leaf_wipe", "One huge green pear leaf filling most of the image, with a curved central vein."),
  fx("fx_clay_blob", "A big round flat blob of smooth pear green clay, slightly irregular edge, one single colour, plain, no dots and no decorations."),
  fx("fx_confetti_big", "A joyful burst of clay confetti pieces in many colours spreading out from the centre."),
  fx("fx_hearts_burst", "A handful of chubby coral and pink clay hearts of different sizes, spread out."),
  fx("fx_sparkle_burst", "A burst of shiny golden four-pointed and five-pointed clay stars, sparkling."),
  fx("fx_shield_glow", "A green clay shield with a white pear leaf on it and a soft glow around it."),
  fx("fx_check_big", "A big chunky rounded green check mark."),
  fx("fx_cross_big", "A big chunky rounded cross like an X, all in one single coral colour, plain, no dots and no decorations."),
  fx("fx_arrow_curve", "A chunky curved arrow in sunny yellow clay, bending from left to upper right."),
  fx("fx_bubble_empty", "An empty white speech bubble with a small tail at the bottom left, rounded and puffy."),
  fx("fx_thought_empty", "An empty white thought cloud with two small round puffs trailing below it."),
  fx("fx_magnifier", "A magnifying glass with a brown handle and a shiny glass lens."),
  fx("fx_padlock", "A closed sky blue padlock with a shiny silver shackle."),
  fx("fx_clock", "A round lilac alarm clock with two bells on top, its face with only two hands and no numbers."),
  fx("fx_vibrate", "Two groups of three short curved coral clay lines, one group on the left and one on the right, like the vibration marks drawn beside a ringing phone; the middle of the image between them is completely empty. Plain, no dots."),
];

const card = (name: string, what: string, size = TALL) => ({ name, dir: v("cards"), size, prompt: `${what} ${CARD}` });
LOTES["12"] = [
  { name: "logo_mark", dir: v("cards"), from: "assets/peri/peri_icon", prompt: `A simple app symbol made from this character: a round green clay pear with a leaf on top and a small green shield with a white leaf in front of it, friendly and clean, no face details beyond a tiny smile. ${OBJECT}` },
  card("card_title_bg", "A sky made of clay with puffy white clouds around the edges and a large calm empty light blue space in the centre, for a title."),
  card("card_end_bg", "A sunset over a gentle green clay hill; on the hilltop in the lower part, four little pear characters stand together: a big green pear with a leaf and three smaller pears, one with a flat cap, one with a bow, one with a beanie. Large empty sky at the top."),
  card("card_ribbon", `An empty horizontal coral clay ribbon banner with folded ends, nothing written on it. ${CREAM}.`, "1024x1024"),
  card("card_badge", `An empty round sunny yellow clay disc with a raised rim, nothing on it. ${CREAM}.`, "1024x1024"),
  card("card_stage", `Six empty podium pedestals in a row, each a wide short round platform with a flat top, wider than tall, like a winners podium, in pear green, coral, sky blue, lilac, sunny yellow and mint, slightly different heights. ${CREAM}.`, WIDE),
  card("card_board", "An empty dark green clay chalkboard with a rounded wooden frame, nothing written on it, filling most of the image, on a cream wall."),
  card("card_split", "A background split vertically into two halves: soft pear green clay on the left and soft coral clay on the right, with a gentle wavy edge between them, nothing else."),
  card("social_square_bg", "A cheerful clay background: soft green hills, a sunny sky with puffy clouds, little leaves and hearts floating around the edges, calm empty space in the centre.", "1024x1024"),
  card("social_vertical_bg", "A cheerful clay background: soft green hills at the bottom, a sunny sky with puffy clouds, little leaves and hearts floating around the edges, calm empty space in the middle."),
];

const peri = (name: string, dir: string, change: string, from = "assets/peri/peri_calm", size?: string) =>
  ({ name, dir, from, size, prompt: `Same character, same style. Change: Peri ${change} ${PERI}` });
const story = (name: string, change: string) => peri(name, v("peri"), change);
LOTES["13"] = [
  story("story_suitcase", "arrives pulling a small coral suitcase on wheels, cheerful, as if moving in."),
  story("story_from_phone", "climbs out of the plain blank screen of a big clay smartphone, waving."),
  story("story_point_screen", "points to the right side of the image with one arm, as if presenting a screen there, friendly."),
  story("story_present", "opens both arms wide, presenting something big, delighted."),
  story("story_hook_magnifier", "looks closely through a big magnifying glass at a small shiny fishing hook floating beside her, curious."),
  story("story_hold_phone", "holds a big clay smartphone with a plain blank screen in both hands in front of her, smiling."),
  story("story_shield_up", "raises a green shield with a white leaf on it, brave and kind."),
  story("story_family_pears", "stands with three smaller pear friends beside her, the same clay style: one with a flat cap, one with a coral bow, one with a beanie; all smiling together."),
  story("story_bow", "makes a polite goodbye bow, one hand on her tummy."),
  story("story_sleep_moon", "sleeps curled up on a yellow clay crescent moon, eyes closed, a tiny night cap."),
  story("story_run_message", "runs happily holding a white envelope up in one hand."),
  story("story_heart_family", "hugs a big coral heart, with three smaller pear friends around her (one with a flat cap, one with a coral bow, one with a beanie)."),
];

const ui = "assets/ui";
const badge = (name: string, metal: string) =>
  ({ name, dir: ui, prompt: `A round ${metal} medal badge with a raised shield and a small pear leaf on it, shiny, nothing written. ${OBJECT}` });
LOTES["14"] = [
  peri("ui_empty_diary", ui, "holds an open blank notebook and a pencil, ready to write, hopeful smile."),
  peri("ui_empty_family", ui, "waits patiently holding a coral heart in one hand and a small phone with a blank screen in the other."),
  peri("ui_welcome_home", ui, "arrives at a small cosy clay house shaped like a smartphone, with a little door and a window, waving."),
  peri("ui_welcome_shield", ui, "holds a green shield with a white leaf in front of a white envelope, protective and friendly."),
  peri("ui_welcome_family", ui, "stands with three smaller pear friends: one with a flat cap, one with a coral bow, one with a beanie."),
  peri("ui_perm_sms", ui, "holds an open white envelope with a letter peeking out, kind smile."),
  peri("ui_perm_notifications", ui, "holds up a small golden bell, ringing it gently."),
  peri("ui_perm_calls", ui, "holds an old-style sky blue clay telephone handset, friendly."),
  peri("ui_perm_battery", ui, "hugs a big upright battery, a clear cylinder battery with a small round terminal on top, its side showing four bright green bars stacked up to the top: full; happy."),
  peri("ui_offline", ui, "calmly holds an unplugged cable with a plug in one hand, a relaxed smile, a tiny cloud beside her."),
  badge("ui_badge_bronze", "bronze"),
  badge("ui_badge_silver", "silver"),
  badge("ui_badge_gold", "gold"),
  { name: "ui_tool_game", dir: ui, prompt: `Two clay playing cards standing side by side, plain with rounded corners: one sky blue with a green check mark, one coral with a small fishing hook on it. ${OBJECT}` },
  { name: "ui_tool_ask", dir: ui, prompt: `A puffy white speech bubble with a chunky green question mark on it. ${OBJECT}` },
];

LOTES["15"] = [
  { name: "icon_peri", dir: ui, from: "assets/peri/peri_icon", prompt: `Same character, same style. Change: Peri's head only, smiling, straight front view, centred and filling most of the image, leaf on top. ${PERI}` },
  { name: "icon_peri_shield", dir: ui, from: `${ui}/icon_peri`, prompt: `Exactly the same image. Only change: a small green shield with a white leaf on it sits at the lower right of her face. ${PERI}` },
  { name: "icon_bg", dir: ui, prompt: `A completely empty, smooth, plain light pear green clay surface filling the whole image, a soft lighter glow in the centre, one single colour, no objects, no shapes, no dots, ${NO_TEXT}.` },
  peri("splash_peri", ui, "waves hello with both hands raised, a big warm smile."),
  peri("store_banner", ui, "stands waving in the middle of her world: soft green clay hills, a little village, a sunny sky with puffy clouds and floating leaves; a wide landscape scene.", "assets/peri/peri_calm", WIDE),
];

// Round 6 (docs/IMAGENES_RONDA6.md). The palette made every object sprinkled with dots, so objects say "smooth" now;
// anything white or very light goes on a sky blue background, which cutout.py can take away (cream can't).
const SKY = "plain flat light sky blue background (#BFE3F7), no floor shadow";
const THING = (bg = CREAM) =>
  `Soft 3D clay render of one single object, isolated and centered, straight front view, no character, ${NO_TEXT}, ` +
  `smooth clean clay surfaces without sprinkles or dots, ${bg}, tactile matte plasticine material, gentle studio lighting.`;
const thing = (name: string, dir: string, what: string, bg = CREAM, size?: string) => ({ name, dir, size, prompt: `${what} ${THING(bg)}` });
// A move as frames of one picture, as for Peri's frames in sprites.ts.
const moving = (name: string, dir: string, from: string, n: number, of: number, move: string, change: string, size?: string, style = PERI) =>
  ({ name, dir, from, size, prompt: `Exactly the same character and style, same size in the frame, same framing and same camera. This is frame ${n} of ${of} of ${move}. Change: ${change} ${style}` });
const frames = (group: string, dir: string, from: string, move: string, changes: string[], size?: string, style = PERI) =>
  changes.map((c, i) => moving(`${group}_${String(i + 1).padStart(2, "0")}`, dir, from, i + 1, changes.length, move, c, size, style));

LOTES["27"] = [
  thing("icon_tool_picture", ui, "A clay magnifying glass with a coral handle held over a small square photo of a green hill and a sun."),
  thing("icon_tool_checkup", ui, "A sky blue clay phone with a small green shield on its blank screen and a grey stethoscope curled around it."),
  thing("icon_tool_traps", ui, "A shiny clay fishing hook with a little coral paper envelope hanging on it as bait."),
  thing("icon_tool_chat", ui, "A white clay speech bubble with three green dots in a row inside it.", SKY),
  thing("icon_tool_mini", ui, "A tiny green clay pear with a leaf floating inside a small round sky blue window frame."),
  thing("icon_tool_wardrobe", ui, "A wooden clay coat hanger with a small red bow tie hanging on it."),
  thing("icon_tab_peri", ui, "A small round green clay pear with one leaf on its stem."),
  thing("icon_tab_diary", ui, "An open coral clay diary with blank cream pages and a ribbon bookmark."),
  thing("icon_tab_learn", ui, "A glowing sunny yellow clay light bulb."),
  thing("icon_tab_family", ui, "Two chubby clay hearts, one coral and one pink, side by side holding little hands."),
  { name: "notif_peri_alert", dir: ui, from: "assets/peri/peri_icon", prompt: `Same character, same style. Change: Peri's head under a small open coral umbrella, alert but kind. ${PERI}` },
  { name: "notif_peri_happy", dir: ui, from: "assets/peri/peri_icon", prompt: `Same character, same style. Change: Peri's head with a big happy smile and eyes curved with joy. ${PERI}` },
  { name: "notif_peri_family", dir: ui, from: "assets/peri/peri_icon", prompt: `Same character, same style. Change: Peri's head with a small coral heart floating beside it, a warm smile. ${PERI}` },
  peri("widget_small", ui, "sits happily on a small wooden clay shelf, legs dangling, waving."),
  peri("widget_wide", ui, "sits happily on the left end of a long wooden clay shelf that spans the image, legs dangling, waving; a small plant pot on the right end.", "assets/peri/peri_calm", WIDE),
];

LOTES["28"] = [
  peri("ui_report_sent", ui, "posts a white clay envelope into a small red mailbox, happy."),
  peri("ui_family_yes", ui, "gives a thumbs up, three small coral hearts floating above her."),
  peri("ui_family_waiting", ui, "sits patiently beside a big lilac clay clock with no numbers."),
  peri("ui_family_no", ui, "holds up one open hand like a gentle \"stop\", a kind face."),
  peri("ui_server_down", ui, "fixes a big thick clay cable with a small wrench, a little light blue drop of sweat beside her head, determined."),
  peri("ui_update", ui, "opens a coral clay gift box tied with a green ribbon, delighted."),
  peri("ui_quiz_right", ui, "cheers with both arms up, a big chunky green clay check mark beside her."),
  peri("ui_quiz_wrong", ui, "gives a gentle shrug with a kind smile, a small glowing yellow light bulb above her head."),
  peri("ui_streak_3", ui, "waters a tiny green sprout in a small pot with a little watering can."),
  peri("ui_streak_7", ui, "waters a small flowering plant with pink flowers in a pot with a little watering can."),
  peri("ui_streak_30", ui, "stands proudly beside a little clay tree with small green pears on it."),
  peri("ui_shield_first", ui, "proudly holds up her first small green clay shield with a white leaf on it."),
  peri("ui_shield_10", ui, "stands beside a tower of ten small green clay shields stacked on top of each other, proud."),
  peri("ui_shield_100", ui, "stands on a small clay podium holding up a golden trophy, colourful confetti around."),
  peri("ui_goodbye", ui, "waves goodbye holding a small coral suitcase, a gentle smile."),
];

LOTES["29"] = [
  peri("check_lock", ui, "holds a big sky blue clay padlock in both hands."),
  peri("check_update", ui, "spins a big green clay circular arrow beside her."),
  peri("check_adb", ui, "unplugs a clay cable from a small clay phone, the plug in her hand."),
  peri("check_alerts", ui, "rings a small golden clay bell."),
  peri("check_calls", ui, "answers a sky blue clay phone handset that has a small green shield on it."),
  peri("check_dns", ui, "holds a clay globe with a small green shield in front of it."),
  peri("check_screen", ui, "covers the blank screen of a small clay phone with her hand, a wink."),
  peri("ui_perm_contacts", ui, "holds a small coral clay address book with little hearts on its cover."),
  { name: "ui_perm_overlay", dir: ui, from: "assets/peri/sprites/mini_noface", prompt: `Same character, same style. Change: this small round pear head with a smile floats over the blank screen of a big sky blue clay phone. ${PERI}` },
  peri("ui_perm_usage", ui, "looks at a clay phone whose screen shows colourful blank app tiles, a magnifying glass in her other hand."),
  peri("ui_perm_mic", ui, "leans towards a small grey clay microphone on a stand, listening."),
  peri("ui_perm_location", ui, "holds a coral clay map pin, a small heart beside it."),
  peri("ui_perm_files", ui, "holds a small clay photo of a green hill and a sun."),
  peri("ui_unlock_guide", ui, "opens a small wooden clay door with a big golden key."),
  peri("ui_install_usb", ui, "plugs a small clay phone into a little clay laptop with a cable, focused."),
];

// Three panels per trap, as the ones in generate.ts: how it starts, the trick, how it stops; friendly objects, no fear.
const panel = (name: string, scene: string) =>
  ({ name, dir: ui, from: "assets/peri/peri_calm", prompt: `Same character as in the image, same style, same colours and proportions. A simple, friendly comic panel: ${scene} Soft 3D clay render, rounded shapes, gentle studio lighting, tactile matte plasticine material, ${NO_TEXT}, ${CREAM}.` });
LOTES["30"] = [
  panel("peri_trap_invest_1", "A clay smartphone whose screen shows a green graph line shooting up with golden coins around it; Peri looks at it, impressed."),
  panel("peri_trap_invest_2", "A shiny fishing hook hidden in a pile of golden clay coins. Peri watches with wide, surprised eyes."),
  panel("peri_trap_invest_3", "Peri closes the cover of a clay smartphone with a green shield beside her, calm and smiling."),
  panel("peri_trap_police_1", "A cream clay envelope with a big round red wax seal; Peri looks at it, a little worried."),
  panel("peri_trap_police_2", "A cardboard mask falls off the big red wax seal of a clay envelope, showing a fishing hook underneath. Peri watches, surprised."),
  panel("peri_trap_police_3", "Peri holds a phone to her ear and reads a small plain card in her other hand, confident."),
  panel("peri_trap_job_1", "A clay smartphone with a small brown briefcase full of golden coins coming out of its screen; Peri looks curious."),
  panel("peri_trap_job_2", "The brown briefcase of coins hangs from a fishing line and hook. Peri watches with wide, surprised eyes."),
  panel("peri_trap_job_3", "Peri holds up one open clay hand in a firm, kind \"no\", a green check mark beside her."),
  panel("peri_trap_voice_1", "A clay smartphone with soft sound waves and a coral heart coming out of it; Peri looks moved."),
  panel("peri_trap_voice_2", "The coral heart turns out to be a cardboard mask held up in front of a small grey microphone. Peri watches, surprised."),
  panel("peri_trap_voice_3", "Peri holds a phone to her ear and a small empty speech bubble with a padlock floats beside her: she asks the family's secret word."),
  panel("peri_trap_popup_1", "A clay smartphone with a big red warning triangle popping out of its screen; Peri looks alarmed."),
  panel("peri_trap_popup_2", "A big red triangle sign lifts up like a flap, and a small shiny fishing hook peeks out from behind it. Peri leans back, curious."),
  panel("peri_trap_popup_3", "Peri pushes the red warning away with one hand; the phone's blank screen glows green, safe, and she smiles."),
];

const story6 = (group: string, move: string, changes: string[]) => frames(group, v("peri"), "assets/peri/peri_calm", move, changes, TALL);
LOTES["31"] = [
  ...story6("story_phone_out", "Peri coming out of a phone lying flat on a wooden table", [
    "a big clay smartphone lies flat on a small wooden table; only Peri's leaf pokes out of its blank screen.",
    "Peri's head pokes out of the blank screen of the big clay smartphone lying on the table.",
    "half of Peri's body is out of the phone's blank screen, her arms pushing on its edges.",
    "Peri jumps out and lands on the table next to the big clay smartphone, arms up, happy.",
  ]),
  ...story6("story_shield", "Peri protecting from a clay fishing hook", [
    "a clay fishing hook on a line comes down from the top of the image; Peri looks up at it, her closed coral umbrella in her hand.",
    "Peri opens her coral umbrella halfway above her, the fishing hook coming closer from above.",
    "Peri's coral umbrella is fully open above her and the fishing hook bounces off it, brave face.",
  ]),
  ...story6("story_detective", "Peri the detective with a small brown detective hat", [
    "crouching with a big magnifying glass in front of a square sticker on a grey clay parking meter.",
    "jumping back from the parking meter, pointing at the sticker, surprised.",
  ]),
  ...frames("story_run", v("peri"), "assets/peri/sprites/turn_side_right", "Peri running seen from the side towards the right side of the image", [
    "one leg stretched forward, the other pushing behind, arms pumping, eager face.",
    "both feet off the ground, legs apart the other way, arms pumping.",
  ], TALL),
  ...story6("story_wave", "Peri waving goodbye at sunset with a warm orange light on her", [
    "her hand raised low beside her, a gentle smile.",
    "her hand raised to her shoulder, waving.",
    "her hand raised high above her head, waving, big smile.",
  ]),
  moving("story_bow", v("peri"), "assets/peri/peri_calm", 1, 1, "a final bow on a little stage", "she bows deeply on a small wooden clay stage with a little red velvet curtain behind her.", TALL),
];

const walking = frames("carmen_walk", v("characters"), `${v("characters")}/carmen`, "Carmen walking towards the right side of the image, seen from the side, holding her phone", [
  "contact: her front foot stretched forward, heel on the ground, the back foot on its toes.",
  "down: her weight on the front foot, knees a little bent.",
  "passing: the back leg swings forward under her body.",
  "up: up on the standing leg, the other leg reaching forward.",
], TALL, PERSON);
LOTES["32"] = [
  ...walking,
  carmen("carmen_answer_01", "she lifts her phone towards her ear to answer a call."),
  carmen("carmen_answer_02", "her phone is at her ear and she listens attentively."),
  carmen("carmen_gasp", "one hand on her chest, surprised, eyebrows up, mouth open."),
  carmen("carmen_hangup", "she firmly taps the red button on her phone's screen with one finger, proud."),
  carmen("carmen_laugh_peri", "she laughs, looking down at a small empty spot on the ground beside her on the right side of the image."),
  carmen("carmen_hug_ana", "she hugs her daughter Ana, a woman around forty with shoulder-length dark brown hair, a teal jumper over a white shirt and dark trousers; both smile."),
  carmen("carmen_doze", "she dozes on a small round coral clay sofa, a knitted blanket over her knees, eyes closed, a little smile."),
  pose("ana_run", "ana_base", "she hurries, mid-step, her phone in her hand, worried but focused."),
  pose("ana_relief", "ana_base", "her phone at her ear, she closes her eyes with relief, a hand on her chest."),
  pose("pablo_wave", "pablo_base", "he waves with a big smile."),
  pose("papa_thumbs", "papa_base", "he gives a thumbs up with a warm smile."),
];

const after = (name: string, dir: string, from: string, change: string, bg = CREAM, size?: string) =>
  ({ name, dir, size, from: `${dir}/${from}`, prompt: `Exactly the same objects, same style, same size, same framing. Change: ${change} ${THING(bg)}` });
LOTES["33"] = [
  thing("hook_drop_01", v("props"), "A shiny clay fishing hook on a thin line with a little coral clay envelope hanging on it as bait, coming down from the top edge of the image and hanging high, in the upper part of the image."),
  after("hook_drop_02", v("props"), "hook_drop_01", "the line is longer: the hook and the envelope hang in the middle of the image."),
  after("hook_drop_03", v("props"), "hook_drop_01", "the line is much longer: the hook and the envelope hang near the bottom of the image."),
  after("hook_away", v("props"), "hook_drop_01", "the hook is pulled back up near the top edge, empty: the envelope bait is gone."),
  thing("mask_fall_01", v("props"), "A folded cream paper envelope made of clay, lying flat, with a cheerful clay carnival mask on its front, the mask slipping a little to one side. The envelope is blank: no writing, no symbols."),
  after("mask_fall_02", v("props"), "mask_fall_01", "the mask has fallen off and lies below the envelope, and a plain shiny fishing hook is now visible on the envelope where the mask was."),
  thing("qr_peel_01", v("props"), "A grey and blue clay parking meter on a post; on its front, a square white sticker with a blocky black code pattern, one corner peeling."),
  after("qr_peel_02", v("props"), "qr_peel_01", "the fake sticker has peeled off and hangs from one corner, and under it there is another, clean square code sticker on the meter."),
  thing("phone_ring_01", v("props"), "A sky blue clay smartphone with a blank screen, tilted to the left, ringing, short curved coral vibration lines around it."),
  after("phone_ring_02", v("props"), "phone_ring_01", "the phone is now tilted to the right, the curved vibration lines on the other sides."),
];

LOTES["34"] = [
  ...["entering from the left edge: only its tip shows on the left side of the image.", "covering the left third of the image.", "covering most of the image.", "leaving across the right edge: only its stem end shows on the right side of the image."]
    .map((c, i) => after(`wipe_leaf_0${i + 1}`, v("fx"), "fx_leaf_wipe", `the big pear leaf is sweeping across the frame from left to right, ${c}`, CREAM, TALL)),
  thing("wipe_pear_01", v("fx"), "A big smooth green clay pear with a leaf, rolling across the frame: it is entering from the left edge, lying on its side.", CREAM, TALL),
  ...["in the left third of the image, turned upside down as it rolls.", "in the right third of the image, on its other side as it rolls.", "leaving across the right edge, half of it visible, upright."]
    .map((c, i) => after(`wipe_pear_0${i + 2}`, v("fx"), "wipe_pear_01", `the same pear has rolled further: ${c}`, CREAM, TALL)),
  ...["a small burst of a few pieces close to the centre.", "a medium burst spreading out.", "spread wide across the whole image."]
    .map((c, i) => after(`burst_confetti_0${i + 1}`, v("fx"), "fx_confetti_big", `the clay confetti bursting from the centre is ${c}`)),
  thing("trail_sparkle_01", v("fx"), "A short trail of small white clay four-pointed sparkles curving from the bottom left, just starting.", SKY, TALL),
  after("trail_sparkle_02", v("fx"), "trail_sparkle_01", "the trail of white sparkles is finished, curving all the way across to the top right of the image.", SKY, TALL),
  thing("pop_heart_01", v("fx"), "A big chubby coral clay heart, swelling up, shiny and round."),
  after("pop_heart_02", v("fx"), "pop_heart_01", "the heart has popped into many small coral and pink clay hearts flying outwards."),
];

const PLACE_WIDE = PLACE.replace("tall vertical composition", "wide landscape composition");
const widen = (name: string, from: string) =>
  ({ name, dir: v("backdrops"), from: `${v("backdrops")}/${from}`, size: WIDE, prompt: `Exactly the same place, same objects, same colours, same light and same style, now as a wide landscape image. ${PLACE_WIDE}` });
const wideplace = (name: string, scene: string) => ({ name, dir: v("backdrops"), size: WIDE, prompt: `${scene} ${PLACE_WIDE}` });
LOTES["35"] = [
  ...["village_sunset", "living_day", "living_evening", "kitchen", "street_parking", "ana_home", "peri_workshop", "plaza"].map((n) => widen(`${n}_wide`, n)),
  wideplace("bus_stop_wide", "A clay bus stop with a little roof and a wooden bench on the pavement of a small Spanish town, houses with balconies behind."),
  wideplace("pharmacy_wide", "The front of a small-town pharmacy made of clay, a glowing green cross sign above the door, plants by the window."),
  wideplace("bank_front_wide", "The front of a generic small bank made of clay, columns and a big wooden door, a window, plain walls."),
  wideplace("park_bench_wide", "A wooden park bench under a big tree with orange and yellow autumn leaves, leaves on the grass, a path."),
];

LOTES["36"] = [
  ...["only the big green pear with a leaf stands on the hilltop; the three smaller pears are not there.", "the big pear and the smaller pear with the flat cap stand on the hilltop; the other two are not there.", "the big pear, the pear with the flat cap and the pear with the bow stand on the hilltop; the one with the beanie is not there.", "all four pears stand together on the hilltop, as in the original."]
    .map((c, i) => ({ name: `end_pears_0${i + 1}`, dir: v("cards"), from: `${v("cards")}/card_end_bg`, size: TALL, prompt: `Exactly the same image: same sunset, same hill, same framing and style. Change: ${c} ${CARD}` })),
  card("end_logo_bg", "A soft clay sky at sunset over one gentle green hill, a large calm empty space in the middle for a logo."),
  thing("ad_hook", v("cards"), "A big shiny clay fishing hook on a line with a white chat message bubble hanging on it as bait.", SKY),
  peri("ad_peri_hero", v("cards"), "jumps towards the viewer with her coral umbrella open above her, dynamic, joyful, seen slightly from below; a soft sky blue background with clouds instead of cream.", "assets/peri/peri_calm", TALL),
  card("ad_split", "A smooth clay surface filling the entire image edge to edge, split vertically into two halves: the left half warm peach orange clay, the right half cool sky blue clay, a soft wavy line between them; nothing else, no frame, no border."),
  peri("social_square_peri", v("cards"), "waves in the middle of a cheerful clay background of green hills, a sunny sky and puffy clouds, instead of cream."),
  peri("social_vertical_peri", v("cards"), "waves in the middle of a cheerful clay background of green hills, a sunny sky and puffy clouds, instead of cream.", "assets/peri/peri_calm", TALL),
  card("thumbnail_bg", "A bright cheerful clay background: a clear sky blue sky with a few small puffy clouds and low green hills only along the bottom edge; the left half and the right half of the image are both calm and empty above the hills.", WIDE),
  card("qr_frame", `A chunky green clay frame with rounded corners around an empty plain white square in the middle, small leaves on the corners. ${CREAM}.`, "1024x1024"),
];

async function call(path: string, body: unknown): Promise<Buffer> {
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

const file = (job: Job) => join(root, job.dir, `${job.name}.webp`);

async function make(job: Job) {
  const body = { model: "muse-image-1.0", prompt: job.prompt, n: 1, size: job.size ?? "1024x1024", output_format: "webp" };
  const image = job.from
    ? await call("/images/edits", { ...body, images: [{ image_url: `data:image/webp;base64,${readFileSync(join(root, `${job.from}.webp`)).toString("base64")}` }] })
    : await call("/images/generations", body);
  mkdirSync(join(root, job.dir), { recursive: true });
  writeFileSync(file(job), image);
  console.log(`${job.name}: ${Math.round(image.length / 1024)} KB`);
}

const [lote, ...names] = process.argv.slice(2);
const jobs = LOTES[lote];
if (!jobs) throw new Error(`lote must be one of ${Object.keys(LOTES).join(", ")}`);
if (names[0] === "--paths") {
  console.log(jobs.map(file).join("\n"));
  process.exit(0);
}
const todo = names.length ? jobs.filter((j) => names.includes(j.name)) : jobs.filter((j) => !existsSync(file(j)));

// A few at a time; a picture edited from another one of this run waits for it.
const done = new Set<string>();
const failed: string[] = [];
const pending = [...todo];
const waitsFor = (j: Job) => todo.find((t) => j.from === `${t.dir}/${t.name}`);
async function worker() {
  while (pending.length) {
    const i = pending.findIndex((j) => { const w = waitsFor(j); return !w || done.has(w.name); });
    if (i < 0) { await new Promise((r) => setTimeout(r, 500)); continue; }
    const [job] = pending.splice(i, 1);
    try { await make(job); } catch (e) { failed.push(job.name); console.error(`${job.name}: ${(e as Error).message}`); }
    done.add(job.name);
  }
}
await Promise.all(Array.from({ length: 4 }, worker));

for (const dir of new Set(todo.map((j) => j.dir))) {
  const log = join(root, dir, "prompts.json");
  const previous = existsSync(log) ? (JSON.parse(readFileSync(log, "utf8")) as Record<string, unknown>) : {};
  for (const j of todo) if (j.dir === dir && !failed.includes(j.name)) previous[j.name] = { prompt: j.prompt, from: j.from ?? null, size: j.size ?? "1024x1024" };
  writeFileSync(log, JSON.stringify(previous, null, 2) + "\n");
}
const made = todo.length - failed.length;
console.log(`lote ${lote}: ${made} images (about $${(made * 0.01).toFixed(2)})${failed.length ? `, failed: ${failed.join(" ")}` : ""}`);
