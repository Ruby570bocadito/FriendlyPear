# ROADMAP — FriendlyPear

Único escritor: DIR. Cada rol marca `[x]` sus tareas al terminar y propone ideas en su estado.
Formato: `- [ ] (ID) slug — [talla] descripción — rutas`. Tallas: `[S]` corta · `[M]` con tests · `[L]` partir en piezas.
Entrega: **4 nov 2026, 21:00 ET**. Objetivo propio: 18:00 ET.

## Base
- [x] (PUL) estructura-base — [S] carpetas, builds de core/android/backend, CI y documentación — /

## Ronda 2 · Peri viva (28 oct – 3 nov; decisión de la persona, 27 oct)

La app funciona, pero se ve estática y de plantilla, y Peri tiene poca vida. Orden de la persona: primero la UI completa (profesional y viva), luego Peri (frases, poses, interacciones), luego detecciones y seguridad, probando muchas más situaciones (sobre todo la familia), y al final el guion del vídeo, juntos. **Lo más importante:** simpleza, cómo se vende y seguridad, porque mucha gente puede llegar a depender de esto. **Público:** todos; las personas mayores siguen siendo el ejemplo principal. Personalidad de Peri: [`docs/PERI_PERSONAJE.md`](../PERI_PERSONAJE.md).

### A · Sistema visual vivo
- [x] (IMP-2) r2-direccion-visual — [S] elegir con la persona la dirección visual (bocetos) y fijarla en `docs/DISENO.md`; sustituye a las reglas planas del 27 oct — docs/DISENO.md · hecho 2026-10-28 (el mundo de Peri: fondos que van cambiando)
- [x] (IMP-2) r2-tipografia — [S] fuentes propias: títulos redondeados y simpáticos y lectura muy legible, con licencia libre y dentro de la app — android/app/src/main/res/font/, ui/peri/Style.kt · hecho 2026-10-28 (Fredoka y Atkinson Hyperlegible, OFL)
- [x] (IMP-2) r2-mundo-peri — [M] fondo vivo que cambia con el estado de Peri y la hora (cielo, nubes, sol, estrellas, el rojo suave de un aviso), quieto si se quitan las animaciones — ui/peri/ · hecho 2026-10-28 (`PeriWorld`: 4 cielos, 5 lugares por estación, elegible en Ajustes)
- [x] (IMP-2) r2-componentes — [M] paleta viva y componentes con movimiento (botones con rebote, tarjetas, chips, transiciones entre pantallas) — ui/peri/Style.kt · hecho 2026-10-28 (botones y tarjetas de goma)
- [x] (IMP-2) r2-navegacion — [M] barra inferior con apartados: Peri, Diario, Aprende, Familia (y Ajustes) — ui/peri/ · hecho 2026-10-28
- [x] (IMP-2) r2-diario — [M] "Diario de Peri": avisos, escudos, denuncias y lo que hizo, en una línea de tiempo — ui/peri/, config/ · hecho 2026-10-28 (`Diary` en el móvil, sin textos ni remitentes)
- [x] (IMP-2) r2-aprende — [M] "Aprende con Peri": la biblioteca de trampas (viñetas) y el juego "¿Timo o no?" — ui/peri/, backend/src/ · hecho 2026-10-28 (16 mensajes por idioma, en la app)
- [x] (IMP-2) r2-pantallas — [M] todas las pantallas con el sistema nuevo — ui/peri/ · hecho 2026-10-28 (heredan fuentes, goma y el cielo claro de la hora)
- [ ] (IMP-2) r2-barra-volver — [S] barra superior con botón de volver en las pantallas secundarias y la bienvenida sobre el mundo de Peri — ui/peri/

### B · Peri con personalidad
- [x] (IMP-2) r2-poses — [M] 10 poses nuevas con Muse Image (sorpresa, medalla, capa de superheroína, detective, aplauso, confeti, gafas, bufanda, cucú, despedida) — assets/peri/ · hecho 2026-10-28 (0,10 $)
- [x] (IMP-2) r2-interacciones — [M] cosquillas al mantener pulsado, arrastrarla, agitar el móvil, rascar la hoja — ui/peri/PeriFace.kt, PeriActivity.kt · hecho 2026-10-28
- [x] (IMP-1) r2-frases — [M] bancos de frases mucho más grandes y según el momento (rachas, hitos, volver tras días sin abrir, primera vez en cada pantalla); propuestas con **Muse Wonder** y revisadas una a una — res/values*/, core/.../agent/Phrases.kt · hecho 2026-10-28 (18 bancos; 0,36 $)
- [ ] (IMP-1) r2-hitos — [S] rachas de días tranquilos e hitos de escudos (el primero, cinco) con su pose y su frase — ui/peri/, res/values*/
- [ ] (IMP-1) r2-charla — [M] "Pregúntale a Peri": dudas y charla con Muse Spark, con guardarraíles (nunca lee enlaces ni códigos; lo que dice el modelo pasa el filtro de Peri) — backend/src/, android/

### C · Seguridad y pruebas
- [ ] (TEST) r2-escenarios — [L] batería automática de 150+ situaciones en es y en (mensajes, llamadas, familia, webs, fotos), con informe de fallos — eval/
- [ ] (TEST) r2-familia-e2e — [M] la familia de punta a punta en dos emuladores: aprobación por la web y Peri a Peri, caducidad, enlace reenviado o reutilizado, número falso, aviso de caída — eval/, docs/agentes/hallazgos/
- [ ] (SEG) r2-revision — [M] revisión de seguridad de lo nuevo (`/v1/report`, avisos de webs, registros en el móvil, Peri a Peri) — backend/src/, android/
- [ ] (IMP-1) r2-detecciones — [M] arreglar lo que salga de las pruebas; reglas nuevas (letras que imitan a otras, acortadores, QR) — core/.../shield/

