/**
 * Short cards of the scam scripts that reach families in Spain and the UK, for the investigator to compare a
 * doubtful message with. Each card: how it works, the giveaway and the safe action. Defensive use only.
 */
export interface ScamCard {
  id: string;
  keywords: string[];
  how: string;
  giveaway: string;
  safe: string;
}

export const CARDS: ScamCard[] = [
  {
    id: "family_new_number",
    keywords: ["new number", "número nuevo", "numero nuevo", "mamá", "mama", "mum", "mom", "phone broke", "se me ha roto", "bizum"],
    how: "Someone writes as a son, daughter or grandchild from an unknown number, says their phone broke and soon asks for money for an urgent bill.",
    giveaway: "Unknown number claiming to be family plus a money request, often with excuses not to call.",
    safe: "Call the relative on the number already saved.",
  },
  {
    id: "family_emergency",
    keywords: ["accident", "accidente", "hospital", "police", "policía", "detenido", "arrested", "abuela", "grandma"],
    how: "A relative supposedly had an accident or is detained and needs money now, sometimes with a cloned voice.",
    giveaway: "Urgency, secrecy ('don't tell mum') and payment by transfer or Bizum.",
    safe: "Hang up and call the relative or another family member on saved numbers; ask the family word.",
  },
  {
    id: "parcel_fee",
    keywords: ["correos", "seur", "dhl", "ups", "royal mail", "evri", "parcel", "paquete", "customs", "aduana", "fee", "tasas"],
    how: "A delivery company says a parcel is held for a small fee and links to a payment page that steals card details.",
    giveaway: "Tiny amount, link to a domain that is not the company's, urgency.",
    safe: "Don't touch the link; check in the company's official app or website typed by hand.",
  },
  {
    id: "bank_access",
    keywords: ["bank", "banco", "account", "cuenta", "blocked", "bloqueada", "suspicious", "verify", "verifique", "acceso"],
    how: "The 'bank' warns of a suspicious login and asks to verify identity on a link or to give the code just sent.",
    giveaway: "Link to a look-alike domain, or a request for a one-time code or password.",
    safe: "Never give codes; call the bank on the number on the card.",
  },
  {
    id: "bank_code_call",
    keywords: ["código", "codigo", "code", "otp", "llamada", "call", "anular", "cancel the operation"],
    how: "A fake bank employee calls and asks for the code that is about to arrive 'to cancel a fraudulent operation'.",
    giveaway: "A code arriving during a call, and someone asking for it.",
    safe: "Hang up; the code is what lets them move the money.",
  },
  {
    id: "tax_refund",
    keywords: ["hacienda", "agencia tributaria", "aeat", "hmrc", "irs", "refund", "reembolso", "devolución"],
    how: "The tax office supposedly owes a refund and asks for bank or card details on a link.",
    giveaway: "Tax offices don't announce refunds by SMS with links.",
    safe: "Check in the official tax office website or app.",
  },
  {
    id: "tech_support",
    keywords: ["microsoft", "virus", "infected", "infectado", "anydesk", "teamviewer", "soporte", "support", "technician"],
    how: "Fake support says the device is infected and asks to install a remote-control app.",
    giveaway: "Remote-control apps and unsolicited support.",
    safe: "Don't install anything; hang up and ask family.",
  },
  {
    id: "prize_investment",
    keywords: ["prize", "premio", "won", "ganado", "lottery", "sorteo", "invest", "invierte", "crypto", "bitcoin", "returns"],
    how: "A prize to claim or an investment with guaranteed returns, after a small payment or details.",
    giveaway: "Too good to be true, payment to receive money.",
    safe: "Ignore it; nobody gives prizes you didn't enter.",
  },
  {
    id: "utilities_cut",
    keywords: ["luz", "electricity", "iberdrola", "endesa", "naturgy", "corte", "cut off", "impago", "unpaid"],
    how: "The power company threatens to cut supply today unless a bill is paid on a link.",
    giveaway: "Same-day threat and a payment link.",
    safe: "Check with the company on the number on a real bill.",
  },
  {
    id: "subscription",
    keywords: ["netflix", "amazon", "spotify", "subscription", "suscripción", "payment failed", "pago", "card", "tarjeta"],
    how: "A subscription service says the payment failed and asks to update the card on a link.",
    giveaway: "Link outside the official domain.",
    safe: "Open the official app to check the account.",
  },
  {
    id: "romance",
    keywords: ["love", "amor", "cariño", "darling", "my dear", "mi vida", "soldier", "militar", "plataforma petrolífera", "oil rig", "meet you", "conocerte", "visa", "flight", "billete"],
    how: "A friendly stranger builds an online relationship for weeks, then needs money for a flight, a visa, customs or a medical bill.",
    giveaway: "Never on video, always an emergency abroad, and money sent to a third party.",
    safe: "Never send money to someone you haven't met in person; talk it over with family.",
  },
  {
    id: "job_task",
    keywords: ["job", "trabajo", "empleo", "part-time", "media jornada", "earn", "gana", "€/día", "per day", "likes", "reseñas", "reviews", "tasks", "tareas", "telegram"],
    how: "A recruiter offers easy money for liking videos or writing reviews, pays a little at first, then asks for a deposit to unlock bigger earnings.",
    giveaway: "Unsolicited offer, pay for simple online tasks, and a deposit to withdraw your money.",
    safe: "Real jobs never ask you to pay; stop answering and don't send any deposit.",
  },
  {
    id: "marketplace",
    keywords: ["wallapop", "vinted", "milanuncios", "marketplace", "ebay", "comprador", "buyer", "envío", "shipping", "recibir el pago", "receive payment", "qr"],
    how: "A buyer on a second-hand site pays 'through the platform' and sends a link or QR where you must enter your card to receive the money.",
    giveaway: "A link outside the app to receive a payment, or a request for your card details to get paid.",
    safe: "Keep the chat and the payment inside the official app; you never need a card to receive money.",
  },
  {
    id: "fake_police",
    keywords: ["policía", "police", "guardia civil", "detective", "fraude en su cuenta", "fraud on your account", "safe account", "cuenta segura", "investigación", "investigation", "juzgado", "court"],
    how: "Someone posing as the police or the bank's fraud team says your money is at risk and asks you to move it to a 'safe account' or hand over cash or cards.",
    giveaway: "Real police never ask you to move money, give codes or hand over cards; pressure to keep it secret.",
    safe: "Hang up and call the bank or the police on their official numbers, from another phone if you can.",
  },
  {
    id: "loan_fee",
    keywords: ["préstamo", "loan", "crédito", "credit", "approved", "aprobado", "sin aval", "no checks", "comisión de apertura", "upfront fee", "seguro del préstamo"],
    how: "A quick loan is approved without checks, but you must first pay a fee, insurance or tax to receive it.",
    giveaway: "Paying in advance to receive a loan; the money never arrives.",
    safe: "Never pay to get a loan; check the lender with your bank first.",
  },
];

/** The cards whose keywords appear in the topic, best match first, at most three. */
export function findCards(topic: string): ScamCard[] {
  const t = topic.toLowerCase();
  return CARDS.map((c) => ({ c, hits: c.keywords.filter((k) => t.includes(k)).length }))
    .filter((x) => x.hits > 0)
    .sort((a, b) => b.hits - a.hits)
    .slice(0, 3)
    .map((x) => x.c);
}
