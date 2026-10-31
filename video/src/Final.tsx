import type { ReactNode } from "react";
import { AbsoluteFill, Audio, Easing, Img, interpolate, Sequence, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import durations from "../public/final/durations.json";
import lines from "../public/final/lines.json";
import scenes from "../public/final/scenes.json";
import visemes from "../public/final/visemes.json";
import { BODY, clamp, HEADING } from "./theme";
import { Actor, Cast, CHEER, Coins, CREAM, DROP, Drawn, f, Flash, Floaties, FPS, H, INK, musicVolume, OUTFITS, PeriMoods, pic, Pop, script, ScriptContext, Slam, Snd, Sparks, SPLIT, SubtitleBand, timing } from "./cast";
import type { Line } from "./cast";
import { Burst, CONFETTI, img } from "./Minute";
import { AppPhone, BUBBLE, BUTTONS, CALLER, NOTE, NOTE_BEFORE, QR, SCAM_BOX, shown, Zoom } from "./Submission";
import type { Take } from "./Submission";

/**
 * The 3-minute submission video, "the heist" (16:9, 30 fps, exactly 3:00). A clumsy crook has four plans to scam Rosa;
 * each round is the real app beating one of them, and the fourth shows the family key: anything risky waits for the
 * family's yes. The people are animated like Peri (talking frames that follow their voices, blinks, breathing); no
 * words are ever put over them: subtitles live in their own band, labels and close-ups on the app's side. Pictures by
 * Muse Image (scripts/heist_frames.ts and earlier), cut out with Adobe Photoshop; voices, lip sync and timing from
 * scripts/final_voice.py, music from scripts/final_music.py; scene times in public/final/scenes.json.
 */
export const FINAL_FRAMES = 180 * FPS;
type Name = keyof typeof scenes;
const AT = scenes as Record<Name, [number, number]>;
const FINAL = script(AT, lines as Record<Name, Line[]>, durations as Record<Name, number[]>, visemes as Record<string, string[]>);
const SPOKEN = FINAL.spoken;
const { say, ends } = timing(FINAL);

const TAKES = {
  // Android draws a blank white screen for two seconds while the call opens (20.4-22.9 s): cut from "I'm all ears" to "Calling...".
  r1: { clip: "m1_newnumber", cuts: [[0.5, 10.6], [19.6, 20.35], [22.9, 29.4], [30.9, 32.5]], rate: 1 },
  r2: { clip: "m3_code", cuts: [[1.2, 9.6], [10.7, 19.5]], rate: 1 },
  r3: { clip: "m4_picture", cuts: [[2.4, 33]], rate: 1 },
  r4: { clip: "m6_family_ana", cuts: [[45.6, 72]], rate: 1 },
  heart: { clip: "m5_fallen", cuts: [[3.4, 6.4], [12.6, 14.35], [16.8, 18.9]], rate: 0.99 },
} satisfies Record<string, Take>;
/** On Ana's approval page: "This needs two guardians' yes: yours and one more. A single no stops it." */
const TWO_YES: [number, number, number, number] = [0.02, 0.19, 0.98, 0.33];

// ---------------------------------------------------------------- building blocks

/** A round's title card. */
const Banner = ({ n, title, color }: { n: number; title: string; color: string }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  if (fr > 46) return null;
  const p = spring({ frame: fr, fps, config: { damping: 14, stiffness: 160 } });
  const out = interpolate(fr, [34, 46], [0, 1], clamp);
  return (
    <div style={{ position: "absolute", left: 0, top: 0, width: 1920, height: H, background: color, translate: `${out * -1920}px 0`, display: "grid", placeItems: "center" }}>
      <div style={{ textAlign: "center", scale: `${interpolate(p, [0, 1], [1.8, 1])}` }}>
        <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 70, letterSpacing: 18, color: INK, opacity: 0.75 }}>ROUND {n}</div>
        <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 170, lineHeight: 1, color: INK, textShadow: "0 10px 0 rgba(0,0,0,0.18)" }}>{title}</div>
      </div>
    </div>
  );
};

/** The app's side of a round: the round's colour behind Rosa's (or Ana's) real phone. */
const AppSide = ({ colors }: { colors: [string, string] }) => (
  <div style={{ position: "absolute", left: SPLIT, top: 0, width: 1920 - SPLIT, height: H, background: `radial-gradient(circle at 70% 40%, ${colors[0]}, ${colors[1]} 85%)`, overflow: "hidden" }}>
    <AbsoluteFill style={{ backgroundImage: "radial-gradient(rgba(255,255,255,0.18) 1.4px, transparent 1.6px)", backgroundSize: "12px 12px" }} />
  </div>
);
const PhoneLabel = ({ text }: { text: string }) => (
  <div style={{ position: "absolute", right: 26, top: 14, whiteSpace: "nowrap", padding: "6px 18px", borderRadius: 999, background: "rgba(31,42,26,0.85)", color: CREAM, fontFamily: BODY, fontWeight: 700, fontSize: 22, letterSpacing: 3 }}>{text} · REAL APP</div>
);
const PHONE = { x: 1640, y: 492, h: 860 };

/** A round's point for Peri, on the app's side while the crook reacts on his: she jumps for joy, as in the app. */
const Point = ({ n, at }: { n: number; at: number }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  if (fr < at) return null;
  const p = spring({ frame: fr - at, fps, config: { damping: 14 } });
  const point = spring({ frame: fr - at - 14, fps, config: { damping: 7, stiffness: 220 } });
  return (
    <>
      <div style={{ position: "absolute", left: SPLIT, top: 0, width: 1920 - SPLIT, height: H, background: "radial-gradient(circle at 50% 40%, #2F6B22, #10260B 85%)", clipPath: `circle(${p * 150}% at 0% 50%)` }}>
        {fr >= at + 14 && (
          <div style={{ position: "absolute", left: 40, top: 0, width: 640, height: H, display: "grid", placeItems: "center" }}>
            <div style={{ textAlign: "center", scale: `${interpolate(point, [0, 1], [2.4, 1])}`, opacity: Math.min(1, point * 2) }}>
              <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 64, color: "#BDE68A", letterSpacing: 10 }}>POINT</div>
              <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 240, lineHeight: 0.9, color: CREAM, textShadow: "0 12px 0 #0B2007" }}>PERI</div>
              <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 90, color: "#FFC83D" }}>0 : {n}</div>
            </div>
          </div>
        )}
      </div>
      <Drawn keys={CHEER} at={at + 12} x={SPLIT + 900} y={900} size={520} />
      <Sparks at={at + 12 + f(0.76)} x={SPLIT + 900} y={640} kinds={["fx_star", "fx_star_green", "fx_confetti_dot", "fx_confetti_strip"]} count={12} />
    </>
  );
};

