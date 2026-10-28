# ROADMAP — FriendlyPear

Único escritor: DIR (o quien trabaje a petición de la persona). Cada rol marca `[x]` sus tareas al terminar y propone ideas en su estado.
Formato: `- [ ] (ID) slug — [talla] descripción — rutas`. Tallas: `[S]` corta · `[M]` con tests · `[L]` partir en piezas.
Entrega: **4 nov 2026, 21:00 ET**. Objetivo propio: 18:00 ET.

**Empezado de cero el 28 oct por la noche (decisión de la persona).** Lo hecho en las rondas 1 a 4 está en
[`ROADMAP_RONDAS_1-4.md`](ROADMAP_RONDAS_1-4.md); aquí va todo lo que falta, junto y por orden. Lo que se trae de antes
lleva su nombre viejo entre paréntesis.

## Lo que pide la persona (28 oct, noche)

1. La app no se pudo ni descargar en su móvil: la seguridad lo bloqueó. Tiene que instalarse y funcionar en un móvil de verdad.
2. «¿De verdad detecta vishing y phishing, o tiene que ser el texto exacto?»: el chat parece fijo («Tengo un problema» no
   lo entiende). Tomárselo más en serio: que funcione con lo que escribe la gente de verdad.
3. Efecto fantasma al cambiar de fotograma (las piernas al andar); que no pase en ninguna animación.
4. Al coger algo (el té, la flor, el móvil) se mueve todo el disfraz.
5. Sí a la Peri por capas en el inicio y en todo.
6. El logotipo parece un texto encima de otro.
7. Las herramientas de Peri están escondidas: más protagonismo.
8. El diario se hace larguísimo cuando hay mucho.
9. La imagen del chat (el paraguas) no tiene naturalidad.
10. Una lista de todas las imágenes para el vídeo (efectos, fondos, rótulos) y para la UI, como la de Peri.
11. Seguir el roadmap.

## P0 · En un móvil de verdad

- [ ] (IMP-2) r5-instalar — [M] el APK se instala en el móvil de la persona: por USB con `adb install` (sin pasar por el navegador ni la descarga del chat), y si Play Protect avisa, «Instalar de todas formas» lo decide la persona; guía en inglés `INSTALL.md` para el jurado y las familias (orígenes desconocidos, Play Protect, ajustes restringidos de Android 13+ para notificaciones y accesibilidad, permisos de SMS y llamadas) — docs/, README.md
- [ ] (IMP-2) r5-release — [M] APK de release firmado con una clave propia guardada fuera del repo (`~/.gradle/gradle.properties`), minificado y sin el token compartido de pruebas: es el que se enseña y se entrega; el de depuración nunca se publica — android/app/build.gradle.kts
- [ ] (TEST) r5-movil-real — [M] una tarde con el móvil de la persona: SMS, WhatsApp y correo de verdad (sintéticos enviados desde otro móvil), una llamada de número desconocido, Mini Peri encima de otras apps, batería y fluidez con la Peri por capas; informe de fallos — eval/, docs/agentes/hallazgos/
- [ ] (SEG) r5-restricciones — [M] (r2-android-restricciones) pelear con Android: ajustes restringidos, optimización de batería, rol de filtro de llamadas, superposición y códigos ocultos en Android 15; para cada uno, la app lo detecta y guía a la persona con un paso claro — android/
- [ ] (IMP-1) r5-servidor-publico — [M] (tunel-https) el servidor de la familia con dirección HTTPS pública para que la familia funcione en móviles de verdad fuera de casa; preparado hasta el último paso en la ronda 1 — backend/

## P0 · El chat y la detección, en serio

