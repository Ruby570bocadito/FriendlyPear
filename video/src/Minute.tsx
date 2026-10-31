import type { CSSProperties, ReactNode } from "react";
import { AbsoluteFill, Audio, Img, interpolate, OffthreadVideo, Sequence, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { BODY, clamp, HEADING } from "./theme";

/**
 * The one-minute demo, 16:9 at 30 fps. Peri's poses were cut out of their cream backgrounds with Adobe Photoshop
 * (the Adobe connector's remove-background), the app's real recordings play in a phone, and the music is
 * scripts/minute_music.py. Scene changes fall on the music's bars (every 2 s).
 */
export const MINUTE_FRAMES = 1800;

const S = {
  hook: [0, 180],
  meet: [180, 360],
  number: [360, 720],
  code: [720, 1080],
  family: [1080, 1380],
  proof: [1380, 1560],
  end: [1560, 1800],
} as const;

type Look = { from: string; to: string; blob: string; ink: string; shade: string };
export const LOOK: Record<keyof typeof S, Look> = {
  hook: { from: "#5A2D6B", to: "#2B1436", blob: "#7B3F8F", ink: "#FFF8EC", shade: "#1A0C21" },
  meet: { from: "#D6F0A6", to: "#8CCB4E", blob: "#B5E07A", ink: "#1F3A12", shade: "#5E9A2C" },
  number: { from: "#DDF2FF", to: "#7FCBFF", blob: "#B4E2FF", ink: "#123049", shade: "#4E9BD0" },
  code: { from: "#D9503F", to: "#8E1F18", blob: "#EA6B58", ink: "#FFF8EC", shade: "#5C120D" },
  family: { from: "#ECE4FF", to: "#B39BFF", blob: "#D5C6FF", ink: "#2A1A5E", shade: "#8467E0" },
  proof: { from: "#FFF1B8", to: "#FFC83D", blob: "#FFE07A", ink: "#3A2A00", shade: "#D99B00" },
  end: { from: "#E4F6C8", to: "#7CC242", blob: "#BDE68A", ink: "#1F3A12", shade: "#4F8A22" },
};

export const img = (name: string) => staticFile(`minute/${name}.png`);

/** A gradient with soft clay blobs drifting and a slow ring of sun rays: Peri's world, never still. */
const World = ({ look, rays = false }: { look: Look; rays?: boolean }) => {
  const f = useCurrentFrame();
  const blobs = [
    { x: 12, y: 18, r: 340, s: 0.013, p: 0 },
    { x: 86, y: 22, r: 260, s: 0.017, p: 2 },
    { x: 78, y: 84, r: 420, s: 0.011, p: 4 },
    { x: 20, y: 88, r: 300, s: 0.015, p: 1 },
    { x: 50, y: 50, r: 180, s: 0.02, p: 3 },
  ];
  return (
    <AbsoluteFill style={{ background: `radial-gradient(circle at 35% 30%, ${look.from}, ${look.to} 75%)`, overflow: "hidden" }}>
      {rays && (
        <div
          style={{
            position: "absolute",
            left: "50%",
            top: "50%",
            width: 3000,
            height: 3000,
            marginLeft: -1500,
            marginTop: -1500,
            rotate: `${f * 0.25}deg`,
            background: `repeating-conic-gradient(${look.blob}55 0deg 9deg, transparent 9deg 22.5deg)`,
            maskImage: "radial-gradient(circle, black 10%, transparent 60%)",
          }}
        />
      )}
      {blobs.map((b, i) => (
        <div
          key={i}
          style={{
            position: "absolute",
            left: `${b.x + Math.sin(f * b.s + b.p) * 4}%`,
            top: `${b.y + Math.cos(f * b.s * 1.3 + b.p) * 5}%`,
            width: b.r * 2,
            height: b.r * 2 * (1 + Math.sin(f * 0.03 + i) * 0.06),
            marginLeft: -b.r,
            marginTop: -b.r,
            borderRadius: "46% 54% 58% 42% / 52% 44% 56% 48%",
            rotate: `${f * 0.2 * (i % 2 ? 1 : -1) + i * 40}deg`,
            background: `radial-gradient(circle at 35% 30%, ${look.blob}, ${look.blob}00 70%)`,
            opacity: 0.7,
          }}
        />
      ))}
      {/* Clay grain: tiny dots, so the flat gradient feels like plasticine. */}
      <AbsoluteFill
        style={{
          backgroundImage: `radial-gradient(${look.shade}22 1.2px, transparent 1.4px)`,
          backgroundSize: "9px 9px",
          opacity: 0.6,
        }}
      />
    </AbsoluteFill>
  );
};

/** A scene: its world opens over the last one as a growing clay circle, from where the eye already is. */
export const Scene = ({ at, look, origin, rays, last, children }: { at: readonly [number, number]; look: Look; origin: string; rays?: boolean; last?: boolean; children: ReactNode }) => {
  return (
    <Sequence from={at[0]} durationInFrames={at[1] - at[0] + (last ? 0 : 16)}>
      <Opening origin={origin}>
        <World look={look} rays={rays} />
        {children}
      </Opening>
    </Sequence>
  );
};

const Opening = ({ origin, children }: { origin: string; children: ReactNode }) => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  const r = spring({ frame: f, fps, config: { damping: 18, mass: 0.7 } }) * 160;
  return <AbsoluteFill style={{ clipPath: f > 30 ? undefined : `circle(${r}% at ${origin})` }}>{children}</AbsoluteFill>;
};

