import { AbsoluteFill, Audio, Img, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { BODY, clamp, HEADING } from "./theme";
import { Burst, Chip, CONFETTI, img, LOOK, Peri, Phone, Scene, Words } from "./Minute";

/**
 * The 30-second cut of the one-minute demo, for Discord: the same pieces, faster, and an end card that says who
 * made it. 16:9 at 30 fps; music is scripts/minute_music.py thirty. Scene changes fall on the music's bars.
 */
export const THIRTY_FRAMES = 900;

const T = {
  hook: [0, 120],
  meet: [120, 240],
  number: [240, 420],
  code: [420, 600],
  family: [600, 720],
  end: [720, 900],
} as const;

const Hook = () => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  const L = LOOK.hook;
  const drop = spring({ frame: f - 2, fps, config: { damping: 12, stiffness: 80 } });
  const swing = Math.sin(f / 9) * 9 * Math.exp(-f / 60) + Math.sin(f / 25) * 2;
  return (
    <>
      <div style={{ position: "absolute", left: 1130, top: -60, width: 760, height: 760, translate: `0 ${interpolate(drop, [0, 1], [-800, 0])}px`, rotate: `${swing}deg`, transformOrigin: "50% 0%" }}>
        <Img src={img("hook")} style={{ width: "100%", height: "100%" }} />
        <div
          style={{
            position: "absolute",
            left: "28%",
            top: "64%",
            width: "44%",
            height: "18%",
            display: "grid",
            placeItems: "center",
            textAlign: "center",
            fontFamily: BODY,
            fontWeight: 700,
            fontSize: 27,
            lineHeight: 1.2,
            color: "#2B1436",
            opacity: interpolate(f, [18, 26], [0, 1], clamp),
          }}
        >
          Mum, it's me. New phone. Can you send money?
        </div>
      </div>
      <Words text="Messages like this trick someone we love every day." delay={10} size={96} color={L.ink} shade={L.shade} accent={{ word: "trick", color: "#FF8A7A" }} style={{ left: 120, top: 330, width: 1000 }} />
    </>
  );
};

const Meet = () => {
  const L = LOOK.meet;
  return (
    <>
      <Peri name="wave" x={600} y={1010} size={860} delay={4} />
      <Burst x={600} y={420} delay={16} colors={CONFETTI} />
      <Words text="Meet Peri." delay={12} size={150} color={L.ink} shade={L.shade} style={{ left: 1060, top: 300, width: 800 }} />
      <Words text="A clay pear in your phone that stops scams." delay={30} size={58} color={L.ink} shade="transparent" style={{ left: 1066, top: 520, width: 760 }} />
    </>
  );
};

const NewNumber = () => {
  const L = LOOK.number;
  return (
    <>
      <Phone clip="m1_newnumber" cuts={[[5, 13], [23.3, 30]]} rate={2.5} x={480} y={560} h={940} delay={2} tilt={-5} />
      <Words text="A new number says it's family?" delay={8} size={80} color={L.ink} shade={L.shade} style={{ left: 860, top: 110, width: 980 }} />
      <Chip text="Peri spots the trap" icon="1" delay={36} x={880} y={340} bg="#FFF8EC" color="#123049" />
      <Chip text="Finds the real Ana" icon="2" delay={80} x={880} y={455} bg="#FFF8EC" color="#123049" />
      <Chip text={'One spoken "yes" calls her'} icon="3" delay={120} x={880} y={570} bg="#25541A" color="#FFF8EC" />
      <Peri name="detective" x={1640} y={1090} size={470} delay={16} />
    </>
  );
};

