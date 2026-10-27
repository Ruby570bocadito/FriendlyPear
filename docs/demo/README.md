# Demo (vídeo de 3 minutos)

Plan completo, anuncio y Higgsfield: [`../VIDEO.md`](../VIDEO.md).

Solo se muestra lo construido durante el hackathon.

1. 0:00–0:20 — El problema: "mamá, he cambiado de número" y una cifra oficial de estafas a mayores (citada).
2. 0:20–1:00 — Peri verifica: ofrece llamar a la hija real; ella insiste; el guardián rechaza y llama. Dos móviles en pantalla.
3. 1:00–1:25 — Código durante llamada: Peri dice "cuelga" y avisa a la familia.
4. 1:25–1:50 — "Mira esta imagen": carta falsa, SAM 3.1 marca el QR, Peri lo explica.
5. 1:50–2:10 — Emergencia: "me he caído", cuenta atrás, llamada al nieto.
6. 2:10–2:45 — Cómo funciona: arquitectura, modelos de Meta y falsas alarmas al mes.
7. 2:45–3:00 — Cierre con la reacción de un familiar mayor.

## Clips grabados (27 oct)

Grabaciones reales de la app en dos emuladores (Android 17), con `screenrecord` de Android y montadas a 1920×1080 con un rótulo en inglés por paso y la nota *"Recorded live on Android emulators; voice orders typed in, as the emulator has no microphone"*. Mamá y Ana con la app en inglés. Sin sonido: la voz de Peri y la música van en el montaje final.

| Clip | Qué se ve | Modelos de Meta |
|---|---|---|
| `m1_newnumber` | SMS "Hi Mum, it's Ana… new number… send me 380" → Peri avisa y ofrece llamar a la Ana de verdad → "yes" → llama, con Mini Peri al lado → escudo | Spark Open (segunda opinión) |
| `m2_call` | Llama un número desconocido → aviso antes de descolgar → "Who did they say they were?" → "the bank" → "What did they ask you for?" → "the code" → "That's the trick… Shall I call Ana?" | — (reglas) |
| `m3_code` | En plena llamada llega un código → "Please hang up right now" | — (regla dura) |
| `m4_picture` | Carta falsa de una empresa inventada → Muse Spark la lee, SAM 3.1 rodea el QR en rojo | Muse Spark, SAM 3.1 |
| `m5_fallen` | "I've fallen" → 10 s para cancelar → SMS con ubicación y llamada a Ana | — |
| `m6_family` | Los dos móviles: el "banco" llama → Mamá pide mandar un código → URGENTE a Ana → el Peri de Ana abre la petición → "Call Mamá now" → el Peri de Mamá no manda nada | — |
| `m7_after` | SMS falso de Royal Mail → "See the trap" (viñetas) → "Report it" (denuncia escrita) | Muse Image (viñetas), Muse Spark |

Los vídeos pesan (unos 30–60 MB cada uno), así que **no van al repo**: están en `dist/demo/` (ignorado por git) y se suben con el vídeo final. Para repetirlos, con los emuladores preparados como en [`../FEATURES.md`](../FEATURES.md):

```
bash docs/demo/tools/moments.sh newnumber        # y call, code, picture, family, after, fallen
python docs/demo/tools/compose.py dist/demo/raw dist/demo
```

Grabándolos salieron dos fallos, ya arreglados: el aviso de caída de un familiar se marcaba como "Peri falso" en el móvil de la hija, y una orden dicha justo después de "¿Qué te pidieron?" se tomaba como respuesta.
