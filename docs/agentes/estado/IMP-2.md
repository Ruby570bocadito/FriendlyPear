# Estado IMP-2

Tareas hechas por IMP-1 a petición de la persona. Todo probado en el emulador (API 37).

## Hecho (24 h)
- Ronda 2, Peri viva (28 oct), probado en el emulador en inglés, de día, al amanecer y de noche, y con "quitar animaciones":
  - `PeriWorld`: cielo por la hora (amanecer, día, atardecer, noche con luna, estrellas y estrella fugaz) y por el estado (velo coral y todo quieto en un aviso, confeti contenta, noche dormida); 5 lugares por estación (pradera, huerto, playa, otoño, nieve), uno distinto cada día o fijo en Ajustes; colinas a los pies de Peri; 30 fps y parado si no se ve.
  - `Style.kt`: Fredoka y Atkinson Hyperlegible (OFL, `res/font/`), botones y tarjetas de goma con labio, `pressScale`, `popIn`; `ground()` es el cielo claro de la hora para las pantallas secundarias.
  - `TabBar` + `Pages.kt`: Peri, Diario (`config/Diary.kt`, sin textos ni remitentes), Aprende (trampas, reglas de oro) y Familia (resumen; cambiar abre `FamilyActivity` con el bloqueo).
  - `QuizActivity`: "¿Timo o no?", 16 mensajes por idioma, 4 trampas y 2 de fiar por partida; escudo `LEARNED` una vez al día.
  - Peri: 10 poses nuevas (Muse Image, 0,10 $), cosquillas, arrastrar, frotar la hoja y agitar; 18 bancos de frases propuestos por Muse Wonder (0,36 $) y revisados.
  - Fallos encontrados y arreglados en la prueba: alfa arrastrado que dejaba el cielo transparente, `ObjectAnimator.ofPropertyValuesHolder(null, …)` que caía en la sobrecarga de `ValueAnimator`, y `right` del juego tapado por `View.right` dentro de un `apply`.
- imp-ronda (IMP-2): `AccessActivity` + `UseLog`, `Shields`, `ReportActivity` y `LastScam`, `TrapActivity` (6 × 3 viñetas), avisos de webs y DNS privado guiado, `Haptics`; diálogos con `friendly()`.
- Antes: servicios Android, permisos uno a uno, filtro de llamadas, pausa de seguridad, familia, Mini Peri, Peri en plastilina con ~125 fotogramas, bienvenida, guía de desbloqueo, idioma y voz (historial en git).

## Cómo probar sin micrófono (solo builds de depuración)
- `adb shell "am broadcast -a app.friendlypear.debug.HEAR --es text 'call my daughter'"`; también `--es installed <paquete>` y `--es opened <paquete>`.
- Gestos: `input swipe x y x y 1500` (cosquillas), `input swipe` largo (arrastrar), `input motionevent DOWN/MOVE/UP` sobre la hoja, `adb emu sensor set acceleration 28:0:9.8` alternando el signo (agitar).
- `uiautomator dump` falla mientras Peri se mueve: con `settings put global animator_duration_scale 0` funciona (y prueba el modo sin animaciones); volver a 1 después.
- Permisos de prueba con `pm grant`; rol de filtro: `cmd role add-role-holder android.app.role.CALL_SCREENING app.friendlypear`.
- Backend: `~/.gradle/gradle.properties` con `friendlypear.backendUrl` y `friendlypear.deviceToken`; `cd backend; bun src/main.ts`.

## En curso
- Nada.

## Bloqueado
- Nada.

## Peticiones
- SEG: revisar los registros nuevos en el móvil (`Diary`) y que el juego nunca abra ni lea enlaces; queda `SYSTEM_ALERT_WINDOW` y el servicio de micrófono de Mini Peri.

## Propuestas para el roadmap
- `r2-barra-volver` y `r2-hitos` (añadidas). Regrabar los GIF del README con el diseño nuevo en `r2-grabaciones`.