const Code = () => {
  const f = useCurrentFrame();
  const L = LOOK.code;
  const pulse = 1 + Math.max(0, Math.sin(f / 4.5)) * 0.04 * interpolate(f, [60, 80], [0, 1], clamp);
  return (
    <>
      <Peri name="protective" x={380} y={1060} size={700} delay={6} />
      <Words text="A code during a call?" delay={6} size={84} color={L.ink} shade={L.shade} style={{ left: 760, top: 120, width: 700 }} />
      <div style={{ position: "absolute", left: 760, top: 350, width: 600, scale: `${pulse}`, transformOrigin: "0% 50%" }}>
        <Words text="Hang up. It's always a scam." delay={55} size={92} color="#FFE07A" shade={L.shade} style={{ position: "relative" }} />
      </div>
      <Chip text="A hard rule no AI can overrule" delay={110} x={760} y={760} bg="#FFF8EC" color="#8E1F18" />
      <Phone clip="m3_code" cuts={[[4, 9], [14, 26]]} rate={2.6} x={1620} y={560} h={940} delay={2} tilt={4} shake={6} />
    </>
  );
};

const Family = () => {
  const L = LOOK.family;
  return (
    <>
      <Peri name="heartfamily" x={360} y={1040} size={640} delay={4} />
      <Words text="Something risky? Peri asks the family." delay={6} size={76} color={L.ink} shade={L.shade} style={{ left: 700, top: 110, width: 760 }} />
      <Chip text="No" delay={40} x={720} y={420} bg="#25541A" color="#FFF8EC" />
      <Chip text="Call me first" delay={50} x={870} y={420} bg="#FFF8EC" color="#2A1A5E" />
      <Chip text="Yes" delay={60} x={1230} y={420} bg="#FFF8EC" color="#2A1A5E" />
      <Words text="Without ever reading your messages." delay={72} size={50} color={L.ink} shade="transparent" style={{ left: 722, top: 580, width: 700 }} />
      <Phone clip="m6_family_ana" cuts={[[45, 72]]} rate={1.5} x={1640} y={560} h={940} delay={2} tilt={5} />
    </>
  );
};

/** The end card: the app, Peri's promise, and who built it. */
const End = () => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  const L = LOOK.end;
  const logo = spring({ frame: f - 14, fps, config: { damping: 10, stiffness: 120 } });
  return (
    <>
      <Peri name="hero" x={500} y={1040} size={820} delay={2} />
      <Burst x={500} y={360} delay={10} count={34} colors={CONFETTI} />
      <div style={{ position: "absolute", left: 1000, top: 150, display: "flex", alignItems: "center", gap: 26, scale: `${logo}`, transformOrigin: "0% 50%" }}>
        <Img src={img("icon")} style={{ width: 150, height: 150, borderRadius: "50%", boxShadow: "0 8px 0 rgba(0,0,0,0.12)" }} />
        <span style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 112, color: L.ink, textShadow: `0 7px 0 ${L.shade}` }}>FriendlyPear</span>
      </div>
      <Words text="You relax. I'll keep watch." delay={28} size={74} color={L.ink} shade="transparent" style={{ left: 1010, top: 360, width: 820 }} />
      <Chip text="91% of unseen scams flagged" delay={60} x={1010} y={560} bg="#FFF8EC" color="#1F3A12" />
      <Words text="Built by Rafa" delay={84} size={60} color={L.ink} shade={L.shade} style={{ left: 1012, top: 700, width: 820 }} />
      <Words text="Meta Global AI Developer Hackathon" delay={96} size={34} color={L.ink} shade="transparent" style={{ left: 1016, top: 790, width: 820 }} />
    </>
  );
};

export const Thirty = () => (
  <AbsoluteFill style={{ background: "#2B1436" }}>
    <Audio src={staticFile("minute/music30.wav")} />
    <Scene at={T.hook} look={LOOK.hook} origin="50% 50%">
      <Hook />
    </Scene>
    <Scene at={T.meet} look={LOOK.meet} origin="70% 40%" rays>
      <Meet />
    </Scene>
    <Scene at={T.number} look={LOOK.number} origin="30% 50%">
      <NewNumber />
    </Scene>
    <Scene at={T.code} look={LOOK.code} origin="85% 50%">
      <Code />
    </Scene>
    <Scene at={T.family} look={LOOK.family} origin="20% 70%">
      <Family />
    </Scene>
    <Scene at={T.end} look={LOOK.end} origin="30% 60%" rays last>
      <End />
    </Scene>
  </AbsoluteFill>
);
