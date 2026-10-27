import type { Config } from "./config";

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json", "cache-control": "no-store" } });

/** Routes are added by module: approvals/, investigator/ and models/ each register their own. */
export function createHandler(_config: Config) {
  return async function handle(req: Request): Promise<Response> {
    const { pathname } = new URL(req.url);
    if (req.method === "GET" && pathname === "/health") return json({ ok: true, service: "friendlypear" });
    return json({ error: "not_found" }, 404);
  };
}