- [ ] (IMP-1) r5-chat-libre — [L] «Pregúntale a Peri» entiende lo que escribe la gente: en el idioma en que se le escribe (aunque el móvil esté en otro), frases vagas («tengo un problema», «me han liado») con una pregunta y opciones en vez de «no sé», faltas y sin tildes, mezcla de idiomas; 200+ frases de prueba en core en los dos idiomas — core/.../agent/, android/.../ui/peri/ChatActivity.kt
- [ ] (IMP-1) r5-chat-escudo — [M] pegar o contar un mensaje en el chat y que lo mire el escudo de verdad (las mismas señales que con los SMS): veredicto, por qué y qué hacer, sin que salga del móvil — core/, ChatActivity.kt
- [ ] (IMP-1) r5-vishing — [M] las llamadas en serio: lo que se detecta hoy (código durante una llamada, número nuevo que pide datos o que le devuelvas la llamada, «cuenta segura», empresa desde un móvil), lo que no (el audio: por diseño, D3) y lo que se añade: al colgar un número desconocido largo, «¿te han pedido algo?», y aviso si se abre el banco o una app de control remoto durante la llamada o justo después — core/, android/.../services/
- [ ] (IMP-2) r5-lo-que-ve — [S] una pantalla honesta «Lo que Peri puede ver y lo que no» (mensajes sí, audio de llamadas no, WhatsApp por la notificación...), en Ajustes y en el README — ui/peri/, README.md
- [ ] (TEST) r5-escenarios — [L] (r2-escenarios) batería automática de 200+ situaciones en es y en (mensajes, llamadas, familia, webs, fotos, chat), con informe de fallos — eval/
- [ ] (TEST) r5-reales — [M] (reservado-200) mensajes reales de familias, anonimizados y con permiso, que nunca entrenan: el número honesto — eval/data/ (no se sube)
- [ ] (TEST) r5-metricas — [M] (metricas) acierto y falsas alarmas con intervalo de Wilson en el conjunto reservado, una fila por capa — eval/
- [ ] (TEST) r5-familia-e2e — [M] (r2-familia-e2e) la familia de punta a punta en dos emuladores: aprobación por la web y Peri a Peri, caducidad, enlace reenviado o reutilizado, número falso, aviso de caída — eval/
- [ ] (IMP-1) r5-meta-seguridad — [M] (r2-meta-seguridad) más jugo a los modelos de Meta: viñetas al vuelo con Muse Image para trampas nuevas, resumen semanal escrito por Spark (revisado por las reglas de Peri) — backend/src/

## P0 · Peri viva de verdad

- [ ] (IMP-2) r5-sin-fantasma — [S] ningún fundido entre fotogramas de piernas o brazos (se veían dobles): cambios secos, como la plastilina, con un estirón; entre estados, un «boing» en vez de un fundido — ui/peri/PeriFace.kt
- [ ] (IMP-2) r5-ropa-cara — [M] la ropa pegada a la cara: los ojos de cada imagen medidos de verdad (en las poses con ojos cerrados se medían la boca, el brazo o la flor) y la ropa con la misma colocación que la cara, así no salta al parpadear, coger el té o la flor, ni al saludar — assets/peri/, ui/peri/Outfit.kt
- [ ] (IMP-2) r5-capas-todo — [L] (r4-peri-capas) la Peri por capas en todas las pantallas: inicio, estados, pasos, gestos, poses y bailes; los cuerpos del mismo tamaño y con los pies en el suelo, para que nada salte al cambiar de pose; la Peri de imágenes enteras solo donde falte un cuerpo — ui/peri/, assets/peri/layers.py
- [ ] (IMP-2) r5-mini-viva — [M] (r4-mini-viva) Mini Peri por capas: parpadea, habla, se asoma, saluda, duerme — ui/peri/MiniPeriService.kt
- [ ] (IMP-2) r5-efectos — [S] los efectos del lote 6 (notas al tararear, gota de sudor, interrogación, bombilla, confeti, copos) en `Sparkles` — ui/peri/Sparkles.kt
- [ ] (IMP-2) r5-movimientos — [M] los movimientos del lote 4 (saltar, bailar, señalar, reverencia, correr, sentarse, abrazo) en las acciones de Peri — ui/peri/PeriFace.kt
- [ ] (IMP-1) r5-hitos — [S] (r2-hitos) rachas de días tranquilos e hitos de escudos (el primero, cinco) con su pose y su frase — core/, ui/peri/

## P1 · Una UI que se recuerde