/** Peri: drops in, squashes like clay on landing, rebounds, then breathes and sways while she's on screen. */
export const Peri = ({ name, x, y, size, delay = 0, flip = false, shadow = true }: { name: string; x: number; y: number; size: number; delay?: number; flip?: boolean; shadow?: boolean }) => {
  const f = useCurrentFrame() - delay;
  const { fps } = useVideoConfig();
  if (f < 0) return null;
  const drop = spring({ frame: f, fps, config: { damping: 9, stiffness: 140, mass: 0.6 } });
  const land = interpolate(f, [7, 11, 16, 22], [0, 1, -0.5, 0], clamp);
  const squash = 1 - land * 0.16;
  const breathe = Math.sin(f / 13) * 0.015;
  const sway = Math.sin(f / 23) * 2.2;
  return (
    <div style={{ position: "absolute", left: x - size / 2, top: y - size, width: size, height: size }}>
      {shadow && (
        <div
          style={{
            position: "absolute",
            left: size * 0.22,
            right: size * 0.22,
            bottom: size * 0.02,
            height: size * 0.07,
            borderRadius: "50%",
            background: "rgba(0,0,0,0.22)",
            filter: "blur(8px)",
            scale: `${0.6 + drop * 0.4}`,
          }}
        />
      )}
      <Img
        src={img(name)}
        style={{
          width: "100%",
          height: "100%",
          transformOrigin: "50% 96%",
          translate: `0 ${interpolate(drop, [0, 1], [-size * 0.9, 0])}px`,
          scale: `${(flip ? -1 : 1) * (2 - squash + breathe)} ${squash - breathe}`,
          rotate: `${sway}deg`,
          filter: "drop-shadow(0 18px 22px rgba(0,0,0,0.18))",
        }}
      />
    </div>
  );
};

/** A headline that arrives word by word, each one popping like a clay letter. */
export const Words = ({ text, delay = 0, size = 92, color, shade, style, accent }: { text: string; delay?: number; size?: number; color: string; shade: string; style?: CSSProperties; accent?: { word: string; color: string } }) => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <div style={{ position: "absolute", display: "flex", flexWrap: "wrap", columnGap: size * 0.26, rowGap: size * 0.02, ...style }}>
      {text.split(" ").map((w, i) => {
        const p = spring({ frame: f - delay - i * 3, fps, config: { damping: 11, stiffness: 160 } });
        const lit = accent && w.replace(/[.,?!]/g, "") === accent.word;
        return (
          <span
            key={i}
            style={{
              fontFamily: HEADING,
              fontWeight: 700,
              fontSize: size,
              lineHeight: 1.04,
              color: lit ? accent.color : color,
              textShadow: `0 ${size * 0.06}px 0 ${shade}`,
              opacity: Math.min(1, p * 2),
              translate: `0 ${(1 - p) * 40}px`,
              scale: `${0.6 + p * 0.4}`,
              rotate: `${(1 - p) * (i % 2 ? 8 : -8)}deg`,
              display: "inline-block",
            }}
          >
            {w}
          </span>
        );
      })}
    </div>
  );
};