### D · Vídeo y anuncio (con la persona)
- [ ] (DIR) r2-guion — [S] el guion juntos: arranque de anuncio que vende, luego ejemplos de uso, para quién es (todos), la personalidad de Peri, los modelos de Meta y resultados — docs/VIDEO.md
- [ ] (DIR) r2-grabaciones — [M] regrabar los clips con la UI nueva y con la voz de Peri — docs/demo/
- Se llevan: video-3-min, anuncio y post-x (Día 8). **No hay modelo de vídeo de Meta en la API** (`muse-wonder-1.0` es texto) y **Higgsfield queda descartado** (D11): el vídeo sale de los clips reales, de los fondos del mundo de Peri y de animar fotogramas de Muse Image.

### E · Ideas de la persona (28 oct, escritas antes de ver la ronda 2)
Ya hechas: el fondo que cambia (mundo de Peri), fuentes y animaciones, más personalidad (poses, frases, gestos). El vídeo de anuncio con la herramienta en uso y un ejemplo real con un modelo de Meta va en `r2-guion`; las pruebas reales y la familia con varios móviles, en `r2-familia-e2e` y `pruebas-mayores`.
- [x] (IMP-2) r2-atuendos — [M] vestir a tu Peri: accesorios de cabeza, ojos y cuello hechos con Muse Image y colocados en cada pose (también en Mini Peri), con un armario para elegirlos — assets/peri/, ui/peri/ · hecho 2026-10-28 (11 accesorios, 0,13 $); pulido por la noche: gafas nuevas sin patillas, cada ojo medido entero (todo iba a la izquierda), cada prenda centrada en su eje y sin hoja bajo los sombreros (48 cabezas de Muse Image, 0,48 $); por la tarde: siguen cada fotograma (plantilla de la cabeza en `anchors.py`), gafas sin patillas que la atraviesen, nada sobre lo que sujeta en el pecho y una caché de imágenes al tamaño justo
- [x] (IMP-2) r2-peras-familia — [L] las peras de tu familia como amigos en el apartado Familia, con su atuendo y su papel (te cuida, la cuidas, familia), por el servidor de la familia; avisos dentro de la app además del SMS, y la pera afectada se ve en peligro o en atención — backend/src/, ui/peri/ · hecho 2026-10-28 (probado con dos emuladores; el aviso solo mientras la app está abierta: falta en segundo plano)
- [x] (IMP-2) r2-fondo-sorpresa — [S] un lugar al azar cada vez que se abre, además de "cada día" y uno fijo — ui/peri/PeriWorld.kt · hecho 2026-10-28
- [x] (IMP-2) r2-fondo-vivo — [M] el fondo con profundidad y luz: colinas lejanas en la bruma, montañas nevadas, faro que gira de noche, sol de arcilla con rayos, nubes y árboles con sombra, paralaje al inclinar el móvil; formas y degradados en caché — ui/peri/PeriWorld.kt · hecho 2026-10-28 (0,2 ms por fotograma en el hilo de la interfaz, sin fotogramas tarde)
- [x] (IMP-2) r2-bienvenida — [M] la bienvenida nueva: el mundo de Peri, los apartados y vestir a tu Peri desde el principio — ui/peri/WelcomeActivity.kt · hecho 2026-10-28 (sobre su mundo, con la página de su ropa)
- [x] (IMP-1) r2-confianza-familia — [M] correlación de confianza entre familiares: Peri pregunta en silencio al Peri del familiar ("¿has cambiado de número?") antes de avisar; el mismo timo a dos familiares a la vez avisa a toda la familia; un sí solo cuenta si el móvil del guardián no está él mismo en peligro — core/, backend/src/ · hecho 2026-10-28: «¿has sido tú?» (`/v1/checks`, 15 min, solo la pregunta, cada móvil contesta una vez; Peri dice quién contestó o que nadie, y un sí nunca quita el aviso) y aviso de campaña (misma trampa en dos o más en 30 min); probado con dos emuladores. El guardián en peligro pasa a `r2-guardian-en-peligro`
- [x] (IMP-1) r2-doble-familia — [M] mejorar la doble autenticación familiar (la petición con la pera de quien la pide, quién aprobó, dos guardianes para lo más arriesgado) — backend/src/approvals/, android/ · hecho 2026-10-28: la pera de quien pide, vestida como la conoce la familia, en la página (navegador) y en el Peri del cuidador; probado. Quién aprobó y dos guardianes pasan a `r2-guardian-en-peligro`
- [x] (IMP-1) r2-colgar — [M] colgar al desconocido: botón en la pausa, la llamada que espera a que la línea quede libre, y «colgar su llamada» para la familia (`/v1/pears/:id/hangup`) — core/, backend/src/pears/, android/ · hecho 2026-10-28 (probado con dos emuladores: Ana colgó la llamada de Mamá)
- [x] (IMP-1) r2-bloquear — [S] bloquear el número de un timo desde su tarjeta: llamadas rechazadas (con el rol de filtrar llamadas), mensajes en silencio, desbloqueo en Ajustes — android/ · hecho 2026-10-28
- [x] (IMP-1) r2-avisos-fuera — [M] las noticias de la familia como notificaciones con la app cerrada, con «¿has sido tú?» contestable desde la notificación — services/FamilyNotes.kt · hecho 2026-10-28 (probado con dos emuladores)
- [x] (IMP-2) r2-estrellas — [S] estrellas de plastilina de Muse Image que saltan de Peri — ui/peri/Sparkles.kt · hecho 2026-10-28 (0,02 $)
- [x] (IMP-2) r2-pueblo — [M] un fondo con sprites de Muse Image (el pueblo de Peri) junto a los dibujados a mano, para compararlos — ui/peri/PeriWorld.kt · hecho y retirado 2026-10-28: la persona prefiere el mundo dibujado a mano desde cero (queda en el commit 478e636)
- [x] (IMP-1) r2-pulido-estres — [M] la app menos cargada, más animada, optimizada y probada en situaciones de estrés; la ropa bien en el fotograma de mirar a la derecha — ui/peri/, eval/stress.py · hecho 2026-10-28: dos anclajes (cara y cabeza) en `anchors.py`; una tarjeta cada vez y fondo más tranquilo; 30 SMS seguidos, sin red, poca memoria, app matada, letra grande, 60 cambios de pestaña y 3.700 toques del monkey sin un cierre ni un bloqueo; memoria tras recorrer las pestañas de 121 a 76 MB
- [ ] (IMP-1) r2-guardian-en-peligro — [M] un sí solo cuenta si el móvil del guardián no está él mismo en peligro (su pera en atención o una llamada rara); quién aprobó, a la vista de la familia; dos guardianes para lo más arriesgado — backend/src/approvals/, android/
- [ ] (IMP-2) r2-trampas-mas — [S] más viñetas de ataques con Muse Image (Hacienda, luz, inversión, policía falsa, amor, trabajo, segunda mano, QR, voz clonada) y sus preguntas del juego — assets/peri/, res/values*/
- [ ] (SEG) r2-android-restricciones — [M] pelear con las restricciones de Android (ajustes restringidos, códigos ocultos en Android 15, batería, rol de llamadas, superposición): una revisión con la guía de cada una — android/
- [ ] (IMP-1) r2-meta-seguridad — [M] más jugo a los modelos de Meta en seguridad: viñetas al vuelo con Muse Image para trampas nuevas, Muse Spark comparando con lo que dice el Peri del familiar, resumen semanal escrito por Spark — backend/src/
- [ ] (IMP-2) r2-taste — [S] una pasada con la skill design-taste-frontend sobre las pantallas, con D8 por delante — ui/peri/

