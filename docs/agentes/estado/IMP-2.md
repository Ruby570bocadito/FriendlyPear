# Estado IMP-2

Tareas hechas por IMP-1 a petición de la persona. Todo probado en el emulador (API 37).

## Hecho (24 h)
- 28 oct, noche (la persona: ropa, acciones de la familia, notificaciones, estrellas, dos fondos, roadmap de seguridad, qué hacer tras dar datos):
  - Ropa: gafas nuevas de Muse Image sin patillas; cada ojo medido entero (el derecho, más iluminado, se medía por la mitad y todo iba a la izquierda); cada prenda centrada en su eje (`wear.py`); sin hoja bajo los sombreros: 48 cabezas redibujadas por Muse Image (`generate.ts tops`, `tops.py`, `PeriTops.kt`, `BareHead`), también en la página de aprobación (`/p/bare.webp`).
  - Llamadas: con la línea ocupada la llamada espera; botón de colgar en la pausa (`ANSWER_PHONE_CALLS`); «colgar su llamada» para la familia (`/v1/pears/:id/hangup`, `/v1/hangup`, pera con kind `call`); bloquear el número de un timo (`BlockList`, `CallScreener`, Ajustes). Probado con dos emuladores.
  - `FamilyNotes`: noticias de la familia como notificaciones con la app cerrada; «¿has sido tú?» se contesta desde la notificación (probado).
  - Estrellas de plastilina (Muse Image) en `Sparkles.Effect.STARS`; el pueblo con sprites de Muse se probó y se retiró: la persona prefiere el mundo dibujado a mano.
  - Roadmap de seguridad: sección F de `docs/agentes/ROADMAP.md`. Hecho de ella: colgar con aviso, cuidador en llamada (sí en espera), límites por móvil, release sin token (`eval/release_check.py`), apps que controlan la pantalla (`ScreenAccess`; prueba: `--es intruder "paquete|nombre"`), burbuja de Mini Peri en ventana propia; primeros auxilios tras un timo (`Breach`, `AftercareActivity`, `AftercareReminder`; prueba: `--es text 'le he dado los datos de la tarjeta'`); lo guardado en el móvil, sellado con la clave del Keystore (`Sealed`); bloqueo familiar por huellas (`FamilyBlocks`, `/v1/blocks`; forzar la sincronización: `cmd jobscheduler run -f app.friendlypear 4202`).
  - Pulido: dos anclajes por imagen (cara para gafas y cuello, cabeza para sombreros: al mirar a un lado gira la cara); una tarjeta cada vez, fondo más tranquilo, tarjetas con muelle; pruebas de estrés (`eval/stress.py` + monkey) sin fallos; Peri suelta sus fotogramas cuando no se ve (121 → 76 MB).
- 28 oct, tarde: fondo con profundidad y luz de plastilina, «¿has sido tú?», campaña, la pera de quien pide, `docs/SECURITY.md`.
- 28 oct, antes: armario, peras de la familia, bienvenida, `PeriWorld`, fuentes, apartados, juego, poses y gestos, frases de Muse Wonder.

## Cómo probar (solo builds de depuración)
- Voz: `adb shell "am broadcast -a app.friendlypear.debug.HEAR --es text 'call my daughter'"`.
- Gestos: `input swipe x y x y 1500` (cosquillas), `adb emu sensor set acceleration 28:0:9.8` alternando el signo (agitar); `-6:7.7:0.8` inclina. Estrellas: un toque en sus pies.
- Dos móviles: AVD `Peri_Family` (5556) es Mamá, 5554 es Ana. Emparejar con `POST /v1/pairing` (token de admin) y el enlace. Ajustes de prueba con `run-as` y `MSYS_NO_PATHCONV=1`.
- Llamada de un desconocido: `adb -s emulator-5556 emu gsm call 699000111` y `emu gsm accept 699000111`; antes `cmd role add-role-holder android.app.role.CALL_SCREENING app.friendlypear` y `pm grant app.friendlypear android.permission.ANSWER_PHONE_CALLS`.
- Notificaciones: la app de Ana en segundo plano (HOME) y un SMS de número nuevo a Mamá; llegan en un minuto.
- Lugar: `shared_prefs/world.xml` con `place` (SNOW, SEASIDE…). Hora: `adb shell service call alarm 3 s16 Pacific/Auckland` (noche), volver con `GMT`.
- `uiautomator dump` se atasca mientras Peri se mueve: mejor tocar por coordenadas.
- Familia de cuatro: AVDs `Peri_Papa` (5558) y `Peri_Pablo` (5560) además de los dos; `python eval/family4.py`. Email simulado: `am broadcast -a app.friendlypear.debug.HEAR --es email "remitente|texto"` (todo entre comillas en el shell del móvil).
- Backend: `cd backend; PUBLIC_BASE_URL=http://10.0.2.2:8711 bun src/main.ts`.

## En curso
- Nada.

## Bloqueado
- Nada.

## Peticiones
- SEG: `r2s-revision-nuevo` (lo de hoy: preguntas, colgar, notificaciones con acciones, bloqueados).

## Propuestas para el roadmap
- Sección F (seguridad), `r2-guardian-en-peligro`; regrabar los GIF del README con el diseño nuevo.
