# Prueba amplia del 27 oct (emulador API 37 + backend local)

Qué se probó, qué falló y qué se arregló. Sin datos personales: contactos y mensajes de prueba.

## Fallos encontrados y arreglados

| # | Dónde | Qué pasaba | Arreglo |
|---|---|---|---|
| 1 | `PeriFace` | Dos cambios de estado de ánimo en menos de 200 ms (p. ej. "me he caído" y "estoy bien" seguidos): el final del primer fundido ocultaba la capa con el estado nuevo y Peri desaparecía de la pantalla | Cada cambio cancela el fundido anterior y deja las capas en su sitio; probado con dos órdenes a 50 ms |
| 2 | `PeriFace` | Un parpadeo o un saludo programado antes de un cambio de estado podía mostrar un fotograma del estado viejo | `put` solo acepta fotogramas del estado actual (y el saludo en tranquila) |
| 3 | Emergencia | "Me he caído" pedía el permiso de ubicación con el diálogo de Android en plena cuenta atrás, y el diálogo se quedaba en pantalla aunque se cancelara | En la emergencia solo se piden llamar y SMS si faltan; la ubicación se usa si ya estaba y se ofrece antes, en la tarjeta de pasos ("compartir dónde estás", opcional). Probado: cuenta atrás sin diálogo → SMS con ubicación + llamada a Ana + Mini Peri junto a la llamada |
| 4 | Repositorio | La rama por defecto en GitHub es `ccr-b9b202a7-9hh9si` (de una sesión en la nube), 57 commits por detrás de `main` | `main` es ahora la rama por defecto y la rama vieja está borrada (local y remota) |

Ningún commit tiene autor ni coautor que no sea Ruby570bocadito (148 commits; la API de GitHub lista un solo contribuidor).

## Probado y correcto

- Órdenes de voz: leer mensajes, orden desconocida (sugiere ejemplos), "repite", dictar un mensaje y decir "no", "estoy bien" cancela la emergencia a tiempo (ni llamada ni SMS), "call the neighbour" → Marisa por su apodo.
- Prueba 20/20 solo reglas, sin cambios: 20/20 estafas avisadas, 19/20 normales en silencio.
- Tests: core 180, backend 68, Android unitarios; lint 0 errores.

## Observaciones (no arregladas)

- La boca abierta grande de "preocupada" (`_talkwide`) sonríe un poco: al hablar preocupada, a ratos parece contenta.
- El número de Ana en el emulador (5556) tiene menos de 6 cifras, así que sus SMS cuentan como de un desconocido (solo en el emulador).
