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
12. (29 oct, mañana) Las letras de «FriendlyPear» muy juntas: separarlas un poco y hacerlo más pequeño.
13. (29 oct, mañana) En algunos fotogramas la boca borra la mitad de lo que hay delante de la cara (el corazón de las
    manos, la lupa): cuidarlo en todos, no solo en esos.
14. (29 oct, tarde) «Me encanta cuando entro en el inicio y la pera salta»: una lista extensa de imágenes con muchos
    más fotogramas para todo (Peri, la app y el vídeo).
15. (29 oct, mediodía) El salto va muy rápido y se ve mal (arreglado: ~1,3 s, cada pose a la vista y un arco de verdad).
    Un plan para mejorar la seguridad, el pulido y la app: «Ronda 7» abajo.

## P0 · En un móvil de verdad

- [ ] (IMP-2) r5-instalar — [M] el APK se instala en el móvil de la persona: por USB con `adb install` (sin pasar por el navegador ni la descarga del chat), y si Play Protect avisa, «Instalar de todas formas» lo decide la persona; guía en inglés `INSTALL.md` para el jurado y las familias (orígenes desconocidos, Play Protect, ajustes restringidos de Android 13+ para notificaciones y accesibilidad, permisos de SMS y llamadas) — docs/, README.md · a medias 2026-10-29: `docs/INSTALL.md` (por qué el móvil lo bloquea: protección contra fraude de Play Protect, ajustes restringidos, verificación de desarrolladores; por USB con `adb`, que está exento de las tres); falta instalarlo con la persona en su móvil
- [x] (IMP-2) r5-release — [M] APK de release firmado con una clave propia guardada fuera del repo (`~/.gradle/gradle.properties`), minificado y sin el token compartido de pruebas: es el que se enseña y se entrega; el de depuración nunca se publica — android/app/build.gradle.kts · hecho 2026-10-29: `./gradlew :app:assembleRelease`: firmado con una clave propia (`~/.gradle/friendlypear-release.jks`, contraseñas en `~/.gradle/gradle.properties`, nunca en el repo), R8 y recursos encogidos (23 MB frente a 29), sin el token de pruebas (comprobado en el APK); probado en un móvil recién puesto (un quinto emulador): bienvenida, inicio, SMS de timo detectado
- [ ] (TEST) r5-movil-real — [M] una tarde con el móvil de la persona: SMS, WhatsApp y correo de verdad (sintéticos enviados desde otro móvil), una llamada de número desconocido, Mini Peri encima de otras apps, batería y fluidez con la Peri por capas; informe de fallos — eval/, docs/agentes/hallazgos/
- [x] (SEG) r5-restricciones — [M] (r2-android-restricciones) pelear con Android: ajustes restringidos, optimización de batería, rol de filtro de llamadas, superposición y códigos ocultos en Android 15; para cada uno, la app lo detecta y guía a la persona con un paso claro — android/ · hecho 2026-10-29: `device/Obstacles.kt` y la revisión del móvil: ahorro de batería (la pregunta de Android de un toque, o la página de la app si está restringida), permiso para la pausa sobre otras apps, filtro de llamadas solo donde existe y, desde Android 15, los códigos ocultos sin SMS (pide los SMS o abre la guía si están bloqueados); en el inicio, la batería como paso opcional; los ajustes restringidos ya los guiaba `UnlockActivity`
- [ ] (IMP-1) r5-servidor-publico — [M] (tunel-https) el servidor de la familia con dirección HTTPS pública para que la familia funcione en móviles de verdad fuera de casa; preparado hasta el último paso en la ronda 1 — backend/

## P0 · El chat y la detección, en serio

