# Muse Glimmer

Primera opinión privada sobre mensajes dudosos, antes de llegar a Muse Spark.

- Modelo: `meta-models/Muse-Glimmer-30B` (30B, Apache 2.0, texto e imagen, 131K de contexto).
- Memoria: ~72 GB de GPU en bf16; ~17 GB en GGUF Q4_K_M.
- Formato: chat completions de OpenAI servido con vLLM (`--tool-call-parser muse_glimmer --reasoning-parser muse_glimmer`).
- Razonamiento: va en el mensaje `system` como `Reasoning strength: low|medium|high|xhigh` y gasta tokens de salida. Con un límite bajo la respuesta sale vacía.
- Muestreo recomendado: `temperature 1.0`, `top_p 0.95`, `top_k 64`.
- Cliente: `backend/src/models/glimmer.ts`.

## Opciones

| Opción | Coste | Notas |
|---|---|---|
| Baseten (H100, catálogo) | ~0,108 $/min, 100 $ de créditos | Un clic; con escalado a cero solo se paga mientras responde |
| Modal (A100 80 GB) | ~2,50 $/h por segundo, 100 $ de créditos | Hay que adaptar el ejemplo de vLLM |
| Equipo propio | Gratis | Con 16 GB de VRAM, el GGUF Q4 no cabe entero en la GPU |

## Desplegar en Baseten con escalado a cero

1. Reclama los créditos de Baseten del hackathon y pon un límite o aviso de gasto en *Billing*.
2. Abre <https://www.baseten.co/library/muse-glimmer-30b/> y pulsa **Deploy now**. Deja la GPU por defecto (1× H100).
3. En el modelo, abre **Autoscaling** y pon:
   - **Min replicas: 0** (escalado a cero: sin peticiones no se paga).
   - **Max replicas: 1**.
   - **Scale down delay:** el mínimo que permita (unos minutos); cada minuto encendido cuesta ~0,11 $.
4. Crea una clave en **Settings → API keys**.
5. Copia el ID del modelo (aparece en el panel y en la URL `model-<id>.api.baseten.co`).
6. Añade a `backend/.env` (nunca al repo):
   ```
   BASETEN_MODEL_ID=<id>
   BASETEN_API_KEY=<clave>
   ```
7. Mide la latencia (1 petición de arranque + 5 mensajes sintéticos):
   ```
   cd backend; bun ../models/glimmer/latencia.ts
   ```
   La primera petición despierta el modelo y puede tardar varios minutos.
8. Si no se va a usar en días, **desactiva** el despliegue desde el panel.

Para un servidor vLLM propio (Modal o local) basta con `GLIMMER_BASE_URL=<url>/v1` y `GLIMMER_API_KEY`, que tienen prioridad sobre Baseten.

## Latencia medida

Pendiente.
