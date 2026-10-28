import { expect, test } from "bun:test";
import { ApprovalStore } from "../src/approvals/store";
import { loadConfig } from "../src/config";
import { DeviceStore } from "../src/devices/store";
import { LIMITS, RateLimiter } from "../src/limits";
import { HangUpStore } from "../src/pears/hangups";
import { PearStore } from "../src/pears/store";
import { createHandler } from "../src/server";

const shared = "s".repeat(32);
const admin = "a".repeat(32);

function setup() {
  let now = 1_000_000;
  const devices = new DeviceStore(() => now);
  const pears = new PearStore(() => now);
  const hangups = new HangUpStore(() => now);
  const approvals = new ApprovalStore(() => now);
  const config = loadConfig({ DEVICE_TOKEN: shared, ADMIN_TOKEN: admin, PUBLIC_BASE_URL: "https://peri.example.org" });
  const handle = createHandler(config, { devices, pears, hangups, approvals }, new RateLimiter(() => now));
  return { handle, approvals, advance: (ms: number) => (now += ms) };
}

const call = (method: string, path: string, auth?: string, body?: unknown) =>
  new Request(`http://x${path}`, {
    method,
    headers: { ...(auth ? { authorization: `Bearer ${auth}` } : {}), "content-type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

async function phone(handle: (r: Request, ip?: string) => Promise<Response>, name: string, state = "calm", kind = "") {
  const { code } = (await (await handle(call("POST", "/v1/pairing", admin))).json()) as { code: string };
  const { id, key } = (await (await handle(call("POST", "/v1/devices", undefined, { code, name: "Pixel" }))).json()) as { id: string; key: string };
  await handle(call("PUT", "/v1/pears/self", key, { name, outfit: ",,", role: "guardian", state, kind }));
  return { id, key };
}

test("a phone is limited as itself, wherever it calls from: changing IP doesn't buy more", async () => {
  const { handle, advance } = setup();
  const ana = await phone(handle, "Ana");
  let last = 200;
  for (let i = 0; i < LIMITS.phone.max + 5; i++) last = (await handle(call("GET", "/v1/pears", ana.key), `10.0.0.${i % 50}`)).status;
  expect(last).toBe(429);
  // Another phone of the family is not held up by Ana's.
  const papa = await phone(handle, "Papá");
  expect((await handle(call("GET", "/v1/pears", papa.key), "10.0.1.1")).status).toBe(200);
  advance(LIMITS.phone.windowMs + 1);
  expect((await handle(call("GET", "/v1/pears", ana.key), "10.0.2.1")).status).toBe(200);
});

test("one phone can ask to hang up only a few times in ten minutes", async () => {
  const { handle } = setup();
  const mama = await phone(handle, "Mamá", "attention", "call");
  const ana = await phone(handle, "Ana");
  const statuses: number[] = [];
  for (let i = 0; i < LIMITS.hangup.max + 2; i++) {
    statuses.push((await handle(call("POST", `/v1/pears/${mama.id}/hangup`, ana.key))).status);
    await handle(call("GET", "/v1/hangup", mama.key));
  }
  expect(statuses[0]).toBe(202);
  expect(statuses.at(-1)).toBe(429);
});

test("a guardian's yes waits while their own phone is on a call with a stranger; a no doesn't", async () => {
  const { handle, approvals } = setup();
  const ana = await phone(handle, "Ana", "attention", "call");
  const form = (decision: string, key?: string) =>
    new Request(url, {
      method: "POST",
      headers: { "content-type": "application/x-www-form-urlencoded", ...(key ? { authorization: `Bearer ${key}` } : {}) },
      body: `decision=${decision}&confirm=yes`,
    });
  const mama = await phone(handle, "Mamá");
  const { url } = (await (await handle(call("POST", "/v1/approvals", mama.key, { kind: "send_code", toName: "Pablo", lang: "en" }))).json()) as { url: string };
  const refused = await handle(form("approve", ana.key));
  expect(refused.status).toBe(409);
  expect(await refused.text()).toContain("Hang up first");
  // Still pending: nothing was approved.
  expect(approvals.recent()[0].status).toBe("pending");
  // Once her call is over, her yes counts, and her name goes with it.
  await handle(call("PUT", "/v1/pears/self", ana.key, { name: "Ana", outfit: ",,", role: "guardian", state: "calm", kind: "" }));
  expect((await handle(form("approve", ana.key))).status).toBe(200);
  expect(approvals.recent()[0].status).toBe("approved");
  expect(approvals.recent()[0].decidedBy).toBe("Ana");
});
