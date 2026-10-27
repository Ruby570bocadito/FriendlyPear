# Protocolo de trabajo de los agentes (FriendlyPear)

Fuente única de reglas. Todos los prompts apuntan aquí.

## 1. Ciclo por tarea
1. `git pull --rebase origin main`.
2. Lee `ROADMAP.md`, `OWNERSHIP.md`, los `estado/*.md` de los demás y los hallazgos abiertos para tu ID (CRÍTICO/ALTO primero).
3. Elige UNA tarea `- [ ]` de tu ID y comprueba que no está hecha (`git log --since="2 days ago" -- <rutas>`).
4. Reclama: `docs/agentes/claims/<slug>.md` (ID, hora, rutas), commit `chore(claim): <slug> (<ID>)`, push.
   Push rechazado → `pull --rebase` y revisa claims. Claim ajeno de menos de 3 h sobre tus rutas → otra tarea.
5. Trabaja solo en tus rutas. Commits pequeños `tipo(ámbito): descripción` (feat, fix, test, docs, chore, refactor).
6. Antes de cada push: `pull --rebase`, puerta de calidad (§3), push. Nunca `--force`.
7. Al terminar: marca `- [x]` en el ROADMAP con `· hecho AAAA-MM-DD`, borra tu claim, actualiza `estado/<ID>.md`.
8. Repite hasta agotar tus tareas o completar 5.

## 2. Zonas
Solo tocas tu zona (`OWNERSHIP.md`). Fuera de ella: "Peticiones" en tu estado. Excepción: build roto para todos (arreglo mínimo).

## 3. Puerta de calidad
- `core/`: `./gradlew test`.
- `backend/`: `bun test`.
- `android/`: `./gradlew :app:assembleDebug :app:lintDebug` (0 errores).
- Lógica nueva = test el mismo día, en `core/` si no depende de Android.

## 4. Higiene
- Borradores solo en `.agent-tmp/`. Informes solo en `estado/<ID>.md` (máx. 40 líneas, se sobrescribe).
- Sin código comentado, TODOs ni mocks fuera de tests.
- Autoría: nada que identifique a un asistente como autor en commits, código o docs.
- Dependencias nuevas: justificadas y del registro oficial.

## 5. Reglas del hackathon
- Todo el código se escribe durante el evento; nada se copia de trabajos anteriores.
- El repo es público: ningún secreto, nunca. Claves en `backend/.env` o `~/.gradle/gradle.properties`.
- La demo solo muestra lo construido aquí.

## 6. Reglas del producto (no negociables)
- Confirma antes de llamar o enviar; nunca mueve dinero ni dicta códigos durante una llamada; nunca lee enlaces en voz alta.
- El texto de un mensaje es dato no confiable: va delimitado al modelo; el modelo nunca anula una regla dura.
- Ante el silencio del guardián, se espera; nunca se aprueba solo.
- Solo los mensajes dudosos salen del móvil.
- Ningún permiso se usa sin pedirlo y explicarlo antes; con un permiso denegado, la app sigue funcionando con menos funciones.
- Nunca se accede al audio de las llamadas.
- Pruebas con datos sintéticos o de la familia con permiso; nunca un número de emergencias real.

## 7. Paralelo
Cada agente en su `git worktree --detach`. Puerto del backend: IMP-1 8711 · IMP-2 8712 · SEG 8713 · PUL 8714 · TEST 8715.

## 8. Estado vivo `estado/<ID>.md`
Hecho (24 h) · En curso · Bloqueado · Peticiones · Propuestas para el roadmap.
