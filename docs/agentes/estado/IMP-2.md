# Estado IMP-2

Tareas hechas por IMP-1 a petición de la persona. Todo probado en el emulador (API 37).

## Hecho (24 h)
- Ideas de la persona (28 oct, tarde):
  - Armario de Peri: 11 accesorios de Muse Image (0,13 $), `wear.py` los recorta, `anchors.py` encuentra sus ojos en las 29 ilustraciones y genera `PeriAnchors.kt`; `Outfit` + `OutfitLayer` dentro del rig (gira y bota con ella); `WardrobeActivity` y la percha junto a sus pies.
  - Peras de la familia: `backend/src/pears/` (`PUT /v1/pears/self`, `GET /v1/pears`, solo con la clave propia del móvil, 5 tests), `PearClient`, tarjetas en Familia y aviso en la pantalla de Peri cada 30 s mientras está abierta. Probado con dos emuladores: SMS de timo a "Mamá" → su pera en atención → el Peri de "Ana" lo dice y muestra la tarjeta coral.
  - Bienvenida sobre el mundo de Peri con la página "¡Vísteme a tu gusto!"; lugar sorpresa en Ajustes; Peri se para cuando no se ve.
- Ronda 2, Peri viva (28 oct, mañana): `PeriWorld` (cielos por hora y estado, 5 lugares), fuentes Fredoka y Atkinson, botones y tarjetas de goma, apartados Diario, Aprende y Familia, "¿Timo o no?", 10 poses y gestos (cosquillas, arrastrar, hoja, agitar), 18 bancos de frases de Muse Wonder revisados.
- imp-ronda y antes: `AccessActivity`, escudos, denuncia, viñetas, avisos de webs, servicios Android, permisos, familia, Mini Peri, bienvenida, idioma y voz (historial en git).

## Cómo probar (solo builds de depuración)
- Voz: `adb shell "am broadcast -a app.friendlypear.debug.HEAR --es text 'call my daughter'"`.
- Gestos: `input swipe x y x y 1500` (cosquillas), swipe largo (arrastrar), `input motionevent` sobre la hoja, `adb emu sensor set acceleration 28:0:9.8` alternando el signo (agitar).
- `uiautomator dump` necesita `settings put global animator_duration_scale 0` mientras Peri se mueve (volver a 1).
- Dos móviles: segundo AVD `Peri_Family` (puerto 5556). Emparejar: `POST /v1/pairing` con el token de admin → `am start -a android.intent.action.VIEW -d <link>` → "Connect". Ajustes de prueba con `run-as` y `MSYS_NO_PATHCONV=1` en Git Bash.
- Si un emulador deja de llegar a `10.0.2.2` (SocketTimeout), `svc wifi disable` y `enable` lo arregla.
- Backend: `cd backend; PUBLIC_BASE_URL=http://10.0.2.2:8711 bun src/main.ts`.

## En curso
- Nada.

## Bloqueado
- Nada.

## Peticiones
- SEG: revisar `/v1/pears` (solo nombre, atuendo, papel y calma o atención; atención solo si se comparten las novedades), los registros nuevos (`Diary`) y que el juego nunca abra ni lea enlaces.
- Backend: una petición sin cabecera `Host` da 500 en `new URL(req.url)`; mejor 400.

## Propuestas para el roadmap
- Aviso de la pera de un familiar también con la app cerrada (trabajo periódico o el SMS de siempre).
- `r2-barra-volver`, `r2-hitos`; regrabar los GIF del README con el diseño nuevo.
