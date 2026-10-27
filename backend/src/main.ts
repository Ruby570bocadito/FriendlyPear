import { ApprovalStore } from "./approvals/store";
import { loadConfig } from "./config";
import { join } from "node:path";
import { GuardianStore } from "./guardians/store";
import { samFromEnv } from "./models/sam";
import { sparkFromEnv } from "./models/spark";
import { sparkOpenFromEnv } from "./models/spark-open";
import { voiceFromEnv } from "./models/voice";
import { clientIp } from "./limits";
import { createHandler } from "./server";

const config = loadConfig();
const opinion = sparkOpenFromEnv();
const investigator = sparkFromEnv();
const sam = samFromEnv();
const voice = voiceFromEnv();
const approvals = new ApprovalStore();
// Guardians' links (hashes only) and Peri's news survive restarts; DATA_DIR is git-ignored by default.
const guardians = new GuardianStore(Date.now, join(process.env.DATA_DIR ?? "data", "guardians.json"));
const handle = createHandler(config, { opinion, investigator, sam, voice, approvals, guardians });
// Behind a tunnel or proxy (TRUST_PROXY=1) the client's IP is the one the proxy added; otherwise the socket's.
const trustProxy = process.env.TRUST_PROXY === "1";
const server = Bun.serve({
  port: config.port,
  // Bodies are small JSON or a one-field form, or one picture of about 1.5 MB; nothing bigger reaches the routes.
  maxRequestBodySize: 3 * 1024 * 1024,
  fetch(req, srv) {
    return handle(req, clientIp(req.headers.get("x-forwarded-for"), trustProxy, srv.requestIP(req)?.address));
  },
});
console.log(`FriendlyPear backend on :${server.port} · second opinion: ${opinion ? opinion.model : "off"} · investigator: ${investigator ? investigator.model : "off"} · pictures: ${investigator && sam ? "on" : "off"} · voice: ${voice ? voice.model : "off"} · approvals: ${config.publicBaseUrl}/a/…`);
