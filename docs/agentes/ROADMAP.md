# ROADMAP — FriendlyPear

Único escritor: DIR. Cada rol marca `[x]` sus tareas al terminar y propone ideas en su estado.
Formato: `- [ ] (ID) slug — [talla] descripción — rutas`. Tallas: `[S]` corta · `[M]` con tests · `[L]` partir en piezas.
Entrega: **4 nov 2026, 21:00 ET**. Objetivo propio: 18:00 ET.

## Base
- [x] (PUL) estructura-base — [S] carpetas, builds de core/android/backend, CI y documentación — /

## Día 1 · 27 oct — validar lo desconocido
- [ ] (IMP-1) creditos-y-spark — [S] reclamar los 150 $, límite de gasto, probar formato de Muse Spark y llamadas a herramientas — backend/src/models/
- [ ] (IMP-2) voice-transcribe — [S] 10 órdenes en español dichas por personas mayores; comparar con el reconocedor de Android — android/app/src/main/java/app/friendlypear/device/
- [ ] (IMP-1) glimmer-servido — [M] servir Muse Glimmer en local o en Modal/Baseten y medir latencia — models/glimmer/
- [ ] (IMP-1) finetune-viable — [S] comprobar si Modal o Fireworks permiten LoRA de Muse Glimmer / Spark Open y su coste — training/
- [ ] (TEST) reservado-200 — [M] recoger 200 mensajes reales anonimizados que nunca se usan para entrenar — eval/data/ (no se sube)
- [ ] (TEST) prueba-20-20 — [S] 20 estafas actuales y 20 mensajes reales anonimizados por Spark y Glimmer; criterio de pivote si fallan más de un tercio — eval/
- [ ] (DIR) bases-y-dudas — [S] confirmar en #questions que el diseño previo está permitido; guardar la respuesta — docs/agentes/

## Día 2 · 28 oct — núcleo y datos
- [ ] (IMP-1) base-conocimiento — [M] fichas de los 15 tipos de estafa (señales, ejemplos, acción segura) — training/knowledge/
- [ ] (IMP-1) dataset-entrenamiento — [M] ~1.500 ejemplos anonimizados en JSONL con etiquetas de `ENTRENAMIENTO.md` — training/data/ (no se sube)
- [ ] (IMP-1) pesos-aprendidos — [M] regresión logística para los pesos de las señales del escudo, con informe — training/, core/.../shield/
- [ ] (IMP-1) escudo-capas — [L] señales, log-odds, reglas duras, acción segura y explicación de Peri — core/.../shield/
- [ ] (IMP-1) voz-y-agenda — [M] órdenes en español, alias y relaciones — core/.../voice/
- [ ] (IMP-1) agente-base — [M] bucle con confirmaciones y verificación de resultados — core/.../agent/
- [ ] (IMP-2) android-servicios — [M] lector de notificaciones, estado de llamada, voz y TTS — android/.../services/, device/
- [ ] (SEG) permisos-minimos — [S] permisos justos y ningún log con texto de mensajes — android/app/src/main/AndroidManifest.xml

## Día 3 · 29 oct — primer flujo en el móvil
- [ ] (IMP-2) ui-compose — [M] pasar la UI a Jetpack Compose + Material 3 con la paleta de `docs/DISENO.md` — android/app/
- [ ] (IMP-2) peri-animada — [L] Peri vectorial con estados tranquila, escuchando, pensando, preocupada y protectora; respeta "quitar animaciones" — android/.../ui/peri/
- [ ] (IMP-2) permisos-progresivos — [M] pedir cada permiso al usarlo por primera vez con la explicación de Peri; degradar funciones si se deniega — android/.../ui/peri/, config/
- [ ] (IMP-2) momento-1 — [M] "he cambiado de número" → llamada a la hija real en un móvil — android/
- [ ] (IMP-2) momento-2 — [S] código durante llamada → "cuelga" — android/
- [ ] (IMP-1) investigador — [L] Muse Spark con herramientas de solo lectura (dominio, historial, agenda, imagen) — backend/src/investigator/
- [ ] (IMP-2) peri-v1 — [M] pantalla de un botón con estados básicos de Peri — android/.../ui/peri/

## Día 4 · 30 oct — familia y llamadas
- [ ] (IMP-1) cuentame-llamada — [M] clasificador de relatos de llamadas frente a guiones de vishing — backend/src/investigator/, training/
- [ ] (IMP-2) filtro-llamadas — [M] `CallScreeningService`: aviso de número fuera de la agenda antes de descolgar — android/.../services/
- [ ] (IMP-1) verificar-llamante — [M] al colgar, "¿quién decía ser?" → comparar con la agenda → ofrecer llamar al número guardado; palabra de la familia; si pide dinero, colgar y devolver la llamada — core/.../agent/, voice/, trust/
- [ ] (SEG) llamadas-sin-audio — [S] verificar que nada accede al audio de llamadas y que el rol de filtro se pide con explicación — android/app/src/main/AndroidManifest.xml
- [ ] (IMP-1) aprobaciones — [M] enlace de un solo uso, 15 min, página del guardián — backend/src/approvals/
- [ ] (IMP-2) mfa-punta-a-punta — [M] MFA familiar con dos móviles — android/
- [ ] (IMP-1) avisos-familia — [M] inmediatos, ataque en curso, resumen semanal — core/.../alerts/
- [ ] (SEG) backend-endurecido — [S] cabeceras, límites de cuerpo y de peticiones — backend/src/server.ts

