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
- circulo-reglas: `trust/CircleRules.kt`. El primer guardián se pone en el momento; después, cambiar guardianes pide el sí de un guardián actual (enlace de MFA, tipo `circle_change`, a todos los guardianes) y espera 48 h; la persona puede cancelar. La espera se retoma si la app se reinicia (`approvalId`). Probado: "add Marisa" aprobado → "It applies on Oct 29".
- investigador: `investigator/` (`tools.ts`, `knowledge.ts`, `investigate.ts`), `POST /v1/investigate` (token, solo DOUBT). Muse Spark con 3 herramientas de solo lectura: `domain_age` (RDAP), `scam_patterns` (10 fichas es/en) y `family_has` (solo relaciones, nunca nombres). Máx. 3 rondas y 6 consultas; la última va sin herramientas. Prueba real: paquete de Correos → scam +1,9; farmacia → ok −1,3; 4 llamadas, ~5,4 k tokens.
- investigador-en-el-movil: si tras Spark Open sigue en DOUBT, `OpinionClient.investigate` (30 s) manda también las relaciones de la familia (nunca nombres); los dos votos juntos se recortan a ±2. La evidencia (nunca el mensaje) se guarda en el móvil (`config/InvestigationLog`, últimas 10) y se ve en la configuración de la familia. Emulador: excursión del cole → 0 + 0 → dudoso (9 s); "cuota del gimnasio" con enlace → +1,8 + 1,7 → estafa (8 s).
- enlaces-inseguros: `Links.insecure` + `Signal.INSECURE_LINK` (2,5): un enlace de la familia con `http://` ya es dudoso. Emulador: "fotos de la boda" con `http://` → "its link has no padlock…". La prueba 20/20 (solo reglas) no cambia.
- pedir-ayuda: `VoiceCommands` (`askForHelp`, `helpTail`) y `Relation.PARENT`. Emulador: "avisa a mi hija que necesito ayuda" → "Sure! Shall I call Ana?".
- pausa-segura: `PeriAgent.onAppOpened` + `RecentRisk` (30 min tras un timo, un dudoso o una llamada desconocida contestada) → `DevicePort.showPause`; `onPauseAnswer` (llamar, salir, seguir); el "sí" hablado también llama. `watchesApps` mantiene el vigilante de apps solo mientras hay riesgo.
- vista-guardian (backend): `guardians/` (`store.ts`, `page.ts`), `POST/DELETE /v1/guardians/links`, `POST /v1/events` (solo nivel y canal, o la respuesta de la pausa), `GET /g/:token`. En disco (`backend/data/guardians.json`, ignorado por git): sobrevive a un reinicio (probado).
- Tests: core (179) y backend (58) en verde.

## En curso
- Nada.

## Bloqueado
- Nada.

## Peticiones
- PUL: poner `META_API_BASE=https://api.meta.ai/v1` en `backend/.env.example`.
- SEG: hecho a petición de la persona (`backend/src/limits.ts`: límites por IP, bloqueo por token malo, cuerpo limitado, cabeceras; 62 tests). Queda revisar las rutas con ojos de SEG.

## Propuestas para el roadmap
- Resuelto: la aprobación de las 09:39 fue un toque de la persona en la ventana del emulador. Investigado: ni la espera ni WAKEUP, BACK o ENDCALL aprueban nada; abrir el enlace solo hace GET.
- Entre dos emuladores, el SMS largo (varias partes) no llegó; en la prueba se entregó con `adb emu sms send`. Probar en móviles reales.
- `muse-spark-1.3-contributor` es ~20 veces más barato, pero Meta entrena con los prompts: solo para datos sintéticos.
- pesos-aprendidos debe producir un `ShieldWeights` que `Shield` ya acepta.
