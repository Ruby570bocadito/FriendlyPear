# Conectar Claude al emulador Android (MCP)

Con esta guía, Claude Code en tu ordenador puede instalar FriendlyPear en el emulador, tocar la pantalla, leer la interfaz, hacer capturas, grabar vídeo y simular SMS y llamadas. Así puede ejecutar por ti los escenarios de prueba de [`ANDROID_STUDIO.md`](ANDROID_STUDIO.md). Además, con el MCP de Android Studio puede compilar, pasar inspecciones y buscar símbolos dentro del IDE.

## Cómo encaja

```
Tu ordenador
├── Android Studio (proyecto android/ abierto) + emulador "Persona" (y "Guardian")
├── adb (platform-tools)
└── Claude Code ──► mobile-mcp ──► adb ──► emulador
                ├─► android-studio ──► servidor MCP del IDE (127.0.0.1:64342)
                └─► terminal (adb emu sms / gsm, gradlew, bun)
```

[`.mcp.json`](../.mcp.json) declara dos servidores:

| Servidor | Qué controla | Necesita |
|---|---|---|
| `mobile-mcp` | El emulador (pantalla, apps, capturas, vídeo) | Emulador arrancado y `adb` en el PATH |
| `android-studio` | El IDE (compilar, inspecciones, búsqueda, refactor) | Android Studio abierto con `android/` |

El MCP corre **en tu ordenador**, donde está el emulador. Una sesión de Claude en la nube no ve tu emulador.

## 1. Requisitos

| Qué | Cómo comprobarlo |
|---|---|
| Node.js 18+ | `node -v` |
| Android Studio con el AVD **Persona** creado | Device Manager |
| `adb` en el PATH | `adb devices` |
| Claude Code | `claude --version` |

### Crear el AVD "Persona"

Los prompts de esta guía usan los nombres **Persona** (y **Guardian**, sección 6). Android Studio crea por defecto un AVD con otro nombre (por ejemplo `Medium_Phone_API_37.0`), así que créalo o renómbralo:

1. Android Studio → **Device Manager** → **+** → **Create Virtual Device**.
2. Elige un teléfono (por ejemplo *Medium Phone*) y una imagen de sistema de Android 12 o superior con Google APIs.
3. En **AVD Name** escribe exactamente `Persona` (sin espacios). Para renombrar uno existente: ✏️ *Edit* → **AVD Name**.
4. Comprueba el nombre con:

```bash
emulator -list-avds
```

(`emulator` está en `%LOCALAPPDATA%\Android\Sdk\emulator` en Windows.)

Si `adb` no se encuentra, añade al PATH la carpeta `platform-tools` del SDK:

- Windows: `%LOCALAPPDATA%\Android\Sdk\platform-tools`
- Mac: `~/Library/Android/sdk/platform-tools`
- Linux: `~/Android/Sdk/platform-tools`

Para instalar Claude Code, sigue la guía oficial: https://code.claude.com/docs

## 2. Añadir el MCP

**Opción A — automática (recomendada).** El repo ya trae [`.mcp.json`](../.mcp.json) en la raíz. Abre Claude Code **en la carpeta raíz de FriendlyPear**:

```bash
cd FriendlyPear
claude
```

Claude Code detecta `.mcp.json` y te pregunta si confías en los servidores `mobile-mcp` y `android-studio`: acepta los dos. Si abres Claude Code en otra carpeta (por ejemplo, la carpeta padre del repo), **no** detecta `.mcp.json` y las herramientas no aparecen.

**Opción B — a mano:**

```bash
claude mcp add mobile-mcp -- npx -y @mobilenext/mobile-mcp@1.0.13
```

```bash
claude mcp add android-studio -- npx -y mcp-remote@0.1.29 http://127.0.0.1:64342/sse
```