- [ ] (IMP-2) r5-logo — [S] el logotipo sin letras encima de otras: primero todos los bordes, luego las letras, y la luz sin copia desplazada — ui/peri/Wordmark.kt
- [ ] (IMP-2) r5-chat-imagen — [S] Peri saludando con bocadillos (`scene_chat`) en la cabecera del chat en vez del paraguas — ui/peri/ChatActivity.kt
- [ ] (IMP-2) r5-herramientas — [M] las herramientas de Peri a la vista: una fila en el inicio con «Mira una imagen», «Revisa tu móvil», «Juego de timos» y «Pregúntale», con sus ilustraciones de plastilina — ui/peri/PeriActivity.kt, Pages.kt
- [ ] (IMP-2) r5-diario-corto — [M] el diario corto: lo de hoy arriba, los días anteriores plegados por día con su resumen y «ver más» — ui/peri/Pages.kt
- [ ] (IMP-2) r5-ilustraciones — [M] las 15 ilustraciones del lote 7 en sus apartados (diario, aprende, familia, ajustes, foto, revisión, trofeo, mañana y noche) — ui/peri/, res/drawable-nodpi/
- [ ] (IMP-2) r5-barra-volver — [S] (r2-barra-volver) barra superior con volver en las pantallas secundarias y la bienvenida sobre el mundo de Peri — ui/peri/
- [ ] (IMP-2) r5-taste — [S] (r2-taste) una pasada con la skill design-taste-frontend por todas las pantallas, con D8 por delante — ui/peri/
- [ ] (IMP-2) r5-accesible — [S] TalkBack, letra al 200 % y contraste en las pantallas nuevas (chat, prueba de capas, herramientas) — ui/peri/

## P1 · Seguridad

- [ ] (SEG) r5-revision — [M] (r2-revision y r2s-revision-nuevo) revisión de lo nuevo: `/v1/report`, avisos de webs, registros en el móvil, Peri a Peri, preguntas y colgar, notificaciones con acciones, bloqueados, el chat con el escudo — backend/src/, android/
- [ ] (SEG) r5-https-pin — [M] (r2s-https-pin) servidor solo por HTTPS, HSTS y certificado fijado en la app con fecha de caducidad — android/app/src/main/res/xml/, backend/
- [ ] (SEG) r5-integridad — [L] (r2s-integridad, después) comprobar que la app que habla con el servidor es la nuestra (Play Integrity) y Safe Browsing para los enlaces — necesita Google Play

## P1 · Vídeo y entrega

- [ ] (IMG) r5-imagenes — [L] las imágenes de [`docs/IMAGENES_VIDEO_UI.md`](../IMAGENES_VIDEO_UI.md) (lotes 8 a 15: familia, lugares, timos sin miedo, efectos, rótulos, Peri para la historia, UI e icono) con Muse Image, lote a lote — video/public/, assets/ui/
- [ ] (DIR) r5-guion — [S] (r2-guion) el guion juntos, con las cuatro decisiones de `docs/HISTORIA.md` (formato, Carmen, voz, idioma) — docs/HISTORIA.md, docs/VIDEO.md
- [ ] (DIR) r5-historia — [M] (r3-historia) la historia de Peri en Remotion con el arte nuevo, sin lag — video/
- [ ] (DIR) r5-grabaciones — [M] (r2-grabaciones) regrabar los clips de la app con la Peri por capas y la UI nueva — video/public/clips/
- [ ] (DIR) r5-video — [M] (video-3-min) montaje de la demo de 3 min — video/
- [ ] (DIR) r5-anuncio — [M] (anuncio) anuncio de 30–45 s, vertical y cuadrado, es/en — video/
- [ ] (DIR) r5-post — [S] (post-x) post con el anuncio para Most Viral — docs/demo/
- [ ] (PUL) r5-readme — [S] (readme-resultados) resultados, límites, cómo instalar, qué se hizo en el hackathon, capturas y GIF nuevos — README.md
- [ ] (DIR) r5-entrega — [S] (entrega) entregar antes de las 18:00 ET: repo público, vídeo y post accesibles — /

## Con la persona

