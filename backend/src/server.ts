import { timingSafeEqual } from "node:crypto";
import { askPage, resultPage } from "./approvals/page";
import { type ApprovalStore, KINDS, type Kind } from "./approvals/store";
import type { Config } from "./config";
import { guardianPage } from "./guardians/page";
import { type GuardianStore, readEvent } from "./guardians/store";
import { investigate, readInvestigation } from "./investigator/investigate";
import { readInput, secondOpinion } from "./investigator/opinion";
import { lookAtPicture, MAX_IMAGE_BASE64, readPicture } from "./investigator/picture";
import { LIMITS, RateLimiter, readLimited } from "./limits";
import type { ChatClient } from "./models/chat";
import type { SamClient } from "./models/sam";

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json", "cache-control": "no-store" } });

const html = (body: string, status = 200) =>
  new Response(body, {
    status,
    headers: {
      "content-type": "text/html; charset=utf-8",
      "cache-control": "no-store",
      "referrer-policy": "no-referrer",
      "x-frame-options": "DENY",
      "content-security-policy": "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'",
    },
  });

/** On every answer: no sniffing, no referrer, no embedding elsewhere, no camera or microphone; HSTS over HTTPS. */
function secure(res: Response, https: boolean): Response {
  const h = res.headers;
  h.set("x-content-type-options", "nosniff");
  h.set("referrer-policy", "no-referrer");
  h.set("cross-origin-resource-policy", "same-origin");
  h.set("cross-origin-opener-policy", "same-origin");
  h.set("permissions-policy", "camera=(), microphone=(), geolocation=(), interest-cohort=()");
  if (https) h.set("strict-transport-security", "max-age=31536000");
  return res;
}

const tooMany = (seconds: number, page: boolean) => {
  const res = page ? html(resultPage("es", "busy"), 429) : json({ error: "too_many_requests" }, 429);
  res.headers.set("retry-after", String(seconds));
  return res;
};

