# Estado IMP-1

## Hecho (24 h)
- escudo-capas: `core/.../shield/`
  - Señales: remitente fuera de la agenda, enlace, acortador, dominio que imita a una marca (también en el email del remitente), prisa, dinero, petición de código o clave (ignora "no lo compartas"), "número nuevo", premios o inversión, app de control remoto, instrucciones para la IA, código de un solo uso.
  - Reglas duras: código durante llamada y dominio que imita a una marca → estafa; el modelo no las anula.
  - Puntuación log-odds con `ShieldWeights.PROVISIONAL` (a mano); `withModelOpinion` limita el voto del modelo a ±2 y sustituye al anterior.
  - Acción segura y frase de Peri sin enlaces ni números (test).
  - 37 tests nuevos en `core/src/test/.../shield/`.

## En curso
- Nada.

## Bloqueado
- creditos-y-spark, glimmer-servido: falta la clave de Meta Model API (la persona reclama los 150 $).

## Peticiones
- TEST: pasar la prueba 20/20 también por `Shield` (capa 1 sola) para tener la fila de reglas.

## Propuestas para el roadmap
- pesos-aprendidos debe producir un `ShieldWeights` (mapa señal → peso) que `Shield` ya acepta.
- Ampliar `Brands.ALL` con dominios oficiales verificados de más bancos y empresas.
