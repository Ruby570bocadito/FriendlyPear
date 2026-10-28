# Vídeo de demo y anuncio

> **28 oct (D11):** Higgsfield queda descartado. Las escenas de historia se sustituyen por los fondos del mundo de Peri (cielos y lugares que van cambiando, `docs/DISENO.md` §2) detrás de los clips reales. El guion se rehace con la persona en la tarea `r2-guion`; lo que sigue de Higgsfield es histórico.

> **28 oct, noche: anuncio de ejemplo con Remotion.** 20 s en vertical, hecho como código en [`video/`](../video/) con la skill oficial de Remotion como referencia: clips reales del emulador (`video/scripts/record.sh`), tres lugares del mundo de Peri con Muse Image (`video/scripts/backdrops.ts`, 0,03 $), Peri con sus propios fotogramas y las fuentes de la app. Se renderiza en el PC (`cd video && npm run render` → `video/out/ad.mp4`, ignorado por git). La API de Meta no tiene modelo de vídeo ni de voz: la música y la voz van en el montaje final. Remotion es gratis para equipos de hasta 3 personas.

> **La historia:** propuesta de guion con Carmen y Peri en ese mismo mundo de plastilina, y un teaser del primer minuto a 60 fps, en [`HISTORIA.md`](HISTORIA.md). Pendiente de decidir con la persona.

Dos piezas distintas:

| Pieza | Para qué | Duración | Formato |
|---|---|---|---|
| **Vídeo de demo** (obligatorio) | Jurado: uno de los artefactos principales | 3:00 | Horizontal 16:9 |
| **Anuncio** | X (premio Most Viral), galería (Community Vote), redes | 30–45 s | Vertical 9:16 y cuadrado 1:1 |

## 1. Regla de oro

> La guía exige que la demo muestre **solo lo construido durante el hackathon** y que los jueces puedan identificarlo.

- **Todo lo que es producto se graba de verdad:** emulador o móvil real, pantalla y voz reales (grabación con `mobile_start_screen_recording` del MCP, el grabador de Android Studio o el del móvil).
- **Lo generado con IA (Higgsfield)** solo para escenas de **historia**: la abuela que recibe el mensaje, la familia, Peri como personaje en el mundo real, cortinillas.
- **Nunca** un vídeo generado que finja que la app hace algo que no hace.
- Rótulo pequeño en las escenas generadas: *"Escena ilustrativa generada con IA"*.

## 2. Herramienta: Higgsfield MCP

Servidor MCP oficial y remoto de Higgsfield para generar vídeo desde Claude.

| Dato | Valor (según Higgsfield) |
|---|---|
| URL del MCP | `https://mcp.higgsfield.ai` |
| Dónde se añade | Ajustes de conectores de Claude (web, escritorio o Claude Code) |
| Modelos | Veo 3.1, Sora 2, Kling 3.0, Seedance 2.0, Wan 2.6, MiniMax Hailuo y modelos propios |
| Salida | Hasta 4K, clips de hasta 15 s, cualquier formato |
| Skills | Higgsfield ofrece skills y CLI; **la skill concreta la elige Rafa** |

**Conectar:**

1. Claude → Ajustes → Conectores → añadir conector personalizado → `https://mcp.higgsfield.ai`.
2. Iniciar sesión en Higgsfield cuando lo pida y revisar el coste en créditos de cada generación.
3. Probar con un clip corto de Peri antes de generar las escenas finales.

**Coherencia de Peri:** generar primero una imagen de referencia de Peri (Muse Image o Higgsfield) y usarla en todas las escenas como referencia de personaje.

## 3. Vídeo de demo (3:00)

