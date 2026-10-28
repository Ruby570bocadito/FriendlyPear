# Estado SPRITES (agente de imágenes)

## Hecho (24 h)
- 28 oct, noche (la persona: «Peri por partes», 183 imágenes con Muse Image): 174 hechas en `assets/peri/sprites/`, una hoja de contacto por lote en `sprites/hojas/<lote>.jpg`, prompts en `sprites/prompts.json`. Script: `assets/peri/sprites.ts` (`cd backend; bun ../assets/peri/sprites.ts <lote> [nombres] [--soft]`), hojas con `assets/peri/sprites_sheet.py`. Coste: ~1,81 $ (181 imágenes cobradas, con repeticiones).
- Lotes 1, 2, 5, 6 y 7 completos. Lote 3: 63 de 71. Lote 4: 44 de 45.
- Repetidas tras revisarlas: `face_eyes_laugh`, `face_eyes_down`, `face_mouth_surprised`, `mini_peek` y `scene_checkup`.

## Para el agente de la app
- `face_eyes_laugh` sale como «> <» al revés («<  >»): se lee como risa, pero puede espejarse.
- `mini_peek`: Peri asoma tras un borde a la derecha, de perfil, sin cara, con los dos coloretes juntos; vale como asomo, pero no lleva cara por capas.
- El fondo de los cuerpos de `worried` es crema tostado, como su imagen original.

## Bloqueado
El filtro de Muse (`content_policy_violation`) rechaza estas 9 dos veces, también con `--soft`: `body_calm_stepleft_noface`, `body_worried_noface`, `body_worried_stepright_noface`, `body_worried_steprighthalf_noface`, `body_happy_noface`, `body_happy_gesturea_noface`, `body_happy_gestureb_noface`, `body_happy_steplefthalf_noface`, `move_dance3_noface`. Mientras tanto: el fotograma vecino (`steplefthalf`, `gesturea`…) o borrar la cara a mano.
