/**
 * `bun rotate`: a new admin token, written to backend/.env, with the old one still accepted for a day so nothing
 * stops while the family's scripts or `bun pair` are updated. The new token is printed this once; restart the server
 * to use it. (A phone's own key rotates from the phone: POST /v1/devices/self/key.)
 */
import { randomBytes } from "node:crypto";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

export const OVERLAP_MS = 24 * 60 * 60 * 1000;

/** The .env text with [token] as ADMIN_TOKEN and the old one as ADMIN_TOKEN_PREVIOUS until [until]. */
export function rotated(env: string, token: string, until: Date): string {
  const lines = env.split(/\r?\n/).filter((l) => !/^ADMIN_TOKEN(_PREVIOUS|_PREVIOUS_UNTIL)?=/.test(l));
  const old = env.split(/\r?\n/).find((l) => l.startsWith("ADMIN_TOKEN="))?.slice("ADMIN_TOKEN=".length).trim();
  while (lines.length > 0 && lines[lines.length - 1] === "") lines.pop();
  lines.push(`ADMIN_TOKEN=${token}`);
  if (old) lines.push(`ADMIN_TOKEN_PREVIOUS=${old}`, `ADMIN_TOKEN_PREVIOUS_UNTIL=${until.toISOString()}`);
  return lines.join("\n") + "\n";
}

if (import.meta.main) {
  const file = join(import.meta.dir, "..", ".env");
  const token = randomBytes(32).toString("base64url");
  const until = new Date(Date.now() + OVERLAP_MS);
  writeFileSync(file, rotated(existsSync(file) ? readFileSync(file, "utf8") : "", token, until), { mode: 0o600 });
  console.log(`New admin token (shown this once):\n\n  ${token}\n\nThe old one keeps working until ${until.toISOString()}. Restart the server to use the new one.`);
}
