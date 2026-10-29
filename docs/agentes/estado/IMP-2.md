# Estado IMP-2

Tareas hechas por IMP-1 a petición de la persona. Todo probado en el emulador (API 37).

## Hecho (24 h)
- 29 oct, tarde 2 (la persona: «el pulido»): la entrada al momento (el arranque espera a sus fotogramas); rendimiento medido (mediana 17 ms, 95 % < 24 ms, 78 MB); accesibilidad al 200 % y sin animaciones comprobada en lo nuevo. Antes: Gradle 8.14.6 por sus arreglos de seguridad (lint había subido a 5 avisos por la versión).
- 29 oct, tarde (la persona: «sí, el resto»): `bun keycopy` con su ensayo, dependencias con huella SHA-256 (Gradle se niega si cambia una) y «Borrar lo que Peri sabe» sin poder quitar a los guardianes. Con la verificación de dependencias, si una tarea nueva descarga algo, hay que regenerar las huellas.
- 29 oct, mediodía 2 (la persona: «sí, la siguiente»): release sin registros y con el informe sensible, comprobado por `release_check.py` (que miraba el APK sin firmar); los 7 componentes exportados probados con intents malos, todo rechazado; la verificación de desarrolladores preparada en INSTALL (falta la cuenta de la persona).
- 29 oct, mediodía (la persona: «sí, la seguridad»): r7-pantalla-segura y r7-toques-tapados con `Guard` (captura negra comprobada en el informe; el toque tapado sin probar con una superposición real); antes, los saltos más lentos y la ronda 7 en el roadmap.
- 29 oct, media mañana 3 (la persona: «celebraciones»): salto de alegría, saltos y entradas al azar (`Drawn`, `variants`) y la vuelta en 8 vistas (lotes 17 y 20); la entrada ya no la corta un salto ni un momento del saludo. Grabado: tierra, cielo y paraguas; salto grande y de estrella.
- 29 oct, media mañana 2 (la persona: «sí, el salto»): el salto en 9 fotogramas y la entrada desde el cielo en 6 (lote 16) en `PeriFace` (`Key`, `play`, `along`), con su ropa (`anchors.py`) y decodificados en un hilo propio; grabado en el emulador: entra sin parpadeo y salta con doble toque.
- 29 oct, media mañana (la persona: sigue con el roadmap): icono y arranque nuevos; las imágenes de la interfaz en su sitio (32 recortes de los lotes 14, 15, 28 y 29 con `sprites_cut.py ui`); hitos con medalla (`Milestone` en core, `Milestones`); obstáculos de Android en la revisión y en el inicio (`Obstacles`); README para el jurado. El agente de imágenes hace commits en este mismo checkout: su commit `b6cb973` se llevó tres borrados que yo tenía preparados (correctos; el icono los completa en `f5d41f7`). Desde ahora, añadir y hacer commit en el mismo comando.
- 28 oct, noche (la persona: ropa, acciones de la familia, notificaciones, estrellas, dos fondos, roadmap de seguridad, qué hacer tras dar datos):
  - Roadmap de seguridad: sección F de `docs/agentes/ROADMAP.md`. Hecho de ella: colgar con aviso, cuidador en llamada (sí en espera), límites por móvil, release sin token (`eval/release_check.py`), apps que controlan la pantalla (`ScreenAccess`; prueba: `--es intruder "paquete|nombre"`), burbuja de Mini Peri en ventana propia; primeros auxilios tras un timo (`Breach`, `AftercareActivity`, `AftercareReminder`; prueba: `--es text 'le he dado los datos de la tarjeta'`); lo guardado en el móvil, sellado con la clave del Keystore (`Sealed`); bloqueo familiar por huellas (`FamilyBlocks`, `/v1/blocks`; forzar la sincronización: `cmd jobscheduler run -f app.friendlypear 4202`); dos guardianes para lo más arriesgado (enlace por guardián, `needed`, `yes`); señales de robo de SIM (`LineWatch`; prueba: `emu gsm voice unregistered` y `gsm data unregistered`, un minuto en debug, o `--es sim new|lost|back`).

## Cómo probar (solo builds de depuración)
- Voz: `adb shell "am broadcast -a app.friendlypear.debug.HEAR --es text 'call my daughter'"`.
- Gestos: `input swipe x y x y 1500` (cosquillas), `adb emu sensor set acceleration 28:0:9.8` alternando el signo (agitar); `-6:7.7:0.8` inclina. Estrellas: un toque en sus pies. `uiautomator dump` se atasca mientras Peri se mueve: mejor por coordenadas.
- Dos móviles: AVD `Peri_Family` (5556) es Mamá, 5554 es Ana. Emparejar con `POST /v1/pairing` (token de admin) y el enlace. Ajustes de prueba con `run-as` y `MSYS_NO_PATHCONV=1`.
- Llamada de un desconocido: `adb -s emulator-5556 emu gsm call 699000111` y `emu gsm accept 699000111`; antes `cmd role add-role-holder android.app.role.CALL_SCREENING app.friendlypear` y `pm grant app.friendlypear android.permission.ANSWER_PHONE_CALLS`.
- Notificaciones: la app de Ana en segundo plano (HOME) y un SMS de número nuevo a Mamá; llegan en un minuto.
- Lugar: `shared_prefs/world.xml` con `place` (SNOW, SEASIDE…). Hora: `adb shell service call alarm 3 s16 Pacific/Auckland` (noche), volver con `GMT`.
- Imagen compartida: copiarla con `run-as app.friendlypear` a `files/` y `am start -a android.intent.action.SEND -t image/png --eu android.intent.extra.STREAM file:///data/data/app.friendlypear/files/x.png -n app.friendlypear/.ui.peri.PictureActivity`; sin red: `svc wifi disable; svc data disable`.
- Familia de cuatro: AVDs `Peri_Papa` (5558) y `Peri_Pablo` (5560) además de los dos; `python eval/family4.py`. Email simulado: `am broadcast -a app.friendlypear.debug.HEAR --es email "remitente|texto"` (todo entre comillas en el shell del móvil).
- Backend: `cd backend; PUBLIC_BASE_URL=http://10.0.2.2:8711 bun src/main.ts`.
- Emuladores: `emulator -avd … -cores 2` (los cuatro a la vez sin latencia); la pantalla se apaga a los 5 min (Ctrl+P la enciende); para medir, `svc power stayon true` y `dumpsys gfxinfo app.friendlypear`.
- Equipo rojo: `cd backend; REDTEAM_DAY=<día> bun ../eval/redteam/redteam.ts --run`, `evalShield` del lote y `python eval/redteam/report.py eval/redteam/<día>`; un lote nuevo se juzga antes de leerlo (es la cifra honesta).
- Dos guardianes: Mamá (5556) tiene a Ana y Papá. Los SMS entre emuladores no llegan: el enlace está en `content://sms/sent` de Mamá y se entrega con `emu sms send 15555215556 "texto"`.

## En curso
- r4-peri-capas: la Peri por capas al inicio, a los estados, con la ropa pegada a la cara y en Mini Peri, cuando la persona la vea en la prueba.

## Bloqueado
- Nada.

## Peticiones
- SEG: `r2s-revision-nuevo` (lo de hoy: preguntas, colgar, notificaciones con acciones, bloqueados).

## Propuestas para el roadmap
- Sección F (seguridad); regrabar los GIF del README con el diseño nuevo; las trampas que faltan (inversión, policía falsa, trabajo, voz clonada) cuando haya una señal que las distinga.
