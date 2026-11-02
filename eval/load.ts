/**
 * Load and abuse tests for the backend, on its own instance: a free port, a throwaway data folder and key, no model
 * keys (so nothing spends credits) and TRUST_PROXY=1, so X-Forwarded-For can stand in for many phones. Prints what
 * each test expects and what it got, and fails if the server ever answers 5xx, dies, or lets a limit through.
 *
 *     bun eval/load.ts
 */
import { mkdtempSync, readFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { randomBytes } from "node:crypto";

const PORT = 8799;
const BASE = `http://127.0.0.1:${PORT}`;
const backend = join(import.meta.dir, "..", "backend");
const data = mkdtempSync(join(tmpdir(), "fp-load-"));
const admin = randomBytes(32).toString("hex");

// From the throwaway folder, so Bun doesn't load backend/.env with the real keys.
const env = { PATH: process.env.PATH!, SYSTEMROOT: process.env.SYSTEMROOT ?? "", PORT: String(PORT), DEVICE_TOKEN: admin, DATA_DIR: data, DATA_KEY: randomBytes(32).toString("base64"), TRUST_PROXY: "1", PUBLIC_BASE_URL: BASE };
const start = () => Bun.spawn(["bun", join(backend, "src", "main.ts")], { cwd: data, env, stdout: "pipe", stderr: "pipe" });
let server = start();

let failures = 0;
const check = (name: string, ok: boolean, detail: string) => {
  console.log(`${ok ? "ok  " : "FAIL"} ${name}: ${detail}`);
  if (!ok) failures++;
};

async function up() {
  for (let i = 0; i < 100; i++) {
    try {
      await fetch(`${BASE}/`);
      return;
    } catch {
      await Bun.sleep(100);
    }
  }
  throw new Error("server didn't start");
}

type Shot = { status: number; ms: number };
async function shoot(n: number, concurrency: number, make: (i: number) => [string, RequestInit]): Promise<Shot[]> {
  const out: Shot[] = [];
  let next = 0;
  const worker = async () => {
    while (next < n) {
      const i = next++;
      const [path, init] = make(i);
      const t = performance.now();
      try {
        const r = await fetch(BASE + path, init);
        await r.arrayBuffer();
        out.push({ status: r.status, ms: performance.now() - t });
      } catch {
        out.push({ status: 0, ms: performance.now() - t });
      }
    }
  };
  await Promise.all(Array.from({ length: concurrency }, worker));
  return out;
}

const counts = (s: Shot[]) => Object.entries(s.reduce<Record<number, number>>((m, x) => ((m[x.status] = (m[x.status] ?? 0) + 1), m), {})).map(([k, v]) => `${k}×${v}`).join(" ");
const pct = (s: Shot[], p: number) => s.map((x) => x.ms).sort((a, b) => a - b)[Math.floor((s.length - 1) * p)].toFixed(1);
const broken = (s: Shot[]) => s.filter((x) => x.status === 0 || x.status >= 500).length;
const ip = (i: number) => `10.${(i >> 16) & 255}.${(i >> 8) & 255}.${i & 255}`;
const rss = () => {
  try {
    // Windows has no /proc: ask for the working set of the server's process.
    const out = Bun.spawnSync(["powershell", "-NoProfile", "-Command", `(Get-Process -Id ${server.pid}).WorkingSet64`]).stdout.toString().trim();
    return Math.round(Number(out) / 1048576);
  } catch {
    return -1;
  }
};

// The limits live in memory: a restart gives the next tests fresh quotas.
async function restart() {
  server.kill();
  await server.exited;
  server = start();
  await up();
}

await up();
const auth = (key: string) => ({ authorization: `Bearer ${key}` });

// A family of 20 phones (the most one server pairs), each with its own key.
const keys: string[] = [];
for (let i = 0; i < 20; i++) {
  const { code } = await (await fetch(`${BASE}/v1/pairing`, { method: "POST", headers: auth(admin) })).json();
  const r = await fetch(`${BASE}/v1/devices`, { method: "POST", headers: { "x-forwarded-for": ip(9000 + i), "content-type": "application/json" }, body: JSON.stringify({ code, name: `Phone ${i}` }) });
  keys.push((await r.json()).key);
}
check("pair 20 phones", keys.every(Boolean), `${keys.filter(Boolean).length} keys`);
const extra = await (await fetch(`${BASE}/v1/pairing`, { method: "POST", headers: auth(admin) })).json();
const r21 = await fetch(`${BASE}/v1/devices`, { method: "POST", headers: { "x-forwarded-for": ip(9100), "content-type": "application/json" }, body: JSON.stringify({ code: extra.code, name: "One too many" }) });
check("a 21st phone is refused", r21.status === 403, `status ${r21.status}`);

// 1. The family's normal traffic, much faster than real: 20 phones writing their pear and reading everyone's.
let t = performance.now();
const pear = (i: number) => JSON.stringify({ name: `Pear ${i % 20}`, outfit: "CROWN,,BOWTIE", role: "protected", state: i % 3 ? "calm" : "attention", kind: i % 3 ? "" : "parcel" });
let s = await shoot(4000, 100, (i) => {
  const k = Math.floor(i / 2) % 20;
  const headers = { ...auth(keys[k]), "x-forwarded-for": ip(1000 + k), "content-type": "application/json" };
  return i % 2 ? [`/v1/pears`, { headers }] : [`/v1/pears/self`, { method: "PUT", headers, body: pear(i) }];
});
let secs = (performance.now() - t) / 1000;
check("family traffic, 4000 requests ×100 at once", broken(s) === 0, `${(s.length / secs).toFixed(0)} req/s, p50 ${pct(s, 0.5)} ms, p99 ${pct(s, 0.99)} ms, ${counts(s)}`);
check("per-phone limit holds (120/min)", s.filter((x) => x.status === 429).length > 0 && s.filter((x) => x.status === 200).length <= 20 * 120, `${s.filter((x) => x.status === 200).length} answered`);

await restart();

// 2. One IP hammering the API: 240 a minute, then 429.
s = await shoot(400, 50, () => [`/v1/pears`, { headers: { ...auth(keys[0]), "x-forwarded-for": "203.0.113.7" } }]);
check("one IP: the rest wait (240/min)", s.filter((x) => x.status !== 429).length <= 240 && broken(s) === 0, counts(s));

// 3. Guessing the token: 10 wrong ones, then even the right one waits.
s = await shoot(30, 10, () => [`/v1/pears`, { headers: { authorization: `Bearer ${randomBytes(32).toString("hex")}`, "x-forwarded-for": "198.51.100.9" } }]);
const right = await fetch(`${BASE}/v1/pears`, { headers: { ...auth(keys[1]), "x-forwarded-for": "198.51.100.9" } });
check("token guessing is locked out", s.filter((x) => x.status === 401).length <= 10 && right.status === 429, `${counts(s)}; right key afterwards ${right.status}`);

// 4. Guardian links: random /a/ and /g/ codes, 30 a minute per IP.
s = await shoot(100, 20, (i) => [`/${i % 2 ? "a" : "g"}/${randomBytes(16).toString("hex")}`, { headers: { "x-forwarded-for": "192.0.2.44" } }]);
check("guessing guardian links waits (30/min)", s.filter((x) => x.status !== 429).length <= 30 && broken(s) === 0, counts(s));

await restart();

// 5. Junk: bad JSON, wrong types, huge strings, unknown paths and methods, odd headers.
const junk: [string, RequestInit][] = [
  ["/v1/pears/self", { method: "PUT", body: "{not json" }],
  ["/v1/pears/self", { method: "PUT", body: JSON.stringify({ name: 7, outfit: [], role: null }) }],
  ["/v1/pears/self", { method: "PUT", body: JSON.stringify({ name: "x".repeat(100_000), outfit: ",,", role: "protected", state: "calm" }) }],
  ["/v1/pears/self", { method: "PUT", body: JSON.stringify({ name: "Eve", outfit: "<script>,,", role: "protected", state: "calm" }) }],
  ["/v1/blocks/self", { method: "PUT", body: JSON.stringify({ prints: Array(50_000).fill("a".repeat(64)) }) }],
  ["/v1/blocks/self", { method: "PUT", body: JSON.stringify({ prints: "nope" }) }],
  ["/v1/checks", { method: "POST", body: JSON.stringify({ from: { $gt: "" } }) }],
  ["/v1/devices", { method: "POST", body: JSON.stringify({ code: "../../etc", name: "\u0000" }) }],
  ["/v1/../../etc/passwd", { method: "GET" }],
  ["/v1/pears", { method: "PATCH" }],
  ["/%00%ff", { method: "GET" }],
  ["/v1/opinion", { method: "POST", body: JSON.stringify({ text: "hola" }) }],
  ["/v1/picture", { method: "POST", body: "x" }],
];
// One kind at a time, so a closed connection can be told apart from a crash.
const junkCodes: string[] = [];
let junkBroken = 0;
for (const [n, [path, init]] of junk.entries()) {
  s = await shoot(100, 20, (i) => [path, { ...init, headers: { ...auth(keys[n % 20]), "content-type": "application/json", "x-forwarded-for": ip(20_000 + n * 100 + i) } }]);
  // The 50,000-print body (3.4 MB) is over Bun's 3 MB cap: Bun drops those connections itself, before our code runs.
  const overCap = path === "/v1/blocks/self" && String(init.body).length > 3 * 1024 * 1024;
  junkBroken += s.filter((x) => x.status >= 500 || (x.status === 0 && !overCap)).length;
  junkCodes.push(`${init.method} ${path.slice(0, 18)} → ${counts(s)}`);
}
const alive = await fetch(`${BASE}/v1/pears`, { headers: { ...auth(keys[19]), "x-forwarded-for": ip(29_999) } });
check("junk never breaks the server", junkBroken === 0 && alive.status === 200, `\n      ${junkCodes.join("\n      ")}`);

await restart();

// 6. Bodies too big: declared, and streamed without a length.
const big = new Uint8Array(4 * 1024 * 1024);
const r1 = await fetch(`${BASE}/v1/pears/self`, { method: "PUT", headers: { ...auth(keys[3]), "x-forwarded-for": ip(30_001) }, body: big }).catch(() => null);
const stream = new ReadableStream({
  start(c) {
    for (let i = 0; i < 40; i++) c.enqueue(new Uint8Array(100 * 1024));
    c.close();
  },
});
const r2 = await fetch(`${BASE}/v1/pears/self`, { method: "PUT", headers: { ...auth(keys[3]), "x-forwarded-for": ip(30_002) }, body: stream, duplex: "half" } as RequestInit).catch(() => null);
const r3 = await fetch(`${BASE}/v1/pears/self`, { method: "PUT", headers: { ...auth(keys[3]), "x-forwarded-for": ip(30_003), "content-type": "application/json" }, body: JSON.stringify({ name: "x".repeat(200_000) }) });
check("bodies over the limit are refused", (r1?.status ?? 413) === 413 && (r2?.status ?? 413) === 413 && r3.status === 413, `4 MB declared → ${r1?.status ?? "closed by Bun"}, 4 MB streamed → ${r2?.status ?? "closed by Bun"}, 200 kB JSON → ${r3.status}`);

await restart();
const rss0 = rss();

// 7. Lock an IP out for guessing, then many IPs at once (a botnet's worth): the limiter's table has to stay bounded,
// and the lockout must survive it.
for (let i = 0; i < 10; i++) await fetch(`${BASE}/v1/pears`, { headers: { authorization: "Bearer wrong", "x-forwarded-for": "198.51.100.9" } });
t = performance.now();
s = await shoot(15_000, 200, (i) => [`/v1/pears`, { headers: { authorization: "Bearer nope", "x-forwarded-for": ip(40_000 + i) } }]);
secs = (performance.now() - t) / 1000;
check("15000 different IPs", broken(s) === 0, `${(s.length / secs).toFixed(0)} req/s, p99 ${pct(s, 0.99)} ms, ${counts(s)}`);
const after = await fetch(`${BASE}/v1/pears`, { headers: { ...auth(keys[1]), "x-forwarded-for": "198.51.100.9" } });
check("the token lockout survives a flood of new IPs", after.status === 429, `locked IP with the right key gets ${after.status}`);

const rss1 = rss();
check("memory stays bounded", rss1 < rss0 + 200, `${rss0} MB → ${rss1} MB`);

// 9. Restart: the family's pears come back from the sealed file intact after all those writes.
server.kill();
await server.exited;
server = start();
await up();
const back = await fetch(`${BASE}/v1/pears`, { headers: { ...auth(keys[5]), "x-forwarded-for": ip(50_000) } });
const pears = back.ok ? (await back.json()).pears : [];
check("pears survive a restart", pears.length === 20, `${pears.length} pears, file ${readFileSync(join(data, "pears.json"), "utf8").slice(0, 12)}…`);

server.kill();
await server.exited;
const stderr = await new Response(server.stderr).text();
if (stderr.trim()) console.log("server stderr:\n" + stderr.slice(0, 2000));
rmSync(data, { recursive: true, force: true });
console.log(failures ? `\n${failures} failed` : "\nall passed");
process.exit(failures ? 1 : 0);