### F · Seguridad al máximo (28 oct, noche)
Por orden: primero lo que corta un timo en curso o impide que el propio Peri se vuelva contra la persona; después lo que endurece el servidor y el móvil; al final, lo que necesita servicios de fuera.
- [x] (IMP-1) r2s-colgar-con-aviso — [S] «colgar su llamada» con margen: Peri dice «Ana me pide que cuelgue» y cuenta tres segundos en los que la persona puede decir que no; queda en el registro — core/, android/ · hecho 2026-10-28 (tres segundos con «Seguir hablando»; la pausa se cierra al acabar la llamada)
- [ ] (IMP-1) r2s-guardian-seguro — [M] = `r2-guardian-en-peligro`: un sí no cuenta si el móvil del guardián está en peligro (su pera en atención o con un desconocido al teléfono); dos guardianes para cambiar guardianes o mandar un código durante una llamada; quién aprobó, a la vista — backend/src/approvals/, android/
- [ ] (SEG) r2s-sin-token-release — [S] la build de release sin el token compartido y el servidor de la demo con `ALLOW_SHARED_TOKEN=0`; un test que falle si el token entra en un APK de release — android/app/build.gradle.kts, backend/
- [ ] (SEG) r2s-https-pin — [M] servidor de la familia solo por HTTPS (túnel fijo), HSTS y el certificado fijado en la app (network security config con pin y fecha de caducidad) — android/app/src/main/res/xml/, backend/
- [ ] (SEG) r2s-revision-nuevo — [M] revisión de seguridad de lo de hoy: `/v1/checks`, `/v1/pears/:id/hangup`, `/v1/hangup`, notificaciones con acciones, lista de bloqueo; límites por clave de móvil además de por IP — backend/src/, android/
- [ ] (IMP-1) r2s-accesibilidad-intrusa — [S] avisar si una app desconocida tiene accesibilidad o está compartiendo la pantalla (lo que usan los troyanos bancarios y el «soporte técnico» falso) — android/.../device/
- [ ] (IMP-1) r2s-sim-cambiada — [M] notar un cambio de SIM o de número en el móvil de la persona y preguntar a la familia (el robo de SIM abre las cuentas) — android/, backend/src/checks/
- [ ] (IMP-1) r2s-bloqueo-familiar — [M] los números bloqueados por un móvil protegen a toda la familia: solo viaja su huella (SHA-256 con sal de la familia), nunca el número — backend/src/pears/, android/
- [ ] (IMP-1) r2s-guardado-cifrado — [M] lo que Peri guarda en el móvil (diario, último timo, bloqueados, peras) cifrado con una clave del Keystore — android/.../config/
- [x] (TEST) r2s-e2e-familia — [M] las pruebas de dos móviles en un script repetible (¿has sido tú?, colgar, aprobación, notificaciones con la app cerrada) con informe — eval/ · hecho 2026-10-28: `eval/family4.py` con cuatro móviles (llamada de un desconocido colgada desde la notificación de Papá, «número nuevo» con respuesta de Pablo, email de phishing a Ana y la misma trampa por SMS a Pablo: aviso de campaña); sin cierres
- [ ] (IMP-1) r2s-meta-rojo — [M] Muse Spark escribe cada semana variantes nuevas de timos para probar el escudo (equipo rojo) y Muse Image las viñetas; los fallos van al informe — training/, eval/
- [ ] (IMP-1) r2s-qr-trampa — [S] «mira esta imagen» detecta QR que llevan a webs falsas (SAM 3.1 + Muse Spark), sin abrirlos — backend/src/models/
- [ ] (SEG) r2s-servidor-duro — [M] peticiones y preguntas persistidas cifradas, rotación de claves de móvil y del token de admin, registros sin datos personales revisados — backend/
- [ ] (SEG) r2s-integridad — [L] (después) comprobar que la app que habla con el servidor es la nuestra (Play Integrity) y Safe Browsing para los enlaces — necesita Google Play
### Se llevan de antes
- Con la persona: pruebas-mayores, voice-transcribe (voces reales), bases-y-dudas, peri-referencia-video y la entrega.
- TEST y PUL: metricas, readme-resultados.
- Opcionales o fuera de alcance: tunel-https, iphone (necesita un Mac). Aparcadas: glimmer-servido y el LoRA (`training/FINETUNE.md`).

