/**
 * Small protections for a backend with public links: requests per IP in fixed windows, a block for whoever keeps
 * guessing the device token, and bodies read only up to a limit. All in memory: a restart forgives everyone.
 */

export interface Limit {
  max: number;
  windowMs: number;
}

/** Per IP: a guardian opens their page a few times; the phone polls an approval every 3 seconds. */
export const LIMITS = {
  /** The guardians' pages and approval links: `/a/…` and `/g/…`. */
  link: { max: 30, windowMs: 60_000 },
  /** The phone's API, `/v1/…`, with the device token. */
  device: { max: 240, windowMs: 60_000 },
  /** "Look at this picture": each one costs model credits. */
  picture: { max: 20, windowMs: 60 * 60_000 },
  /** "Report it": a scam written up for the police; each one costs model credits, and a person needs very few. */
  report: { max: 10, windowMs: 60 * 60_000 },
  /** Voice orders through Muse Voice Transcribe: a busy day is a few dozen. */
  voice: { max: 120, windowMs: 60 * 60_000 },
  /** Redeeming pairing codes: a family pairs a phone or two; guessing a code takes far more tries than this. */
  pair: { max: 10, windowMs: 15 * 60_000 },
  /** Wrong device tokens; after this many the IP waits out the window, even with the right one. */
  badToken: { max: 10, windowMs: 15 * 60_000 },
} satisfies Record<string, Limit>;

const MAX_KEYS = 10_000;

export class RateLimiter {
  private readonly windows = new Map<string, { start: number; count: number }>();

  constructor(private readonly now: () => number = Date.now) {}

  /** Counts one request; returns how many there were in the current window, this one included. */
  hit(key: string, limit: Limit): number {
    const w = this.current(key, limit);
    w.count++;
    return w.count;
  }

  /** How many requests the window already has, without counting a new one. */
  count(key: string, limit: Limit): number {
    return this.current(key, limit).count;
  }

  /** Seconds until the window for [key] starts again. */
  retryAfter(key: string, limit: Limit): number {
    const w = this.windows.get(key);
    return w ? Math.max(1, Math.ceil((w.start + limit.windowMs - this.now()) / 1000)) : 1;
  }

  private current(key: string, limit: Limit) {
    const at = this.now();
    let w = this.windows.get(key);
    if (!w || at - w.start >= limit.windowMs) {
      if (this.windows.size >= MAX_KEYS) this.sweep(at);
      w = { start: at, count: 0 };
      this.windows.set(key, w);
    }
    return w;
  }

  /** Forgets windows older than the longest limit, so the map never grows without bound. */
  private sweep(at: number) {
    const longest = Math.max(...Object.values(LIMITS).map((l) => l.windowMs));
    for (const [key, w] of this.windows) if (at - w.start >= longest) this.windows.delete(key);
    if (this.windows.size >= MAX_KEYS) this.windows.clear();
  }
}

/**
 * The request body as text, or "too_large" past [max] bytes. A declared length over the limit is refused
 * before reading; otherwise the stream is read only until it goes over.
 */
/**
 * The client's IP for the limits. Behind one trusted proxy (the tunnel, TRUST_PROXY=1) it is the LAST address in
 * X-Forwarded-For: that one the proxy added itself, while anything before it the client could have written to dodge
 * the limits. Without a trusted proxy the header is ignored and the socket's address counts.
 */
export function clientIp(forwardedFor: string | null, trustProxy: boolean, socketIp: string | undefined): string {
  const last = trustProxy ? forwardedFor?.split(",").at(-1)?.trim() : undefined;
  return last || socketIp || "unknown";
}

export async function readLimited(req: Request, max: number): Promise<string | "too_large"> {
  const bytes = await readBytesLimited(req, max);
  return bytes === "too_large" ? bytes : new TextDecoder().decode(bytes);
}

/** The same, as bytes: for audio. */
export async function readBytesLimited(req: Request, max: number): Promise<Uint8Array | "too_large"> {
  const declared = Number(req.headers.get("content-length") ?? "0");
  if (declared > max) return "too_large";
  if (!req.body) return new Uint8Array();
  const reader = req.body.getReader();
  const chunks: Uint8Array[] = [];
  let size = 0;
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    size += value.byteLength;
    if (size > max) {
      await reader.cancel();
      return "too_large";
    }
    chunks.push(value);
  }
  return new Uint8Array(Buffer.concat(chunks));
}
