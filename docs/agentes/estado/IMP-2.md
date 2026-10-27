# Estado IMP-2

## Hecho (24 h)
- android-servicios (lo hizo IMP-1 a petición de la persona):
  - `device/AndroidDevice.kt`: `DevicePort` con TTS en español (0,9×), `ACTION_CALL`, SMS y última ubicación. Sin permiso o con un intent fallido lanza una excepción → plan B del agente.
  - `device/VoiceInput.kt`: reconocedor de Android en es-ES con 2 s de silencio (habla lenta). Escucha sola cuando Peri espera un "sí".
  - `services/MessageListener.kt`: WhatsApp y correo por notificaciones; SMS también si no hay permiso de SMS.
  - `services/SmsReceiver.kt`: SMS entrantes completos (`RECEIVE_SMS`). Motivo: desde Android 15 el sistema oculta a los lectores de notificaciones los avisos con códigos (llegan sin título y con "Sensitive notification content hidden"), justo lo que necesita el momento 2.
  - `services/CallStateWatcher.kt`: estado de llamada con `TelephonyCallback`; nunca el audio.
  - `services/PeriHub.kt`: un solo `PeriAgent` en el hilo principal; cuenta atrás de emergencia de 10 s. Registro solo con canal, nivel y regla dura, nunca texto ni remitente.
  - `config/PhoneContacts.kt`: agenda del móvil; "Laura hija" → Laura, relación hija; apodos → alias.
  - Pantalla de un botón con la frase de Peri en grande.
- Probado en el emulador (API 37), con contacto "Laura hija" +34600111222:
  - Momento 1: SMS de 699000111 "Mamá… número nuevo… Bizum" → aviso + "Si me dices que sí, llamo a Laura" → "sí" → llamada saliente a +34600111222 (registro de llamadas, tipo 2).
  - Momento 2: llamada entrante descolgada + SMS "BBVA: tu código… 640281" → SCAM, `CODE_DURING_CALL`, "…cuelga y no se lo digas a nadie" en pantalla con la llamada activa.

## Cómo probar sin micrófono (solo builds de depuración)
- `adb shell am broadcast -a app.friendlypear.debug.HEAR --es text "si"` hace que Peri "oiga" esa frase.
- Tras `adb install -r`, reconectar el lector: `adb shell cmd notification disallow_listener app.friendlypear/app.friendlypear.services.MessageListener` y después `allow_listener`.
- En `adb emu gsm`, esperar a que suene (`mCallState=1`) antes de `accept`.

## En curso
- Nada.

## Bloqueado
- Nada.

## Peticiones
- DIR: `docs/IDEA.md` dice que los mensajes se leen "con un solo permiso"; ahora los SMS usan además `RECEIVE_SMS` (mismo grupo que `SEND_SMS`), por la protección de códigos de Android 15.
- SEG: revisar los permisos nuevos del manifiesto (RECORD_AUDIO, READ_CONTACTS, READ_PHONE_STATE, CALL_PHONE, SEND_SMS, RECEIVE_SMS, ubicación) y el receptor de depuración exportado.

## Propuestas para el roadmap
- permisos-progresivos: hoy se piden todos al abrir la app, con una frase; hay que pedirlos de uno en uno.
- momento-1 y momento-2: hechos en el emulador; falta grabarlos en un móvil real.
