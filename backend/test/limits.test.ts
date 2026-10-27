import { expect, test } from "bun:test";
import { loadConfig } from "../src/config";
import { GuardianStore } from "../src/guardians/store";
import { clientIp, LIMITS, RateLimiter, readLimited } from "../src/limits";
import { createHandler } from "../src/server";

const token = "x".repeat(32);

function setup(base = "http://10.0.2.2:8711") {
  let now = 1_000_000;
  const config = loadConfig({ DEVICE_TOKEN: token, PUBLIC_BASE_URL: base });
  const handle = createHandler(config, { guardians: new GuardianStore(() => now) }, new RateLimiter(() => now));
  return { handle, advance: (ms: number) => (now += ms) };
}

const page = (ip: string) => (h: ReturnType<typeof setup>["handle"]) => h(new Request(`http://x/g/${"a".repeat(43)}`), ip);
const event = (auth: string) =>
  new Request("http://x/v1/events", {
    method: "POST",
    headers: { authorization: auth, "content-type": "application/json" },
    body: JSON.stringify({ kind: "pause", answer: "leave" }),
  });

test("someone trying guardian links gets slowed down, per IP, and is let back in after a minute", async () => {
  const { handle, advance } = setup();
  for (let i = 0; i < LIMITS.link.max; i++) expect((await page("1.2.3.4")(handle)).status).toBe(404);
  const blocked = await page("1.2.3.4")(handle);
  expect(blocked.status).toBe(429);
  expect(Number(blocked.headers.get("retry-after"))).toBeGreaterThan(0);
  expect(await blocked.text()).toContain("Demasiados intentos");
  expect((await page("5.6.7.8")(handle)).status).toBe(404);
  advance(LIMITS.link.windowMs);
  expect((await page("1.2.3.4")(handle)).status).toBe(404);
});

test("an IP that keeps guessing the device token is shut out, even with the right one", async () => {
  const { handle, advance } = setup();
  for (let i = 0; i < LIMITS.badToken.max; i++) expect((await handle(event("Bearer wrong"), "9.9.9.9")).status).toBe(401);
  expect((await handle(event(`Bearer ${token}`), "9.9.9.9")).status).toBe(429);
  expect((await handle(event(`Bearer ${token}`), "8.8.8.8")).status).toBe(201);
  advance(LIMITS.badToken.windowMs);
  expect((await handle(event(`Bearer ${token}`), "9.9.9.9")).status).toBe(201);
});

test("every answer carries the security headers; HSTS only over HTTPS", async () => {
  const local = await setup().handle(new Request("http://x/health"));
  expect(local.headers.get("x-content-type-options")).toBe("nosniff");
  expect(local.headers.get("referrer-policy")).toBe("no-referrer");
  expect(local.headers.get("permissions-policy")).toContain("microphone=()");
  expect(local.headers.get("strict-transport-security")).toBeNull();
  const html = await page("1.1.1.1")(setup("https://peri.example").handle);
  expect(html.headers.get("strict-transport-security")).toContain("max-age=");
  expect(html.headers.get("content-security-policy")).toContain("frame-ancestors 'none'");
});

test("bodies past the limit are refused, whether the length is declared or not", async () => {
  const big = "x".repeat(20_000);
  const declared = new Request("http://x/v1/events", { method: "POST", headers: { authorization: `Bearer ${token}` }, body: big });
  expect((await setup().handle(declared)).status).toBe(413);
  const stream = new ReadableStream({
    start(c) {
      for (let i = 0; i < 5; i++) c.enqueue(new TextEncoder().encode(big));
      c.close();
    },
  });
  expect(await readLimited(new Request("http://x", { method: "POST", body: stream }), 8 * 1024)).toBe("too_large");
  expect(await readLimited(new Request("http://x", { method: "POST", body: "decision=approve" }), 256)).toBe("decision=approve");
});

test("behind the tunnel the client's IP is the one the proxy added, so a forged header can't dodge the limits", () => {
  // A client sends its own X-Forwarded-For; the proxy appends the real address at the end.
  expect(clientIp("1.2.3.4, 203.0.113.9", true, "127.0.0.1")).toBe("203.0.113.9");
  expect(clientIp("203.0.113.9", true, "127.0.0.1")).toBe("203.0.113.9");
  expect(clientIp(null, true, "127.0.0.1")).toBe("127.0.0.1");
  // Without a trusted proxy the header means nothing.
  expect(clientIp("1.2.3.4", false, "198.51.100.7")).toBe("198.51.100.7");
  expect(clientIp(null, false, undefined)).toBe("unknown");
});
