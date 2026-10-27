import { expect, test } from "bun:test";
import { loadConfig } from "../src/config";
import { createHandler } from "../src/server";

const handle = createHandler(loadConfig({ DEVICE_TOKEN: "x".repeat(32) }));

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