- [x] (IMP-1) r5-chat-libre — [L] «Pregúntale a Peri» entiende lo que escribe la gente: en el idioma en que se le escribe (aunque el móvil esté en otro), frases vagas («tengo un problema», «me han liado») con una pregunta y opciones en vez de «no sé», faltas y sin tildes, mezcla de idiomas; 200+ frases de prueba en core en los dos idiomas — core/.../agent/, android/.../ui/peri/ChatActivity.kt · a medias 2026-10-29: contesta en el idioma en que se le escribe (`Language`), «tengo un problema», «creo que me han estafado» o «ayuda» escrito a solas abren una pregunta tranquila con opciones (y «es una emergencia» entre ellas), y entiende los pasados («me llamó», «me han llamado», «me llegó»); una orden dicha primero sigue siendo orden; faltan las 200 frases de prueba · hecho 2026-10-29: 81 frases como las escribe la gente (faltas, sin tildes, «me a llamado», «q hago», pasados, inglés sin «that») en `ChatPhrasesTest`, todas bien; «my phone was stolen» ya no es una llamada a «was stolen»; con lo de esta mañana (idioma, «tengo un problema», mensajes pegados)
- [x] (IMP-1) r5-chat-escudo — [M] pegar o contar un mensaje en el chat y que lo mire el escudo de verdad (las mismas señales que con los SMS): veredicto, por qué y qué hacer, sin que salga del móvil — core/, ChatActivity.kt · hecho 2026-10-29: un mensaje pegado o contado («me ha llegado esto: …», o el mensaje con su enlace, importe o código) pasa por el mismo escudo que los SMS, en el móvil: veredicto y qué hacer en el idioma en que se escribe (`PastedMessage`)
- [x] (IMP-1) r5-vishing — [M] las llamadas en serio: lo que se detecta hoy (código durante una llamada, número nuevo que pide datos o que le devuelvas la llamada, «cuenta segura», empresa desde un móvil), lo que no (el audio: por diseño, D3) y lo que se añade: al colgar un número desconocido largo, «¿te han pedido algo?», y aviso si se abre el banco o una app de control remoto durante la llamada o justo después — core/, android/.../services/ · hecho 2026-10-29: lo que ya se hacía (aviso antes de contestar, código durante la llamada, banco o app de control remoto durante la llamada o en la media hora siguiente con la pausa, «¿quién decía ser?» y lo que pidieron comparado con los guiones) y, nuevo, la pregunta al colgar según lo que duró: menos de 15 s nada que preguntar, más de 3 min una pregunta directa («¿quién decía ser, y te ha pedido algo: un código, dinero o instalar una app?»); el audio sigue sin oírse (D3)
- [x] (IMP-2) r5-lo-que-ve — [S] una pantalla honesta «Lo que Peri puede ver y lo que no» (mensajes sí, audio de llamadas no, WhatsApp por la notificación...), en Ajustes y en el README — ui/peri/, README.md · hecho 2026-10-29: arriba en «Lo que hace Peri» (Ajustes), con un tic verde lo que ve (SMS, avisos de apps sin la conversación, quién llama, qué app está abierta en una llamada, contactos, imágenes que se le enseñan), con una cruz coral lo que no ve nunca (el sonido de las llamadas, los chats, lo que se escribe, dónde se está salvo emergencia, lo apagado) y qué sale del móvil; la misma tabla en el README
- [ ] (TEST) r5-escenarios — [L] (r2-escenarios) batería automática de 200+ situaciones en es y en (mensajes, llamadas, familia, webs, fotos, chat), con informe de fallos — eval/
- [ ] (TEST) r5-reales — [M] (reservado-200) mensajes reales de familias, anonimizados y con permiso, que nunca entrenan: el número honesto — eval/data/ (no se sube)
- [ ] (TEST) r5-metricas — [M] (metricas) acierto y falsas alarmas con intervalo de Wilson en el conjunto reservado, una fila por capa — eval/
- [ ] (TEST) r5-familia-e2e — [M] (r2-familia-e2e) la familia de punta a punta en dos emuladores: aprobación por la web y Peri a Peri, caducidad, enlace reenviado o reutilizado, número falso, aviso de caída — eval/
- [ ] (IMP-1) r5-meta-seguridad — [M] (r2-meta-seguridad) más jugo a los modelos de Meta: viñetas al vuelo con Muse Image para trampas nuevas, resumen semanal escrito por Spark (revisado por las reglas de Peri) — backend/src/

