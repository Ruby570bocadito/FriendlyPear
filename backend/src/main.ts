import { ApprovalStore } from "./approvals/store";
import { loadConfig } from "./config";
import { sparkOpenFromEnv } from "./models/spark-open";
import { createHandler } from "./server";

const config = loadConfig();
const opinion = sparkOpenFromEnv();
const approvals = new ApprovalStore();
const server = Bun.serve({ port: config.port, fetch: createHandler(config, { opinion, approvals }) });
console.log(`FriendlyPear backend on :${server.port} · second opinion: ${opinion ? opinion.model : "off"} · approvals: ${config.publicBaseUrl}/a/…`);