## Día 1 · 27 oct — validar lo desconocido
- [x] (IMP-1) creditos-y-spark — [S] reclamar los 150 $, límite de gasto, probar formato de Muse Spark y llamadas a herramientas — backend/src/models/ · hecho 2026-10-27
- [x] (IMP-1) muse-voz — [M] Muse Voice Transcribe como oídos de Peri (Ajustes, desactivado por defecto): grabación con fin por silencio, `POST /v1/transcribe`, vuelta al reconocedor de Android — backend/src/models/voice.ts, android/.../device/VoiceRecorder.kt · hecho 2026-10-27
- [ ] (IMP-2) voice-transcribe — [S] 10 órdenes en español dichas por personas mayores; comparar con el reconocedor de Android — android/app/src/main/java/app/friendlypear/device/
- [ ] (IMP-1) glimmer-servido — [M] servir Muse Glimmer en local o en Modal/Baseten y medir latencia — models/glimmer/ · aparcada 27 oct (decisión de la persona): la segunda opinión va con Muse Spark Open en Baseten; Glimmer no hace falta para la demo
- [x] (IMP-1) finetune-viable — [S] comprobar si Modal o Fireworks permiten LoRA de Muse Glimmer / Spark Open y su coste — training/ · hecho 2026-10-27: Glimmer sí (Unsloth, gratis en Kaggle), pero servir el adaptador exige un despliegue propio; Spark Open (816B) no es viable aquí. Recomendación: no afinar en el hackathon (`training/FINETUNE.md`)
- [ ] (TEST) reservado-200 — [M] recoger 200 mensajes reales anonimizados que nunca se usan para entrenar — eval/data/ (no se sube) · a medias: 200 escritos a mano en `eval/reservado-200/` (no reales); faltan los reales de familias
- [x] (TEST) prueba-20-20 — [S] 20 estafas actuales y 20 mensajes reales anonimizados por Spark y Glimmer; criterio de pivote si fallan más de un tercio — eval/ · hecho 2026-10-27 con 40 sintéticos (es/en) y Muse Spark Open: sin pivote; repetir con reales
- [ ] (DIR) bases-y-dudas — [S] confirmar en #questions que el diseño previo está permitido; guardar la respuesta — docs/agentes/

## Día 2 · 28 oct — núcleo y datos
- [x] (IMP-1) base-conocimiento — [M] fichas de los 15 tipos de estafa (señales, ejemplos, acción segura) — training/knowledge/ · hecho 2026-10-27: 15 fichas en `backend/src/investigator/knowledge.ts` (amor, trabajos de tareas, segunda mano, falsa policía y préstamos con comisión añadidas), con tests
- [x] (IMP-1) dataset-entrenamiento — [M] ~1.500 ejemplos anonimizados en JSONL con etiquetas de `ENTRENAMIENTO.md` — training/data/ (no se sube) · hecho 2026-10-27 (1.306 sintéticos con Spark contributor, `training/generate.ts`; sin mensajes reales todavía)
- [x] (IMP-1) pesos-aprendidos — [M] regresión logística para los pesos de las señales del escudo, con informe — training/, core/.../shield/ · hecho 2026-10-27 (`eval/REPORT.md`: AUC 0,965 frente a 0,957 y 67 % de avisos firmes frente a 53 %, pero una falsa alarma firme en 20/20; `ShieldWeights.LEARNED` existe y no es el de por defecto)
- [x] (IMP-1) escudo-capas — [L] señales, log-odds, reglas duras, acción segura y explicación de Peri — core/.../shield/ · hecho 2026-10-27
- [x] (IMP-1) voz-y-agenda — [M] órdenes en español, alias y relaciones — core/.../voice/ · hecho 2026-10-27
- [x] (IMP-1) agente-base — [M] bucle con confirmaciones y verificación de resultados — core/.../agent/ · hecho 2026-10-27
- [x] (IMP-2) android-servicios — [M] lector de notificaciones, estado de llamada, voz y TTS — android/.../services/, device/ · hecho 2026-10-27
- [x] (SEG) permisos-minimos — [S] permisos justos y ningún log con texto de mensajes — android/app/src/main/AndroidManifest.xml · hecho 2026-10-27 (revisados; HTTP en claro solo en debug; el puente de depuración exige DUMP, es decir, `adb`)

- [x] (IMP-1) peri-bilingue — [L] Peri habla y entiende inglés además de español: frases del escudo y del agente, órdenes de voz, relaciones y señales de estafa en inglés; TTS y reconocedor en el idioma del móvil — core/.../shield/, voice/, agent/, android/.../device/ · hecho 2026-10-27

