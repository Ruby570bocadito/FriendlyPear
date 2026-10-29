# Imágenes y fotogramas, ronda 6: Peri más viva, la app y el vídeo

Lista para el agente de imágenes, como [`PERI_SPRITES.md`](PERI_SPRITES.md) y [`IMAGENES_VIDEO_UI.md`](IMAGENES_VIDEO_UI.md):
todo con **Muse Image** (`muse-image-1.0`, 0,01 $ cada una), en lotes de 15 como mucho, mirando la hoja de contacto de
cada lote antes de pedir el siguiente y **nunca en bucle**. Son **299 imágenes** en 21 lotes (16 a 36): unos **3,5 $** con
repeticiones. Tarea del roadmap: `r6-imagenes`; montarlas en la app y en el vídeo: `r6-fotogramas`.

**Por qué:** a la persona le encanta cuando Peri entra en el inicio y salta («ese tipo de animación me encanta»). Hoy
un salto son 3 imágenes y la entrada es una sola caída: con más fotogramas por movimiento (anticipación, impulso,
vuelo, caída, aplastamiento, rebote), cada gesto se ve como en una película de plastilina. Esta lista da a Peri todos
sus movimientos fotograma a fotograma, más caras, una Mini Peri más viva, las pantallas que faltan en la app y lo que
pide el vídeo para animarse de verdad.

## Lo que hay ya (no se repite)

- **Peri:** sus 9 estados con parpadeo, boca, miradas, pasos y gestos; 25 poses de calma; 49 cuerpos sin cara; 16 ojos
  y 19 bocas por capas; 21 movimientos y 3 vistas de giro (lote 4); 7 cabezas de Mini Peri (lote 5); 16 efectos
  (lote 6); 15 ilustraciones de los apartados (lote 7); 33 viñetas de trampas (11 tipos × 3).
- **Vídeo** (`video/public/`): 16 fondos verticales, 12 poses de Carmen, Ana (3), Pablo y Papá, 11 tarjetas y rótulos,
  15 efectos de montaje y 15 Peris para la historia.
- **App** (`assets/ui/`): icono (con y sin escudo) y su fondo, arranque, banner, insignias bronce/plata/oro, vacíos del
  diario y la familia, sin conexión, 4 permisos, 2 herramientas y 3 de bienvenida.

## Reglas (las de siempre y lo aprendido)

