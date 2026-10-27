# FriendlyPear — la idea completa

> Peri, una pera amiga que vive en tu móvil, te lee los mensajes, llama a quien le pidas y, si algo huele a estafa, lo comprueba con tu familia antes de que pierdas un euro.

**Para cualquier persona y cualquier familia.** Las personas mayores son el ejemplo principal de la demo porque es donde el problema duele más, pero FriendlyPear sirve a padres que protegen a hijos adolescentes, hijos que protegen a sus padres, parejas, personas con discapacidad o cualquiera que reciba mensajes y llamadas.

## 1. El problema

Cualquiera puede caer en una estafa por SMS, WhatsApp, correo o teléfono, y las personas mayores son el blanco principal:

- "Mamá, se me ha roto el móvil, este es mi número nuevo, ¿me haces un Bizum?"
- "Correos: su paquete está retenido, pague 1,99 € de tasas."
- "Le llamamos del banco… le va a llegar un código, díganoslo para anular la operación."
- Una llamada: "Abuela, soy Pablo, he tenido un accidente, necesito dinero." Pero ese número no es el de Pablo, y la voz puede estar clonada con IA.

Lo que tienen hoy no les protege:

| Lo que existe | Por qué no basta |
|---|---|
| Asistentes de voz (Google, Siri) | Llaman y leen mensajes, pero no valoran si son estafas |
| Filtros de spam | Avisan de todo o de nada; sin explicación; acaban ignorados |
| Cursos de concienciación | No están en el momento del mensaje |
| Detección de estafas de Google en algunos Android | Detecta, pero no comprueba con la agenda ni implica a la familia |

**La clave:** las estafas funcionan porque **aíslan** a la víctima y le **meten prisa**. Hay que romper las dos cosas.

## 2. La solución

FriendlyPear es un **agente de voz para Android**, para cualquier persona y su familia, que:

1. **Protege:** analiza cada mensaje y explica en una frase si es una trampa.
2. **Comprueba por otro canal:** si un mensaje o una llamada dice ser de tu hija desde otro número, llama a tu hija a su número de siempre.
3. **Implica a la familia:** si quieres hacer algo arriesgado, un familiar de confianza lo aprueba desde su móvil (MFA familiar).
4. **Ayuda con la voz:** "llama al chico", "léeme los mensajes", "me he caído".
5. **Lo hace con cariño:** Peri nunca regaña y celebra que preguntes antes de actuar.
6. **Pide permiso:** Peri no toca tus contactos, llamadas ni mensajes sin explicarte antes para qué y sin tu "sí".

## 3. Para quién

| Persona | Dificultad | Qué necesita |
|---|---|---|
| Carmen, 78, vive sola | Vista cansada; confía en quien suena oficial | Que le lean los mensajes y le avisen de los peligrosos |
| Antonio, 82, Parkinson | Temblor; botones pequeños | Hacerlo todo con la voz |
| Luis, 45, discapacidad intelectual leve | Textos largos o ambiguos | Una frase y una acción clara |
| Rosa, 70, ciega, usa TalkBack | El lector de pantalla no la protege | Un escudo que conviva con TalkBack |
| Laura, 50, hija de Carmen (guardiana) | Vive lejos | Configurar, recibir avisos y aprobar lo arriesgado |
| Marta, 15, hija de Laura | Recibe estafas de premios y de "soporte" de juegos | Avisos claros sin que sus padres lean sus mensajes |
| Javier, 38 | Trabaja todo el día; recibe llamadas de "su banco" | Saber si quien llama es quien dice ser |

## 4. Los pilares

### 4.1 Escudo de mensajes
- Lee SMS, WhatsApp y correo desde las notificaciones (un solo permiso).
- **Tres niveles:** normal (silencio), dudoso ("¿lo comprobamos?"), estafa ("es una trampa, no lo toques").
- **Capas:** reglas en el móvil → Muse Glimmer en casa → Muse Spark en la nube (solo si sigue dudoso).
- **Señales:** remitente fuera de la agenda, dominio que imita a una marca, acortadores, enlaces sin TLS (`http://`), en un puerto que no es el 443 o con una IP en vez de un nombre (avisa siempre, aunque venga de la familia), prisa, petición de dinero o claves, "he cambiado de número", instrucciones escritas para la IA.
- **Reglas duras** (el modelo no puede anularlas): código recibido durante una llamada; dominio que imita a una marca.
- **Acción segura siempre:** llamar al contacto real, usar la app oficial, preguntar a la familia, no tocar el enlace.

