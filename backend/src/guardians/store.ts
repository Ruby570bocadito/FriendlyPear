/**
 * What a guardian can see about their relative, only if the relative chose to share it: that Peri warned about
 * a message (scam or doubtful, and the channel), and safety pauses with what the person chose. Never the text,
 * a number, a name or anything about calls. Each guardian has their own long-lived link; only its hash is kept.
 */
import { createHash, randomBytes } from "node:crypto";
import type { Lang } from "../approvals/store";

export const CHANNELS = ["SMS", "WHATSAPP", "EMAIL", "BROWSER_NOTIFICATION"] as const;
export const PAUSE_ANSWERS = ["call_family", "leave", "continue"] as const;
export const KEEP_MS = 30 * 24 * 60 * 60 * 1000;
const MAX_EVENTS = 300;

export type FamilyEvent =
  | { kind: "warning"; level: "scam" | "doubt"; channel: (typeof CHANNELS)[number]; at: number }
  | { kind: "pause"; answer: (typeof PAUSE_ANSWERS)[number]; at: number };

export interface GuardianLink {
  id: string;
  /** The guardian's first name, to greet them. */
  name: string;
  lang: Lang;
  createdAt: number;
}

const hash = (token: string) => createHash("sha256").update(token).digest("hex");

export class GuardianStore {
  private readonly links = new Map<string, GuardianLink>();
  private events: FamilyEvent[] = [];

  constructor(private readonly now: () => number = Date.now) {}

  createLink(name: string, lang: Lang): { link: GuardianLink; token: string } {
    const token = randomBytes(32).toString("base64url");
    const link: GuardianLink = { id: randomBytes(9).toString("base64url"), name, lang, createdAt: this.now() };
    this.links.set(hash(token), link);
    return { link, token };
  }

  byLink(token: string): GuardianLink | undefined {
    return this.links.get(hash(token));
  }

  /** Turning sharing off on the phone invalidates every guardian link at once. */
  revokeAll() {
    this.links.clear();
    this.events = [];
  }

  /** The time is the backend's own: the phone only says what happened. */
  add(event: Omit<FamilyEvent, "at">) {
    this.events.push({ ...event, at: this.now() } as FamilyEvent);
    const cutoff = this.now() - KEEP_MS;
    this.events = this.events.filter((e) => e.at >= cutoff).slice(-MAX_EVENTS);
  }

  /** Newest first. */
  recent(): FamilyEvent[] {
    const cutoff = this.now() - KEEP_MS;
    return this.events.filter((e) => e.at >= cutoff).reverse();
  }
}

/** Checks what the phone sent: only the known kinds and values, nothing else is kept. */
export function readEvent(body: any): Omit<FamilyEvent, "at"> | string {
  if (body?.kind === "warning") {
    if (body.level !== "scam" && body.level !== "doubt") return "bad level";
    if (!CHANNELS.includes(body.channel)) return "bad channel";
    return { kind: "warning", level: body.level, channel: body.channel };
  }
  if (body?.kind === "pause") {
    if (!PAUSE_ANSWERS.includes(body.answer)) return "bad answer";
    return { kind: "pause", answer: body.answer };
  }
  return "unknown kind";
}