## Día 3 · 29 oct — primer flujo en el móvil
- [x] (IMP-2) ui-compose — [M] pasar la UI a Jetpack Compose + Material 3 con la paleta de `docs/DISENO.md` — android/app/ · descartada 2026-10-27: la app se queda en vistas nativas con `Style.kt` y el diseño plano de `docs/DISENO.md`; pasarla a Compose no cambia nada que vea el jurado
- [x] (IMP-2) peri-animada — [L] Peri vectorial con estados tranquila, escuchando, pensando, preocupada y protectora; respeta "quitar animaciones" — android/.../ui/peri/ · a medias: fotogramas de Muse Image recortados (parpadeo en 4 pasos, dos bocas, saludo) con inclinación, bote, sombra y pop entre estados; sustituida 2026-10-27 por la Peri de plastilina (9 estados de 13 fotogramas y 13 poses en `PeriFace`, con movimiento propio y quieta si se quitan las animaciones): la vectorial ya no hace falta
- [x] (IMP-2) peri-mini — [M] modo Mini Peri: solo la pera flotando sobre otras apps, con bocadillo; tocar = hablar, arrastrar = mover, mantener = abrir la app — android/.../ui/peri/MiniPeriService.kt
- [x] (IMP-2) peri-fotogramas — [S] 4 fotogramas por estado con Muse Image (36, 0,36 $) — assets/peri/
- [x] (IMP-2) peri-miradas — [S] 27 fotogramas más (miradas a cada lado y boca en "o"), 0,27 $; Peri mira a los lados cada 6–11 s — assets/peri/, ui/peri/PeriFace.kt · hecho 2026-10-27
- [x] (TEST) prueba-amplia-1 — [S] emulador + backend; 3 fallos arreglados (fundido, fotogramas viejos, diálogo de ubicación en la emergencia) — docs/agentes/hallazgos/2026-10-27-prueba-amplia.md · hecho 2026-10-27
- [x] (IMP-2) peri-pulido — [M] interfaz profesional: Peri recortada a 1024 px (`cutout.py`), animación sin fundidos entre poses, 24 imágenes nuevas (0,24 $), icono de la app, botones con icono, chip de estado, tarjeta de pasos pendientes, bocadillo con cola, familia en tarjetas — assets/peri/, android/.../ui/peri/
- [x] (IMP-2) permisos-progresivos — [M] pedir cada permiso al usarlo por primera vez con la explicación de Peri; degradar funciones si se deniega — android/.../ui/peri/, config/ · hecho 2026-10-27 (primer arranque: agenda, SMS y estado de llamada de uno en uno; micrófono, llamadas, envío de SMS y ubicación al usarlos)
- [x] (IMP-2) momento-1 — [M] "he cambiado de número" → llamada a la hija real en un móvil — android/ · hecho 2026-10-27 (en el emulador, en español e inglés; falta grabarlo en un móvil real)
- [x] (IMP-2) momento-2 — [S] código durante llamada → "cuelga" — android/ · hecho 2026-10-27 (en el emulador, en español e inglés; falta grabarlo en un móvil real)
- [x] (IMP-1) investigador — [L] Muse Spark con herramientas de solo lectura (dominio, historial, agenda, imagen) — backend/src/investigator/
- [x] (IMP-1) investigador-en-el-movil — [M] si tras la segunda opinión sigue en DOUBT, el móvil llama a `/v1/investigate` y guarda la evidencia para la familia — android/app/src/main/java/app/friendlypear/net/
- [x] (IMP-2) peri-v1 — [M] pantalla de un botón con estados básicos de Peri — android/.../ui/peri/ · hecho 2026-10-27 (pantalla de un botón con la frase de Peri en grande (sin estados ilustrados todavía))

## Día 4 · 30 oct — familia y llamadas
- [x] (IMP-1) cuentame-llamada — [M] clasificador de relatos de llamadas frente a guiones de vishing — backend/src/investigator/, training/ · hecho 2026-10-27: `core/shield/CallStory.kt`: reglas en el móvil (lo que cuenta la persona no sale de él) para quién decía ser y qué pedía; «¿Qué te pidieron?» tras un banco a secas; consejo por truco y oferta de llamar a la familia; 7 tests
- [x] (IMP-2) filtro-llamadas — [M] `CallScreeningService`: aviso de número fuera de la agenda antes de descolgar — android/.../services/ · hecho 2026-10-27 (aviso antes de descolgar, probado en el emulador)
- [x] (IMP-1) verificar-llamante — [M] al colgar, "¿quién decía ser?" → comparar con la agenda → ofrecer llamar al número guardado; palabra de la familia; si pide dinero, colgar y devolver la llamada — core/.../agent/, voice/, trust/ · hecho 2026-10-27 (al colgar, "¿quién decía ser?" → llamada al número guardado; probado en el emulador)
- [x] (SEG) llamadas-sin-audio — [S] verificar que nada accede al audio de llamadas y que el rol de filtro se pide con explicación — android/app/src/main/AndroidManifest.xml · hecho 2026-10-27 (sin acceso al audio; el micrófono no se abre durante ninguna llamada)
- [x] (IMP-1) aprobaciones — [M] enlace de un solo uso, 15 min, página del guardián — backend/src/approvals/ · hecho 2026-10-27
- [x] (IMP-2) mfa-punta-a-punta — [M] MFA familiar con dos móviles — android/ · hecho 2026-10-27 (dos emuladores; SMS entre ellos entregado con adb)
- [x] (IMP-1) avisos-familia — [M] inmediatos, ataque en curso, resumen semanal — core/.../alerts/ · hecho 2026-10-27 (avisos al momento en la página, URGENTE en la petición, y resumen semanal por SMS con solo los recuentos: `WeeklySummary`, alarma diaria inexacta; probado: "avisé de 3 mensajes que eran timos")
- [x] (IMP-2) ajustes-idioma — [S] pantalla de Ajustes (rueda en la cabecera) con Idioma: igual que el móvil, español o inglés; cambia pantallas y voz de Peri sin reiniciar — android/.../ui/peri/SettingsActivity.kt · hecho 2026-10-27
- [x] (IMP-2) mini-en-llamadas — [S] Mini Peri sale sola junto a la pantalla de la llamada (llamadas que hace Peri y llamadas descolgadas de desconocidos) y se va al colgar — android/.../ui/peri/MiniPeriService.kt · hecho 2026-10-27
- [x] (IMP-1) novedades-en-disco — [S] enlaces de guardianes (solo hash) y novedades en `backend/data/guardians.json`, escritura atómica — backend/src/guardians/ · hecho 2026-10-27
- [ ] (IMP-2) iphone — [L] app de iPhone según `docs/IPHONE.md`: `core` con Kotlin Multiplatform, SwiftUI, filtro de SMS, pausa con Screen Time, Live Activity, compartir con Peri — necesita un Mac con Xcode y cuenta de Apple Developer
- [x] (IMP-1) pausa-segura — [M] app de dinero o de control remoto durante la llamada de un desconocido, o 30 min después de un timo, un dudoso o una llamada desconocida → pantalla de pausa sobre la app: llamar a la familia, salir, o seguir (a los 5 s) — core/.../agent/, android/.../ui/peri/PauseOverlay.kt · hecho 2026-10-27
- [x] (IMP-1) enlaces-inseguros — [S] señal `INSECURE_LINK`: `http://`, puerto distinto de 443 o IP sin nombre; peso 2,5, avisa siempre (dudoso) aunque venga de la familia — core/.../shield/ · hecho 2026-10-27
- [x] (IMP-1) pedir-ayuda — [S] "pide ayuda a mamá", "avisa a mi hija", "ask my daughter for help", "llama a mamá, que necesito ayuda" → llamada con el "sí" de siempre; relación `PARENT` (madre, padre, mum, dad) — core/.../voice/ · hecho 2026-10-27
- [x] (IMP-1) docs-confianza-y-modelos — [S] `docs/CONFIANZA.md` (acompañar, no vigilar; guion para proponerlo) y `docs/META_MODELS.md` (modelos en el motor y en las imágenes) · hecho 2026-10-27
- [x] (IMP-2) vista-guardian — [M] "Novedades de Peri": enlace personal por guardián (`/g/:token`, solo su hash), resumen de 7 días y cronología de avisos, pausas y aprobaciones; solo si la persona lo activa, con la misma lista en su móvil; quitarlo invalida los enlaces — backend/src/guardians/ · hecho 2026-10-27 (se guarda en disco desde novedades-en-disco)
- [x] (SEG) backend-endurecido — [S] cabeceras, límites de cuerpo y de peticiones — backend/src/server.ts · hecho 2026-10-27 (`limits.ts`: 30/min en `/a/` y `/g/`, 240/min en `/v1/`, 10 tokens malos → 15 min fuera; cuerpo leído hasta el límite; cabeceras en todo)

