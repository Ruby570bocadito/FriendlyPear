import { ApprovalStore } from "./approvals/store";
import { loadConfig } from "./config";
import { join } from "node:path";
import { GuardianStore } from "./guardians/store";
import { sparkFromEnv } from "./models/spark";
import { sparkOpenFromEnv } from "./models/spark-open";
import { createHandler } from "./server";

const config = loadConfig();
const opinion = sparkOpenFromEnv();
const investigator = sparkFromEnv();
const approvals = new ApprovalStore();
// Guardians' links (hashes only) and Peri's news survive restarts; DATA_DIR is git-ignored by default.
const guardians = new GuardianStore(Date.now, join(process.env.DATA_DIR ?? "data", "guardians.json"));
const handle = createHandler(config, { opinion, investigator, approvals, guardians });
// Behind a tunnel or proxy (TRUST_PROXY=1) the client's IP is the first one it forwarded; otherwise the socket's.
const trustProxy = process.env.TRUST_PROXY === "1";
const server = Bun.serve({
  port: config.port,
  // Bodies are small JSON or a one-field form; anything bigger never reaches the routes.
  maxRequestBodySize: 64 * 1024,
  fetch(req, srv) {
    const forwarded = trustProxy ? req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() : undefined;
    return handle(req, forwarded || srv.requestIP(req)?.address || "unknown");
  },
});
console.log(`FriendlyPear backend on :${server.port} · second opinion: ${opinion ? opinion.model : "off"} · investigator: ${investigator ? investigator.model : "off"} · approvals: ${config.publicBaseUrl}/a/…`);
