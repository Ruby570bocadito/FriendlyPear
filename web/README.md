# FriendlyPear website

The project's landing page: Peri live in her world, the numbers, why, how it works, the *Scam or not?* game, tested moments, the family double-check, the app,
privacy, the six Meta models, and a footer where Peri's eyes follow the cursor.
Next.js (App Router), React, TypeScript and plain CSS, with the app's own fonts (Fredoka and Atkinson Hyperlegible)
and palette (`docs/DISENO.md`).

```bash
npm install
npm run dev
```

Published at https://ruby570bocadito.github.io/FriendlyPear/ from the `gh-pages` branch. To update it, build with the
`/FriendlyPear` base path and put `out/` (plus an empty `.nojekyll`, so `_next/` is served) on that branch:

```bash
npm run build:pages
```

## Peri, live

Peri moves like a Rive character, in code: `app/live-peri.tsx` breathes, leans towards the cursor, blinks, glances
around on her own, and hops with clay stars when tapped. In her calm mood she is a rig (body, two eyes and their
shines, made by `scripts/peri_rig.py` from the app's cut-out drawables), so her eyes follow the cursor; her other moods
use the app's pictures with their blink frames. `app/hero-world.tsx` puts her in her world, with a sky that follows the
visitor's hour, and `app/scam-game.tsx` is the app's *Scam or not?* game, where she reacts to each answer. Every line
she says comes from the app (`app/peri-lines.json`, taken from `android/app/src/main/res/values/strings.xml`).
"Reduce motion" keeps her still.

## Pictures

`public/img/` holds pictures made for the site with Muse Image, as edits of Peri's base so she stays the same
character (`grandma`, `check`, `code`, `family`, `privacy`; prompts in `scripts/generate-images.mjs`), and the
app's own screenshots from `docs/media/` (`screens`, `guardian`, `moods`). Make the Muse ones again with:

```bash
META_API_KEY=... node scripts/generate-images.mjs [name ...]
```

## Peri's gaze

`public/peri-gaze.mp4` is Peri (`assets/peri/peri_calm.webp`) with her eyes going once round a circle, one frame
every 5 degrees, and `app/gaze-frames.json` says which frame looks which way. On a computer the footer seeks to the
frame that looks at the cursor; on a phone it plays in a loop (paused with "reduce motion"). Make both again with:

```bash
python scripts/peri_gaze.py
```

(needs Pillow, numpy, scipy, opencv-python-headless and ffmpeg).