## P0 · Peri viva de verdad

- [x] (IMP-2) r5-sin-fantasma — [S] ningún fundido entre fotogramas de piernas o brazos (se veían dobles): cambios secos, como la plastilina, con un estirón; entre estados, un «boing» en vez de un fundido — ui/peri/PeriFace.kt · hecho 2026-10-29: nunca dos imágenes a la vez: cambio seco con un estirón de plastilina; un estado nuevo entra con un salto
- [x] (IMP-2) r5-ropa-cara — [M] la ropa pegada a la cara: los ojos de cada imagen medidos de verdad (en las poses con ojos cerrados se medían la boca, el brazo o la flor) y la ropa con la misma colocación que la cara, así no salta al parpadear, coger el té o la flor, ni al saludar — assets/peri/, ui/peri/Outfit.kt · hecho 2026-10-29: los ojos de cada imagen llevados desde su imagen quieta casando rasgos (SIFT y RANSAC); con la Peri por capas el cuerpo no cambia al hablar ni al parpadear
- [x] (IMP-2) r5-capas-todo — [L] (r4-peri-capas) la Peri por capas en todas las pantallas: inicio, estados, pasos, gestos, poses y bailes; los cuerpos del mismo tamaño y con los pies en el suelo, para que nada salte al cambiar de pose; la Peri de imágenes enteras solo donde falte un cuerpo — ui/peri/, assets/peri/layers.py · hecho 2026-10-29: `PeriFace` dibuja por capas (`LayeredFrame`, `PeriLayers`) 89 fotogramas de calma, escuchando, protectora, llamando y esperando, sus pasos, gestos y 17 poses, desde 49 cuerpos; lo que sostiene delante de la cara (manos en corazón, taza, carta, mango del paraguas…) va encima de ojos y boca en 26 cuerpos; pensando (la lupa), dormida, contenta y preocupada siguen con su imagen entera; todas las pantallas y Mini Peri lo usan
- [x] (IMP-2) r5-mini-viva — [M] (r4-mini-viva) Mini Peri por capas: parpadea, habla, se asoma, saluda, duerme — ui/peri/MiniPeriService.kt · a medias 2026-10-29: Mini Peri usa `PeriFace`, así que ya parpadea, habla y cambia de cara por capas; su burbuja ya no tapa la de la app durante una llamada; faltan asomarse y dormir con los dibujos del lote 5 · 29 oct: las cabezas del lote 5 no tienen cara, así que necesitan su propia colocación de la cara antes de usarlas (siguiente paso) · hecho 2026-10-29: Mini Peri es ya su cabeza (lote 5) con los ojos y la boca de Peri puestos por sus mejillas (`MiniHead`, `MiniHeadData` de `sprites_cut.py mini`): parpadea, habla, la hoja se mueve con el viento, saluda al aparecer, sostiene el paraguas al proteger y duerme con su gorro; sus momentos (risa, amor, idea…) con su efecto; asomarse queda fuera (sus mejillas apenas se ven)
- [x] (IMG) r6-imagenes — [L] las 299 imágenes y fotogramas de [`docs/IMAGENES_RONDA6.md`](../IMAGENES_RONDA6.md) (lotes 16 a 36: el salto en 9 fotogramas y la entrada en 6, más saltos y entradas, andar y correr de frente y de lado, girar en 8 vistas, emociones y estados en movimiento, caras nuevas, Mini Peri, iconos y pantallas de la app, trampas nuevas y el vídeo fotograma a fotograma); el lote 16 primero y parar — assets/peri/sprites/, assets/ui/, video/public/ · hecho 2026-10-29
- [x] (IMP-2) r6-ui-imagenes — [M] las imágenes de la interfaz en la app · hecho 2026-10-29: icono adaptable (Peri con su escudo sobre su verde, encuadrada para cualquier máscara) y pantalla de arranque (brazos en alto); Peri en cada paso que falta (llamadas, avisos, SMS, banco, ubicación, batería), en cada punto de la revisión del móvil (lote 29), en el diario vacío, sin conexión con la familia o sin nadie más, en la prueba, en «Lo que ve Peri» y en la guía para desbloquear; `sprites_cut.py ui`
- [ ] (IMP-2) r6-ui-resto — [M] lo que queda de los lotes 27 a 30: iconos de pestañas y herramientas, notificaciones con Peri, widget, acierto y fallo en la prueba, informe enviado, la familia dice sí, no o espera, despedida, novedades, permisos de contactos, micrófono y archivos, y las cinco trampas nuevas en viñetas (inversión, policía, trabajo, voz, aviso de virus) — android/, assets/peri/sprites_cut.py
- [ ] (IMP-2) r6-fotogramas — [L] montar los fotogramas de la ronda 6: el salto de 9 y la entrada de 6 en cuanto llegue el lote 16 (lo que más le gusta a la persona), las variantes al azar, los ciclos de andar y correr, las vueltas, las emociones y los estados, las caras nuevas en `layers.py` y lo nuevo de Mini Peri — ui/peri/PeriFace.kt, MiniHead.kt, assets/peri/ · en curso 2026-10-29: el salto de 9 y la entrada de 6 montados (`PeriFace`: cada fotograma a su altura de `anim_offsets.json`, la ropa por `anchors.py`); después el salto de alegría (contenta o al ganar un escudo), saltos al azar (estrella, con vuelta), entradas al azar (del cielo, de la tierra, con el paraguas) y la vuelta en 8 vistas; quedan andar y correr, emociones y estados, caras nuevas y Mini Peri
- [x] (IMP-2) r5-efectos — [S] los efectos del lote 6 (notas al tararear, gota de sudor, interrogación, bombilla, confeti, copos) en `Sparkles` — ui/peri/Sparkles.kt · hecho 2026-10-29: `Sparkles` dibuja los efectos de plastilina del lote 6 (`assets/peri/sprites_cut.py`): notas al tararear, gota de sudor al preocuparse, interrogación al pensar o investigar, exclamación al avisar o sorprenderse, bombilla en una idea, confeti en fiesta, medalla y aplauso, lágrima, corazones de plastilina y polvo al aterrizar; los blancos (polvo, copo, destello) no se distinguen del crema y se dibujan en código
- [x] (IMP-2) r5-movimientos — [M] los movimientos del lote 4 (saltar, bailar, señalar, reverencia, correr, sentarse, abrazo) en las acciones de Peri — ui/peri/PeriFace.kt · hecho 2026-10-29: en calma o contenta, sus movimientos de verdad: salto (agacharse, en el aire, aterrizar), baile de cuatro pasos y cadera, una vuelta entera (de lado, de espaldas, del otro lado, sin la ropa mientras), estirarse con los brazos arriba y una reverencia; cada uno se decodifica antes en segundo plano y se suelta al acabar; la ropa va con su cabeza (sus ojos medidos en `anchors.py`)
- [x] (IMP-1) r5-hitos — [S] (r2-hitos) rachas de días tranquilos e hitos de escudos (el primero, cinco) con su pose y su frase — core/, ui/peri/ · hecho 2026-10-29: escudos (el primero, cinco, veinte) y días tranquilos seguidos (tres, una semana, un mes; un susto en el diario los empieza otra vez) en `core/agent/Milestone.kt` con sus pruebas; cada uno con su dibujo de Peri (lote 28: escudo en alto, torre, trofeo; brote, flores, peral), su medalla (lote 14), su frase en los dos idiomas y su momento de medalla; llega tras el saludo y nunca encima de un aviso, y no cuenta como celebrado hasta que se ve; las medallas y la racha en la tarjeta de los escudos

