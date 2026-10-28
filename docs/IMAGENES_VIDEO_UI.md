# Imágenes para el vídeo y la interfaz (ronda 5)

Lista para el agente de imágenes, como [`PERI_SPRITES.md`](PERI_SPRITES.md): todo con **Muse Image**
(`muse-image-1.0`, 0,01 $ cada una), en lotes de 15 como mucho, mirando cada lote (hoja de contacto) antes de pedir
el siguiente y **nunca en bucle**. Unas 110 imágenes con repeticiones: **~1,10 $**. Tarea del roadmap: `r5-imagenes`.

## Para qué

- **El vídeo** ([`HISTORIA.md`](HISTORIA.md), proyecto Remotion en `video/`): la historia de Carmen y Peri en un
  mundo de plastilina. Lo que hace la app **se graba de verdad** (D6); estas imágenes son la historia, los fondos de
  los rótulos y los efectos del montaje. Los textos los pone Remotion con Fredoka, nunca la imagen.
- **La interfaz**: vacíos, bienvenida, permisos, insignias, sin conexión, iconos de las herramientas e icono de la app.

## Reglas para todas

| Regla | Cómo |
|---|---|
| Estilo | El de Peri (`STYLE` de `assets/peri/sprites.ts`): plastilina 3D suave, luz de estudio cálida, material mate, formas redondas; colores vivos de la paleta (verde pera, coral, cielo, lila, sol) |
| Sin texto | Ni letras, ni números, ni marcas reales (ningún banco, app, logotipo o mensajería conocida); las pantallas de móvil, lisas |
| Coherencia | Cada personaje y cada lugar sale de **una** primera imagen; las demás son ediciones de esa («Exactly the same character…»), como Peri. Carmen ya existe: `video/public/characters/carmen.webp` |
| Recortables | Personajes, objetos y efectos: fondo crema liso `#FFF8EC` y sin sombra en el suelo, para `assets/peri/cutout.py` |
| Fondos | Escena completa sin personajes, con hueco abajo en el centro para Peri y Carmen. Vertical `1024x1536` (anuncio y teaser, como los de ahora). Apaisado `1536x1024` para la demo de 3 min **solo si la API lo acepta**: probar con una antes del lote |
| Personas | Personajes inventados (Carmen, Ana, Pablo, Papá), nunca parecidos a gente real; Carmen es lista, activa y con humor, sin estereotipos de edad |
| Timos | Se cuentan con objetos y metáforas amables (un anzuelo, una careta), nunca con miedo ni con caras de malo |
| Dónde | Vídeo en `video/public/<carpeta>/`; interfaz en `assets/ui/` (la app los recorta a `res/drawable-nodpi/`). Cada prompt en el `prompts.json` de su carpeta |
| Generador | Lotes 8 a 15 en `assets/peri/sprites.ts` (o un script hermano en `video/scripts/`), con el mismo registro de prompts y hoja de contacto |

## Lote 8: la familia (15) → `video/public/characters/`

Ediciones de `carmen.webp` (misma persona, ropa y peinado):

| Nombre | Qué es |
|---|---|
| `carmen_phone_doubt` | Mira el móvil con la ceja levantada: le ha llegado algo raro |
| `carmen_worried` | Preocupada, la mano en el pecho |
| `carmen_laugh` | Riéndose a carcajadas |
| `carmen_wave` | Saluda con la mano, contenta |
| `carmen_hug_phone` | Abraza el móvil contra el pecho, aliviada |
| `carmen_calling` | Hablando por el móvil, tranquila |
| `carmen_sofa` | Sentada en el sofá con una manta (para el salón) |
| `carmen_walk` | Andando con el bolso (para la calle) |
| `carmen_point` | Señala algo con el dedo, decidida |
| `carmen_thumbs` | Pulgar arriba |

Familia (la primera de cada uno se genera; sus poses, ediciones):

| Nombre | Qué es |
|---|---|
| `ana_base` | Ana, la hija, unos 40, ropa de trabajo cómoda |
| `ana_alert` | Ana mirando su móvil, seria: le llega el aviso |
| `ana_calling` | Ana llamando, aliviada |
| `pablo_base` | Pablo, el nieto, unos 16, sudadera |
| `papa_base` | El abuelo, con gorra y chaleco |

## Lote 9: lugares (12) → `video/public/backdrops/`

