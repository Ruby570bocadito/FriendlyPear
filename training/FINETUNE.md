# ¿Es viable un LoRA? (finetune-viable, 27 oct)

Comprobado en la web, sin gastar créditos ni lanzar ningún entrenamiento.

| Modelo | ¿Se puede afinar? | Dónde | Coste | Veredicto |
|---|---|---|---|---|
| **Muse Glimmer** (30B) | Sí, LoRA o QLoRA | Unsloth en una GPU de 24 GB; gratis en Kaggle (2× T4). Fireworks lo sirve (`accounts/fireworks/models/muse-glimmer-30b`), pero no hay guía suya para afinarlo | Entrenar: 0 $ en Kaggle. Servir el adaptador: un despliegue propio (Fireworks bajo demanda, Modal o una GPU local), que ya no es por token | Viable técnicamente; caro de servir para lo que aporta |
| **Muse Spark Open** (816B MoE, 42B activos) | Una fuente cita LoRA por API en Fireworks; sin precio publicado en ningún proveedor | Fireworks | Desconocido; descomunal para el presupuesto | No viable aquí |

## Por qué no lo hacemos en el hackathon

- **Ya aprendimos lo que el dataset enseña.** Los 1.306 mensajes sintéticos entrenaron los pesos del escudo (`training/fit.py`). En el conjunto reservado de 200, los pesos aprendidos ya suben el AUC y los avisos firmes, aunque no son los de por defecto por una falsa alarma firme (`eval/REPORT.md`).
- **El modelo solo vota ±2.** Las reglas deciden y el modelo inclina la balanza en los dudosos. Un Glimmer afinado cambiaría poco el resultado final.
- **Servirlo cuesta más que entrenarlo.** La segunda opinión actual (Spark Open en Baseten) se paga por token y cuesta céntimos. Un adaptador propio necesita una máquina encendida.
- **Riesgo para la evaluación.** Habría que medirlo otra vez en el reservado (finetune-glimmer-2), sin mezclarlo nunca con los datos de entrenamiento (D5).

## Si se retoma

1. Unsloth en Kaggle, LoRA de rango 16 (alpha 32, lr 1e-4, secuencias de 2.048), con `training/data/` y ejemplos razonados mezclados con respuestas directas, como recomienda la guía de Unsloth.
2. Servirlo en Fireworks (despliegue bajo demanda) o en local con `models/glimmer/`.
3. Comparar con Spark Open en `eval/reservado-200` con el mismo `fit.py`: tasa de aciertos, falsas alarmas e intervalos de Wilson.

Fuentes: [guía de Unsloth para Muse Glimmer](https://unsloth.ai/docs/models/muse-glimmer/train), [Muse Glimmer 30B en Fireworks](https://www.typingmind.com/guide/fireworks-ai/accounts-fireworks-models-muse-glimmer-30b), [precios y pesos de Muse Spark Open](https://capitalandcompute.net/blog/muse-spark-open-pricing-benchmarks/).
