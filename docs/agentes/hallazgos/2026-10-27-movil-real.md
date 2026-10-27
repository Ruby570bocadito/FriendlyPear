# Primera prueba en un móvil real (OPPO Reno14 F 5G, ColorOS) — 27 oct

Lo que contó la persona al instalar el APK de depuración en su móvil, y qué se hizo.

| Qué pasó | Causa | Arreglo |
|---|---|---|
| La app se cerraba al abrirla | Instalación nueva: la tarjeta de pasos mostraba todos los pasos y Peri se quedaba sin alto; la sombra con radio 0 lanzaba una excepción | Sombra solo con espacio; un paso cada vez (el más importante primero). Probado: instalación limpia negando todo, sin cierres |
| Texto gigante y Peri enana; "FriendlyP ear" en dos líneas | El móvil tiene el texto grande en Accesibilidad, y los tamaños de la app ya empiezan grandes | El texto crece con el ajuste del móvil hasta 1,15×; el bocadillo cabe en ~¼ de pantalla (encoge la letra si hace falta); el nombre encoge o deja paso a la cara de Peri; los botones encogen su texto en vez de cortarlo. Probado con letra 1,5× y densidad 480 |
| Peri hablaba mucho al principio | Cada explicación de permiso se leía en voz alta | Las explicaciones se escriben en el bocadillo sin voz; el saludo se dice solo la primera vez del día |
| La voz sonaba robótica | Motor de voz del fabricante | Se usa el motor de Google si está instalado, con su mejor voz sin conexión y tono natural |
| Mini Peri no movía la boca | Probablemente el motor del fabricante avisaba "terminado" antes de tiempo (en el emulador sí la mueve) | La boca sigue también el inicio real de cada frase (`onStart`) |
| Configurar la familia era difícil con muchos contactos | Se listaban todos los contactos con todos sus controles | Buscador; sin buscar, solo la familia probable (guardianes, con relación o apodos, o guardados como "Laura hija"); filas creadas al mostrarse, 20 resultados como mucho |
| "Dale personalidad" | — | Saludo según la hora con variaciones; al tocar a Peri salta, se ríe y da un consejito contra timos (8 distintos) |
| "Aplicación dañina bloqueada por Google Play Protect" y SMS bloqueado | Android y Play Protect desconfían de las apps instaladas fuera de una tienda que leen SMS y notificaciones (es lo que hacen también las apps espía) | No se arregla desde el código. Instalar con "Más detalles → Instalar de todas formas" y permitir los ajustes restringidos (Ajustes → Aplicaciones → FriendlyPear → ⋮ → "Permitir ajustes restringidos"); Peri lo explica cuando Android lo bloquea. Publicar en Google Play lo evitaría |

## Segunda ronda (APK descargado otra vez)

| Qué pasó | Causa | Arreglo |
|---|---|---|
| El acceso a SMS seguía bloqueado y "me inhabilita mucho" | Ajustes restringidos de Android 13+ para apps instaladas desde un archivo | Los SMS son opcionales: Peri protege con contactos, llamadas y notificaciones. El paso de SMS (y el del banco) se puede ocultar con "×"; el chip dice "Protegiéndote" sin SMS |
| La voz seguía robótica | Cada móvil trae varias voces y la elegida no tiene por qué ser la mejor | Ajustes → "La voz de Peri": todas las voces del idioma, se oye una frase al tocar y se recuerda; botón a los ajustes de voz de Android (o al motor de Google en Play Store). En el emulador: 8 voces, la elegida sigue marcada al volver |
| Mini Peri "muy descuidada" | Aparecía de golpe, saltaba al borde, solo se quitaba desde la notificación | Sale con un bote; se desliza al lado más cercano; al arrastrarla aparece una "×" abajo (se pone roja, vibra y Peri encoge encima) y al soltarla se guarda; anillo verde que late mientras escucha; el bocadillo entra con suavidad y encoge la letra si la frase es larga |
| "Más sprites" | — | 3 poses nuevas con Muse Image (idea al tocarla, perdón si no entendió, pulgar arriba al salir de una app de riesgo), 0,03 $ |
| "8 modelos de pera nueva" | — | `assets/peri/concepts.ts`: barro, acuarela, pegatina, abuela, heroína, geométrica, peluche y anime (0,08 $), en `assets/peri/concepts/peri_concepts.webp` |

Fallos encontrados probando, antes de subir: la etiqueta de la "×" salía cortada ("Drop"); la "×" se pintaba encima de Peri; soltar a Peri sobre la "×" no la guardaba (se olvidaba que estaba encima antes de mirarlo). Arreglados y comprobados en el emulador.

## Peri en plastilina

La persona eligió el diseño de plastilina 3D. Se rehizo toda Peri con ese diseño como base: 9 estados, 7 fotogramas por estado y 9 extras (82 imágenes, 0,82 $). Al revisarlas: "preocupada" hablando salía con una sonrisa (se rehízo con un prompt que mantiene el ceño) y el recorte dejaba el suelo de "preocupada" (cada borde se compara con su propio color y se quita el suelo beige del quinto inferior). Probado en el emulador: app, idea al tocarla, Mini Peri e icono.
