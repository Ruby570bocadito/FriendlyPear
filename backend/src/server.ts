import { timingSafeEqual } from "node:crypto";
import { askPage, confirmPage, resultPage } from "./approvals/page";
import { asset } from "./assets";
import { CODE, type DeviceStore } from "./devices/store";
import { type ApprovalStore, DECISIONS, type Decision, KINDS, type Kind } from "./approvals/store";
import type { Config } from "./config";
import { guardianPage } from "./guardians/page";
import { type GuardianStore, readEvent } from "./guardians/store";
import { investigate, readInvestigation } from "./investigator/investigate";
import { type PearStore, readPear } from "./pears/store";
import { ANSWERS, type CheckStore, KINDS as CHECK_KINDS } from "./checks/store";
import { readInput, secondOpinion } from "./investigator/opinion";
import { lookAtPicture, MAX_IMAGE_BASE64, readPicture } from "./investigator/picture";
import { readReport, writeReport } from "./investigator/report";
import { LIMITS, RateLimiter, readBytesLimited, readLimited } from "./limits";
import type { ChatClient } from "./models/chat";
import type { SamClient } from "./models/sam";
import { isVoiceWav, type VoiceClient } from "./models/voice";

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
      "content-security-policy": "default-src 'none'; style-src 'unsafe-inline'; img-src 'self'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'",
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
/** About 30 seconds of 16 kHz mono 16-bit audio: far more than a voice order needs. */
const MAX_AUDIO = 1_000_000;
const NAME = /^[\p{L}][\p{L} .'-]{0,39}$/u;

/** What the backend can use; a missing piece means that layer is off and the phone decides alone. */
export interface Services {
  opinion?: ChatClient | null;
  /** Muse Spark with read-only tools, for messages still doubtful after the second opinion. */
  investigator?: ChatClient | null;
  /** "Look at this picture": SAM 3.1 marks what Muse Spark found risky. */
  sam?: SamClient | null;
  /** Muse Voice Transcribe, only when the person turned it on in Settings. */
  voice?: VoiceClient | null;
  approvals?: ApprovalStore | null;
  /** What guardians see, only when the person chose to share it. */
  guardians?: GuardianStore | null;
  /** Phones paired one by one, each with its own key. */
  devices?: DeviceStore | null;
  /** Each family phone's pear (name, outfit, role, calm or attention), for the family section. */
  pears?: PearStore | null;
  /** "Was it you?": questions between the family's phones about messages from new numbers. */
  checks?: CheckStore | null;
}

function sameSecret(req: Request, secret: string): boolean {
  const given = Buffer.from(req.headers.get("authorization") ?? "");
  const expected = Buffer.from(`Bearer ${secret}`);
  return given.length === expected.length && timingSafeEqual(given, expected);
}

/** The paired phone behind the request's own key; never the shared test token, which is no one phone. */
function ownDevice(req: Request, devices?: DeviceStore | null) {
  const bearer = (req.headers.get("authorization") ?? "").match(/^Bearer ([A-Za-z0-9_-]{32,64})$/)?.[1];
  return bearer ? (devices?.check(bearer) ?? null) : null;
}

/** A paired phone's own key, or the shared test token while it is still allowed. */
function fromDevice(req: Request, config: Config, devices?: DeviceStore | null): boolean {
  const bearer = (req.headers.get("authorization") ?? "").match(/^Bearer ([A-Za-z0-9_-]{32,64})$/)?.[1];
  if (bearer && devices?.check(bearer)) return true;
  return config.allowSharedToken && sameSecret(req, config.deviceToken);
}

const fromAdmin = (req: Request, config: Config) => sameSecret(req, config.adminToken);

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
    let pathname: string;
    try {
      pathname = new URL(req.url).pathname;
    } catch {
      // A request whose address can't be read (no Host header, say) is the client's mistake, not ours.
      return secure(json({ error: "bad_request" }, 400), https);
    }
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
    if (pathname === "/v1/devices" && req.method === "POST" && limiter.hit(`pair:${ip}`, LIMITS.pair) > LIMITS.pair.max) {
      return secure(tooMany(limiter.retryAfter(`pair:${ip}`, LIMITS.pair), false), https);
    }
    if (pathname === "/v1/transcribe" && limiter.hit(`voice:${ip}`, LIMITS.voice) > LIMITS.voice.max) {
      return secure(tooMany(limiter.retryAfter(`voice:${ip}`, LIMITS.voice), false), https);
    }
    // Pictures cost model credits: a few an hour is plenty for a person.
    if (pathname === "/v1/picture" && limiter.hit(`picture:${ip}`, LIMITS.picture) > LIMITS.picture.max) {
      return secure(tooMany(limiter.retryAfter(`picture:${ip}`, LIMITS.picture), false), https);
    }
    if (pathname === "/v1/report" && limiter.hit(`report:${ip}`, LIMITS.report) > LIMITS.report.max) {
      return secure(tooMany(limiter.retryAfter(`report:${ip}`, LIMITS.report), false), https);
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

    const devices = services.devices;

    // `bun pair` on the server: a one-time code, and the link its QR carries for the phone's camera.
    if (req.method === "POST" && pathname === "/v1/pairing") {
      if (!fromAdmin(req, config)) return json({ error: "unauthorized" }, 401);
      if (!devices) return json({ error: "no_devices" }, 503);
      const { code, expiresAt } = devices.newCode();
      const link = `friendlypear://pair?code=${code}&server=${encodeURIComponent(config.publicBaseUrl)}`;
      return json({ code, expiresAt, link }, 201);
    }

    // The phone redeems its code once and gets its own key, shown only this time.
    if (req.method === "POST" && pathname === "/v1/devices") {
      if (!devices) return json({ error: "no_devices" }, 503);
      const body = (await readJson(req)) as any;
      if (body instanceof Response) return body;
      const code = typeof body?.code === "string" ? body.code.trim().toUpperCase() : "";
      const name = typeof body?.name === "string" ? body.name.trim().slice(0, 40) : "";
      if (!CODE.test(code) || !/^[\p{L}\p{N} ._()-]{1,40}$/u.test(name)) return json({ error: "bad_request" }, 400);
      const paired = devices.redeem(code, name);
      if (!paired) return json({ error: "unknown_code" }, 403);
      return json({ id: paired.device.id, key: paired.key }, 201);
    }

    // A phone disconnects itself (Settings): its own key stops working.
    if (req.method === "DELETE" && pathname === "/v1/devices/self") {
      const me = ownDevice(req, devices);
      if (!me) return json({ error: "unauthorized" }, 401);
      devices!.revoke(me.id);
      services.pears?.forget(me.id);
      services.checks?.forget(me.id);
      return json({ ok: true });
    }

    // The family sees and disconnects the paired phones.
    if (pathname === "/v1/devices" && req.method === "GET") {
      if (!fromAdmin(req, config)) return json({ error: "unauthorized" }, 401);
      return json({ devices: devices?.list() ?? [] });
    }
    const one = pathname.match(/^\/v1\/devices\/([A-Za-z0-9_-]{8,20})$/);
    if (one && req.method === "DELETE") {
      if (!fromAdmin(req, config)) return json({ error: "unauthorized" }, 401);
      services.pears?.forget(one[1]);
      services.checks?.forget(one[1]);
      return devices?.revoke(one[1]) ? json({ ok: true }) : json({ error: "not_found" }, 404);
    }

    // The family's pears: each paired phone sends its own and sees everyone's. A pear belongs to one phone, so only
    // a phone's own key works here, never the shared test token.
    if (pathname === "/v1/pears/self" && req.method === "PUT") {
      const me = ownDevice(req, devices);
      if (!me) return json({ error: "unauthorized" }, 401);
      if (!services.pears) return json({ error: "no_pears" }, 503);
      const body = await readJson(req);
      if (body instanceof Response) return body;
      const input = readPear(body);
      if (typeof input === "string") return json({ error: "bad_request", detail: input }, 400);
      if (!services.pears.put(me.id, input)) return json({ error: "too_many" }, 409);
      return json({ ok: true });
    }
    if (pathname === "/v1/pears" && req.method === "GET") {
      const me = ownDevice(req, devices);
      if (!me) return json({ error: "unauthorized" }, 401);
      if (!services.pears) return json({ error: "no_pears" }, 503);
      const paired = new Set(devices!.list().map((d) => d.id));
      const listed = services.pears.list().filter((p) => paired.has(p.device));
      const pears = listed.map(({ device, name, outfit, role, state, kind, updatedAt }) => ({ id: device, name, outfit, role, state, kind, updatedAt, self: device === me.id }));
      return json({ pears, campaign: services.pears.campaignAmong(paired) });
    }

    // "Was it you?": a phone asks the rest of the family about a message from a new number; each answers once.
    if (pathname.startsWith("/v1/checks")) {
      const me = ownDevice(req, devices);
      if (!me) return json({ error: "unauthorized" }, 401);
      const checks = services.checks;
      if (!checks) return json({ error: "no_checks" }, 503);
      const paired = new Set(devices!.list().map((d) => d.id));
      const names = new Map((services.pears?.list() ?? []).map((p) => [p.device, p.name]));
      if (pathname === "/v1/checks" && req.method === "POST") {
        const body = (await readJson(req)) as any;
        if (body instanceof Response) return body;
        if (!CHECK_KINDS.includes(body?.kind)) return json({ error: "bad_request", detail: "kind" }, 400);
        const check = checks.ask(me.id, body.kind);
        if (!check) return json({ error: "too_many" }, 409);
        return json({ id: check.id, expiresAt: checks.expiresAt(check) }, 201);
      }
      if (pathname === "/v1/checks" && req.method === "GET") {
        const open = checks.open(me.id).filter((c) => paired.has(c.from));
        return json({ checks: open.map((c) => ({ id: c.id, from: names.get(c.from) ?? "", kind: c.kind, createdAt: c.createdAt })) });
      }
      const answering = pathname.match(/^\/v1\/checks\/([A-Za-z0-9_-]{16})\/answer$/);
      if (answering && req.method === "POST") {
        const body = (await readJson(req)) as any;
        if (body instanceof Response) return body;
        if (!ANSWERS.includes(body?.answer)) return json({ error: "bad_request", detail: "answer" }, 400);
        const result = checks.answer(answering[1], me.id, body.answer);
        const status = { ok: 200, not_found: 404, own: 403, already: 409 }[result];
        return json(result === "ok" ? { ok: true } : { error: result }, status);
      }
      const asked = pathname.match(/^\/v1\/checks\/([A-Za-z0-9_-]{16})$/);
      if (asked && req.method === "GET") {
        const check = checks.mine(asked[1], me.id);
        if (!check) return json({ error: "not_found" }, 404);
        const answers = [...check.answers].filter(([d]) => paired.has(d)).map(([d, a]) => ({ name: names.get(d) ?? "", answer: a.answer, at: a.at }));
        // Asked: the other phones with a pear, the family's Peris in use; an old paired phone nobody uses won't answer.
        const others = [...paired].filter((d) => d !== me.id && names.has(d)).length;
        return json({ answers, asked: others, expiresAt: checks.expiresAt(check) });
      }
      return json({ error: "not_found" }, 404);
    }

    if (req.method === "POST" && pathname === "/v1/opinion") {
      if (!fromDevice(req, config, services.devices)) return json({ error: "unauthorized" }, 401);
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
      if (!fromDevice(req, config, services.devices)) return json({ error: "unauthorized" }, 401);
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

    // A voice order the person said to Peri, as a short WAV; the audio and the text are never stored or logged.
    if (req.method === "POST" && pathname === "/v1/transcribe") {
      if (!fromDevice(req, config, services.devices)) return json({ error: "unauthorized" }, 401);
      const wav = await readBytesLimited(req, MAX_AUDIO);
      if (wav === "too_large") return json({ error: "too_large" }, 413);
      if (!isVoiceWav(wav)) return json({ error: "bad_request", detail: "send a 16-bit mono PCM WAV" }, 400);
      if (!services.voice) return json({ error: "no_model" }, 503);
      try {
        const { text } = await services.voice.transcribe(wav);
        return json({ text, model: services.voice.model });
      } catch {
        return json({ error: "model_failed" }, 502);
      }
    }

    // "Look at this picture": a photo or screenshot the person chose to show Peri. It is never stored or logged.
    if (req.method === "POST" && pathname === "/v1/picture") {
      if (!fromDevice(req, config, services.devices)) return json({ error: "unauthorized" }, 401);
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

    // "Report it": a scam Peri stopped, written up for the police or a helpline, only when the person asks.
    if (req.method === "POST" && pathname === "/v1/report") {
      if (!fromDevice(req, config, services.devices)) return json({ error: "unauthorized" }, 401);
      const body = await readJson(req);
      if (body instanceof Response) return body;
      const input = readReport(body);
      if (typeof input === "string") return json({ error: "bad_request", detail: input }, 400);
      if (!services.investigator) return json({ error: "no_model" }, 503);
      try {
        return json(await writeReport(services.investigator, input));
      } catch {
        // Neither the message nor the report is logged.
        return json({ error: "model_failed" }, 502);
      }
    }

    const approvals = services.approvals;

    // The phone asks for a guardian's approval and gets the one-time link to text them.
    if (req.method === "POST" && pathname === "/v1/approvals") {
      if (!fromDevice(req, config, services.devices)) return json({ error: "unauthorized" }, 401);
      if (!approvals) return json({ error: "no_approvals" }, 503);
      const body = (await readJson(req)) as any;
      if (body instanceof Response) return body;
      if (!KINDS.includes(body?.kind)) return json({ error: "bad_request", detail: "unknown kind" }, 400);
      if (typeof body.toName !== "string" || !NAME.test(body.toName)) return json({ error: "bad_request", detail: "bad name" }, 400);
      const lang = body.lang === "en" ? "en" : "es";
      const { approval, token } = approvals.create(body.kind as Kind, body.toName, lang, body.onCall === true);
      return json({ id: approval.id, url: `${config.publicBaseUrl}/a/${token}`, expiresAt: approval.expiresAt }, 201);
    }

    // The phone polls until the guardian decides or the link expires.
    const status = pathname.match(/^\/v1\/approvals\/([A-Za-z0-9_-]{8,40})$/);
    if (req.method === "GET" && status) {
      if (!fromDevice(req, config, services.devices)) return json({ error: "unauthorized" }, 401);
      const a = approvals?.get(status[1]);
      if (!a) return json({ error: "not_found" }, 404);
      return json({ status: a.status });
    }

    const guardians = services.guardians;

    // The phone shares a guardian's personal link, when the person turns sharing on.
    if (req.method === "POST" && pathname === "/v1/guardians/links") {
      if (!fromDevice(req, config, services.devices)) return json({ error: "unauthorized" }, 401);
      if (!guardians) return json({ error: "no_guardians" }, 503);
      const body = (await readJson(req)) as any;
      if (body instanceof Response) return body;
      if (typeof body?.name !== "string" || !NAME.test(body.name)) return json({ error: "bad_request", detail: "bad name" }, 400);
      const { token } = guardians.createLink(body.name, body.lang === "en" ? "en" : "es");
      return json({ url: `${config.publicBaseUrl}/g/${token}` }, 201);
    }

    // Turning sharing off: every guardian link stops working and the news is forgotten.
    if (req.method === "DELETE" && pathname === "/v1/guardians/links") {
      if (!fromDevice(req, config, services.devices)) return json({ error: "unauthorized" }, 401);
      guardians?.revokeAll();
      return json({ ok: true });
    }

    // What happened, never what it was about: a warning's level and channel, or a pause and its answer.
    if (req.method === "POST" && pathname === "/v1/events") {
      if (!fromDevice(req, config, services.devices)) return json({ error: "unauthorized" }, 401);
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
      // The guardian's own Peri asks for the same request as data, to show it in the app; the link is still the key.
      if (req.method === "GET" && (req.headers.get("accept") ?? "").includes("application/json")) {
        const a = approvals.byLink(token);
        if (!a) return json({ error: "not_found" }, 404);
        const { kind, toName, lang, onCall, status, expiresAt } = a;
        return json({ kind, toName, lang, onCall, status, expiresAt });
      }
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
        const form = new URLSearchParams(raw);
        const decision = form.get("decision");
        if (!DECISIONS.includes(decision as Decision)) return html(resultPage("es", "unknown"), 400);
        const pending = approvals.byLink(token);
        const lang = pending?.lang ?? "es";
        // A yes needs a second, deliberate step: the first one only shows the "are you sure?" page.
        if (decision === "approve" && form.get("confirm") !== "yes" && pending?.status === "pending") {
          return html(confirmPage(pending, token));
        }
        const result = approvals.decide(token, decision as Decision);
        if (typeof result === "string") return html(resultPage(lang, result), result === "unknown" ? 404 : 410);
        return html(resultPage(result.lang, result.status === "approved" ? "approved" : result.status === "callback" ? "callback" : "rejected"));
      }
    }

    if (req.method === "GET" && pathname.startsWith("/p/")) {
      const picture = asset(pathname);
      if (picture) return picture;
    }

    return json({ error: "not_found" }, 404);
  };
}
