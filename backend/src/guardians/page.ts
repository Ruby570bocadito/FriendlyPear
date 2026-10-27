/**
 * The guardian's page: Peri's news about their relative, in plain words, newest first, with a summary of the
 * last seven days. It says plainly what the guardian can't see. No scripts, no tracking, not indexed.
 */
import type { Approval, Lang } from "../approvals/store";
import { esc, layout } from "../approvals/page";
import type { FamilyEvent, GuardianLink } from "./store";

const DAY = 24 * 60 * 60 * 1000;

const CHANNEL = {
  es: { SMS: "un SMS", WHATSAPP: "un WhatsApp", EMAIL: "un correo", BROWSER_NOTIFICATION: "un aviso del navegador" },
  en: { SMS: "a text", WHATSAPP: "a WhatsApp message", EMAIL: "an email", BROWSER_NOTIFICATION: "a browser notification" },
};

const T = {
  es: {
    title: "Novedades de Peri",
    hello: (name: string) => `Hola, ${esc(name)}.`,
    intro: "Esto es lo que Peri ha hecho para cuidar de tu familiar. Tu familiar ve esta misma lista en su móvil.",
    week: "Últimos 7 días",
    warnings: "avisos",
    pauses: "pausas de seguridad",
    approvals: "permisos que pidió",
    calm: "Todo tranquilo: no hay nada que contarte por ahora.",
    scam: (ch: string) => `Peri avisó de ${ch} que parecía un timo, y le dijo qué hacer sin tocar nada.`,
    doubt: (ch: string) => `Peri le propuso comprobar juntos ${ch} que no le cuadraba.`,
    call_family: "Peri le paró un momento al abrir una app de dinero después de algo raro, y decidió llamar a la familia para comprobarlo.",
    leave: "Peri le paró un momento al abrir una app de dinero después de algo raro, y decidió salir de la app.",
    continue: "Peri le paró un momento al abrir una app de dinero después de algo raro, y decidió seguir. Quizá quieras llamarle.",
    send_code: (name: string, s: string) => `Pidió permiso para mandar un código a ${esc(name)}: ${s}.`,
    circle_change: (name: string, s: string) => `Se pidió un cambio de guardianes (${esc(name)}): ${s}.`,
    status: { pending: "pendiente", approved: "aprobado", rejected: "rechazado", callback: "pidió hablarlo antes", expired: "nadie respondió a tiempo" },
    privacy:
      "Nunca verás sus mensajes, llamadas, ubicación ni apps: solo que Peri ha avisado o ha hecho una pausa. Tu familiar puede dejar de compartir esto cuando quiera, y este enlace dejará de funcionar.",
    ago: (ms: number) => ago("es", ms),
  },
  en: {
    title: "Peri's news",
    hello: (name: string) => `Hi ${esc(name)}.`,
    intro: "This is what Peri has done to look after your relative. Your relative sees this same list on their phone.",
    week: "Last 7 days",
    warnings: "warnings",
    pauses: "safety pauses",
    approvals: "approvals asked",
    calm: "All calm: nothing to tell you for now.",
    scam: (ch: string) => `Peri warned about ${ch} that looked like a scam, and told them what to do without touching anything.`,
    doubt: (ch: string) => `Peri suggested checking ${ch} together because something didn't feel right.`,
    call_family: "Peri paused them for a moment when they opened a money app after something worrying, and they chose to call family to check.",
    leave: "Peri paused them for a moment when they opened a money app after something worrying, and they chose to leave the app.",
    continue: "Peri paused them for a moment when they opened a money app after something worrying, and they chose to carry on. You may want to give them a call.",
    send_code: (name: string, s: string) => `They asked for permission to send a code to ${esc(name)}: ${s}.`,
    circle_change: (name: string, s: string) => `A change of guardians was asked (${esc(name)}): ${s}.`,
    status: { pending: "pending", approved: "approved", rejected: "declined", callback: "asked to talk first", expired: "nobody answered in time" },
    privacy:
      "You will never see their messages, calls, location or apps: only that Peri warned them or paused them. Your relative can stop sharing this whenever they like, and this link will stop working.",
    ago: (ms: number) => ago("en", ms),
  },
};

