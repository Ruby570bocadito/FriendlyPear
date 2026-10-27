# Peri a Peri y página del guardián — 27 oct

Dos emuladores: 5554 la madre (app en español, rol de filtro de llamadas), 5556 Ana con FriendlyPear (en inglés) y "Mamá" (5554) guardada.

| Prueba | Resultado |
|---|---|
| Petición normal → Peri de Ana | Se abre su pantalla ("Mamá is asking for your OK"), Peri lo dice en voz alta. "No" → la Peri de la madre: "tu familia prefiere que no lo mandemos" |
| Petición urgente (llamada del "banco" y luego "dile a Marisa que el código es 7350") | SMS "Peri, URGENTE…"; en la pantalla de Ana, arte de alerta y "Possible scam in progress". "Call Mamá now" → la Peri de la madre ofrece llamar y el móvil de Ana abre el marcador con 5554 |
| SMS falso "Peri, URGENT… http://peri-family.top/a/…" de un desconocido | Timo firme: "Careful: this message pretends to be from me…" (paraguas); no se consulta ningún modelo |

Fallos encontrados:
- Con dos Peris, la de Ana habría marcado el SMS de aprobación como timo (enlace + "código" + "URGENTE"). Ahora se reconoce antes del escudo.
- Tras reinstalar, el móvil de la madre perdió el rol de filtro de llamadas: sin él Peri no ve el número, no avisa al sonar ni marca la urgencia. Es del montaje de la prueba (el rol se pide al primer arranque), pero conviene recordárselo en la tarjeta de pasos.
- La pantalla del guardián se quedaba abierta encima al terminar: ahora "Volver con Peri".
- La página web tenía los botones bajo el pliegue: ahora van justo después de la petición.
- "No cuelgue y facilite el código" no contaba como pedir el código: la negación se tomaba de "no cuelgue". Ahora se corta en "y".
