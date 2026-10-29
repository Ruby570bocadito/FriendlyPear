# Guion de la demo (3:00)

Decidido con la persona el 29 oct: **horizontal 16:9** (1920×1080, 60 fps), **Carmen de plastilina**, **voz sintética
para el borrador y la de la persona al final** (el mismo texto), y **solo en inglés**, con subtítulos en inglés.
Sustituye la tabla de `HISTORIA.md` y de `VIDEO.md` §3.

**La regla de oro:** cada momento de la app es una grabación real, con el rótulo *Real app · recorded on Android*.
El mundo de plastilina cuenta la historia; nada generado hace como que la app hace algo que no hace.

**Cómo se ve:** el fondo apaisado del lugar a toda pantalla, Carmen y Peri en él, y cuando habla la app, el móvil con
la grabación entra por la derecha (un tercio del ancho) mientras la escena sigue a la izquierda.

| # | Tiempo | Lugar y personajes | La app de verdad | Voz (inglés) |
|---|---|---|---|---|
| 1 | 0:00–0:14 | Pueblo al atardecer (`village_sunset_wide`); Carmen llega andando (`carmen_walk_01–04`), Peri saluda (`story_wave_01–03`) | — | This is Carmen. Every week, someone writes to her pretending to be her bank, her daughter or the post office. And this is Peri: a pear who moved into her phone to look after her. |
| 2 | 0:14–0:44 | Salón de día (`living_day_wide`); llega el SMS (`ScamText`), Peri sale del móvil con el paraguas (`story_phone_out_01–04`) | El aviso, *"Shall I call Ana on her usual number?"*, el sí y la llamada | A new number asking for money? Peri warns her before she answers, and offers to call her daughter on the number she already trusts. One call, and the scam is over. |
| 3 | 0:44–1:04 | Salón al atardecer (`living_evening_wide`); suena el móvil (`phone_ring_01–02`), Carmen contesta (`carmen_answer_01–02`) y cuelga (`carmen_hangup`) | Aviso antes de descolgar; llega un código en plena llamada: *"Hang up"* | When a stranger calls pretending to be the bank, Peri says so before she picks up. And if a code arrives during the call, Peri knows: no bank ever asks for it. Hang up. |
| 4 | 1:04–1:24 | Calle con parquímetro (`street_parking_wide`); el QR pegado (`qr_peel_01–02`), Peri de detective (`story_detective_01–02`) | «Mira esta imagen»: la trampa del QR rodeada | A sticker on a parking meter? Carmen shows Peri a picture. Peri reads the QR code on the phone without opening it, and Muse Spark with SAM 3.1 rings what looks wrong. |
| 5 | 1:24–1:52 | Casa de Ana (`ana_home_wide`); Ana corre (`ana_run`), alivio (`ana_relief`) | Dos móviles: la petición urgente y *"Call Mamá now"* | When something is risky, the family decides together. Ana's Peri gets an urgent request: a stranger just called her mother. One tap, and she's calling Mum. |
| 6 | 1:52–2:06 | Cocina (`kitchen_wide`) | «Me he caído»: la cuenta atrás y la llamada a Ana | And when Carmen needs help, she only has to say so. |
| 7 | 2:06–2:40 | El taller de Peri (`peri_workshop_wide`); rótulos que entran uno a uno | La revisión del móvil y una medalla, de pasada | Most of Peri lives on the phone: rules that flag 91 percent of scams they had never seen, with 4 percent false alarms. Only doubtful messages go to Meta's Muse Spark models for a second opinion. Muse Image drew every picture of Peri, and the family never reads Carmen's messages. |
| 8 | 2:40–3:00 | Atardecer (`village_sunset_wide`); Carmen abraza a Ana (`carmen_hug_ana`) y las peras de la familia (`end_pears_01–04`); logo | — | FriendlyPear. A friend in the phone, so nobody is ever alone with a scammer. |

**Rótulos del taller (escena 7):** *On the phone: rules, 91 % caught · 4 % false alarms (200 unseen messages)* ·
*Muse Spark Open: second opinion* · *Muse Spark + tools: the investigator* · *SAM 3.1: what's wrong in a picture* ·
*Muse Voice Transcribe: Peri's ears* · *Muse Image: every picture of Peri* · *Open source*.

**Voz:** unas 240 palabras, ~100 s de voz; el resto, la app y la música. El borrador con una voz de Windows
(`System.Speech`, sin descargar nada); la persona graba después el mismo texto y se cambia la pista.

**Música:** libre de derechos; hay que descargarla, así que antes se pregunta cuál.

## Las grabaciones (r5-grabaciones)

Con un solo emulador encendido (los demás en pausa), a 864×1920, la app en inglés:

1. **Número nuevo** (escena 2): Ana guardada como hija en los contactos de Carmen; el SMS por `adb emu sms send`; el sí por la orden de depuración `HEAR`.
2. **El falso banco** (escena 3): `adb emu gsm call`, contestar, y un SMS con código en plena llamada.
3. **La foto del parquímetro** (escena 4): una foto con un QR trampa compartida con Peri.
4. **La familia** (escena 5): los dos emuladores (Carmen y Ana) con el servidor, uno detrás de otro y sincronizados en el montaje.
5. **«Me he caído»** (escena 6): la orden por `HEAR`, la cuenta atrás y la llamada.
6. **De pasada** (escena 7): la revisión del móvil y la medalla de cinco escudos.