/** The score, at the top of the app's side during the rounds. */
const Scoreboard = () => {
  const fr = useCurrentFrame();
  const t = fr / FPS;
  const keyOn = t >= AT.r4[0] + (say("r4", 3) - 8) / FPS && t < AT.r4[0] + (say("r4", 7) - 10) / FPS;
  if (t < AT.r1[0] + 1.4 || t >= AT.tech[0] || keyOn) return null;
  const won = (["r1", "r2", "r3", "r4"] as Name[]).filter((r) => t >= SPOKEN.filter((s) => s.scene === r).pop()!.end + 1.2).length;
  return (
    <div style={{ position: "absolute", left: SPLIT + 26, top: 14, display: "flex", alignItems: "center", gap: 14, padding: "6px 22px", borderRadius: 999, background: "rgba(20,12,24,0.88)" }}>
      <span style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 30, color: "#C9B7E8" }}>SCAMMER 0</span>
      <span style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 26, color: "#FFC83D" }}>VS</span>
      <span style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 30, color: "#BDE68A" }}>{won} PERI</span>
    </div>
  );
};

// ---------------------------------------------------------------- the scenes

const PLANS = [
  { letter: "A", title: "The fake daughter", color: "#6EC6FF" },
  { letter: "B", title: "The fake bank", color: "#FF8A7A" },
  { letter: "C", title: "A lying sticker", color: "#FFC83D" },
  { letter: "D", title: "Keep her talking", color: "#A98BFF" },
];
const PLAN_POS: [number, number][] = [
  [SPLIT + 120, 230],
  [SPLIT + 640, 250],
  [SPLIT + 130, 590],
  [SPLIT + 640, 610],
];

/** 0:00 The crook pins his four plans on a cork board. */
const Cold = () => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const header = spring({ frame: fr - say("cold", 1), fps, config: { damping: 10, stiffness: 180 } });
  return (
    <>
      <Actor pose="crook_plan" who="crook" scene="cold" />
      <div style={{ position: "absolute", left: SPLIT, top: 0, width: 1920 - SPLIT, height: H, background: "radial-gradient(circle at 40% 30%, #C8925A, #8E5B2E 85%)", boxShadow: "inset 14px 0 30px rgba(0,0,0,0.35)" }}>
        <AbsoluteFill style={{ backgroundImage: "radial-gradient(rgba(60,30,10,0.35) 2px, transparent 2.4px)", backgroundSize: "14px 14px" }} />
      </div>
      {fr >= say("cold", 1) && (
        <div style={{ position: "absolute", left: SPLIT + 70, top: 50, padding: "14px 34px", background: "#FFFDF5", boxShadow: "0 12px 24px rgba(0,0,0,0.35)", rotate: "-1.5deg", scale: `${interpolate(header, [0, 1], [1.8, 1])}`, opacity: Math.min(1, header * 2.5), fontFamily: HEADING, fontWeight: 700, fontSize: 52, color: INK }}>
          4 plans · 1 grandma, all alone
        </div>
      )}
      <svg style={{ position: "absolute", left: 0, top: 0 }} width={1920} height={H}>
        {[[0, 1], [1, 3], [3, 2], [2, 0]].map(([a, b], i) => {
          const t0 = say("cold", 2 + Math.max(a, b));
          const k = interpolate(fr, [t0, t0 + 10], [0, 1], clamp);
          const [ax, ay] = PLAN_POS[a];
          const [bx, by] = PLAN_POS[b];
          return <line key={i} x1={ax + 200} y1={ay + 30} x2={ax + 200 + (bx - ax) * k} y2={ay + 30 + (by - ay) * k} stroke="#C0392B" strokeWidth={6} strokeLinecap="round" />;
        })}
      </svg>
      {PLANS.map((p, i) => {
        const at = say("cold", 2 + i);
        if (fr < at) return null;
        const s = spring({ frame: fr - at, fps, config: { damping: 9, stiffness: 210 } });
        const [x, y] = PLAN_POS[i];
        return (
          <div key={p.letter} style={{ position: "absolute", left: x, top: y, width: 400, padding: "26px 28px", background: "#FFFDF5", boxShadow: "0 14px 28px rgba(0,0,0,0.35)", rotate: `${[-5, 4, 3, -4][i]}deg`, scale: `${interpolate(s, [0, 1], [1.8, 1])}`, opacity: Math.min(1, s * 2.5) }}>
            <div style={{ position: "absolute", left: "50%", top: -14, width: 30, height: 30, marginLeft: -15, borderRadius: "50%", background: "#C0392B", boxShadow: "inset -4px -5px 0 rgba(0,0,0,0.25)" }} />
            <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 44, color: p.color, textShadow: "0 3px 0 rgba(0,0,0,0.15)" }}>PLAN {p.letter}</div>
            <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 50, lineHeight: 1.05, color: INK }}>{p.title}</div>
          </div>
        );
      })}
      {PLANS.map((_, i) => (
        <Snd key={i} src="reel/sfx/stamp" at={say("cold", 2 + i)} volume={0.55} />
      ))}
      <Snd src="reel/sfx/scratch" at={f(11.4)} volume={0.6} />
    </>
  );
};

/**
 * 0:12 Peri arrives as she does when the app opens (she drops from the sky, splats, bounces and opens her arms), then
 * talks: calm, protective with her umbrella for "I stop scams", her blue phone for the family's yes.
 */