/** A soft rubber pill, like the app's buttons, that bounces in. */
export const Chip = ({ text, delay, x, y, bg, color, icon }: { text: string; delay: number; x: number; y: number; bg: string; color: string; icon?: string }) => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  const p = spring({ frame: f - delay, fps, config: { damping: 10, stiffness: 150 } });
  if (f < delay) return null;
  return (
    <div
      style={{
        position: "absolute",
        left: x,
        top: y,
        padding: "22px 34px",
        borderRadius: 999,
        background: bg,
        color,
        fontFamily: BODY,
        fontWeight: 700,
        fontSize: 40,
        boxShadow: "0 10px 0 rgba(0,0,0,0.16), 0 18px 30px rgba(0,0,0,0.14)",
        scale: `${p}`,
        transformOrigin: "0% 50%",
        display: "flex",
        alignItems: "center",
        gap: 18,
        whiteSpace: "nowrap",
      }}
    >
      {icon && (
        <span style={{ fontFamily: HEADING, width: 52, height: 52, borderRadius: 26, background: color, color: bg, display: "grid", placeItems: "center", fontSize: 32 }}>
          {icon}
        </span>
      )}
      {text}
    </div>
  );
};

/** The app's real recording in a phone that swings in and floats. */
/** Stretches of a recording, in seconds, played back to back (to skip the blank frames a screen change leaves). */
type Cut = [number, number];

export const Phone = ({ clip, cuts, rate, x, y, h, delay = 0, tilt = -4, shake = 0 }: { clip: string; cuts: Cut[]; rate: number; x: number; y: number; h: number; delay?: number; tilt?: number; shake?: number }) => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  const p = spring({ frame: f - delay, fps, config: { damping: 13, stiffness: 90 } });
  const w = h * 0.45;
  const jitter = shake ? Math.sin(f * 2.1) * shake * interpolate(f % 45, [0, 6, 14], [1, 1, 0], clamp) : 0;
  return (
    <div
      style={{
        position: "absolute",
        left: x - w / 2,
        top: y - h / 2,
        width: w,
        height: h,
        borderRadius: 64,
        background: "#16181A",
        padding: 14,
        boxShadow: "0 40px 70px rgba(0,0,0,0.35), inset 0 0 0 3px #3A3F44",
        translate: `${jitter}px ${interpolate(p, [0, 1], [900, 0]) + Math.sin(f / 20) * 8}px`,
        rotate: `${interpolate(p, [0, 1], [tilt * 4, tilt]) + Math.sin(f / 31) * 0.8}deg`,
      }}
    >
      <div style={{ width: "100%", height: "100%", borderRadius: 52, overflow: "hidden", background: "#000" }}>
        {cuts.map(([a, b], i) => {
          const start = cuts.slice(0, i).reduce((t, [c, d]) => t + Math.round(((d - c) * fps) / rate), 0);
          const last = i === cuts.length - 1;
          return (
            <Sequence key={i} from={start} durationInFrames={last ? undefined : Math.round(((b - a) * fps) / rate)} layout="none">
              <OffthreadVideo src={staticFile(`clips/${clip}.mp4`)} startFrom={Math.round(a * fps)} playbackRate={rate} muted style={{ width: "100%", height: "100%", objectFit: "cover" }} />
            </Sequence>
          );
        })}
      </div>
    </div>
  );
};

/** Chubby clay confetti and stars bursting out of a point and falling. */
export const Burst = ({ x, y, delay, count = 26, colors }: { x: number; y: number; delay: number; count?: number; colors: string[] }) => {
  const f = useCurrentFrame() - delay;
  if (f < 0 || f > 90) return null;
  return (
    <>
      {Array.from({ length: count }, (_, i) => {
        const a = (i / count) * Math.PI * 2 + (i % 3) * 0.3;
        const v = 14 + (i * 7) % 11;
        const t = f;
        const px = x + Math.cos(a) * v * t * 0.9;
        const py = y + Math.sin(a) * v * t * 0.9 + 0.35 * t * t;
        const s = 18 + (i * 5) % 16;
        const star = i % 4 === 0;
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              left: px,
              top: py,
              width: s,
              height: s,
              background: colors[i % colors.length],
              borderRadius: star ? 0 : "50%",
              clipPath: star ? "polygon(50% 0,62% 36%,100% 38%,70% 60%,80% 100%,50% 76%,20% 100%,30% 60%,0 38%,38% 36%)" : undefined,
              rotate: `${t * 9 + i * 30}deg`,
              opacity: interpolate(f, [60, 90], [1, 0], clamp),
              boxShadow: star ? undefined : "inset -3px -4px 0 rgba(0,0,0,0.18)",
            }}
          />
        );
      })}
    </>
  );
};

