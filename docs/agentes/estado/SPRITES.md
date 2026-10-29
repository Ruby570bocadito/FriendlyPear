# Estado SPRITES (agente de imágenes)

## Hecho (24 h)
- 29 oct (la persona: ronda 6, `docs/IMAGENES_RONDA6.md`): **lote 16 hecho y parado para verlo montado**. `anim_jump_01…09` y `anim_drop_01…06` en `assets/peri/sprites/` (~0,17 $). Muse no sube a Peri en el aire (se queda en el centro): `assets/peri/ground.py` deja los fotogramas en el suelo justo en la línea de `peri_calm` y escribe en `sprites/anim_offsets.json` cuánto más alto hay que dibujar cada fotograma en el aire (fracción de la altura; p. ej. `anim_jump_05` +0,198, `anim_drop_01` +0,321). Para el agente de la app: `MOVES[HOP]` y `enter()` deben sumar ese desplazamiento.
- 29 oct, madrugada (la persona: imágenes para el vídeo y la interfaz, ronda 5): los 94 de `docs/IMAGENES_VIDEO_UI.md` hechos con `video/scripts/images.ts` (`cd backend; bun ../video/scripts/images.ts <lote> [nombres]`), cada prompt en el `prompts.json` de su carpeta. ~1,01 $ (101 imágenes con repeticiones). Lotes 8, 9, 13, 10, 11, 12 en `video/public/{characters,backdrops,peri,props,fx,cards}/`; 14 y 15 en `assets/ui/` (sin copiar a `res/`). El apaisado 1536x1024 funciona (`card_stage`, `store_banner`). Repetidas: `card_stage`, `phone_table`, `street_parking`, `fx_clay_blob`, `fx_cross_big`, `fx_vibrate`, `scam_qr_sticker`, `icon_bg`, `ui_perm_battery`. Ninguna rechazada por el filtro.
- 28 oct, noche (la persona: «Peri por partes», 183 imágenes con Muse Image): 174 hechas en `assets/peri/sprites/`, una hoja de contacto por lote en `sprites/hojas/<lote>.jpg`, prompts en `sprites/prompts.json`. Script: `assets/peri/sprites.ts` (`cd backend; bun ../assets/peri/sprites.ts <lote> [nombres] [--soft]`), hojas con `assets/peri/sprites_sheet.py`. Coste: ~1,81 $ (181 imágenes cobradas, con repeticiones).
- Lotes 1, 2, 5, 6 y 7 completos. Lote 3: 63 de 71. Lote 4: 44 de 45.
- Repetidas tras revisarlas: `face_eyes_laugh`, `face_eyes_down`, `face_mouth_surprised`, `mini_peek` y `scene_checkup`.

## Para el agente de la app
- `face_eyes_laugh` sale como «> <» al revés («<  >»): se lee como risa, pero puede espejarse.
- `mini_peek`: Peri asoma tras un borde a la derecha, de perfil, sin cara, con los dos coloretes juntos; vale como asomo, pero no lleva cara por capas.
- El fondo de los cuerpos de `worried` es crema tostado, como su imagen original.

- `fx_vibrate` lleva un bulto de colores en el centro tras dos intentos: recortar solo las rayitas de los lados.
- Muchos efectos y timos llevan puntitos de colores de la paleta; quedan bien en plastilina, pero `fx_check_big` y `fx_arrow_curve` son los más cargados.

## Bloqueado
El filtro de Muse (`content_policy_violation`) rechaza estas 9 dos veces, también con `--soft`: `body_calm_stepleft_noface`, `body_worried_noface`, `body_worried_stepright_noface`, `body_worried_steprighthalf_noface`, `body_happy_noface`, `body_happy_gesturea_noface`, `body_happy_gestureb_noface`, `body_happy_steplefthalf_noface`, `move_dance3_noface`. Mientras tanto: el fotograma vecino (`steplefthalf`, `gesturea`…) o borrar la cara a mano.
