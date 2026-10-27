/**
 * The guardian's page: what the family member wants to do, in plain words, and two big buttons.
 * "No" comes first: when in doubt, the safe answer is the easy one. No scripts, no tracking.
 */
import type { Approval, Lang } from "./store";

const T = {
  es: {
    title: "Peri te pide permiso",
    send_code: (name: string) => `Tu familiar quiere mandar a ${esc(name)} un mensaje con un código.`,
    circle_change: (name: string) => `Se ha pedido un cambio en los guardianes de tu familiar: ${esc(name)}.`,
    why_send_code: "Peri lo ha parado porque los estafadores piden códigos para entrar en cuentas o mover dinero.",
    why_circle_change:
      "Los cambios en los guardianes necesitan el sí de un guardián y esperan 48 horas, para que nadie pueda quedarse con la protección de tu familiar. Si no lo reconoces, recházalo.",
    advice: "Si tienes dudas, llámale antes de decidir.",
    no_send_code: "No, que no lo mande",
    no_circle_change: "No, que no cambie nada",
    yes: "Sí, lo apruebo",
    expires: (min: number) => `Este enlace caduca en ${min} minutos y solo sirve una vez.`,
    approved: "Aprobado. Peri se lo dirá ahora mismo.",
    rejected: "Rechazado. Peri no lo mandará y se lo dirá con cariño.",
    used: "Este enlace ya se usó. Si no fuiste tú, llama a tu familiar.",
    expired: "Este enlace ha caducado y no se aprobó nada. Si hace falta, Peri pedirá otro.",
    unknown: "Este enlace no es válido.",
    busy: "Demasiados intentos seguidos. Espera un minuto y vuelve a abrir el enlace.",
  },
  en: {
    title: "Peri is asking for your OK",
    send_code: (name: string) => `Your relative wants to send ${esc(name)} a message with a code.`,
    circle_change: (name: string) => `Someone asked to change your relative's guardians: ${esc(name)}.`,
    why_send_code: "Peri stopped it because scammers ask for codes to get into accounts or move money.",
    why_circle_change:
      "Changes to the guardians need a guardian's yes and wait 48 hours, so nobody can take over your relative's protection. If you don't recognise it, decline it.",
    advice: "If you're unsure, give them a call before deciding.",
    no_send_code: "No, don't send it",
    no_circle_change: "No, don't change anything",
    yes: "Yes, I approve",
    expires: (min: number) => `This link expires in ${min} minutes and works only once.`,
    approved: "Approved. Peri will tell them right away.",
    rejected: "Declined. Peri won't send it and will let them know kindly.",
    used: "This link has already been used. If it wasn't you, call your relative.",
    expired: "This link has expired and nothing was approved. Peri will ask again if needed.",
    unknown: "This link isn't valid.",
    busy: "Too many tries in a row. Wait a minute and open the link again.",
  },
};

export function esc(s: string): string {
  return s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]!);
}

export function layout(lang: Lang, body: string, title: string = T[lang].title, css = ""): string {
  return `<!doctype html><html lang="${lang}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex"><title>${title}</title>
<style>
body{font-family:system-ui,sans-serif;background:#fbf7ee;color:#2c2a26;margin:0;padding:24px 16px;font-size:20px;line-height:1.45}
main{max-width:520px;margin:0 auto}h1{font-size:26px;margin:0 0 16px}p{margin:0 0 14px}
form{display:flex;flex-direction:column;gap:14px;margin-top:22px}
button{font:inherit;font-size:22px;padding:18px;border-radius:14px;border:0;cursor:pointer}
.no{background:#2f6b3a;color:#fff}.yes{background:#fff;color:#8a2b1f;border:2px solid #8a2b1f}.small{font-size:16px;color:#6b665c}
${css}</style></head><body><main>${body}</main></body></html>`;
}

export function askPage(a: Approval, token: string, now: number): string {
  const t = T[a.lang];
  const minutes = Math.max(1, Math.ceil((a.expiresAt - now) / 60000));
  const action = `/a/${encodeURIComponent(token)}`;
  return layout(
    a.lang,
    `<h1>🍐 ${t.title}</h1><p><strong>${t[a.kind](a.toName)}</strong></p><p>${t[`why_${a.kind}`]}</p><p>${t.advice}</p>
<form method="post" action="${action}">
<button class="no" name="decision" value="reject">${t[`no_${a.kind}`]}</button>
<button class="yes" name="decision" value="approve">${t.yes}</button>
</form><p class="small">${t.expires(minutes)}</p>`,
  );
}

export function resultPage(lang: Lang, outcome: "approved" | "rejected" | "used" | "expired" | "unknown" | "busy"): string {
  return layout(lang, `<h1>🍐 Peri</h1><p>${T[lang][outcome]}</p>`);
}
