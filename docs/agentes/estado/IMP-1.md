# Estado IMP-1

## Hecho (24 h)
- escudo-capas: `core/.../shield/` (señales, log-odds, reglas duras, voto del modelo ±2, acción segura, frase de Peri; 37 tests).
- creditos-y-spark: `backend/src/models/spark.ts` + 10 tests sin red.
  - Meta Model API: `https://api.meta.ai/v1/chat/completions`, formato OpenAI, `Authorization: Bearer`. La antigua `api.llama.com` ya no sirve modelos.
  - Clave con formato `LLM|id|secreto`. `/v1/models` lista muse-spark-1.1/1.2/1.3 (+ contributor), muse-wonder, muse-image, muse-voice-transcribe y sam-3.1.
  - Herramientas: `tool_choice` solo admite `"auto"`; llamadas en paralelo por defecto (Spark pidió `consultar_agenda` para hijo e hija a la vez).
  - El razonamiento cuenta en `max_completion_tokens` incluso con `reasoning_effort: "minimal"` (~150 tokens): con 40 y 120 la respuesta salió vacía (`length`). Por defecto 400, tope 512.
  - Coste de las 3 pruebas: ~1.400 tokens de entrada y ~440 de salida (< 0,01 $).
  - Caché en memoria por petición idéntica; `untrusted()` delimita el texto del mensaje.

- glimmer-servido (preparado, sin medir): cliente `backend/src/models/glimmer.ts` sobre el cliente común `chat.ts`, script `models/glimmer/latencia.ts` y pasos en `models/glimmer/README.md`.

## En curso
- voz-y-agenda.

## Bloqueado
- glimmer-servido, aparcado (P1) el 2026-10-27: los 150 $ de Meta Model API no incluyen Glimmer (solo se aloja en Together AI o en hardware propio). Créditos de Baseten solicitados el 27 oct: si llegan, la capa abierta será Muse Spark Open (`meta-models/muse-spark-open-1026`) en Baseten, que también cuenta para el premio de pesos abiertos; si no, Glimmer en el PC con Ollama (GGUF Q4, ~17 GB, 16 GB de VRAM). Se retoma el 30 oct.

## Peticiones
- PUL: poner `META_API_BASE=https://api.meta.ai/v1` en `backend/.env.example`.
- TEST: pasar la prueba 20/20 también por `Shield` (capa 1 sola) para tener la fila de reglas.

## Propuestas para el roadmap
- `muse-spark-1.3-contributor` cuesta ~20 veces menos, pero Meta entrena con los prompts: solo para datos sintéticos (20/20, variantes), nunca con mensajes reales de la familia.
- pesos-aprendidos debe producir un `ShieldWeights` (mapa señal → peso) que `Shield` ya acepta.
- Ampliar `Brands.ALL` con dominios oficiales verificados de más bancos y empresas.
