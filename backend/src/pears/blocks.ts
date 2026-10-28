/**
 * A number blocked on one family phone protects them all, as a fingerprint only: each phone sends HMAC-SHA256 of the
 * numbers it blocked from a scam's card, keyed with the family's salt and computed on the phone, so no number ever
 * reaches this server. Each phone sees the others' fingerprints, never whose they are. A phone's set is replaced whole
 * each time and goes when the phone is disconnected. The salt is made here once and given only to paired phones.
 */
import { randomBytes } from "node:crypto";
import { readStore, type Vault, writeStore } from "../vault";

/** At most this many blocked numbers per phone. */
export const MAX_PRINTS = 100;
const PRINT = /^[0-9a-f]{64}$/;

export class BlockStore {
  private readonly prints = new Map<string, string[]>();
  private familySalt = "";

  constructor(
    private readonly file?: string,
    private readonly vault?: Vault | null,
  ) {
    if (file) this.load(file);
    if (!this.familySalt) {
      this.familySalt = randomBytes(32).toString("base64url");
      this.save();
    }
  }

  /** The key every family phone hashes its blocked numbers with. */
  get salt(): string {
    return this.familySalt;
  }

  /** This phone's fingerprints, replacing the ones it sent before. */
  put(device: string, prints: string[]) {
    if (prints.length === 0) this.prints.delete(device);
    else this.prints.set(device, prints);
    this.save();
  }

  /** What the family's other phones (the ones still paired) blocked, once each and without saying whose. */
  othersOf(device: string, paired: Set<string>): string[] {
    const all = new Set<string>();
    for (const [from, prints] of this.prints) if (from !== device && paired.has(from)) for (const p of prints) all.add(p);
    return [...all].sort();
  }

  forget(device: string) {
    if (this.prints.delete(device)) this.save();
  }

  private load(file: string) {
    // A damaged file starts again with a new salt: each phone fetches it and sends its fingerprints again.
    const data = readStore(file, this.vault) as { salt?: unknown; prints?: Record<string, unknown> } | null;
    if (typeof data?.salt === "string" && data.salt.length >= 32) this.familySalt = data.salt;
    for (const [device, list] of Object.entries(data?.prints ?? {})) {
      const prints = readPrints({ prints: list });
      if (typeof prints !== "string" && prints.length > 0) this.prints.set(device, prints);
    }
  }

  private save() {
    if (this.file) writeStore(this.file, { salt: this.familySalt, prints: Object.fromEntries(this.prints) }, this.vault);
  }
}

/** A phone's fingerprints as it sends them: `{ prints: [64 lowercase hex characters, ...] }`; a string says what was wrong. */
export function readPrints(body: any): string[] | string {
  const list = body?.prints;
  if (!Array.isArray(list)) return "prints";
  if (list.length > MAX_PRINTS) return "too_many";
  if (!list.every((p) => typeof p === "string" && PRINT.test(p))) return "print";
  return [...new Set(list as string[])];
}