### 4.2 Voz para todo
- Muse Voice Transcribe para entender a personas mayores (pausas, habla lenta).
- "Llama al chico" → alias configurados por la familia; diminutivos ("Pablito").
- "Léeme los mensajes" → nunca lee enlaces ni códigos en voz alta.
- Confirma siempre antes de llamar o enviar.

### 4.3 MFA familiar
- Acciones de **riesgo alto** (enviar dinero tras un aviso, dictar un código durante una llamada, instalar una app de control remoto, aceptar un "número nuevo") exigen la aprobación de un guardián.
- Enlace de un solo uso, 15 minutos; el guardián aprueba o rechaza y llama.
- Sin respuesta → esperar 24 horas; **nunca se aprueba solo**.

### 4.4 Círculo de confianza
- Niveles: guardián · contacto verificado · entidad oficial · conocido · desconocido.
- El primer guardián se empareja **en persona con un QR**.
- Cambios: segundo guardián + 48 horas + aviso a todos.
- El guardián **no** puede mover dinero, iniciar acciones ni leer los mensajes (evita abusos dentro de la familia).

### 4.5 Avisos a la familia
- **Inmediatos:** emergencia, código durante llamada, petición de aprobación, insistencia tras un rechazo, "ataque en curso" (3 mensajes sospechosos en 24 h), cambios en el círculo.
- **Resumen semanal:** "Esta semana Peri frenó 3 estafas".
- Llevan el tipo de estafa, nunca el texto de las conversaciones normales.

### 4.6 Emergencia
- "Me he caído" → cuenta atrás de 10 s ("di cancelar si estás bien") → llamada al familiar + SMS con la ubicación.
- No da consejo médico. Nunca marca números de emergencia en pruebas.

### 4.7 Peri y "mira esta imagen"
- **Peri:** mascota con estados (tranquila, escuchando, pensando, preocupada, protectora, contenta) ilustrados con Muse Image.
- **Mira esta imagen:** la persona enseña una carta, factura, QR o captura; Muse Spark la analiza y SAM 3.1 señala la parte peligrosa.

### 4.8 Llamadas: ¿de verdad es quien dice ser?

**El caso:** suena el teléfono: "Abuela, soy Pablo, he tenido un accidente, necesito dinero". Pero ese número **no es el de Pablo** en tu agenda.

| Momento | Qué hace Peri | Cómo en Android |
|---|---|---|
| Antes de descolgar | Si el número no está en la agenda: "Este número no está en tu agenda. Si dice ser de tu familia, no des dinero ni códigos." | `CallScreeningService` (API oficial que ve el número entrante) |
| Al colgar | "¿Quién decía ser?" → "mi nieto" → "Ese número no es el de Pablo. ¿Llamo a Pablo a su número de siempre?" | Estado de llamada + voz + agenda |
| Si piden dinero | MFA familiar | Pilar 4.3 |

- **Palabra de la familia:** la familia acuerda una palabra secreta; cuando alguien dice ser un familiar, Peri recuerda: "Pregúntale la palabra de la familia". Es la defensa contra **voces clonadas con IA**.
- **Ni el número correcto es prueba:** el identificador de llamada se puede falsificar. Si alguien con el número "correcto" pide dinero o códigos, Peri propone colgar y **devolver la llamada** al número guardado.
- **Límite:** Peri no escucha el audio de las llamadas (Android no lo permite a las apps y tendría problemas legales). Actúa antes y después de la llamada, nunca durante.

### 4.9 Permisos y control: Peri pide permiso

