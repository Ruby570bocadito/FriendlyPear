# Pesos aprendidos del escudo — 27 oct

- Conjunto reservado: 200 mensajes escritos a mano (`eval/reservado-200/`), solo para evaluar. Entrenamiento: 1.306 sintéticos con `muse-spark-1.3-contributor` (`training/generate.ts`, ~180 k tokens, céntimos), solo para ajustar.
- Fallo del primer intento: el cliente de chat recorta las respuestas a 512 tokens y los lotes de 10 mensajes llegaban cortados; nada se guardaba. Ahora lotes de 5, se aprovechan los mensajes completos de una respuesta cortada, 4 llamadas a la vez y un registro por lote.
- Regresión desde cero: peor que los pesos a mano (TPR 77,5 % frente a 83,3 %): aprende el estilo sintético (casi todo timo sintético lleva enlace; "dime el código" casi sin peso).
- Regresión con los pesos a mano como punto de partida (fuerza por validación cruzada): AUC 0,965 frente a 0,957; avisos firmes 66,7 % frente a 52,9 % con el mismo 1 % de falsas alarmas firmes; dudosas falsas 2,0 % frente a 4,1 %.
- Pero en 20/20 convierte el aviso real del gimnasio en estafa firme (urgencia + dinero de un número no guardado). Análisis de errores: faltaban recordatorios de pago legítimos en el entrenamiento; se añadió esa categoría (50 mensajes) y bajaron las falsas dudosas, pero el gimnasio queda justo en el umbral (2,0).
- Decisión: el escudo sigue con los pesos a mano; `ShieldWeights.LEARNED` queda en el core con un test de sus promesas. Siguiente paso: mensajes legítimos reales de familias.
- De paso: faltaba `lloydsbank.co.uk` en las marcas (un correo real de Lloyds disparaba la regla dura). Arreglado.
