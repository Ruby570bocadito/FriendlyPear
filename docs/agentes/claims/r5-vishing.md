# Claim r5-vishing, r5-mini-viva, r5-release y r5-accesible (IMP-2)

- Hora: 2026-10-29 09:30
- Tareas: r5-vishing (la pregunta al colgar según lo que duró la llamada), r5-mini-viva (Mini Peri como cabeza con su cara), r5-release (APK de release firmado y minificado, sin token), r5-accesible (letra al 200 % y TalkBack en lo nuevo)
- Rutas: core/src/main/kotlin/app/friendlypear/core/agent/ (PeriAgent, Phrases), core/src/test/, assets/peri/sprites_cut.py, android/app/build.gradle.kts, android/app/proguard-rules.pro, android/app/src/main/java/app/friendlypear/ui/peri/ (MiniHead, MiniHeadData, MiniPeriService, PeriActivity, LayersActivity, Style), android/app/src/main/res/drawable-nodpi/peri_mini_*
- No toca: assets/peri/sprites/ (agente de imágenes)
