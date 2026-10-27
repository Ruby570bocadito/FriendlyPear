# Entrenamiento y datos

Cómo hacemos que los modelos detecten mejor el phishing, el smishing y el vishing, y cómo lo medimos con honestidad. Se avanza de lo barato y seguro a lo ambicioso: si una capa falla, las anteriores siguen funcionando.

## 1. Qué detectamos

| Tipo | Canal | Cómo lo ve Peri |
|---|---|---|
| Smishing | SMS | Texto del mensaje + remitente + agenda |
| Phishing | Correo | Texto de la notificación + dominio de los enlaces |
| Estafa por mensajería | WhatsApp | Texto + "he cambiado de número" + agenda |
| Vishing | Llamada | **Señales** alrededor de la llamada (número, código durante la llamada, app del banco o de control remoto) + **lo que cuenta la persona** al colgar |
| Estafa visual | Cartas, facturas, QR, capturas | Muse Spark multimodal + SAM 3.1 |

> Peri **nunca escucha el audio de las llamadas**: Android no lo permite a las apps y tendría problemas legales.

## 2. Taxonomía de estafas (España)

Cada mensaje de entrenamiento lleva una de estas etiquetas (o `legitimo`):

| Etiqueta | Ejemplo típico |
|---|---|
| `paqueteria` | "Correos/SEUR: paquete retenido, pague 1,99 €" |
| `trafico` | "DGT: multa pendiente, evite el recargo" |
| `hacienda` | "Agencia Tributaria: tiene un reembolso pendiente" |
| `seguridad_social` | "Su tarjeta sanitaria caduca" |
| `banco_acceso` | "Acceso no autorizado; verifique su identidad" |
| `banco_codigo` | "Díganos el código que le ha llegado" (vishing) |
| `familiar_numero_nuevo` | "Mamá, este es mi número nuevo" |
| `familiar_urgencia` | "Abuela, he tenido un accidente, necesito dinero" |
| `soporte_tecnico` | "Su ordenador está infectado; instale AnyDesk" |
| `inversion` | "Invierte 250 € y gana 3.000 € al mes" |
| `premio` | "Has ganado un iPhone; reclama tu premio" |
| `suministros` | "Corte de luz por impago" |
| `suscripcion` | "Netflix: problema con su pago" |
| `romance` | Relación a distancia que acaba pidiendo dinero |
| `compra_falsa` | Tienda online falsa o vendedor de segunda mano |

## 3. Las cinco capas de entrenamiento

| # | Capa | Qué es | Modelo | Coste | Cuándo |
|---|---|---|---|---|---|
| 1 | **Base de conocimiento** | Ficha por etiqueta: cómo funciona, señales, ejemplos reales, acción segura. El modelo la consulta antes de opinar | Muse Spark, Glimmer | ½ día | Día 2 |
| 2 | **Ejemplos en el prompt** | 10–20 ejemplos escogidos (estafas **y** mensajes normales) en cada consulta | Muse Spark, Glimmer | ½ día | Día 2 |
| 3 | **Pesos aprendidos** | Regresión logística que ajusta cuánto pesa cada señal del escudo a partir de datos etiquetados; sigue siendo explicable | Reglas del móvil | ½ día | Día 2–3 |
| 4 | **"Cuéntame la llamada"** | Al colgar, la persona describe la llamada con la voz; un clasificador entrenado con guiones de vishing dice si encaja con una estafa | Voice Transcribe + Spark/Glimmer | 1 día | Día 4 |
| 5 | **Fine-tuning con LoRA** | Afinar Muse Glimmer o Muse Spark Open con el conjunto de entrenamiento | Glimmer / Spark Open en Modal o Fireworks | 2 días | Días 5–6 (apuesta) |

**Capa 5, a comprobar el día 1:** si Modal o Fireworks permiten afinar los modelos Muse y con qué coste. El taller de Fireworks del **viernes 30 oct** (*From Inference to Training with Muse Open Models*) es la referencia. Si no es viable, nos quedamos con las capas 1–4.

## 4. Datos

| Fuente | Aporta | Cuidado |
|---|---|---|
| Avisos públicos de INCIBE y OSI | Estafas reales y actuales en España | Revisar condiciones de uso; citar la fuente |
| Datasets públicos de SMS spam y smishing | Volumen | Mayoría en inglés y antiguos; revisar licencias |
| Variantes sintéticas generadas con Muse Spark | Variedad dentro de cada etiqueta | El modelo puede aprender el estilo sintético: nunca se evalúa con ellas |
| Mensajes normales propios y de familiares, anonimizados y con permiso | **Mensajes legítimos**, clave para no tener falsas alarmas | Nunca suben al repo |
| Guiones de vishing descritos en avisos públicos | Capa 4 | Se reescriben como relatos en primera persona |

**Proporción:** como en la vida real, muchos más mensajes legítimos que estafas (objetivo de unos 3 legítimos por cada estafa en entrenamiento).

**Objetivo de tamaño:** unos 1.500 ejemplos de entrenamiento y **200 reales reservados** para evaluar.

### Formato

`training/data/` (ignorado por git), un JSON por línea:

```json
{"id":"m-0042","canal":"SMS","remitente_en_agenda":false,"llamada_activa":false,
 "texto":"Correos: su paquete está retenido...","etiqueta":"paqueteria",
 "origen":"incibe|publico|sintetico|propio","fecha":"2026-10-28"}
```

### Anonimización (obligatoria antes de guardar)

Nombres → `[NOMBRE]` · teléfonos → `[TELEFONO]` · IBAN y tarjetas → `[CUENTA]` · direcciones → `[DIRECCION]`. Los dominios de estafa se conservan (son la señal).

## 5. Evaluación honesta

1. **Conjunto reservado:** 200 mensajes reales recogidos sin mirar las reglas; **nunca** se entrena ni se ajusta con ellos.
2. **Baseline:** el mismo modelo sin base de conocimiento, sin ejemplos y sin contexto de agenda.
3. **Métricas:** TPR, FPR, **falsas alarmas al mes** (objetivo ≤ 1), acción segura correcta, todo con intervalo de Wilson.
4. **Tabla de resultados** para el README y el vídeo:

| Configuración | TPR | FPR | Falsas alarmas/mes |
|---|---|---|---|
| Baseline (Spark sin contexto) | — | — | — |
| + base de conocimiento y ejemplos | — | — | — |
| + pesos aprendidos y agenda | — | — | — |
| + Glimmer afinado | — | — | — |

Las celdas se rellenan solo con resultados medidos.

## 6. Ética

- Los ejemplos de estafas se usan **solo para defender**; no se publica ningún generador de estafas.
- El repo solo contiene ejemplos anonimizados y las fichas de la base de conocimiento.
- Nada de datos de terceros sin permiso.

## 7. Entrenamiento de personas (bonus)

Además de entrenar modelos, Peri puede entrenar a la persona: tras frenar una estafa, una **viñeta de 3 escenas** generada con Muse Image explica el truco ("así funciona esta trampa"). Es prioridad P2.