| Nombre | Qué es |
|---|---|
| `village_sunset` | El pueblo de Carmen al atardecer: casas de plastilina, campanario, colinas |
| `village_morning` | El mismo pueblo por la mañana (edición del anterior) |
| `living_day` | El salón de Carmen de día: sofá, mesita, planta, ventana (ya hay `home.webp`: editar desde él) |
| `living_evening` | El mismo salón por la tarde con la lámpara encendida |
| `kitchen` | La cocina de Carmen, con la radio y el frutero |
| `street_parking` | Una calle con un parquímetro (la pegatina del QR va aparte, lote 10) |
| `ana_home` | El salón de Ana en la ciudad, moderno |
| `peri_workshop` | El taller de Peri: engranajes, tubos y pantallas lisas de plastilina, luz cálida |
| `plaza` | La plaza del pueblo con un banco y una fuente |
| `night_window` | La ventana de Carmen de noche, con la luna |
| `phone_hands` | Dos manos de plastilina (las de Carmen) sosteniendo un móvil de frente con la pantalla blanca lisa: ahí va la grabación real |
| `phone_table` | Un móvil boca arriba en la mesa del salón, pantalla blanca lisa |

## Lote 10: los timos, sin miedo (8) → `video/public/props/`

| Nombre | Qué es |
|---|---|
| `scam_hook_message` | Un bocadillo de mensaje colgado de un anzuelo (phishing) |
| `scam_mask_phone` | Un teléfono con un antifaz (vishing: alguien que se hace pasar por otro) |
| `scam_qr_sticker` | Una pegatina de QR con un anzuelo pequeño (la del parquímetro) |
| `scam_parcel` | Una caja de paquete con un signo de interrogación |
| `scam_prize_box` | Una caja de regalo con un anzuelo saliendo del lazo |
| `scam_heart_hook` | Un corazón enganchado a un anzuelo (timo del amor) |
| `scam_remote_hand` | Una mano de plastilina saliendo de una pantalla hacia unas monedas (control remoto) |
| `scam_safe_account` | Una hucha con un candado falso, abierto por detrás («cuenta segura») |

## Lote 11: efectos del montaje (15) → `video/public/fx/`

Recortables, para animarlos en Remotion (entrar rebotando, girar, esparcirse):

| Nombre | Qué es |
|---|---|
| `fx_leaf_wipe` | Una hoja de pera enorme, para la transición entre escenas |
| `fx_clay_blob` | Una mancha redonda de plastilina verde, para tapar y destapar |
| `fx_confetti_big` | Un estallido de confeti de plastilina de colores |
| `fx_hearts_burst` | Un puñado de corazones coral y rosa |
| `fx_sparkle_burst` | Destellos dorados en estrella |
| `fx_shield_glow` | Un escudo de plastilina verde con brillo, para «protegido» |
| `fx_check_big` | Una marca de visto verde, gruesa y redonda |
| `fx_cross_big` | Una cruz coral, gruesa y redonda |
| `fx_arrow_curve` | Una flecha curva de plastilina, para señalar en pantalla |
| `fx_bubble_empty` | Un bocadillo de diálogo blanco vacío, para los subtítulos de Peri |
| `fx_thought_empty` | Una nube de pensamiento vacía |
| `fx_magnifier` | Una lupa de plastilina (el investigador) |
| `fx_padlock` | Un candado de plastilina cerrado |
| `fx_clock` | Un reloj de plastilina (la espera de la familia) |
| `fx_vibrate` | Unas rayitas de vibración, para un móvil que suena |

## Lote 12: rótulos y tarjetas (10) → `video/public/cards/`

| Nombre | Qué es |
|---|---|
| `card_title_bg` | Cielo de plastilina con nubes y hueco grande en el centro, para el título |
| `card_end_bg` | Atardecer con las peras de la familia en una colina, hueco arriba para el logo |
| `card_ribbon` | Una cinta de plastilina horizontal vacía, para nombres y rótulos |
| `card_badge` | Un círculo de plastilina vacío con borde, para números de capítulo |
| `card_stage` | Seis pedestales de plastilina vacíos en fila: los modelos de Meta |
| `card_board` | Una pizarra de plastilina vacía con marco de madera, para las cifras |
| `card_split` | Un fondo partido en dos colores (verde y coral), para «antes / después» |
| `logo_mark` | El símbolo de la app: una pera de plastilina con hoja y un escudo pequeño, sin letras |
| `social_square_bg` | Fondo cuadrado (`1024x1024`) para el post |
| `social_vertical_bg` | Fondo vertical para historias |

