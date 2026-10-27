/**
 * Family MFA: a risky action waits for a guardian's yes. Each request gets a one-time link that expires
 * after 15 minutes. Silence never approves: an expired request is simply "expired".
 * Only the kind of action and a first name are stored, never the message text or the code.
 */
import { createHash, randomBytes } from "node:crypto";

export const TTL_MS = 15 * 60 * 1000;
export const KINDS = ["send_code", "circle_change"] as const;
export type Kind = (typeof KINDS)[number];
export type Status = "pending" | "approved" | "rejected" | "expired";
export type Lang = "es" | "en";

export interface Approval {
  id: string;
  kind: Kind;
  /** Who the action is for, e.g. the contact the message would go to. A first name, nothing else. */
  toName: string;
  lang: Lang;
  createdAt: number;
  expiresAt: number;
  status: Status;
  decidedAt?: number;
}

const hash = (token: string) => createHash("sha256").update(token).digest("hex");
const randomId = (bytes: number) => randomBytes(bytes).toString("base64url");

export class ApprovalStore {
  private readonly byId = new Map<string, Approval>();
  /** sha256(link token) → approval id; the token itself is never stored. */
  private readonly byToken = new Map<string, string>();

  constructor(private readonly now: () => number = Date.now) {}

  create(kind: Kind, toName: string, lang: Lang): { approval: Approval; token: string } {
    this.sweep();
    const token = randomId(32);
    const at = this.now();
    const approval: Approval = { id: randomId(12), kind, toName, lang, createdAt: at, expiresAt: at + TTL_MS, status: "pending" };
    this.byId.set(approval.id, approval);
    this.byToken.set(hash(token), approval.id);
    return { approval, token };
  }

  get(id: string): Approval | undefined {
    const a = this.byId.get(id);
    if (a) this.expireIfDue(a);
    return a;
  }

  /** What the guardian's link points to, while it can still be used. */
  byLink(token: string): Approval | undefined {
    const id = this.byToken.get(hash(token));
    return id ? this.get(id) : undefined;
  }

  /** One decision per link: afterwards, or after 15 minutes, the link no longer works. */
  decide(token: string, approve: boolean): Approval | "used" | "expired" | "unknown" {
    const key = hash(token);
    const id = this.byToken.get(key);
    const a = id ? this.get(id) : undefined;
    if (!a) return "unknown";
    if (a.status === "expired") return "expired";
    if (a.status !== "pending") return "used";
    a.status = approve ? "approved" : "rejected";
    a.decidedAt = this.now();
    this.byToken.delete(key);
    return a;
  }

  /** The last day's requests, newest first, for the guardians' page. */
  recent(): Approval[] {
    this.sweep();
    return [...this.byId.values()].map((a) => (this.expireIfDue(a), a)).sort((x, y) => y.createdAt - x.createdAt);
  }

  private expireIfDue(a: Approval) {
    if (a.status === "pending" && this.now() >= a.expiresAt) a.status = "expired";
  }

  /** Forgets requests older than a day, so the store never grows without bound. */
  private sweep() {
    const cutoff = this.now() - 24 * 60 * 60 * 1000;
    for (const [id, a] of this.byId) if (a.createdAt < cutoff) this.byId.delete(id);
    for (const [key, id] of this.byToken) if (!this.byId.has(id)) this.byToken.delete(key);
  }
}
