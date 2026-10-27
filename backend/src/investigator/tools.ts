/**
 * The investigator's tools. All are read-only: they look things up, they never act, write or contact anyone.
 * None receives personal data: a domain, a scam topic, or a relation like "CHILD" (never names or numbers).
 */
import type { Tool } from "../models/chat";
import { findCards } from "./knowledge";

export interface ToolContext {
  /** Relations present in the person's family circle, e.g. ["CHILD", "GRANDCHILD"]; never names or numbers. */
  familyRelations: string[];
  fetch?: typeof fetch;
  now?: () => number;
}

export const TOOLS: Tool[] = [
  {
    type: "function",
    function: {
      name: "domain_age",
      description: "Look up when a web domain was registered (public RDAP registry). Brand-new domains imitating a company are a strong scam sign.",
      parameters: {
        type: "object",
        properties: { domain: { type: "string", description: "Domain only, e.g. correos-envio.top" } },
        required: ["domain"],
      },
    },
  },
  {
    type: "function",
    function: {
      name: "scam_patterns",
      description: "Get short cards of known scam scripts (how they work, the giveaway, the safe action) matching a topic.",
      parameters: {
        type: "object",
        properties: { topic: { type: "string", description: "A few words about the message, e.g. 'parcel fee correos'" } },
        required: ["topic"],
      },
    },
  },
  {
    type: "function",
    function: {
      name: "family_has",
      description: "Whether the person's family circle includes someone with this relation, e.g. CHILD, GRANDCHILD, SIBLING, PARTNER.",
      parameters: {
        type: "object",
        properties: { relation: { type: "string", enum: ["CHILD", "GRANDCHILD", "NEPHEW_NIECE", "SIBLING", "PARTNER", "FRIEND", "CAREGIVER"] } },
        required: ["relation"],
      },
    },
  },
];

const DOMAIN = /^(?=.{3,253}$)([a-z0-9-]{1,63}\.)+[a-z]{2,24}$/;

export async function runTool(name: string, rawArgs: string, ctx: ToolContext): Promise<unknown> {
  let args: any;
  try {
    args = JSON.parse(rawArgs || "{}");
  } catch {
    return { error: "arguments are not valid JSON" };
  }
  switch (name) {
    case "domain_age":
      return domainAge(String(args.domain ?? ""), ctx);
    case "scam_patterns":
      return { cards: findCards(String(args.topic ?? "")).map(({ id, how, giveaway, safe }) => ({ id, how, giveaway, safe })) };
    case "family_has":
      return { relation: args.relation, present: ctx.familyRelations.includes(String(args.relation)) };
    default:
      return { error: `unknown tool ${name}` };
  }
}

async function domainAge(raw: string, ctx: ToolContext): Promise<unknown> {
  const domain = raw.toLowerCase().replace(/^https?:\/\//, "").replace(/^www\./, "").split(/[/?#]/)[0];
  if (!DOMAIN.test(domain)) return { error: "not a domain" };
  try {
    const res = await (ctx.fetch ?? fetch)(`https://rdap.org/domain/${domain}`, {
      headers: { accept: "application/rdap+json" },
      signal: AbortSignal.timeout(5_000),
    });
    if (res.status === 404) return { domain, registered: false };
    if (!res.ok) return { domain, error: `registry answered ${res.status}` };
    const data: any = await res.json();
    const event = (data.events ?? []).find((e: any) => e.eventAction === "registration");
    if (!event?.eventDate) return { domain, registered: true, registeredOn: null };
    const days = Math.floor(((ctx.now ?? Date.now)() - Date.parse(event.eventDate)) / 86_400_000);
    return { domain, registered: true, registeredOn: event.eventDate.slice(0, 10), ageDays: days };
  } catch {
    return { domain, error: "registry not reachable" };
  }
}