const Title = () => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const logo = spring({ frame: fr - 4, fps, config: { damping: 10, stiffness: 150 } });
  const landed = ends("title", 0) + 2;
  return (
    <AbsoluteFill style={{ height: H, background: "radial-gradient(circle at 30% 50%, #FFF6C8, #FFC83D 45%, #7CC242 100%)", overflow: "hidden" }}>
      <div style={{ position: "absolute", left: "26%", top: "52%", width: 3200, height: 3200, marginLeft: -1600, marginTop: -1600, rotate: `${fr * 0.3}deg`, background: "repeating-conic-gradient(rgba(255,255,255,0.35) 0deg 8deg, transparent 8deg 20deg)" }} />
      <Burst x={500} y={330} delay={17} count={36} colors={CONFETTI} />
      <Drawn keys={DROP} at={2} until={landed} x={500} y={960} size={800} />
      {fr >= landed && (
        <PeriMoods scene="title" parts={[{ from: landed, pose: "peri_calm" }, { from: say("title", 2) - 3, pose: "peri_protective" }, { from: say("title", 2, "anything risky") - 3, pose: "peri_calling" }]} x={100} y={160} w={800} h={800} enter={-30} />
      )}
      <Sparks at={17} x={500} y={860} kinds={["fx_star", "fx_star_green"]} count={8} spread={320} />
      <div style={{ position: "absolute", left: 1000, top: 100, display: "flex", alignItems: "center", gap: 22, scale: `${interpolate(logo, [0, 1], [2.2, 1])}`, opacity: Math.min(1, logo * 2), transformOrigin: "0% 50%" }}>
        <Img src={img("icon")} style={{ width: 130, height: 130, borderRadius: "50%", boxShadow: "0 8px 0 rgba(0,0,0,0.15)" }} />
        <span style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 118, color: INK, textShadow: "0 8px 0 rgba(255,255,255,0.6)" }}>FriendlyPear</span>
      </div>
      <Slam text={"Rosa isn't\nalone."} at={say("title", 1)} x={1010} y={300} size={120} color={INK} shade="rgba(255,255,255,0.6)" rotate={-3} />
      <Slam text="Stops scams before money moves" at={say("title", 2)} x={990} y={620} size={40} color={CREAM} bg="#25541A" rotate={-2} />
      <Slam text="Anything risky needs the family's yes" at={say("title", 2, "anything risky")} x={1000} y={710} size={40} color={INK} bg={CREAM} rotate={1} />
      <Snd src="submission/sfx/sparkle" at={8} volume={0.5} />
    </AbsoluteFill>
  );
};

/** 0:20 What scams like the crook's cost. */
const Stakes = () => {
  const fr = useCurrentFrame();
  const n = interpolate(fr, [6, 50], [0, 7.7], { ...clamp, easing: Easing.out(Easing.cubic) });
  const shake = fr > 50 && fr < 58 ? Math.sin(fr * 3) * (58 - fr) * 2 : 0;
  return (
    <AbsoluteFill style={{ height: H, background: "radial-gradient(circle at 50% 45%, #6B4A8E, #2E1B47 85%)", display: "grid", placeItems: "center" }}>
      <Coins />
      <div style={{ textAlign: "center", translate: `${shake}px 0` }}>
        <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 250, lineHeight: 1, color: "#FFC83D", textShadow: "0 14px 0 #5C3A00" }}>${n.toFixed(1)}B</div>
        <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 58, color: CREAM, marginTop: 18 }}>lost to fraud by Americans over 60, in 2025 alone</div>
        <div style={{ fontFamily: BODY, fontSize: 26, color: "rgba(255,248,236,0.65)", marginTop: 14 }}>Source: FBI Internet Crime Complaint Center (IC3), 2025</div>
      </div>
      <Slam text="GAME ON." at={f(5.0)} x={960} y={70} size={90} color="#BDE68A" rotate={-3} center />
      <Snd src="submission/sfx/ticks" at={6} volume={0.35} />
      <Snd src="final/sfx/slam" at={50} volume={0.7} />
    </AbsoluteFill>
  );
};

/** 0:26 Round 1: the fake daughter. */
const Round1 = () => {
  const T = TAKES.r1;
  const result = say("r1", 4) - 6;
  return (
    <>
      <Cast scene="r1" parts={[{ from: 0, pose: "crook_desk", who: "crook" }, { from: say("r1", 1) - 10, pose: "rosa_reading", who: "rosa" }, { from: say("r1", 2) - 8, pose: "rosa_yes", who: "rosa" }, { from: say("r1", 3) - 12, pose: "ana_call_rest", who: "ana" }, { from: result, pose: "crook_shock", who: "crook" }]} />
      <AppSide colors={["#DDF2FF", "#6EC6FF"]} />
      <Sequence from={0} durationInFrames={result} layout="none">
        <AppPhone take={T} x={PHONE.x} y={PHONE.y} h={PHONE.h} tilt={3} marks={[{ region: BUBBLE, from: shown(T, 4.9), to: shown(T, 10.6) }, { region: CALLER, from: shown(T, 23.6), to: shown(T, 28.4), color: "#6EC6FF" }]} />
        <Zoom take={T} region={BUBBLE} x={800} y={420} w={600} from={shown(T, 4.9)} to={shown(T, 10.6) - 2} label="Peri, at once" />
        <Zoom take={T} region={CALLER} x={820} y={460} w={560} from={shown(T, 23.6)} to={shown(T, 28.4)} label="Calling the real Ana" color="#6EC6FF" />
        <PhoneLabel text="ROSA'S PHONE" />
      </Sequence>
      <Slam text={"ONE “YES”\nCALLS THE REAL ANA"} at={ends("r1", 2)} until={result - 2} x={800} y={90} size={54} color={INK} bg={CREAM} rotate={-3} />
      <Point n={1} at={result} />
      <Banner n={1} title="The fake daughter" color="#6EC6FF" />
      <Flash />
      <Snd src="final/sfx/impact" at={0} volume={0.55} />
      <Snd src="short/sfx/typing" at={say("r1", 0) + 10} volume={0.45} />
      <Snd src="short/sfx/send" at={shown(T, 4.1)} volume={0.45} />
      <Snd src="short/sfx/ding" at={shown(T, 4.3)} volume={0.5} />
      <Snd src="submission/sfx/listen" at={shown(T, 19.7)} volume={0.45} />
      <Snd src="submission/sfx/ringback" at={shown(T, 23.3)} volume={0.3} length={shown(T, 25.0) - shown(T, 23.3)} />
      <Snd src="submission/sfx/answer" at={shown(T, 25.0)} volume={0.45} />
      <Snd src="short/sfx/score" at={ends("r1", 4) + f(1.2)} volume={0.55} />
    </>
  );
};

