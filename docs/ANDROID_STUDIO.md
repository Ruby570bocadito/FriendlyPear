# Desarrollo y pruebas con Android Studio

Cómo montar el entorno, desarrollar y probar FriendlyPear en el ordenador, con emulador y con móvil real.

## 1. Requisitos

| Herramienta | Versión | Para qué |
|---|---|---|
| Android Studio | Última estable | Editor, SDK y emulador |
| Android SDK Platform | 35 | Compilar la app |
| JDK | 17 (incluido en Android Studio) | Gradle y `core/` |
| Bun | ≥ 1.1 | Backend |
| Git | Cualquiera | Repo |

Al instalar Android Studio, deja marcados **Android SDK**, **Android Emulator** y **Platform-Tools**. Añade `platform-tools` al `PATH` para usar `adb` desde la terminal.

## 2. Abrir el proyecto

1. Clona el repo.
2. En Android Studio: **Open** → carpeta `android/` (no la raíz). Gradle incluye `core/` automáticamente.
3. Crea `android/local.properties` si Android Studio no lo hace:
   ```
   sdk.dir=/ruta/a/tu/Android/Sdk
   ```
4. Valores locales (nunca en el repo) en `~/.gradle/gradle.properties`:
   ```
   friendlypear.backendUrl=http://10.0.2.2:8787
   friendlypear.deviceToken=<el mismo DEVICE_TOKEN que backend/.env>
   ```

## 3. Crear el emulador

**Device Manager → Create Virtual Device:**

- Modelo: **Pixel 8**.
- Imagen: **API 35 con Google Play** (incluye Mensajes, reconocimiento de voz y voz en español).
  - Mac con chip Apple → **arm64**. Windows, Linux, Mac Intel → **x86_64**.
- Dentro del emulador: Ajustes → Sistema → Idioma → **Español (España)**.
- Activa la aceleración por hardware (en Windows: *Windows Hypervisor Platform*).

Crea **dos** emuladores para probar el MFA familiar: `Persona` y `Guardian`.

## 4. Ciclo de desarrollo

```
core/ (lógica + tests)  →  android/ (servicios + UI)  →  emulador  →  móvil real
```

1. **La lógica va en `core/`** y se prueba sin emulador: `cd core && ./gradlew test` (segundos).
2. **Lo que depende de Android** (notificaciones, llamadas, voz) va en `android/`.
3. Ejecuta la app con ▶ en Android Studio o:
   ```bash
   cd android && ./gradlew :app:installDebug
   ```
4. Backend en tu ordenador: `cd backend && bun run dev`. Desde el emulador es `http://10.0.2.2:8787`.

## 5. Permisos en el emulador

Tras instalar, concede los permisos que pida la app y activa a mano:

- **Acceso a notificaciones:** Ajustes → Notificaciones → Acceso a notificaciones del dispositivo → FriendlyPear.
- **Micrófono del emulador:** controles ampliados (`⋯`) → Microphone → *Virtual microphone uses host audio input*.

## 6. Simular lo que necesita FriendlyPear

Con el emulador abierto, desde la terminal:

| Prueba | Comando |
|---|---|
| SMS entrante | `adb emu sms send +34600111222 "Correos: su paquete está retenido, pague 1,99 €"` |
| "Mamá, he cambiado de número" | `adb emu sms send +34699000111 "Mamá, se me ha roto el móvil, este es mi número nuevo"` |
| Llamada entrante | `adb emu gsm call +34911000001` |
| Colgar la llamada | `adb emu gsm cancel +34911000001` |
| Código durante llamada (momento 2) | `adb emu gsm call +34911000001` y después `adb emu sms send BBVA "Su código es 482913"` |
| Ubicación (emergencia) | `adb emu geo fix -3.7038 40.4168` (longitud, latitud) |
| Ver registros de la app | `adb logcat -s FriendlyPear` |

Con dos emuladores, usa `adb -s emulator-5554 ...` y `adb -s emulator-5556 ...`. Pueden enviarse SMS y llamarse usando **5554** y **5556** como números de teléfono: así se prueba el MFA familiar sin dos móviles.

> Los SMS de prueba son **sintéticos**. Nunca uses números reales de terceros ni números de emergencia.

## 7. Escenarios de prueba (los momentos de la demo)

| # | Escenario | Pasos | Resultado esperado |
|---|---|---|---|
| 1 | Número nuevo | Contacto "Laura" guardado; SMS desde otro número "mamá, este es mi número nuevo, ¿me haces un Bizum?" | Peri avisa y ofrece llamar a Laura; al decir "sí" llama al número guardado |
| 2 | MFA familiar | Tras el escenario 1, decir "quiero hacer el Bizum" | Llega la petición al emulador Guardián; al rechazar, Peri lo comunica |
| 3 | Código durante llamada | Llamada entrante + SMS con código | Peri dice "cuelga" y avisa al guardián |
| 4 | Emergencia | Decir "me he caído" y esperar 10 s | Llamada al familiar y SMS con ubicación |
| 5 | Cancelar emergencia | "Me he caído" y luego "estoy bien" | No llama |
| 6 | Leer mensajes | Varios SMS, uno con enlace; "léeme los mensajes" | Lee los normales sin enlaces y avisa del sospechoso |
| 7 | Correos falso | SMS con dominio que imita a Correos | "Es una trampa" + usar la app oficial |
| 8 | Correos real | SMS con enlace a `correos.es` | Silencio (normal) |

Registra cada fallo como hallazgo en `docs/agentes/hallazgos/`.

## 8. Pruebas automáticas

| Qué | Comando | Cuándo |
|---|---|---|
| Lógica del núcleo | `cd core && ./gradlew test` | Antes de cada push |
| Backend | `cd backend && bun test` | Antes de cada push |
| Compilación y lint de Android | `cd android && ./gradlew :app:assembleDebug :app:lintDebug` | Antes de cada push |
| Todo lo anterior | CI de GitHub (`.github/workflows/ci.yml`) | En cada push |

Los tres momentos de la demo deben existir también como **tests del núcleo** con un móvil falso (`DevicePort`), para que nunca se rompan sin darnos cuenta.

## 9. Móvil real

Imprescindible para WhatsApp, para medir tiempos reales y para grabar el vídeo.

1. En el móvil (Android 12+): Ajustes → Información del teléfono → pulsa 7 veces en **Número de compilación**.
2. Opciones de desarrollador → **Depuración USB**.
3. Conéctalo y acepta la huella del ordenador; `adb devices` debe mostrarlo.
4. Instala con ▶ o `./gradlew :app:installDebug`.
5. Backend: el móvil necesita la IP de tu ordenador en la misma red (no `10.0.2.2`), o exponer el backend con HTTPS.

## 10. Problemas habituales

| Problema | Solución |
|---|---|
| El emulador va muy lento | Activar aceleración por hardware; imagen de tu arquitectura |
| No llegan notificaciones a la app | Revisar el acceso a notificaciones; reiniciar la app |
| El reconocimiento de voz no oye | Activar el micrófono del host en controles ampliados |
| `SDK location not found` | Crear `android/local.properties` con `sdk.dir` |
| La app no llega al backend | Emulador: `10.0.2.2`; móvil: IP local; comprobar `/health` |
| Android no deja llamar con la app en segundo plano | Esperado: se resolverá con un servicio en primer plano |
