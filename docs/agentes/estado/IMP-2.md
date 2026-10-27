# Estado IMP-2

Tareas hechas por IMP-1 a petición de la persona. Todo probado en el emulador (API 37).

## Hecho (24 h)
- android-servicios: `AndroidDevice` (TTS cálida, llamadas, SMS, ubicación; un fallo lanza excepción → plan B), `VoiceInput` (2 s de silencio), `MessageListener` (WhatsApp y correo), `SmsReceiver` (`RECEIVE_SMS`: Android 15 oculta los códigos a los lectores de notificaciones), `CallStateWatcher` (sonando/descolgada, nunca el audio), `PeriHub` (un solo agente; registro sin texto ni remitente), `PhoneContacts` ("Laura hija" → relación hija).
  - Momentos 1 y 2 en español e inglés: número nuevo → llamada a la hija; código durante llamada → "cuelga". filtro-llamadas: `CallScreener` (`CallScreeningService`, nunca bloquea ni escucha): aviso al sonar y verificación al colgar.
- permisos-progresivos: `config/Need.kt` + `config/PermissionGate.kt`, uno a uno y con la frase de Peri.
  - Primer arranque: agenda → SMS entrantes → estado de llamada → rol de filtro → notificaciones, una sola vez cada uno.
  - Al usarlos: micrófono ("Hablar"), llamar y enviar SMS (`DevicePort.prepare`), ubicación (cuenta atrás de emergencia).
  - Android concede llamar junto con el estado de llamada (mismo grupo). Sin entrada, un diálogo espera; BACK o ENDCALL cuentan como "no".
- te-estan-guiando: `AppWatcher` + `RemoteApps` (15 apps de control remoto, solo visibles por `<queries>`) → aviso, "cuelga" si hay llamada y oferta de avisar al guardián tras un "sí". `BankWatcher` + `BankApps` (22 apps de bancos) solo durante una llamada descolgada de un desconocido, con acceso al uso explicado.
  - Probado con el puente de depuración (`--es installed`, `--es opened`); el vigilante real detectó el cambio de app durante la llamada. Sin apps reales instaladas.
