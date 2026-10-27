# FriendlyPear en iPhone

iOS no deja a una app hacer lo mismo que Android: no puede leer las notificaciones ni los WhatsApp de otras apps, ni ver qué app se abre, ni pintar encima de otras apps, ni llamar o mandar SMS sin que la persona pulse. Peri en iPhone es posible, pero **con otras piezas** y algo menos de alcance. Este documento dice qué se puede hacer y cómo.

## Lo que necesitamos antes de empezar

- **Un Mac con Xcode.** Las apps de iPhone solo se compilan y se prueban en macOS (el portátil actual es Windows). Sin Mac no se puede ni compilar.
- **Una cuenta de Apple Developer** (99 $/año) para instalar en un iPhone real y para los permisos especiales.
- **El permiso Family Controls** (`com.apple.developer.family-controls`): Apple tiene que aprobarlo para publicar; para desarrollo funciona sin aprobación.

## Qué se reutiliza tal cual

| Pieza | En iPhone |
|---|---|
| Backend (segunda opinión, investigador, aprobaciones, novedades para la familia) | Igual, sin cambios: el iPhone habla con las mismas rutas |
| Escudo, agente, voz, reglas del círculo (`core/`, Kotlin puro) | Con **Kotlin Multiplatform** se compila a un framework para iOS: mismas reglas y mismos tests |
| Peri (las 70 ilustraciones recortadas) | Las mismas imágenes en el catálogo de la app; la animación se rehace en SwiftUI |
| Textos de Peri (`Phrases`, es/en) | Vienen con el `core` compartido |

## Equivalencias, función a función

| En Android | En iPhone | ¿Se puede? |
|---|---|---|
| Leer SMS entrantes y avisar | **Message Filter Extension** (`ILMessageFilterExtension`): iOS le pasa el texto de los SMS de **números que no están en la agenda**, y la extensión los clasifica (correo no deseado, transacciones…) y puede preguntar al backend. No puede hablar ni avisar en el momento: el SMS acaba en "Desconocidos/No deseado" | Sí, para SMS de desconocidos |
| Leer WhatsApp y correo | No hay forma: iOS no deja leer notificaciones de otras apps. Alternativa: **"Compártelo con Peri"** (extensión de compartir: la persona comparte el mensaje o una captura y Peri lo revisa, con Muse Spark multimodal para capturas) | Solo si la persona lo comparte |
| Aviso antes de descolgar a un número desconocido | **Call Directory Extension** (etiqueta o bloquea números de una lista) o **Live Caller ID Lookup** (iOS 18, consulta privada a un servidor). No ve todas las llamadas ni puede hablar antes de descolgar | Parcial: etiquetar "Posible estafa" |
| "¿Quién decía ser?" al colgar | `CXCallObserver` sabe cuándo empieza y acaba una llamada (sin el número), solo mientras la app está abierta | Parcial |
| Pausa de seguridad antes de una app de dinero | **Screen Time API** (Family Controls con autorización individual + `ManagedSettings` + `DeviceActivity`): la persona elige sus apps de banco y, tras algo raro, iOS las "escuda" con una pantalla de Peri (`ShieldConfiguration`) y botones (`ShieldAction`) | Sí, con el permiso de Apple |
| Aviso al instalar AnyDesk o TeamViewer | No: iOS no avisa de instalaciones. Se puede "escudar" esas apps igual que el banco | Parcial |
| Mini Peri flotando | No se puede pintar encima de otras apps. Equivalente: **Live Activity / Dynamic Island** con Peri pequeñita durante una llamada que ella hizo o tras un aviso, y un widget | Sí, a su manera |
| Hablar con Peri | `Speech` + `AVSpeechSynthesizer` dentro de la app, y **App Intents** para Siri: "Oye Siri, pregúntale a Peri", "Oye Siri, pide ayuda a mamá con Peri" | Sí |
| Llamar a la familia | Abre la llamada (`tel:`) y iOS pide confirmar con un toque | Sí, con un toque más |
| SMS al guardián (aprobaciones, emergencia) | `MFMessageComposeViewController`: Peri prepara el SMS y la persona pulsa "Enviar". Mejor: el backend avisa al guardián por su enlace o por notificación | Sí, con un toque más |
| Emergencia "me he caído" | Cuenta atrás en la app y llamada a la familia; la Detección de caídas y "Emergencia SOS" de Apple siguen siendo las de iOS | Parcial |

## Plan propuesto (cuando haya Mac)

1. **`core` con Kotlin Multiplatform** (objetivo `iosArm64` y simulador) y los mismos tests. Sin tocar la lógica.
2. **App SwiftUI**: la pantalla de Peri (misma paleta, bocadillo, botones), voz, familia y novedades para los guardianes.
3. **Message Filter Extension** con el escudo del `core` y la segunda opinión del backend para los dudosos.
4. **Pausa de seguridad con Screen Time**: la persona elige sus apps de banco; tras un aviso, se escudan 30 minutos con la pantalla de Peri.
5. **Live Activity** con Peri pequeñita durante las llamadas que ella hace y después de un aviso.
6. **Compártelo con Peri** (extensión de compartir) e **intents de Siri**.

Límites que hay que contar con claridad: en iPhone Peri no lee WhatsApp ni avisa antes de descolgar como en Android; su fuerza allí es el filtro de SMS, la pausa de las apps de banco y "Compártelo con Peri".
