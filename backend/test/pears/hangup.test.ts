import { expect, test } from "bun:test";
import { loadConfig } from "../../src/config";
import { DeviceStore } from "../../src/devices/store";
import { RateLimiter } from "../../src/limits";
import { HANGUP_TTL_MS, HangUpStore } from "../../src/pears/hangups";
import { PearStore } from "../../src/pears/store";
import { createHandler } from "../../src/server";

const shared = "s".repeat(32);
const admin = "a".repeat(32);

function setup() {
  let now = 1_000_000;
  const devices = new DeviceStore(() => now);
  const pears = new PearStore(() => now);
  const hangups = new HangUpStore(() => now);
  const config = loadConfig({ DEVICE_TOKEN: shared, ADMIN_TOKEN: admin, PUBLIC_BASE_URL: "https://peri.example.org" });
  const handle = createHandler(config, { devices, pears, hangups }, new RateLimiter(() => now));
  return { handle, advance: (ms: number) => (now += ms) };
}

const call = (method: string, path: string, auth?: string, body?: unknown) =>
  new Request(`http://x${path}`, {
    method,
    headers: { ...(auth ? { authorization: `Bearer ${auth}` } : {}), "content-type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

async function phone(handle: (r: Request) => Promise<Response>, name: string, state = "calm", kind = "") {
  const { code } = (await (await handle(call("POST", "/v1/pairing", admin))).json()) as { code: string };
  const { id, key } = (await (await handle(call("POST", "/v1/devices", undefined, { code, name: "Pixel" }))).json()) as { id: string; key: string };
  await handle(call("PUT", "/v1/pears/self", key, { name, outfit: ",,", role: "protected", state, kind }));
  return { id, key };
}

test("Ana sees Mamá on a stranger's call and asks her Peri to hang up; Mamá's Peri takes it once", async () => {
  const { handle } = setup();
  const mama = await phone(handle, "Mamá", "attention", "call");
  const ana = await phone(handle, "Ana");
  const pears = ((await (await handle(call("GET", "/v1/pears", ana.key))).json()) as any).pears;
  expect(pears.find((p: any) => p.name === "Mamá").kind).toBe("call");
  expect((await handle(call("POST", `/v1/pears/${mama.id}/hangup`, ana.key))).status).toBe(202);
  // Asked twice, it's still one request.
  expect((await handle(call("POST", `/v1/pears/${mama.id}/hangup`, ana.key))).status).toBe(409);
  expect(await (await handle(call("GET", "/v1/hangup", mama.key))).json()).toEqual({ from: "Ana" });
  expect(await (await handle(call("GET", "/v1/hangup", mama.key))).json()).toEqual({ from: null });
  // Nobody asked Ana's phone anything.
  expect(await (await handle(call("GET", "/v1/hangup", ana.key))).json()).toEqual({ from: null });
});

test("only a family phone on a stranger's call can be asked, by another phone with its own key", async () => {
  const { handle } = setup();
  const mama = await phone(handle, "Mamá", "attention", "parcel");
  const ana = await phone(handle, "Ana");
  expect((await handle(call("POST", `/v1/pears/${mama.id}/hangup`, ana.key))).status).toBe(409);
  await handle(call("PUT", "/v1/pears/self", mama.key, { name: "Mamá", outfit: ",,", role: "protected", state: "calm", kind: "" }));
  expect((await handle(call("POST", `/v1/pears/${mama.id}/hangup`, ana.key))).status).toBe(409);
  expect((await handle(call("POST", `/v1/pears/${ana.id}/hangup`, ana.key))).status).toBe(409);
  expect((await handle(call("POST", `/v1/pears/${mama.id}/hangup`, shared))).status).toBe(401);
  expect((await handle(call("GET", "/v1/hangup", shared))).status).toBe(401);
  expect((await handle(call("POST", "/v1/pears/AAAAAAAAAAAA/hangup", ana.key))).status).toBe(409);
});

test("a request waits two minutes at most, and goes with a disconnected phone", async () => {
  const { handle, advance } = setup();
  const mama = await phone(handle, "Mamá", "attention", "call");
  const ana = await phone(handle, "Ana");
  await handle(call("POST", `/v1/pears/${mama.id}/hangup`, ana.key));
  advance(HANGUP_TTL_MS + 1);
  expect(await (await handle(call("GET", "/v1/hangup", mama.key))).json()).toEqual({ from: null });
  await handle(call("PUT", "/v1/pears/self", mama.key, { name: "Mamá", outfit: ",,", role: "protected", state: "attention", kind: "call" }));
  await handle(call("POST", `/v1/pears/${mama.id}/hangup`, ana.key));
  expect((await handle(call("DELETE", "/v1/devices/self", ana.key))).status).toBe(200);
  expect(await (await handle(call("GET", "/v1/hangup", mama.key))).json()).toEqual({ from: null });
});
