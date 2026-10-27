import { expect, test } from "bun:test";
import { ApprovalStore, TTL_MS } from "../../src/approvals/store";
import { loadConfig } from "../../src/config";
import { createHandler } from "../../src/server";

const token = "x".repeat(32);
const config = loadConfig({ DEVICE_TOKEN: token, PUBLIC_BASE_URL: "http://10.0.2.2:8711" });

function setup() {
  let now = 1_000_000;
  const store = new ApprovalStore(() => now);
  return { store, handle: createHandler(config, { approvals: store }), advance: (ms: number) => (now += ms) };
}

const device = (method: string, path: string, body?: unknown) =>
  new Request(`http://x${path}`, {
    method,
    headers: { authorization: `Bearer ${token}`, "content-type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

const decide = (url: string, decision: string) =>
  new Request(url, { method: "POST", headers: { "content-type": "application/x-www-form-urlencoded" }, body: `decision=${decision}` });

async function newApproval(handle: (r: Request) => Promise<Response>, lang = "es") {
  const res = await handle(device("POST", "/v1/approvals", { kind: "send_code", toName: "Pablo", lang }));
  expect(res.status).toBe(201);
  return (await res.json()) as { id: string; url: string; expiresAt: number };
}

const statusOf = async (handle: (r: Request) => Promise<Response>, id: string) =>
  ((await (await handle(device("GET", `/v1/approvals/${id}`))).json()) as { status: string }).status;

test("a request gives a one-time link on the public address that expires in 15 minutes", async () => {
  const { handle } = setup();
  const a = await newApproval(handle);
  expect(a.url).toMatch(/^http:\/\/10\.0\.2\.2:8711\/a\/[A-Za-z0-9_-]{40,}$/);
  expect(a.expiresAt - 1_000_000).toBe(TTL_MS);
  expect(await statusOf(handle, a.id)).toBe("pending");
});

test("the guardian sees what is asked, with No first, and never the code", async () => {
  const { handle } = setup();
  const a = await newApproval(handle);
  const page = await (await handle(new Request(a.url))).text();
  expect(page).toContain("Tu familiar quiere mandar a Pablo un mensaje con un código.");
  expect(page.indexOf("No, que no lo mande")).toBeLessThan(page.indexOf("Sí, lo apruebo"));
  expect(page).not.toMatch(/\d{4,}/);
});

test("a change of guardians says so and why it waits 48 hours", async () => {
  const { handle } = setup();
  const res = await handle(device("POST", "/v1/approvals", { kind: "circle_change", toName: "añadir a Marisa", lang: "es" }));
  expect(res.status).toBe(201);
  const { url } = (await res.json()) as { url: string };
  const page = await (await handle(new Request(url))).text();
  expect(page).toContain("Se ha pedido un cambio en los guardianes de tu familiar: añadir a Marisa.");
  expect(page).toContain("esperan 48 horas");
  expect(page).toContain("No, que no cambie nada");
});

test("the page follows the phone's language", async () => {
  const { handle } = setup();
  const a = await newApproval(handle, "en");
  expect(await (await handle(new Request(a.url))).text()).toContain("Your relative wants to send Pablo a message with a code.");
});

test("approving reaches the phone, and the link then stops working", async () => {
  const { handle } = setup();
  const a = await newApproval(handle);
  const res = await handle(decide(a.url, "approve"));
  expect(await res.text()).toContain("Aprobado");
  expect(await statusOf(handle, a.id)).toBe("approved");
  expect((await handle(decide(a.url, "reject"))).status).toBe(404);
  expect(await statusOf(handle, a.id)).toBe("approved");
});

test("rejecting is final too", async () => {
  const { handle } = setup();
  const a = await newApproval(handle);
  await handle(decide(a.url, "reject"));
  expect(await statusOf(handle, a.id)).toBe("rejected");
});

test("silence never approves: after 15 minutes it is expired and the link is dead", async () => {
  const { handle, advance } = setup();
  const a = await newApproval(handle);
  advance(TTL_MS);
  expect(await statusOf(handle, a.id)).toBe("expired");
  expect((await handle(new Request(a.url))).status).toBe(410);
  expect((await handle(decide(a.url, "approve"))).status).toBe(410);
  expect(await statusOf(handle, a.id)).toBe("expired");
});

test("only the phone can create or poll requests", async () => {
  const { handle } = setup();
  const noAuth = new Request("http://x/v1/approvals", { method: "POST", body: JSON.stringify({ kind: "send_code", toName: "Pablo" }) });
  expect((await handle(noAuth)).status).toBe(401);
  const a = await newApproval(handle);
  expect((await handle(new Request(`http://x/v1/approvals/${a.id}`))).status).toBe(401);
});

test("bad requests are refused", async () => {
  const { handle } = setup();
  expect((await handle(device("POST", "/v1/approvals", { kind: "move_money", toName: "Pablo" }))).status).toBe(400);
  expect((await handle(device("POST", "/v1/approvals", { kind: "send_code", toName: "<script>" }))).status).toBe(400);
  expect((await handle(device("POST", "/v1/approvals", { kind: "send_code", toName: "Pablo 600111222" }))).status).toBe(400);
});

test("unknown links and odd decisions do nothing", async () => {
  const { handle } = setup();
  const a = await newApproval(handle);
  expect((await handle(new Request(`http://x/a/${"z".repeat(43)}`))).status).toBe(404);
  expect((await handle(decide(a.url, "maybe"))).status).toBe(400);
  expect(await statusOf(handle, a.id)).toBe("pending");
});

test("the store keeps only a hash of the link", () => {
  const store = new ApprovalStore();
  const { token: link } = store.create("send_code", "Pablo", "es");
  expect(JSON.stringify([...(store as any).byToken.keys()])).not.toContain(link);
});

test("the guardian can ask to talk first, and the phone hears it as callback", async () => {
  const { handle } = setup();
  const a = await newApproval(handle);
  const page = await (await handle(new Request(a.url))).text();
  // No first, then "call me", then yes: the safe answers are the easy ones.
  expect(page.indexOf("No, que no lo mande")).toBeLessThan(page.indexOf("Que me llame primero"));
  expect(page.indexOf("Que me llame primero")).toBeLessThan(page.indexOf("Sí, lo apruebo"));
  const done = await (await handle(decide(a.url, "callback"))).text();
  expect(done).toContain("le ofrece llamarte");
  expect(await statusOf(handle, a.id)).toBe("callback");
  expect((await handle(decide(a.url, "approve"))).status).toBe(404);
});

test("a scam in progress is flagged to the guardian, without any detail of the call", async () => {
  const { handle } = setup();
  const res = await handle(device("POST", "/v1/approvals", { kind: "send_code", toName: "Pablo", lang: "en", onCall: true }));
  const { url } = (await res.json()) as { url: string };
  const page = await (await handle(new Request(url))).text();
  expect(page).toContain("They're on the phone, or just were, with a number they haven't saved.");
  expect(page).not.toMatch(/\d{4,}/);
  const calm = await newApproval(handle, "en");
  expect(await (await handle(new Request(calm.url))).text()).not.toContain("on the phone with a number");
});

test("an unknown decision is refused", async () => {
  const { handle } = setup();
  const a = await newApproval(handle);
  expect((await handle(decide(a.url, "maybe"))).status).toBe(400);
  expect(await statusOf(handle, a.id)).toBe("pending");
});