## Día 5 · 31 oct — confianza y emergencia
- [x] (IMP-2) te-estan-guiando — [M] avisos al instalar apps de control remoto y al abrir la app del banco durante una llamada desconocida — android/.../services/ · hecho 2026-10-27 (apps de control remoto y app del banco en llamada desconocida; probado con el puente de depuración, sin apps reales)
- [ ] (IMP-1) finetune-glimmer-1 — [L] apuesta: LoRA de Glimmer con el dataset — training/ · aparcada 27 oct (decisión de la persona: no afinar en el hackathon); el porqué en `training/FINETUNE.md`
- [x] (IMP-2) emergencia — [M] cuenta atrás, llamada y SMS con ubicación — android/ · hecho (ya estaba): "me he caído" → 10 s para cancelar, SMS a la familia con la última ubicación y llamada (`PeriAgent.onEmergencyTimeout`, tests en es y en)
- [x] (IMP-2) config-familia — [M] guardianes, alias, relaciones, números oficiales — android/.../config/ · hecho 2026-10-27 (guardianes, relaciones, apodos y palabra de la familia sin guardarla; faltan los números oficiales)
- [x] (IMP-2) panel-permisos — [M] "Lo que Peri puede hacer": estado de cada permiso, para qué sirve, botón para quitarlo y registro de usos sensibles — android/.../ui/peri/, config/ · hecho 2026-10-27: `AccessActivity` con 12 permisos y accesos, encender o apagar en la pantalla de Android, y `UseLog` (llamadas, SMS, ubicación, fotos, voz, denuncias)
- [x] (PUL) docs-hackaton — [S] README con "At a glance" para el jurado, conectar cada móvil, bienvenida, guía de desbloqueo y voz; META_MODELS (diseños, fotogramas, datos de entrenamiento) y ARQUITECTURA al día; página de `bun pair` con Peri, pasos y cuenta atrás — README.md, docs/, backend/src/pair.ts · hecho 2026-10-27
- [x] (SEG) emparejamiento-qr — [L] clave por dispositivo con QR en persona — android/.../config/, backend/src/ · hecho 2026-10-27 (`bun pair`: código de un uso, 10 min, QR propio sin dependencias verificado con los dos lectores de OpenCV; clave por móvil cifrada con el Keystore, solo hashes en el servidor, desconexión por móvil, `ALLOW_SHARED_TOKEN=0`; un QR de otro servidor se rechaza; sin copias de seguridad)
- [x] (IMP-2) gifs-y-microfono — [S] los dos GIFs del README regrabados con el diseño nuevo en dos emuladores (aprobación por la web en inglés; Peri a Peri, madre en español e hija en inglés); al grabarlos salió que el reconocedor seguía abierto si empezaba una llamada mientras Peri escuchaba: ahora se cierra y lo oído se descarta — docs/media/, android/.../device/VoiceInput.kt · hecho 2026-10-27
- [x] (IMP-2) diseno-sencillo — [M] la app y las páginas web con la guía design-taste-frontend, sin perder el estilo amable: fondo crema liso, bordes finos sin sombras, tres tamaños de letra, un botón verde por pantalla; textos sin jerga y más frases; 5 poses nuevas (risa, corazón, mareo, bostezo, carta, 0,05 $) con corazones y zzz dibujados; fotogramas al tamaño justo y en segundo plano (Mini Peri fuera: 176 → 65 MB) — android/.../ui/peri/, backend/src/, assets/peri/ · hecho 2026-10-27
- [x] (IMP-2) bienvenida — [M] Peri se presenta antes de pedir permisos: 4 páginas animadas, idioma arriba, solo la primera en voz alta; no sale al actualizar — android/.../ui/peri/WelcomeActivity.kt · hecho 2026-10-27
- [x] (IMP-1) circulo-reglas — [S] segundo guardián y 48 h para cambios — core/.../trust/ · hecho 2026-10-27 (el sí de un guardián actual + 48 h; la persona puede cancelar)

