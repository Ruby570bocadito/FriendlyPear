# Estado IMP-2

Tareas hechas por IMP-1 a petición de la persona. Todo probado en el emulador (API 37).

## Hecho (24 h)
- android-servicios: `AndroidDevice` (TTS cálida, llamadas, SMS, ubicación; un fallo lanza excepción → plan B), `VoiceInput` (2 s de silencio), `MessageListener` (WhatsApp y correo), `SmsReceiver` (`RECEIVE_SMS`: Android 15 oculta los códigos a los lectores de notificaciones), `CallStateWatcher` (sonando/descolgada, nunca el audio), `PeriHub` (un solo agente; registro sin texto ni remitente), `PhoneContacts` ("Laura hija" → relación hija).
  - Momentos 1 y 2 en español e inglés: número nuevo → llamada a la hija; código durante llamada → "cuelga".
- filtro-llamadas: `CallScreener` (`CallScreeningService`, nunca bloquea ni escucha): aviso al sonar y verificación al colgar.
- permisos-progresivos: `config/Need.kt` + `config/PermissionGate.kt`, uno a uno y con la frase de Peri.
  - Primer arranque: agenda → SMS entrantes → estado de llamada → rol de filtro → notificaciones, una sola vez cada uno.
  - Al usarlos: micrófono ("Hablar"), llamar y enviar SMS (`DevicePort.prepare`), ubicación (cuenta atrás de emergencia).
  - Android concede llamar junto con el estado de llamada (mismo grupo). Sin entrada, un diálogo espera; BACK o ENDCALL cuentan como "no".
- te-estan-guiando: `AppWatcher` + `RemoteApps` (15 apps de control remoto, solo visibles por `<queries>`) → aviso, "cuelga" si hay llamada y oferta de avisar al guardián tras un "sí". `BankWatcher` + `BankApps` (22 apps de bancos) solo durante una llamada descolgada de un desconocido, con acceso al uso explicado.
  - Probado con el puente de depuración (`--es installed`, `--es opened`); el vigilante real detectó el cambio de app durante la llamada. Sin apps reales instaladas.
- config-familia: `FamilyConfig` en core (aplicada sobre la agenda) + `FamilyStore` (solo en el móvil) + `FamilyActivity` (guardián, relación, apodos, "hemos acordado una palabra"; la palabra nunca se guarda). Probado: apodo "the neighbour" → Marisa; llamada desconocida → "…ask them for the family word".
- peri-ilustrada: 9 estados con Muse Image (`assets/peri/generate.ts`, base + ediciones; 0,11 $); `core` `Mood` según lo que dice Peri; `ui/peri/PeriFace.kt` con fundido y respiración (sin movimiento si se quitan las animaciones), fondo crema y texto tinta. Probado: tranquila en reposo, protectora con la estafa de Correos.

- peri-fotogramas: `generate.ts frames` → 4 fotogramas por estado (parpadeo, boca, balanceo izq./der.), ediciones de la imagen de cada estado; 36 imágenes, 0,36 $. `PeriFace` con dos capas: balanceo lento, parpadeo cada 3–5 s, boca mientras habla; quieta si se quitan las animaciones.
- peri-mini: `MiniPeriService` (servicio en primer plano tipo micrófono, o `specialUse` sin micrófono) con ventana `TYPE_APPLICATION_OVERLAY`: insignia redonda de 104 dp, bocadillo, se pega al lado más cercano y crece hacia arriba. Botón "Mini Peri" en la app (Peri explica el permiso de mostrarse encima); abrir la app la guarda. `PeriHub` admite varias pantallas (`PeriView`). Probado: tranquila → lupa → paraguas con el bocadillo.

## Cómo probar sin micrófono (solo builds de depuración)
- `adb shell "am broadcast -a app.friendlypear.debug.HEAR --es text 'call my daughter'"`; también `--es installed <paquete>` y `--es opened <paquete>`.
- Tras `adb install -r`: `cmd notification disallow_listener` y `allow_listener app.friendlypear/app.friendlypear.services.MessageListener`.
- Mini Peri: `adb shell appops set app.friendlypear SYSTEM_ALERT_WINDOW allow` y pulsar "Mini Peri". `uiautomator dump` falla mientras Peri se mueve (no queda inactiva): usar capturas.
- Rol de filtro: `adb shell cmd role add-role-holder android.app.role.CALL_SCREENING app.friendlypear`. Acceso al uso: `adb shell appops set app.friendlypear GET_USAGE_STATS allow`.
- En `adb emu gsm`, esperar a que suene (`mCallState=1`) antes de `accept`.
- Backend: `~/.gradle/gradle.properties` con `friendlypear.backendUrl=http://10.0.2.2:8711` y `friendlypear.deviceToken`; `cd backend; bun src/main.ts`.

## En curso
- Nada.

## Bloqueado
- Nada.

## Peticiones
- DIR: `docs/IDEA.md` dice "con un solo permiso"; los SMS usan además `RECEIVE_SMS` por la protección de códigos de Android 15.
- SEG: revisar los permisos del manifiesto (incluido `PACKAGE_USAGE_STATS`), el receptor de depuración exportado y los nuevos de Mini Peri (`SYSTEM_ALERT_WINDOW`, servicio en primer plano de micrófono).

## Propuestas para el roadmap
- Grabar los momentos en un móvil real; comprobar allí los nombres de paquete de bancos y apps de control remoto.
