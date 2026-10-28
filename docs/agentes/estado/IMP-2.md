# Estado IMP-2

Tareas hechas por IMP-1 a petición de la persona. Todo probado en el emulador (API 37).

## Hecho (24 h)
- 28 oct, noche 2 (la persona: permisos deprisa, trampas QR): pantalla quieta mientras se piden los permisos (`settingUp`); QR en «Mira esta imagen» con `QrReader` (ZXing 3.5.4) y `QrTraps` (core), el QR solo empeora el veredicto y vale sin red.
- 28 oct, tarde 2 (la persona: bienvenida, chat, paraguas, permisos negados, vida, más seguridad, APK): sección G del roadmap. Paraguas sin tercera mano (`BUSY_HANDS`); «Pregúntale a Peri» (`Advice`, `ChatActivity`, `PeriHub.ask`); sin micrófono se abre el chat; bienvenida con puntos y progreso; cinco momentos de calma (`CALM_MOMENTS`); revisión de seguridad (`CheckupActivity`); APK de prueba con `-Pfriendlypear.backendUrl=http://192.168.1.131:8711` (nunca se sube). Después: escena estable (bocadillo de altura fija, Peri se desliza), fundidos y medios pasos, herramientas en Aprende; servidor duro (`vault.ts`, `bun rotate`, clave del móvil mensual; la clave de datos está en `~/.friendlypear/data.key`).
- 28 oct, noche (la persona: ropa, acciones de la familia, notificaciones, estrellas, dos fondos, roadmap de seguridad, qué hacer tras dar datos):
  - Ropa: gafas nuevas de Muse Image sin patillas; cada ojo medido entero (el derecho, más iluminado, se medía por la mitad y todo iba a la izquierda); cada prenda centrada en su eje (`wear.py`); sin hoja bajo los sombreros: 48 cabezas redibujadas por Muse Image (`generate.ts tops`, `tops.py`, `PeriTops.kt`, `BareHead`), también en la página de aprobación (`/p/bare.webp`).
  - Llamadas: con la línea ocupada la llamada espera; botón de colgar en la pausa (`ANSWER_PHONE_CALLS`); «colgar su llamada» para la familia (`/v1/pears/:id/hangup`, `/v1/hangup`, pera con kind `call`); bloquear el número de un timo (`BlockList`, `CallScreener`, Ajustes). Probado con dos emuladores.
  - `FamilyNotes`: noticias de la familia como notificaciones con la app cerrada; «¿has sido tú?» se contesta desde la notificación (probado).
  - Estrellas de plastilina (Muse Image) en `Sparkles.Effect.STARS`; el pueblo con sprites de Muse se probó y se retiró: la persona prefiere el mundo dibujado a mano.
  - Roadmap de seguridad: sección F de `docs/agentes/ROADMAP.md`. Hecho de ella: colgar con aviso, cuidador en llamada (sí en espera), límites por móvil, release sin token (`eval/release_check.py`), apps que controlan la pantalla (`ScreenAccess`; prueba: `--es intruder "paquete|nombre"`), burbuja de Mini Peri en ventana propia; primeros auxilios tras un timo (`Breach`, `AftercareActivity`, `AftercareReminder`; prueba: `--es text 'le he dado los datos de la tarjeta'`); lo guardado en el móvil, sellado con la clave del Keystore (`Sealed`); bloqueo familiar por huellas (`FamilyBlocks`, `/v1/blocks`; forzar la sincronización: `cmd jobscheduler run -f app.friendlypear 4202`); dos guardianes para lo más arriesgado (enlace por guardián, `needed`, `yes`); señales de robo de SIM (`LineWatch`; prueba: `emu gsm voice unregistered` y `gsm data unregistered`, un minuto en debug, o `--es sim new|lost|back`).
  - Pulido: dos anclajes por imagen (cara para gafas y cuello, cabeza para sombreros: al mirar a un lado gira la cara); una tarjeta cada vez, fondo más tranquilo, tarjetas con muelle; pruebas de estrés (`eval/stress.py` + monkey) sin fallos; Peri suelta sus fotogramas cuando no se ve (121 → 76 MB).
- 28 oct, tarde y antes: fondo con profundidad, «¿has sido tú?», campaña, `docs/SECURITY.md`; armario, peras de la familia, bienvenida, `PeriWorld`, juego, poses.

## Cómo probar (solo builds de depuración)
- Voz: `adb shell "am broadcast -a app.friendlypear.debug.HEAR --es text 'call my daughter'"`.
- Gestos: `input swipe x y x y 1500` (cosquillas), `adb emu sensor set acceleration 28:0:9.8` alternando el signo (agitar); `-6:7.7:0.8` inclina. Estrellas: un toque en sus pies.
- Dos móviles: AVD `Peri_Family` (5556) es Mamá, 5554 es Ana. Emparejar con `POST /v1/pairing` (token de admin) y el enlace. Ajustes de prueba con `run-as` y `MSYS_NO_PATHCONV=1`.
- Llamada de un desconocido: `adb -s emulator-5556 emu gsm call 699000111` y `emu gsm accept 699000111`; antes `cmd role add-role-holder android.app.role.CALL_SCREENING app.friendlypear` y `pm grant app.friendlypear android.permission.ANSWER_PHONE_CALLS`.
- Notificaciones: la app de Ana en segundo plano (HOME) y un SMS de número nuevo a Mamá; llegan en un minuto.
- Lugar: `shared_prefs/world.xml` con `place` (SNOW, SEASIDE…). Hora: `adb shell service call alarm 3 s16 Pacific/Auckland` (noche), volver con `GMT`.
- `uiautomator dump` se atasca mientras Peri se mueve: mejor tocar por coordenadas.
- Imagen compartida: copiarla con `run-as app.friendlypear` a `files/` y `am start -a android.intent.action.SEND -t image/png --eu android.intent.extra.STREAM file:///data/data/app.friendlypear/files/x.png -n app.friendlypear/.ui.peri.PictureActivity`; sin red: `svc wifi disable; svc data disable`.
- Familia de cuatro: AVDs `Peri_Papa` (5558) y `Peri_Pablo` (5560) además de los dos; `python eval/family4.py`. Email simulado: `am broadcast -a app.friendlypear.debug.HEAR --es email "remitente|texto"` (todo entre comillas en el shell del móvil).
- Backend: `cd backend; PUBLIC_BASE_URL=http://10.0.2.2:8711 bun src/main.ts`.
- Dos guardianes: Mamá (5556) tiene a Ana y Papá. Los SMS entre emuladores no llegan: el enlace está en `content://sms/sent` de Mamá y se entrega con `emu sms send 15555215556 "texto"`.

## En curso
- Nada.

## Bloqueado
- Nada.

## Peticiones
- SEG: `r2s-revision-nuevo` (lo de hoy: preguntas, colgar, notificaciones con acciones, bloqueados).

## Propuestas para el roadmap
- Sección F (seguridad), `r2-guardian-en-peligro`; regrabar los GIF del README con el diseño nuevo.