/** 0:48 Round 2: the fake bank, and a code during the call. */
const Round2 = () => {
  const T = TAKES.r2;
  const fire = shown(T, 13.0);
  const result = say("r2", 4) - 6;
  return (
    <>
      <Cast scene="r2" parts={[{ from: 0, pose: "crook_phone", who: "crook" }, { from: say("r2", 1) - 8, pose: "rosa_kitchen", who: "rosa" }, { from: say("r2", 2) - 6, pose: "crook_phone", who: "crook" }, { from: say("r2", 3) - 6, pose: "rosa_kitchen", who: "rosa" }, { from: result, pose: "crook_angry", who: "crook" }]} />
      <AppSide colors={["#FFD9D2", "#E5533D"]} />
      <Sequence from={0} durationInFrames={result} layout="none">
        <AppPhone take={T} x={PHONE.x} y={PHONE.y} h={PHONE.h} tilt={-3} marks={[{ region: BUBBLE, from: shown(T, 4.6), to: shown(T, 9.6) }, { region: BUBBLE, from: shown(T, 13.2), to: result, color: "#FFE07A" }]} />
        <Zoom take={T} region={BUBBLE} x={800} y={420} w={600} from={shown(T, 4.6)} to={shown(T, 9.6) - 2} label="Before Rosa answers" />
        <Zoom take={T} region={BUBBLE} x={800} y={420} w={600} from={shown(T, 13.2)} to={result} label="A code arrived during the call" color="#FFE07A" />
        <PhoneLabel text="ROSA'S PHONE" />
      </Sequence>
      <Slam text="HANG UP." at={fire} until={result - 2} x={800} y={80} size={140} color={CREAM} shade="#8E1F18" rotate={-5} />
      <Slam text={"HARD RULE:\nNO AI CAN OVERRULE IT"} at={ends("r2", 3)} until={result - 2} x={810} y={260} size={40} color="#8E1F18" bg={CREAM} rotate={2} />
      <Point n={2} at={result} />
      <Banner n={2} title="The fake bank" color="#FF8A7A" />
      <Flash />
      <Snd src="final/sfx/impact" at={0} volume={0.55} />
      <Snd src="submission/sfx/ring" at={shown(T, 3.8)} volume={0.4} length={shown(T, 7.8) - shown(T, 3.8)} />
      <Snd src="submission/sfx/answer" at={shown(T, 7.8)} volume={0.45} />
      <Snd src="short/sfx/ding" at={shown(T, 12.8)} volume={0.5} />
      <Snd src="submission/sfx/alert" at={fire} volume={0.7} />
      <Snd src="short/sfx/boing" at={say("r2", 4, "PEAR")} volume={0.6} />
      <Snd src="short/sfx/score" at={ends("r2", 4) + f(1.2)} volume={0.55} />
    </>
  );
};

/** 1:08 Round 3: a QR sticker with a note telling the AI it's safe. */
const Round3 = () => {
  const T = TAKES.r3;
  const verdict = shown(T, 9.05);
  const result = say("r3", 2) - 8;
  return (
    <>
      <Cast scene="r3" parts={[{ from: 0, pose: "crook_stickers", who: "crook" }, { from: say("r3", 1) - 8, pose: "rosa_qr", who: "rosa" }, { from: result, pose: "crook_shock", who: "crook" }]} />
      <AppSide colors={["#FFF4C2", "#FFC83D"]} />
      <Sequence from={0} durationInFrames={result} layout="none">
        <AppPhone take={T} x={PHONE.x} y={PHONE.y} h={PHONE.h} tilt={3} marks={[{ region: NOTE_BEFORE, from: shown(T, 4.2), to: verdict }, { region: QR, from: verdict, to: result, color: "#FF8A7A" }]} />
        <Zoom take={T} region={NOTE_BEFORE} x={790} y={430} w={620} from={shown(T, 4.2)} to={verdict - 9} label="Hidden in the sticker, for the AI" />
        <Zoom take={T} region={QR} x={800} y={420} w={340} from={verdict + 6} to={result} label="SAM 3.1 circles it" color="#FF8A7A" />
        <Zoom take={T} region={NOTE} x={800} y={800} w={560} from={verdict + 6} to={result} label="...and Peri ignores it" />
        <PhoneLabel text="ROSA'S PHONE" />
      </Sequence>
      <Slam text="NOT FOOLED." at={say("r3", 1, "A note")} until={result - 2} x={800} y={90} size={104} color="#C0392B" shade="rgba(255,255,255,0.7)" rotate={-6} />
      <Slam text={"Muse Spark reads the picture.\nThe phone has the last word."} at={ends("r3", 1)} until={result - 2} x={1010} y={250} size={30} color={INK} bg={CREAM} rotate={2} />
      <Point n={3} at={result} />
      <Banner n={3} title="The lying sticker" color="#FFC83D" />
      <Flash />
      <Snd src="final/sfx/impact" at={0} volume={0.55} />
      <Snd src="submission/sfx/shutter" at={shown(T, 3.7)} volume={0.5} />
      <Snd src="submission/sfx/alert" at={verdict} volume={0.5} />
      <Snd src="reel/sfx/stamp" at={say("r3", 1, "A note")} volume={0.6} />
      <Snd src="short/sfx/score" at={ends("r3", 2) + f(1.2)} volume={0.55} />
    </>
  );
};

/** The family key: Rosa's pear asks the family's pears; two yeses, each behind their own phone's lock; one no stops it. */
const STEPS = [
  { text: "Peri to Peri: family phones only", line: 4, word: undefined as string | undefined, color: "#7CC242" },
  { text: "They see: possible scam in progress", line: 4, word: "Ana and Pablo", color: "#FFC83D" },
  { text: "A code needs two guardians' yes", line: 5, word: undefined, color: "#6EC6FF" },
  { text: "Each yes: their own phone's lock", line: 5, word: "each confirmed", color: "#A98BFF" },
  { text: "A single no stops it", line: 5, word: "A single no", color: "#FF8A7A" },
  { text: "They never see her messages", line: 6, word: undefined, color: "#BDE68A" },
];

