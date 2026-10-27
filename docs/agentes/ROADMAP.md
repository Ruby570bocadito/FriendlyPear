# ROADMAP — FriendlyPear

Único escritor: DIR. Cada rol marca `[x]` sus tareas al terminar y propone ideas en su estado.
Formato: `- [ ] (ID) slug — [talla] descripción — rutas`. Tallas: `[S]` corta · `[M]` con tests · `[L]` partir en piezas.
Entrega: **4 nov 2026, 21:00 ET**. Objetivo propio: 18:00 ET.

## Base
- [x] (PUL) estructura-base — [S] carpetas, builds de core/android/backend, CI y documentación — /

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
- [ ] (DIR) peri-referencia-video — [S] imagen de referencia de Peri y prueba de Higgsfield MCP — docs/demo/
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
- [ ] (DIR) grabaciones-reales — [M] los 5 momentos grabados en emulador o móvil real — docs/demo/clips/
- [ ] (IMP-1) tunel-https — [S] **opcional** (27 oct, decisión de la persona): servidor con dirección HTTPS pública para probar en móviles reales fuera de casa. Preparado y probado hasta el último paso: backend con `PUBLIC_BASE_URL=https://rby.tailb6f848.ts.net TRUST_PROXY=1`, APK con `-Pfriendlypear.backendUrl=…` y la IP del cliente tomada de lo que añade el proxy. Falta solo aprobar Funnel en la cuenta de Tailscale (`tailscale funnel --bg 8711`). Los móviles no necesitan Tailscale. Sin túnel, la demo se graba con los dos emuladores — backend/src/main.ts, android/app/build.gradle.kts
- [ ] (DIR) escenas-higgsfield — [M] escenas de historia con Higgsfield, rotuladas como generadas con IA — docs/demo/
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
