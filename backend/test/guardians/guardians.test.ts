import { expect, test } from "bun:test";
import { mkdtempSync, readFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { ApprovalStore } from "../../src/approvals/store";
import { loadConfig } from "../../src/config";
import { GuardianStore, KEEP_MS, readEvent } from "../../src/guardians/store";
import { createHandler } from "../../src/server";

const token = "x".repeat(32);
const config = loadConfig({ DEVICE_TOKEN: token, PUBLIC_BASE_URL: "http://10.0.2.2:8711" });

function setup() {
  let now = Date.now();
  const guardians = new GuardianStore(() => now);
  const approvals = new ApprovalStore(() => now);
  return { guardians, approvals, handle: createHandler(config, { guardians, approvals }), advance: (ms: number) => (now += ms) };
}

const device = (method: string, path: string, body?: unknown, auth = `Bearer ${token}`) =>
  new Request(`http://x${path}`, {
    method,
    headers: { authorization: auth, "content-type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

async function linkFor(handle: (r: Request) => Promise<Response>, name = "Ana", lang = "en") {
  const res = await handle(device("POST", "/v1/guardians/links", { name, lang }));
  expect(res.status).toBe(201);
  return ((await res.json()) as { url: string }).url;
}

const page = (handle: (r: Request) => Promise<Response>, url: string) => handle(new Request(url.replace("http://10.0.2.2:8711", "http://x")));

test("only the kind of event travels: anything else is refused or dropped", () => {
  expect(readEvent({ kind: "warning", level: "scam", channel: "SMS", text: "Pay now at…", sender: "+34 699" })).toEqual({
    kind: "warning",
    level: "scam",
    channel: "SMS",
  });
  expect(readEvent({ kind: "pause", answer: "leave" })).toEqual({ kind: "pause", answer: "leave" });
  expect(readEvent({ kind: "call", number: "+34 699" })).toBe("unknown kind");
  expect(readEvent({ kind: "warning", level: "ok", channel: "SMS" })).toBe("bad level");
  expect(readEvent({ kind: "pause", answer: "dance" })).toBe("bad answer");
});

test("a guardian's page shows Peri's news, newest first, and what they can never see", async () => {
  const { handle, approvals, advance } = setup();
  const url = await linkFor(handle);
  expect(url).toMatch(/^http:\/\/10\.0\.2\.2:8711\/g\/[A-Za-z0-9_-]{40,}$/);
  expect((await handle(device("POST", "/v1/events", { kind: "warning", level: "scam", channel: "WHATSAPP" }))).status).toBe(201);
  advance(60_000);
  await handle(device("POST", "/v1/events", { kind: "pause", answer: "continue" }));
  approvals.create("send_code", "Pablo", "en");
  const res = await page(handle, url);
  expect(res.status).toBe(200);
  expect(res.headers.get("content-security-policy")).toContain("default-src 'none'");
  const body = await res.text();
  expect(body).toContain("Hi Ana.");
  expect(body).toContain("a WhatsApp message that looked like a scam");
  expect(body).toContain("You may want to give them a call");
  expect(body).toContain("send a code to Pablo: pending");
  expect(body).toContain("You will never see their messages, calls, location or apps");
  expect(body.indexOf("carry on")).toBeLessThan(body.indexOf("looked like a scam"));
  expect(body).not.toContain("<script");
});

test("with nothing to tell, the page says all is calm, in the guardian's language", async () => {
  const { handle } = setup();
  const body = await (await page(handle, await linkFor(handle, "Marisa", "es"))).text();
  expect(body).toContain("Hola, Marisa.");
  expect(body).toContain("Todo tranquilo");
});

test("events need the device token, links are personal, and stopping sharing kills them all", async () => {
  const { handle } = setup();
  expect((await handle(device("POST", "/v1/events", { kind: "pause", answer: "leave" }, "Bearer nope"))).status).toBe(401);
  expect((await handle(device("POST", "/v1/guardians/links", { name: "<script>" }))).status).toBe(400);
  const url = await linkFor(handle);
  expect((await handle(new Request("http://x/g/" + "a".repeat(43)))).status).toBe(404);
  expect((await handle(device("DELETE", "/v1/guardians/links"))).status).toBe(200);
  expect((await page(handle, url)).status).toBe(404);
});

test("news older than 30 days is forgotten", () => {
  let now = 0;
  const store = new GuardianStore(() => now);
  store.add({ kind: "pause", answer: "leave" });
  now = KEEP_MS + 1;
  store.add({ kind: "warning", level: "doubt", channel: "SMS" });
  expect(store.recent().map((e) => e.kind)).toEqual(["warning"]);
});

test("links and news survive a restart, and the file never holds a link itself", () => {
  const file = join(mkdtempSync(join(tmpdir(), "peri-")), "guardians.json");
  const first = new GuardianStore(Date.now, file);
  const { token } = first.createLink("Ana", "es");
  first.add({ kind: "warning", level: "scam", channel: "SMS" });
  expect(readFileSync(file, "utf8")).not.toContain(token);
  const again = new GuardianStore(Date.now, file);
  expect(again.byLink(token)?.name).toBe("Ana");
  expect(again.recent()).toHaveLength(1);
  again.revokeAll();
  expect(new GuardianStore(Date.now, file).byLink(token)).toBeUndefined();
});
