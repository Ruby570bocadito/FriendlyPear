/**
 * "Hang up their call": a guardian sees a family member's pear on a call with a number that isn't in their contacts
 * and asks that phone's Peri to hang up. One request per phone at a time, gone after two minutes or once that Peri
 * took it. Only who asked travels, never the number or the call.
 */
export const HANGUP_TTL_MS = 2 * 60_000;

export class HangUpStore {
  private readonly asks = new Map<string, { from: string; at: number }>();

  constructor(private readonly now: () => number = Date.now) {}

  /** False if a request is already waiting for that phone. */
  ask(target: string, from: string): boolean {
    this.sweep();
    if (this.asks.has(target)) return false;
    this.asks.set(target, { from, at: this.now() });
    return true;
  }

  /** The request waiting for this phone, if any: it's taken, so it's carried out once. */
  take(target: string): { from: string; at: number } | null {
    this.sweep();
    const found = this.asks.get(target) ?? null;
    this.asks.delete(target);
    return found;
  }

  /** A disconnected phone: nothing waits for it, and nothing it asked for. */
  forget(device: string): void {
    this.asks.delete(device);
    for (const [target, ask] of this.asks) if (ask.from === device) this.asks.delete(target);
  }

  private sweep(): void {
    const now = this.now();
    for (const [target, ask] of this.asks) if (now - ask.at > HANGUP_TTL_MS) this.asks.delete(target);
  }
}
