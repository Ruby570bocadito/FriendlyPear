# Peri por partes: la lista de imágenes

Encargo para el agente de imágenes. Objetivo: que Peri deje de sentirse estática. Que hable y parpadee en cualquier
pose, que te mire, que reaccione con la cara cuando la tocas, que salude de verdad y que Mini Peri tenga vida. Un
agente genera estas imágenes con **Muse Image**; el agente de la app las monta y las anima en código.
**Nadie empieza hasta que la persona dé el visto bueno a esta lista.**

## Cómo será Peri

Hoy cada fotograma es una imagen entera con la cara pintada: para que hable en una pose, esa pose necesita una imagen
por cada boca. Por eso en las poses especiales (estrellas, medalla, capa…) ni habla ni parpadea, y las gafas dan un
salto raro al saludar.

Peri pasa a ir **por capas**:

| Capa | Qué es | De dónde sale |
|---|---|---|
| Cuerpo sin cara | La misma imagen de siempre (pose, brazos, pies, objetos) con la cara lisa: sin ojos, cejas ni boca; los coloretes se quedan | Lotes 1, 3 y 4 |
| Ojos y cejas | La zona de los ojos de una Peri en la que solo cambian los ojos; el código la recorta | Lotes 1 y 2, y los fotogramas que ya hay |
| Boca | Igual, con la zona de la boca | Lotes 1 y 2, y los fotogramas que ya hay |
| Ropa | Gafas, sombreros, pajarita: ya existen; irán pegados a la cara | — |
| Efectos | Corazones, estrellas, notas, gotitas… | Lote 6 |

Así cualquier cuerpo lleva cualquier cara: boca al ritmo de la voz, parpadeos, mirar tu dedo, reírse, sorprenderse.
Muchas caras ya existen en los fotogramas de la Peri tranquila (ojos abiertos, medio cerrados y cerrados, mirada a
cada lado, guiño, tres bocas, risa, sorpresa, mareo) y se reutilizan: esta lista solo pide lo que falta.

**En la app (28 oct):** `assets/peri/layers.py` recorta las piezas: lo que cambian las imágenes de ojos y las de boca respecto a `peri_calm`, lejos del contorno, y cada píxel para el tipo de imagen que más lo cambia (Muse retoca a veces un ojo en una boca, como en `face_mouth_smirk`). La mirada a la derecha de siempre (`peri_calm_glanceright`) es un giro de cabeza y la de la izquierda apenas mueve el brillo: no son piezas, Peri mira a los lados moviendo los ojos. Los cuerpos sin cara mantienen el encuadre de su imagen (3 px como mucho), así que la cara va donde `PeriAnchors` dice que estaban sus ojos.

**En la app (29 oct):** `assets/peri/sprites_cut.py` recorta a su tamaño los efectos (lote 6, 256 px como mucho), las
ilustraciones de los apartados (lote 7, 512 px) y los movimientos (lote 4, 1024 px, como sus fotogramas). Los efectos
blancos (polvo, copo, destello) y la pompa de jabón se confunden con el fondo crema y no se usan. Las cabezas de Mini
Peri (lote 5) no tienen cara: esperan su colocación de la cara.

## Reglas para todas las imágenes

- **Modelo:** `muse-image-1.0`. `POST {META_API_BASE}/images/edits` con la imagen de referencia de cada fila
  (`/images/generations` en el lote 6), `size: "1024x1024"`, `output_format: "webp"`, `n: 1`. Se ejecuta desde
  `backend/` para que Bun cargue `backend/.env` (`META_API_KEY`); la clave nunca se escribe ni se enseña.
- **Script propio:** `assets/peri/sprites.ts`, copiando `call()` de `assets/peri/generate.ts`. Guarda en
  `assets/peri/sprites/<nombre>.webp` y apunta cada prompt en `assets/peri/sprites/prompts.json` (nombre → prompt).
  **No** copia nada a `android/` ni toca ningún `.kt` o `res/`: montar las imágenes es trabajo del agente de la app.
- **Estilo**, al final de cada prompt:
  `Soft 3D clay render, rounded shapes, gentle studio lighting, tactile matte plasticine material, no text, no letters, centered character with space around it, plain flat warm cream background (#FFF8EC), no floor shadow.`
