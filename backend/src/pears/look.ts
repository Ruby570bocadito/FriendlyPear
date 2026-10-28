/**
 * A family member's pear on the guardian's page, dressed as on their phone: the calm Peri with the same accessories,
 * placed the same way as the app does (android/.../ui/peri/Outfit.kt) from where her eyes are in that picture. It's a
 * sign only the family knows: a fake page can't show Mamá's Peri in her straw hat.
 */
import type { Lang } from "../approvals/store";

/** Width in eye distances, the point of the picture that goes where, and where (from between the eyes), as in the app. */
const WEAR: Record<string, { size: [number, number]; width: number; ax: number; ay: number; dx: number; dy: number; es: string; en: string }> = {
  STRAW: { size: [512, 272], width: 1.95, ax: 0.5, ay: 0.8, dx: 0, dy: -1.02, es: "un sombrero de paja", en: "a straw hat" },
  BEANIE: { size: [454, 512], width: 1.45, ax: 0.5, ay: 0.9, dx: 0, dy: -0.92, es: "un gorro de lana", en: "a woolly hat" },
  CROWN: { size: [512, 361], width: 1.25, ax: 0.5, ay: 0.92, dx: 0, dy: -1.08, es: "una corona", en: "a crown" },
  FLOWERS: { size: [512, 212], width: 1.55, ax: 0.5, ay: 0.75, dx: 0, dy: -0.98, es: "una corona de flores", en: "a flower crown" },
  BOW: { size: [474, 343], width: 0.72, ax: 0.5, ay: 0.5, dx: 0.06, dy: -1.22, es: "un lazo", en: "a bow" },
  SUNGLASSES: { size: [512, 241], width: 2, ax: 0.5, ay: 0.5, dx: 0, dy: 0, es: "gafas de sol", en: "sunglasses" },
  HEARTS: { size: [512, 224], width: 2.05, ax: 0.5, ay: 0.5, dx: 0, dy: 0, es: "gafas de corazón", en: "heart glasses" },
  GLASSES: { size: [512, 207], width: 2, ax: 0.5, ay: 0.5, dx: 0, dy: 0, es: "gafas", en: "glasses" },
  BOWTIE: { size: [512, 275], width: 0.9, ax: 0.5, ay: 0.5, dx: 0, dy: 0.78, es: "una pajarita", en: "a bow tie" },
  TIE: { size: [176, 512], width: 0.46, ax: 0.5, ay: 0.04, dx: 0, dy: 0.6, es: "una corbata", en: "a tie" },
  BANDANA: { size: [393, 512], width: 0.85, ax: 0.5, ay: 0.05, dx: 0, dy: 0.66, es: "un pañuelo", en: "a neckerchief" },
};

/** The accessories' files under `/w/`, lower case: "straw", "bowtie". */
export const WEAR_NAMES = Object.keys(WEAR).map((k) => k.toLowerCase());

/** The calm Peri's eyes in her 1024 px picture (PeriAnchors.kt). */
const EYES = [408, 465, 603, 471];

/** Only this table's own accessories: never a name from somewhere else ("constructor" is "in" every object). */
function items(outfit: string): string[] {
  return outfit.split(",").filter((k) => Object.hasOwn(WEAR, k));
}

/** The pear, 1:1, as an HTML figure: Peri and each accessory placed in percentages of her picture. */
export function pearFigure(outfit: string, size = 150): string {
  const [lx, ly, rx, ry] = EYES;
  const d = Math.hypot(rx - lx, ry - ly);
  const angle = Math.atan2(ry - ly, rx - lx);
  const cx = (lx + rx) / 2;
  const cy = (ly + ry) / 2;
  const pct = (v: number) => `${((v / 1024) * 100).toFixed(2)}%`;
  const layers = items(outfit).map((key) => {
    const w = WEAR[key];
    const width = w.width * d;
    const height = (width * w.size[1]) / w.size[0];
    const px = cx + Math.cos(angle) * w.dx * d - Math.sin(angle) * w.dy * d;
    const py = cy + Math.sin(angle) * w.dx * d + Math.cos(angle) * w.dy * d;
    const style = `left:${pct(px - w.ax * width)};top:${pct(py - w.ay * height)};width:${pct(width)};transform-origin:${w.ax * 100}% ${w.ay * 100}%;transform:rotate(${((angle * 180) / Math.PI).toFixed(2)}deg)`;
    return `<img src="/w/${key.toLowerCase()}.webp" style="${style}" alt="">`;
  });
  return `<div class="pear" style="width:${size}px;height:${size}px" aria-hidden="true"><img src="/p/calm.webp" style="left:0;top:0;width:100%" alt="">${layers.join("")}</div>`;
}

/** "a straw hat and a bow tie", "un sombrero de paja y una pajarita"; empty if she wears nothing. */
export function outfitWords(outfit: string, lang: Lang): string {
  const words = items(outfit).map((k) => WEAR[k][lang]);
  if (words.length <= 1) return words.join("");
  return `${words.slice(0, -1).join(", ")} ${lang === "es" ? "y" : "and"} ${words[words.length - 1]}`;
}
