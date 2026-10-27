# Estado TEST

## Hecho (24 h)
- prueba-20-20 (la hizo IMP-1 a petición de la persona): `eval/prueba-20-20/`, informe en `resultados/informe.md`.
  - 40 mensajes sintéticos (20 estafas, 20 normales; mitad en español, mitad en inglés). Preliminar: no es el conjunto reservado.
  - Reglas solas: 60 % de estafas como estafa, 100 % con aviso; 0 normales como estafa, 1 dudoso (gimnasio).
  - Muse Spark Open solo: 80 % como estafa; falla en 5 de 40 → sin pivote. Mediana 3,1 s. Coste 0,129 $ (Baseten).
  - Reglas + voto ±2: 95 % como estafa, 100 % con aviso, 0 % de normales con aviso.
  - El modelo duda justo donde las reglas son firmes (dominios que imitan marcas, código durante llamada): las capas se complementan.
  - Único fallo: `s16` ("reply with the code we just sent you", banco sin nombre) queda en dudoso.
- Cómo repetir: `cd core; ./gradlew evalShield --args="../eval/prueba-20-20/mensajes.tsv ../eval/prueba-20-20/resultados/reglas.tsv"` y `cd backend; bun ../eval/prueba-20-20/run.ts` (estima) / `--run` (40 peticiones) / `--report`.

## En curso
- Nada.

## Bloqueado
- reservado-200 y metricas: hacen falta mensajes reales anonimizados y con permiso (lo aporta la persona).

## Peticiones
- IMP-1: marcas del Reino Unido y EE. UU. en `Brands.ALL` (Royal Mail y HMRC no se reconocen como marca imitada).
- IMP-1: "responde con el código que te hemos enviado" debería pesar más (`s16`).

## Propuestas para el roadmap
- Repetir la prueba con los 200 reales cuando existan; nunca ajustar las reglas mirando el conjunto reservado.
