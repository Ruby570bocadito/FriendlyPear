# Diseño: el mundo de Peri

FriendlyPear debe sentirse **viva, alegre y cercana**, como una mascota de videojuego amable, sin dejar de ser **legible para alguien con poca vista**. Desde la ronda 2 (D8, 27 oct) manda lo vivo: colores vivos, un fondo que se mueve, fuentes propias, efectos y apartados para navegar. Las reglas planas del 27 oct (fondo liso, sin sombras) quedan sustituidas; se mantienen la letra grande, el contraste y la simpleza.

## 1. Principios

1. **Peri es la interfaz.** En su apartado, Peri grande y animada, lo que dice y un botón para hablar. Lo demás aparece cuando hace falta.
2. **Viva pero legible.** Color y movimiento en el fondo, los botones y Peri; el texto, siempre con contraste de 7:1 o más (tabla de la §4).
3. **El mundo acompaña.** El cielo sigue la hora y el estado de Peri; el lugar cambia cada día. Nada se mueve porque sí: cada movimiento dice algo (escucho, pienso, cuidado, todo bien).
4. **El peligro primero.** Ante un timo, el mundo se queda quieto y se tiñe de coral suave, sin partículas ni bromas; nada de rojos parpadeantes ni sirenas.
5. **Grande y tocable.** Botones de 56 dp o más (el principal, 76 dp), texto base de 20 sp, títulos de 26 sp.
6. **Respeta "quitar animaciones".** Con la opción del sistema activada, el fondo se dibuja quieto y Peri cambia de estado sin moverse.

## 2. Dirección visual: el mundo de Peri (28 oct)

Elegida con la persona: **fondos que van cambiando**, en vez de escenas de vídeo de Higgsfield (descartado).

- **Cielo por la hora:** amanecer (6–8 h, melocotón), día (9–17 h, azul a crema), atardecer (18–20 h, naranja), noche (21–5 h, azul noche con luna y estrellas que titilan). Sol o luna que flotan despacio y nubes que cruzan.
- **Cielo por el estado:** aviso de timo → velo coral suave y todo quieto; contenta → confeti por el cielo; dormida → noche.
- **Un lugar cada día, según la estación:** pradera con flores, huerto de perales, playa con barquito, colinas de otoño con hojas que caen y nieve con pinos. En Ajustes se puede fijar uno.
- **Las colinas llegan a los pies de Peri** en su apartado; al ir a otro apartado se hunden y el cielo deja sitio a las tarjetas.
- **Pantallas secundarias** (Ajustes, denuncia, viñetas…): el cielo claro de la hora, sin colinas, para que lean bien.
- **Vídeo:** los mismos lugares y cielos sirven de fondo detrás de los clips del móvil.

## 3. Tipografía

| Uso | Fuente | Por qué |
|---|---|---|
| Títulos, botones, chips, barra de apartados, el nombre | **Fredoka** (variable, 600–700) | Redonda y simpática, como Peri |
| Todo lo que se lee (bocadillo, textos, listas) | **Atkinson Hyperlegible** (400 y 700) | Hecha por el Braille Institute para personas con poca visión |

Licencia OFL 1.1, dentro de la app (`android/app/src/main/res/font/`; licencias en `assets/licenses/`), sin internet. Atkinson es la letra por defecto del tema; Fredoka se construye en código a 500, 600 y 700 (`Fonts` en `Style.kt`). Tamaños: títulos 26, lo que se lee o se pulsa 20, letra pequeña 16; crecen con la letra del móvil hasta ×1,15.

## 4. Paleta

| Rol | Color | Texto encima | Contraste |
|---|---|---|---|
| Verde pera (Peri, apartado Peri) | `#7CC242` | (no lleva texto) | — |
| Verde bosque (botón principal) | `#25541A`, labio `#173A10` | Blanco | 8,89:1 ✅ |
| Verde brote (chip "Te estoy cuidando") | `#E1F0CF` | Verde bosque | 7,44:1 ✅ |
| Sol (logros, apartado Diario) | `#FFC83D` / tinte `#FFF1C9` | Tinta / ámbar `#6B3E00` | 13,30:1 / 8,07:1 ✅ |
| Cielo (apartado Aprende, llamadas) | `#6EC6FF` / tinte `#DDF1FF` | Tinta / azul `#1E4A7A` | 12,89:1 / 7,82:1 ✅ |
| Lila (apartado Familia) | `#A98BFF` / tinte `#ECE5FF` | Tinta / lila `#4A2F99` | 12,26:1 / 7,98:1 ✅ |
| Coral (cuidado) | `#FF8A7A` / tinte `#FFE2DC` | Tinta / coral `#8A2B1F` | 12,22:1 / 7,02:1 ✅ |
| Blanco (tarjetas, bocadillo, botones secundarios) | `#FFFFFF`, labio `#E3D7BD` | Tinta / tinta suave `#3F4D39` | 14,95:1 / 9,01:1 ✅ |
| Crema (fondo claro) | `#FFF8EC` | Tinta `#1F2A1A` | 14,16:1 ✅ |
| Cielo de día / amanecer / atardecer (arriba) | `#8FD3FF` / `#FFC9A0` / `#FFA585` | Tinta | 9,21 / 10,05 / 7,81:1 ✅ |
| Cielo de noche | `#141C44` → `#2C3878` | Crema | 15,55 / 10,23:1 ✅ |

