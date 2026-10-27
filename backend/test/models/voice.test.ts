import { expect, test } from "bun:test";
import { loadConfig } from "../../src/config";
import { isVoiceWav, VoiceClient } from "../../src/models/voice";
import { createHandler } from "../../src/server";

const token = "x".repeat(32);

/** A WAV header for mono 16-bit PCM, plus a little silence. */
function wav(channels = 1, bits = 16): Uint8Array {
  const data = new Uint8Array(3200);
  const b = new Uint8Array(44 + data.length);
  const v = new DataView(b.buffer);
  const put = (at: number, s: string) => [...s].forEach((c, i) => (b[at + i] = c.charCodeAt(0)));
  put(0, "RIFF");
  v.setUint32(4, 36 + data.length, true);
  put(8, "WAVE");
  put(12, "fmt ");
  v.setUint32(16, 16, true);
  v.setUint16(20, 1, true);
  v.setUint16(22, channels, true);
  v.setUint32(24, 16000, true);
  v.setUint32(28, 16000 * channels * (bits / 8), true);
  v.setUint16(32, channels * (bits / 8), true);
  v.setUint16(34, bits, true);
  put(36, "data");
  v.setUint32(40, data.length, true);
  return b;
}

function fakeVoice(transcript = "Oye Peri, llama a mi hija, por favor.") {
  const sent: { request: any; audio: Blob | null }[] = [];
  const fn = (async (_url: string, init: RequestInit) => {
    const form = init.body as FormData;
    sent.push({ request: JSON.parse(await (form.get("request") as Blob).text()), audio: form.get("audio") as Blob });
    return new Response(JSON.stringify({ sessionId: "s", transcript, audioDurationMs: 3680, turns: [] }));
  }) as unknown as typeof fetch;
  return { client: new VoiceClient({ baseUrl: "https://m/v1", apiKey: "k", fetch: fn }), sent };
}

test("only a mono 16-bit PCM WAV is accepted", () => {
  expect(isVoiceWav(wav())).toBe(true);
  expect(isVoiceWav(wav(2))).toBe(false);
  expect(isVoiceWav(wav(1, 8))).toBe(false);
  expect(isVoiceWav(new TextEncoder().encode("not audio at all, just some text that is long enough"))).toBe(false);
});

test("the clip goes as multipart with the push-to-talk settings, and the transcript comes back", async () => {
  const v = fakeVoice();
  expect(await v.client.transcribe(wav())).toEqual({ text: "Oye Peri, llama a mi hija, por favor.", durationMs: 3680 });
  expect(v.sent[0].request).toEqual({ model: "muse-voice-transcribe-1.0", audioEncoding: "WAV", mode: "PUSH_TO_TALK" });
  expect(v.sent[0].audio?.type).toBe("audio/wav");
});

test("the route needs the device token, a real WAV and a small body", async () => {
  const config = loadConfig({ DEVICE_TOKEN: token, PUBLIC_BASE_URL: "http://x" });
  const handle = createHandler(config, { voice: fakeVoice().client });
  const post = (body: Uint8Array, auth = `Bearer ${token}`) =>
    handle(new Request("http://x/v1/transcribe", { method: "POST", headers: { authorization: auth, "content-type": "audio/wav" }, body }));
  expect((await post(wav(), "Bearer nope")).status).toBe(401);
  expect((await post(new TextEncoder().encode("hello hello hello hello hello hello hello hello"))).status).toBe(400);
  expect((await post(new Uint8Array(1_200_000))).status).toBe(413);
  const ok = await post(wav());
  expect(ok.status).toBe(200);
  expect(await ok.json()).toEqual({ text: "Oye Peri, llama a mi hija, por favor.", model: "muse-voice-transcribe-1.0" });
  expect((await createHandler(config)(new Request("http://x/v1/transcribe", { method: "POST", headers: { authorization: `Bearer ${token}` }, body: wav() }))).status).toBe(503);
});
