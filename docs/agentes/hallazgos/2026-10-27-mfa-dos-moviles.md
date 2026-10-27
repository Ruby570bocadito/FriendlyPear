# Doble verificación familiar con dos móviles — 27 oct

Dos emuladores a la vez: 5554 es la madre (FriendlyPear instalado desde Descargas, Ana guardiana) y 5556 es Ana, su hija, sin la app: solo SMS y el navegador.

| Paso | Móvil | Qué pasó |
|---|---|---|
| 1 | Madre | Llama el 911 000 111 (no guardado). Peri avisa antes de descolgar |
| 2 | Madre | Cuelga. "¿Quién decía ser?" → "del banco" → "cuelga y llama tú al número oficial" |
| 3 | Madre | "Dile a Marisa que el código es 7350" → Peri no lo manda: "se lo pregunto a Ana" (pose de esperar) |
| 4 | Ana | SMS: "Peri, URGENTE: tu familiar quiere mandar a Marisa un código justo después de hablar por teléfono con un número desconocido…" con el enlace. Sin el código ni el número |
| 5 | Ana | La página: aviso ámbar "Está hablando, o acaba de hablar, por teléfono con un número que no tiene guardado"; "No", "Que me llame primero", "Sí". Elige la del medio |
| 6 | Madre | 3 s después: "Ana prefiere hablarlo contigo antes de mandar nada. Si me dices que sí, le llamo ahora mismo" (pose de llamar) |
| 7 | Madre | "Sí" → llamada a Ana hija con Mini Peri al lado. No se mandó nada |

Hallazgos:
- Antes la página solo tenía "No" y "Sí": a la hija le faltaba la respuesta natural ("que me llame"). Añadida, de extremo a extremo (`callback`).
- El aviso de llamada en curso solo valía con la llamada abierta; en la realidad la persona suele colgar y luego pedir mandar el código. Ahora vale también en los 30 minutos siguientes.
- En el emulador de Ana, Chrome pide iniciar sesión la primera vez; la página se probó en el navegador del equipo con el mismo enlace (`10.0.2.2` es el equipo visto desde el emulador). En móviles reales hace falta el túnel HTTPS.
- La SMS del emulador no pasa entre los dos: se reenvió con `adb emu sms send` (ya pasaba en la primera prueba).