const FamilyKey = ({ from }: { from: number }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const k = fr - from;
  const asked = say("r4", 4) - from;
  const no = say("r4", 5, "A single no") - from;
  const beam = interpolate(k, [asked, asked + 18], [0, 1], clamp);
  const card = (name: string, label: string, x: number, y: number, size: number, at: number, extra?: ReactNode) => {
    const s = spring({ frame: k - at, fps, config: { damping: 10, stiffness: 170 } });
    if (k < at) return null;
    return (
      <div style={{ position: "absolute", left: x - size / 2, top: y, width: size, textAlign: "center", scale: `${s}` }}>
        <div style={{ width: size, height: size, borderRadius: 40, background: CREAM, boxShadow: "0 12px 0 rgba(0,0,0,0.15)", display: "grid", placeItems: "center", position: "relative" }}>
          <Img src={pic(`heist/${name}_cut`)} style={{ width: size * 0.9, height: size * 0.9, objectFit: "contain", translate: `0 ${Math.sin((k + x) / 12) * 4}px` }} />
          {extra}
        </div>
        <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 34, color: INK, marginTop: 10 }}>{label}</div>
      </div>
    );
  };
  const lock = (at: number) => (k >= at ? <div style={{ position: "absolute", right: -16, bottom: -16, width: 64, height: 64, borderRadius: 32, background: "#25541A", color: CREAM, display: "grid", placeItems: "center", fontFamily: HEADING, fontWeight: 700, fontSize: 32, scale: `${spring({ frame: k - at, fps, config: { damping: 8, stiffness: 220 } })}` }}>✓</div> : null);
  const nope = k >= no ? <div style={{ position: "absolute", left: -18, top: -18, padding: "4px 14px", borderRadius: 14, border: "6px solid #C0392B", background: CREAM, color: "#C0392B", fontFamily: HEADING, fontWeight: 700, fontSize: 34, rotate: "-12deg", scale: `${spring({ frame: k - no, fps, config: { damping: 8, stiffness: 220 } })}` }}>NO</div> : null;
  return (
    <div style={{ position: "absolute", left: 0, top: 0, width: 1240, height: H, background: "radial-gradient(circle at 40% 35%, #FFFDF5, #F0E6FF 70%, #D9C8FF 100%)", overflow: "hidden" }}>
      <div style={{ position: "absolute", left: 50, top: 40, fontFamily: HEADING, fontWeight: 700, fontSize: 70, color: "#2A1A5E", textShadow: "0 5px 0 rgba(169,139,255,0.5)" }}>The family key</div>
      <div style={{ position: "absolute", left: 54, top: 130, width: 520, fontFamily: BODY, fontWeight: 700, fontSize: 30, lineHeight: 1.25, color: "#3F3566" }}>Two-step verification, where the second key is the family.</div>
      <PeriMoods scene="r4" parts={[{ from, pose: "peri_thinking" }, { from: say("r4", 4) - 4, pose: "peri_calling" }, { from: say("r4", 5) - 4, pose: "peri_waiting" }, { from: say("r4", 6) - 4, pose: "peri_calm" }]} x={20} y={300} w={560} h={H - 300} />
      <svg style={{ position: "absolute", left: 0, top: 0 }} width={1240} height={H}>
        {[740, 1080].map((x2) => (
          <line key={x2} x1={910} y1={250} x2={910 + (x2 - 910) * beam} y2={250 + (330 - 250) * beam} stroke="#A98BFF" strokeWidth={8} strokeDasharray="18 14" strokeDashoffset={-k * 3} strokeLinecap="round" />
        ))}
      </svg>
      {card("pear_rosa", "Rosa", 910, 40, 200, 6)}
      {card("pear_ana", "Ana", 740, 330, 170, asked + 10, <>{lock(say("r4", 5, "each confirmed") - from)}{nope}</>)}
      {card("pear_pablo", "Pablo", 1080, 330, 170, asked + 16, lock(say("r4", 5, "each confirmed") - from + 8))}
      {STEPS.map((s, i) => {
        const at = say("r4", s.line, s.word) - from;
        if (k < at) return null;
        const p = spring({ frame: k - at, fps, config: { damping: 11, stiffness: 180 } });
        return (
          <div key={s.text} style={{ position: "absolute", left: 620, top: 590 + i * 64, display: "flex", alignItems: "center", gap: 14, translate: `${(1 - p) * 120}px 0`, opacity: p }}>
            <div style={{ width: 40, height: 40, borderRadius: 20, background: s.color, display: "grid", placeItems: "center", fontFamily: HEADING, fontWeight: 700, fontSize: 24, color: INK, flex: "none" }}>{i + 1}</div>
            <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 28, color: INK, whiteSpace: "nowrap" }}>{s.text}</div>
          </div>
        );
      })}
    </div>
  );
};

/** 1:26 Round 4: keep her talking and get the code; Peri pauses it and the family key does the rest. */
const Round4 = () => {
  const T = TAKES.r4;
  const keyFrom = say("r4", 3) - 8;
  const anaFrom = say("r4", 7) - 10;
  const result = say("r4", 8) - 6;
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const pause = spring({ frame: fr - say("r4", 2), fps, config: { damping: 11, stiffness: 160 } });
  return (
    <>
      <Cast scene="r4" parts={[{ from: 0, pose: "crook_phone", who: "crook" }, { from: say("r4", 1) - 8, pose: "rosa_garden_call", who: "rosa" }, { from: anaFrom, pose: "ana_no", who: "ana" }, { from: result, pose: "crook_quit", who: "crook" }]} />
      <AppSide colors={["#EEE6FF", "#A98BFF"]} />
      {fr < keyFrom && (
        <>
          <PeriMoods scene="r4" parts={[{ from: 30, pose: "peri_listening" }, { from: say("r4", 2) - 4, pose: "peri_protective" }]} x={1180} y={180} w={680} h={H - 180} />
          <Slam text="PAUSED." at={say("r4", 2)} until={keyFrom - 2} x={800} y={90} size={120} color={CREAM} shade="#2A1A5E" rotate={-5} />
          {fr >= say("r4", 2) + 10 && (
            <div style={{ position: "absolute", left: 800, top: 280, width: 360, padding: "18px 24px", borderRadius: 24, background: CREAM, boxShadow: "0 10px 0 rgba(0,0,0,0.15)", fontFamily: BODY, fontWeight: 700, fontSize: 30, lineHeight: 1.25, color: "#2A1A5E", scale: `${pause}` }}>
              A code during a stranger's call needs the family's yes.
            </div>
          )}
        </>
      )}
      <Sequence from={keyFrom} durationInFrames={result - keyFrom} layout="none">
        <AppPhone take={T} x={PHONE.x} y={PHONE.y} h={PHONE.h} tilt={-3} marks={[{ region: SCAM_BOX, from: say("r4", 4, "Ana and Pablo") - keyFrom, to: say("r4", 5) - keyFrom }, { region: TWO_YES, from: say("r4", 5) - keyFrom, to: anaFrom - keyFrom, color: "#6EC6FF" }, { region: BUTTONS, from: anaFrom - keyFrom, to: result - keyFrom, color: "#A98BFF" }]} />
        <Zoom take={T} region={BUTTONS} x={800} y={430} w={560} from={anaFrom - keyFrom} to={result - keyFrom} label="One tap" color="#A98BFF" />
        <PhoneLabel text="ANA'S PHONE" />
      </Sequence>
      {fr >= keyFrom && fr < anaFrom && <FamilyKey from={keyFrom} />}
      <Slam text="FAMILY SAYS NO." at={ends("r4", 7)} until={result - 2} x={800} y={90} size={80} color={CREAM} bg="#2A1A5E" rotate={-5} />
      <Point n={4} at={result} />
      <Banner n={4} title="Keep her talking" color="#A98BFF" />
      <Flash />
      <Flash at={keyFrom} />
      <Flash at={anaFrom} />
      <Snd src="final/sfx/impact" at={0} volume={0.55} />
      <Snd src="submission/sfx/alert" at={say("r4", 2)} volume={0.55} />
      <Snd src="short/sfx/ding" at={keyFrom + 12} volume={0.5} />
      <Snd src="final/sfx/slam" at={say("r4", 5, "each confirmed")} volume={0.45} />
      <Snd src="reel/sfx/stamp" at={say("r4", 5, "A single no")} volume={0.55} />
      <Snd src="reel/sfx/stamp" at={ends("r4", 7)} volume={0.6} />
      <Snd src="short/sfx/score" at={ends("r4", 8) + f(1.2)} volume={0.55} />
    </>
  );
};

