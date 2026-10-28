# Roadmap — FriendlyPear

Vista general para personas. La lista de tareas que usan los agentes (con IDs, slugs y rutas) está en [`agentes/ROADMAP.md`](agentes/ROADMAP.md).

**28 oct, noche:** el roadmap de los agentes empieza de cero (ronda 5) con todo lo que falta junto: móvil de verdad, chat y detección en serio, Peri por capas en todo, UI y vídeo. Lo de las rondas 1 a 4 está en [`agentes/ROADMAP_RONDAS_1-4.md`](agentes/ROADMAP_RONDAS_1-4.md).

**Regla de oro:** todo lo P0 funciona en un móvil (o emulador) el **31 de octubre**. Si algo se retrasa, se recorta primero lo P2; la demo P0 nunca se recorta.

## Hitos

| Fecha | Hito | % |
|---|---|---|
| Mar 27 oct | Validación de modelos y API | 10 |
| Jue 29 oct | Núcleo listo + momentos 1 y 2 en el móvil | 50 |
| Sáb 31 oct | MFA familiar, avisos, confianza y emergencia | 75 |
| Dom 1 nov | Meta a fondo: Glimmer, "mira esta imagen", Peri | 85 |
| Lun 2 nov | Evaluación con métricas y personas mayores | 95 |
| Mar 3 nov | Vídeo de 3 min y post en X | 98 |
| Mié 4 nov | **Entrega antes de las 18:00 ET** | 100 |

## Día a día

### Día 1 · martes 27 oct — validar lo desconocido
- [ ] Repositorio público con la estructura base
- [ ] Reclamar los 150 $ de créditos y poner límite de gasto
- [ ] Probar Muse Spark: formato de la API y llamadas a herramientas
- [ ] Probar Muse Voice Transcribe con 10 órdenes en español dichas por personas mayores
- [ ] Servir Muse Glimmer (local, Modal o Baseten) y medir latencia
- [ ] Prueba 20/20: 20 estafas y 20 mensajes reales anonimizados
- [ ] Comprobar si Modal o Fireworks permiten afinar Muse Glimmer / Spark Open
- [ ] Empezar a recoger los 200 mensajes reales reservados para evaluar
- [ ] Decidir el pivote si el modelo falla en más de un tercio

### Día 2 · miércoles 28 oct — núcleo y datos
- [ ] Base de conocimiento: fichas de los 15 tipos de estafa
- [ ] Conjunto de entrenamiento (~1.500 ejemplos, anonimizado) y ejemplos para el prompt
- [ ] Pesos de las reglas aprendidos con regresión logística
- [x] Escudo por capas con tests
- [ ] Voz en español, alias y relaciones
- [ ] Agente con confirmaciones y verificación de resultados
- [ ] Android: lector de notificaciones, estado de llamada, voz y lectura en voz alta
- [ ] Revisión de permisos mínimos

### Día 3 · jueves 29 oct — primer flujo real
- [ ] Permisos de uno en uno con explicación de Peri; la app funciona con cualquier permiso denegado
- [ ] Momento 1: "he cambiado de número" → llamada a la hija real
- [ ] Momento 2: código durante llamada → "cuelga"
- [ ] Agente investigador con herramientas
- [ ] Interfaz en Jetpack Compose con la paleta de colores de `DISENO.md`
- [ ] Peri animada v1: tranquila, escuchando, pensando, preocupada, protectora

### Día 4 · viernes 30 oct — familia y llamadas
- [ ] "Cuéntame la llamada": clasificador de guiones de vishing
- [ ] Taller de Fireworks (12:00 ET): entrenamiento con modelos Muse abiertos
- [ ] Llamadas: aviso de número fuera de la agenda (`CallScreeningService`) y "¿quién decía ser?" al colgar
- [ ] Palabra de la familia y "cuelga y devuelve la llamada" si piden dinero
- [ ] Backend de aprobaciones (enlace de un solo uso)
- [ ] MFA familiar de punta a punta con dos dispositivos
- [ ] Avisos a la familia y "ataque en curso"
- [ ] Endurecer el backend: cabeceras y límites

