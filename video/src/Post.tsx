import { AbsoluteFill, Img, staticFile } from "remotion";
import { BODY, HEADING } from "./theme";
import { img } from "./Minute";

/** An Instagram feed post (1080x1350, 4:5): the scam text, Peri under her umbrella, and the app's name. A still. */
const INK = "#1F2A1A";
const SMS = "Hi Mum, it's Ana. My phone broke and this is my new number. Can you send me 380 today for a bill?";

export const Post = () => (
  <AbsoluteFill style={{ background: "radial-gradient(circle at 35% 25%, #FFF8EC, #FFE9A8 55%, #FFC83D 100%)", overflow: "hidden" }}>
    <AbsoluteFill style={{ backgroundImage: "radial-gradient(rgba(217,155,0,0.18) 1.4px, transparent 1.6px)", backgroundSize: "10px 10px" }} />
    <div style={{ position: "absolute", left: 70, right: 70, top: 70, fontFamily: HEADING, fontWeight: 700, fontSize: 96, lineHeight: 1.02, color: INK, textShadow: "0 6px 0 rgba(217,155,0,0.45)" }}>
      Would your mum spot this scam?
    </div>
    <div style={{ position: "absolute", left: 70, top: 360, width: 620, rotate: "-3deg" }}>
      <div style={{ background: "#FFFFFF", borderRadius: 40, padding: "26px 32px 30px", boxShadow: "0 22px 50px rgba(0,0,0,0.22)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 14, marginBottom: 12 }}>
          <div style={{ width: 52, height: 52, borderRadius: 26, background: "#D8DEE8", display: "grid", placeItems: "center", fontFamily: HEADING, fontWeight: 700, fontSize: 26, color: "#5B6575" }}>?</div>
          <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 30, color: "#2A2F38" }}>+44 7700 900123</div>
        </div>
        <div style={{ fontFamily: BODY, fontSize: 38, lineHeight: 1.28, color: "#1E2228" }}>{SMS}</div>
      </div>
      <div style={{ position: "absolute", right: -70, top: -56, padding: "12px 28px", borderRadius: 20, border: "8px solid #E5533D", color: "#E5533D", background: "rgba(255,248,236,0.95)", fontFamily: HEADING, fontWeight: 700, fontSize: 62, rotate: "10deg" }}>
        NOT ANA
      </div>
    </div>
    <Img src={img("protective")} style={{ position: "absolute", right: -40, bottom: 150, width: 620, height: 620, filter: "drop-shadow(0 22px 26px rgba(0,0,0,0.2))" }} />
    <div style={{ position: "absolute", left: 70, top: 900, width: 560, fontFamily: HEADING, fontWeight: 700, fontSize: 54, lineHeight: 1.12, color: INK }}>
      Peri does. And she stops it before any money moves.
    </div>
    <div style={{ position: "absolute", left: 0, right: 0, bottom: 0, height: 150, background: "#25541A", display: "flex", alignItems: "center", justifyContent: "space-between", padding: "0 70px" }}>
      <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
        <Img src={staticFile("minute/icon.png")} style={{ width: 92, height: 92, borderRadius: "50%" }} />
        <span style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 58, color: "#FFF8EC" }}>FriendlyPear</span>
      </div>
      <span style={{ fontFamily: BODY, fontWeight: 700, fontSize: 30, color: "#D6F0A6", textAlign: "right", lineHeight: 1.2 }}>
        Open-source Android app
        <br />
        Built for the Meta AI Hackathon
      </span>
    </div>
  </AbsoluteFill>
);
