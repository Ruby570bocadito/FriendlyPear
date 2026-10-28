/**
 * "Was it you?": when someone writes to a family member from a new number saying they're family ("new_number"), or
 * their phone gets a different SIM or loses its line while the internet works ("sim", a SIM swap's signs), that
 * member's Peri asks the rest of the family's Peris, in silence. Each paired phone answers once, "it was me" or "it wasn't me", and
 * the asking phone hears back who said what. Nothing about the message travels: not its words, not the number, not
 * who it claimed to be. A question lasts 15 minutes and lives only in memory.
 */
import { randomBytes } from "node:crypto";

export const CHECK_TTL_MS = 15 * 60 * 1000;
export const KINDS = ["new_number", "sim"] as const;
export const ANSWERS = ["me", "not_me"] as const;
/** A phone can't flood its family with questions: a few open at a time. */
const MAX_OPEN_PER_PHONE = 3;
const MAX_CHECKS = 200;

export type CheckKind = (typeof KINDS)[number];
export type CheckAnswer = (typeof ANSWERS)[number];

export interface Check {
  id: string;
  from: string;
  kind: CheckKind;
  createdAt: number;
  answers: Map<string, { answer: CheckAnswer; at: number }>;
}

export type Answered = "ok" | "not_found" | "own" | "already";

export class CheckStore {
  private readonly checks = new Map<string, Check>();

  constructor(private readonly now: () => number = Date.now) {}

  /** A new question from [from]; null if that phone already has too many open. */
  ask(from: string, kind: CheckKind): Check | null {
    this.sweep();
    const open = [...this.checks.values()].filter((c) => c.from === from).length;
    if (open >= MAX_OPEN_PER_PHONE || this.checks.size >= MAX_CHECKS) return null;
    const check: Check = { id: randomBytes(12).toString("base64url"), from, kind, createdAt: this.now(), answers: new Map() };
    this.checks.set(check.id, check);
    return check;
  }

  /** The questions [device] can still answer: the others' and not yet answered, oldest first. */
  open(device: string): Check[] {
    this.sweep();
    return [...this.checks.values()].filter((c) => c.from !== device && !c.answers.has(device)).sort((a, b) => a.createdAt - b.createdAt);
  }

  answer(id: string, device: string, answer: CheckAnswer): Answered {
    this.sweep();
    const check = this.checks.get(id);
    if (!check) return "not_found";
    if (check.from === device) return "own";
    if (check.answers.has(device)) return "already";
    check.answers.set(device, { answer, at: this.now() });
    return "ok";
  }

  /** A question, only for the phone that asked it. */
  mine(id: string, device: string): Check | null {
    this.sweep();
    const check = this.checks.get(id);
    return check && check.from === device ? check : null;
  }

  expiresAt(check: Check): number {
    return check.createdAt + CHECK_TTL_MS;
  }

  /** A phone that's disconnected stops asking and answering. */
  forget(device: string) {
    for (const [id, c] of this.checks) {
      if (c.from === device) this.checks.delete(id);
      else c.answers.delete(device);
    }
  }

  private sweep() {
    const now = this.now();
    for (const [id, c] of this.checks) if (now >= c.createdAt + CHECK_TTL_MS) this.checks.delete(id);
  }
}