El texto sobre coral fuerte es siempre tinta oscura (con blanco solo llega a 4,44:1). **Nunca solo color:** cada estado lleva también icono y palabra.

## 5. Componentes y movimiento

- **Botones de goma:** píldoras con un labio de 5 dp debajo (verde bosque sobre `#173A10`; blancos sobre arena `#E3D7BD`). Al pulsar, el botón baja al labio; al soltar, rebota. Un único botón verde por pantalla.
- **Tarjetas:** blancas, esquinas de 26 dp y una sombra suave verdosa; al pulsarlas se hunden un poco (0,96).
- **Cabecera de apartado:** título grande en Fredoka, una línea de Peri y su pegatina (una pose suya) al lado.
- **Chips:** píldoras con tinte del color de su apartado.
- **Entradas:** cada pieza sube y aparece una tras otra (340 ms, 70 ms entre piezas). Cambio de apartado: 220 ms.
- Nada parpadea más de 3 veces por segundo (seguro para fotosensibilidad).

## 6. Navegación

Barra inferior flotante con cuatro apartados, cada uno con su color; Ajustes en la rueda de arriba.

| Apartado | Color | Qué tiene |
|---|---|---|
| **Peri** | verde | Peri, lo que dice, "Hablar con Peri", "Mira esta imagen", Mini Peri y la tarjeta que toque (pasos, web, denuncia) |
| **Diario** | sol | Lo que Peri ha hecho por la persona, por días: timos parados, escudos, llamadas y mensajes que hizo, fotos miradas; cifras arriba |
| **Aprende** | cielo | El juego **"¿Timo o no?"**, las trampas que Peri conoce (viñetas) y sus consejos de oro |
| **Familia** | lila | Quién cuida a la persona, si hay un cambio pendiente, lo que ven los guardianes y el botón para cambiarlo (con el bloqueo del móvil) |

## 7. Peri

**Cómo se hace:** ilustraciones de plastilina 3D de **Muse Image** (`assets/peri/`), recortadas, con fotogramas (parpadeo, boca, miradas, pasos, gestos) y animación en código (`PeriFace`): bote, balanceo, aplastar y estirar, sombra. Personalidad, poses e interacciones: [`PERI_PERSONAJE.md`](PERI_PERSONAJE.md); voz: [`PERI_VOZ.md`](PERI_VOZ.md).

| Estado | Cuándo | Animación | Mundo |
|---|---|---|---|
| Tranquila | Sin novedades | Respira, parpadea, mira, a veces salta o baila | Cielo de la hora |
| Escuchando | Botón de hablar | Se inclina hacia ti | Igual |
| Pensando | Investigando un mensaje | Lupa, puntos amarillos | Igual |
| Preocupada / Protectora | Mensaje dudoso / timo | Ceño suave / paraguas | Velo coral, todo quieto |
| Llamando / Esperando | Llamada / respuesta de la familia | Teléfono / reloj | Igual |
| Contenta | Todo resuelto | Salta y corre en el sitio | Confeti en el cielo |
| Dormida | De noche y tranquila | Ojos cerrados, "z z z" | Noche |

## 8. Accesibilidad (no negociable)

- Texto escalable hasta ×1,15 sobre tamaños ya grandes, sin cortar texto (las frases largas encogen un poco).
- Todo lo que Peri dice aparece escrito y lo lee TalkBack; el fondo es decorativo y TalkBack lo ignora.
- "Quitar animaciones": fondo quieto, sin partículas, Peri sin moverse.
- Áreas táctiles de 48 dp o más; nada importante en las esquinas.
- Contraste de 7:1 o más en todo el texto, también sobre el cielo de noche.

## 9. Tecnología

| Qué | Cómo |
|---|---|
| Interfaz | Vistas de Android creadas en código (`ui/peri/`), sin librerías externas |
| Estilo | `Style.kt`: paleta, fuentes (`Fonts`), `Pill`, tarjetas, cabeceras, movimiento |
| Mundo | `PeriWorld.kt`: cielo, astros, nubes, colinas, lugares y partículas en `Canvas`, a 30 fps y parado si no se ve |
| Peri | `PeriFace.kt` + `Sparkles.kt`; ilustraciones de Muse Image en `res/drawable-nodpi/` |
| Fuentes | `res/font/`: Atkinson por el tema; Fredoka variable con `android.graphics.fonts.Font` a 500, 600 y 700 |
| Vibración | `Haptics` con patrones cortos por estado |

## 10. Más ideas

| Idea | Qué aporta | Estado |
|---|---|---|
| **Escudos ganados** | Refuerza el buen hábito sin culpa | Hecho (IMP) |
| **Disfraces de temporada** | Bufanda en invierno, capa de heroína tras parar un timo | Ronda 2 (`r2-poses`) |
| **Pregúntale a Peri** | Dudas y charla con Muse Spark y guardarraíles | Ronda 2 (`r2-charla`) |
| **Buenos días de Peri** | Saludo diario; si no hay respuesta en todo el día, aviso suave a la familia | Después |
| **Widget de Peri** | Peri en la pantalla de inicio con su estado | Después |
| **Modo adolescente** | Avisos que no revelan el contenido a los padres | Después |
