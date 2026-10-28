/**
 * What the server keeps on disk, sealed with AES-256-GCM: the paired phones (key hashes), the pears, the guardians'
 * links and news, the blocked fingerprints, and the pending approvals and "was it you?" questions. The key comes from
 * DATA_KEY (32 bytes in base64) or from a key file kept apart from the data (DATA_KEY_FILE; by default in the home
 * folder, made on the first start), so a copy of the data folder alone reveals nothing. A file saved before sealing
 * existed is read as it was and sealed on its next save. A changed byte makes a file fail to open, and its store
 * starts empty rather than trust it.
 */
import { createCipheriv, createDecipheriv, randomBytes } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, renameSync, writeFileSync } from "node:fs";
import { homedir } from "node:os";
import { dirname, join } from "node:path";

const MARK = "fpv1:";

export class Vault {
  constructor(private readonly key: Buffer) {
    if (key.length !== 32) throw new Error("the data key must be 32 bytes");
  }

  /** From DATA_KEY, or the key file (made with a new random key the first time). */
  static fromEnv(env: Record<string, string | undefined> = process.env): Vault {
    if (env.DATA_KEY) return new Vault(Buffer.from(env.DATA_KEY, "base64"));
    const file = env.DATA_KEY_FILE ?? join(homedir(), ".friendlypear", "data.key");
    if (!existsSync(file)) {
      mkdirSync(dirname(file), { recursive: true });
      writeFileSync(file, randomBytes(32).toString("base64"), { mode: 0o600 });
      console.log(`made a new data key at ${file}: keep it apart from the data, and back it up`);
    }
    return new Vault(Buffer.from(readFileSync(file, "utf8").trim(), "base64"));
  }

  seal(data: unknown): string {
    const iv = randomBytes(12);
    const cipher = createCipheriv("aes-256-gcm", this.key, iv);
    const body = Buffer.concat([cipher.update(JSON.stringify(data), "utf8"), cipher.final()]);
    return MARK + Buffer.concat([iv, body, cipher.getAuthTag()]).toString("base64");
  }

  /** What was sealed, or a file from before sealing as it is; null if it can't be opened. */
  open(text: string): unknown | null {
    try {
      if (!text.startsWith(MARK)) return JSON.parse(text);
      const raw = Buffer.from(text.slice(MARK.length), "base64");
      const decipher = createDecipheriv("aes-256-gcm", this.key, raw.subarray(0, 12));
      decipher.setAuthTag(raw.subarray(raw.length - 16));
      const plain = Buffer.concat([decipher.update(raw.subarray(12, raw.length - 16)), decipher.final()]);
      return JSON.parse(plain.toString("utf8"));
    } catch {
      return null;
    }
  }
}

/** A store's file: sealed if there's a vault (as the server always has), plain JSON in tests. Null if missing or damaged. */
export function readStore(file: string, vault?: Vault | null): unknown | null {
  if (!existsSync(file)) return null;
  const text = readFileSync(file, "utf8");
  if (vault) return vault.open(text);
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

/** Written whole, to a side file first, so a crash mid-write never leaves half a file; readable by its owner only. */
export function writeStore(file: string, data: unknown, vault?: Vault | null): void {
  mkdirSync(dirname(file), { recursive: true });
  const tmp = `${file}.tmp`;
  writeFileSync(tmp, vault ? vault.seal(data) : JSON.stringify(data), { mode: 0o600 });
  renameSync(tmp, file);
}
