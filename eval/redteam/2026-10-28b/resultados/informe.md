# Red team, 2026-10-28b

123 scams written by Muse Spark to slip past the shield, judged by its rules alone (no model, no contacts). "Warned" counts a doubt or a scam warning; "firm" only the scam warning. "Before" is the previous day's rules; the others are the rules as they are now. The honest number for a batch is the one taken before its misses were read: see eval/REPORT.md.

| Kind | Messages | Warned before | Warned | Firm | Got past |
|---|---:|---:|---:|---:|---:|
| banco_acceso | 5 | 1 (20%) | 5 (100%) | 2 (40%) | 0 |
| banco_codigo | 9 | 3 (33%) | 9 (100%) | 1 (11%) | 0 |
| compra_falsa | 10 | 4 (40%) | 10 (100%) | 7 (70%) | 0 |
| familiar_numero_nuevo | 10 | 8 (80%) | 10 (100%) | 6 (60%) | 0 |
| familiar_urgencia | 6 | 5 (83%) | 6 (100%) | 4 (67%) | 0 |
| hacienda | 8 | 3 (38%) | 8 (100%) | 3 (38%) | 0 |
| inversion | 10 | 5 (50%) | 10 (100%) | 3 (30%) | 0 |
| paqueteria | 10 | 3 (30%) | 8 (80%) | 1 (10%) | 2 |
| premio | 5 | 3 (60%) | 5 (100%) | 3 (60%) | 0 |
| romance | 9 | 4 (44%) | 9 (100%) | 4 (44%) | 0 |
| seguridad_social | 7 | 3 (43%) | 7 (100%) | 3 (43%) | 0 |
| soporte_tecnico | 8 | 3 (38%) | 8 (100%) | 4 (50%) | 0 |
| suministros | 9 | 2 (22%) | 9 (100%) | 6 (67%) | 0 |
| suscripcion | 7 | 2 (29%) | 7 (100%) | 4 (57%) | 0 |
| trafico | 10 | 1 (10%) | 10 (100%) | 3 (30%) | 0 |
| **All** | 123 | 50 (41%) | 121 (98%) | 54 (44%) | 2 |

## Still getting past (2)

Senders are described and numbers, addresses and links masked: made-up ones may belong to someone real.

- `paqueteria-en-1` WHATSAPP from a UK number, signals: UNKNOWN_SENDER,CALLBACK_REQUEST. "hiya its Jess, your parcel couldnt fit through the letterbox so its back with me, message me here and il help you book redelivery tomorrow"
- `paqueteria-en-3` SMS from "EVRI", signals: UNKNOWN_SENDER. "Morning 😊 sorry we missed you yesterday, your box is at the local shop, pls call [number] to arrange a time that suits you"