function ago(lang: Lang, ms: number): string {
  const min = Math.max(1, Math.round(ms / 60000));
  if (min < 60) return lang === "es" ? `hace ${min} min` : `${min} min ago`;
  const h = Math.round(min / 60);
  if (h < 24) return lang === "es" ? `hace ${h} h` : `${h} h ago`;
  const d = Math.round(h / 24);
  return lang === "es" ? `hace ${d} ${d === 1 ? "día" : "días"}` : `${d} ${d === 1 ? "day" : "days"} ago`;
}

interface Item {
  at: number;
  text: string;
  tone: "red" | "amber" | "green";
}

export function guardianPage(link: GuardianLink, events: FamilyEvent[], approvals: Approval[], now: number): string {
  const t = T[link.lang];
  const items: Item[] = [
    ...events.map((e): Item => {
      if (e.kind === "warning") {
        const ch = CHANNEL[link.lang][e.channel];
        return { at: e.at, text: e.level === "scam" ? t.scam(ch) : t.doubt(ch), tone: e.level === "scam" ? "red" : "amber" };
      }
      return { at: e.at, text: t[e.answer], tone: e.answer === "continue" ? "amber" : "green" };
    }),
    ...approvals.map((a): Item => ({
      at: a.createdAt,
      text: t[a.kind](a.toName, t.status[a.status]),
      tone: a.status === "approved" ? "green" : "amber",
    })),
  ].sort((x, y) => y.at - x.at);

  const week = now - 7 * DAY;
  const count = (f: (i: FamilyEvent) => boolean) => events.filter((e) => e.at >= week && f(e)).length;
  const summary = [
    [count((e) => e.kind === "warning"), t.warnings],
    [count((e) => e.kind === "pause"), t.pauses],
    [approvals.filter((a) => a.createdAt >= week).length, t.approvals],
  ]
    .map(([n, label]) => `<div class="stat"><strong>${n}</strong><span>${label}</span></div>`)
    .join("");

  const list = items.length
    ? `<ul>${items.map((i) => `<li class="${i.tone}"><div><span class="when">${t.ago(now - i.at)}</span>${i.text}</div></li>`).join("")}</ul>`
    : `<p class="calm">${t.calm}</p>`;

  return layout(
    link.lang,
    `<h1>${t.title}</h1><p>${t.hello(link.name)} ${t.intro}</p>
<h2>${t.week}</h2><div class="stats">${summary}</div>${list}<p class="small">${t.privacy}</p>`,
    t.title,
    // The week's numbers in one card split by thin lines; the news as one list, a small dot for how serious.
    `h1{margin:4px 0 8px}h2{margin:22px 0 10px;color:#1f2a1a}
.stats{display:flex;background:#fff;border:1px solid #ede4d0;border-radius:24px}.stat{flex:1;min-width:0;padding:14px 16px}.stat+.stat{border-left:1px solid #ede4d0}
.stat strong{display:block;font-size:26px;line-height:1.2;font-variant-numeric:tabular-nums}.stat span{font-size:15px;color:#3f4d39}
ul{list-style:none;padding:0;margin:16px 0;background:#fff;border:1px solid #ede4d0;border-radius:24px}
li{display:flex;gap:12px;align-items:baseline;padding:14px 18px}li+li{border-top:1px solid #f3ecdd}
li:before{content:"";flex:none;width:10px;height:10px;border-radius:50%;background:#7cc242}li.red:before{background:#c2412d}li.amber:before{background:#d08a00}
.when{display:block;font-size:15px;color:#5e5a50}.calm{background:#fff;border:1px solid #ede4d0;border-radius:24px;padding:16px 18px}`,
  );
}
