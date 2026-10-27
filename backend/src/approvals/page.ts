/**
 * The guardian's page: what the family member wants to do, in plain words, Peri's face, and three big answers.
 * "No" comes first and "Yes" last, behind a second "are you sure?" page: when in doubt, the safe answer is the
 * easy one. No scripts, no tracking, no third-party anything; images come from this server only.
 */
import type { Approval, Lang } from "./store";

const T = {
  es: {
    title: "Peri te pide permiso",
    heading: "Tu familiar te necesita",
    send_code: (name: string) => `Tu familiar quiere mandar a ${esc(name)} un mensaje con un código.`,
    circle_change: (name: string) => `Se ha pedido un cambio en los guardianes de tu familiar: ${esc(name)}.`,
    alert_title: "Posible timo en curso",
    on_call: "Está hablando, o acaba de hablar, por teléfono con un número que no tiene guardado. Así suelen actuar los estafadores.",
    why_title: "Por qué Peri lo ha parado",
    why_send_code: "Peri lo ha parado porque los estafadores piden códigos para entrar en cuentas o mover dinero.",
    why_circle_change:
      "Los cambios en los guardianes necesitan el sí de un guardián y esperan 48 horas, para que nadie pueda quedarse con la protección de tu familiar. Si no lo reconoces, recházalo.",
    meanwhile_title: "Mientras decides",
    meanwhile: "Peri no manda nada. Si no contestas, tampoco se manda.",
    advice: "Si tienes dudas, llámale antes de decidir.",
    no_send_code: "No, que no lo mande",
    no_circle_change: "No, que no cambie nada",
    ask_call: "Que me llame primero",
    yes: "Sí, lo apruebo",
    expires: (min: number) => `Este enlace caduca en ${min} ${min === 1 ? "minuto" : "minutos"} y solo sirve una vez.`,
    never_code: "Peri nunca te enseñará el código ni con quién hablaba.",
    in_app: "¿Tienes FriendlyPear? Esta petición también te aparece en tu Peri.",
    confirm_title: "¿Seguro que lo apruebas?",
    confirm_intro: "Apruébalo solo si puedes decir que sí a las tres:",
    confirm_1: "Has hablado con tu familiar, por teléfono o en persona.",
    confirm_2: (name: string) => `Sabes quién es ${esc(name)} y es de confianza.`,
    confirm_3: "Nadie os está metiendo prisa.",
    confirm_no: "No, mejor no",
    confirm_yes: "Sí, lo he comprobado",
    back: "Volver",
    approved: "Aprobado. Peri se lo dirá ahora mismo.",
    rejected: "Rechazado. Peri no lo mandará y se lo dirá con cariño.",
    callback: "Peri no manda nada y le ofrece llamarte ahora mismo. Ten el teléfono a mano.",
    used: "Este enlace ya se usó. Si no fuiste tú, llama a tu familiar.",
    expired: "Este enlace ha caducado y no se aprobó nada. Si hace falta, Peri pedirá otro.",
    unknown: "Este enlace no es válido.",
    busy: "Demasiados intentos seguidos. Espera un minuto y vuelve a abrir el enlace.",
    thanks: "Gracias por cuidar de tu familia.",
  },
  en: {
    title: "Peri is asking for your OK",
    heading: "Your relative needs you",
    send_code: (name: string) => `Your relative wants to send ${esc(name)} a message with a code.`,
    circle_change: (name: string) => `Someone asked to change your relative's guardians: ${esc(name)}.`,
    alert_title: "Possible scam in progress",
    on_call: "They're on the phone, or just were, with a number they haven't saved. That's how scammers usually work.",
    why_title: "Why Peri stopped it",
    why_send_code: "Peri stopped it because scammers ask for codes to get into accounts or move money.",
    why_circle_change:
      "Changes to the guardians need a guardian's yes and wait 48 hours, so nobody can take over your relative's protection. If you don't recognise it, decline it.",
    meanwhile_title: "While you decide",
    meanwhile: "Peri sends nothing. If you don't answer, nothing is sent either.",
    advice: "If you're unsure, give them a call before deciding.",
    no_send_code: "No, don't send it",
    no_circle_change: "No, don't change anything",
    ask_call: "Ask them to call me first",
    yes: "Yes, I approve",
    expires: (min: number) => `This link expires in ${min} ${min === 1 ? "minute" : "minutes"} and works only once.`,
    never_code: "Peri never shows you the code or who they were talking to.",
    in_app: "Got FriendlyPear? This request also shows up in your Peri.",
    confirm_title: "Are you sure you approve?",
    confirm_intro: "Only approve it if you can say yes to all three:",
    confirm_1: "You've spoken to your relative, on the phone or in person.",
    confirm_2: (name: string) => `You know who ${esc(name)} is and trust them.`,
    confirm_3: "Nobody is rushing either of you.",
    confirm_no: "No, better not",
    confirm_yes: "Yes, I've checked",
    back: "Back",
    approved: "Approved. Peri will tell them right away.",
    rejected: "Declined. Peri won't send it and will let them know kindly.",
    callback: "Peri sends nothing and offers to call you right now. Keep your phone close.",
    used: "This link has already been used. If it wasn't you, call your relative.",
    expired: "This link has expired and nothing was approved. Peri will ask again if needed.",
    unknown: "This link isn't valid.",
    busy: "Too many tries in a row. Wait a minute and open the link again.",
    thanks: "Thank you for looking after your family.",
  },
};