## P1 · Una UI que se recuerde

- [x] (IMP-2) r5-logo — [S] el logotipo sin letras encima de otras: primero todos los bordes, luego las letras, y la luz sin copia desplazada — ui/peri/Wordmark.kt · hecho 2026-10-29: primero los bordes y luego las letras (anoche), y la causa de verdad del «texto encima de otro»: medir el nombre dejaba las letras a 100 px tras cualquier otra pasada de medida, más grandes que su borde; se mide con su propia pintura; un poco de aire entre letras y un 82 % del tamaño de un título; sin recuadro gris al enfocarlo
- [x] (IMP-2) r5-chat-imagen — [S] Peri saludando con bocadillos (`scene_chat`) en la cabecera del chat en vez del paraguas — ui/peri/ChatActivity.kt · hecho 2026-10-29: la Peri por capas en la cabecera (no la ilustración), que mueve la boca con cada respuesta
- [x] (IMP-2) r5-herramientas — [M] las herramientas de Peri a la vista: una fila en el inicio con «Mira una imagen», «Revisa tu móvil», «Juego de timos» y «Pregúntale», con sus ilustraciones de plastilina — ui/peri/PeriActivity.kt, Pages.kt · hecho 2026-10-29: una fila de cinco losetas de plastilina en el inicio con la pose de Peri de cada una: Mira una imagen, Revisa tu móvil, ¿Timo o no?, Las trampas y Mini Peri
- [x] (IMP-2) r5-diario-corto — [M] el diario corto: lo de hoy arriba, los días anteriores plegados por día con su resumen y «ver más» — ui/peri/Pages.kt · hecho 2026-10-29: hoy entero (los cinco primeros y «Ver N más»), los días anteriores plegados en una línea con sus insignias que se abre al tocarla, y solo la última semana
- [x] (IMP-2) r5-ilustraciones — [M] las 15 ilustraciones del lote 7 en sus apartados (diario, aprende, familia, ajustes, foto, revisión, trofeo, mañana y noche) — ui/peri/, res/drawable-nodpi/ · hecho 2026-10-29: diario (escribiendo; por la mañana con su té y de noche leyendo con su lámpara; vacío, en la hamaca), aprende (birrete), «¿Timo o no?» (pulgar arriba), familia (abrazando un corazón), ajustes (llave inglesa), revisión del móvil (doctora, o celebrando si todo está bien) y trofeo al ganar una ronda; la pompa de jabón no se recorta bien y queda fuera
- [ ] (IMP-2) r5-barra-volver — [S] (r2-barra-volver) barra superior con volver en las pantallas secundarias y la bienvenida sobre el mundo de Peri — ui/peri/
- [ ] (IMP-2) r5-taste — [S] (r2-taste) una pasada con la skill design-taste-frontend por todas las pantallas, con D8 por delante — ui/peri/
- [x] (IMP-2) r5-accesible — [S] TalkBack, letra al 200 % y contraste en las pantallas nuevas (chat, prueba de capas, herramientas) — ui/peri/ · hecho 2026-10-29: losetas de herramientas y botones de la prueba que crecen con la letra (al 200 % nada se corta), los títulos son encabezados para TalkBack en toda la app, la cabeza de Mini Peri dice su estado; el diario y el chat probados al 200 %

