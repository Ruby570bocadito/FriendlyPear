# Estado IMP-2

Tareas hechas por IMP-1 a petición de la persona. Todo probado en el emulador (API 37).

## Hecho (24 h)
- 28 oct, tarde (la persona: ropa descolocada, confianza en familia, seguridad y fondo):
  - Ropa de Peri: `anchors.py` sigue la cabeza fotograma a fotograma (un paso la movía hasta 35 px) y genera los 132 anclajes; `wear.py` quita las patillas de las gafas; nada sobre lo que sujeta en el pecho; caché compartida de imágenes al tamaño justo.
  - «¿Has sido tú?»: `backend/src/checks/` (`/v1/checks`, 15 min, 3 abiertas por móvil, solo con la clave propia), `PeriHub.askFamily` y la tarjeta lila para contestar. Campaña: la misma trampa en dos o más peras en 30 min avisa a todos. Probado con dos emuladores.
  - La pera de quien pide, vestida, en la página de aprobación (`pears/look.ts`, solo la ropa de la tabla) y en `GuardianActivity`. Una petición sin dirección da 400, no 500. `docs/SECURITY.md` al día.
  - Fondo: profundidad (colinas en la bruma, montañas nevadas, faro), luz de plastilina, sombras, paralaje al inclinar; formas y degradados en caché (0,2 ms por fotograma en el hilo de la interfaz).
- 28 oct, antes: armario (11 accesorios de Muse Image), peras de la familia (`/v1/pears`), bienvenida en su mundo, lugar sorpresa, `PeriWorld`, fuentes, apartados, "¿Timo o no?", poses y gestos, 18 bancos de frases.
- Antes: `AccessActivity`, escudos, denuncia, viñetas, avisos de webs, servicios, permisos, familia, Mini Peri, idioma y voz (git).

## Cómo probar (solo builds de depuración)
- Voz: `adb shell "am broadcast -a app.friendlypear.debug.HEAR --es text 'call my daughter'"`.
- Gestos: `input swipe x y x y 1500` (cosquillas), swipe largo (arrastrar), `adb emu sensor set acceleration 28:0:9.8` alternando el signo (agitar); `-6:7.7:0.8` inclina (paralaje).
- `uiautomator dump` necesita `settings put global animator_duration_scale 0` mientras Peri se mueve (volver a 1).
- Dos móviles: AVD `Peri_Family` (5556). Emparejar: `POST /v1/pairing` con el token de admin → `am start -a android.intent.action.VIEW -d <link>` → "Connect". Ajustes con `run-as` y `MSYS_NO_PATHCONV=1`.
- Lugar: `shared_prefs/world.xml` con `place` (SNOW, SEASIDE…). Hora: `adb shell service call alarm 3 s16 Pacific/Auckland` (noche) y volver con `GMT`.
- Peri a Peri con pera: el móvil de Ana tiene el contacto "Mama" (611000111); un móvil de prueba emparejado publica su pera y pide (`POST /v1/approvals`), y `adb emu sms send 611000111 "<enlace>"`.
- Si un emulador no llega a `10.0.2.2` (SocketTimeout), `svc wifi disable` y `enable`.
- Backend: `cd backend; PUBLIC_BASE_URL=http://10.0.2.2:8711 bun src/main.ts`.

## En curso
- Nada.

## Bloqueado
- Nada.

## Peticiones
- SEG: revisar `/v1/checks` (quién ve qué, límites) y la pera en la página de aprobación; `/v1/pears` con el tipo de trampa.

## Propuestas para el roadmap
- `r2-guardian-en-peligro` (ya en el roadmap). Avisos de la familia también con la app cerrada (trabajo periódico).
- `r2-barra-volver`, `r2-hitos`; regrabar los GIF del README con el diseño nuevo.