const GATES = [
  { title: "Rules on the phone", text: "Work offline", color: "#7CC242", word: "Rules" },
  { title: "Hard rules", text: "No AI can overrule them", color: "#FF8A7A", word: "Hard rules" },
  { title: "Muse Spark Open", text: "A second opinion, ±2 at most", color: "#6EC6FF", word: "A second" },
  { title: "The family", text: "The final yes", color: "#A98BFF", word: "And the family" },
];
const TEAM = [
  { name: "Muse Spark Open", job: "Second opinion", color: "#FFB27A", robot: 5 },
  { name: "Muse Spark + SAM 3.1", job: "Investigator, pictures", color: "#FF8A7A", robot: 0 },
  { name: "Muse Voice Transcribe", job: "Peri's ears", color: "#6EC6FF", robot: 1 },
  { name: "Muse Image", job: "Peri's looks", color: "#A98BFF", robot: 2 },
  { name: "Muse Spark contributor", job: "Training data, red team", color: "#7CC242", robot: 4 },
  { name: "Muse Wonder", job: "Ideas for her lines", color: "#FFC83D", robot: 3 },
];
const ROBOTS_X = [0.105, 0.262, 0.412, 0.57, 0.725, 0.88];

/** 2:06 How Peri does it: Rosa asks; four layers; the team; the results. */
const Tech = () => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const layersAt = say("tech", 2) - 6;
  const teamAt = say("tech", 3) - 6;
  const resultsAt = say("tech", 4) - 6;
  const gates = GATES.map((g) => say("tech", 2, g.word));
  const reached = gates.filter((t) => fr >= t).length;
  const cards = TEAM.map((_, i) => say("tech", 3) + 8 + i * 18);
  return (
    <>
      {fr < layersAt && (
        <>
          <Actor pose="rosa_home" who="rosa" scene="tech" />
          <div style={{ position: "absolute", left: SPLIT, top: 0, width: 1920 - SPLIT, height: H, background: "radial-gradient(circle at 50% 40%, #FFF8EC, #FFD27A 80%)" }} />
          {fr < say("tech", 1) - 4 && <Actor pose="peri_listening" who="peri" scene="tech" x={1000} y={280} w={680} h={680} contain enter={6} />}
          <Pop src={pic("reel/peri_guard_cut")} x={1340} y={960} size={680} at={say("tech", 1) - 4} />
          <Slam text="TEAMWORK!" at={say("tech", 1)} x={1340} y={70} size={120} color={INK} shade="rgba(217,155,0,0.5)" rotate={-4} center />
        </>
      )}
      {fr >= layersAt && fr < teamAt && (
        <AbsoluteFill style={{ height: H, background: "radial-gradient(circle at 50% 40%, #1E3B5C, #0B1626 85%)" }}>
          <AbsoluteFill style={{ backgroundImage: "linear-gradient(rgba(110,198,255,0.08) 2px, transparent 2px), linear-gradient(90deg, rgba(110,198,255,0.08) 2px, transparent 2px)", backgroundSize: "60px 60px" }} />
          <div style={{ position: "absolute", left: 0, right: 0, top: 50, textAlign: "center", fontFamily: HEADING, fontWeight: 700, fontSize: 70, color: CREAM }}>How Peri decides</div>
          <Actor pose="peri_thinking" who="peri" scene="tech" x={10} y={640} w={250} h={H - 640} contain enter={layersAt} />
          {GATES.map((g, i) => {
            const p = spring({ frame: fr - gates[i], fps, config: { damping: 11, stiffness: 160 } });
            const lit = reached - 1 === i;
            return (
              <div key={g.title} style={{ position: "absolute", left: 300 + i * 380, top: 220, width: 320, height: 450, borderRadius: 40, background: fr >= gates[i] ? g.color : "rgba(255,255,255,0.08)", boxShadow: lit ? `0 0 60px ${g.color}` : "0 14px 0 rgba(0,0,0,0.3)", scale: `${fr >= gates[i] ? 0.9 + 0.1 * p : 0.9}`, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: 24, textAlign: "center" }}>
                <div style={{ width: 92, height: 92, borderRadius: 46, background: "rgba(255,255,255,0.9)", display: "grid", placeItems: "center", fontFamily: HEADING, fontWeight: 700, fontSize: 54, color: INK }}>{i + 1}</div>
                <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 44, lineHeight: 1.05, color: INK, marginTop: 22, opacity: fr >= gates[i] ? 1 : 0.2 }}>{g.title}</div>
                <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 27, color: INK, marginTop: 10, opacity: fr >= gates[i] ? 0.85 : 0 }}>{g.text}</div>
              </div>
            );
          })}
          {reached > 0 && (
            <div style={{ position: "absolute", top: 720, left: interpolate(spring({ frame: fr - gates[reached - 1], fps, config: { damping: 14 } }), [0, 1], [300 + Math.max(0, reached - 2) * 380, 300 + (reached - 1) * 380]) + 160, translate: "-50% 0", padding: "14px 26px", borderRadius: "28px 28px 28px 6px", background: reached === 4 && fr > gates[3] + 20 ? "#E5533D" : CREAM, color: reached === 4 && fr > gates[3] + 20 ? CREAM : INK, fontFamily: BODY, fontWeight: 700, fontSize: 28, whiteSpace: "nowrap", boxShadow: "0 10px 24px rgba(0,0,0,0.35)" }}>
              {reached === 4 && fr > gates[3] + 20 ? "TRAP. Stopped." : "“Hi Mum, new number...”"}
            </div>
          )}
          {gates.map((t, i) => (
            <Snd key={i} src="final/sfx/slam" at={t} volume={0.5} />
          ))}
        </AbsoluteFill>
      )}
      {fr >= teamAt && fr < resultsAt && (
        <AbsoluteFill style={{ height: H, background: "radial-gradient(circle at 30% 70%, #2F6B22, #10260B 85%)" }}>
          <div style={{ position: "absolute", left: 30, top: 230, width: 740, height: 740 }}>
            {/* Each little helper lights up from behind, with a spot on the floor, as its model's card comes in. */}
            {TEAM.map((m, i) => {
              if (fr < cards[i]) return null;
              const s = spring({ frame: fr - cards[i], fps, config: { damping: 8, stiffness: 180 } });
              const x = ROBOTS_X[m.robot] * 740;
              return (
                <div key={m.name}>
                  <div style={{ position: "absolute", left: x - 85, top: 0.86 * 740 - 95, width: 170, height: 170, borderRadius: "50%", background: `radial-gradient(circle, ${m.color} 0%, ${m.color}99 38%, transparent 70%)`, scale: `${s}` }} />
                  <div style={{ position: "absolute", left: x - 58, top: 0.86 * 740 + 50, width: 116, height: 26, borderRadius: "50%", background: m.color, opacity: 0.85, scale: `${s}`, filter: "blur(1px)" }} />
                </div>
              );
            })}
            <Img src={img("conductor")} style={{ position: "absolute", inset: 0, width: "100%", height: "100%", filter: "drop-shadow(0 18px 22px rgba(0,0,0,0.35))" }} />
          </div>
          <div style={{ position: "absolute", left: 0, right: 0, top: 40, textAlign: "center", fontFamily: HEADING, fontWeight: 700, fontSize: 72, color: CREAM, textShadow: "0 6px 0 rgba(0,0,0,0.3)" }}>Six Meta models, one job each</div>
          {TEAM.map((m, i) => {
            if (fr < cards[i]) return null;
            const s = spring({ frame: fr - cards[i], fps, config: { damping: 10, stiffness: 200 } });
            return (
              <div key={m.name} style={{ position: "absolute", left: 830 + (i % 2) * 540, top: 190 + Math.floor(i / 2) * 230, width: 510, padding: "22px 26px", borderRadius: 34, background: CREAM, boxShadow: `0 10px 0 ${m.color}, 0 22px 34px rgba(0,0,0,0.3)`, scale: `${interpolate(s, [0, 1], [1.6, 1])}`, opacity: Math.min(1, s * 3), display: "flex", gap: 20, alignItems: "center" }}>
                <div style={{ width: 56, height: 56, borderRadius: 28, background: m.color, flex: "none" }} />
                <div>
                  <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 38, color: INK, lineHeight: 1.05 }}>{m.name}</div>
                  <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 28, color: "#3F4D39", marginTop: 4 }}>{m.job}</div>
                </div>
              </div>
            );
          })}
          {cards.map((t, i) => (
            <Snd key={i} src="final/sfx/slam" at={t} volume={0.3} />
          ))}
        </AbsoluteFill>
      )}
      {fr >= resultsAt && (
        <AbsoluteFill style={{ height: H, background: "radial-gradient(circle at 50% 45%, #FFF6C8, #FFC83D 60%, #E59B00 100%)" }}>
          <Slam text="91%" at={say("tech", 4)} x={960} y={150} size={330} color={INK} shade="rgba(255,255,255,0.55)" rotate={-3} center />
          <Slam text="of 200 unseen scams, caught on the phone, before any model" at={say("tech", 4) + 14} x={960} y={600} size={46} color={CREAM} bg="#25541A" rotate={0} center />
          <Slam text="4% false alarms" at={say("tech", 4) + 40} x={960} y={720} size={44} color={INK} bg={CREAM} rotate={2} center />
          <Burst x={960} y={380} delay={say("tech", 4)} count={40} colors={CONFETTI} />
          <Snd src="final/sfx/impact" at={say("tech", 4)} volume={0.6} />
        </AbsoluteFill>
      )}
      <Flash at={layersAt} />
      <Flash at={teamAt} />
      <Flash at={resultsAt} />
    </>
  );
};

