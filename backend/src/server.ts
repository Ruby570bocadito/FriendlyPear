import { timingSafeEqual } from "node:crypto";
import type { Config } from "./config";
import { readInput, secondOpinion } from "./investigator/opinion";
import type { ChatClient } from "./models/chat";

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json", "cache-control": "no-store" } });

const MAX_BODY = 8 * 1024;

/** Models the backend can use; a missing one means that layer is off and the phone keeps its rules verdict. */
export interface Services {
  opinion?: ChatClient | null;
}

function fromDevice(req: Request, config: Config): boolean {
  const given = Buffer.from(req.headers.get("authorization") ?? "");
  const expected = Buffer.from(`Bearer ${config.deviceToken}`);
  return given.length === expected.length && timingSafeEqual(given, expected);
}

/** Routes are added by module: approvals/, investigator/ and models/ each register their own. */
export function createHandler(config: Config, services: Services = {}) {
  return async function handle(req: Request): Promise<Response> {
    const { pathname } = new URL(req.url);
    if (req.method === "GET" && pathname === "/health") return json({ ok: true, service: "friendlypear" });

    if (req.method === "POST" && pathname === "/v1/opinion") {
      if (!fromDevice(req, config)) return json({ error: "unauthorized" }, 401);
      const raw = await req.text();
      if (raw.length > MAX_BODY) return json({ error: "too_large" }, 413);
      let body: unknown;
      try {
        body = JSON.parse(raw);
      } catch {
        return json({ error: "bad_json" }, 400);
      }
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

    return json({ error: "not_found" }, 404);
  };
}