export type Outcome = "approved" | "rejected" | "callback" | "used" | "expired" | "unknown" | "busy";

/** Which picture of Peri goes with each moment (served by this server from backend/public/). */
const ART: Record<Outcome | "ask" | "alert" | "confirm", string> = {
  ask: "guardian",
  alert: "urgent",
  confirm: "thinking",
  approved: "thumbsup",
  rejected: "protective",
  callback: "calling",
  used: "sorry",
  expired: "sorry",
  unknown: "sorry",
  busy: "waiting",
};

export function esc(s: string): string {
  return s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]!);
}

const peri = (name: string, size = 112) => `<img class="peri" src="/p/${name}.webp" width="${size}" height="${size}" alt="">`;

/**
 * Peri's look, the same as in the app: one flat cream ground, white cards set apart by a thin edge (no shadows),
 * green for the one safe answer, ink text at 7:1 or more. Three type sizes, big targets (at least 56 px), for
 * guardians who are older too.
 */
const CSS = `*{box-sizing:border-box}
body{font-family:system-ui,-apple-system,"Segoe UI",Roboto,sans-serif;background:#fff8ec;color:#1f2a1a;margin:0;padding:16px 16px 32px;font-size:19px;line-height:1.45}
main{max-width:520px;margin:0 auto}
.brand{margin:0 0 6px;font-size:19px;font-weight:800;color:#25541a}
.hero{display:flex;align-items:center;gap:10px;margin:0 0 14px}.peri{flex:none}
h1{font-size:26px;line-height:1.15;margin:0}h2{font-size:19px;margin:0 0 4px;color:#25541a}p{margin:0 0 10px}
.card{background:#fff;border-radius:24px;padding:16px 18px;margin:0 0 12px;border:1px solid #ede4d0}
.ask{font-weight:700;line-height:1.35}
.alert{background:#fff1d6;border:1.5px solid #f2c46d;display:flex;gap:12px;align-items:flex-start}
.alert .icon{flex:none;width:32px;height:32px;border-radius:50%;background:#9c1f17;color:#fff;font-weight:800;display:flex;align-items:center;justify-content:center}
.alert strong{display:block;color:#6b3e00}.alert p{margin:2px 0 0;color:#4a2b00;font-size:15px}
.card p:last-child{margin-bottom:0}.row{display:flex;gap:10px;align-items:flex-start}.row+.row{margin-top:10px}
form{display:flex;flex-direction:column;gap:12px;margin:18px 0 14px}
button,.button{font:inherit;font-weight:600;min-height:60px;padding:14px 18px;border-radius:999px;border:0;cursor:pointer;text-align:center;text-decoration:none;display:block}
.no{background:#25541a;color:#fff}
.call{background:#fff;color:#25541a;border:1.5px solid #d8ccb2}
.yes{background:#fff;color:#8a2b1f;border:1.5px solid #8a2b1f;min-height:56px}
button:active{transform:scale(.98)}
button:focus-visible,.button:focus-visible{outline:4px solid #f2c46d;outline-offset:3px}
.check{list-style:none;padding:0;margin:8px 0 0}.check li{padding:8px 0 8px 36px;position:relative}
.check li:before{content:"";position:absolute;left:0;top:10px;width:22px;height:22px;border-radius:7px;border:3px solid #25541a}
.small{font-size:15px;color:#3f4d39;margin:6px 0}.center{text-align:center}
.done{text-align:center;padding-top:16px}.done .peri{margin:0 auto 10px;display:block}.done h1{margin-bottom:10px}`;

