# La historia de Peri (borrador para decidir juntos)

> **29 oct:** decidido con la persona; el guion de la demo está en [`GUION.md`](GUION.md).

> Propuesta del 28 oct, noche, para el vídeo de demo (`r2-guion`) y el anuncio. Arte: el mundo de plastilina de los fondos de Muse Image; Peri con sus propios fotogramas de la app; lo que hace la app, **siempre grabación real**. Teaser del primer minuto: `cd video && npx remotion render StoryTeaser out/story-teaser.mp4`.

## Idea

**"Peri se muda al móvil de Carmen."** Una abuela, su pueblo, su familia y una pera que la cuida. Cada vez que pasa algo, la historia se para y se ve la app de verdad haciendo eso mismo, con el rótulo *Real app · recorded on Android*. Nada generado finge lo que la app no hace.

## Demo (3:00)

| # | Tiempo | Escena (mundo de plastilina) | La app de verdad |
|---|---|---|---|
| 1 | 0:00–0:15 | El pueblo al atardecer; Peri llega andando y saluda: *"This is Carmen's village. And this is Peri…"* | — |
| 2 | 0:15–0:45 | El salón: SMS de "número nuevo"; Peri sale del móvil con el paraguas | Aviso, *"Shall I call Ana on her usual number?"* y la llamada |
| 3 | 0:45–1:05 | La tarde: llama un número que dice ser el banco | Aviso antes de descolgar; un código en plena llamada: *"Hang up"* |
| 4 | 1:05–1:25 | La calle: un QR en el parquímetro; Peri con su sombrero de detective | "Look at this picture": *QR trap* |
| 5 | 1:25–1:50 | Casa de Ana: su pera recibe el aviso urgente | Dos móviles: la petición, *"Call Mamá now"* |
| 6 | 1:50–2:05 | "Me he caído" | Cuenta atrás y llamada a Ana |
| 7 | 2:05–2:40 | El taller de Peri: cómo funciona (reglas en el móvil, Muse Spark Open, Muse Spark con herramientas, SAM 3.1, Muse Image, Muse Voice Transcribe) y las cifras de `eval/` | — |
| 8 | 2:40–3:00 | Atardecer: Carmen y las peras de la familia; logo, *Open source · Built with Meta's Muse models* | — |

**Anuncio (30–40 s):** escenas 1 (corta), 2, 4 y 8.

## Lo que hace falta

- **Muse Image:** Carmen en 5–6 poses (ya hay dos, cada pose es una edición de la primera para que sea la misma persona), Ana en 3, y 4–5 lugares nuevos (calle de noche, casa de Ana, taller de Peri, plaza). Unas 25–35 imágenes con repeticiones: **0,25–0,40 $**.
- **Grabaciones (gratis):** las seis de la app con un solo móvil virtual encendido, y Ana marcada como hija en la familia de Mamá para que Peri ofrezca llamarla (hoy no lo ofrece en el emulador por eso).
- **Voz:** la API de Meta no tiene voz. O la tuya, o una voz sintética para el borrador. Subtítulos en inglés y en español siempre.
- **Música:** libre de derechos; hay que descargarla, así que antes se pregunta cuál.

## Fluidez

- Grabar con un solo móvil virtual encendido (los demás en pausa con `emu avd stop`) y a 864×1920: los fotogramas que llegan tarde (p95) bajan de 51 ms a 19–31 ms.
- Render a 60 fotogramas por segundo.
- Las esperas del modelo, como mucho 1,5 s en pantalla, o aceleradas con una marca "×2" visible.
- Arreglado en la app al grabar: Peri ya no se vuelve transparente al hablar tras un gesto, y la medalla de "escudo ganado" ya no tapa el paraguas cuando avisa.

## Para decidir juntos

1. Formato de la demo: vertical como el teaser, u horizontal 16:9 (como pedía `VIDEO.md`; necesita fondos apaisados).
2. Carmen de plastilina (como en el teaser) o la familia solo como peras.
3. Voz: la tuya, una sintética de borrador o solo subtítulos.
4. Idioma: inglés con subtítulos en español para el jurado, y una versión en español aparte.