### Día 5 · sábado 31 oct — confianza y emergencia
- [ ] "¿Te están guiando por teléfono?": app de control remoto instalada y app del banco durante una llamada
- [ ] (Apuesta) Fine-tuning con LoRA de Glimmer, parte 1
- [ ] "Me he caído": cuenta atrás, llamada y ubicación
- [ ] Pantalla de configuración de la familia
- [ ] Panel "Lo que Peri puede hacer": permisos, para qué sirven y registro de uso
- [ ] Emparejamiento por QR
- [ ] Reglas del círculo: segundo guardián y 48 h

### Día 6 · domingo 1 nov — Meta a fondo y Peri
- [ ] (Apuesta) Fine-tuning de Glimmer, parte 2 y comparación con el baseline
- [ ] Imagen de referencia de Peri y prueba de Higgsfield
- [ ] (P2) Denunciar con un toque y viñeta "así funciona esta trampa"
- [ ] "Mira esta imagen" con Muse Spark + SAM 3.1
- [ ] Glimmer como primera opinión privada
- [ ] Estados extra de Peri (llamando, esperando, contenta, dormida) y escudos ganados
- [ ] Ilustraciones especiales con Muse Image
- [ ] (P2) Notificaciones basura de webs y DNS privado guiado

### Día 7 · lunes 2 nov — evaluación
- [ ] Métricas en el conjunto reservado (TPR, FPR, falsas alarmas/mes, Wilson) y tabla por capa de entrenamiento
- [ ] 20 tareas con 2–3 personas mayores (con permiso)
- [ ] README con resultados y límites

### Día 8 · martes 3 nov — demo
- [ ] Grabación real de los 5 momentos de la demo
- [ ] Escenas de historia con Higgsfield (ver `VIDEO.md`)
- [ ] Montaje del vídeo de demo de 3 minutos
- [ ] Anuncio de 30–45 s vertical y cuadrado
- [ ] Post en X con el anuncio
- [ ] Instalación desde cero siguiendo el README
- [ ] Dudas de entrega en Discord antes de que acabe el día

### Día 9 · miércoles 4 nov — entrega
- [ ] Entregar antes de las 18:00 ET
- [ ] Comprobar repo público, vídeo accesible y enlace del post

## Prioridades

| Prioridad | Qué incluye |
|---|---|
| P0 (sí o sí) | Escudo, verificación por agenda, código durante llamada, voz, MFA familiar, avisos, permisos con consentimiento |
| P1 (importante) | "¿Te están guiando?", entrenamiento capas 1–4, verificación de llamadas, palabra de la familia, agente investigador, mira esta imagen, círculo con QR, emergencia, Peri, panel de permisos |
| P2 (si sobra tiempo) | Protección web básica, denunciar con un toque, viñeta educativa, fine-tuning de Glimmer |
| Después | App Familia, guía visual, filtro web propio, iOS, Google Play |

## Riesgos y plan B

| Riesgo | Plan B |
|---|---|
| El rol de filtro de llamadas no se concede en el emulador | Probar en móvil real; en emulador, aviso al terminar la llamada |
| Muse Spark sin llamadas a herramientas | Plan en JSON que ejecuta la app |
| Glimmer no corre en local | Modal o Baseten |
| Voice Transcribe falla en español | Reconocedor de Android |
| Android bloquea llamadas en segundo plano | Servicio en primer plano |
| WhatsApp no funciona en el emulador | Probar con SMS; WhatsApp en móvil real |
| Falta de tiempo | Recortar P2; nunca la demo P0 |
| No se puede afinar Glimmer a tiempo | Quedarse con las capas 1–4 de entrenamiento |
| Escenas de Higgsfield poco realistas o caras | Vídeo solo con grabaciones reales y Peri animada de la app |
