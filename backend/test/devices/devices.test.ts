import { expect, test } from "bun:test";
import { loadConfig } from "../../src/config";
import { CODE, CODE_TTL_MS, DeviceStore } from "../../src/devices/store";
import { RateLimiter } from "../../src/limits";
import { createHandler } from "../../src/server";

const shared = "s".repeat(32);
const admin = "a".repeat(32);

function setup(env: Record<string, string> = {}) {
  let now = 1_000_000;
  const devices = new DeviceStore(() => now);
  const config = loadConfig({ DEVICE_TOKEN: shared, ADMIN_TOKEN: admin, PUBLIC_BASE_URL: "https://peri.example.org", ...env });
  const handle = createHandler(config, { devices }, new RateLimiter(() => now));
  return { devices, handle, advance: (ms: number) => (now += ms) };
}

const call = (method: string, path: string, auth?: string, body?: unknown) =>
  new Request(`http://x${path}`, {
    method,
    headers: { ...(auth ? { authorization: `Bearer ${auth}` } : {}), "content-type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

async function pairPhone(handle: (r: Request) => Promise<Response>, name = "Pixel 8") {
  const made = (await (await handle(call("POST", "/v1/pairing", admin))).json()) as { code: string; link: string };
  const res = await handle(call("POST", "/v1/devices", undefined, { code: made.code, name }));
  return { made, res, body: (await res.json()) as { id: string; key: string } };
}

test("only the admin key makes codes, and the QR link names our own server", async () => {
  const { handle } = setup();
  expect((await handle(call("POST", "/v1/pairing", shared))).status).toBe(401);
  const res = await handle(call("POST", "/v1/pairing", admin));
  expect(res.status).toBe(201);
  const { code, link } = (await res.json()) as { code: string; link: string };
  expect(code).toMatch(CODE);
  expect(link).toBe(`friendlypear://pair?code=${code}&server=${encodeURIComponent("https://peri.example.org")}`);
});

test("a code pairs one phone, once; its own key opens the API", async () => {
  const { handle } = setup();
  const { made, res, body } = await pairPhone(handle);
  expect(res.status).toBe(201);
  expect(body.key).toMatch(/^[A-Za-z0-9_-]{43}$/);
  expect((await handle(call("POST", "/v1/devices", undefined, { code: made.code, name: "Otro" }))).status).toBe(403);
  // A device route accepts the key (400: the body is empty, so it got past the key check).
  expect((await handle(call("POST", "/v1/opinion", body.key, {}))).status).toBe(400);
  expect((await handle(call("POST", "/v1/opinion", "x".repeat(43), {}))).status).toBe(401);
});

test("codes expire after 10 minutes and only hashes are kept", async () => {
  const { handle, devices, advance } = setup();
  const made = (await (await handle(call("POST", "/v1/pairing", admin))).json()) as { code: string };
  advance(CODE_TTL_MS);
  expect((await handle(call("POST", "/v1/devices", undefined, { code: made.code, name: "Pixel" }))).status).toBe(403);
  const { body } = await pairPhone(handle);
  const kept = JSON.stringify([...(devices as any).byKey.keys(), ...(devices as any).codes.keys()]);
  expect(kept).not.toContain(body.key);
});

test("a phone can disconnect itself, and the family can disconnect any", async () => {
  const { handle } = setup();
  const a = (await pairPhone(handle, "Mamá")).body;
  const b = (await pairPhone(handle, "Ana")).body;
  expect((await handle(call("DELETE", "/v1/devices/self", a.key))).status).toBe(200);
  expect((await handle(call("POST", "/v1/opinion", a.key, {}))).status).toBe(401);
  const list = (await (await handle(call("GET", "/v1/devices", admin))).json()) as { devices: { id: string; name: string }[] };
  expect(list.devices.map((d) => d.name)).toEqual(["Ana"]);
  expect((await handle(call("DELETE", `/v1/devices/${b.id}`, admin))).status).toBe(200);
  expect((await handle(call("POST", "/v1/opinion", b.key, {}))).status).toBe(401);
});

test("with ALLOW_SHARED_TOKEN=0 only paired phones get in", async () => {
  const { handle } = setup({ ALLOW_SHARED_TOKEN: "0" });
  expect((await handle(call("POST", "/v1/opinion", shared, {}))).status).toBe(401);
  const { body } = await pairPhone(handle);
  expect((await handle(call("POST", "/v1/opinion", body.key, {}))).status).toBe(400);
});

test("guessing codes is cut off quickly", async () => {
  const { handle } = setup();
  let blocked = false;
  for (let i = 0; i < 15; i++) {
    const res = await handle(call("POST", "/v1/devices", undefined, { code: "AAAA-AAAA", name: "x" }));
    if (res.status === 429) blocked = true;
  }
  expect(blocked).toBe(true);
});

test("bad pairing requests are refused", async () => {
  const { handle } = setup();
  expect((await handle(call("POST", "/v1/devices", undefined, { code: "1234", name: "x" }))).status).toBe(400);
  expect((await handle(call("POST", "/v1/devices", undefined, { code: "AAAA-AAAA", name: "<script>" }))).status).toBe(400);
});
