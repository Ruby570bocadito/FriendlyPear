import { loadConfig } from "./config";
import { createHandler } from "./server";

const config = loadConfig();
const server = Bun.serve({ port: config.port, fetch: createHandler(config) });
console.log(`FriendlyPear backend on :${server.port}`);
