import { HangUpStore } from "./pears/hangups";
import { BlockStore } from "./pears/blocks";
import { ApprovalStore } from "./approvals/store";
import { loadConfig } from "./config";
import { join } from "node:path";
import { DeviceStore } from "./devices/store";
import { GuardianStore } from "./guardians/store";
import { PearStore } from "./pears/store";
import { CheckStore } from "./checks/store";
import { samFromEnv } from "./models/sam";
import { sparkFromEnv } from "./models/spark";
import { sparkOpenFromEnv } from "./models/spark-open";
import { voiceFromEnv } from "./models/voice";
import { clientIp } from "./limits";
import { createHandler } from "./server";
import { Vault } from "./vault";

const config = loadConfig();
// Everything kept on disk is sealed with a key that lives apart from the data (see vault.ts).
const vault = Vault.fromEnv();
const dataDir = process.env.DATA_DIR ?? "data";
const opinion = sparkOpenFromEnv();
const investigator = sparkFromEnv();
const sam = samFromEnv();
const voice = voiceFromEnv();
// Pending approvals survive a restart: a guardian's link sent a minute ago still works.
const approvals = new ApprovalStore(Date.now, join(dataDir, "approvals.json"), vault);
// Guardians' links (hashes only) and Peri's news survive restarts; DATA_DIR is git-ignored by default.
const guardians = new GuardianStore(Date.now, join(dataDir, "guardians.json"), vault);
// Paired phones (key hashes only), next to the guardians' file.
const devices = new DeviceStore(Date.now, join(dataDir, "devices.json"), vault);
// The family's pears (a name, an outfit, calm or attention), next to the phones they belong to.
const pears = new PearStore(Date.now, join(dataDir, "pears.json"), vault);
// "Was it you?" questions between the family's phones: 15 minutes each, kept sealed across a restart.
const checks = new CheckStore(Date.now, join(dataDir, "checks.json"), vault);
// "Hang up their call" requests: two minutes each, in memory only.
const hangups = new HangUpStore();
// Fingerprints of the numbers each phone blocked (never the numbers), with the family's salt.
const blocks = new BlockStore(join(dataDir, "blocks.json"), vault);
const handle = createHandler(config, { opinion, investigator, sam, voice, approvals, guardians, devices, pears, checks, hangups, blocks });
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
