# Zonas

| ID | Rutas |
|----|-------|
| DIR | docs/agentes/ROADMAP.md, docs/agentes/OWNERSHIP.md, docs/agentes/PROTOCOLO.md, docs/CONCEPTO.md, docs/demo/ |
| IMP-1 | core/src/ (model, shield, voice, agent, trust, alerts y tests), backend/src/, backend/test/, models/glimmer/, training/ |
| IMP-2 | android/app/src/ (ui/peri, services, device, config, net, res), android/app/build.gradle.kts, assets/peri/ |
| PUL | README.md, .gitignore, .github/, docs/ARQUITECTURA.md, ficheros de build (core/*.kts, android/*.kts, backend/package.json, tsconfig.json), wrappers de Gradle, limpieza y refactors sin cambio de comportamiento |
| SEG | backend/src/server.ts y la autenticación, AndroidManifest.xml (permisos), emparejamiento QR, reglas duras del escudo (con claim); fixes mínimos en cualquier ruta con claim |
| TEST | eval/, docs/agentes/hallazgos/. Nunca modifica código de producto |

Zona compartida (claim obligatorio, cambios aditivos): `core/.../model/` y `core/.../agent/DevicePort.kt` (contrato core ↔ Android).
