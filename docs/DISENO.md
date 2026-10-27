# Diseño: interfaz animada y Peri

FriendlyPear debe sentirse **alegre, viva y cercana**, como una mascota de videojuego amable, sin dejar de ser **legible para alguien con poca vista**. Colores llamativos sí, pero siempre con contraste comprobado.

## 1. Principios

1. **Peri es la interfaz.** La pantalla principal es Peri, grande y animada, y un solo botón. Todo lo demás aparece cuando hace falta.
2. **Alegre pero legible.** Colores vivos de fondo y en botones; el texto, siempre con contraste ≥ 4,5:1 (comprobado más abajo).
3. **La animación explica.** Cada movimiento de Peri dice algo: escucho, pienso, cuidado, todo bien. Nada se mueve porque sí.
4. **Nunca asusta.** El peligro es coral y Peri protege con un paraguas; nada de rojos parpadeantes ni sirenas.
5. **Grande y tocable.** Botones de al menos 64 dp de alto, texto base de 22 sp, títulos de 30 sp.
6. **Respeta a quien no puede o no quiere animaciones.** Si el sistema tiene "quitar animaciones" activado, Peri cambia de estado sin moverse.

## 2. Paleta

| Rol | Color | Uso | Texto encima | Contraste |
|---|---|---|---|---|
| Verde pera (primario) | `#7CC242` | Peri, botón principal, "todo bien" | Tinta `#1F2A1A` | 6,87:1 ✅ |
| Amarillo sol (acento) | `#FFC83D` | Brillos, logros, "pensando" | Tinta `#1F2A1A` | 9,67:1 ✅ |
| Coral (cuidado) | `#FF8A7A` | Mensaje dudoso o estafa | Tinta `#1F2A1A` | 6,53:1 ✅ |
| Azul cielo (llamadas) | `#1F6FD1` | Llamadas, verificación | Blanco | 4,94:1 ✅ |
| Lila (familia) | `#7A4DE0` | MFA, guardianes, avisos a la familia | Blanco | 5,30:1 ✅ |
| Crema (fondo claro) | `#FFF8EC` | Fondo | Tinta `#1F2A1A` | 14,16:1 ✅ |
| Noche (fondo oscuro) | `#141B12` | Fondo en modo oscuro | Texto `#F3F7EE` / verde `#9BD85E` | 16,19:1 / 10,36:1 ✅ |

> Coral con texto **blanco** solo llega a 4,44:1: por eso el texto sobre coral es siempre tinta oscura.

Colores de la interfaz (`ui/peri/Style.kt`), todos ≥ 7:1:

| Rol | Color | Texto encima | Contraste |
|---|---|---|---|
| Verde bosque (botón principal) | `#25541A` | Blanco | 8,89:1 ✅ |
| Verde brote (chip "Protecting you", idioma elegido) | `#E1F0CF` | Verde bosque | 7,44:1 ✅ |
| Blanco (tarjetas, bocadillo, botones secundarios) | `#FFFFFF` | Tinta / tinta suave `#3F4D39` / verde bosque | 14,95:1 / 9,01:1 / 8,89:1 ✅ |
| Borde de tarjeta / de botón | `#EDE4D0` / `#D8CCB2` | (no lleva texto) | — |
| Ámbar claro (chip "Setup not finished", cambios pendientes) | `#FFEBC7` | Ámbar oscuro `#6B3E00` | 7,76:1 ✅ |

**Forma (27 oct, guía design-taste-frontend, conservando la paleta y el tono de Peri):**
- Un solo fondo crema liso en todas las pantallas y en las páginas web; nada de degradados.
- Tarjetas y bocadillo blancos con un borde fino, sin sombra: solo lleva sombra lo que flota (Mini Peri sobre otras apps).
- Tres tamaños de letra (`TextSize` en `Style.kt`): títulos 26, todo lo que se lee o se pulsa 20, letra pequeña 16. En la web, 26 / 19 / 15.
- Un único botón verde relleno por pantalla (la acción principal); los demás, blancos con borde y letra verde. Todos los botones son píldoras redondas.
- Texto alineado a la izquierda; solo la bienvenida y las pantallas de un único momento (pausa, conectar el móvil) van centradas.
- Sin iconos dentro de círculos de color, sin antetítulos encima de los títulos y sin emojis.

**Nunca solo color:** cada estado lleva también icono y palabra ("Todo bien", "Cuidado", "Es una trampa"), para personas que no distinguen colores.

## 3. Peri

**Forma:** pera redonda verde `#7CC242` con brillo amarillo, hoja que se mueve, ojos grandes y mejillas rosadas. Silueta simple para que se reconozca en tamaño pequeño (icono, widget, notificación).

**Cómo se hace:** Peri es un **dibujo vectorial animado en el código** (Jetpack Compose: formas + animaciones), no un vídeo. Así pesa poco, se ve nítida en cualquier pantalla y cambia de estado al instante. Muse Image genera las ilustraciones de referencia y las escenas especiales (logros, entrenamiento); la Peri animada se dibuja a partir de ellas.

### Estados y animaciones