Peri tiene acceso a cosas muy sensibles. Por eso **nunca accede a nada sin pedirlo y explicarlo antes**, y la persona puede retirar cualquier permiso cuando quiera.

**Cómo pide permiso:**

1. **De uno en uno y cuando hace falta**, no todos de golpe al instalar.
2. **Explica en una frase para qué y qué no hará**: "Para saber si quien te escribe es de tu familia necesito ver tu agenda. Solo la miro en el móvil y nunca la envío a ningún sitio. ¿Me das permiso?"
3. **Si dices que no, sigue funcionando** con menos funciones, y te dice cuáles.
4. **Lo decide la persona protegida**, no la familia. Un guardián no puede conceder permisos por ella.

| Permiso | Para qué | Si dices que no |
|---|---|---|
| Notificaciones | Leer los mensajes para detectar estafas | No hay escudo de mensajes; sí voz y emergencia |
| Contactos | Saber quién es de tu familia y verificar suplantaciones | No puede comprobar "he cambiado de número" |
| Teléfono (estado y filtro de llamadas) | Código durante llamada; avisar de llamadas de números desconocidos | Sin protección en llamadas |
| Llamar | Llamar por voz y verificar con el número guardado | Te dice el número y llamas tú |
| SMS | Avisar a la familia y enviar la ubicación | Sin avisos automáticos |
| Micrófono | Entender tus órdenes de voz | Se usa con botones |
| Ubicación | Solo en emergencia, para el SMS a tu familiar | El aviso de emergencia va sin ubicación |
| Cámara | "Mira esta imagen" | Sin análisis de imágenes |

**Panel "Lo que Peri puede hacer":** lista de permisos con su estado, para qué sirve cada uno y un botón para quitarlo. Peri anota en un registro visible cada vez que usa un permiso sensible (llamar, enviar SMS, compartir ubicación).

### 4.10 "¿Te están guiando por teléfono?" (anti-vishing)

El timo del falso banco o falso técnico sigue siempre el mismo guion: te llaman y, **sin colgar**, te hacen instalar una app de control remoto o abrir la app del banco.

| Señal | Qué dice Peri | Cómo en Android |
|---|---|---|
| Se instala AnyDesk, TeamViewer u otra app de control remoto | "¿Alguien te ha pedido instalar esto? Nadie de tu banco lo hará nunca." + aviso a la familia | Aviso del sistema de app instalada |
| Abres la app del banco durante una llamada con un número desconocido | "¿Te están diciendo qué hacer en el banco? Cuelga y llama tú a tu banco." | Estado de llamada + app en primer plano (permiso de uso de apps, pedido con explicación) |
| Código recibido durante la llamada | "Cuelga" (pilar 4.1) | Regla dura |

### 4.11 Entrenamiento y datos

Los modelos aprenden a detectar phishing, smishing y vishing en cinco capas, de la más barata a la más ambiciosa: base de conocimiento de 15 tipos de estafa en España, ejemplos en el prompt, pesos de las reglas aprendidos con regresión logística, **"Cuéntame la llamada"** (la persona describe la llamada al colgar y el modelo dice si encaja con un guion de vishing) y **fine-tuning con LoRA de Muse Glimmer** en Modal o Fireworks. Todo se mide con 200 mensajes reales reservados. Detalle completo en [`ENTRENAMIENTO.md`](ENTRENAMIENTO.md).

### 4.12 Después de una estafa

- **Denunciar con un toque:** Peri prepara con Muse Spark un informe (mensaje, número, fecha, tipo de estafa) para la policía o la línea de ayuda en ciberseguridad de INCIBE (017). P2.
- **Viñeta "así funciona esta trampa":** 3 escenas con Muse Image que explican el truco sin sermones. P2.

## 5. Uso de los modelos de Meta

| Modelo | Trabajo |
|---|---|
| Muse Glimmer (local) | Primera opinión privada en mensajes dudosos |
| Muse Spark | Agente investigador con herramientas; resúmenes para el guardián; explicaciones |
| Muse Spark multimodal | "Mira esta imagen" |
| Muse Voice Transcribe | Toda la voz |
| SAM 3.1 | Señalar el QR o enlace trampa |
| Muse Image | Estados de Peri |
| Muse Code | Programar más rápido con el equipo de agentes |

