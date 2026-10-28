/**
 * Family MFA: a risky action waits for a guardian's yes, and the riskiest (changing the guardians, or a code while a
 * stranger may be on the line) for two guardians' yes when two or more are asked. Each guardian gets their own
 * one-time link, so nobody can say yes twice; one "no" (or "call me first") settles it. Links expire after 15 minutes.
 * Silence never approves: an expired request is simply "expired". Only the kind of action and first names are stored,
 * never the message text or the code.
 */
import { createHash, randomBytes } from "node:crypto";

export const TTL_MS = 15 * 60 * 1000;
export const KINDS = ["send_code", "circle_change"] as const;
export type Kind = (typeof KINDS)[number];
/** "callback": the guardian would rather talk first, so the person's Peri offers to call them. */
export type Status = "pending" | "approved" | "rejected" | "callback" | "expired";
export type Decision = "approve" | "reject" | "callback";
export const DECISIONS: readonly Decision[] = ["approve", "reject", "callback"];
export type Lang = "es" | "en";
/** At most this many guardians are asked, each with a link of their own. */
export const MAX_GUARDIANS = 5;

/** Two yeses for the riskiest requests, when there are two guardians to give them (the app applies the same rule). */
export function needsTwo(kind: Kind, onCall: boolean): boolean {
  return kind === "circle_change" || (kind === "send_code" && onCall);
}

export interface Approval {
  id: string;
  kind: Kind;
  /** Who the action is for, e.g. the contact the message would go to. A first name, nothing else. */
  toName: string;
  lang: Lang;
  /** The person is on the phone with a number they haven't saved, right now: a yes or no, nothing about the call. */
  onCall: boolean;
  createdAt: number;
  expiresAt: number;
  status: Status;
  /** How many guardians must say yes: two for the riskiest requests when two or more were asked, otherwise one. */
  needed: number;
  /** The guardians asked, by first name, one link each; empty when one link went to all of them. */
  asked: string[];
  /** Who said yes so far, by first name ("" for a shared link opened in a browser). */
  yes: string[];
  decidedAt?: number;
  /** Who settled it: the guardian whose link it was, or their own Peri's pear name. */
  decidedBy?: string;
  /** The asking phone's pear when it asked (its name and outfit), shown to the guardian as a sign only the family knows. */
  pear?: { name: string; outfit: string };
}

/** One guardian's link: the request it belongs to and whose it is ("" when it went to all of them). */
interface Link {
  id: string;
  name: string;
}

const hash = (token: string) => createHash("sha256").update(token).digest("hex");
const randomId = (bytes: number) => randomBytes(bytes).toString("base64url");

export class ApprovalStore {
  private readonly byId = new Map<string, Approval>();
  /** sha256(link token) → whose link and for which request; the token itself is never stored. */
  private readonly byToken = new Map<string, Link>();

  constructor(private readonly now: () => number = Date.now) {}

  /**
   * A request with a link per guardian in [guardians] (first names), or a single link for all of them if none are
   * named. [token] is the first link's.
   */
  create(
    kind: Kind,
    toName: string,
    lang: Lang,
    onCall = false,
    pear?: { name: string; outfit: string },
    guardians: string[] = [],
  ): { approval: Approval; token: string; links: { name: string; token: string }[] } {
    this.sweep();
    const at = this.now();
    const asked = guardians.slice(0, MAX_GUARDIANS);
    const needed = needsTwo(kind, onCall) && asked.length >= 2 ? 2 : 1;
    const approval: Approval = {
      id: randomId(12), kind, toName, lang, onCall, createdAt: at, expiresAt: at + TTL_MS, status: "pending", needed, asked, yes: [],
      ...(pear ? { pear } : {}),
    };
    this.byId.set(approval.id, approval);
    const links = (asked.length > 0 ? asked : [""]).map((name) => {
      const token = randomId(32);
      this.byToken.set(hash(token), { id: approval.id, name });
      return { name, token };
    });
    return { approval, token: links[0]!.token, links };
  }

  get(id: string): Approval | undefined {
    const a = this.byId.get(id);
    if (a) this.expireIfDue(a);
    return a;
  }

  /** What the guardian's link points to, while it can still be used. */
  byLink(token: string): Approval | undefined {
    const link = this.byToken.get(hash(token));
    return link ? this.get(link.id) : undefined;
  }

  /**
   * One decision per link. A yes counts once per guardian, and the request is approved when enough guardians said
   * yes; a no or "call me first" settles it at once. Once settled, or after 15 minutes, no link works any more.
   * [by] is the deciding guardian's own Peri (their pear's name), when they answer from the app.
   */
  decide(token: string, decision: Decision, by?: string): Approval | "used" | "expired" | "unknown" {
    const key = hash(token);
    const link = this.byToken.get(key);
    const a = link ? this.get(link.id) : undefined;
    if (!a || !link) return "unknown";
    if (a.status === "expired") return "expired";
    if (a.status !== "pending") return "used";
    // The same guardian's Peri can't say yes twice, even holding two links.
    if (decision === "approve" && by && a.yes.includes(by)) return "used";
    this.byToken.delete(key);
    const who = by ?? link.name;
    if (decision === "approve") {
      a.yes.push(who);
      if (a.yes.length < a.needed) return a;
      a.status = "approved";
    } else {
      a.status = decision === "reject" ? "rejected" : "callback";
    }
    a.decidedAt = this.now();
    if (who) a.decidedBy = who;
    for (const [k, l] of this.byToken) if (l.id === a.id) this.byToken.delete(k);
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
    for (const [key, link] of this.byToken) if (!this.byId.has(link.id)) this.byToken.delete(key);
  }
}
