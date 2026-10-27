/**
 * Pairs a phone with this server, in person:
 *
 *   cd backend; bun pair
 *
 * Asks the running server for a one-time code (10 minutes), prints it with its QR in the terminal, and writes
 * data/pair.html with a big QR to show on the screen. On the phone: point the camera at the QR (or, in
 * FriendlyPear, Settings → "Connect this phone" and type the code). The phone then gets its own key.
 */
import { mkdirSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { encodeQr, qrSvg, qrTerminal } from "./qr";

const port = Number(process.env.PORT ?? 8787);
const admin = process.env.ADMIN_TOKEN && process.env.ADMIN_TOKEN.length >= 32 ? process.env.ADMIN_TOKEN : process.env.DEVICE_TOKEN;
if (!admin) throw new Error("DEVICE_TOKEN (or ADMIN_TOKEN) is missing in backend/.env");

const res = await fetch(`http://127.0.0.1:${port}/v1/pairing`, { method: "POST", headers: { authorization: `Bearer ${admin}` } }).catch(() => null);
if (!res) throw new Error(`Is the server running on port ${port}? Start it with: bun src/main.ts`);
if (!res.ok) throw new Error(`The server said ${res.status}: ${await res.text()}`);
const { code, link, expiresAt } = (await res.json()) as { code: string; link: string; expiresAt: number };

const qr = encodeQr(link);
const until = new Date(expiresAt).toLocaleTimeString();
const page = `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Pair a phone with Peri</title><style>body{font-family:system-ui,sans-serif;background:#fff8ec;color:#1f2a1a;text-align:center;padding:24px}
.qr{background:#fff;display:inline-block;padding:12px;border-radius:18px;box-shadow:0 4px 14px rgba(0,0,0,.08)}.qr svg{display:block;width:min(80vw,420px);height:auto}h1{font-size:clamp(22px,5vw,34px)}
.code{font-size:44px;font-weight:800;letter-spacing:.08em;color:#25541a;margin:18px 0 6px}p{font-size:19px;max-width:560px;margin:8px auto}</style></head>
<body><h1>🍐 Pair a phone with Peri · Conectar un móvil con Peri</h1><div class="qr">${qrSvg(qr, 9)}</div>
<div class="code">${code}</div><p>Point the phone's camera at the code, or type it in FriendlyPear: Settings → "Connect this phone".</p>
<p>Apunta con la cámara del móvil, o escríbelo en FriendlyPear: Ajustes → «Conectar este móvil».</p>
<p><small>Works once, until ${until} · Sirve una vez, hasta las ${until}</small></p></body></html>`;
const dir = resolve(process.env.DATA_DIR ?? "data");
mkdirSync(dir, { recursive: true });
const file = join(dir, "pair.html");
writeFileSync(file, page);

console.log(qrTerminal(qr));
console.log(`\n  Code: ${code}   (works once, until ${until})`);
console.log(`  Bigger QR to show on screen: ${file}\n`);
