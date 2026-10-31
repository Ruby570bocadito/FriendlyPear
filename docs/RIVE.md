# Rive en FriendlyPear

Dieciséis animaciones vectoriales de [Rive](https://rive.app) para los momentos de la app que cambian de estado. Cada una
es una máquina de estados con **una sola entrada numérica**: la app le dice el número y la animación pasa sola de un
estado a otro, con su transición (la celebración, el lápiz, el destello y la flecha de "Siguiente" no tienen entrada: se reproducen al aparecer). Peri no se toca: sigue siendo la de plastilina.

## Dónde sale cada una

| Archivo | Máquina / entrada | Valores | Pantalla |
|---|---|---|---|
| `rive_shield.riv` | `Shield` / `level` | 0 vigilando (respira), 1 peligro (rojo, "!", tiembla), 2 a salvo (verde, el check se dibuja, chispas) | La pausa de seguridad (1, al lado del título) y "Cómo era la trampa" (2, en la cabecera) |
| `rive_stars.riv` | `Stars` / `score` | 0 a 6: cuántas estrellas se encienden, una tras otra | El final de "¿Timo o no?" (solo en rondas de 6; si no, las estrellas quietas de siempre) |
| `rive_scan.riv` | `Scan` / `state` | 0 nada, 1 mirando (la lupa da vueltas), 2 bien (check verde), 3 duda (ámbar, "…"), 4 trampa (rojo, "!") | "Mira esta imagen": la lupa mientras miran Muse Spark y SAM 3.1, luego la insignia del veredicto 2,2 s y se va |
| `rive_ring.riv` | `Ring` / `percent` | 0, 10 … 100: el anillo se llena; coral si falta algo, verde con chispas al 100 | La cabecera de "Revisión de seguridad", alrededor de Peri |
| `rive_pulse.riv` | `Pulse` / `on` | 1 protegiendo (punto verde que lanza ondas de sonar), 0 falta configurar (punto ámbar que parpadea despacio) | El chip "Te estoy cuidando" de la pantalla principal |
| `rive_burst.riv` | `Burst` / sin entrada | Rayos de sol con degradado que giran despacio y una explosión de confeti al empezar (dos capas a la vez) | Detrás de Peri en "¡Nueva insignia!" y detrás del trofeo en una ronda de 5 o 6 aciertos |
| `rive_write.riv` | `Write` / sin entrada | Un lápiz escribe línea tras línea a mano; se borran y vuelve a empezar | "Denúnciala" mientras Muse Spark escribe la denuncia (en lugar de las barras grises) |
| `rive_tab_peri.riv`, `_diary`, `_learn`, `_family` | `Tab` / `on` | 1 elegida (su color y su momento), 0 no (gris). La pera se menea con chispas, el libro se abre, la bombilla se enciende con rayos, los dos saltan y sale un corazón | La barra de pestañas de abajo |
| `rive_voice.riv` | `Voice` / `on` | 1 escuchando (el micrófono da paso a cinco barras de sonido que botan), 0 el micrófono | El botón "Hablar con Peri", en blanco sobre el verde |
| `rive_glint.riv` | `Glint` / sin entrada | El escudo asiente y le brilla una estrella en la esquina cada cuatro segundos | El chip de escudos de la pantalla principal |
| `rive_link.riv` | `Link` / `state` | 0 esperando, 1 conectando (los puntos corren del móvil al escudo), 2 conectado (la línea se pone verde, el escudo se ilumina, suben corazones), 3 falló (la línea se rompe en ámbar con una cruz) | "Conectar este móvil", en la tarjeta del código |
| `rive_send.riv` | `Send` / `sent` | Vuela a la derecha y vuelve por la izquierda cada vez que el número pasa de 1 a 2 o al revés; 0 en reposo | El botón de enviar del chat |
| `rive_next.riv` | `Next` / sin entrada | La flecha empuja hacia delante cada dos segundos | El botón "Siguiente" de la bienvenida |

Todo pasa por [`RiveArt.kt`](../android/app/src/main/java/app/friendlypear/ui/peri/RiveArt.kt): `rive(RiveArt.SHIELD, 1f)` da la vista, o `null`
con "quitar animaciones" activado, y entonces la pantalla deja lo que tenía. Son decoración: el texto de al lado dice
lo mismo, así que los lectores de pantalla las saltan. Y no se quedan los toques: la vista de Rive los guarda para sus
propios oyentes, y las nuestras no tienen, así que el toque llega al botón, la pestaña o la fila de debajo.

Dos cosas más que hace `RiveArt.kt` (31 oct):

- **Rive se carga la primera vez que una pantalla enseña una animación**, no al arrancar la app: cuando un SMS o una
  llamada despiertan a Peri en segundo plano no se dibuja nada, y su librería nativa (unos 5 MB) no se carga. Si no
  puede cargarse (un móvil cuya arquitectura no lleva la APK), `rive()` da `null` y la pantalla deja su dibujo quieto.
- **Cada pantalla le da a Rive un ciclo de vida que acaba con ella.** Rive solo devuelve la memoria nativa de una
  animación (su archivo, su artboard y su máquina de estados) cuando acaba el ciclo de vida que encuentra en la vista,
  y nuestras `Activity` sencillas y las ventanas sobre otras apps (la pausa) no tienen ninguno. Medido con la galería
  en el emulador, mismo proceso y ninguna galería abierta: sin él, abrirla y cerrarla 6 veces dejaba 13 MB más de
  memoria nativa (unos 140 KB por animación); con él, 1,5 MB tras 14 veces (una caché que no crece).

## Cómo se hacen

Los `.riv` no salen del editor de Rive: los escribe código, en [`assets/rive`](../assets/rive).

```bash
python assets/rive/build.py
```

- `shield.py`, `stars.py`, `scan.py`, `ring.py`, `pulse.py`, `burst.py`, `write.py`: una animación cada uno (formas, colores, degradados, fotogramas y máquina de estados, con una o varias capas).
- `link.py`: el móvil y el escudo de emparejar.
- `tabs.py` e `icons.py`: los cuatro iconos de la barra, el micrófono, el escudo del chip y las flechas, dibujados a partir de los iconos vectoriales de la propia app (`res/drawable/ic_pear.xml`, `ic_mic.xml`, `ic_shield.xml`, `ic_chevron.xml`, etc.): si cambia un icono, se regenera igual. `rivwriter.py` lee el `pathData` de Android y de SVG (M L H V C S Z).
- `rivwriter.py`: escribe el formato de ejecución de Rive (versión 7) a partir de `schema.json`.
- `schema.json`: las claves de tipos y propiedades, sacadas de las cabeceras generadas de `rive-runtime` (licencia MIT) con `schema.py`.
- `rivdump.py`: imprime un `.riv` como objetos, para revisar uno (`python assets/rive/rivdump.py assets/rive/schema.json archivo.riv`).

Lo que va antes en el archivo se dibuja encima. Una transición con tiempo de salida (`exit ms`) espera a que la animación
llegue hasta ahí, como cuando una de una sola vez da paso a un bucle. Para cambiar una animación, se cambia su script y se vuelve a ejecutar
`build.py`. Si algún día se rehacen en el editor de Rive, basta con mantener el nombre de la máquina, el de la entrada y
sus valores: el código de la app no cambia.

## Para verlas sin pasar por las pantallas

En las versiones debug hay una galería con un botón por cada valor:

```bash
adb shell am start -n app.friendlypear/.ui.peri.RiveDemoActivity
```

## Lo que cuesta

- `app.rive:rive-android:10.4.0` (MIT). Es la última que compila con `compileSdk 35`; la 11.x pide 36.
- La APK de release pasa de unos 26 MB a 42,6 MB: la librería nativa de Rive (unos 5 MB por arquitectura) y lo que
  arrastra (Compose, lifecycle, corrutinas). El release lleva ARM de 64 y de 32 bits (los móviles Android Go van en
  32) y x86_64 (los emuladores de hoy); el x86 de 32 bits, que no usa ningún móvil con Android 12, se queda fuera
  (eran 48 MB con él). Con un App Bundle cada móvil se bajaría solo la suya.
- `verification-metadata.xml` añade las sumas de las dependencias nuevas.
- Mientras el inicio está abierto, Rive dibuja en su propio hilo (el radar del chip y el brillo de los escudos están
  siempre en marcha): en el emulador se vio su hilo cerca del 15 % de un núcleo (una sola medida, con el emulador
  cargado; falta medirlo en un móvil de verdad).