- **Ediciones de Peri:** empiezan por `Exactly the same image: same character, same pose, same framing, same size and position, same background and props.`
  y siguen con `Only change: ` y el texto de la fila. En las de ojos se añade `The mouth, the cheeks and everything else stay exactly the same.`;
  en las de boca, `The eyes, the cheeks and everything else stay exactly the same.`
- **Lados:** siempre "on the left/right side of the image", nunca "her left".
- **Lotes de 15 como mucho.** Al acabar cada uno: una hoja de contacto (`assets/peri/sprites/hojas/<lote>.jpg`),
  mirarla imagen a imagen y repetir solo las que fallen, dos intentos como mucho. Nunca en bucle. Apuntar el coste.
- **Coste:** 0,01 $ por imagen; las 183, unos 2–2,5 $ con repeticiones.
- **Protocolo:** claim en `docs/agentes/claims/r4-sprites.md`; commits pequeños en español (`feat(peri): …`) a nombre
  de la persona, sin «Co-Authored-By» ni nada que identifique a un asistente; nunca `push --force`.

**Una imagen vale si** es Peri (misma pera, mismo verde, misma hoja y tallo, mismos ojos marrón oscuro con brillo
blanco, mismo tamaño en el encuadre), con fondo crema liso, sin letras ni números, y solo cambia lo que dice la fila.
En las «sin cara», la pera queda lisa por delante con el mismo sombreado, sin restos de ojos ni boca.

## Lote 1: prueba (15). Parar al acabar

Con este lote, el agente de la app monta una Peri por capas y la persona la ve antes de seguir.

| Nombre | Referencia | Only change: … |
|---|---|---|
| `body_calm_noface` | `peri_calm` | Peri's face is empty: no eyes, no eyebrows and no mouth; that part of the pear is smooth green clay with the same soft shading as the rest of her body. Keep the two rosy cheeks exactly where they are. |
| `face_eyes_happy` | `peri_calm` | the eyes are two happy upward curves, as when smiling with the eyes. |
| `face_eyes_wide` | `peri_calm` | the eyes are open extra wide and round, with bigger white highlights, and the eyebrows are raised: amazed. |
| `face_eyes_laugh` | `peri_calm` | the eyes are squeezed shut into two curved lines, as when laughing hard. |
| `face_eyes_worried` | `peri_calm` | the eyebrows tilt up in the middle and the eyes look a little worried, still kind. |
| `face_eyes_love` | `peri_calm` | each eye is a glossy coral heart. |
| `face_eyes_stars` | `peri_calm` | each eye is a shiny golden five-pointed star with a small white highlight. |
| `face_eyes_up` | `peri_calm` | both pupils look up, as if at something above her. |
| `face_eyes_down` | `peri_calm` | both pupils look down, as if at her feet. |
| `face_mouth_m` | `peri_calm` | the lips are pressed together in a small closed line, as when saying "m". |
| `face_mouth_ee` | `peri_calm` | the mouth is a wide open smile showing the top teeth, as when saying "ee". |
| `face_mouth_u` | `peri_calm` | the lips are pushed forward in a small round pucker, as when saying "oo". |
| `face_mouth_laugh` | `peri_calm` | the mouth is wide open in a big laugh, with a little pink tongue. |
| `face_mouth_grin` | `peri_calm` | a big closed-teeth grin from cheek to cheek. |
| `face_mouth_surprised` | `peri_calm` | the mouth is a big round open "oh!". |

## Lote 2: el resto de la cara (14)

Todas con referencia `peri_calm`.

| Nombre | Only change: … |
|---|---|
| `face_eyes_wink_left` | the eye on the left side of the image is closed in a happy curve; the other eye stays open. |
| `face_eyes_sleepy` | heavy, half-closed eyelids: sleepy and content. |
| `face_eyes_brave` | the eyebrows are lowered and determined, the eyes bright and focused: brave, ready to protect. |
| `face_eyes_teary` | the eyes are glossy with a small tear at the corner of each: moved, not sad. |
| `face_mouth_f` | the top teeth rest on the lower lip, as when saying "f". |
| `face_mouth_l` | the mouth is open with the tip of the tongue touching the top teeth, as when saying "l". |
| `face_mouth_worried` | a small wavy, worried mouth. |
| `face_mouth_sad` | a small, gentle frown. |
| `face_mouth_tongue` | a playful smile with the tongue sticking out a little. |
| `face_mouth_kiss` | the lips puckered as if blowing a kiss. |
| `face_mouth_whistle` | small round lips, as if whistling a tune. |
| `face_mouth_smirk` | a cheeky smile, one corner higher than the other. |
| `face_mouth_yawn` | a big, tall open yawn. |
| `face_blush_strong` | the cheeks are much rosier and a little bigger, as if shy. |

