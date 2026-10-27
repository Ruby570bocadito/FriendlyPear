import { timingSafeEqual } from "node:crypto";
import { askPage, resultPage } from "./approvals/page";
import { type ApprovalStore, KINDS, type Kind } from "./approvals/store";
import type { Config } from "./config";
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
  approvals?: ApprovalStore | null;
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
