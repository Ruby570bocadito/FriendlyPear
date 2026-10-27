/**
 * Peri's pictures for the guardian pages, served by this server so the pages need nothing from anywhere else.
 * Only the files in backend/public/ that are on this list, read once at start-up.
 */
import { existsSync, readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

export const ASSET_NAMES = ["waiting", "protective", "thinking", "thumbsup", "calling", "sorry", "calm", "guardian", "urgent"] as const;

const dir = fileURLToPath(new URL("../public/", import.meta.url));
const assets = new Map<string, Uint8Array>();
for (const name of ASSET_NAMES) {
  const file = `${dir}${name}.webp`;
  if (existsSync(file)) assets.set(name, readFileSync(file));
}

/** The picture for `/p/<name>.webp`, or null. */
export function asset(pathname: string): Response | null {
  const name = pathname.match(/^\/p\/([a-z]+)\.webp$/)?.[1];
  const bytes = name ? assets.get(name) : undefined;
  if (!bytes) return null;
  return new Response(bytes, { headers: { "content-type": "image/webp", "cache-control": "public, max-age=86400" } });
}