## Día 6 · 1 nov — Meta a fondo y Peri
- [ ] (IMP-1) finetune-glimmer-2 — [M] evaluar el modelo afinado frente al baseline en el reservado — training/, eval/
- [x] (IMP-1) denuncia-un-toque — [S] P2: informe para policía o 017 generado con Muse Spark — backend/src/investigator/ · hecho 2026-10-27: `POST /v1/report` + `ReportActivity`: declaración en primera persona y sin género, enlaces desactivados, «dónde llevarlo» fijo (017, comisaría; Report Fraud, FTC); probado con la API real
- [x] (IMP-2) vineta-trampa — [S] P2: viñeta de 3 escenas con Muse Image tras una estafa — android/, assets/peri/ · hecho 2026-10-27: `TrapActivity`: 6 tipos × 3 viñetas de Muse Image (0,18 $), elegidas por las señales y la marca
- [ ] (DIR) peri-referencia-video — [S] imagen de referencia de Peri para el vídeo (sin Higgsfield, D11) — docs/demo/
- [x] (IMP-1) mira-esta-imagen — [M] Muse Spark multimodal + SAM 3.1 marcando QR o enlace — backend/src/models/, investigator/ · hecho 2026-10-27 (`POST /v1/picture`: Spark con imagen + SAM 3.1 para el QR; botón "Mira esta imagen" y compartir con Peri desde cualquier app; prueba real: timo 97 %, QR marcado)
- [x] (IMP-1) glimmer-primera-opinion — [M] Glimmer como primera opinión privada en dudosos — backend/src/models/ · hecho 2026-10-27 (resuelta con Muse Spark Open en Baseten en lugar de Glimmer)
- [x] (IMP-2) peri-estados-extra — [M] llamando, esperando a la familia, contenta (confeti) y dormida; vibraciones y sonidos suaves — android/.../ui/peri/ · hecho 2026-10-27: los estados ya estaban; `device/Haptics.kt` añade vibraciones por estado (sin sonidos, que asustan); comprobado en el registro de vibración de Android
- [x] (IMP-2) escudos-ganados — [S] P1: escuditos por estafas frenadas y por preguntar antes — android/, core/ · hecho 2026-10-27: `config/Shields.kt`, insignia junto a Peri y felicitación solo con calma
- [x] (IMP-2) peri-ilustrada — [M] estados de Peri con Muse Image — assets/peri/, android/app/src/main/res/drawable/ · hecho 2026-10-27 (9 estados con Muse Image, misma Peri por edición de una base; 0,11 $)
- [x] (IMP-2) web-basica — [S] P2: notificaciones basura de webs y DNS privado guiado — android/ · hecho 2026-10-27: avisos de webs por el navegador al escudo y botón al interruptor de esa web; DNS privado con Quad9 guiado en Ajustes

## Día 7 · 2 nov — evaluación
- [ ] (TEST) metricas — [M] TPR, FPR, falsas alarmas/mes con Wilson en el conjunto reservado, una fila por capa de entrenamiento — eval/
- [ ] (TEST) pruebas-mayores — [M] 20 tareas con 2-3 personas mayores, con permiso — eval/
- [ ] (PUL) readme-resultados — [S] resultados, límites, qué se hizo en el hackathon, capturas — README.md