## Día 5 · 31 oct — confianza y emergencia
- [ ] (IMP-2) te-estan-guiando — [M] avisos al instalar apps de control remoto y al abrir la app del banco durante una llamada desconocida — android/.../services/
- [ ] (IMP-1) finetune-glimmer-1 — [L] apuesta: LoRA de Glimmer con el dataset — training/
- [ ] (IMP-2) emergencia — [M] cuenta atrás, llamada y SMS con ubicación — android/
- [ ] (IMP-2) config-familia — [M] guardianes, alias, relaciones, números oficiales — android/.../config/
- [ ] (IMP-2) panel-permisos — [M] "Lo que Peri puede hacer": estado de cada permiso, para qué sirve, botón para quitarlo y registro de usos sensibles — android/.../ui/peri/, config/
- [ ] (SEG) emparejamiento-qr — [L] clave por dispositivo con QR en persona — android/.../config/, backend/src/
- [ ] (IMP-1) circulo-reglas — [S] segundo guardián y 48 h para cambios — core/.../trust/

## Día 6 · 1 nov — Meta a fondo y Peri
- [ ] (IMP-1) finetune-glimmer-2 — [M] evaluar el modelo afinado frente al baseline en el reservado — training/, eval/
- [ ] (IMP-1) denuncia-un-toque — [S] P2: informe para policía o 017 generado con Muse Spark — backend/src/investigator/
- [ ] (IMP-2) vineta-trampa — [S] P2: viñeta de 3 escenas con Muse Image tras una estafa — android/, assets/peri/
- [ ] (DIR) peri-referencia-video — [S] imagen de referencia de Peri y prueba de Higgsfield MCP — docs/demo/
- [ ] (IMP-1) mira-esta-imagen — [M] Muse Spark multimodal + SAM 3.1 marcando QR o enlace — backend/src/models/, investigator/
- [ ] (IMP-1) glimmer-primera-opinion — [M] Glimmer como primera opinión privada en dudosos — backend/src/models/
- [ ] (IMP-2) peri-estados-extra — [M] llamando, esperando a la familia, contenta (confeti) y dormida; vibraciones y sonidos suaves — android/.../ui/peri/
- [ ] (IMP-2) escudos-ganados — [S] P1: escuditos por estafas frenadas y por preguntar antes — android/, core/
- [ ] (IMP-2) peri-ilustrada — [M] estados de Peri con Muse Image — assets/peri/, android/app/src/main/res/drawable/
- [ ] (IMP-2) web-basica — [S] P2: notificaciones basura de webs y DNS privado guiado — android/

## Día 7 · 2 nov — evaluación
- [ ] (TEST) metricas — [M] TPR, FPR, falsas alarmas/mes con Wilson en el conjunto reservado, una fila por capa de entrenamiento — eval/
- [ ] (TEST) pruebas-mayores — [M] 20 tareas con 2-3 personas mayores, con permiso — eval/
- [ ] (PUL) readme-resultados — [S] resultados, límites, qué se hizo en el hackathon, capturas — README.md

## Día 8 · 3 nov — demo
- [ ] (DIR) grabaciones-reales — [M] los 5 momentos grabados en emulador o móvil real — docs/demo/clips/
- [ ] (DIR) escenas-higgsfield — [M] escenas de historia con Higgsfield, rotuladas como generadas con IA — docs/demo/
- [ ] (DIR) video-3-min — [M] montaje del vídeo de demo según `docs/VIDEO.md` — docs/demo/
- [ ] (DIR) anuncio — [M] anuncio de 30–45 s vertical y cuadrado, ES/EN — docs/demo/
- [ ] (DIR) post-x — [S] post con el anuncio para Most Viral — docs/demo/
- [ ] (TEST) instalacion-limpia — [S] instalar desde el README en un móvil limpio — docs/agentes/hallazgos/

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
- D1: las reglas deciden; el modelo ajusta como máximo ±2 y nunca anula una regla dura.
- D2: ningún permiso se usa sin pedirlo y explicarlo; lo concede la persona protegida, no la familia.
- D3: Peri no escucha el audio de las llamadas; actúa antes (filtro) y después (verificación).
- D4: público = cualquier persona y familia; personas mayores como ejemplo principal de la demo.
- D5: nunca se evalúa con datos de entrenamiento ni sintéticos; solo con los 200 reales reservados.
- D6: en los vídeos, todo lo que es producto se graba de verdad; lo generado con IA solo para escenas de historia y rotulado.
