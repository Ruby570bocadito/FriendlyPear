# Estado IMP-2

Tareas hechas por IMP-1 a petición de la persona. Todo probado en el emulador (API 37).

## Hecho (24 h)
- 31 oct, tarde 2 (la persona: que todo el repo figure a su nombre, por una norma del hackatón): `main` ya era todo suyo (autor, committer y sin coautores); lo tenía `gh-pages`, la rama de la web, con dos commits que no estaban a su nombre. Rehecha en un solo commit a nombre de la persona con exactamente los mismos ficheros (forzado con lease) en vez de borrarla, que tumbaría la web; la web se volvió a publicar bien. Las ramas de las sesiones en la nube ya no existen.
- 31 oct, tarde (la persona: «mejora el README para el jurado, sigue revisando y limpia el repo»): README rehecho y comprobado en GitHub (insignias, la cifra del FBI IC3 2025 con su fuente, tres pilares con las ilustraciones de la web, criterios del jurado, diagrama mermaid vertical, cifras, seguridad, familia, probarlo). Cifras vueltas a medir: 91,2 % / 69,6 % / 4,1 % / 1,0 % en los 200 reservados; el equipo rojo, 98–100 % (ya entrenado con él). Limpieza: en git no sobra nada salvo dos imágenes que dejó de usar el README (quitadas); fuera de git, las tres APK viejas de `dist/`. Falta: el enlace al vídeo (hueco comentado en la cabecera) y la Release. Licencia MIT puesta (la persona: «Sí, MIT»).
- 31 oct, mediodía (la persona: «sigue revisando: bugs, seguridad, funciones»): dos fallos del agente con su prueba. Un código en palabras o en lista («siete tres cinco cero», «7, 3, 5 y 0») salía sin el sí de la familia; y una pregunta sin respuesta se quedaba abierta para siempre (Peri dejaba de responder a los toques, saludar y enseñar medallas, y un «sí» horas después llamaba). Revisados sin fallos: el servidor (enlaces, escapado, límites), los componentes exportados, los avisos y la validación de los enlaces de Peri a Peri. La APK de release, probada de cero en `Peri_Release`: bienvenida, permisos, tres SMS de timo (también un remitente «Hija» con ese contacto guardado) y cambios de idioma, sin fallos.
- 31 oct (la persona: «revisa mi proyecto» y «arregla todo»): el CI estaba rojo desde el 29 porque las huellas de `verification-metadata.xml` se sacaron con la caché llena (en core faltaba `kotlinx-coroutines-bom-1.6.4.pom`, en android el aapt2 de Linux): regeneradas con una caché vacía y el aapt2 de Linux y macOS a mano. En Windows la caché vacía tiene que ir en una ruta corta (`.agent-tmp/g`): con una larga, aapt2 no arranca («Daemon startup failed»).
- 31 oct: un remitente con nombre elegido por quien escribe (SMS con letras, nombre de un correo) ya no pasa por contacto (`IncomingMessage.nameFromContacts`); medido con `evalShield` antes y después, los 200 reservados igual.
- 31 oct: Rive commiteado con dos arreglos en `RiveArt.kt`: se carga al verse (no en segundo plano) y cada pantalla le da un ciclo de vida. La fuga se midió con la galería (`RiveDemoActivity`) y `dumpsys meminfo`, mismo proceso y ninguna galería abierta: +13 MB en 6 aperturas sin él, +1,5 MB fijos con él. Ojo: una primera medida salió plana sin el arreglo (el emulador iba sin memoria); medir siempre comprobando el PID.
- 31 oct: el vídeo (composiciones, scripts y guiones; los 1,4 GB de imágenes y audios fuera del repo por `.gitignore`) y la web en `main` en un solo commit, con la autoría del resto del repo. README con la web, la descarga y créditos de Rive; INSTALL con la Release.

## Cómo probar (solo builds de depuración)
- Voz: `adb shell "am broadcast -a app.friendlypear.debug.HEAR --es text 'call my daughter'"`.
- Gestos: `input swipe x y x y 1500` (cosquillas), `adb emu sensor set acceleration 28:0:9.8` alternando el signo (agitar); `-6:7.7:0.8` inclina. Estrellas: un toque en sus pies. `uiautomator dump` se atasca mientras Peri se mueve: mejor por coordenadas.
- Dos móviles: AVD `Peri_Family` (5556) es Mamá, 5554 es Ana. Emparejar con `POST /v1/pairing` (token de admin) y el enlace. Ajustes de prueba con `run-as` y `MSYS_NO_PATHCONV=1`.
- Llamada de un desconocido: `adb -s emulator-5556 emu gsm call 699000111` y `emu gsm accept 699000111`; antes `cmd role add-role-holder android.app.role.CALL_SCREENING app.friendlypear` y `pm grant app.friendlypear android.permission.ANSWER_PHONE_CALLS`.
- Notificaciones: la app de Ana en segundo plano (HOME) y un SMS de número nuevo a Mamá; llegan en un minuto.
- Lugar: `shared_prefs/world.xml` con `place` (SNOW, SEASIDE…). Hora: `adb shell service call alarm 3 s16 Pacific/Auckland` (noche), volver con `GMT`.
- Imagen compartida: copiarla con `run-as app.friendlypear` a `files/` y `am start -a android.intent.action.SEND -t image/png --eu android.intent.extra.STREAM file:///data/data/app.friendlypear/files/x.png -n app.friendlypear/.ui.peri.PictureActivity`; sin red: `svc wifi disable; svc data disable`.
- Familia de cuatro: AVDs `Peri_Papa` (5558) y `Peri_Pablo` (5560) además de los dos; `python eval/family4.py`. Email simulado: `am broadcast -a app.friendlypear.debug.HEAR --es email "remitente|texto"` (todo entre comillas en el shell del móvil).
- Backend: `cd backend; PUBLIC_BASE_URL=http://10.0.2.2:8711 bun src/main.ts`.
- Emuladores: `emulator -avd … -cores 2` (los cuatro a la vez sin latencia); la pantalla se apaga a los 5 min (Ctrl+P la enciende); para medir, `svc power stayon true` y `dumpsys gfxinfo app.friendlypear`.
- Equipo rojo: `cd backend; REDTEAM_DAY=<día> bun ../eval/redteam/redteam.ts --run`, `evalShield` del lote y `python eval/redteam/report.py eval/redteam/<día>`; un lote nuevo se juzga antes de leerlo (es la cifra honesta).
- Dos guardianes: Mamá (5556) tiene a Ana y Papá. Los SMS entre emuladores no llegan: el enlace está en `content://sms/sent` de Mamá y se entrega con `emu sms send 15555215556 "texto"`.

## En curso
- r4-peri-capas: la Peri por capas al inicio, a los estados, con la ropa pegada a la cara y en Mini Peri, cuando la persona la vea en la prueba.

## Bloqueado
- Nada.

## Peticiones
- SEG: `r2s-revision-nuevo` (lo de hoy: preguntas, colgar, notificaciones con acciones, bloqueados).
- La persona: publicar la APK firmada en una Release de GitHub (el README y INSTALL ya enlazan a la última).

## Propuestas para el roadmap
- Sección F (seguridad); regrabar los GIF del README con el diseño nuevo; las trampas que faltan (inversión, policía falsa, trabajo, voz clonada) cuando haya una señal que las distinga.
