import { AbsoluteFill, Img } from "remotion";
import { INK, pic } from "./cast";
import { BODY, HEADING } from "./theme";

/**
 * The thumbnail of "Peri's diary" (1280x720, for YouTube and Devpost), with no spoilers: a face-off on a torn page.
 * On the left, in his purple den, the crook is on the phone ("Hi Mum, it's me!", his first line in the video); on the
 * right, on bright paper, Peri the detective leans in with her magnifying glass. It asks who wins and doesn't say.
 */
const PERI = { left: 610, top: 96, size: 660 };

/** The tear between the two halves: a slanted line from (720, 0) to (600, 720) with a ragged paper edge. */
const tear = (shift: number) => {
  const edge = Array.from({ length: 25 }, (_, i) => {
    const y = (i / 24) * 720;
    const x = 720 - (i / 24) * 120 + (i % 2 ? 11 : -9) + ((i * 7) % 5) + shift;
    return `${x}px ${y}px`;
  });
  return `polygon(1280px 0px, ${edge.join(", ")}, 1280px 720px)`;
};

export const Thumb = () => (
  <AbsoluteFill style={{ overflow: "hidden", background: "#2B1D3A" }}>
    {/* The crook's half: big, on the phone, in purple lamplight. */}
    <Img src={pic("short/crook_phone")} style={{ position: "absolute", left: -40, top: -150, width: 800, height: 1200, objectFit: "cover" }} />
    <AbsoluteFill style={{ background: "linear-gradient(90deg, rgba(43,29,58,0.1) 0%, rgba(43,29,58,0.35) 50%, rgba(43,29,58,0) 60%)" }} />

    {/* The torn page: a white paper edge, then Peri's bright half. */}
    <AbsoluteFill style={{ background: "#FFFDF4", clipPath: tear(-16), boxShadow: "none" }} />
    <AbsoluteFill style={{ background: "radial-gradient(circle at 72% 42%, #FFF6CF 0%, #FFE38A 38%, #F9C74F 75%, #EFA93A 100%)", clipPath: tear(0) }} />
    <AbsoluteFill style={{ clipPath: tear(0), background: "repeating-conic-gradient(from 0deg at 72% 46%, rgba(255,255,255,0.22) 0deg 8deg, rgba(255,255,255,0) 8deg 20deg)" }} />

    {/* Peri the detective, turned to face him, standing on her shadow. */}
    <div style={{ position: "absolute", left: PERI.left + PERI.size * 0.3, top: PERI.top + PERI.size * 0.9, width: PERI.size * 0.42, height: 40, borderRadius: "50%", background: "radial-gradient(closest-side, rgba(120,60,0,0.38), rgba(120,60,0,0))" }} />
    <Img src={pic("diary/peri/peri_detective")} style={{ position: "absolute", left: PERI.left, top: PERI.top, width: PERI.size, height: PERI.size, scale: "-1 1", filter: "drop-shadow(0 18px 24px rgba(90,50,0,0.35))" }} />

    {/* What the crook says. */}
    <div style={{ position: "absolute", left: 26, top: 500, padding: "14px 30px 18px", borderRadius: 30, background: "#FFFFFF", boxShadow: "0 14px 30px rgba(0,0,0,0.45)", rotate: "-3deg", fontFamily: HEADING, fontWeight: 700, fontSize: 70, color: INK, whiteSpace: "nowrap" }}>
      <div style={{ position: "absolute", left: 120, top: -30, width: 0, height: 0, borderLeft: "22px solid transparent", borderRight: "22px solid transparent", borderBottom: "38px solid #FFFFFF" }} />
      “Hi Mum, it's me!”
    </div>

    {/* Who made it, on Peri's side. */}
    <div style={{ position: "absolute", right: 30, top: 24, display: "flex", alignItems: "center", gap: 10, padding: "8px 20px 8px 10px", borderRadius: 999, background: "rgba(255,253,244,0.95)", boxShadow: "0 6px 16px rgba(120,70,0,0.3)" }}>
      <Img src={pic("peri7/calm.webp")} style={{ width: 46, height: 46 }} />
      <span style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 34, color: "#25541A" }}>FriendlyPear</span>
      <span style={{ fontFamily: BODY, fontWeight: 700, fontSize: 24, color: INK }}>· Peri's Diary</span>
    </div>
  </AbsoluteFill>
);