- (pruebas-mayores) 20 tareas con 2-3 personas mayores, con permiso.
- (voice-transcribe) 10 órdenes en español dichas por personas mayores frente al reconocedor de Android.
- (bases-y-dudas) confirmar en #questions que el diseño previo está permitido.
- (peri-referencia-video) imagen de referencia de Peri para el vídeo (D11: sin Higgsfield).

## Aparcado o fuera de alcance

- (iphone) app de iPhone: necesita un Mac con Xcode y cuenta de Apple Developer.
- (glimmer-servido, finetune-glimmer-1 y -2) aparcadas el 27 oct: la segunda opinión va con Muse Spark Open; el porqué en `training/FINETUNE.md`.
- (r2-charla) charla libre con Muse Spark: choca con la promesa del chat («lo que escribes no sale del móvil»); el chat entiende más en el propio móvil (r5-chat-libre).

## Ideas

- Entrenamiento amable: simulacros seguros personalizados con Muse Spark + Muse Image.
- Guía visual con SAM 3.1: «toca aquí» en pantalla y en aparatos.
- App FriendlyPear Familia con push.
- Filtro web propio con VPN local.
- Sonidos suaves de plastilina (un «boing» al saltar, un «pop» al aparecer), siempre apagables.
- Peri que cambia de ropa sola según el tiempo de verdad (bufanda con frío, gafas de sol en verano).

## Riesgos y decisiones vigentes

- R1: modelo flojo en español → escudo más determinista (criterio en prueba-20-20).
- R2: Android bloquea llamadas desde segundo plano → servicio en primer plano.
- R3: Google Play restringe SMS y llamadas → APK instalado a mano para el hackathon.
- R4 (27 oct): sin servidor público, en un móvil real solo funciona lo local (escudo, llamadas, Mini Peri, Peri, chat); la segunda opinión, el investigador, «Mira esta imagen», la voz de Meta y la familia necesitan el servidor (r5-servidor-publico).
- R5 (28 oct, noche): la descarga del APK desde el chat o el navegador la bloquea la seguridad del móvil, y Play Protect avisa o bloquea las apps instaladas a mano que piden SMS, notificaciones o accesibilidad → instalar por USB (`adb install`) y guía clara (r5-instalar); el jurado verá sobre todo el vídeo.
- D1: las reglas deciden; el modelo ajusta como máximo ±2 y nunca anula una regla dura.
- D2: ningún permiso se usa sin pedirlo y explicarlo; lo concede la persona protegida, no la familia.
- D3: Peri no escucha el audio de las llamadas; actúa antes (filtro) y después (verificación).
- D4: público = cualquier persona y familia; personas mayores como ejemplo principal de la demo.
- D5: nunca se evalúa con datos de entrenamiento ni sintéticos; solo con los reservados (reales cuando los haya).
- D6: en los vídeos, todo lo que es producto se graba de verdad; lo generado con IA solo para escenas de historia y rotulado.
- D7 (27 oct): el jurado habla inglés. La app es bilingüe inglés/español; todo lo que ve el jurado va en inglés. Reglas en `PROTOCOLO.md` §9.
- D8 (27 oct): la app tiene que verse viva y llamativa, y Peri, graciosa y con mucha vida; manda sobre las reglas planas de diseño, sin perder letra grande, contraste y simpleza.
- D9 (27 oct): prioridades: simpleza, cómo se vende y seguridad. Público: todos.
- D10 (27 oct): la API de Meta no tiene modelo de vídeo; `muse-wonder-1.0` solo para trabajo puntual revisado (con `reasoning_effort` bajo).
- D11 (28 oct): sin Higgsfield; el mundo de Peri cambia de cielo con la hora y el estado, y de lugar con la estación; esos fondos sirven en el vídeo.
- D12 (28 oct, noche, decisión de la persona): Peri por capas en toda la app (cuerpo sin cara, ojos y boca encima, ropa pegada a la cara).
- D13 (28 oct, noche): el chat contesta en el idioma en que se le escribe y sigue sin sacar nada del móvil; entiende más con reglas y el escudo en el propio móvil, no con un modelo en la nube.
