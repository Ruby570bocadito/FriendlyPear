# Estado IMP-1

## Hecho (24 h)
- escudo-capas: señales, log-odds, reglas duras, voto del modelo ±2, acción segura y frase de Peri; marcas de Reino Unido y EE. UU.; señal `CODE_FORWARD_REQUEST`.
- creditos-y-spark: `backend/src/models/` (`chat.ts`, `spark.ts`). Meta Model API en `https://api.meta.ai/v1`, `muse-spark-1.3`; el razonamiento cuenta en `max_completion_tokens` (por defecto 400, tope 512).
- voz-y-agenda: órdenes, alias, diminutivos, apodos, relaciones; `RelationWords` para "Laura hija".
- agente-base: `PeriAgent` con confirmación por voz, riesgo, verificación, plan B, lectura segura y emergencia.
- peri-bilingue y peri-personalidad: `Lang.ES`/`Lang.EN`, `Phrases`/`PeriExplainer`, guía `docs/PERI_VOZ.md`.
- segunda-opinion: Muse Spark Open en Baseten Model APIs (`inference.baseten.co/v1`); `POST /v1/opinion`, solo DOUBT, voto ±2; en el móvil, el dudoso espera el voto y Peri habla una vez.
- llamadas: aviso antes de descolgar si el número no está en la agenda; al colgar, "¿quién decía ser?" → llamada al número guardado; palabra de la familia (se recuerda, nunca se dice).
- mfa-familiar:
  - Backend `approvals/`: enlace de un solo uso (solo se guarda su hash), 15 min, `POST /v1/approvals`, `GET /v1/approvals/:id`, página del guardián `/a/:token` (es/en, "No" primero, sin scripts). Silencio = expirado; nunca se aprueba solo.
  - `PeriAgent`: riesgo alto (dictar un código) → `ApprovalPort`; el SMS al guardián lleva el enlace, nunca el código; "cancela" anula la petición.
  - Android: `net/ApprovalClient.kt` crea, manda el SMS y consulta cada 3 s.
  - Dos emuladores (5554 protegido, 5556 guardiana "Ana hija"): abrir el enlace solo hace GET; pulsar "Yes, I approve" → `APPROVED` → se manda "the code is 7350" a Marisa.
  - Registro de auditoría de `/a/` con método y navegador, nunca el enlace.
- Tests: 134 en `core`, 44 en el backend.

## En curso
- Nada.

## Bloqueado
- Nada.

## Peticiones
- PUL: poner `META_API_BASE=https://api.meta.ai/v1` en `backend/.env.example`.
- SEG: revisar las rutas nuevas de `server.ts` (`/v1/approvals`, `/a/:token`) y añadir límite de peticiones a `/a/`.

## Propuestas para el roadmap
- Resuelto: la aprobación de las 09:39 fue un toque de la persona en la ventana del emulador. Investigado: ni la espera ni WAKEUP, BACK o ENDCALL aprueban nada; abrir el enlace solo hace GET.
- Entre dos emuladores, el SMS largo (varias partes) no llegó; en la prueba se entregó con `adb emu sms send`. Probar en móviles reales.
- `muse-spark-1.3-contributor` es ~20 veces más barato, pero Meta entrena con los prompts: solo para datos sintéticos.
- pesos-aprendidos debe producir un `ShieldWeights` que `Shield` ya acepta.
