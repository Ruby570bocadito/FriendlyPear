import { timingSafeEqual } from "node:crypto";
import { askPage, resultPage } from "./approvals/page";
import { type ApprovalStore, KINDS, type Kind } from "./approvals/store";
import type { Config } from "./config";
import { guardianPage } from "./guardians/page";
import { type GuardianStore, readEvent } from "./guardians/store";
import { investigate, readInvestigation } from "./investigator/investigate";
import { readInput, secondOpinion } from "./investigator/opinion";
import type { ChatClient } from "./models/chat";

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
      "content-security-policy": "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'",
    },
  });

const MAX_BODY = 8 * 1024;
const NAME = /^[\p{L}][\p{L} .'-]{0,39}$/u;

/** What the backend can use; a missing piece means that layer is off and the phone decides alone. */
export interface Services {
  opinion?: ChatClient | null;
  /** Muse Spark with read-only tools, for messages still doubtful after the second opinion. */
  investigator?: ChatClient | null;
  approvals?: ApprovalStore | null;
  /** What guardians see, only when the person chose to share it. */
  guardians?: GuardianStore | null;
}

function fromDevice(req: Request, config: Config): boolean {
  const given = Buffer.from(req.headers.get("authorization") ?? "");
  const expected = Buffer.from(`Bearer ${config.deviceToken}`);
  return given.length === expected.length && timingSafeEqual(given, expected);
}

async function readJson(req: Request): Promise<unknown | Response> {
  const raw = await req.text();
  if (raw.length > MAX_BODY) return json({ error: "too_large" }, 413);
  try {
    return JSON.parse(raw);
  } catch {
    return json({ error: "bad_json" }, 400);
  }
}

/** Routes are added by module: approvals/, investigator/ and models/ each register their own. */
export function createHandler(config: Config, services: Services = {}) {
  return async function handle(req: Request): Promise<Response> {
    const { pathname } = new URL(req.url);
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
        const raw = await req.text();
        if (raw.length > 256) return html(resultPage("es", "unknown"), 413);
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
