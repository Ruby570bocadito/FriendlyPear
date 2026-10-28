import { expect, test } from "bun:test";
import { randomBytes } from "node:crypto";
import { mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { ApprovalStore } from "../src/approvals/store";
import { CheckStore } from "../src/checks/store";
import { loadConfig } from "../src/config";
import { DeviceStore } from "../src/devices/store";
import { PearStore } from "../src/pears/store";
import { rotated } from "../src/rotate";
import { createHandler } from "../src/server";
import { Vault, readStore, writeStore } from "../src/vault";

const dir = () => mkdtempSync(join(tmpdir(), "vault-"));
const vault = () => new Vault(randomBytes(32));

test("a sealed file reads back, shows nothing in clear, and a changed byte or another key opens nothing", () => {
  const v = vault();
  const file = join(dir(), "pears.json");
  writeStore(file, { pears: [{ name: "Mamá", outfit: "STRAW,," }] }, v);
  const raw = readFileSync(file, "utf8");
  expect(raw.startsWith("fpv1:")).toBe(true);
  expect(raw).not.toContain("Mamá");
  expect(readStore(file, v)).toEqual({ pears: [{ name: "Mamá", outfit: "STRAW,," }] });
  expect(readStore(file, vault())).toBeNull();
  const bytes = Buffer.from(raw.slice(5), "base64");
  bytes[20] ^= 1;
  writeFileSync(file, "fpv1:" + bytes.toString("base64"));
  expect(readStore(file, v)).toBeNull();
});

test("a file saved before sealing is read as it was, and sealed on its next save", () => {
  const v = vault();
  const file = join(dir(), "pears.json");
  writeFileSync(file, JSON.stringify({ pears: [{ device: "d1", name: "Ana", outfit: ",,", role: "guardian", state: "calm", kind: "", updatedAt: 1 }] }));
  const pears = new PearStore(Date.now, file, v);
  expect(pears.list().map((p) => p.name)).toEqual(["Ana"]);
  pears.put("d2", { name: "Papá", outfit: ",,", role: "guardian", state: "calm", kind: "" });
  expect(readFileSync(file, "utf8")).not.toContain("Papá");
  expect(new PearStore(Date.now, file, v).list().map((p) => p.name)).toEqual(["Ana", "Papá"]);
});

test('a pending approval and a "was it you?" question survive a restart, sealed', () => {
  const v = vault();
  const d = dir();
  const first = new ApprovalStore(Date.now, join(d, "approvals.json"), v);
  const { approval, links } = first.create("send_code", "Pablo", "es", true, undefined, ["Ana", "Papá"]);
  first.decide(links[0]!.token, "approve");
  expect(readFileSync(join(d, "approvals.json"), "utf8")).not.toContain("Pablo");
  const again = new ApprovalStore(Date.now, join(d, "approvals.json"), v);
  expect(again.get(approval.id)?.yes).toEqual(["Ana"]);
  expect(again.decide(links[1]!.token, "approve")).toMatchObject({ status: "approved", yes: ["Ana", "Papá"] });
  expect(again.decide(links[0]!.token, "approve")).toBe("unknown");

  const checks = new CheckStore(Date.now, join(d, "checks.json"), v);
  const c = checks.ask("mama", "sim")!;
  checks.answer(c.id, "ana", "not_me");
  const later = new CheckStore(Date.now, join(d, "checks.json"), v);
  expect(later.mine(c.id, "mama")?.answers.get("ana")?.answer).toBe("not_me");
});

test("a phone changes its own key: the old one stops at once, the new one works", async () => {
  const shared = "s".repeat(32);
  const admin = "a".repeat(32);
  const devices = new DeviceStore();
  const handle = createHandler(loadConfig({ DEVICE_TOKEN: shared, ADMIN_TOKEN: admin }), { devices });
  const call = (method: string, path: string, auth?: string, body?: unknown) =>
    handle(new Request(`http://x${path}`, { method, headers: { ...(auth ? { authorization: `Bearer ${auth}` } : {}), "content-type": "application/json" }, body: body === undefined ? undefined : JSON.stringify(body) }));
  const { code } = (await (await call("POST", "/v1/pairing", admin)).json()) as { code: string };
  const { key } = (await (await call("POST", "/v1/devices", undefined, { code, name: "Pixel" })).json()) as { key: string };
  const res = await call("POST", "/v1/devices/self/key", key);
  expect(res.status).toBe(201);
  const fresh = ((await res.json()) as { key: string }).key;
  expect(fresh).not.toBe(key);
  expect((await call("POST", "/v1/devices/self/key", key)).status).toBe(401);
  expect((await call("POST", "/v1/devices/self/key", shared)).status).toBe(401);
  expect(devices.check(fresh)?.rotatedAt).toBeGreaterThan(0);
});

test("after `bun rotate` the old admin token works for a day, then not at all", async () => {
  const env = rotated("DEVICE_TOKEN=" + "s".repeat(32) + "\nADMIN_TOKEN=" + "o".repeat(32) + "\n", "n".repeat(32), new Date(Date.now() + 60_000));
  expect(env).toContain("ADMIN_TOKEN=" + "n".repeat(32));
  expect(env).toContain("ADMIN_TOKEN_PREVIOUS=" + "o".repeat(32));
  const parse = (text: string) => Object.fromEntries(text.trim().split("\n").map((l) => [l.slice(0, l.indexOf("=")), l.slice(l.indexOf("=") + 1)]));
  const live = createHandler(loadConfig(parse(env)), { devices: new DeviceStore() });
  const pairing = (token: string, h = live) => h(new Request("http://x/v1/pairing", { method: "POST", headers: { authorization: `Bearer ${token}` } }));
  expect((await pairing("n".repeat(32))).status).toBe(201);
  expect((await pairing("o".repeat(32))).status).toBe(201);
  const expired = createHandler(loadConfig({ ...parse(env), ADMIN_TOKEN_PREVIOUS_UNTIL: new Date(Date.now() - 1).toISOString() }), { devices: new DeviceStore() });
  expect((await pairing("o".repeat(32), expired)).status).toBe(401);
});

test("what the server writes to its log has no names, numbers or messages", async () => {
  const lines: string[] = [];
  const log = console.log;
  console.log = (...args: unknown[]) => lines.push(args.join(" "));
  try {
    const store = new ApprovalStore();
    const handle = createHandler(loadConfig({ DEVICE_TOKEN: "s".repeat(32), PUBLIC_BASE_URL: "http://x" }), { approvals: store });
    const res = await handle(new Request("http://x/v1/approvals", { method: "POST", headers: { authorization: `Bearer ${"s".repeat(32)}`, "content-type": "application/json" }, body: JSON.stringify({ kind: "send_code", toName: "Pablo", lang: "es", guardians: ["Ana", "Papá"] }) }));
    const { links } = (await res.json()) as { links: { url: string }[] };
    await handle(new Request(links[0]!.url, { headers: { "user-agent": "Mozilla/5.0 (Linux; Android 15; Pixel 9) Chrome/131" } }));
    await handle(new Request(links[0]!.url, { method: "POST", headers: { "content-type": "application/x-www-form-urlencoded", "user-agent": "Pixel 9" }, body: "decision=reject" }));
  } finally {
    console.log = log;
  }
  expect(lines.length).toBeGreaterThan(0);
  for (const line of lines) expect(line).not.toMatch(/Pablo|Ana|Papá|Pixel|Mozilla|\/a\/[A-Za-z0-9_-]{20,}|\d{6,}/);
});
