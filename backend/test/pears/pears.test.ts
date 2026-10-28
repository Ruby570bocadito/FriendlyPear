import { expect, test } from "bun:test";
import { loadConfig } from "../../src/config";
import { DeviceStore } from "../../src/devices/store";
import { RateLimiter } from "../../src/limits";
import { ATTENTION_TTL_MS, CAMPAIGN_WINDOW_MS, PearStore, readPear } from "../../src/pears/store";
import { createHandler } from "../../src/server";

const shared = "s".repeat(32);
const admin = "a".repeat(32);

function setup() {
  let now = 1_000_000;
  const devices = new DeviceStore(() => now);
  const pears = new PearStore(() => now);
  const config = loadConfig({ DEVICE_TOKEN: shared, ADMIN_TOKEN: admin, PUBLIC_BASE_URL: "https://peri.example.org" });
  const handle = createHandler(config, { devices, pears }, new RateLimiter(() => now));
  return { devices, pears, handle, advance: (ms: number) => (now += ms) };
}

const call = (method: string, path: string, auth?: string, body?: unknown) =>
  new Request(`http://x${path}`, {
    method,
    headers: { ...(auth ? { authorization: `Bearer ${auth}` } : {}), "content-type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

async function pair(handle: (r: Request) => Promise<Response>, name: string) {
  const { code } = (await (await handle(call("POST", "/v1/pairing", admin))).json()) as { code: string };
  return (await (await handle(call("POST", "/v1/devices", undefined, { code, name }))).json()) as { id: string; key: string };
}

const mum = { name: "Mamá", outfit: "STRAW,,BOWTIE", role: "protected", state: "calm" };

test("a phone sends its pear and every paired phone of the family sees it, marked as its own or not", async () => {
  const { handle } = setup();
  const a = await pair(handle, "Pixel de mamá");
  const b = await pair(handle, "Pixel de Ana");
  expect((await handle(call("PUT", "/v1/pears/self", a.key, mum))).status).toBe(200);
  expect((await handle(call("PUT", "/v1/pears/self", b.key, { name: "Ana", outfit: ",HEARTS,", role: "guardian", state: "calm" }))).status).toBe(200);
  const seen = (await (await handle(call("GET", "/v1/pears", b.key))).json()) as { pears: any[] };
  expect(seen.pears.map((p) => [p.name, p.outfit, p.role, p.self])).toEqual([
    ["Ana", ",HEARTS,", "guardian", true],
    ["Mamá", "STRAW,,BOWTIE", "protected", false],
  ]);
  // Only the pear: no device name, no key, nothing about messages.
  expect(Object.keys(seen.pears[0]).sort()).toEqual(["id", "kind", "name", "outfit", "role", "self", "state", "updatedAt"]);
});

test("pears need a phone's own key: not the shared test token, not a wrong key", async () => {
  const { handle } = setup();
  expect((await handle(call("PUT", "/v1/pears/self", shared, mum))).status).toBe(401);
  expect((await handle(call("GET", "/v1/pears", shared))).status).toBe(401);
  expect((await handle(call("GET", "/v1/pears", "x".repeat(43)))).status).toBe(401);
});

test("only known names, outfits, roles and states get in", () => {
  expect(readPear(mum)).toEqual({ ...mum, kind: "" } as any);
  expect(readPear({ ...mum, name: "" })).toBe("name");
  expect(readPear({ ...mum, name: "<script>" })).toBe("name");
  expect(readPear({ ...mum, name: "x".repeat(25) })).toBe("name");
  expect(readPear({ ...mum, outfit: "HAT,," })).toBe("outfit");
  expect(readPear({ ...mum, outfit: "STRAW,BOWTIE," })).toBe("outfit");
  expect(readPear({ ...mum, outfit: "STRAW,," + ",".repeat(2) })).toBe("outfit");
  expect(readPear({ ...mum, role: "admin" })).toBe("role");
  expect(readPear({ ...mum, state: "panic" })).toBe("state");
  expect(readPear({ ...mum, outfit: ",," })).toEqual({ ...mum, outfit: ",,", kind: "" } as any);
  expect(readPear({ ...mum, kind: "lottery" })).toBe("kind");
  // A calm pear carries no kind of trap, whatever it sends.
  expect((readPear({ ...mum, kind: "parcel" }) as any).kind).toBe("");
  expect((readPear({ ...mum, state: "attention", kind: "parcel" }) as any).kind).toBe("parcel");
});

test("an attention nobody refreshed fades back to calm", async () => {
  const { handle, advance } = setup();
  const a = await pair(handle, "Pixel");
  await handle(call("PUT", "/v1/pears/self", a.key, { ...mum, state: "attention" }));
  const now = async () => ((await (await handle(call("GET", "/v1/pears", a.key))).json()) as { pears: any[] }).pears[0].state;
  expect(await now()).toBe("attention");
  advance(ATTENTION_TTL_MS + 1);
  expect(await now()).toBe("calm");
});

test("a disconnected phone's pear goes with it", async () => {
  const { handle } = setup();
  const a = await pair(handle, "Pixel A");
  const b = await pair(handle, "Pixel B");
  await handle(call("PUT", "/v1/pears/self", a.key, mum));
  await handle(call("PUT", "/v1/pears/self", b.key, { ...mum, name: "Ana" }));
  expect((await handle(call("DELETE", "/v1/devices/self", a.key))).status).toBe(200);
  const seen = (await (await handle(call("GET", "/v1/pears", b.key))).json()) as { pears: any[] };
  expect(seen.pears.map((p) => p.name)).toEqual(["Ana"]);
});

test("the same trap reaching two family phones within half an hour is a campaign; one alone, or old, is not", async () => {
  const { handle, advance } = setup();
  const a = await pair(handle, "Pixel A");
  const b = await pair(handle, "Pixel B");
  const campaignNow = async () => ((await (await handle(call("GET", "/v1/pears", b.key))).json()) as { campaign: string | null }).campaign;
  await handle(call("PUT", "/v1/pears/self", a.key, { ...mum, state: "attention", kind: "parcel" }));
  expect(await campaignNow()).toBeNull();
  await handle(call("PUT", "/v1/pears/self", b.key, { ...mum, name: "Ana", state: "attention", kind: "link" }));
  expect(await campaignNow()).toBeNull();
  await handle(call("PUT", "/v1/pears/self", b.key, { ...mum, name: "Ana", state: "attention", kind: "parcel" }));
  expect(await campaignNow()).toBe("parcel");
  advance(CAMPAIGN_WINDOW_MS + 1);
  expect(await campaignNow()).toBeNull();
});
