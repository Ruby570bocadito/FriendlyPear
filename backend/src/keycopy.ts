/**
 * `bun keycopy <folder>`: a copy of the server's data key in [folder], kept apart from the data (a USB stick, another
 * disk, a password manager's file). Without the key the sealed files can't be opened, and the family would pair its
 * phones again. The copy only counts once it has opened today's sealed files; the key is never printed.
 * `bun keycopy --check <file>`: the restore drill: would this copy open today's data?
 */
import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { homedir } from "node:os";
import { join } from "node:path";
import { Vault } from "./vault";

/** The data key's text, as the server reads it: DATA_KEY, else the key file. Null if there isn't one yet. */
export function keyText(env: Record<string, string | undefined> = process.env): string | null {
  if (env.DATA_KEY) return env.DATA_KEY.trim();
  const file = env.DATA_KEY_FILE ?? join(homedir(), ".friendlypear", "data.key");
  return existsSync(file) ? readFileSync(file, "utf8").trim() : null;
}

/** How many of [dataDir]'s sealed files [key] opens, of how many sealed files there are. */
export function opens(dataDir: string, key: string): { opened: number; sealed: number } {
  const vault = new Vault(Buffer.from(key, "base64"));
  const files = existsSync(dataDir) ? readdirSync(dataDir).filter((n) => n.endsWith(".json")) : [];
  const sealed = files.map((n) => readFileSync(join(dataDir, n), "utf8")).filter((text) => text.startsWith("fpv1:"));
  return { opened: sealed.filter((text) => vault.open(text) !== null).length, sealed: sealed.length };
}

/** Writes the copy in [folder] (its owner only) and returns its path, once it has opened every sealed file. */
export function copyKey(key: string, dataDir: string, folder: string, now = new Date()): string {
  const { opened, sealed } = opens(dataDir, key);
  if (opened < sealed) throw new Error(`this key opens ${opened} of ${sealed} sealed files: it isn't the data's key`);
  mkdirSync(folder, { recursive: true });
  const file = join(folder, `friendlypear-data-key-${now.toISOString().slice(0, 10)}.key`);
  writeFileSync(file, key + "\n", { mode: 0o600 });
  if (readFileSync(file, "utf8").trim() !== key) throw new Error(`the copy at ${file} doesn't read back the same`);
  return file;
}

if (import.meta.main) {
  const dataDir = process.env.DATA_DIR ?? join(import.meta.dir, "..", "data");
  const [first, second] = process.argv.slice(2);
  if (first === "--check" && second) {
    const { opened, sealed } = opens(dataDir, readFileSync(second, "utf8").trim());
    console.log(opened === sealed ? `ok: this copy opens all ${sealed} sealed files` : `FAIL: this copy opens ${opened} of ${sealed} sealed files`);
    process.exit(opened === sealed ? 0 : 1);
  }
  const key = keyText();
  if (!first || !key) {
    console.log(key ? "usage: bun keycopy <folder apart from the data>   |   bun keycopy --check <copy>" : "no data key yet: start the server once first");
    process.exit(1);
  }
  console.log(`copied to ${copyKey(key, dataDir, first)}: keep it apart from the server and its data`);
}