/**
 * 2:30 More than a shield: a friend (Peri tries on her wardrobe while she says so), then "I've fallen": Rosa on the
 * rug, Peri listening, the real app checking on her and calling Ana.
 */
const Heart = () => {
  const fr = useCurrentFrame();
  const fallen = say("heart", 1) - 6;
  const phoneAt = say("heart", 2) - 12;
  const anaAt = say("heart", 3) - 8;
  const T = TAKES.heart;
  const looks = OUTFITS.map((o, i) => ({ from: 4 + i * 22, pose: `dressed_${o}` }));
  return (
    <>
      {/* Ana takes over behind the phone, so she's there the moment it goes. */}
      <Cast scene="heart" parts={[{ from: 0, pose: "rosa_home", who: "rosa" }, { from: fallen, pose: "rosa_fallen", who: "rosa" }, { from: anaAt - 12, pose: "ana_go_rest", who: "ana" }]} />
      {fr < fallen && (
        <div style={{ position: "absolute", left: SPLIT, top: 0, width: 1920 - SPLIT, height: H, background: "radial-gradient(circle at 50% 40%, #FFF8EC, #FFE2B8 85%)", overflow: "hidden" }}>
          <Floaties x={60} w={1040} kinds={["fx_heart_pink", "fx_music_note", "fx_heart_coral", "fx_star"]} />
          <div style={{ position: "absolute", left: 0, right: 0, top: 40, textAlign: "center", fontFamily: HEADING, fontWeight: 700, fontSize: 76, color: INK }}>Just a friend.</div>
          <PeriMoods scene="heart" parts={looks} x={230} y={170} w={700} h={780} enter={2} />
          {looks.slice(1).map((l) => (
            <Sparks key={l.from} at={l.from} x={580} y={430} kinds={["fx_star", "fx_heart_pink", "fx_star_green"]} count={8} spread={300} />
          ))}
        </div>
      )}
      {fr >= fallen && fr < phoneAt && (
        <div style={{ position: "absolute", left: SPLIT, top: 0, width: 1920 - SPLIT, height: H, background: "radial-gradient(circle at 50% 45%, #FFF3EA, #FFC9BC 85%)" }}>
          <PeriMoods scene="heart" parts={[{ from: fallen, pose: "peri_listening" }]} x={260} y={180} w={640} h={740} />
        </div>
      )}
      <Sequence from={phoneAt} durationInFrames={anaAt - phoneAt} layout="none">
        <AbsoluteFill style={{ height: H, background: "radial-gradient(circle at 50% 40%, #F4FBFF, #A9D8F5 90%)" }} />
        <AppPhone take={T} x={960} y={492} h={860} tilt={0} marks={[{ region: BUBBLE, from: shown(T, 4.0), to: shown(T, 6.4) }, { region: CALLER, from: shown(T, 16.9), to: anaAt - phoneAt, color: "#6EC6FF" }]} />
        <Zoom take={T} region={BUBBLE} x={80} y={330} w={620} from={shown(T, 4.2)} to={shown(T, 6.4)} label="Peri checks" />
        <Zoom take={T} region={CALLER} x={1230} y={330} w={600} from={shown(T, 16.9)} to={anaAt - phoneAt} label="No answer: calling Ana" color="#6EC6FF" />
      </Sequence>
      {fr >= anaAt && (
        <>
          <div style={{ position: "absolute", left: SPLIT, top: 0, width: 1920 - SPLIT, height: H, background: "radial-gradient(circle at 50% 40%, #DDF2FF, #6EC6FF 90%)" }} />
          <Slam text={"Family,\none call away."} at={anaAt + 4} x={1340} y={170} size={110} color={INK} shade="rgba(255,255,255,0.6)" rotate={-3} center />
          <Pop src={img("calling")} x={1340} y={950} size={460} at={anaAt + 1} />
        </>
      )}
      {looks.map((l) => (
        <Snd key={l.from} src="short/sfx/pop" at={l.from} volume={0.4} />
      ))}
      <Snd src="submission/sfx/listen" at={phoneAt + shown(T, 13.0)} volume={0.45} />
      <Snd src="submission/sfx/ringback" at={phoneAt + shown(T, 16.8)} volume={0.3} length={Math.max(1, anaAt - phoneAt - shown(T, 16.8))} />
    </>
  );
};