- config-familia: `FamilyConfig` en core (aplicada sobre la agenda) + `FamilyStore` (solo en el móvil) + `FamilyActivity` (guardián, relación, apodos, "hemos acordado una palabra"; la palabra nunca se guarda). Probado: apodo "the neighbour" → Marisa; llamada desconocida → "…ask them for the family word".
- peri-ilustrada: 9 estados con Muse Image (`assets/peri/generate.ts`, base + ediciones; 0,11 $); `core` `Mood` según lo que dice Peri; `ui/peri/PeriFace.kt` con fundido y respiración (sin movimiento si se quitan las animaciones), fondo crema y texto tinta. Probado: tranquila en reposo, protectora con la estafa de Correos.
- peri-fotogramas, peri-pulido y peri-miradas: 97 imágenes de Muse Image en total (0,98 $; ahora también miradas y boca en "o"). Prueba amplia: `docs/agentes/hallazgos/2026-10-27-prueba-amplia.md`: estados, fotogramas por edición de cada estado (`_blinkhalf`, `_blink`, `_talk`, `_talkwide`), saludo, icono, ilustraciones. `cutout.py` quita el fondo crema (1024 px con transparencia). `PeriFace`: inclinación, bote y sombra pintada por estado de ánimo; los fotogramas se cambian sin fundido (el fundido entre poses hacía doble contorno). `Style.kt` (paleta, `Pill`), `BubbleDrawable`, chip de estado, tarjeta de pasos pendientes, tema de borde a borde, icono adaptativo. Probado: saludo, lupa → paraguas con la boca, Mini Peri recortada, familia en tarjetas.
- pausa-segura y vista-guardian (móvil): `PauseOverlay` (pantalla completa sobre la app, "seguir" a los 5 s), `BankWatcher` con `keepGoing`; `SharedNews` + `GuardianClient`; casilla "Que mis guardianes vean las novedades de Peri" y "Lo que ven tus guardianes". Probado: timo → pausa → "Salir de esta app" → la página de Ana muestra 1 aviso y 1 pausa; al quitarlo, el enlace da 404.
- mira-esta-imagen (móvil): `PictureActivity` (selector de fotos o compartir), `MarkedPicture` (anillo rojo y etiqueta), frase propia de Peri por tipo de riesgo; tras un timo se activa la pausa. Con `adb` no se puede compartir una foto (no tiene permiso): probado con el selector.
- ajustes-idioma: `SettingsActivity` + `LocaleManager` (Android 13+; `localeConfig`, `bundle.language.enableSplit = false`); `PeriApp.onConfigurationChanged` → `PeriHub.languageChanged()` cambia voz y agente y Peri lo dice en el nuevo idioma. Probado: español ↔ igual que el móvil.
- peri-mini y mini-en-llamadas: `MiniPeriService` (primer plano tipo micrófono o `specialUse`), ventana `TYPE_APPLICATION_OVERLAY`: solo la pera (128 dp), bocadillo con cola que encoge la letra, sale con un bote, se desliza al lado más cercano, "×" abajo para guardarla, anillo al escuchar; sale sola junto a la llamada y se va al colgar. Abrir la app la guarda.
- movil-real-3: `UnlockActivity` (ajustes restringidos en 3 pasos; `restricted()` por `packageSource` 3/4), sin pedir SMS al arrancar si está bloqueada; "×" de Mini Peri ligera; `Sparkles`, `lookToward`, bocadillo que se escribe; dinero "200 EUR"/"me prestas"; `docs/FEATURES.md`.
- peri-plastilina y peri-viva: Peri en plastilina 3D (base `concepts/concept_clay.webp`), 98 imágenes (0,98 $; `cutout.py` quita el suelo); fotogramas de pasos y acciones solas cada pocos segundos (saltito, pasitos, mirar, estirarse, saludar), peso al lado, aplastamiento al aterrizar, cabeceo al hablar; contenta corre en el sitio. Mini Peri a 100 dp; entrada suave de la pantalla y tarjetas con borde verde. movil-real-2: SMS opcional (pasos con "×"), "La voz de Peri" en Ajustes (`AndroidDevice.voices`/`chooseVoice`), poses idea/perdón/pulgar (`PeriMoment`), 8 diseños en `assets/peri/concepts/`.

## Cómo probar sin micrófono (solo builds de depuración)
- `adb shell "am broadcast -a app.friendlypear.debug.HEAR --es text 'call my daughter'"`; también `--es installed <paquete>` y `--es opened <paquete>`.
- Tras `adb install -r`: `cmd notification disallow_listener` y `allow_listener app.friendlypear/app.friendlypear.services.MessageListener`. Mini Peri: `adb shell appops set app.friendlypear SYSTEM_ALERT_WINDOW allow` y pulsar "Mini Peri". `uiautomator dump` falla mientras Peri se mueve: usar capturas. Rol de filtro: `adb shell cmd role add-role-holder android.app.role.CALL_SCREENING app.friendlypear`. En `adb emu gsm`, esperar a que suene (`mCallState=1`) antes de `accept`. Acceso al uso: `adb shell appops set app.friendlypear GET_USAGE_STATS allow`.
- Backend: `~/.gradle/gradle.properties` con `friendlypear.backendUrl=http://10.0.2.2:8711` y `friendlypear.deviceToken`; `cd backend; bun src/main.ts`.

## En curso
- Nada.

## Bloqueado
- Nada.

## Peticiones
- DIR: `docs/IDEA.md` dice "con un solo permiso"; los SMS usan además `RECEIVE_SMS` por la protección de códigos de Android 15.
- SEG: revisados permisos y receptor de depuración (ahora exige `DUMP`, solo `adb`); HTTP en claro solo en debug; `VoiceInput` no se abre durante una llamada. Queda revisar `SYSTEM_ALERT_WINDOW` y el servicio de micrófono de Mini Peri.

## Propuestas para el roadmap
- Grabar los momentos en un móvil real; comprobar allí los nombres de paquete de bancos y apps de control remoto. iPhone: `docs/IPHONE.md`.