export function layout(lang: Lang, body: string, title: string = T[lang].title, css = ""): string {
  return `<!doctype html><html lang="${lang}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex"><meta name="theme-color" content="#fff8ec"><title>${title}</title>
<style>${CSS}${css}</style></head><body><main><p class="brand">FriendlyPear</p>${body}</main></body></html>`;
}

export function askPage(a: Approval, token: string, now: number): string {
  const t = T[a.lang];
  const minutes = Math.max(1, Math.ceil((a.expiresAt - now) / 60000));
  const action = `/a/${encodeURIComponent(token)}`;
  const alert = a.onCall
    ? `<section class="card alert"><span class="icon" aria-hidden="true">!</span><div><strong>${t.alert_title}</strong><p>${t.on_call}</p></div></section>`
    : "";
  return layout(
    a.lang,
    `<header class="hero">${peri(a.onCall ? ART.alert : ART.ask)}<h1>${t.heading}</h1></header>
<section class="card ask"><p>${t[a.kind](a.toName)}</p></section>${alert}
<form method="post" action="${action}">
<button class="no" name="decision" value="reject">${t[`no_${a.kind}`]}</button>
<button class="call" name="decision" value="callback">${t.ask_call}</button>
<button class="yes" name="decision" value="approve">${t.yes}</button>
</form>
<p class="small">${t.expires(minutes)}</p>
<section class="card"><h2>${t.why_title}</h2><p>${t[`why_${a.kind}`]}</p><h2>${t.meanwhile_title}</h2><p>${t.meanwhile} ${t.advice}</p></section>
<p class="small">${t.never_code}</p><p class="small">${t.in_app}</p>`,
  );
}

/** The second step before a yes: three plain checks, and the safe answer still first. */
export function confirmPage(a: Approval, token: string): string {
  const t = T[a.lang];
  const action = `/a/${encodeURIComponent(token)}`;
  return layout(
    a.lang,
    `<header class="hero">${peri(ART.confirm)}<h1>${t.confirm_title}</h1></header>
<section class="card"><p>${t.confirm_intro}</p><ul class="check"><li>${t.confirm_1}</li><li>${t.confirm_2(a.toName)}</li><li>${t.confirm_3}</li></ul></section>
<form method="post" action="${action}">
<button class="no" name="decision" value="reject">${t.confirm_no}</button>
<input type="hidden" name="confirm" value="yes">
<button class="yes" name="decision" value="approve">${t.confirm_yes}</button>
</form><p><a class="small" href="${action}">${t.back}</a></p>`,
  );
}

export function resultPage(lang: Lang, outcome: Outcome): string {
  const t = T[lang];
  const thanks = outcome === "approved" || outcome === "rejected" || outcome === "callback" ? `<p class="small">${t.thanks}</p>` : "";
  return layout(lang, `<section class="done">${peri(ART[outcome], 160)}<h1>Peri</h1><p>${t[outcome]}</p>${thanks}</section>`);
}