/** 2:44 The final score, who built Peri, and the end card. */
const Finale = () => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const card = say("finale", 2) - 24;
  const logo = spring({ frame: fr - card - 4, fps, config: { damping: 10, stiffness: 140 } });
  if (fr < card) {
    return (
      <>
        <Actor pose="crook_angry" who="crook" scene="finale" />
        <div style={{ position: "absolute", left: SPLIT, top: 0, width: 1920 - SPLIT, height: H, background: "radial-gradient(circle at 50% 40%, #2F6B22, #10260B 85%)" }} />
        <Slam text="SCAMMERS 0 · PERI 4" at={4} x={1340} y={60} size={84} color={CREAM} shade="#0B2007" rotate={-2} center />
        <Slam text="BUILT BY RAFA IN 9 DAYS" at={say("finale", 1, "In nine")} x={1340} y={210} size={50} color={INK} bg="#FFC83D" rotate={2} center />
        <Pop src={img("hero")} x={1340} y={970} size={640} at={say("finale", 1)} />
        <Snd src="final/sfx/impact" at={4} volume={0.55} />
      </>
    );
  }
  return (
    <AbsoluteFill style={{ height: H, background: "radial-gradient(circle at 30% 45%, #FFF8EC, #D6F0A6 50%, #7CC242 100%)", overflow: "hidden" }}>
      <div style={{ position: "absolute", left: "26%", top: "50%", width: 3200, height: 3200, marginLeft: -1600, marginTop: -1600, rotate: `${fr * 0.25}deg`, background: "repeating-conic-gradient(rgba(255,255,255,0.3) 0deg 8deg, transparent 8deg 20deg)" }} />
      <Burst x={480} y={330} delay={card + 8} count={36} colors={CONFETTI} />
      <Actor pose="peri_calm" who="peri" scene="finale" x={80} y={120} w={800} h={H - 120} contain enter={card + 2} />
      <div style={{ position: "absolute", left: 990, top: 100, display: "flex", alignItems: "center", gap: 24, scale: `${logo}`, transformOrigin: "0% 50%" }}>
        <Img src={img("icon")} style={{ width: 140, height: 140, borderRadius: "50%", boxShadow: "0 8px 0 rgba(0,0,0,0.12)" }} />
        <span style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 112, color: INK, textShadow: "0 7px 0 rgba(79,138,34,0.5)" }}>FriendlyPear</span>
      </div>
      <Slam text={"You relax.\nI'll keep watch."} at={say("finale", 2, "You relax")} x={1000} y={290} size={84} color={INK} shade="rgba(255,255,255,0.6)" rotate={-2} />
      <Slam text="Real app · Android · English & Spanish" at={say("finale", 2) + 40} x={1000} y={540} size={36} color={CREAM} bg="#25541A" rotate={0} />
      <Slam text="github.com/Ruby570bocadito/FriendlyPear" at={say("finale", 2) + 60} x={1000} y={640} size={36} color={INK} bg={CREAM} rotate={0} />
      <Slam text="Built by Rafa · Meta Global AI Developer Hackathon · Indie track" at={say("finale", 2) + 80} x={1000} y={740} size={26} color={INK} bg="rgba(255,248,236,0.85)" rotate={0} />
      <Snd src="submission/sfx/sparkle" at={card + 8} volume={0.5} />
    </AbsoluteFill>
  );
};

const BODIES: Record<Name, ReactNode> = {
  cold: <Cold />,
  title: <Title />,
  stakes: <Stakes />,
  r1: <Round1 />,
  r2: <Round2 />,
  r3: <Round3 />,
  r4: <Round4 />,
  tech: <Tech />,
  heart: <Heart />,
  finale: <Finale />,
};

export const Final = () => (
  <ScriptContext.Provider value={FINAL}>
  <AbsoluteFill style={{ background: "#0E0A18" }}>
    <Audio src={staticFile("final/music.wav")} volume={musicVolume(SPOKEN)} />
    {(Object.keys(AT) as Name[]).map((name) => (
      <Sequence key={name} from={f(AT[name][0])} durationInFrames={f(AT[name][1] - AT[name][0])}>
        <div style={{ position: "absolute", left: 0, top: 0, width: 1920, height: H, overflow: "hidden" }}>{BODIES[name]}</div>
        {name !== "cold" && !name.startsWith("r") && <Flash />}
      </Sequence>
    ))}
    <Scoreboard />
    <SubtitleBand />
    {SPOKEN.map((s) => (
      <Sequence key={`${s.scene}${s.n}`} from={f(s.start)} layout="none">
        <Audio src={staticFile(`final/vo_${s.scene}_${s.n}.wav`)} />
      </Sequence>
    ))}
    {(Object.keys(AT) as Name[]).slice(1).map((name) => (
      <Snd key={name} src="submission/sfx/whoosh" at={f(AT[name][0]) - 8} volume={0.35} />
    ))}
  </AbsoluteFill>
  </ScriptContext.Provider>
);
