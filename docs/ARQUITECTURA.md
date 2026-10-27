# Arquitectura

Las reglas del móvil deciden casi todo; lo dudoso pasa primero por Muse Spark Open (segunda opinión) y, si sigue dudoso, por el investigador con Muse Spark. Muse Glimmer en casa queda como capa opcional.

```
Notificaciones ─┐
Estado llamada ─┼─► Reglas locales ──normal/estafa──────────────┐
Agenda/círculo ─┘        │ dudoso                               ▼
                         ▼                                 Agente Peri ──► Peri habla ──sí──► Acciones
                  Muse Spark Open (Baseten) ──aún dudoso──► Muse Spark (investigador con herramientas)
Voz y cámara (Muse Voice Transcribe) ─────────────────────────► │
                                                              riesgo alto
                                                                 ▼
                                                    Backend de aprobaciones ──SMS──► Guardián (web o su Peri)
```

## Módulos

| Ruta | Qué contiene |
|---|---|
| `core/.../model` | Tipos compartidos: mensajes, contactos, canales, niveles |
| `core/.../shield` | Escudo por capas: señales, puntuación, reglas duras, acción segura, explicación |
| `core/.../voice` | Órdenes de voz en español y resolución de contactos por alias |
| `core/.../agent` | Bucle del agente, herramientas, permisos y verificación de resultados |
| `core/.../trust` | Círculo de confianza, niveles de riesgo, MFA familiar y reconocimiento de peticiones de Peri a Peri |
| `core/.../alerts` | Resumen semanal para los guardianes (solo recuentos) |
| `android/.../ui/peri` | Peri y sus pantallas: inicio, bienvenida, guía de desbloqueo, Mini Peri, pausa, imagen, familia, ajustes, guardián (Peri a Peri) y conectar el móvil |
| `android/.../services` | Lector de notificaciones, estado de llamada y filtro de llamadas (`CallScreeningService`) |
| `android/.../device` | Llamadas, SMS, ubicación, voz y lectura en voz alta |
| `android/.../config` | Configuración de la familia, novedades compartidas y permisos progresivos |
| `android/.../net` | Clientes del backend, clave del móvil (`DeviceKey`, cifrada con el Keystore) y peticiones de Peri a Peri |
| `backend/src/approvals` | Enlaces de un solo uso, página del guardián con Peri y paso "¿seguro?", la petición en JSON para su Peri |
| `backend/src/devices`, `qr.ts`, `pair.ts` | Una clave por móvil: códigos de un uso, `bun pair` con QR propio, desconexión por móvil |
| `backend/src/investigator` | Agente investigador y sus herramientas de solo lectura |
| `backend/src/models` | Clientes de Muse Spark, Glimmer, Voice Transcribe y SAM 3.1 |
| `training/` | Base de conocimiento, scripts de dataset, pesos aprendidos y fine-tuning (datos fuera del repo) |

## Contratos

- **core ↔ Android:** interfaz `DevicePort` (`core/.../agent/DevicePort.kt`). Android la implementa; los tests usan un falso.
- **Android ↔ backend:** HTTP con la clave propia del móvil (emparejado con QR) o, en pruebas, el token compartido; el texto de los mensajes solo viaja en casos dudosos.
- **Guardián ↔ backend:** el enlace de un solo uso es la llave; su Peri pide la petición en JSON y nunca se fía del texto del SMS.
- **Agente ↔ modelos:** el texto de un mensaje siempre va delimitado como dato; el modelo ajusta la puntuación como máximo ±2 y nunca anula una regla dura.

Seguridad: amenazas, defensas y lo que queda abierto en [`SECURITY.md`](SECURITY.md).
