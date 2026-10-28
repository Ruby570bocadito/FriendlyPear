/**
 * Peri's pictures for the guardian pages, served by this server so the pages need nothing from anywhere else.
 * Only the files in backend/public/ that are on this list, read once at start-up: Peri's poses at `/p/` and the
 * accessories a family member's pear can wear at `/w/`.
 */
import { existsSync, readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { WEAR_NAMES } from "./pears/look";

export const ASSET_NAMES = ["waiting", "protective", "thinking", "thumbsup", "calling", "sorry", "calm", "guardian", "urgent"] as const;

const dir = fileURLToPath(new URL("../public/", import.meta.url));
const assets = new Map<string, Uint8Array>();
for (const name of ASSET_NAMES) {
  const file = `${dir}${name}.webp`;
  if (existsSync(file)) assets.set(name, readFileSync(file));
}

const wear = new Map<string, Uint8Array>();
for (const name of WEAR_NAMES) {
  const file = `${dir}wear/${name}.webp`;
  if (existsSync(file)) wear.set(name, readFileSync(file));
}

/** The picture for `/p/<name>.webp` or `/w/<name>.webp`, or null. */
export function asset(pathname: string): Response | null {
  const found = pathname.match(/^\/([pw])\/([a-z]+)\.webp$/);
  const bytes = found ? (found[1] === "p" ? assets : wear).get(found[2]) : undefined;
  if (!bytes) return null;
  return new Response(bytes, { headers: { "content-type": "image/webp", "cache-control": "public, max-age=86400" } });
}
