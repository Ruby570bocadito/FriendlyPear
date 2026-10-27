/**
 * Pairs a phone with this server, in person:
 *
 *   cd backend; bun pair
 *
 * Asks the running server for a one-time code (10 minutes), prints it with its QR in the terminal, and writes
 * data/pair.html: a page to show on the screen with the QR, the code, the steps and a countdown. On the phone:
 * point the camera at the QR (or, in FriendlyPear, Settings → "Connect this phone" and type the code). The phone
 * then gets its own key. The page is a local file with everything inside it: nothing is loaded from anywhere.
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { encodeQr, qrSvg, qrTerminal } from "./qr";

const port = Number(process.env.PORT ?? 8787);
const admin = process.env.ADMIN_TOKEN && process.env.ADMIN_TOKEN.length >= 32 ? process.env.ADMIN_TOKEN : process.env.DEVICE_TOKEN;
if (!admin) throw new Error("DEVICE_TOKEN (or ADMIN_TOKEN) is missing in backend/.env");

const res = await fetch(`http://127.0.0.1:${port}/v1/pairing`, { method: "POST", headers: { authorization: `Bearer ${admin}` } }).catch(() => null);
if (!res) throw new Error(`Is the server running on port ${port}? Start it with: bun src/main.ts`);
if (!res.ok) throw new Error(`The server said ${res.status}: ${await res.text()}`);
const { code, link, expiresAt } = (await res.json()) as { code: string; link: string; expiresAt: number };
const server = decodeURIComponent(new URL(link.replace("friendlypear://", "https://x/")).searchParams.get("server") ?? "");

const qr = encodeQr(link);
const periFile = fileURLToPath(new URL("../public/guardian.webp", import.meta.url));
const peri = existsSync(periFile) ? `data:image/webp;base64,${readFileSync(periFile).toString("base64")}` : "";
const page = render(qrSvg(qr, 9), code, server, expiresAt, peri);

const dir = resolve(process.env.DATA_DIR ?? "data");
mkdirSync(dir, { recursive: true });
const file = join(dir, "pair.html");
writeFileSync(file, page);

const until = new Date(expiresAt).toLocaleTimeString();
console.log(qrTerminal(qr));
console.log(`\n  Code: ${code}   (works once, until ${until})`);
console.log(`  Bigger QR to show on screen: ${file}\n`);

function render(svg: string, code: string, server: string, expiresAt: number, peri: string): string {
  const T = {
    en: {
      title: "Connect a phone to Peri",
      lead: "Each phone gets its own key for Peri's server, handed over in person. If a phone is lost, you disconnect just that one.",
      s1: "Open the phone's <b>camera</b> and point it at the code.",
      s2: "Tap <b>Open with FriendlyPear</b>.",
      s3: "Check the code matches, and tap <b>Connect</b>.",
      alt: "No camera? In FriendlyPear: <b>Settings → Connect this phone</b>, and type the code.",
      left: "Expires in",
      expired: "This code has expired. Run <code>bun pair</code> again for a new one.",
      once: "Works once, for one phone.",
      safe: "Only show it to the phone you're connecting: whoever uses it connects a phone to your family's Peri.",
      server: "Server",
    },
    es: {
      title: "Conectar un móvil con Peri",
      lead: "Cada móvil recibe su propia clave para el servidor de Peri, entregada en persona. Si se pierde un móvil, se desconecta solo ese.",
      s1: "Abre la <b>cámara</b> del móvil y apunta al código.",
      s2: "Toca <b>Abrir con FriendlyPear</b>.",
      s3: "Comprueba que el código coincide y toca <b>Conectar</b>.",
      alt: "¿Sin cámara? En FriendlyPear: <b>Ajustes → Conectar este móvil</b>, y escribe el código.",
      left: "Caduca en",
      expired: "Este código ha caducado. Vuelve a ejecutar <code>bun pair</code> para tener otro.",
      once: "Sirve una vez, para un móvil.",
      safe: "Enséñalo solo al móvil que estás conectando: quien lo use conecta un móvil al Peri de tu familia.",
      server: "Servidor",
    },
  };
  return `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex"><title>Connect a phone to Peri</title>
<style>
*{box-sizing:border-box}body{margin:0;font-family:system-ui,-apple-system,"Segoe UI",Roboto,sans-serif;color:#1f2a1a;background:linear-gradient(#fff8ec,#eaf4dc) fixed;min-height:100vh}
main{max-width:980px;margin:0 auto;padding:28px 20px 40px}
header{display:flex;align-items:center;gap:14px}header img{width:92px;height:92px;filter:drop-shadow(0 6px 8px rgba(0,0,0,.12))}
.eyebrow{margin:0;font-weight:600;color:#25541a;letter-spacing:.02em}h1{margin:2px 0 0;font-size:clamp(26px,4vw,38px);line-height:1.1}
.lead{font-size:19px;line-height:1.45;margin:14px 0 22px;max-width:700px}
.lang{margin-left:auto;display:flex;gap:6px}.lang button{font:inherit;font-weight:600;border:0;border-radius:999px;padding:8px 14px;cursor:pointer;background:#e1f0cf;color:#25541a}.lang button[aria-pressed=true]{background:#25541a;color:#fff}
.grid{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:22px;align-items:start}@media (max-width:760px){.grid{grid-template-columns:1fr}header .lang{display:none}}
.card{background:#fff;border-radius:26px;padding:22px;border:2px solid #e1f0cf;box-shadow:0 4px 14px rgba(37,84,26,.08)}
.qr{text-align:center;position:relative}.qr svg{display:block;width:100%;max-width:400px;height:auto;margin:0 auto;transition:filter .3s,opacity .3s}
.expired .qr svg{filter:blur(6px) grayscale(1);opacity:.35}
.code{font-size:clamp(34px,5vw,48px);font-weight:800;letter-spacing:.12em;color:#25541a;margin:14px 0 4px;font-variant-numeric:tabular-nums}
.timer{display:inline-flex;align-items:center;gap:8px;background:#e1f0cf;color:#25541a;font-weight:700;border-radius:999px;padding:8px 16px;font-size:18px}
.expired .timer{background:#fff1d6;color:#6b3e00}.dot{width:10px;height:10px;border-radius:50%;background:#7cc242}.expired .dot{background:#9c1f17}
ol{list-style:none;padding:0;margin:0;counter-reset:s}ol li{counter-increment:s;position:relative;padding:4px 0 18px 56px;font-size:19px;line-height:1.4}
ol li:before{content:counter(s);position:absolute;left:0;top:0;width:40px;height:40px;border-radius:50%;background:#25541a;color:#fff;font-weight:800;display:flex;align-items:center;justify-content:center}
.alt{background:#f6f9f1;border-radius:16px;padding:12px 14px;font-size:17px}
.note{margin-top:14px;font-size:16px;color:#3f4d39;line-height:1.45}.note b{color:#6b3e00}.server{font-size:15px;color:#3f4d39;margin-top:8px;word-break:break-all}
.gone{display:none;margin-top:12px;font-weight:600;color:#6b3e00}.expired .gone{display:block}
code{background:#f1efe8;border-radius:6px;padding:1px 6px}
</style></head><body><main id="main">
<header>${peri ? `<img src="${peri}" alt="">` : ""}<div><p class="eyebrow">FriendlyPear · Peri</p><h1 data-t="title"></h1></div>
<div class="lang"><button data-l="en">English</button><button data-l="es">Español</button></div></header>
<p class="lead" data-t="lead"></p>
<div class="grid">
<section class="card qr">${svg}<div class="code">${code}</div><div class="timer" role="timer"><span class="dot"></span><span data-t="left"></span> <span id="left">10:00</span></div>
<p class="gone" data-t="expired"></p></section>
<section class="card"><ol><li data-t="s1"></li><li data-t="s2"></li><li data-t="s3"></li></ol><p class="alt" data-t="alt"></p>
<p class="note"><b data-t="once"></b> <span data-t="safe"></span></p><p class="server"><span data-t="server"></span>: ${server}</p></section>
</div></main>
<script>
const T = ${JSON.stringify(T)}, end = ${expiresAt};
function lang(l) {
  document.documentElement.lang = l;
  document.querySelectorAll("[data-t]").forEach((e) => (e.innerHTML = T[l][e.dataset.t]));
  document.querySelectorAll("[data-l]").forEach((b) => b.setAttribute("aria-pressed", b.dataset.l === l));
  document.title = T[l].title;
}
document.querySelectorAll("[data-l]").forEach((b) => (b.onclick = () => lang(b.dataset.l)));
lang((navigator.language || "en").startsWith("es") ? "es" : "en");
function tick() {
  const s = Math.max(0, Math.round((end - Date.now()) / 1000));
  document.getElementById("left").textContent = Math.floor(s / 60) + ":" + String(s % 60).padStart(2, "0");
  if (s === 0) document.getElementById("main").classList.add("expired");
  else setTimeout(tick, 1000);
}
tick();
</script></body></html>`;
}
