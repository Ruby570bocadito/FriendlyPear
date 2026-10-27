# Estado IMP-1

## Hecho (24 h)
- escudo-capas: señales, log-odds, reglas duras, voto del modelo ±2, acción segura y frase de Peri.
- creditos-y-spark: `backend/src/models/` (cliente común `chat.ts`, `spark.ts`).
  - Meta Model API en `https://api.meta.ai/v1`, formato OpenAI; clave `LLM|id|secreto`; `muse-spark-1.3`.
  - El razonamiento cuenta en `max_completion_tokens` incluso con "minimal" (~150 tokens): por defecto 400, tope 512.
  - `tool_choice` solo "auto"; llamadas en paralelo. Pruebas reales: 3 peticiones, < 0,01 $.
- glimmer-servido (preparado, sin medir): `glimmer.ts`, `models/glimmer/latencia.ts`, pasos en `models/glimmer/README.md`.
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

## Siguiente
- Capa de pesos abiertos desbloqueada: `BASETEN_API_KEY` en `backend/.env`; Model APIs de Baseten (`https://inference.baseten.co/v1`, `Authorization: Api-Key`) sirve `meta-models/muse-spark-open` (herramientas, 1M de contexto, 1,25 $/4,25 $ por millón, de los créditos de Baseten). Sin desplegar H100.

## Peticiones
- PUL: poner `META_API_BASE=https://api.meta.ai/v1` en `backend/.env.example`.
- TEST: pasar la prueba 20/20 también por `Shield` (capa 1 sola) para tener la fila de reglas; añadir mensajes en inglés.
- IMP-2: `call`/`sendSms` lanzan excepción si fallan (hecho en `AndroidDevice`).

## Propuestas para el roadmap
- `muse-spark-1.3-contributor` es ~20 veces más barato, pero Meta entrena con los prompts: solo para datos sintéticos.
- pesos-aprendidos debe producir un `ShieldWeights` que `Shield` ya acepta.
- Ampliar `Brands.ALL` con bancos y empresas del Reino Unido y EE. UU. (Barclays, HSBC, Royal Mail, HMRC, USPS…) para la demo en inglés.
