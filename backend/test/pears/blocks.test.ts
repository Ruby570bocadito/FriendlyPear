import { expect, test } from "bun:test";
import { createHmac } from "node:crypto";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { loadConfig } from "../../src/config";
import { DeviceStore } from "../../src/devices/store";
import { RateLimiter } from "../../src/limits";
import { BlockStore, MAX_PRINTS } from "../../src/pears/blocks";
import { createHandler } from "../../src/server";

const shared = "s".repeat(32);
const admin = "a".repeat(32);

function setup(blocks = new BlockStore()) {
  const devices = new DeviceStore();
  const config = loadConfig({ DEVICE_TOKEN: shared, ADMIN_TOKEN: admin, PUBLIC_BASE_URL: "https://peri.example.org" });
  return { handle: createHandler(config, { devices, blocks }, new RateLimiter()), blocks };
}

const call = (method: string, path: string, auth?: string, body?: unknown) =>
  new Request(`http://x${path}`, {
    method,
    headers: { ...(auth ? { authorization: `Bearer ${auth}` } : {}), "content-type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

async function phone(handle: (r: Request) => Promise<Response>) {
  const { code } = (await (await handle(call("POST", "/v1/pairing", admin))).json()) as { code: string };
  return (await (await handle(call("POST", "/v1/devices", undefined, { code, name: "Pixel" }))).json()) as { id: string; key: string };
}

/** What a phone does: the last nine digits, keyed with the family's salt (android/.../config/FamilyBlocks.kt). */
const print = (salt: string, number: string) => createHmac("sha256", salt).update(number.replace(/\D/g, "").slice(-9)).digest("hex");

test("a number Mamá blocks reaches Ana's phone as a fingerprint, never as a number, and without saying whose", async () => {
  const { handle } = setup();
  const mama = await phone(handle);
  const ana = await phone(handle);
  const { salt } = (await (await handle(call("GET", "/v1/blocks", mama.key))).json()) as { salt: string };
  const scam = print(salt, "+34 611 000 029");
  expect((await handle(call("PUT", "/v1/blocks/self", mama.key, { prints: [scam, scam] }))).status).toBe(200);
  const seen = (await (await handle(call("GET", "/v1/blocks", ana.key))).json()) as any;
  expect(seen).toEqual({ salt, prints: [scam] });
  // The same number, written another way on Ana's phone, gives the same fingerprint.
  expect(seen.prints).toContain(print(seen.salt, "611000029"));
  expect(JSON.stringify(seen)).not.toContain("611000029");
  // Mamá doesn't get her own back: they're already on her phone.
  expect(((await (await handle(call("GET", "/v1/blocks", mama.key))).json()) as any).prints).toEqual([]);
});

test("unblocking on Mamá's phone frees the number for everyone; a disconnected phone's blocks go with it", async () => {
  const { handle } = setup();
  const mama = await phone(handle);
  const papa = await phone(handle);
  const ana = await phone(handle);
  const { salt } = (await (await handle(call("GET", "/v1/blocks", mama.key))).json()) as { salt: string };
  await handle(call("PUT", "/v1/blocks/self", mama.key, { prints: [print(salt, "611000029")] }));
  await handle(call("PUT", "/v1/blocks/self", papa.key, { prints: [print(salt, "622000111")] }));
  expect(((await (await handle(call("GET", "/v1/blocks", ana.key))).json()) as any).prints).toHaveLength(2);
  await handle(call("PUT", "/v1/blocks/self", mama.key, { prints: [] }));
  expect(((await (await handle(call("GET", "/v1/blocks", ana.key))).json()) as any).prints).toEqual([print(salt, "622000111")]);
  expect((await handle(call("DELETE", `/v1/devices/${papa.id}`, admin))).status).toBe(200);
  expect(((await (await handle(call("GET", "/v1/blocks", ana.key))).json()) as any).prints).toEqual([]);
});

test("only a paired phone's own key, and only fingerprints, at most a hundred", async () => {
  const { handle } = setup();
  const mama = await phone(handle);
  expect((await handle(call("GET", "/v1/blocks", shared))).status).toBe(401);
  expect((await handle(call("GET", "/v1/blocks"))).status).toBe(401);
  expect((await handle(call("PUT", "/v1/blocks/self", shared, { prints: [] }))).status).toBe(401);
  for (const prints of [["611000029"], ["A".repeat(64)], "x", [1], Array.from({ length: MAX_PRINTS + 1 }, (_, i) => i.toString(16).padStart(64, "0"))]) {
    expect((await handle(call("PUT", "/v1/blocks/self", mama.key, { prints }))).status).toBe(400);
  }
  expect((await handle(call("PUT", "/v1/blocks/self", mama.key, {}))).status).toBe(400);
});

test("the salt and the fingerprints survive a restart; a new family gets a different salt", () => {
  const file = join(mkdtempSync(join(tmpdir(), "blocks-")), "blocks.json");
  const first = new BlockStore(file);
  first.put("mama", [print(first.salt, "611000029")]);
  const again = new BlockStore(file);
  expect(again.salt).toBe(first.salt);
  expect(again.othersOf("ana", new Set(["mama"]))).toEqual([print(first.salt, "611000029")]);
  expect(new BlockStore().salt).not.toBe(first.salt);
  expect(first.salt.length).toBeGreaterThanOrEqual(43);
});
