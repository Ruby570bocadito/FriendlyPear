/**
 * Each family phone's pear, for the family section of the app: the name the person chose ("Mamá", "Ana"), what
 * their Peri wears, their role in the family, and whether their Peri is calm or needs attention right now, with the
 * kind of trap if it's a scam ("parcel"). Nothing else: no numbers, no messages, no places. Only this family's paired
 * phones see them; a disconnected phone's pear goes with it. An "attention" left behind by a phone that went quiet
 * fades back to calm after a while.
 */
import { existsSync, mkdirSync, readFileSync, renameSync, writeFileSync } from "node:fs";
import { dirname } from "node:path";

/** What a Peri can wear (android/.../ui/peri/Outfit.kt): head, eyes, neck, in that order, each maybe empty. */
export const ITEMS = {
  head: ["STRAW", "BEANIE", "CROWN", "FLOWERS", "BOW"],
  eyes: ["SUNGLASSES", "HEARTS", "GLASSES"],
  neck: ["BOWTIE", "TIE", "BANDANA"],
} as const;
export const ROLES = ["protected", "guardian", "family"] as const;
export const STATES = ["calm", "attention"] as const;
/** The kinds of trap the app draws (android/.../TrapActivity.kt), or none. */
export const TRAP_KINDS = ["", "parcel", "link", "code", "family", "remote", "prize"] as const;
/** The same kind of trap reaching two or more family phones within this time is a campaign against the family. */
export const CAMPAIGN_WINDOW_MS = 30 * 60 * 1000;
/** An attention nobody refreshed is old news after this long. */
export const ATTENTION_TTL_MS = 2 * 60 * 60 * 1000;
const NAME = /^[\p{L}\p{N} .'()-]{1,24}$/u;
const MAX_PEARS = 20;

export type Role = (typeof ROLES)[number];
export type State = (typeof STATES)[number];

export interface Pear {
  device: string;
  name: string;
  outfit: string;
  role: Role;
  state: State;
  kind: string;
  updatedAt: number;
}

export type PearInput = Pick<Pear, "name" | "outfit" | "role" | "state" | "kind">;

export class PearStore {
  private readonly pears = new Map<string, Pear>();

  constructor(
    private readonly now: () => number = Date.now,
    private readonly file?: string,
  ) {
    if (file && existsSync(file)) this.load(file);
  }

  /** The phone's own pear, replaced whole each time it changes. */
  put(device: string, input: PearInput): Pear | null {
    if (!this.pears.has(device) && this.pears.size >= MAX_PEARS) return null;
    const pear: Pear = { device, ...input, updatedAt: this.now() };
    this.pears.set(device, pear);
    this.save();
    return pear;
  }

  /** Every pear of the family, by name; an old attention reads as calm. */
  list(): Pear[] {
    const now = this.now();
    return [...this.pears.values()]
      .map((p) => (p.state === "attention" && now - p.updatedAt > ATTENTION_TTL_MS ? { ...p, state: "calm" as const } : p))
      .sort((a, b) => a.name.localeCompare(b.name));
  }

  forget(device: string) {
    if (this.pears.delete(device)) this.save();
  }

  /** The campaign, if any, among these phones' pears (the ones still paired). */
  campaignAmong(devices: Set<string>): string | null {
    return campaign(this.list().filter((p) => devices.has(p.device)), this.now());
  }

  private load(file: string) {
    try {
      const data = JSON.parse(readFileSync(file, "utf8")) as { pears?: Pear[] };
      for (const p of data.pears ?? []) {
        const input = readPear(p);
        if (typeof input !== "string" && typeof p.device === "string" && typeof p.updatedAt === "number") {
          this.pears.set(p.device, { device: p.device, ...input, updatedAt: p.updatedAt });
        }
      }
    } catch {
      // A damaged file starts empty: each phone sends its pear again the next time it changes or opens.
    }
  }

  private save() {
    if (!this.file) return;
    mkdirSync(dirname(this.file), { recursive: true });
    const tmp = `${this.file}.tmp`;
    writeFileSync(tmp, JSON.stringify({ pears: [...this.pears.values()] }));
    renameSync(tmp, this.file);
  }
}

/** A pear as the phone sends it, checked field by field; a string says what was wrong. */
export function readPear(body: any): PearInput | string {
  const name = typeof body?.name === "string" ? body.name.trim() : "";
  if (!NAME.test(name)) return "name";
  const outfit = typeof body?.outfit === "string" ? body.outfit : "";
  const parts = outfit.split(",");
  const slots = [ITEMS.head, ITEMS.eyes, ITEMS.neck] as readonly (readonly string[])[];
  if (parts.length !== 3 || parts.some((p: string, i: number) => p !== "" && !slots[i].includes(p))) return "outfit";
  if (!ROLES.includes(body?.role)) return "role";
  if (!STATES.includes(body?.state)) return "state";
  const kind = body?.kind ?? "";
  if (!TRAP_KINDS.includes(kind)) return "kind";
  return { name, outfit, role: body.role, state: body.state, kind: body.state === "attention" ? kind : "" };
}

/** The kind of trap that's reaching two or more of the family's phones right now, if any. */
export function campaign(pears: Pear[], now: number): string | null {
  const counts = new Map<string, number>();
  for (const p of pears) {
    if (p.state === "attention" && p.kind && now - p.updatedAt <= CAMPAIGN_WINDOW_MS) counts.set(p.kind, (counts.get(p.kind) ?? 0) + 1);
  }
  for (const [kind, n] of counts) if (n >= 2) return kind;
  return null;
}