Las versiones están fijadas (1.0.13 y 0.1.29) para que nadie pueda colarte un cambio. Para actualizar, revisa el [repositorio](https://github.com/mobile-next/mobile-mcp) y cambia el número a propósito.

## 3. Comprobar la conexión

1. Abre Android Studio con el proyecto `android/` y arranca el AVD **Persona**.
2. En Claude Code escribe `/mcp`: `mobile-mcp` y `android-studio` deben aparecer como **connected**.
3. Pídele: *"Lista los dispositivos disponibles"*. Debe ver un dispositivo con id `Persona`.

### El id del dispositivo es el nombre del AVD

`mobile-mcp` 1.0.13 identifica los emuladores por el **nombre del AVD**, no por el serial de `adb`:

| Para… | Usa | Ejemplo |
|---|---|---|
| Herramientas de `mobile-mcp` (`device`) | Nombre del AVD | `Persona`, `Guardian` |
| Comandos `adb` (`-s`) | Serial de `adb devices` | `emulator-5554`, `emulator-5556` |
| Número de teléfono entre emuladores | Puerto del emulador | `5554`, `5556` |

Si le pasas `emulator-5554` a una herramienta de `mobile-mcp`, responde `Device "emulator-5554" not found`.

## 4. MCP de Android Studio

Android Studio trae un servidor MCP propio que escucha en `http://127.0.0.1:64342/sse` mientras el IDE está abierto. `mcp-remote` lo conecta con Claude Code. Si el puerto cambia, compruébalo en los ajustes de Android Studio (servidor MCP) y actualízalo en `.mcp.json`.

**Pasa siempre `projectPath`.** Sin él, cada herramienta responde *"Unable to determine the target project"*. El proyecto que abre Android Studio es la carpeta `android/`, no la raíz del repo:

```
projectPath = <ruta al repo>/FriendlyPear/android
```

Por ejemplo, en Windows: `C:/Users/<tú>/Documents/MetaHackaton/FriendlyPear/android`. Con esa ruta, `get_project_modules` debe devolver `friendlypear-android.app` y `core`.

| Herramienta | Para qué en FriendlyPear |
|---|---|
| `build_project` | Compilar y ver los errores sin salir del IDE |
| `get_file_problems`, `lint_files` | Inspecciones de Kotlin/Android sobre un archivo |
| `get_project_modules`, `get_project_dependencies` | Ver módulos (`app`, `core`) y dependencias |
| `search_symbol`, `search_text`, `search_regex`, `search_file` | Buscar en el proyecto con el índice del IDE |
| `get_symbol_info`, `analyze_calls` | Ver tipos y jerarquía de llamadas |
| `rename_refactoring`, `reformat_file` | Refactor y formato seguros |
| `get_run_configurations`, `execute_run_configuration` | Lanzar la app o los tests |
| `git_status`, `get_repositories` | Estado de git visto por el IDE |

`execute_terminal_command`, `apply_patch` y `create_new_file` también existen, pero modifican archivos o ejecutan comandos: revísalos antes de aprobarlos, igual que los de la terminal.

Prompt de ejemplo:
> Con el MCP de Android Studio y `projectPath` = la carpeta `android/` del repo, compila el proyecto con `build_project` y dime los errores. Después pasa `get_file_problems` sobre `PeriActivity.kt`.

## 5. Qué puede hacer Claude en el emulador

| Herramienta del MCP | Para qué en FriendlyPear |
|---|---|
| `mobile_list_available_devices` | Ver los emuladores (`Persona`, `Guardian`) |
| `mobile_install_app`, `mobile_launch_app`, `mobile_terminate_app` | Instalar y abrir la app |
| `mobile_list_elements_on_screen` | Leer la interfaz (textos de Peri, botones) |
| `mobile_click_on_screen_at_coordinates`, `mobile_swipe_on_screen`, `mobile_type_keys`, `mobile_press_button` | Usar la app como una persona |
| `mobile_take_screenshot`, `mobile_save_screenshot` | Capturas para el README y la demo |
| `mobile_start_screen_recording`, `mobile_stop_screen_recording` | Grabar clips para el vídeo de 3 minutos |
| `mobile_set_location` | Ubicación para la emergencia |
| `mobile_get_device_logs`, `mobile_list_crashes`, `mobile_get_crash` | Ver errores y cierres |

Todas reciben `device` = nombre del AVD (sección 3).

El MCP **no** simula SMS ni llamadas. Claude los lanza desde la terminal (con un solo emulador arrancado no hace falta `-s`):

```bash
adb emu sms send +34699000111 "Mamá, se me ha roto el móvil, este es mi número nuevo"
adb emu gsm call +34911000001
adb emu gsm cancel +34911000001
```

## 6. Prompts listos para usar

Pégalos en Claude Code con el AVD `Persona` abierto.

**Instalar y abrir:**
> Compila la app con `cd android && ./gradlew :app:installDebug`, ábrela en el emulador y dime qué texto muestra Peri. Haz una captura.

**Escenario 1 — número nuevo:**
> En el emulador hay un contacto "Laura" con el número +34 600 111 222. Envía con `adb emu sms send +34699000111 "Mamá, se me ha roto el móvil, este es mi número nuevo, ¿me haces un Bizum?"`. Espera 3 segundos, lee la pantalla y dime si Peri avisa y ofrece llamar a Laura. Haz una captura.

**Escenario 3 — código durante llamada:**
> Lanza una llamada entrante con `adb emu gsm call +34911000001`, acéptala, y después envía `adb emu sms send BBVA "Su código de verificación es 482913"`. Comprueba si Peri dice que cuelgues. Cuelga con `adb emu gsm cancel +34911000001`.

**Escenario 4 — emergencia:**
> Pon la ubicación en Madrid (40.4168, -3.7038). Pulsa el botón de hablar y comprueba que aparece la cuenta atrás. No dejes que llame a ningún número real.

**Batería completa:**
> Ejecuta los 8 escenarios de `docs/ANDROID_STUDIO.md` (sección 7) uno a uno. Para cada uno, anota resultado esperado, resultado real y captura. Crea un hallazgo en `docs/agentes/hallazgos/` por cada fallo, con el formato del protocolo.

**Clip para la demo:**
> Empieza a grabar la pantalla, ejecuta el escenario 1 y para la grabación. Guarda el vídeo en `docs/demo/clips/`.

## 7. Dos emuladores (MFA familiar)

1. Crea un segundo AVD llamado **Guardian** (igual que Persona, sección 1). Arranca primero `Persona` y después `Guardian`: en `adb` serán `emulator-5554` y `emulator-5556`.
2. Pide a Claude que trabaje con los dos indicando el dispositivo:
   > En `Persona` di que quieres enviar el Bizum. En `Guardian` abre el SMS que llega y pulsa "Rechazar y llamar". Vuelve a `Persona` y dime qué dice Peri.

Con dos emuladores, los comandos `adb` necesitan `-s` para elegir uno:

```bash
adb -s emulator-5554 emu sms send +34699000111 "Mamá, este es mi número nuevo"
```

Los emuladores se mandan SMS y se llaman usando **5554** y **5556** como números.

## 8. Seguridad

- Usa el MCP **solo con emuladores** o un móvil de pruebas, nunca con tu móvil personal: el MCP tiene control total del dispositivo.
- Datos de prueba **sintéticos**: nunca números reales de terceros ni números de emergencia.
- El repo es público: no pongas claves en `.mcp.json` ni en los prompts. Las claves van en `backend/.env` y `~/.gradle/gradle.properties`.
- Revisa cada comando que Claude quiera ejecutar antes de aprobarlo.

## 9. Problemas habituales

| Problema | Solución |
|---|---|
| `/mcp` muestra `mobile-mcp` como failed | Comprueba `node -v` (18+) y ejecuta a mano `npx -y @mobilenext/mobile-mcp@1.0.13` para ver el error |
| `/mcp` no muestra ningún servidor | Abre Claude Code en la raíz de FriendlyPear, donde está `.mcp.json` |
| `/mcp` muestra `android-studio` como failed | Abre Android Studio con `android/` antes de Claude Code; comprueba el puerto 64342 |
| `Device "emulator-5554" not found` | Usa el nombre del AVD (`Persona`) como `device` (sección 3) |
| `Unable to determine the target project` | Pasa `projectPath` = carpeta `android/` del repo (sección 4) |
| No aparece ningún dispositivo | Arranca el emulador antes; comprueba `adb devices` |
| `adb` no encontrado | Añade `platform-tools` al PATH (sección 1) y reinicia la terminal |
| Toca en el sitio equivocado | Pide primero `mobile_list_elements_on_screen` y que toque por el elemento |
| La app no se instala | Compila primero con Gradle; revisa `android/local.properties` |