| Tiempo | Escena | Origen |
|---|---|---|
| 0:00–0:15 | Carmen en su salón recibe "Mamá, he cambiado de número" | Higgsfield |
| 0:15–0:20 | Cifra oficial de estafas a mayores (con fuente) | Rótulo |
| 0:20–1:00 | Peri avisa, ofrece llamar a la hija real; ella insiste; el móvil de la hija recibe el MFA; rechaza y llama | **Grabación real** (dos emuladores o dos móviles) |
| 1:00–1:20 | Llamada de número desconocido: "no está en tu agenda"; al colgar, "¿quién decía ser?" | **Grabación real** |
| 1:20–1:40 | Código durante llamada → "Cuelga" + aviso a la familia; o "¿te están guiando?" al abrir el banco | **Grabación real** |
| 1:40–1:55 | "Mira esta imagen": carta falsa, SAM 3.1 marca el QR | **Grabación real** |
| 1:55–2:10 | "Me he caído" → cuenta atrás → llamada al nieto | **Grabación real** |
| 2:10–2:40 | Cómo funciona: arquitectura, modelos de Meta, tabla de resultados del entrenamiento | Diapositivas |
| 2:40–2:55 | Reacción real de un familiar mayor probando la app (con permiso) | Grabación real |
| 2:55–3:00 | Peri saluda: "FriendlyPear. Que nadie te engañe." | Higgsfield + logo |

## 4. Anuncio (30–45 s)

| Tiempo | Escena | Origen |
|---|---|---|
| 0–3 s | Gancho: el móvil vibra, "Abuela, soy Pablo, necesito dinero" | Higgsfield |
| 3–8 s | Peri aparece y abre su paraguas: "Ese número no es el de Pablo" | Mezcla: Peri generada + pantalla real |
| 8–18 s | Peri llama al Pablo de verdad; Pablo: "¡Yo no te he escrito!" | Higgsfield + pantalla real |
| 18–25 s | La familia recibe el aviso; todos tranquilos | Higgsfield |
| 25–30 s | "FriendlyPear 🍐 — tu familia, siempre al lado. Open source." + enlace | Rótulo |

Subtítulos siempre (la mayoría ve vídeos sin sonido). Versión en español y en inglés.

## 5. Prompts base para Higgsfield

> Ajustar al modelo y a la skill elegida. Personajes ficticios; nunca personas reales.

- **Peri (referencia):** *"A friendly round green pear mascot with a small leaf, big kind eyes and rosy cheeks, soft 3D animation style, bright cheerful colours (#7CC242 green, #FFC83D yellow), plain cream background."*
- **Gancho:** *"Cosy living room in Spain, warm afternoon light. An elderly woman in her late seventies looks at her smartphone with a worried face as it vibrates. Close-up, cinematic, 9:16."*
- **Peri protege:** *"The friendly green pear mascot pops out of the phone screen and opens a small yellow umbrella over the elderly woman, she smiles with relief, soft 3D animation, bright colours."*
- **Familia:** *"A young man in his twenties answers a phone call and laughs saying he never sent that message, family video-call feeling, warm and bright, 9:16."*

## 6. Publicación (premios extra)

- [ ] Post en X con el anuncio, enlace al repo y a la galería, **antes del 4 nov** (cuenta la interacción hasta el 8 nov).
- [ ] Pedir el voto en la galería a familia y comunidad (Community Vote, hasta el 16 nov).
- [ ] El enlace del post va en la entrega.

## 7. Checklist

- [ ] Imagen de referencia de Peri aprobada
- [x] Grabaciones reales de los 5 momentos (y dos más: la familia con dos móviles, y "ver la trampa" + "denunciarlo"), en `dist/demo/`; ver [`demo/README.md`](demo/README.md)
- [ ] Escenas de Higgsfield generadas y revisadas (sin manos raras ni textos inventados en pantalla)
- [ ] Rótulo "generada con IA" en escenas no reales
- [ ] Música con licencia libre
- [ ] Subtítulos ES/EN
- [ ] Demo de 3:00 exactos o menos, accesible públicamente
- [ ] Anuncio vertical y cuadrado