## P1 · Seguridad

- [ ] (SEG) r5-revision — [M] (r2-revision y r2s-revision-nuevo) revisión de lo nuevo: `/v1/report`, avisos de webs, registros en el móvil, Peri a Peri, preguntas y colgar, notificaciones con acciones, bloqueados, el chat con el escudo — backend/src/, android/
- [ ] (SEG) r5-https-pin — [M] (r2s-https-pin) servidor solo por HTTPS, HSTS y certificado fijado en la app con fecha de caducidad — android/app/src/main/res/xml/, backend/
- [ ] (SEG) r5-integridad — [L] (r2s-integridad, después) comprobar que la app que habla con el servidor es la nuestra (Play Integrity) y Safe Browsing para los enlaces — necesita Google Play

## P1 · Vídeo y entrega

- [x] (IMG) r5-imagenes — [L] las imágenes de [`docs/IMAGENES_VIDEO_UI.md`](../IMAGENES_VIDEO_UI.md) (lotes 8 a 15: familia, lugares, timos sin miedo, efectos, rótulos, Peri para la historia, UI e icono) con Muse Image, lote a lote — video/public/, assets/ui/ · hecho 2026-10-29
- [ ] (DIR) r5-guion — [S] (r2-guion) el guion juntos, con las cuatro decisiones de `docs/HISTORIA.md` (formato, Carmen, voz, idioma) — docs/HISTORIA.md, docs/VIDEO.md
- [ ] (DIR) r5-historia — [M] (r3-historia) la historia de Peri en Remotion con el arte nuevo, sin lag — video/
- [ ] (DIR) r5-grabaciones — [M] (r2-grabaciones) regrabar los clips de la app con la Peri por capas y la UI nueva — video/public/clips/
- [ ] (DIR) r5-video — [M] (video-3-min) montaje de la demo de 3 min — video/
- [ ] (DIR) r5-anuncio — [M] (anuncio) anuncio de 30–45 s, vertical y cuadrado, es/en — video/
- [ ] (DIR) r5-post — [S] (post-x) post con el anuncio para Most Viral — docs/demo/
- [x] (PUL) r5-readme — [S] (readme-resultados) resultados, límites, cómo instalar, qué se hizo en el hackathon, capturas y GIF nuevos — README.md · hecho 2026-10-29: icono, banner y cuatro pantallas nuevas, un GIF de cómo se abre, «Try it» (USB y por qué, «Check my phone»), «Limits» y «Built during the hackathon»; INSTALL con los obstáculos de Android
- [ ] (DIR) r5-entrega — [S] (entrega) entregar antes de las 18:00 ET: repo público, vídeo y post accesibles — /

