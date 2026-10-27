# Arquitectura

Las reglas del móvil deciden casi todo; lo dudoso pasa primero por Muse Glimmer en casa y solo si sigue dudoso llega a Muse Spark en la nube.

```
Notificaciones ─┐
Estado llamada ─┼─► Reglas locales ──normal/estafa──────────────┐
Agenda/círculo ─┘        │ dudoso                               ▼
                         ▼                                 Agente Peri ──► Peri habla ──sí──► Acciones
                  Muse Glimmer (casa) ──aún dudoso──► Muse Spark (nube, investigador con herramientas)
Voz y cámara (Muse Voice Transcribe) ─────────────────────────► │
                                                              riesgo alto
                                                                 ▼
                                                    Backend de aprobaciones ──► Móvil del guardián
```

## Módulos

| Ruta | Qué contiene |
|---|---|
| `core/.../model` | Tipos compartidos: mensajes, contactos, canales, niveles |
| `core/.../shield` | Escudo por capas: señales, puntuación, reglas duras, acción segura, explicación |
| `core/.../voice` | Órdenes de voz en español y resolución de contactos por alias |
| `core/.../agent` | Bucle del agente, herramientas, permisos y verificación de resultados |
| `core/.../trust` | Círculo de confianza, niveles de riesgo, MFA familiar |
| `core/.../alerts` | Avisos a la familia y resumen semanal |
| `android/.../ui/peri` | Pantalla de un botón y estados de Peri |
| `android/.../services` | Lector de notificaciones, estado de llamada y filtro de llamadas (`CallScreeningService`) |
| `android/.../device` | Llamadas, SMS, ubicación, voz y lectura en voz alta |
| `android/.../config` | Configuración de la familia, emparejamiento por QR y permisos progresivos |
| `android/.../net` | Cliente del backend |
| `backend/src/approvals` | Enlaces de un solo uso y página del guardián |
| `backend/src/investigator` | Agente investigador y sus herramientas de solo lectura |
| `backend/src/models` | Clientes de Muse Spark, Glimmer, Voice Transcribe y SAM 3.1 |
| `training/` | Base de conocimiento, scripts de dataset, pesos aprendidos y fine-tuning (datos fuera del repo) |

## Contratos

- **core ↔ Android:** interfaz `DevicePort` (`core/.../agent/DevicePort.kt`). Android la implementa; los tests usan un falso.
- **Android ↔ backend:** HTTP con token de dispositivo; el texto de los mensajes solo viaja en casos dudosos.
- **Agente ↔ modelos:** el texto de un mensaje siempre va delimitado como dato; el modelo ajusta la puntuación como máximo ±2 y nunca anula una regla dura.
