# Estado IMP-1

## Hecho (24 h)
- escudo-capas: señales, log-odds, reglas duras, voto del modelo ±2, acción segura y frase de Peri.
- creditos-y-spark: `backend/src/models/` (cliente común `chat.ts`, `spark.ts`).
  - Meta Model API en `https://api.meta.ai/v1`, formato OpenAI; clave `LLM|id|secreto`; `muse-spark-1.3`.
  - El razonamiento cuenta en `max_completion_tokens` incluso con "minimal" (~150 tokens): por defecto 400, tope 512.
  - `tool_choice` solo "auto"; llamadas en paralelo. Pruebas reales: 3 peticiones, < 0,01 $.
- voz-y-agenda: órdenes, alias, diminutivos, apodos, relaciones; `RelationWords` para "Laura hija".
- agente-base: `PeriAgent` con confirmación por voz, riesgo, verificación, plan B, lectura segura y emergencia.
- peri-bilingue: `Lang.ES`/`Lang.EN`.
  - Peri habla el idioma del móvil (`Phrases` en el agente, `PeriExplainer` en el escudo); TTS y reconocedor igual.
  - Órdenes y señales de estafa se entienden siempre en las dos lenguas.
  - Momentos 1 y 2 probados en inglés en el emulador; 101 tests en `core`.
- peri-personalidad: guía `docs/PERI_VOZ.md` y `PROTOCOLO.md` §10; frases del escudo, del agente y de la UI más cálidas en las dos lenguas; voz TTS 0,9 de ritmo, 1,1 de tono y la mejor voz sin conexión.

## En curso
- Nada.

## Bloqueado
- Nada.

- segunda-opinion: Muse Spark Open en Baseten Model APIs (`inference.baseten.co/v1`, `Bearer`, sin desplegar H100).
  - `investigator/opinion.ts` + `POST /v1/opinion` con token del dispositivo: solo nivel DOUBT; JSON scam/doubt/ok + confianza → voto ±2.
  - 2 peticiones reales (~0,005 $): sobrino pidiendo dinero → scam +1,9 (2,8 s); dentista → ok −1,86 (2,0 s). Razona 325–561 tokens: tope 900.

- opinion-en-el-movil: `PeriAgent.onMessage(…, secondOpinion = true)` espera en silencio y `onModelOpinion` aplica el voto (o nada si falla); `net/OpinionClient.kt` en Android.
  - Emulador + backend local (8711), 2 peticiones: sobrino → DOUBT, +1,9 → SCAM y aviso (2,6 s); gimnasio → DOUBT, −1,9 → OK, en silencio (2,1 s). 110 tests en `core`.

## Peticiones
- PUL: poner `META_API_BASE=https://api.meta.ai/v1` en `backend/.env.example`.
- TEST: pasar la prueba 20/20 también por `Shield` (capa 1 sola) para tener la fila de reglas; añadir mensajes en inglés.
- IMP-2: `call`/`sendSms` lanzan excepción si fallan (hecho en `AndroidDevice`).

## Propuestas para el roadmap
- `muse-spark-1.3-contributor` es ~20 veces más barato, pero Meta entrena con los prompts: solo para datos sintéticos.
- pesos-aprendidos debe producir un `ShieldWeights` que `Shield` ya acepta.
- Hecho: marcas de Reino Unido y EE. UU. en `Brands.ALL` y señal `CODE_FORWARD_REQUEST` (pedir que reenvíes un código que te han mandado).