const MAX_BODY = 8 * 1024;
const NAME = /^[\p{L}][\p{L} .'-]{0,39}$/u;

/** What the backend can use; a missing piece means that layer is off and the phone decides alone. */
export interface Services {
  opinion?: ChatClient | null;
  /** Muse Spark with read-only tools, for messages still doubtful after the second opinion. */
  investigator?: ChatClient | null;
  /** "Look at this picture": SAM 3.1 marks what Muse Spark found risky. */
  sam?: SamClient | null;
  approvals?: ApprovalStore | null;
  /** What guardians see, only when the person chose to share it. */
  guardians?: GuardianStore | null;
}

function fromDevice(req: Request, config: Config): boolean {
  const given = Buffer.from(req.headers.get("authorization") ?? "");
  const expected = Buffer.from(`Bearer ${config.deviceToken}`);
  return given.length === expected.length && timingSafeEqual(given, expected);
}

async function readJson(req: Request, max = MAX_BODY): Promise<unknown | Response> {
  const raw = await readLimited(req, max);
  if (raw === "too_large") return json({ error: "too_large" }, 413);
  try {
    return JSON.parse(raw);
  } catch {
    return json({ error: "bad_json" }, 400);
  }
}

/**
 * Every request goes through the limits first, per client IP: the public links (`/a/`, `/g/`) and the phone's
 * API (`/v1/`) have their own, and an IP that keeps sending wrong device tokens waits out a 15-minute window.
 */
export function createHandler(config: Config, services: Services = {}, limiter = new RateLimiter()) {
  const https = config.publicBaseUrl.startsWith("https://");
  const route = routes(config, services);
  return async function handle(req: Request, ip = "local"): Promise<Response> {
    const { pathname } = new URL(req.url);
    const page = pathname.startsWith("/a/") || pathname.startsWith("/g/");
    const api = pathname.startsWith("/v1/");
    if (api && limiter.count(`token:${ip}`, LIMITS.badToken) >= LIMITS.badToken.max) {
      return secure(tooMany(limiter.retryAfter(`token:${ip}`, LIMITS.badToken), false), https);
    }
    const limit = page ? LIMITS.link : api ? LIMITS.device : null;
    if (limit) {
      const key = `${page ? "link" : "device"}:${ip}`;
      if (limiter.hit(key, limit) > limit.max) return secure(tooMany(limiter.retryAfter(key, limit), page), https);
    }
    // Pictures cost model credits: a few an hour is plenty for a person.
    if (pathname === "/v1/picture" && limiter.hit(`picture:${ip}`, LIMITS.picture) > LIMITS.picture.max) {
      return secure(tooMany(limiter.retryAfter(`picture:${ip}`, LIMITS.picture), false), https);
    }
    const res = await route(req, pathname);
    if (api && res.status === 401) limiter.hit(`token:${ip}`, LIMITS.badToken);
    return secure(res, https);
  };
}

/** Routes are added by module: approvals/, guardians/, investigator/ and models/ each have their own. */
function routes(config: Config, services: Services) {
  return async function route(req: Request, pathname: string): Promise<Response> {
    if (req.method === "GET" && pathname === "/health") return json({ ok: true, service: "friendlypear" });

    if (req.method === "POST" && pathname === "/v1/opinion") {
      if (!fromDevice(req, config)) return json({ error: "unauthorized" }, 401);
      const body = await readJson(req);
      if (body instanceof Response) return body;
      const input = readInput(body);
      if (typeof input === "string") return json({ error: "bad_request", detail: input }, 400);
      if (!services.opinion) return json({ error: "no_model" }, 503);
      try {
        const { delta, verdict, confidence, reason, model } = await secondOpinion(services.opinion, input);
        return json({ delta, verdict, confidence, reason, model });
      } catch {
        // The message text is never logged or echoed back.
        return json({ error: "model_failed" }, 502);
      }
    }

    if (req.method === "POST" && pathname === "/v1/investigate") {
      if (!fromDevice(req, config)) return json({ error: "unauthorized" }, 401);
      const body = await readJson(req);
      if (body instanceof Response) return body;
      const input = readInvestigation(body);
      if (typeof input === "string") return json({ error: "bad_request", detail: input }, 400);
      if (!services.investigator) return json({ error: "no_model" }, 503);
      try {
        const { delta, verdict, confidence, reason, evidence, toolCalls, model } = await investigate(services.investigator, input);
        return json({ delta, verdict, confidence, reason, evidence, toolCalls, model });
      } catch {
        return json({ error: "model_failed" }, 502);
      }
    }

    // "Look at this picture": a photo or screenshot the person chose to show Peri. It is never stored or logged.
    if (req.method === "POST" && pathname === "/v1/picture") {
      if (!fromDevice(req, config)) return json({ error: "unauthorized" }, 401);
      const body = await readJson(req, MAX_IMAGE_BASE64 + 1024);
      if (body instanceof Response) return body;
      const input = readPicture(body);
      if (typeof input === "string") return json({ error: "bad_request", detail: input }, 400);
      if (!services.investigator) return json({ error: "no_model" }, 503);
      try {
        const { verdict, confidence, kinds, marks, model } = await lookAtPicture(services.investigator, services.sam ?? null, input);
        return json({ verdict, confidence, kinds, marks, model });
      } catch {
        return json({ error: "model_failed" }, 502);
      }
    }

    const approvals = services.approvals;

    // The phone asks for a guardian's approval and gets the one-time link to text them.
    if (req.method === "POST" && pathname === "/v1/approvals") {
      if (!fromDevice(req, config)) return json({ error: "unauthorized" }, 401);
      if (!approvals) return json({ error: "no_approvals" }, 503);
      const body = (await readJson(req)) as any;
      if (body instanceof Response) return body;
      if (!KINDS.includes(body?.kind)) return json({ error: "bad_request", detail: "unknown kind" }, 400);
      if (typeof body.toName !== "string" || !NAME.test(body.toName)) return json({ error: "bad_request", detail: "bad name" }, 400);
      const lang = body.lang === "en" ? "en" : "es";
      const { approval, token } = approvals.create(body.kind as Kind, body.toName, lang);
      return json({ id: approval.id, url: `${config.publicBaseUrl}/a/${token}`, expiresAt: approval.expiresAt }, 201);
    }

    // The phone polls until the guardian decides or the link expires.
    const status = pathname.match(/^\/v1\/approvals\/([A-Za-z0-9_-]{8,40})$/);
    if (req.method === "GET" && status) {
      if (!fromDevice(req, config)) return json({ error: "unauthorized" }, 401);
      const a = approvals?.get(status[1]);
      if (!a) return json({ error: "not_found" }, 404);
      return json({ status: a.status });
    }

    const guardians = services.guardians;

    // The phone shares a guardian's personal link, when the person turns sharing on.
    if (req.method === "POST" && pathname === "/v1/guardians/links") {
      if (!fromDevice(req, config)) return json({ error: "unauthorized" }, 401);
      if (!guardians) return json({ error: "no_guardians" }, 503);
      const body = (await readJson(req)) as any;
      if (body instanceof Response) return body;
      if (typeof body?.name !== "string" || !NAME.test(body.name)) return json({ error: "bad_request", detail: "bad name" }, 400);
      const { token } = guardians.createLink(body.name, body.lang === "en" ? "en" : "es");
      return json({ url: `${config.publicBaseUrl}/g/${token}` }, 201);
    }

    // Turning sharing off: every guardian link stops working and the news is forgotten.
    if (req.method === "DELETE" && pathname === "/v1/guardians/links") {
      if (!fromDevice(req, config)) return json({ error: "unauthorized" }, 401);
      guardians?.revokeAll();
      return json({ ok: true });
    }

    // What happened, never what it was about: a warning's level and channel, or a pause and its answer.
    if (req.method === "POST" && pathname === "/v1/events") {
      if (!fromDevice(req, config)) return json({ error: "unauthorized" }, 401);
      if (!guardians) return json({ error: "no_guardians" }, 503);
      const body = await readJson(req);
      if (body instanceof Response) return body;
      const event = readEvent(body);
      if (typeof event === "string") return json({ error: "bad_request", detail: event }, 400);
      guardians.add(event);
      return json({ ok: true }, 201);
    }

    // The guardian's page: the link is the secret, kept until the person stops sharing.
    const news = pathname.match(/^\/g\/([A-Za-z0-9_-]{20,64})$/);
    if (req.method === "GET" && news && guardians) {
      const link = guardians.byLink(news[1]);
      if (!link) return html(resultPage("es", "unknown"), 404);
      return html(guardianPage(link, guardians.recent(), approvals?.recent() ?? [], Date.now()));
    }

    // The guardian's link: no device token, the link itself is the secret, and it works once.
    const link = pathname.match(/^\/a\/([A-Za-z0-9_-]{20,64})$/);
    if (link && approvals) {
      const token = link[1];
      // Audit trail without the secret: who decided is a browser, so we keep the method and its user agent.
      console.log(`guardian link ${req.method} ua=${(req.headers.get("user-agent") ?? "-").slice(0, 80)}`);
      if (req.method === "GET") {
        const a = approvals.byLink(token);
        if (!a) return html(resultPage("es", "unknown"), 404);
        if (a.status === "expired") return html(resultPage(a.lang, "expired"), 410);
        if (a.status !== "pending") return html(resultPage(a.lang, "used"), 410);
        return html(askPage(a, token, Date.now()));
      }
      if (req.method === "POST") {
        const raw = await readLimited(req, 256);
        if (raw === "too_large") return html(resultPage("es", "unknown"), 413);
        const decision = new URLSearchParams(raw).get("decision");
        if (decision !== "approve" && decision !== "reject") return html(resultPage("es", "unknown"), 400);
        const lang = approvals.byLink(token)?.lang ?? "es";
        const result = approvals.decide(token, decision === "approve");
        if (typeof result === "string") return html(resultPage(lang, result), result === "unknown" ? 404 : 410);
        return html(resultPage(result.lang, result.status === "approved" ? "approved" : "rejected"));
      }
    }

    return json({ error: "not_found" }, 404);
  };
}