## Lote 13: Peri para la historia (12) → `video/public/peri/`

Ediciones de `peri_calm` o de un movimiento del lote 4, mismo estilo:

| Nombre | Qué es |
|---|---|
| `story_suitcase` | Peri llega con una maleta pequeña (se muda al móvil) |
| `story_from_phone` | Peri saliendo de la pantalla de un móvil de plastilina |
| `story_point_screen` | Peri señalando hacia un lado, como presentando una pantalla |
| `story_present` | Brazos abiertos, presentando algo grande |
| `story_hook_magnifier` | Mirando un anzuelo con la lupa |
| `story_hold_phone` | Sosteniendo un móvil grande con las dos manos |
| `story_shield_up` | Levantando un escudo, valiente |
| `story_family_pears` | Las cuatro peras de la familia juntas (Peri y tres más pequeñas con su accesorio) |
| `story_bow` | Una reverencia de despedida |
| `story_sleep_moon` | Dormida sobre una luna de plastilina |
| `story_run_message` | Corriendo con un sobre en la mano |
| `story_heart_family` | Abrazando un corazón grande con los de la familia alrededor |

## Lote 14: la interfaz (15) → `assets/ui/`

| Nombre | Dónde va |
|---|---|
| `ui_empty_diary` | Diario vacío: Peri con un cuaderno en blanco y un lápiz |
| `ui_empty_family` | Familia vacía: Peri esperando con un corazón y un móvil |
| `ui_welcome_home` | Bienvenida 1: Peri llegando a una casita con forma de móvil |
| `ui_welcome_shield` | Bienvenida 2: Peri con el escudo delante de un sobre |
| `ui_welcome_family` | Bienvenida 3: Peri con las peras de la familia |
| `ui_perm_sms` | Permiso de SMS: Peri con un sobre abierto |
| `ui_perm_notifications` | Permiso de notificaciones: Peri con una campanita |
| `ui_perm_calls` | Permiso de llamadas: Peri con un teléfono |
| `ui_perm_battery` | Batería: Peri abrazando una pila llena |
| `ui_offline` | Sin conexión: Peri con un enchufe desconectado, tranquila |
| `ui_badge_bronze` | Insignia de escudo de bronce (el primero) |
| `ui_badge_silver` | Insignia de plata (cinco) |
| `ui_badge_gold` | Insignia de oro (veinte) |
| `ui_tool_game` | Herramienta «Juego de timos»: dos cartas de plastilina, una con anzuelo |
| `ui_tool_ask` | Herramienta «Pregúntale»: un bocadillo con un signo de interrogación |

Para «Mira una imagen», «Revisa tu móvil» y la familia ya valen `scene_picture`, `scene_checkup` y `scene_family`.

## Lote 15: icono y arranque (5) → `assets/ui/`

| Nombre | Qué es |
|---|---|
| `icon_peri` | La cabeza de Peri sonriendo, de frente y centrada, para el icono adaptable |
| `icon_peri_shield` | Variante con un escudo pequeño |
| `icon_bg` | Fondo verde claro liso con un brillo suave |
| `splash_peri` | Peri saludando con las dos manos, para la pantalla de arranque |
| `store_banner` | Peri y su mundo en apaisado, para la ficha del repo y el post |

## Lo que se hace en código, sin imágenes (Remotion)

Transiciones con la hoja y la mancha, rebotes con muelle, partículas con los efectos de arriba y del lote 6,
subtítulos y rótulos en Fredoka, zoom suave sobre los fondos, temblor al avisar, el móvil con la grabación real
dentro de `phone_hands`, y el mundo de Peri de la app (cielos y lugares) como fondo animado. **Música y sonidos** se
descargan: antes se pregunta a la persona cuáles.

## Orden

1. Lote 12 (`logo_mark` y `card_title_bg`): se ve enseguida si el estilo de los rótulos funciona.
2. Lotes 8 y 9: sin Carmen y sin lugares no hay historia.
3. Lote 13, luego 10 y 11: Peri y los efectos de la historia.
4. Lotes 14 y 15: la interfaz y el icono.

Cada lote: hoja de contacto, mirarla, repetir solo las malas y seguir. Si el filtro de Muse rechaza una, se reformula
una vez y, si vuelve a fallar, se apunta y se sigue.