## Lote 3: cuerpos sin cara de lo que ya hay (71)

El mismo cambio que `body_calm_noface`, sobre cada imagen que ya existe en `assets/peri/`. Nombre:
`body_<estado>[_<fotograma>]_noface` (por ejemplo `body_worried_gesturea_noface` desde `peri_worried_gesturea`).

| Estado | Imágenes de referencia | Cuántas |
|---|---|---|
| calm | `_gesturea`, `_gestureb`, `_stepleft`, `_steplefthalf`, `_stepright`, `_steprighthalf` (la base va en el lote 1) | 6 |
| listening | la base, `_gesturea`, `_gestureb`, `_stepleft`, `_stepright` | 5 |
| thinking, worried, protective, calling, waiting, happy | cada una: la base, `_gesturea`, `_gestureb`, `_stepleft`, `_steplefthalf`, `_stepright`, `_steprighthalf` | 42 |
| sleeping | la base | 1 |
| poses de calm | `_bye`, `_wave1`, `_wave2`, `_hero`, `_medal`, `_clap`, `_party`, `_thumbsup`, `_love`, `_idea`, `_sorry`, `_letter`, `_scarf`, `_butterfly`, `_tea`, `_flower`, `_phone` | 17 |

## Lote 4: movimientos nuevos (45)

Cada movimiento lleva dos imágenes: `move_<nombre>`, edición de `peri_calm` que empieza por
`Same character, same style, same size in the frame. Change: …` (sirve para medir dónde va la cara), y
`move_<nombre>_noface`, edición de la anterior con la cara vacía, como en el lote 3. Son 21 movimientos (42 imágenes)
y 3 vistas de giro.

| Nombre | Change: … |
|---|---|
| `move_wave3` | the arm on the right side of the image is raised to shoulder height with the hand open: the third step of the wave in `peri_calm_wave1` and `peri_calm_wave2`. |
| `move_jump_crouch` | knees bent and body squashed a little, both arms back, about to jump. |
| `move_jump_air` | in the air, both feet tucked off the ground, arms up, body stretched with joy. |
| `move_jump_land` | landing: knees bent, body squashed, arms out for balance. |
| `move_dance1` | dancing: the arm on the left side of the image up, hips swaying to the right. |
| `move_dance2` | dancing: both arms down at her sides, hips swaying to the left. |
| `move_dance3` | dancing: the arm on the right side of the image up, hips swaying to the left. |
| `move_dance4` | dancing: both arms up in a happy "yay". |
| `move_point` | pointing straight at the viewer with one finger, friendly. |
| `move_point_up` | one finger raised, as if saying "listen!" or "I know!". |
| `move_think` | one hand on her chin, thinking; the other hand on her tummy. |
| `move_hips` | both hands on her hips, proud. |
| `move_heart` | both hands together in front of her chest, making a heart shape. |
| `move_hug` | both arms wide open, inviting a hug. |
| `move_bow1` | a small polite bow, leaning a little forward. |
| `move_bow2` | a deeper bow. |
| `move_stretch_up` | both arms stretched up high. |
| `move_stretch_side` | both arms stretched out to the sides. |
| `move_run1` | running on the spot: the foot on the left side of the image forward, arms swinging. |
| `move_run2` | running on the spot: the foot on the right side of the image forward, arms swinging the other way. |
| `move_sit` | sitting on the ground with her legs forward, relaxed, hands resting on the ground. |
| `turn_side_left` | (una sola imagen) Peri seen from the side, facing the left side of the image. |
| `turn_back` | (una sola imagen) Peri seen from behind: the back of the pear, the leaf and the stem, no face. |
| `turn_side_right` | (una sola imagen) Peri seen from the side, facing the right side of the image. |

## Lote 5: Mini Peri (7)

Referencia `peri_icon` (su cabeza de cerca). Todas sin cara, porque la cara va por capas.