## Día 8 · 3 nov — demo
- [x] (DIR) grabaciones-reales — [M] los 5 momentos grabados en emulador o móvil real — docs/demo/ · hecho 2026-10-27 a petición de la persona: 7 clips reales en dos emuladores, montados a 1080p con rótulos en inglés (`docs/demo/README.md`; los vídeos en `dist/demo/`, fuera del repo). Faltan la voz y la música, que van en el montaje final
- [ ] (IMP-1) tunel-https — [S] **opcional** (27 oct, decisión de la persona): servidor con dirección HTTPS pública para probar en móviles reales fuera de casa. Preparado y probado hasta el último paso: backend con `PUBLIC_BASE_URL=https://rby.tailb6f848.ts.net TRUST_PROXY=1`, APK con `-Pfriendlypear.backendUrl=…` y la IP del cliente tomada de lo que añade el proxy. Falta solo aprobar Funnel en la cuenta de Tailscale (`tailscale funnel --bg 8711`). Los móviles no necesitan Tailscale. Sin túnel, la demo se graba con los dos emuladores — backend/src/main.ts, android/app/build.gradle.kts
- [x] (DIR) escenas-higgsfield — [M] escenas de historia con Higgsfield, rotuladas como generadas con IA — docs/demo/ · descartada 2026-10-28 (D11): fondos del mundo de Peri en su lugar
- [ ] (DIR) video-3-min — [M] montaje del vídeo de demo según `docs/VIDEO.md` — docs/demo/
- [ ] (DIR) anuncio — [M] anuncio de 30–45 s vertical y cuadrado, ES/EN — docs/demo/
- [ ] (DIR) post-x — [S] post con el anuncio para Most Viral — docs/demo/
- [x] (IMP-1) peri-a-peri — [L] si el guardián tiene FriendlyPear, la petición se abre en su Peri (voz, pantalla propia, notificación); solo de un contacto guardado y con enlace a nuestro servidor, datos leídos del servidor; "Llamar a Mamá ahora"; SMS que imitan a Peri → timo; página del guardián rediseñada con Peri y paso "¿Seguro?"; bloqueo del móvil para aprobar y para la configuración de la familia; `docs/SECURITY.md` — core/.../trust/FamilyRequestLink.kt, android/.../ui/peri/GuardianActivity.kt, backend/src/approvals/ · hecho 2026-10-27
- [x] (IMP-1) escudo-ronda-2 — [M] señales nuevas por análisis de errores en el entrenamiento (secreto, susto de virus, cebo romántico, más "número nuevo" y "reenvíame el SMS"; la negación se corta en "y"): reservado-200 del 83 % al 88 % de avisos y del 53 % al 60 % de avisos firmes, mismas falsas alarmas — core/.../shield/, eval/REPORT.md · hecho 2026-10-27
- [x] (IMP-1) mfa-dos-moviles — [M] doble verificación familiar con dos móviles: tercera respuesta "Que me llame primero" (Peri no manda nada y ofrece llamar al guardián), aviso URGENTE si hay o acaba de haber una llamada de un desconocido; probado con dos emuladores (madre y Ana) de principio a fin; GIF en el README — core/.../agent/, backend/src/approvals/, android/.../net/ApprovalClient.kt · hecho 2026-10-27
- [x] (TEST) movil-real-3 — [M] tercera ronda: notificaciones bloqueadas por ajustes restringidos (guía de desbloqueo de 3 pasos, sin pedir SMS al arrancar si está bloqueada), la "×" de Mini Peri parecía un muro (ahora ligera), SMS reales en el emulador (7 casos), chispas, mirar al dedo y bocadillo que se escribe; `docs/FEATURES.md` con todo lo que hay — docs/agentes/hallazgos/2026-10-27-movil-real.md · hecho 2026-10-27
- [x] (IMP-2) peri-viva — [M] Peri menos estática: pasos con un pie en alto (16 imágenes, 0,16 $), saltitos, pasitos, mirar alrededor, estirarse y saludar solos; peso al lado, aplastamiento al aterrizar y cabeceo al hablar; Mini Peri a 100 dp; entrada suave y tarjetas con borde — android/.../ui/peri/, assets/peri/ · hecho 2026-10-27
- [x] (IMP-2) peri-plastilina — [M] Peri en plastilina 3D (el diseño elegido de los 8): 9 estados, 7 fotogramas por estado y 9 extras con Muse Image (82 imágenes, 0,82 $); recorte que quita también el suelo — assets/peri/, res/drawable-nodpi/ · hecho 2026-10-27
- [x] (TEST) movil-real-2 — [M] segunda ronda en el móvil de la persona: SMS bloqueado (ahora opcional), voz robótica (elegir voz), Mini Peri descuidada, más poses y 8 diseños nuevos para elegir — docs/agentes/hallazgos/2026-10-27-movil-real.md · hecho 2026-10-27
- [x] (TEST) movil-real-1 — [M] primera prueba en el móvil de la persona: cierre al abrir, letra grande, voz, Mini Peri, familia con muchos contactos, personalidad — docs/agentes/hallazgos/2026-10-27-movil-real.md · hecho 2026-10-27
- [x] (TEST) instalacion-limpia — [S] instalar desde el README en un móvil limpio — docs/agentes/hallazgos/ · hecho 2026-10-27 en un clon nuevo + emulador (huecos del README cubiertos; los 5 momentos en es/en: `hallazgos/2026-10-27-instalacion-y-momentos.md`); falta en un móvil real

## Día 9 · 4 nov — entrega
- [ ] (DIR) entrega — [S] entregar antes de las 18:00 ET; repo público, vídeo y post accesibles — /

## Ideas (P3, el Director las amplía)
- Entrenamiento amable: simulacros seguros personalizados con Muse Spark + Muse Image.
- Guía visual con SAM 3.1: "toca aquí" en pantalla y en aparatos.
- App FriendlyPear Familia con push.
- Filtro web propio con VPN local.

## Riesgos y decisiones vigentes
- R1: modelo flojo en español → escudo más determinista (criterio en prueba-20-20).
- R2: Android bloquea llamadas desde segundo plano → servicio en primer plano.
- R3: Google Play restringe SMS y llamadas → APK instalado a mano para el hackathon.
- R4 (27 oct): sin servidor público, en un móvil real solo funciona lo local (escudo con reglas, llamadas, Mini Peri, Peri); la segunda opinión, el investigador, "mira esta imagen", la voz de Meta y la familia necesitan el túnel o un hosting. La demo de familia se graba con dos emuladores.
- D1: las reglas deciden; el modelo ajusta como máximo ±2 y nunca anula una regla dura.
- D2: ningún permiso se usa sin pedirlo y explicarlo; lo concede la persona protegida, no la familia.
- D3: Peri no escucha el audio de las llamadas; actúa antes (filtro) y después (verificación).
- D4: público = cualquier persona y familia; personas mayores como ejemplo principal de la demo.
- D5: nunca se evalúa con datos de entrenamiento ni sintéticos; solo con los 200 reales reservados.
- D6: en los vídeos, todo lo que es producto se graba de verdad; lo generado con IA solo para escenas de historia y rotulado.
- D7 (27 oct, decisión de la persona): el jurado habla inglés. La app es **bilingüe inglés/español** y sigue el idioma del móvil; todo lo que ve el jurado (UI, README, vídeo, post) va en inglés. Reglas en `PROTOCOLO.md` §9.
- D8 (27 oct, decisión de la persona): la app tiene que verse **viva y llamativa** (colores vivos, fondo animado, fuentes propias, efectos, más apartados) y Peri, graciosa y con mucha más vida. Donde las reglas planas de diseño choquen con esto, manda D8, sin perder la letra grande, el contraste y la simpleza.
- D9 (27 oct): prioridades de la ronda 2: simpleza, cómo se vende y seguridad. Público: todos.
- D10 (27 oct): la API de Meta no tiene modelo de vídeo; `muse-wonder-1.0` (texto, más caro) solo para trabajo puntual, como proponer frases de Peri que luego se revisan.
- D11 (28 oct, decisión de la persona): sin Higgsfield. "Podemos ir variando fondo": el mundo de Peri cambia de cielo con la hora y el estado, y de lugar cada día según la estación; esos mismos fondos sirven en el vídeo.