## Ronda 7 · Seguridad, pulido y la app (plan del 29 oct)

Lo que falta para que FriendlyPear sea de fiar en el móvil de alguien de verdad y se vea terminado. En cada bloque, lo
primero es lo que más protege o lo que más se nota. Ya estaban y suben de prioridad: r5-movil-real, r5-instalar,
r5-servidor-publico, r5-https-pin y r5-revision.

### Seguridad

- [ ] (SEG) r7-pantalla-segura — [S] `FLAG_SECURE` en las pantallas con datos que un timador querría ver: el código de emparejar, la petición y la aprobación de la familia, el informe para la policía, «Lo que Peri puede hacer» con su registro y la configuración de la familia. Una app de control remoto (lo que hacen instalar los timadores) no puede verlas ni grabarlas; en Android 14 y después, Peri explica por qué si alguien intenta una captura — ui/peri/
- [ ] (SEG) r7-toques-tapados — [S] que nadie toque por la persona: los botones que deciden (el sí de la familia, permitir, emparejar, bloquear, «ya lo había contestado») ignoran los toques si otra app pinta encima (`filterTouchesWhenObscured`), y Peri avisa de que hay algo tapando la pantalla — ui/peri/
- [ ] (SEG) r7-verificacion-dev — [S] registrar el paquete y la clave de release en la verificación de desarrolladores de Android (obligatoria en Brasil, Indonesia, Singapur y Tailandia desde septiembre de 2026, después en todas partes): sin eso, el APK dejará de instalarse en los móviles certificados — docs/INSTALL.md (con la persona: su cuenta de Google)
- [ ] (SEG) r7-componentes — [S] los 7 componentes exportados, uno a uno: cada uno con su permiso o su comprobación (la foto compartida, el enlace de emparejar, que ya rechaza servidores ajenos), y una prueba con intents maliciosos por `adb shell am start` — AndroidManifest.xml, ui/peri/
- [ ] (SEG) r7-release-limpio — [S] el APK de release sin registros (R8 quita `Log`), el informe copiado al portapapeles marcado como sensible (Android 13+ no lo enseña) y `eval/release_check.py` comprobando las dos cosas — android/app/proguard-rules.pro, ReportActivity.kt, eval/
- [ ] (SEG) r7-copia-clave — [S] la clave de datos del servidor guardada aparte de los datos, con un ensayo de restaurar desde cero (en «Still open» de SECURITY.md) — backend/, docs/SECURITY.md
- [ ] (TEST) r7-red-team-semanal — [M] el equipo rojo de cada semana, solo: un lote de Spark contributor, su informe y los timos que se escapan a una lista para leer a mano (nunca entrenan solos) — eval/redteam/
- [ ] (SEG) r7-dependencias — [S] dependencias fijadas y auditadas (Gradle y Bun), sin nada nuevo sin leerlo — android/, backend/
- [ ] (IMP-2) r7-borrar-todo — [S] «Borrar lo que Peri sabe» en Ajustes, con el bloqueo del móvil: diario, último timo, números bloqueados, familia y claves, y desemparejar el móvil del servidor — ui/peri/SettingsActivity.kt, config/