| Nombre | Only change: … |
|---|---|
| `mini_noface` | the face is empty, as in lote 3; keep the cheeks. |
| `mini_leaf_left` | (sobre `mini_noface`) the leaf on top bends towards the left side of the image, as in a breeze. |
| `mini_leaf_right` | (sobre `mini_noface`) the leaf bends towards the right side of the image. |
| `mini_peek` | Peri peeks in from the right edge of the image: only her head and one small hand holding the edge show; face empty. |
| `mini_wave` | her head with one small hand waving beside it; face empty. |
| `mini_umbrella` | her head under a tiny coral umbrella held by one small hand; face empty. |
| `mini_sleep` | her head wearing a tiny night cap; face empty. |

## Lote 6: efectos (16)

`/images/generations`, sin referencia, como `generate.ts fx`:
`<objeto>. Soft 3D clay render of one single small object, isolated and centered, straight front view, symmetrical, no character, no face, no text, plain flat warm cream background (#FFF8EC), no floor shadow, tactile matte plasticine material, gentle studio lighting.`

| Nombre | Objeto |
|---|---|
| `fx_heart_coral` | A small chubby coral heart. |
| `fx_heart_pink` | A small chubby soft pink heart. |
| `fx_heart_broken` | A small coral heart cracked in two halves with a little gap. |
| `fx_sparkle` | A small four-pointed twinkle, white with a pale yellow glow. |
| `fx_music_note` | A small green music note (an eighth note). |
| `fx_sweat_drop` | A small light blue drop of sweat. |
| `fx_tear` | A small clear light blue teardrop. |
| `fx_question` | A chunky question mark symbol in soft yellow clay. |
| `fx_exclamation` | A chunky exclamation mark symbol in soft amber clay. |
| `fx_bulb` | A small glowing yellow lightbulb. |
| `fx_confetti_strip` | A small curly strip of confetti in lime green. |
| `fx_confetti_dot` | A small round flat confetti dot in sunny yellow. |
| `fx_dust_puff` | A small soft round puff of cream dust. |
| `fx_leaf` | A single green pear leaf, like the one on Peri's head. |
| `fx_snowflake` | A small chubby white snowflake. |
| `fx_petal` | A single soft pink flower petal. |

## Lote 7: ilustraciones de los apartados (15)

Pegatinas de cabecera, como la del Diario con corazones que tanto gusta. Ediciones de `peri_calm` (con cara) que
empiezan por `Same character, same style. Change: Peri …`

| Nombre | Change: Peri … |
|---|---|
| `scene_diary` | writes in a small diary with a pencil, little coral hearts floating around. |
| `scene_learn` | wears a small graduation cap and holds an open book, small golden stars around. |
| `scene_family` | hugs a big coral heart, small hearts floating around. |
| `scene_settings` | holds a small wrench, a little gear floating beside her, cheerful. |
| `scene_chat` | waves beside two small speech bubbles with tiny hearts in them. |
| `scene_picture` | holds a small camera, with a little flash sparkle. |
| `scene_checkup` | wears a stethoscope around her neck and holds a small green shield. |
| `scene_trophy` | holds up a golden trophy, confetti around. |
| `scene_cheer` | gives a thumbs up with an encouraging smile, a small star beside her. |
| `scene_hello` | waves hello with both arms, a big warm smile. |
| `scene_hearts` | is surrounded by small floating hearts and two tiny leaves, eyes smiling. |
| `scene_relax` | relaxes in a small hammock, eyes happily closed, little hearts floating. |
| `scene_bubble` | is tiny and floats inside a shiny soap bubble. |
| `scene_night` | reads a book under a small warm lamp, cosy. |
| `scene_morning` | stretches happily holding a small cup of tea, a little sun beside her. |

## Totales y orden

| Lote | Imágenes |
|---|---|
| 1. Prueba | 15 |
| 2. El resto de la cara | 14 |
| 3. Cuerpos sin cara | 71 |
| 4. Movimientos nuevos | 45 |
| 5. Mini Peri | 7 |
| 6. Efectos | 16 |
| 7. Ilustraciones | 15 |
| **Total** | **183** |

Orden: el lote 1, **parar**, la persona ve la Peri por capas montada en la app, y después los lotes 2 a 7 (el 3 y el
4 son los más largos). Al terminar cada lote, el agente de imágenes lo dice en `docs/agentes/estado/` para que el de
la app lo vaya montando.