| Estado | Cuándo | Animación | Fondo | Sonido / vibración |
|---|---|---|---|---|
| Tranquila | Sin novedades | Respira (escala 1,00 ↔ 1,03 cada 3 s), parpadea cada 4–6 s, la hoja se balancea | Crema | — |
| Escuchando | Botón de hablar pulsado | Se inclina hacia ti, ojos grandes, ondas verdes que siguen tu voz | Crema con halo verde | Vibración corta al empezar |
| Pensando | Investigando un mensaje | Mira una lupa, puntos amarillos que suben | Crema con brillo amarillo | — |
| Preocupada | Mensaje dudoso | Ceño suave, inclina la cabeza, señala la tarjeta del mensaje | Coral suave | Un toque suave |
| Protectora | Estafa o código durante llamada | Abre un paraguas sobre el mensaje; la tarjeta se "aparta" hacia abajo | Coral | Dos toques firmes |
| Llamando | Verificando con un familiar | Sostiene un teléfono azul, ondas azules | Azul claro | — |
| Esperando a la familia | MFA en curso | Mira un reloj, corazoncitos lila | Lila claro | — |
| Contenta | Problema resuelto, familia respondió | Salta, confeti verde y amarillo | Crema | Melodía corta (opcional) |
| Dormida | De noche (modo descanso) | Ojos cerrados, "zzz" lentos | Noche | — |

**Duraciones:** transiciones de 250–400 ms con curvas suaves; nada parpadea más de 3 veces por segundo (seguro para fotosensibilidad).

## 4. Pantallas

### Inicio
```
┌──────────────────────────────┐
│  Hola, Carmen 👋             │  ← saludo con el nombre, 30 sp
│                              │
│         ( Peri animada )     │  ← 50 % de la pantalla
│                              │
│  "Todo tranquilo hoy"        │  ← lo que dice Peri, también en voz
│                              │
│  ┌────────────────────────┐  │
│  │   🎤  Hablar con Peri   │  │  ← botón verde, 96 dp de alto
│  └────────────────────────┘  │
│  [ Mensajes ] [ Mi familia ] │  ← dos botones secundarios grandes
└──────────────────────────────┘
```

### Aviso de mensaje
- Tarjeta del mensaje (remitente grande, texto **sin enlaces**) con borde coral.
- Peri al lado en estado preocupada o protectora.
- La frase de Peri arriba, en 26 sp.
- Dos botones enormes: **"Llamar a Laura para comprobarlo"** (azul) y **"Ignorar"** (crema).

### Llamada entrante desconocida
- Banner azul a pantalla completa sobre la llamada: Peri con el teléfono y "Este número no está en tu agenda".
- Recordatorio: "Si dice ser de tu familia, pregúntale la palabra de la familia".

### MFA (persona protegida)
- Peri esperando con corazoncitos lila: "Laura tiene que confirmarlo. Te aviso en cuanto conteste."
- Cuenta atrás amable de 15 minutos con barra lila.

### Página del guardián (web)
- Mismos colores; arriba Peri pequeña; qué quiere hacer, a quién, cuánto y por qué sospecha Peri.
- Botones grandes: **"Rechazar y llamar"** (coral, tinta oscura) y **"Aprobar"** (verde).

### Mi familia
- Fotos o iniciales en círculos de colores, con su nivel de confianza (guardián, verificado…).
- Botón "Añadir guardián (en persona, con QR)".

### Lo que Peri puede hacer (permisos)
- Una tarjeta por permiso con icono, para qué sirve y un interruptor grande.
- Registro: "Hoy llamé a Laura a las 18:02 porque me lo pediste".

## 5. Accesibilidad (no negociable)

- Texto escalable: respeta el tamaño de letra del sistema hasta 200 % sin cortar texto.
- Todo lo que Peri dice aparece escrito y se lee con TalkBack (`contentDescription` y regiones vivas).
- Modo **alto contraste**: fondo crema, bordes negros de 3 dp, sin degradados.
- Modo **sin animaciones**: respeta "Quitar animaciones" del sistema.
- Áreas táctiles ≥ 64 dp; nada importante en las esquinas.
- Probado con 2–3 personas mayores antes de la demo.

## 6. Tecnología

| Qué | Cómo |
|---|---|
| Interfaz | **Jetpack Compose** + Material 3 con la paleta de arriba |
| Peri | Composable dibujado con `Canvas` y vectores; `animateFloatAsState`, `rememberInfiniteTransition` |
| Transiciones entre estados | `AnimatedContent` / `Crossfade` |
| Confeti y ondas | Partículas sencillas en `Canvas` |
| Ilustraciones especiales | Muse Image → PNG/WebP en `assets/peri/` y `res/drawable/` |
| Vibración | `HapticFeedback` / `Vibrator` con patrones cortos |
| Sonidos | 3 sonidos cortos y suaves, desactivables |

> El esqueleto actual usa Views; el día 3 se pasa a Compose (tarea `ui-compose` en el roadmap de agentes).

## 7. Más ideas para hacerla especial

| Idea | Qué aporta | Prioridad |
|---|---|---|
| **Escudos ganados** | Cada estafa frenada o cada vez que preguntas antes de actuar, Peri te da un escudito; resumen semanal con tus escudos. Refuerza el buen hábito sin culpa | P1 |
| **Buenos días de Peri** | Saludo diario por voz ("¿Qué tal estás hoy?"); si no hay respuesta en todo el día, aviso suave a la familia. No es consejo médico, es compañía | P2 |
| **Widget de Peri** | Peri en la pantalla de inicio del móvil mostrando su estado; un toque para hablar | P2 |
| **Peri en la página del guardián** | La familia ve la misma Peri: une la experiencia de toda la familia | P1 |
| **Disfraces de temporada** | Peri con bufanda en invierno o gorro en Navidad (Muse Image). Detalle que encanta en la demo | P3 |
| **Modo adolescente** | Peri más "cool" y avisos que no revelan el contenido a los padres | Después |