### Pulido

- [ ] (IMP-2) r7-entrada-al-momento — [S] Peri entra sin esperar: sus fotogramas de entrada se decodifican mientras dura la pantalla de arranque, no después (hoy tarda ~0,6 s en el emulador) — ui/peri/PeriFace.kt
- [ ] (IMP-2) r7-rendimiento — [M] fluidez y memoria en un móvil modesto (perfil de 2 GB de RAM): fotogramas por segundo y memoria con todos los movimientos nuevos; los de movimiento a 768 px si hace falta, y un APK más ligero (hoy 24 MB) — ui/peri/, assets/peri/sprites_cut.py
- [ ] (IMP-2) r7-accesible-2 — [M] TalkBack de punta a punta (bienvenida, avisos, familia, revisión del móvil, medallas), contraste de los textos sobre los dibujos nuevos y «quitar animaciones» en todo lo nuevo — ui/peri/
- [ ] (IMP-2) r7-textos — [S] todos los textos nuevos en los dos idiomas leídos en voz alta con la skill de Peri (medallas, revisión, obstáculos, celebraciones) — res/values*/
- [ ] (IMP-2) r7-ajustes — [S] Ajustes en tres bloques claros (Peri, protección, familia), con la revisión del móvil y «Lo que Peri puede hacer» a mano — ui/peri/SettingsActivity.kt
- Ya en el roadmap: r6-fotogramas (andar y correr, emociones, estados, caras, Mini Peri), r6-ui-resto, r5-barra-volver y r5-taste.

### La app

- [ ] (IMP-2) r7-notificaciones — [S] las notificaciones con la cara de Peri (alerta, familia, contenta; lote 27), para que se reconozcan entre las demás — services/, res/
- [ ] (IMP-2) r7-widget — [M] el widget de Peri en la pantalla de inicio (pequeño y ancho, lote 27): cómo está y un toque para hablarle — ui/, res/xml/
- [ ] (IMP-2) r7-trampas-nuevas — [M] las cinco trampas nuevas en viñetas (inversión, policía, trabajo, voz, aviso de virus; lote 30) en «Aprende» y en «¿Timo o no?», con sus preguntas — ui/peri/TrapActivity.kt, QuizActivity.kt
- Ya en el roadmap: r5-movil-real (lo primero: una tarde con el móvil de la persona), r5-instalar, r5-familia-e2e, r5-escenarios y r5-meta-seguridad.

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