export const CONFETTI = ["#FFC83D", "#FF8A7A", "#6EC6FF", "#A98BFF", "#7CC242", "#FFF8EC"];

/** 0-6 s: a scam's hook swings down with its message. */
const Hook = () => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  const L = LOOK.hook;
  const drop = spring({ frame: f - 4, fps, config: { damping: 12, stiffness: 70 } });
  const swing = Math.sin(f / 9) * 9 * Math.exp(-f / 70) + Math.sin(f / 25) * 2;
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
            opacity: interpolate(f, [30, 40], [0, 1], clamp),
          }}
        >
          Mum, it's me. New phone. Can you send money?
        </div>
      </div>
      <Words text="Every day, a message like this" delay={18} size={96} color={L.ink} shade={L.shade} style={{ left: 120, top: 330, width: 1000 }} />
      <Words text="tries to trick someone we love." delay={70} size={96} color={L.ink} shade={L.shade} accent={{ word: "trick", color: "#FF8A7A" }} style={{ left: 120, top: 560, width: 1000 }} />
    </>
  );
};

/** 6-12 s: Peri arrives. */
const Meet = () => {
  const L = LOOK.meet;
  return (
    <>
      <Peri name="wave" x={600} y={1010} size={860} delay={8} />
      <Burst x={600} y={420} delay={22} colors={CONFETTI} />
      <Words text="Meet Peri." delay={20} size={150} color={L.ink} shade={L.shade} style={{ left: 1060, top: 300, width: 800 }} />
      <Words text="A clay pear who lives in the phone and looks after you." delay={42} size={58} color={L.ink} shade="transparent" style={{ left: 1066, top: 520, width: 760 }} />
    </>
  );
};

/** 12-24 s: a new number that claims to be family. */
const NewNumber = () => {
  const L = LOOK.number;
  return (
    <>
      <Phone clip="m1_newnumber" cuts={[[4, 19.5], [23.3, 32]]} rate={2.1} x={480} y={560} h={940} delay={6} tilt={-5} />
      <Words text="A new number says it's family?" delay={14} size={80} color={L.ink} shade={L.shade} style={{ left: 860, top: 110, width: 980 }} />
      <Chip text="Peri spots the trap" icon="1" delay={70} x={880} y={340} bg="#FFF8EC" color="#123049" />
      <Chip text="Finds the real Ana" icon="2" delay={150} x={880} y={455} bg="#FFF8EC" color="#123049" />
      <Chip text={'One spoken "yes" calls her'} icon="3" delay={230} x={880} y={570} bg="#25541A" color="#FFF8EC" />
      <Peri name="detective" x={1640} y={1090} size={470} delay={30} />
      <Burst x={1640} y={700} delay={300} count={20} colors={CONFETTI} />
    </>
  );
};

/** 24-36 s: a code during a call. No jokes: a clear order. */
const Code = () => {
  const f = useCurrentFrame();
  const L = LOOK.code;
  const pulse = 1 + Math.max(0, Math.sin(f / 4.5)) * 0.04 * interpolate(f, [150, 170], [0, 1], clamp);
  return (
    <>
      <Peri name="protective" x={380} y={1060} size={700} delay={10} />
      <Words text="A code during a call?" delay={12} size={84} color={L.ink} shade={L.shade} style={{ left: 760, top: 120, width: 700 }} />
      <div style={{ position: "absolute", left: 760, top: 350, width: 600, scale: `${pulse}`, transformOrigin: "0% 50%" }}>
        <Words text="Hang up. It's always a scam." delay={150} size={92} color="#FFE07A" shade={L.shade} style={{ position: "relative" }} />
      </div>
      <Chip text="A hard rule no AI can overrule" delay={220} x={760} y={720} bg="#FFF8EC" color="#8E1F18" />
      <Chip text="The code is never read aloud" delay={260} x={760} y={840} bg="#FFF8EC" color="#8E1F18" />
      <Phone clip="m3_code" cuts={[[3, 30]]} rate={2.3} x={1620} y={560} h={940} delay={4} tilt={4} shake={6} />
    </>
  );
};

