import { expect, test } from "bun:test";
import { loadConfig } from "../src/config";
import { SparkOpenClient } from "../src/models/spark-open";
import { createHandler } from "../src/server";

const token = "x".repeat(32);
const config = loadConfig({ DEVICE_TOKEN: token });
const handle = createHandler(config);

const model = (content: string, status = 200) =>
  new SparkOpenClient({
    baseUrl: "https://b/v1",
    apiKey: "k",
    fetch: (async () =>
      new Response(JSON.stringify({ choices: [{ message: { role: "assistant", content }, finish_reason: "stop" }], usage: {} }), {
        status,
      })) as unknown as typeof fetch,
  });

const opinionRequest = (body: unknown, auth = `Bearer ${token}`) =>
  new Request("http://x/v1/opinion", {
    method: "POST",
    headers: { authorization: auth, "content-type": "application/json" },
    body: typeof body === "string" ? body : JSON.stringify(body),
  });

const doubtful = { level: "DOUBT", channel: "SMS", senderKnown: false, signals: ["UNKNOWN_SENDER"], text: "¿Me prestas 50 €?", lang: "es" };

test("health answers", async () => {
  const res = await handle(new Request("http://x/health"));
  expect(res.status).toBe(200);
  expect(await res.json()).toEqual({ ok: true, service: "friendlypear" });
});

test("unknown routes are 404", async () => {
  expect((await handle(new Request("http://x/nope"))).status).toBe(404);
});

test("a short device token is refused", () => {
  expect(() => loadConfig({ DEVICE_TOKEN: "short" })).toThrow();
});

test("second opinion needs the device token", async () => {
  const h = createHandler(config, { opinion: model('{"verdict":"ok","confidence":1}') });
  expect((await h(opinionRequest(doubtful, "Bearer wrong"))).status).toBe(401);
  expect((await h(opinionRequest(doubtful, ""))).status).toBe(401);
});

test("second opinion returns the bounded vote", async () => {
  const h = createHandler(config, { opinion: model('{"verdict":"scam","confidence":1,"reason":"Te pide dinero."}') });
  const res = await h(opinionRequest(doubtful));
  expect(res.status).toBe(200);
  expect(await res.json()).toEqual({ delta: 2, verdict: "scam", confidence: 1, reason: "Te pide dinero.", model: "meta-models/muse-spark-open" });
});

test("messages the rules already decided are refused", async () => {
  const h = createHandler(config, { opinion: model("{}") });
  expect((await h(opinionRequest({ ...doubtful, level: "OK" }))).status).toBe(400);
});

test("bad or huge bodies are refused", async () => {
  const h = createHandler(config, { opinion: model("{}") });
  expect((await h(opinionRequest("{nope"))).status).toBe(400);
  expect((await h(opinionRequest("x".repeat(9_000)))).status).toBe(413);
});

test("without a model the phone keeps its rules verdict", async () => {
  expect((await handle(opinionRequest(doubtful))).status).toBe(503);
});

test("a failing model is a 502 that never echoes the message", async () => {
  const h = createHandler(config, { opinion: model("boom", 500) });
  const res = await h(opinionRequest(doubtful));
  expect(res.status).toBe(502);
  expect(await res.text()).not.toContain("prestas");
});
