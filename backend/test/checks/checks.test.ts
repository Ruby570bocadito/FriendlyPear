import { expect, test } from "bun:test";
import { CHECK_TTL_MS, CheckStore } from "../../src/checks/store";
import { loadConfig } from "../../src/config";
import { DeviceStore } from "../../src/devices/store";
import { RateLimiter } from "../../src/limits";
import { PearStore } from "../../src/pears/store";
import { createHandler } from "../../src/server";

const shared = "s".repeat(32);
const admin = "a".repeat(32);

function setup() {
  let now = 1_000_000;
  const devices = new DeviceStore(() => now);
  const pears = new PearStore(() => now);
  const checks = new CheckStore(() => now);
  const config = loadConfig({ DEVICE_TOKEN: shared, ADMIN_TOKEN: admin, PUBLIC_BASE_URL: "https://peri.example.org" });
  const handle = createHandler(config, { devices, pears, checks }, new RateLimiter(() => now));
  return { handle, advance: (ms: number) => (now += ms) };
}

const call = (method: string, path: string, auth?: string, body?: unknown) =>
  new Request(`http://x${path}`, {
    method,
    headers: { ...(auth ? { authorization: `Bearer ${auth}` } : {}), "content-type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

async function family(handle: (r: Request) => Promise<Response>, names: string[]) {
  const keys: string[] = [];
  for (const name of names) {
    const { code } = (await (await handle(call("POST", "/v1/pairing", admin))).json()) as { code: string };
    const { key } = (await (await handle(call("POST", "/v1/devices", undefined, { code, name: "Pixel" }))).json()) as { key: string };
    await handle(call("PUT", "/v1/pears/self", key, { name, outfit: ",,", role: "guardian", state: "calm" }));
    keys.push(key);
  }
  return keys;
}

test("Mamá asks; Ana and Pablo see who asks and answer once; Mamá hears who said what", async () => {
  const { handle } = setup();
  const [mama, ana, pablo] = await family(handle, ["Mamá", "Ana", "Pablo"]);
  const asked = await handle(call("POST", "/v1/checks", mama, { kind: "new_number" }));
  expect(asked.status).toBe(201);
  const { id } = (await asked.json()) as { id: string };
  // The one who asked doesn't get her own question; the others do, with her pear's name and nothing else.
  expect(((await (await handle(call("GET", "/v1/checks", mama))).json()) as any).checks).toEqual([]);
  const seen = ((await (await handle(call("GET", "/v1/checks", ana))).json()) as any).checks;
  expect(seen.map((c: any) => [c.id, c.from, c.kind])).toEqual([[id, "Mamá", "new_number"]]);
  expect(Object.keys(seen[0]).sort()).toEqual(["createdAt", "from", "id", "kind"]);
  expect((await handle(call("POST", `/v1/checks/${id}/answer`, ana, { answer: "not_me" }))).status).toBe(200);
  expect((await handle(call("POST", `/v1/checks/${id}/answer`, ana, { answer: "me" }))).status).toBe(409);
  expect((await handle(call("POST", `/v1/checks/${id}/answer`, mama, { answer: "me" }))).status).toBe(403);
  // Answered, it's no longer open for Ana; Pablo still has it.
  expect(((await (await handle(call("GET", "/v1/checks", ana))).json()) as any).checks).toEqual([]);
  expect(((await (await handle(call("GET", "/v1/checks", pablo))).json()) as any).checks.length).toBe(1);
  const result = (await (await handle(call("GET", `/v1/checks/${id}`, mama))).json()) as any;
  expect(result.asked).toBe(2);
  // A paired phone without a pear (an old one nobody uses) isn't waited for.
  const { code } = (await (await handle(call("POST", "/v1/pairing", admin))).json()) as { code: string };
  await handle(call("POST", "/v1/devices", undefined, { code, name: "Old phone" }));
  expect(((await (await handle(call("GET", `/v1/checks/${id}`, mama))).json()) as any).asked).toBe(2);
  expect(result.answers.map((a: any) => [a.name, a.answer])).toEqual([["Ana", "not_me"]]);
  // Only the one who asked reads the answers.
  expect((await handle(call("GET", `/v1/checks/${id}`, ana))).status).toBe(404);
});

test("questions need a phone's own key, a known kind and a known answer, and a phone can't flood its family", async () => {
  const { handle } = setup();
  const [mama, ana] = await family(handle, ["Mamá", "Ana"]);
  expect((await handle(call("POST", "/v1/checks", shared, { kind: "new_number" }))).status).toBe(401);
  expect((await handle(call("GET", "/v1/checks", shared))).status).toBe(401);
  expect((await handle(call("POST", "/v1/checks", mama, { kind: "send_money" }))).status).toBe(400);
  const { id } = (await (await handle(call("POST", "/v1/checks", mama, { kind: "new_number" }))).json()) as { id: string };
  expect((await handle(call("POST", `/v1/checks/${id}/answer`, ana, { answer: "maybe" }))).status).toBe(400);
  expect((await handle(call("POST", "/v1/checks/AAAAAAAAAAAAAAAA/answer", ana, { answer: "me" }))).status).toBe(404);
  await handle(call("POST", "/v1/checks", mama, { kind: "new_number" }));
  await handle(call("POST", "/v1/checks", mama, { kind: "new_number" }));
  expect((await handle(call("POST", "/v1/checks", mama, { kind: "new_number" }))).status).toBe(409);
});

test("a question lasts 15 minutes; a disconnected phone stops asking and answering", async () => {
  const { handle, advance } = setup();
  const [mama, ana] = await family(handle, ["Mamá", "Ana"]);
  const { id } = (await (await handle(call("POST", "/v1/checks", mama, { kind: "new_number" }))).json()) as { id: string };
  advance(CHECK_TTL_MS + 1);
  expect(((await (await handle(call("GET", "/v1/checks", ana))).json()) as any).checks).toEqual([]);
  expect((await handle(call("GET", `/v1/checks/${id}`, mama))).status).toBe(404);
  await handle(call("POST", "/v1/checks", mama, { kind: "new_number" }));
  expect((await handle(call("DELETE", "/v1/devices/self", mama))).status).toBe(200);
  expect(((await (await handle(call("GET", "/v1/checks", ana))).json()) as any).checks).toEqual([]);
});

test("a request whose address can't be read gets a 400, not a crash", async () => {
  const { handle } = setup();
  const odd = { url: "/health", method: "GET", headers: new Headers() } as unknown as Request;
  expect((await handle(odd)).status).toBe(400);
});

test("a new SIM on Mamá's phone asks the family too: who changed it, and nothing else", async () => {
  const { handle } = setup();
  const [mama, ana] = await family(handle, ["Mamá", "Ana"]);
  await handle(call("PUT", "/v1/pears/self", mama, { name: "Mamá", outfit: ",,", role: "protected", state: "attention", kind: "sim" }));
  const asked = await handle(call("POST", "/v1/checks", mama, { kind: "sim" }));
  expect(asked.status).toBe(201);
  const { id } = (await asked.json()) as { id: string };
  const seen = ((await (await handle(call("GET", "/v1/checks", ana))).json()) as any).checks;
  expect(seen.map((c: any) => [c.id, c.from, c.kind])).toEqual([[id, "Mamá", "sim"]]);
  expect((await handle(call("POST", `/v1/checks/${id}/answer`, ana, { answer: "not_me" }))).status).toBe(200);
  const pears = ((await (await handle(call("GET", "/v1/pears", ana))).json()) as any).pears;
  expect(pears.find((p: any) => p.name === "Mamá").kind).toBe("sim");
  expect((await handle(call("POST", "/v1/checks", mama, { kind: "sim_swap" }))).status).toBe(400);
});