/** 36-46 s: something risky waits for the family's yes. */
const Family = () => {
  const L = LOOK.family;
  return (
    <>
      <Peri name="heartfamily" x={360} y={1040} size={640} delay={8} />
      <Words text="Something risky? Peri asks the family." delay={10} size={76} color={L.ink} shade={L.shade} style={{ left: 700, top: 110, width: 760 }} />
      <Chip text="No" delay={90} x={720} y={420} bg="#25541A" color="#FFF8EC" />
      <Chip text="Call me first" delay={110} x={870} y={420} bg="#FFF8EC" color="#2A1A5E" />
      <Chip text="Yes" delay={130} x={1230} y={420} bg="#FFF8EC" color="#2A1A5E" />
      <Words text="They never read your messages. Never." delay={170} size={50} color={L.ink} shade="transparent" style={{ left: 722, top: 580, width: 700 }} />
      <Phone clip="m6_family_ana" cuts={[[44, 72]]} rate={1.2} x={1640} y={560} h={940} delay={4} tilt={5} />
    </>
  );
};

/** A big number on a clay card. */
export const Stat = ({ big, small, delay, x, y, rot }: { big: string; small: string; delay: number; x: number; y: number; rot: number }) => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  const p = spring({ frame: f - delay, fps, config: { damping: 9, stiffness: 130 } });
  if (f < delay) return null;
  return (
    <div
      style={{
        position: "absolute",
        left: x,
        top: y,
        width: 470,
        padding: "28px 34px",
        borderRadius: 44,
        background: "#FFF8EC",
        boxShadow: "0 12px 0 #D99B00, 0 26px 40px rgba(0,0,0,0.16)",
        scale: `${p}`,
        rotate: `${rot + Math.sin((f - delay) / 18) * 1.2}deg`,
      }}
    >
      <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 92, lineHeight: 1, color: "#25541A" }}>{big}</div>
      <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 32, color: "#3A2A00", marginTop: 8 }}>{small}</div>
    </div>
  );
};

/** 46-52 s: the proof, fast. */
const Proof = () => (
  <>
    <Peri name="shieldup" x={960} y={1060} size={680} delay={6} />
    <Stat big="91%" small="of 200 unseen scams flagged" delay={16} x={120} y={150} rot={-4} />
    <Stat big="4%" small="false alarms" delay={40} x={1330} y={150} rot={4} />
    <Stat big="6" small="Meta models, one job each" delay={64} x={140} y={620} rot={3} />
    <Stat big="EN · ES" small="English and Spanish" delay={88} x={1310} y={620} rot={-3} />
  </>
);

/** 52-60 s: the end card. */
const End = () => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  const L = LOOK.end;
  const logo = spring({ frame: f - 30, fps, config: { damping: 10, stiffness: 120 } });
  return (
    <>
      <Peri name="hero" x={520} y={1040} size={820} delay={4} />
      <Burst x={520} y={360} delay={14} count={34} colors={CONFETTI} />
      <Burst x={1380} y={300} delay={60} count={24} colors={CONFETTI} />
      <div style={{ position: "absolute", left: 1010, top: 170, display: "flex", alignItems: "center", gap: 26, scale: `${logo}`, transformOrigin: "0% 50%" }}>
        <Img src={img("icon")} style={{ width: 150, height: 150, borderRadius: "50%", boxShadow: "0 8px 0 rgba(0,0,0,0.12)" }} />
        <span style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 112, color: L.ink, textShadow: `0 7px 0 ${L.shade}` }}>FriendlyPear</span>
      </div>
      <Words text="You relax. I'll keep watch." delay={48} size={78} color={L.ink} shade="transparent" style={{ left: 1020, top: 400, width: 800 }} />
      <Words text="Meta Global AI Developer Hackathon" delay={100} size={36} color={L.ink} shade="transparent" style={{ left: 1024, top: 640, width: 800 }} />
    </>
  );
};

export const Minute = () => (
  <AbsoluteFill style={{ background: "#2B1436" }}>
    <Audio src={staticFile("minute/music.wav")} />
    <Scene at={S.hook} look={LOOK.hook} origin="50% 50%">
      <Hook />
    </Scene>
    <Scene at={S.meet} look={LOOK.meet} origin="70% 40%" rays>
      <Meet />
    </Scene>
    <Scene at={S.number} look={LOOK.number} origin="30% 50%">
      <NewNumber />
    </Scene>
    <Scene at={S.code} look={LOOK.code} origin="85% 50%">
      <Code />
    </Scene>
    <Scene at={S.family} look={LOOK.family} origin="20% 70%">
      <Family />
    </Scene>
    <Scene at={S.proof} look={LOOK.proof} origin="50% 70%" rays>
      <Proof />
    </Scene>
    <Scene at={S.end} look={LOOK.end} origin="30% 60%" rays last>
      <End />
    </Scene>
  </AbsoluteFill>
);
