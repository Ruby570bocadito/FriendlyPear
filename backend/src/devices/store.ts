/**
 * Each phone gets its own key, paired in person: the family runs `bun pair` on the server, a one-time code (and
 * its QR) lasts 10 minutes, the phone redeems it once and gets a random key. Only hashes are kept, of codes and
 * keys; a phone can be disconnected on its own without touching the others.
 */
import { createHash, randomBytes, randomInt } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, renameSync, writeFileSync } from "node:fs";
import { dirname } from "node:path";

export const CODE_TTL_MS = 10 * 60 * 1000;
/** No 0/O, 1/I/L: easy to read aloud and type on a phone. */
const ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
export const CODE = /^[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$/;
const MAX_DEVICES = 20;

export interface Device {
  id: string;
  /** What the phone called itself ("Pixel 8"), for the list; never a person's name or number. */
  name: string;
  createdAt: number;
}

const hash = (s: string) => createHash("sha256").update(s).digest("hex");

export class DeviceStore {
  /** sha256(key) → device. */
  private readonly byKey = new Map<string, Device>();
  /** sha256(code) → expiry. Codes live only in memory: a restart simply cancels them. */
  private readonly codes = new Map<string, number>();

  constructor(
    private readonly now: () => number = Date.now,
    private readonly file?: string,
  ) {
    if (file && existsSync(file)) this.load(file);
  }

  /** A fresh one-time code, like "K7PM-3XQD". */
  newCode(): { code: string; expiresAt: number } {
    this.sweep();
    const pick = () => Array.from({ length: 4 }, () => ALPHABET[randomInt(ALPHABET.length)]).join("");
    const code = `${pick()}-${pick()}`;
    const expiresAt = this.now() + CODE_TTL_MS;
    this.codes.set(hash(code), expiresAt);
    return { code, expiresAt };
  }

  /** The code works once and only before it expires; the key is returned this one time and never again. */
  redeem(code: string, name: string): { device: Device; key: string } | null {
    this.sweep();
    const h = hash(code.toUpperCase());
    const expiresAt = this.codes.get(h);
    if (expiresAt === undefined || this.now() >= expiresAt || this.byKey.size >= MAX_DEVICES) return null;
    this.codes.delete(h);
    const key = randomBytes(32).toString("base64url");
    const device: Device = { id: randomBytes(9).toString("base64url"), name, createdAt: this.now() };
    this.byKey.set(hash(key), device);
    this.save();
    return { device, key };
  }

  /** The device behind a key, if it is still paired. */
  check(key: string): Device | null {
    return this.byKey.get(hash(key)) ?? null;
  }

  list(): Device[] {
    return [...this.byKey.values()].sort((a, b) => a.createdAt - b.createdAt);
  }

  /** Disconnects one phone: its key stops working at once. */
  revoke(id: string): boolean {
    for (const [k, d] of this.byKey) {
      if (d.id === id) {
        this.byKey.delete(k);
        this.save();
        return true;
      }
    }
    return false;
  }

  private sweep() {
    const now = this.now();
    for (const [h, until] of this.codes) if (now >= until) this.codes.delete(h);
  }

  private load(file: string) {
    try {
      const data = JSON.parse(readFileSync(file, "utf8")) as { devices?: [string, Device][] };
      for (const [k, d] of data.devices ?? []) if (/^[0-9a-f]{64}$/.test(k) && typeof d?.id === "string") this.byKey.set(k, d);
    } catch {
      // A damaged file starts empty: phones pair again rather than the backend stopping.
    }
  }

  private save() {
    if (!this.file) return;
    mkdirSync(dirname(this.file), { recursive: true });
    const tmp = `${this.file}.tmp`;
    writeFileSync(tmp, JSON.stringify({ devices: [...this.byKey] }));
    renameSync(tmp, this.file);
  }
}