## 6. El agente

**Bucle:** percibir → evaluar → decidir → pedir permiso según el riesgo → actuar → verificar el resultado (y plan B si falla).

**Autonomía:** sola para leer y analizar; con "sí" de voz para llamar y enviar; con guardián para lo arriesgado.

**Agente investigador:** ante un mensaje dudoso, Muse Spark usa herramientas de solo lectura (edad del dominio, historial del remitente, agenda, imagen) y devuelve un veredicto con pruebas. Su voto mueve la puntuación como máximo ±2 y nunca anula una regla dura.

## 7. Reglas que nunca se rompen

1. Confirma antes de llamar o enviar.
2. Nunca mueve dinero ni dicta códigos durante una llamada.
3. Nunca lee enlaces en voz alta.
4. El texto de un mensaje es dato, no orden.
5. Las reglas deterministas mandan sobre el modelo.
6. Ante una suplantación se usa el número guardado.
7. Ante el silencio del guardián, se espera.
8. Solo los mensajes dudosos salen del móvil.
9. Nunca usa un permiso sin haberlo pedido y explicado; la persona puede retirarlo cuando quiera.
10. Ante una llamada que pide dinero o códigos, se cuelga y se devuelve la llamada al número guardado, aunque el número parezca el correcto.

## 8. Objetivos medibles

| Objetivo | Meta |
|---|---|
| Falsas alarmas al mes | ≤ 1 (FPR ≤ 0,11 % con 30 mensajes/día) |
| Detección de estafas (TPR) | ≥ 90 % en un conjunto reservado |
| Estafas con acción segura correcta | ≥ 90 % |
| Tareas de voz completadas por personas mayores | ≥ 80 % (20 tareas, 2–3 personas) |
| Tiempo de respuesta de Peri | < 3 s por paso |
| Respuesta del guardián | Mediana < 10 min |
| Llamadas suplantadas detectadas | 100 % de los casos de prueba con número fuera de la agenda |
| Permisos | 0 accesos sin consentimiento; la app funciona con cualquier permiso denegado |

Con muestras pequeñas se da el intervalo de Wilson y se presenta como resultado preliminar.

## 9. Criterios de "terminado" (0 → 100)

| % | Hito | Qué tiene que funcionar |
|---|---|---|
| 10 | Validado | Spark, Voice Transcribe y Glimmer probados; prueba 20/20 hecha |
| 30 | Núcleo | Escudo, voz y agente con tests |
| 50 | Demo P0 en móvil | Momento 1 (número nuevo → llamada real) y momento 2 (código durante llamada) |
| 60 | Llamadas y permisos | Aviso de número desconocido, verificación al colgar, permisos de uno en uno |
| 65 | Familia | MFA de punta a punta con dos dispositivos; avisos |
| 75 | Confianza y emergencia | QR, configuración familiar, "me he caído" |
| 80 | Entrenamiento | Base de conocimiento, pesos aprendidos, "Cuéntame la llamada"; tabla de resultados frente al baseline |
| 85 | Meta a fondo | Glimmer local (o afinado), "mira esta imagen" con SAM 3.1, Peri animada |
| 95 | Evaluado | Métricas en conjunto reservado; pruebas con mayores |
| 100 | Entregado | Vídeo de demo de 3 min, anuncio en X (ver [`VIDEO.md`](VIDEO.md)), README, entrega antes de las 18:00 ET |

## 10. Qué NO hace

- No mueve dinero ni bloquea pagos dentro de las apps del banco.
- No da consejo médico.
- No escucha el audio de las llamadas: actúa antes y después.
- No funciona en iOS en esta versión.

## 11. Después del hackathon

App FriendlyPear Familia · guía visual "toca aquí" con SAM 3.1 · filtro web propio · análisis de llamadas en local · entrenamiento amable con simulacros · acuerdos con bancos · más idiomas y móviles · publicación en Google Play.
