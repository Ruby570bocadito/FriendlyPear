# Instalación limpia y los 5 momentos de la demo (27 oct)

## Instalación limpia desde el README

Clon nuevo desde GitHub (`082ee7c`), siguiendo solo el README:

| Paso | Resultado |
|---|---|
| `cd core && ./gradlew test` | Bien a la primera |
| `cd backend && bun test` | Bien a la primera (68) |
| `cd android && ./gradlew :app:assembleDebug` | Pide `sdk.dir`, como dice el README; con barras normales compila (APK de 8 MB) |

Huecos del README que un jurado no habría podido salvar, ya cubiertos en "Run it yourself":

- Cómo arrancar el backend y qué claves necesita (`.env.example` no tenía la de Baseten ni la dirección de la Meta Model API).
- Cómo apuntar la app al backend (`friendlypear.backendUrl`, `friendlypear.deviceToken` en `~/.gradle/gradle.properties`).
- Que sin backend Peri sigue protegiendo con las reglas del móvil.
- El formato de `sdk.dir` en Windows y cómo dar los accesos especiales con `adb`.

## Los 5 momentos (`docs/VIDEO.md`), en inglés y en español

| Momento | Inglés | Español |
|---|---|---|
| 1. "He cambiado de número" → aviso → "sí" → llamada a Ana (Mini Peri junto a la llamada) | Bien | Bien |
| 2. Llamada desconocida → aviso antes de descolgar → al colgar "¿Quién decía ser?" → "mi hija" → "Ese número no es el de Ana…" | Bien | Bien |
| 3. Código durante la llamada → "Cuelga ahora mismo"; banco durante la llamada → pausa de seguridad | Bien | Bien (código) |
| 4. "Mira esta imagen": carta falsa → timo, QR marcado, frase de Peri | Bien | Bien |
| 5. "Me he caído" → cuenta atrás sin diálogos → SMS con ubicación + llamada a Ana | Bien | Bien |

Coste de la ronda: 2 imágenes (2 llamadas a Muse Spark y 2 a SAM 3.1); lo demás, solo reglas.

## Fallo encontrado y arreglado

- **Con la pantalla "Mira esta imagen" abierta, los avisos de Peri solo se oían**: un SMS de "número nuevo" o una llamada desconocida no salían en pantalla, porque esa pantalla no escuchaba a Peri. Ahora es una vista más de Peri (como la principal y Mini Peri): muestra el aviso, aparta a Mini Peri y mantiene la cara del resultado mientras se lee.

## Observaciones

- La tecla de colgar de `adb` (`ENDCALL`) apaga la pantalla si no hay llamada: en las grabaciones, despertar con `WAKEUP` y `wm dismiss-keyguard` después.
- Al cambiar de idioma con "Mira esta imagen" abierta, la pantalla se rehace y pierde la imagen (hay que elegirla otra vez).
- `adb` no puede compartir fotos de la galería con otras apps; "compartir con Peri" queda por probar en un móvil real.
