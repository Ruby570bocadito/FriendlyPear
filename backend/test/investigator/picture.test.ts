import { expect, test } from "bun:test";
import { loadConfig } from "../../src/config";
import { lookAtPicture, MAX_IMAGE_BASE64, parseVerdict, readPicture } from "../../src/investigator/picture";
import { parseBoxes, SamClient } from "../../src/models/sam";
import { SparkClient } from "../../src/models/spark";
import { createHandler } from "../../src/server";

const token = "x".repeat(32);
const picture = { image: Buffer.from("fake jpeg bytes").toString("base64"), mime: "image/jpeg", lang: "es" };
const samText = "<0f>0<|box;x1=299;y1=820;x2=590;y2=1110;w=900;h=1200|><|mask;x=0;y=0;data=291,292,~!!#iv|>\n";

function fakeSpark(content: string) {
  const bodies: any[] = [];
  const fn = (async (_url: string, init: RequestInit) => {
    bodies.push(JSON.parse(init.body as string));
    return new Response(JSON.stringify({ choices: [{ message: { role: "assistant", content }, finish_reason: "stop" }], usage: {} }));
  }) as unknown as typeof fetch;
  return { client: new SparkClient({ baseUrl: "https://m/v1", apiKey: "k", fetch: fn }), bodies };
}

function fakeSam(text = samText) {
  const phrases: string[] = [];
  const fn = (async (_url: string, init: RequestInit) => {
    const body = JSON.parse(init.body as string);
    phrases.push(body.input[0].content[0].text);
    expect(body.input[0].content[1].image_url).toStartWith("data:image/jpeg;base64,");
    return new Response(JSON.stringify({ output: [{ type: "message", content: [{ type: "output_text", text }] }] }));
  }) as unknown as typeof fetch;
  return { client: new SamClient({ baseUrl: "https://m/v1", apiKey: "k", fetch: fn }), phrases };
}

test("SAM's boxes come back as fractions of the picture, and odd records are skipped", () => {
  expect(parseBoxes(samText)).toEqual([{ x1: 299 / 900, y1: 820 / 1200, x2: 590 / 900, y2: 1110 / 1200, confidence: null }]);
  expect(parseBoxes("<|box;x1=10;y1=10;x2=5;y2=20;w=100;h=100|>")).toEqual([]);
  expect(parseBoxes("<|box;x1=1;y1=2;x2=3;y2=4;w=10;h=10;c=0.9|>")[0].confidence).toBe(0.9);
  expect(parseBoxes("")).toEqual([]);
});

test("only known kinds are kept, and anything odd is a doubt with nothing to mark", () => {
  expect(parseVerdict('{"verdict":"scam","confidence":0.9,"kinds":["qr","link","qr","drop tables"]}')).toEqual({
    verdict: "scam",
    confidence: 0.9,
    kinds: ["qr", "link"],
  });
  expect(parseVerdict("I think so")).toEqual({ verdict: "doubt", confidence: 0, kinds: [] });
});

test("the phone may only send a picture of a known type and size", () => {
  expect(readPicture(picture)).toMatchObject({ mime: "image/jpeg", lang: "es" });
  expect(readPicture({ ...picture, mime: "image/svg+xml" })).toBeString();
  expect(readPicture({ ...picture, image: "not base64!" })).toBeString();
  expect(readPicture({ ...picture, image: "A".repeat(MAX_IMAGE_BASE64 + 4) })).toBeString();
});

test("a scam letter: SAM marks the QR with a fixed phrase, the rest is only named, the picture is never cached", async () => {
  const spark = fakeSpark('{"verdict":"scam","confidence":0.95,"kinds":["payment","qr","link"]}');
  const sam = fakeSam();
  const result = await lookAtPicture(spark.client, sam.client, readPicture(picture) as any);
  expect(result.verdict).toBe("scam");
  expect(sam.phrases).toEqual(["QR code"]);
  expect(result.marks.map((m) => m.kind)).toEqual(["qr"]);
  expect(result.kinds).toEqual(["payment", "qr", "link"]);
  const user = spark.bodies[0].messages[1];
  expect(user.content[1].image_url.url).toStartWith("data:image/jpeg;base64,");
  await lookAtPicture(spark.client, sam.client, readPicture(picture) as any);
  expect(spark.bodies.length).toBe(2);
});

test("an ordinary letter gets no marks and no SAM requests", async () => {
  const sam = fakeSam();
  const result = await lookAtPicture(fakeSpark('{"verdict":"ok","confidence":0.8,"kinds":["phone"]}').client, sam.client, readPicture(picture) as any);
  expect(result.marks).toEqual([]);
  expect(sam.phrases).toEqual([]);
});

test("the route needs the device token and takes a picture bigger than other bodies", async () => {
  const config = loadConfig({ DEVICE_TOKEN: token, PUBLIC_BASE_URL: "http://x" });
  const handle = createHandler(config, { investigator: fakeSpark('{"verdict":"scam","confidence":1,"kinds":["qr"]}').client, sam: fakeSam().client });
  const post = (body: unknown, auth = `Bearer ${token}`) =>
    handle(new Request("http://x/v1/picture", { method: "POST", headers: { authorization: auth }, body: JSON.stringify(body) }));
  expect((await post(picture, "Bearer nope")).status).toBe(401);
  const big = { ...picture, image: "A".repeat(100_000) };
  const res = await post(big);
  expect(res.status).toBe(200);
  expect(await res.json()).toMatchObject({ verdict: "scam", kinds: ["qr"], marks: [{ kind: "qr" }] });
});
