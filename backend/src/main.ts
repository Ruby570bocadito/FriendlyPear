import { ApprovalStore } from "./approvals/store";
import { loadConfig } from "./config";
import { GuardianStore } from "./guardians/store";
import { sparkFromEnv } from "./models/spark";
import { sparkOpenFromEnv } from "./models/spark-open";
import { createHandler } from "./server";

const config = loadConfig();
const opinion = sparkOpenFromEnv();
const investigator = sparkFromEnv();
const approvals = new ApprovalStore();
const guardians = new GuardianStore();
const server = Bun.serve({ port: config.port, fetch: createHandler(config, { opinion, investigator, approvals, guardians }) });
console.log(`FriendlyPear backend on :${server.port} · second opinion: ${opinion ? opinion.model : "off"} · investigator: ${investigator ? investigator.model : "off"} · approvals: ${config.publicBaseUrl}/a/…`);
