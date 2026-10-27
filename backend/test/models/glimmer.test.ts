import { expect, test } from "bun:test";
import { GlimmerClient, basetenUrl, glimmerFromEnv } from "../../src/models/glimmer";

const reply = {
  choices: [{ message: { role: "assistant", content: "DUDOSO" }, finish_reason: "stop" }],
  usage: { prompt_tokens: 30, completion_tokens: 50 },
};

function capture() {
  const bodies: any[] = [];
  const urls: string[] = [];
  const fn = (async (url: string, init: RequestInit) => {
    urls.push(url);
    bodies.push(JSON.parse(init.body as string));
    return new Response(JSON.stringify(reply));
  }) as unknown as typeof fetch;
  return { fn, bodies, urls };
}

test("sends the recommended sampling and no reasoning_effort", async () => {
  const { fn, bodies } = capture();
  await new GlimmerClient({ baseUrl: "https://g/v1", apiKey: "k", fetch: fn }).chat({ messages: [{ role: "user", content: "x" }] });
  expect(bodies[0]).toMatchObject({ model: "meta-models/Muse-Glimmer-30B", temperature: 1.0, top_p: 0.95, top_k: 64 });
  expect(bodies[0].reasoning_effort).toBeUndefined();
});

test("instructions go as a system message that starts with the reasoning strength", async () => {
  const { fn, bodies } = capture();
  await new GlimmerClient({ baseUrl: "https://g/v1", apiKey: "k", fetch: fn }).chat({
    messages: [
      { role: "developer", content: "Eres el escudo." },
      { role: "user", content: "x" },
    ],
  });
  expect(bodies[0].messages[0]).toEqual({ role: "system", content: "Reasoning strength: low\nEres el escudo." });
  expect(bodies[0].messages[1]).toEqual({ role: "user", content: "x" });
});

test("without instructions the reasoning strength is its own system message", async () => {
  const { fn, bodies } = capture();
  await new GlimmerClient({ baseUrl: "https://g/v1", apiKey: "k", fetch: fn, reasoning: "medium" }).chat({
    messages: [{ role: "user", content: "x" }],
  });
  expect(bodies[0].messages[0]).toEqual({ role: "system", content: "Reasoning strength: medium" });
});

test("builds the Baseten production URL from the model id", () => {
  expect(basetenUrl("abc123")).toBe("https://model-abc123.api.baseten.co/environments/production/sync/v1");
});

test("from env: needs a URL and a key; GLIMMER_BASE_URL wins over Baseten", async () => {
  expect(glimmerFromEnv({})).toBeNull();
  expect(glimmerFromEnv({ BASETEN_MODEL_ID: "abc" })).toBeNull();
  expect(glimmerFromEnv({ BASETEN_MODEL_ID: "abc", BASETEN_API_KEY: "k" })).toBeInstanceOf(GlimmerClient);
  expect(glimmerFromEnv({ GLIMMER_BASE_URL: "http://localhost:8000/v1", GLIMMER_API_KEY: "k", BASETEN_MODEL_ID: "abc" })).toBeInstanceOf(
    GlimmerClient,
  );
});
