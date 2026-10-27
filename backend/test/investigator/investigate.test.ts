import { expect, test } from "bun:test";
import { investigate, MAX_ROUNDS, readInvestigation } from "../../src/investigator/investigate";
import { findCards } from "../../src/investigator/knowledge";
import { runTool, TOOLS } from "../../src/investigator/tools";
import { SparkClient } from "../../src/models/spark";
import { createHandler } from "../../src/server";

const doubtful = {
  level: "DOUBT",
  channel: "SMS",
  senderKnown: false,
  callActive: false,
  signals: ["UNKNOWN_SENDER", "LINK"],
  text: "Correos: tu paquete está retenido, paga 1,99 € en correos-envio.top",
  lang: "es",
  familyRelations: ["CHILD", "Ana", "GRANDCHILD"],
};

const call = (id: string, name: string, args: unknown) => ({ id, type: "function", function: { name, arguments: JSON.stringify(args) } });
const answer = (message: any) =>
  new Response(JSON.stringify({ choices: [{ message: { role: "assistant", ...message }, finish_reason: "stop" }], usage: {} }));

/** A model that replies with the given messages in turn, recording each request body. */
function scriptedModel(replies: any[]) {
  const bodies: any[] = [];
  const fn = (async (_url: string, init: RequestInit) => {
    bodies.push(JSON.parse(init.body as string));
    return answer(replies[Math.min(bodies.length - 1, replies.length - 1)]);
  }) as unknown as typeof fetch;
  return { client: new SparkClient({ baseUrl: "https://m/v1", apiKey: "k", fetch: fn }), bodies };
}

const rdap = (registered: string) =>
  (async (url: string) => {
    expect(url).toBe("https://rdap.org/domain/correos-envio.top");
    return new Response(JSON.stringify({ events: [{ eventAction: "registration", eventDate: registered }] }));
  }) as unknown as typeof fetch;

const NOW = Date.parse("2026-10-27T12:00:00Z");
const verdict = '{"verdict":"scam","confidence":1,"reason":"Ese enlace no es de Correos, no lo toques.","evidence":["El dominio se registró hace 3 días: https://x.top","Coincide con el timo del paquete retenido"]}';

test("only relation names reach the tools, never names", () => {
  const input = readInvestigation(doubtful);
  expect(typeof input).toBe("object");
  expect((input as any).familyRelations).toEqual(["CHILD", "GRANDCHILD"]);
  expect(readInvestigation({ ...doubtful, level: "SCAM" })).toBeString();
});

test("domain_age reads the registration date from RDAP", async () => {
  const out: any = await runTool("domain_age", '{"domain":"https://www.correos-envio.top/pago"}', {
    familyRelations: [],
    fetch: rdap("2026-10-24T08:00:00Z"),
    now: () => NOW,
  });
  expect(out).toEqual({ domain: "correos-envio.top", registered: true, registeredOn: "2026-10-24", ageDays: 3 });
});

test("domain_age refuses what is not a domain and survives the registry failing", async () => {
  expect(await runTool("domain_age", '{"domain":"not a domain; rm -rf"}', { familyRelations: [] })).toEqual({ error: "not a domain" });
  const down = (async () => {
    throw new Error("offline");
  }) as unknown as typeof fetch;
  expect(await runTool("domain_age", '{"domain":"example.com"}', { familyRelations: [], fetch: down })).toEqual({
    domain: "example.com",
    error: "registry not reachable",
  });
});

test("scam_patterns and family_has answer from local data", async () => {
  expect(findCards("parcel fee correos")[0].id).toBe("parcel_fee");
  expect(findCards("good morning")).toEqual([]);
  expect(await runTool("family_has", '{"relation":"SIBLING"}', { familyRelations: ["CHILD"] })).toEqual({ relation: "SIBLING", present: false });
  expect(await runTool("delete_contact", "{}", { familyRelations: [] })).toEqual({ error: "unknown tool delete_contact" });
});

test("the investigator looks things up, then judges, and evidence carries no links", async () => {
  const { client, bodies } = scriptedModel([
    { content: null, tool_calls: [call("1", "domain_age", { domain: "correos-envio.top" }), call("2", "scam_patterns", { topic: "correos paquete" })] },
    { content: verdict },
  ]);
  const result = await investigate(client, readInvestigation(doubtful) as any, { fetch: rdap("2026-10-24T08:00:00Z"), now: () => NOW });
  expect(result.verdict).toBe("scam");
  expect(result.delta).toBe(2);
  expect(result.toolCalls).toBe(2);
  expect(result.evidence).toEqual(["El dominio se registró hace 3 días:", "Coincide con el timo del paquete retenido"]);
  expect(bodies[0].tools.map((t: any) => t.function.name)).toEqual(TOOLS.map((t) => t.function.name));
  expect(bodies[0].messages[1].content).toContain("<mensaje>");
  const results = bodies[1].messages.filter((m: any) => m.role === "tool");
  expect(results.map((m: any) => m.tool_call_id)).toEqual(["1", "2"]);
  expect(JSON.parse(results[0].content).ageDays).toBe(3);
});

test("a model that keeps calling tools is cut off and must answer", async () => {
  const { client, bodies } = scriptedModel([{ content: null, tool_calls: [call("x", "scam_patterns", { topic: "bank" })] }]);
  const result = await investigate(client, readInvestigation(doubtful) as any);
  expect(bodies.length).toBe(MAX_ROUNDS + 1);
  expect(bodies.at(-1).tools).toBeUndefined();
  expect(result).toMatchObject({ verdict: "doubt", delta: 0, toolCalls: MAX_ROUNDS });
});

test("the route needs the device token and only takes doubtful messages", async () => {
  const config = { port: 0, deviceToken: "t".repeat(32), publicBaseUrl: "http://x" } as any;
  const { client } = scriptedModel([{ content: verdict }]);
  const handle = createHandler(config, { investigator: client });
  const post = (body: unknown, auth = `Bearer ${config.deviceToken}`) =>
    handle(new Request("http://x/v1/investigate", { method: "POST", headers: { authorization: auth }, body: JSON.stringify(body) }));
  expect((await post(doubtful, "Bearer nope")).status).toBe(401);
  expect((await post({ ...doubtful, level: "OK" })).status).toBe(400);
  const res = await post(doubtful);
  expect(res.status).toBe(200);
  expect(await res.json()).toMatchObject({ verdict: "scam", delta: 2, toolCalls: 0, model: "muse-spark-1.3" });
  expect((await createHandler(config)(new Request("http://x/v1/investigate", { method: "POST", headers: { authorization: `Bearer ${config.deviceToken}` }, body: JSON.stringify(doubtful) }))).status).toBe(503);
});