| Regla | Cómo |
|---|---|
| Estilo | El de Peri, al final de cada prompt: `Soft 3D clay render, rounded shapes, gentle studio lighting, tactile matte plasticine material, no text, no letters, centered character with space around it, plain flat warm cream background (#FFF8EC), no floor shadow.` |
| Fotogramas de un movimiento | **Todos son ediciones de la misma imagen de partida** (`peri_calm`, o la base del estado), nunca uno de otro: encadenar ediciones va cambiando a Peri. Prompt: `Exactly the same character, same size in the frame, same framing and same camera. This is frame N of M of a <movimiento> animation. Change: …` |
| La línea del suelo | Sus pies, siempre a la misma altura del encuadre que en `peri_calm`, salvo en el aire, donde la fila dice cuánto sube (p. ej. «her feet a quarter of the image above where they usually are»). Así el código solo cambia la imagen, sin recolocarla. |
| Tamaño | El mismo que `peri_calm`; aplastada o estirada, la plastilina cambia de forma, no de tamaño. |
| Cara | En los fotogramas de calma y contenta, la cara de `peri_calm` salvo que la fila diga otra. |
| Sin cara | Los que llevan ⚪ tienen además su gemelo `_noface` (edición del fotograma: «Peri's face is empty: no eyes, no eyebrows and no mouth; smooth green clay with the same shading. Keep the two rosy cheeks.»). Si el filtro de Muse lo rechaza, la versión suave (`sprites.ts --soft`) que ya funcionó. |
| Lo blanco | El polvo, las nubes, los copos, los destellos y todo lo muy claro, **sobre fondo azul cielo liso (#BFE3F7)**: en el crema no se puede recortar (lo aprendimos en el lote 6). `cutout.py` toma el color del borde. |
| Lo transparente | Nada transparente (la pompa de jabón no se pudo recortar); si hace falta cristal, con un borde de color. |
| Lados | Siempre «on the left/right side of the image», nunca «her left». |
| Nombres | `<grupo>_<movimiento>_<nn>`, con el orden de la secuencia en dos cifras: `anim_jump_01` … `anim_jump_09`. |
| Revisión | Cada secuencia se mira **en orden, como una tira de cómic**, en la hoja de contacto: si un fotograma salta (otro tamaño, otra luz, otra Peri), se repite ese, dos intentos como mucho. |
| Dónde | Peri: `assets/peri/sprites/` y su `prompts.json`; app: `assets/ui/`; vídeo: `video/public/<carpeta>/`. |
| Protocolo | Claim `docs/agentes/claims/r6-imagenes.md`; commits pequeños en español a nombre de la persona, sin nada que identifique a un asistente; nunca `push --force`; el estado en `docs/agentes/estado/SPRITES.md`. |

## Orden

1. **Lote 16 (el salto y la entrada) y parar**: el agente de la app lo monta y la persona lo ve.
2. Lotes 17 a 26 (Peri), 27 a 30 (app) y 31 a 36 (vídeo), en ese orden, avisando en el estado al acabar cada uno.

---

## A. Peri, fotograma a fotograma

### Lote 16: el salto y la entrada (15). Parar al acabar

El salto de 3 imágenes pasa a 9, y la entrada (hoy una caída sin imágenes) a 6. En la app irán a 55–70 ms por
fotograma (`PeriFace`: `MOVES[HOP]` y `enter()`).

| Nombre | Referencia | Change: … |
|---|---|---|
| `anim_jump_01` | `peri_calm` | frame 1 of 9 of a jump: anticipation. Knees bent deep, body squashed wide and low like soft clay, both arms swung back, eyes eager. |
| `anim_jump_02` | `peri_calm` | frame 2 of 9: the push. Legs straightening fast, body stretching upward, arms swinging forward and up. Feet still on the ground. |
| `anim_jump_03` | `peri_calm` | frame 3 of 9: take-off. Toes just leaving the ground, body very stretched tall and thin, arms straight up. Her feet a tenth of the image above where they usually are. |
| `anim_jump_04` | `peri_calm` | frame 4 of 9: rising. In the air, legs tucked under her, arms up, a big happy open smile. Her feet a fifth of the image above where they usually are. |
| `anim_jump_05` | `peri_calm` | frame 5 of 9: the top. Body round and relaxed, arms and legs spread out like a happy star, eyes happily closed in two curves. Her feet a quarter of the image above where they usually are. |
| `anim_jump_06` | `peri_calm` | frame 6 of 9: falling. Legs reaching down, body a little stretched downward, arms up, leaf fluttering up. Her feet a sixth of the image above where they usually are. |
| `anim_jump_07` | `peri_calm` | frame 7 of 9: impact. Feet on the ground, body squashed very wide and low like clay, knees bent, arms out, eyes squeezed for a moment. |
| `anim_jump_08` | `peri_calm` | frame 8 of 9: rebound. A small bounce: body slightly stretched up, arms out for balance, feet just on the ground. |
| `anim_jump_09` | `peri_calm` | frame 9 of 9: settle. Standing, a tiny happy wobble to one side, arms relaxed, big smile. |
| `anim_drop_01` | `peri_calm` | frame 1 of 6 of an entrance from the sky: falling, body stretched long and thin, arms up, leaf blown up above her, surprised happy face. Her feet a third of the image above where they usually are. |
| `anim_drop_02` | `peri_calm` | frame 2 of 6: about to touch the ground, very stretched, legs reaching down. Her feet a tenth of the image above where they usually are. |
| `anim_drop_03` | `peri_calm` | frame 3 of 6: impact. Squashed flat and wide like a pancake of clay, eyes squeezed shut, arms flat out to the sides. |
| `anim_drop_04` | `peri_calm` | frame 4 of 6: bouncing back up, stretched tall, arms up, a little in the air. Her feet a tenth of the image above where they usually are. |
| `anim_drop_05` | `peri_calm` | frame 5 of 6: second small landing, slightly squashed, knees bent, arms out. |
| `anim_drop_06` | `peri_calm` | frame 6 of 6: "ta-da!": standing proud, both arms open wide to the sides, huge smile. |

### Lote 17: más saltos y más entradas (15)

Para que no siempre salte igual: un salto de celebración, uno con vuelta, salir de la tierra como una semilla y bajar
con su paraguas.

| Nombre | Referencia | Change: … |
|---|---|---|
| `anim_cheer_01` | `peri_calm` | frame 1 of 4 of a celebration jump: crouched, both fists clenched at her chest, excited. |
| `anim_cheer_02` | `peri_calm` | frame 2 of 4: jumping up with both fists high in the air, mouth open in a cheer. Her feet a fifth of the image above where they usually are. |
| `anim_cheer_03` | `peri_calm` | frame 3 of 4: at the top, one fist up, the other arm pumping down, eyes closed happily. Her feet a quarter of the image above where they usually are. |
| `anim_cheer_04` | `peri_calm` | frame 4 of 4: landed, knees bent, both fists still up, laughing. |
| `anim_spin_01` | `peri_calm` | frame 1 of 4 of a spinning jump: crouched, turning a little towards the right side of the image. |
| `anim_spin_02` | `turn_side_right` | frame 2 of 4: in the air seen from the side, arms tucked in. Her feet a fifth of the image above where they usually are. |
| `anim_spin_03` | `turn_back` | frame 3 of 4: in the air seen from behind, arms out. Her feet a fifth of the image above where they usually are. |
| `anim_spin_04` | `peri_calm` | frame 4 of 4: landing facing the viewer, arms open, a little dizzy smile. |
| `anim_sprout_01` | `peri_calm` | frame 1 of 3 of an entrance from the ground: only her leaf and the top of her head show above the bottom edge of the image, as if coming out of the soil. |
| `anim_sprout_02` | `peri_calm` | frame 2 of 3: half of her body out of the bottom edge of the image, eyes open, curious. |
| `anim_sprout_03` | `peri_calm` | frame 3 of 3: jumping fully out, in the air, arms up. Her feet a fifth of the image above where they usually are. |
| `anim_float_01` | `peri_calm` | frame 1 of 3 of an entrance floating down holding her small coral umbrella open above her head with one hand, legs dangling, high in the image. Her feet a third of the image above where they usually are. |
| `anim_float_02` | `peri_calm` | frame 2 of 3: lower, still holding the open umbrella, swaying to the left side of the image. Her feet a sixth of the image above where they usually are. |
| `anim_float_03` | `peri_calm` | frame 3 of 3: landed softly on tiptoe, closing the umbrella, smiling. |
| `anim_star_01` | `peri_calm` | a star jump: in the air with arms and legs spread wide, huge smile. Her feet a fifth of the image above where they usually are. |

### Lote 18: andar y correr de frente (15)

Ciclos que se repiten: hoy anda con 4 imágenes de pasos; con 8 anda de verdad, y corre con 6 cuando está contenta.

| Nombre | Referencia | Change: … |
|---|---|---|
| `anim_walk_01` … `anim_walk_08` | `peri_calm` | the 8 frames of a walk cycle in place, facing the viewer: 1 left foot forward touching the ground, 2 weight down on it (body a little lower), 3 right foot passing, lifted, 4 up on the left foot (body a little higher), 5 right foot forward touching the ground, 6 weight down on it, 7 left foot passing, lifted, 8 up on the right foot. Arms swinging opposite to the feet. |
| `anim_run_01` … `anim_run_06` | `peri_calm` | the 6 frames of a happy run in place, facing the viewer: 1 left foot pushing, right knee high, 2 both feet off the ground, 3 right foot landing, 4 right foot pushing, left knee high, 5 both feet off the ground, 6 left foot landing. Arms pumping, big smile. In 2 and 5 her feet a tenth of the image above where they usually are. |
| `anim_skip_01` | `peri_calm` | a happy skip: one knee up, the other foot on tiptoe, arms swinging, eyes closed happily. |

### Lote 19: andar y correr de lado (15)

Para cruzar su colina en el inicio y llegar andando en el vídeo (escena 1). Hacia la derecha; el código los da la
vuelta para ir a la izquierda.

| Nombre | Referencia | Change: … |
|---|---|---|
| `anim_sidewalk_01` … `anim_sidewalk_08` | `turn_side_right` | the 8 frames of a walk cycle seen from the side, walking towards the right side of the image, same steps as `anim_walk` (contact, down, passing, up, for each foot), arms swinging, a friendly smile. |
| `anim_siderun_01` … `anim_siderun_06` | `turn_side_right` | the 6 frames of a run seen from the side, towards the right side of the image: push, both feet off the ground, landing, for each foot. |
| `anim_sidejump_01` | `turn_side_right` | a jump forward seen from the side, both legs stretched out in front, arms back. Her feet a fifth of the image above where they usually are. |

### Lote 20: girar en ocho direcciones y la cabeza (15)

Con los 3/4 que faltan, una vuelta tiene 8 vistas y no 4; y la cabeza dice sí, no y «¿eh?».

| Nombre | Referencia | Change: … |
|---|---|---|
| `turn_threequarter_right` | `peri_calm` | turned three quarters towards the right side of the image: one eye and one cheek clearly visible, the other almost hidden. |
| `turn_threequarter_back_right` | `turn_back` | turned three quarters away, towards the back and the right side of the image: only the edge of one cheek visible. |
| `turn_threequarter_back_left` | `turn_back` | turned three quarters away, towards the back and the left side of the image: only the edge of one cheek visible. |
| `turn_threequarter_left` | `peri_calm` | turned three quarters towards the left side of the image: one eye and one cheek clearly visible, the other almost hidden. |
| `anim_nod_01` … `anim_nod_03` | `peri_calm` ⚪ | a nod "yes": 1 head and upper body tilted forward a little, eyes half closed, 2 back up, 3 forward again, smiling. |
| `anim_shake_01` … `anim_shake_03` | `peri_calm` ⚪ | a gentle "no" shake: 1 body turned a little towards the left side of the image, 2 towards the right side, 3 towards the left again; kind, a little serious face. |
| `anim_tilt_left`, `anim_tilt_right` | `peri_calm` ⚪ | a curious head tilt, the top of her body leaning towards the left (right) side of the image. |
| `anim_lookup_body` | `peri_calm` ⚪ | leaning back a little to look up at the sky, hands behind her back. |
| `anim_lookdown_body` | `peri_calm` ⚪ | leaning forward a little to look down at her feet, curious. |
| `anim_shrug` | `peri_calm` ⚪ | shrugging: both hands up with open palms, shoulders up, "I don't know" with a smile. |

### Lote 21: emociones con todo el cuerpo (15)

Su humor es sobre sí misma (`PERI_PERSONAJE.md`): estornuda, se tropieza, se marea; nunca se ríe de nadie.

| Nombre | Referencia | Change: … |
|---|---|---|
| `anim_laugh_01` … `anim_laugh_03` | `peri_calm` | a belly laugh: 1 both hands on her tummy, eyes squeezed shut, mouth wide open, 2 leaning back laughing, 3 wiping a happy tear with one hand. |
| `anim_cry_01`, `anim_cry_02` | `peri_calm` | 1 sad, big shiny eyes about to cry, hands together, 2 wiping a tear with the back of one hand. |
| `anim_startle` | `peri_calm` | startled: a small jump back, both arms up, eyes wide, mouth a small "o". Her feet a tenth of the image above where they usually are. |
| `anim_determined` | `peri_calm` ⚪ | determined: fists on her hips, chest out, eyebrows firm, a brave little smile. |
| `anim_shy_01`, `anim_shy_02` | `peri_calm` ⚪ | shy: 1 hands together in front of her, cheeks extra rosy, eyes looking down, 2 swaying to the left side of the image. |
| `anim_relieved` | `peri_calm` ⚪ | relieved: one hand on her chest, breathing out, eyes closed, soft smile. |
| `anim_dizzy_01`, `anim_dizzy_02` | `peri_calm` | dizzy: spiral eyes, wobbling to the left (then the right) side of the image, arms out. |
| `anim_sneeze_01`, `anim_sneeze_02` | `peri_calm` | 1 about to sneeze: head back, eyes squeezed, mouth open, 2 "achoo!": head forward, eyes shut, leaf shaking. |
| `anim_oops` | `peri_calm` | tripping a little: one foot caught behind, arms flailing, surprised face, still smiling. |

### Lote 22: los estados, en movimiento (15)

Lo que hace cada estado mientras dura, y el momento de abrir el paraguas cuando empieza a proteger.

| Nombre | Referencia | Change: … |
|---|---|---|
| `anim_listen_01`, `anim_listen_02` | `peri_listening` ⚪ | listening: 1 leaning towards the viewer, one hand cupped beside her head, 2 nodding slightly, eyes attentive. |
| `anim_think_01`, `anim_think_02` | `peri_thinking` ⚪ | thinking: 1 tapping her chin with one finger, eyes up, 2 finger away from the chin, eyes to the side. |
| `anim_fidget_01`, `anim_fidget_02` | `peri_worried` ⚪ | worried: 1 wringing her hands together, 2 hands at her mouth, eyebrows up. |
| `anim_umbrella_open_01` … `_04` | `peri_protective` ⚪ (el 04) | opening her umbrella to protect: 1 the coral umbrella closed, held down beside her, 2 lifting it up, 3 half open above her head, 4 fully open above her, brave face. |
| `anim_umbrella_close_01`, `_02` | `peri_protective` | the danger is over: 1 lowering the umbrella, half closed, relieved, 2 closed, held down beside her, smiling. |
| `anim_dial_01` | `peri_calling` | tapping on her small phone with one finger, concentrating. |
| `anim_watch_01`, `anim_watch_02` | `peri_waiting` ⚪ | waiting: 1 looking at a small watch on her wrist, 2 tapping one foot, looking up patiently. |

### Lote 23: la noche y despertarse (10)

| Nombre | Referencia | Change: … |
|---|---|---|
| `anim_doze_01`, `anim_doze_02` | `peri_calm` | falling asleep standing: 1 eyes half closed, head nodding forward, 2 eyes closed, head down, a tiny smile. |
| `anim_sleep_01`, `anim_sleep_02` | `peri_sleeping` | asleep: 1 breathing in (body a little taller), 2 breathing out (a little lower), peaceful. |
| `anim_yawn_01` … `anim_yawn_03` | `peri_calm` | a yawn: 1 mouth starting to open, eyes closing, 2 big yawn, arms stretched up, 3 rubbing one eye with a hand. |
| `anim_wake_01` … `anim_wake_03` | `peri_sleeping` | waking up: 1 one eye opening, 2 stretching with both arms up, 3 waving good morning with a big smile. |

### Lote 24: cuerpos sin cara que faltan (15)

Los 8 que el filtro rechazó en el lote 3 (contenta y preocupada), con la versión suave, y los gemelos ⚪ de los lotes
20 a 22 que más se verán hablando (los demás, en una segunda vuelta si la persona quiere).

| Nombre | Referencia | Change: … |
|---|---|---|
| `body_worried_noface`, `body_worried_stepright_noface`, `body_worried_steprighthalf_noface` | `peri_worried`, sus pasos | sin cara (versión suave). |
| `body_happy_noface`, `body_happy_gesturea_noface`, `body_happy_gestureb_noface`, `body_happy_stepleft_noface`, `body_happy_steplefthalf_noface` | `peri_happy`, sus gestos y pasos | sin cara (versión suave). |
| `anim_listen_01_noface`, `anim_think_01_noface`, `anim_fidget_01_noface`, `anim_umbrella_open_04_noface`, `anim_watch_01_noface`, `anim_shy_01_noface`, `anim_relieved_noface` | el fotograma | sin cara. |

### Lote 25: más caras por capas (15)

Ediciones de `peri_calm` que solo cambian los ojos o la boca (las reglas de `PERI_SPRITES.md`). Con ellas habla con
todas las bocas del castellano y del inglés, y mira de verdad a cualquier lado (hoy mira moviendo los ojos enteros).

| Nombre | Only change: … |
|---|---|
| `face_mouth_a` | the mouth wide open in an "ah", tongue visible at the bottom. |
| `face_mouth_i` | a narrow smile with the teeth just showing, as when saying "i". |
| `face_mouth_w` | the lips small, round and pushed forward, as when saying "w". |
| `face_mouth_rest` | the mouth relaxed and closed, a soft neutral line. |
| `face_eyes_left`, `face_eyes_right` | both pupils looking to the left (right) side of the image, the eye shape the same. |
| `face_eyes_upleft`, `face_eyes_upright`, `face_eyes_downleft`, `face_eyes_downright` | both pupils looking up (down) and to the left (right) side of the image. |
| `face_eyes_squint` | the eyes narrowed, a little suspicious, like a detective. |
| `face_eyes_dizzy` | each eye a small dark spiral. |
| `face_eyes_cry` | big shiny eyes with tears running down below them. |
| `face_eyes_sparkle` | the eyes with extra white sparkles, delighted. |
| `face_eyes_quarter` | the eyelids a quarter closed: between open and half closed, for a smoother blink. |

### Lote 26: Mini Peri, más cabezas (15)

Ediciones de `mini_noface`, **sin cara y con las dos mejillas bien visibles**: así la app le pone la cara por las
mejillas (`sprites_cut.py mini`). Lo de asomarse vuelve, pero con las mejillas a la vista.

| Nombre | Change: … |
|---|---|
| `mini_tilt_left`, `mini_tilt_right` | the head tilted towards the left (right) side of the image, curious. |
| `mini_nod` | the head tilted forward a little, as in a nod. |
| `mini_lookup` | the head tilted back a little, as when looking up. |
| `mini_peek_left`, `mini_peek_right` | peeking in from the left (right) edge of the image: three quarters of the head visible, both cheeks visible. |
| `mini_peek_bottom` | rising from the bottom edge: the top two thirds of the head visible, both cheeks visible. |
| `mini_umbrella_01` … `_03` | 1 a small closed coral umbrella held up beside the head, 2 half open, 3 fully open above the head (`mini_umbrella` is the open one held to the side). |
| `mini_wave_02`, `mini_wave_03` | the waving hand higher (then tilted the other way), for a three-frame wave with `mini_wave`. |
| `mini_leaf_up` | the leaf blown straight up by the wind. |
| `mini_sleep_02` | the knitted cap slipping to one side, sleepier. |
| `mini_party` | a tiny party hat on top. |

---

## B. La app

### Lote 27: iconos de plastilina (15)

Objetos sin Peri, cuadrados y centrados (con la regla de lo blanco). Para las filas de Ajustes, la barra de abajo, las
notificaciones y un widget.

| Nombre | Qué es |
|---|---|
| `icon_tool_picture` | a clay magnifying glass over a small photo. |
| `icon_tool_checkup` | a clay phone with a small green shield and a stethoscope. |
| `icon_tool_traps` | a clay fishing hook with a little paper envelope on it as bait. |
| `icon_tool_chat` | a clay speech bubble with three dots. |
| `icon_tool_mini` | a tiny clay pear floating in a small round window. |
| `icon_tool_wardrobe` | a clay coat hanger with a small red bow tie on it. |
| `icon_tab_peri`, `icon_tab_diary`, `icon_tab_learn`, `icon_tab_family` | the four tabs in clay: a small pear, an open diary, a light bulb, two hearts holding hands. |
| `notif_peri_alert`, `notif_peri_happy`, `notif_peri_family` | Peri's head for notifications: with her umbrella (a warning), smiling (all good), with a small heart (the family). |
| `widget_small`, `widget_wide` | Peri sitting on a small clay shelf (square, and wide 2:1), for a home-screen widget. |

### Lote 28: momentos de la app (15)

Peri entera en una escena, como las del lote 7.

| Nombre | Qué es |
|---|---|
| `ui_report_sent` | Peri posting a clay envelope into a small mailbox: a report sent. |
| `ui_family_yes` | Peri with a thumbs up, three small hearts above her: the family approved. |
| `ui_family_waiting` | Peri sitting beside a big clay clock, waiting patiently: the family hasn't answered yet. |
| `ui_family_no` | Peri holding up one open hand like a gentle "stop", kind face: the family said no. |
| `ui_server_down` | Peri fixing a big clay cable with a wrench, a little sweat drop: Peri's server can't be reached. |
| `ui_update` | Peri opening a clay gift box with a green ribbon: something new in the app. |
| `ui_quiz_right` | Peri cheering with a big green clay tick beside her: right answer. |
| `ui_quiz_wrong` | Peri with a gentle shrug and a small light bulb: it was a trap, now you know. |
| `ui_streak_3`, `ui_streak_7`, `ui_streak_30` | calm days in a row: Peri watering a sprout (3), a small flowering plant (7), a little tree with pears (30). |
| `ui_shield_first` | Peri receiving her first clay shield, proud. |
| `ui_shield_10` | Peri with ten small shields stacked like a tower. |
| `ui_shield_100` | Peri on a small podium with a golden trophy, confetti. |
| `ui_goodbye` | Peri waving goodbye with a small suitcase: the app's uninstall or reset screen. |

### Lote 29: la revisión del móvil y los permisos, paso a paso (15)

Cada comprobación de «Revisa tu móvil» y cada permiso con su dibujo.

| Nombre | Qué es |
|---|---|
| `check_lock` | Peri holding a big clay padlock: the screen lock. |
| `check_update` | Peri spinning a clay circular arrow: the security update. |
| `check_adb` | Peri unplugging a clay cable from a phone: USB debugging off. |
| `check_alerts` | Peri ringing a small clay bell: the alerts on. |
| `check_calls` | Peri answering a clay phone with a small shield on it: call screening. |
| `check_dns` | Peri holding a clay globe with a green shield: safer web. |
| `check_screen` | Peri covering a phone screen with her hand: nobody watching the screen. |
| `ui_perm_contacts` | Peri holding a small clay address book with hearts. |
| `ui_perm_overlay` | Mini Peri floating over a clay phone screen. |
| `ui_perm_usage` | Peri looking at a clay phone with app tiles, a magnifier in hand. |
| `ui_perm_mic` | Peri leaning towards a clay microphone, listening. |
| `ui_perm_location` | Peri holding a clay map pin, only for the family. |
| `ui_perm_files` | Peri holding a small clay photo. |
| `ui_unlock_guide` | Peri opening a clay door with a big key: the restricted settings guide. |
| `ui_install_usb` | Peri plugging a clay phone into a clay laptop with a cable: installing by USB. |

### Lote 30: las trampas que faltan (15)

Tres viñetas por trampa, como las 11 de ahora (`TrapActivity`): cómo empieza, el truco y cómo se para. Con objetos y
metáforas amables (anzuelos, caretas), nunca con miedo ni con caras de malo.

| Nombre | Qué es |
|---|---|
| `peri_trap_invest_1` … `_3` | an investment "too good to be true": 1 a clay phone showing a graph shooting up, 2 a fishing hook in a pile of clay coins, 3 Peri closing the phone with a shield. |
| `peri_trap_police_1` … `_3` | a fake police or court message: 1 a clay envelope with a big seal, 2 a mask falling off the seal, 3 Peri calling the real number written on a card. |
| `peri_trap_job_1` … `_3` | easy money for a "job": 1 a clay phone offering a briefcase of coins, 2 the briefcase hooked on a fishing line, 3 Peri with a big clay "no" hand. |
| `peri_trap_voice_1` … `_3` | a voice that sounds like family: 1 a clay phone with sound waves and a heart, 2 the heart is a mask on a microphone, 3 Peri asking the family's secret word with a speech bubble. |
| `peri_trap_popup_1` … `_3` | a "your phone has a virus" warning: 1 a clay phone with a big red warning sign, 2 a fishing hook behind the sign, 3 Peri closing the warning, phone safe. |

---

## C. El vídeo

### Lote 31: Peri para la historia, fotograma a fotograma (15)

Para las escenas de [`HISTORIA.md`](HISTORIA.md) animadas en Remotion: lo que hoy es una imagen quieta pasa a ser un
movimiento. Formato vertical como los fondos (`1024x1536` si la API lo acepta; si no, 1024 y el código recorta).

| Nombre | Qué es |
|---|---|
| `story_phone_out_01` … `_04` | Peri coming out of a phone lying on a table (escena 2): 1 only her leaf out of the screen, 2 her head out, 3 half her body out, arms pushing, 4 jumping out next to the phone. |
| `story_shield_01` … `_03` | Peri opening her umbrella in front of Carmen as a clay fishing hook comes down (escena 2): 1 hook coming down, Peri looking up, 2 umbrella half open, 3 umbrella open, the hook bouncing off. |
| `story_detective_01`, `_02` | with her small detective hat (escena 4): 1 crouching with a magnifier in front of a sticker on a parking meter, 2 jumping back, pointing at it. |
| `story_run_01`, `_02` | running to the phone that is ringing (escena 3), seen from the side. |
| `story_wave_01` … `_03` | waving goodbye at sunset (escena 8): hand low, middle, high. |
| `story_bow` | a final bow, as on a stage, a little curtain behind. |

### Lote 32: Carmen y la familia, más poses (15)

Ediciones de `carmen.webp`, `ana_base.webp`, `pablo_base.webp` y `papa_base.webp` (misma persona, ropa y peinado).

| Nombre | Qué es |
|---|---|
| `carmen_walk_01` … `_04` | Carmen walking towards the right side of the image, seen from the side: the four steps of a calm walk. |
| `carmen_answer_01`, `_02` | 1 lifting her phone to answer, 2 the phone at her ear, listening. |
| `carmen_gasp` | a hand on her chest, surprised. |
| `carmen_hangup` | firmly tapping the red button on her phone, proud. |
| `carmen_laugh_peri` | laughing, looking down at a small spot beside her (where Peri goes). |
| `carmen_hug_ana` | hugging Ana, both smiling (Ana as in `ana_base`). |
| `carmen_doze` | dozing on her sofa, a blanket on her knees. |
| `ana_run` | Ana hurrying with her phone in her hand. |
| `ana_relief` | Ana with her phone at her ear, relieved. |
| `pablo_wave` | Pablo waving, a big smile. |
| `papa_thumbs` | Papá with a thumbs up. |

### Lote 33: los timos, en movimiento (10)

| Nombre | Qué es |
|---|---|
| `hook_drop_01` … `_03` | a clay fishing hook with a little clay envelope as bait, lowering from the top of the image: high, middle, near the bottom. |
| `hook_away` | the hook pulled back up and away, the bait gone. |
| `mask_fall_01`, `_02` | a cheerful clay mask on a fake bank letter: 1 slipping, 2 fallen off, a plain hook under it. |
| `qr_peel_01`, `_02` | a fake QR sticker on a clay parking meter: 1 one corner peeling, 2 peeled off, the real one under it. |
| `phone_ring_01`, `_02` | a clay phone ringing, shaking left, then right, small curved lines around it. |

### Lote 34: efectos de montaje en secuencia (15)

Fotogramas para las transiciones y los momentos alegres (con la regla de lo blanco).

| Nombre | Qué es |
|---|---|
| `wipe_leaf_01` … `_04` | a big clay pear leaf sweeping across the frame from left to right: entering, covering a third, covering most, leaving. |
| `wipe_pear_01` … `_04` | a pear rolling across the frame from left to right, turning as it rolls. |
| `burst_confetti_01` … `_03` | clay confetti bursting from the centre: small, medium, spread wide. |
| `trail_sparkle_01`, `_02` | a trail of white clay sparkles curving across the frame, starting and finished. |
| `pop_heart_01`, `_02` | a clay heart swelling, then popping into small hearts. |

### Lote 35: fondos apaisados (12)

Para la demo de 3 min en 16:9 (`1536x1024` si la API lo acepta: probar con uno antes del lote; si no, se queda vertical
como el anuncio). Las mismas escenas que los verticales, con hueco abajo en el centro para Peri y Carmen.

| Nombre | Qué es |
|---|---|
| `village_sunset_wide`, `living_day_wide`, `living_evening_wide`, `kitchen_wide`, `street_parking_wide`, `ana_home_wide`, `peri_workshop_wide`, `plaza_wide` | las versiones apaisadas de los fondos de hoy. |
| `bus_stop_wide` | a clay bus stop with a bench in a small town. |
| `pharmacy_wide` | a clay small-town pharmacy front with a green cross (no letters). |
| `bank_front_wide` | a generic clay bank front, no logo, no letters. |
| `park_bench_wide` | a park bench under a tree, autumn leaves. |

### Lote 36: la tarjeta final y el anuncio (12)

| Nombre | Qué es |
|---|---|
| `end_pears_01` … `_04` | the family's pears appearing one by one on a hill at sunset (one, two, three, four pears with leaves). |
| `end_logo_bg` | a soft clay sky with a hill, space in the middle for the logo. |
| `ad_hook` | a big clay fishing hook with a fake SMS bubble as bait, for the first second of the ad. |
| `ad_peri_hero` | Peri jumping towards the viewer with her umbrella open, dynamic, for the ad's poster. |
| `ad_split` | a clay frame split in two halves (left warm, right cool), for "before / with Peri". |
| `social_square_peri`, `social_vertical_peri` | Peri waving in the middle of a square (1:1) and a vertical (9:16) clay background, for the posts. |
| `thumbnail_bg` | a bright clay background with space on the right for a big Peri and on the left for a title. |
| `qr_frame` | a clay frame around an empty square, for the repo's QR at the end. |

---

## Totales

| Bloque | Lotes | Imágenes |
|---|---|---|
| A. Peri | 16 a 26 | 160 |
| B. La app | 27 a 30 | 60 |
| C. El vídeo | 31 a 36 | 79 |
| **Total** | **21 lotes** | **299** (unos 3,5 $ con repeticiones) |

## Cómo se montarán (para el agente de la app)

| Lote | Dónde |
|---|---|
| 16, 17 | `PeriFace`: el salto (`MOVES[HOP]`) a 9 fotogramas, la entrada (`enter()`) a 6, y saltos de celebración, con vuelta, saliendo de la tierra y bajando con el paraguas como variantes al azar. |
| 18, 19 | Pasos y carrera de los estados (`Idle.STEPS`, `runs`) y un paseo por la colina en el inicio. |
| 20, 21 | Vueltas de 8 vistas, sí y no con la cabeza al hablar, y emociones en sus `PeriMoment`. |
| 22, 23 | Al entrar en cada estado (abrir el paraguas al proteger, cerrarlo al terminar) y sus acciones propias. |
| 24, 25 | `layers.py`: más cuerpos y caras por capas (boca para cada sonido, mirada a 8 lados). |
| 26 | `MiniHead`: asomarse, sí y no, paraguas que se abre, saludo de 3 fotogramas. |
| 27 a 30 | Ajustes, barra de abajo, notificaciones, widget, pantallas de éxito y error, revisión del móvil, permisos y trampas nuevas. |
| 31 a 36 | `video/`: escenas animadas en Remotion, transiciones y la tarjeta final. |
