import type { ReactNode } from "react";
import { AbsoluteFill, Audio, Img, interpolate, Sequence, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import timeline from "../public/reel/timeline.json";
import { BODY, clamp, HEADING } from "./theme";
import { Burst, CONFETTI } from "./Minute";

/**
 * The interview reel (1080x1920, 30 fps): Rosa interviews bodyguards for her phone. A padlock that locks out her
 * grandson and a camera that spies are turned down; Peri gets the job, and on day one stops a bank scam. Every picture
 * is new (scripts/reel_images.ts, Muse Image, cut out with Adobe Photoshop); the voices, the timeline
 * (scripts/reel_voice.py) and the music and effects (scripts/reel_music.py) were made for it. Shots follow the lines.
 * Words stay between about 200 and 1500 px from the top, clear of Instagram's own buttons and caption.
 */
const FPS = 30;
type Line = { n?: number; who: string; shot: string; text: string; start: number; end: number };
const LINES = timeline.lines as Line[];
export const INTERVIEW_FRAMES = Math.ceil(timeline.length * FPS);
const INK = "#1F2A1A";
const f = (seconds: number) => Math.round(seconds * FPS);
const pic = (name: string) => staticFile(`reel/${name}.png`);
const sfx = (name: string) => staticFile(`reel/sfx/${name}.wav`);

/** Shots: runs of lines on the same picture, each lasting until the next one starts. */
const SHOTS = LINES.reduce<{ shot: string; start: number; lines: Line[] }[]>((all, line) => {
  const last = all[all.length - 1];
  if (last && last.shot === line.shot) last.lines.push(line);
  else all.push({ shot: line.shot, start: all.length ? line.start - 0.15 : 0, lines: [line] });
  return all;
}, []).map((s, i, all) => ({ ...s, end: i + 1 < all.length ? all[i + 1].start : timeline.length }));

const CANDIDATES: Record<string, { n: number; name: string; skill: string; color: string }> = {
  padlock: { n: 1, name: "The Padlock", skill: "Locks everything. And everyone.", color: "#E0A526" },
  camera: { n: 2, name: "The Camera", skill: "Watches everything. Tells everyone.", color: "#7D8A99" },
  peri_tie: { n: 3, name: "Peri, a pear", skill: "Stops scams. Never spies.", color: "#7CC242" },
};
const NAMES: Record<string, string> = { rosa: "ROSA", lock: "THE PADLOCK", cam: "THE CAMERA", peri: "PERI" };

const hired = LINES.find((l) => l.shot === "rosa_laugh")!;
const joke = LINES.find((l) => l.text.startsWith("I'm a pear"))!;
const nexts = LINES.filter((l) => l.text.endsWith("Next!"));
const scam = LINES.find((l) => l.shot === "scam")!;
const stop = LINES.find((l) => l.shot === "peri_stop")!;
const outro = LINES.find((l) => l.shot === "end")!;

/** A picture filling the frame, slowly pushing in. */
const Full = ({ name, position = "35% 50%", push = 0.05, length }: { name: string; position?: string; push?: number; length: number }) => {
  const fr = useCurrentFrame();
  return (
    <AbsoluteFill style={{ overflow: "hidden" }}>
      <Img src={pic(name)} style={{ width: "100%", height: "100%", objectFit: "cover", objectPosition: position, scale: `${1 + interpolate(fr, [0, length], [0, push], clamp)}` }} />
    </AbsoluteFill>
  );
};

/** A candidate standing on the spotlight in the interview room, breathing and swaying. */
const Candidate = ({ name, length, punch, enter = true }: { name: string; length: number; punch?: number; enter?: boolean }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const c = CANDIDATES[name];
  const walk = enter ? spring({ frame: fr - 2, fps, config: { damping: 11, stiffness: 120 } }) : 1;
  const zoom = punch === undefined ? 0 : spring({ frame: fr - punch, fps, config: { damping: 14, stiffness: 160 } }) * 0.22;
  const plate = enter ? spring({ frame: fr - 10, fps, config: { damping: 13 } }) : 1;
  return (
    <AbsoluteFill style={{ overflow: "hidden" }}>
      <AbsoluteFill style={{ scale: `${1 + zoom}`, transformOrigin: "50% 42%" }}>
        <Img src={pic("office")} style={{ width: "100%", height: "100%", objectFit: "cover", scale: `${1.02 + interpolate(fr, [0, length], [0, 0.04], clamp)}` }} />
        <div style={{ position: "absolute", left: 40, right: 40, top: 520, height: 1000, translate: `${(1 - walk) * 700}px 0`, rotate: `${Math.sin(fr / 14) * 1.5}deg`, transformOrigin: "50% 100%" }}>
          <div style={{ position: "absolute", left: "26%", right: "26%", bottom: 10, height: 60, borderRadius: "50%", background: "rgba(80,40,10,0.28)", filter: "blur(10px)" }} />
          <Img src={pic(`${name}_cut`)} style={{ width: "100%", height: "100%", objectFit: "contain", objectPosition: "50% 100%", scale: `${1 + Math.sin(fr / 11) * 0.012} ${1 - Math.sin(fr / 11) * 0.012}`, transformOrigin: "50% 100%" }} />
        </div>
      </AbsoluteFill>
      <div style={{ position: "absolute", left: 60, top: 1290, translate: `${(1 - plate) * -900}px 0`, display: "flex", flexDirection: "column", gap: 0, filter: "drop-shadow(0 10px 0 rgba(0,0,0,0.18))" }}>
        <div style={{ alignSelf: "flex-start", padding: "8px 22px", background: INK, color: "#FFF8EC", fontFamily: BODY, fontWeight: 700, fontSize: 30, letterSpacing: 3, borderRadius: "18px 18px 0 0" }}>CANDIDATE #{c.n}</div>
        <div style={{ padding: "14px 30px 16px", background: c.color, borderRadius: "0 22px 22px 22px" }}>
          <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 64, color: "#FFF8EC", lineHeight: 1, textShadow: "0 4px 0 rgba(0,0,0,0.2)" }}>{c.name}</div>
          <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 32, color: "#FFF8EC", marginTop: 6 }}>{c.skill}</div>
        </div>
      </div>
    </AbsoluteFill>
  );
};

/** A comic speech bubble: who's talking, and their words typing out as they say them. */
const Bubble = ({ line, x, y, w, tail }: { line: Line; x: number; y: number; w: number; tail: "left" | "center" | "right" }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const from = f(line.start);
  const to = f(line.end);
  if (fr < from - 2 || fr > to + 10) return null;
  const p = spring({ frame: fr - from + 2, fps, config: { damping: 11, stiffness: 180 } });
  const out = interpolate(fr, [to + 4, to + 10], [1, 0], clamp);
  const shown = Math.ceil(line.text.length * interpolate(fr, [from, from + (to - from) * 0.85], [0, 1], clamp));
  const tailX = tail === "left" ? "18%" : tail === "right" ? "80%" : "50%";
  return (
    <div style={{ position: "absolute", left: x, top: y, width: w, scale: `${p * out}`, transformOrigin: `${tailX} 110%` }}>
      <div style={{ position: "relative", background: "#FFFFFF", border: `6px solid ${INK}`, borderRadius: 44, padding: "22px 34px 28px", boxShadow: "0 12px 0 rgba(0,0,0,0.18)" }}>
        <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 26, letterSpacing: 3, color: "#8A7F6A", marginBottom: 6 }}>{NAMES[line.who]}</div>
        <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 54, lineHeight: 1.14, color: INK }}>
          {line.text.slice(0, shown)}
          <span style={{ opacity: 0 }}>{line.text.slice(shown)}</span>
        </div>
        <div style={{ position: "absolute", left: tailX, bottom: -40, width: 0, height: 0, translate: "-50% 0", borderLeft: "26px solid transparent", borderRight: "26px solid transparent", borderTop: `40px solid ${INK}` }} />
        <div style={{ position: "absolute", left: tailX, bottom: -28, width: 0, height: 0, translate: "-50% 0", borderLeft: "18px solid transparent", borderRight: "18px solid transparent", borderTop: "30px solid #FFFFFF" }} />
      </div>
    </div>
  );
};

/** A big red buzzer cross when Rosa says "Next!". */
const Buzzer = ({ at }: { at: number }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  if (fr < at || fr > at + 28) return null;
  const p = spring({ frame: fr - at, fps, config: { damping: 8, stiffness: 220 } });
  const out = interpolate(fr, [at + 20, at + 28], [1, 0], clamp);
  return (
    <AbsoluteFill style={{ display: "grid", placeItems: "center", opacity: out }}>
      <div style={{ width: 420, height: 420, borderRadius: "50%", background: "#E5533D", border: "14px solid #FFF8EC", boxShadow: "0 18px 0 rgba(0,0,0,0.25)", display: "grid", placeItems: "center", scale: `${p}`, rotate: `${(1 - p) * -40}deg` }}>
        <span style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 300, lineHeight: 1, color: "#FFF8EC", marginTop: -20 }}>×</span>
      </div>
      <div style={{ position: "absolute", top: 1180, fontFamily: HEADING, fontWeight: 700, fontSize: 120, color: "#FFF8EC", textShadow: "0 8px 0 #9C1F17", scale: `${p}` }}>NEXT!</div>
    </AbsoluteFill>
  );
};

/** HIRED: a green stamp landing on Rosa's laugh. */
const Hired = ({ at, until }: { at: number; until: number }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  if (fr < at || fr >= until) return null;
  const p = spring({ frame: fr - at, fps, config: { damping: 9, stiffness: 200 } });
  return (
    <>
      <div style={{ position: "absolute", left: 0, right: 0, top: 1060, display: "flex", justifyContent: "center" }}>
        <div style={{ padding: "18px 56px", borderRadius: 34, border: "14px solid #25541A", background: "rgba(214,240,166,0.94)", color: "#25541A", fontFamily: HEADING, fontWeight: 700, fontSize: 150, letterSpacing: 6, rotate: "-8deg", scale: `${interpolate(p, [0, 1], [2.6, 1])}`, opacity: Math.min(1, p * 2) }}>HIRED!</div>
      </div>
      <Burst x={540} y={1100} delay={at + 4} count={40} colors={CONFETTI} />
    </>
  );
};

/** Day one: the phone on a dark table, buzzing with a fake bank message. */
const ScamPhone = ({ x, y, scale = 1, tilt = 0, crossed = false, at = 8 }: { x: number; y: number; scale?: number; tilt?: number; crossed?: boolean; at?: number }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const drop = spring({ frame: fr - at, fps, config: { damping: 13, stiffness: 140 } });
  const buzz = fr >= at && fr < at + 24 ? Math.sin(fr * 2.4) * 9 : 0;
  const cross = spring({ frame: fr - 8, fps, config: { damping: 9, stiffness: 200 } });
  return (
    <div style={{ position: "absolute", left: x - 320, top: y - 620, width: 640, height: 1240, scale: `${scale}`, rotate: `${tilt}deg`, translate: `${buzz}px 0` }}>
      <div style={{ width: "100%", height: "100%", borderRadius: 80, background: "#121416", padding: 18, boxShadow: "0 40px 80px rgba(0,0,0,0.45)" }}>
        <div style={{ position: "relative", width: "100%", height: "100%", borderRadius: 64, overflow: "hidden", background: "linear-gradient(180deg, #3A4C6B, #20283A)" }}>
          <div style={{ position: "absolute", top: 120, left: 0, right: 0, textAlign: "center", fontFamily: HEADING, fontWeight: 600, fontSize: 150, color: "#FFFFFF" }}>10:24</div>
          <div style={{ position: "absolute", top: 300, left: 0, right: 0, textAlign: "center", fontFamily: BODY, fontWeight: 700, fontSize: 34, color: "rgba(255,255,255,0.8)" }}>Tuesday, 3 November</div>
          <div style={{ position: "absolute", left: 28, right: 28, top: 420, translate: `0 ${(1 - drop) * -500}px`, opacity: drop, background: "rgba(255,255,255,0.94)", borderRadius: 36, padding: "26px 28px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 14, marginBottom: 10 }}>
              <div style={{ width: 52, height: 52, borderRadius: 14, background: "#C0392B", display: "grid", placeItems: "center", color: "#FFF", fontFamily: HEADING, fontWeight: 700, fontSize: 30 }}>!</div>
              <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 30, color: "#1E2228" }}>BANK ALERT</div>
              <div style={{ fontFamily: BODY, fontSize: 26, color: "#7A8290", marginLeft: "auto" }}>now</div>
            </div>
            <div style={{ fontFamily: BODY, fontSize: 36, lineHeight: 1.3, color: "#1E2228" }}>Your account is blocked. Call us back now and read us the code we just sent.</div>
          </div>
          {crossed && (
            <div style={{ position: "absolute", left: 28, right: 28, top: 420, height: 330, display: "grid", placeItems: "center", scale: `${cross}` }}>
              <div style={{ padding: "12px 34px", borderRadius: 22, border: "10px solid #E5533D", color: "#E5533D", background: "rgba(255,248,236,0.95)", fontFamily: HEADING, fontWeight: 700, fontSize: 84, rotate: "-10deg" }}>SCAM</div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

/** The shot's picture and its own extras, by name. */
const ShotBody = ({ shot, length, enter }: { shot: string; length: number; enter: boolean }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  if (shot.startsWith("rosa_")) {
    const guard = spring({ frame: fr - 10, fps, config: { damping: 10, stiffness: 140 } });
    return (
      <>
        <Full name={shot} length={length} />
        {shot === "rosa_phone" && (
          <Img src={pic("peri_guard_cut")} style={{ position: "absolute", left: 560, top: 1000, width: 460, height: 460, scale: `${guard}`, transformOrigin: "50% 100%", rotate: `${Math.sin(fr / 12) * 2}deg` }} />
        )}
      </>
    );
  }
  if (shot in CANDIDATES) return <Candidate name={shot} length={length} enter={enter} />;
  if (shot === "scam") {
    return (
      <AbsoluteFill style={{ background: "radial-gradient(circle at 50% 40%, #3B2A40, #140C18)" }}>
        <div style={{ position: "absolute", left: 0, right: 0, top: 150, textAlign: "center", fontFamily: HEADING, fontWeight: 700, fontSize: 76, color: "#FFF8EC" }}>Day one on the job...</div>
        <ScamPhone x={540} y={1010} scale={0.86} at={10} />
      </AbsoluteFill>
    );
  }
  if (shot === "peri_stop") {
    const jump = spring({ frame: fr, fps, config: { damping: 10, stiffness: 160 } });
    return (
      <AbsoluteFill style={{ background: "radial-gradient(circle at 50% 35%, #D9503F, #8E1F18)" }}>
        <ScamPhone x={330} y={1090} scale={0.62} tilt={-8} crossed at={-30} />
        <Img src={pic("peri_stop_cut")} style={{ position: "absolute", left: 420, top: 640, width: 700, height: 700, translate: `${(1 - jump) * 600}px 0`, filter: "drop-shadow(0 20px 24px rgba(0,0,0,0.3))" }} />
      </AbsoluteFill>
    );
  }
  // The end card.
  const logo = spring({ frame: fr - 4, fps, config: { damping: 11, stiffness: 120 } });
  const guard = spring({ frame: fr, fps, config: { damping: 9, stiffness: 120 } });
  return (
    <AbsoluteFill style={{ background: "radial-gradient(circle at 40% 30%, #FFF8EC, #FFD27A 55%, #FF9E7A 100%)" }}>
      <div style={{ position: "absolute", left: 0, right: 0, top: 230, display: "flex", justifyContent: "center", alignItems: "center", gap: 22, scale: `${logo}` }}>
        <Img src={staticFile("minute/icon.png")} style={{ width: 120, height: 120, borderRadius: "50%" }} />
        <span style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 100, color: INK, textShadow: "0 7px 0 rgba(217,120,60,0.5)" }}>FriendlyPear</span>
      </div>
      <div style={{ position: "absolute", left: 60, right: 60, top: 400, textAlign: "center", fontFamily: HEADING, fontWeight: 700, fontSize: 70, lineHeight: 1.1, color: INK, opacity: logo }}>Your phone's new bodyguard.</div>
      <Img src={pic("peri_guard_cut")} style={{ position: "absolute", left: 140, top: 560, width: 800, height: 800, scale: `${guard}`, transformOrigin: "50% 100%", rotate: `${Math.sin(fr / 14) * 2}deg` }} />
      <div style={{ position: "absolute", left: 0, right: 0, top: 1390, display: "flex", justifyContent: "center", opacity: interpolate(fr, [20, 32], [0, 1], clamp) }}>
        <div style={{ padding: "16px 40px", borderRadius: 999, background: "#25541A", color: "#FFF8EC", fontFamily: HEADING, fontWeight: 700, fontSize: 50 }}>A friend in your phone. Not a spy.</div>
      </div>
      <Burst x={540} y={700} delay={6} count={36} colors={CONFETTI} />
    </AbsoluteFill>
  );
};

/** Where each shot's speech bubble sits and where its tail points. */
const bubbleFor = (shot: string): { x: number; y: number; w: number; tail: "left" | "center" | "right" } => {
  if (shot.startsWith("rosa_")) return { x: 300, y: 240, w: 720, tail: "left" };
  if (shot === "peri_stop") return { x: 60, y: 250, w: 960, tail: "right" };
  return { x: 70, y: 220, w: 940, tail: "center" };
};

/** A shot: whips in sideways unless it's Rosa's face changing in the same frame. */
const Shot = ({ shot, start, end, index, children }: { shot: string; start: number; end: number; index: number; children: ReactNode }) => {
  const from = f(start);
  const length = f(end) - from;
  const sameFrame = index > 0 && SHOTS[index - 1].shot.startsWith("rosa_") && shot.startsWith("rosa_");
  return (
    <Sequence from={from} durationInFrames={length + 1}>
      <Whip dir={index % 2 ? 1 : -1} off={sameFrame || index === 0}>
        {children}
      </Whip>
    </Sequence>
  );
};

const Whip = ({ dir, off, children }: { dir: number; off: boolean; children: ReactNode }) => {
  const fr = useCurrentFrame();
  if (off) return <AbsoluteFill>{children}</AbsoluteFill>;
  const x = interpolate(fr, [0, 7], [dir * 1080, 0], { ...clamp, easing: (t) => 1 - (1 - t) ** 3 });
  const blur = interpolate(fr, [0, 7], [28, 0], clamp);
  return <AbsoluteFill style={{ translate: `${x}px 0`, filter: blur > 0.5 ? `blur(${blur}px)` : undefined }}>{children}</AbsoluteFill>;
};

/** Which interview step this is, at the top. */
const Progress = () => {
  const fr = useCurrentFrame();
  const t = fr / FPS;
  const cand = SHOTS.find((s) => s.shot in CANDIDATES && t >= s.start && t < s.end);
  if (!cand || t > hired.start) return null;
  const c = CANDIDATES[cand.shot];
  return (
    <div style={{ position: "absolute", left: 0, right: 0, top: 120, display: "flex", justifyContent: "center", gap: 14 }}>
      {[1, 2, 3].map((n) => (
        <div key={n} style={{ width: 70, height: 14, borderRadius: 7, background: n <= c.n ? "#FFF8EC" : "rgba(255,248,236,0.35)", boxShadow: "0 3px 0 rgba(0,0,0,0.2)" }} />
      ))}
    </div>
  );
};

/** The hook over the first shot: what this is. */
const Hook = () => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const firstCut = f(SHOTS[1].start);
  if (fr > firstCut) return null;
  const p = spring({ frame: fr - 2, fps, config: { damping: 12 } });
  return (
    <div style={{ position: "absolute", left: 50, right: 50, top: 1180, textAlign: "center", scale: `${p}` }}>
      <span style={{ display: "inline", padding: "6px 18px", background: "#FFC83D", boxDecorationBreak: "clone", WebkitBoxDecorationBreak: "clone", fontFamily: HEADING, fontWeight: 700, fontSize: 68, lineHeight: 1.45, color: INK }}>
        Grandma is hiring a bodyguard for her phone
      </span>
    </div>
  );
};

/** The music: lower while someone speaks. */
const musicVolume = (fr: number) => {
  const t = fr / FPS;
  const speaking = LINES.some((l) => l.text && t >= l.start - 0.1 && t <= l.end + 0.05);
  return (speaking ? 0.2 : 0.5) * interpolate(t, [timeline.length - 0.8, timeline.length], [1, 0], clamp);
};

const Sound = ({ name, at, volume = 0.6 }: { name: string; at: number; volume?: number }) => (
  <Sequence from={Math.max(0, at)} layout="none">
    <Audio src={sfx(name)} volume={volume} />
  </Sequence>
);

export const Interview = () => {
  const punchAt = f(joke.start) + 2;
  return (
    <AbsoluteFill style={{ background: "#140C18" }}>
      <Audio src={staticFile("reel/music.wav")} volume={musicVolume} />
      {SHOTS.map((s, i) => (
        <Shot key={`${s.shot}${i}`} shot={s.shot} start={s.start} end={s.end} index={i}>
          {s.lines.includes(joke) ? (
            <Candidate name="peri_tie" length={f(s.end) - f(s.start)} punch={punchAt - f(s.start)} enter={false} />
          ) : (
            <ShotBody shot={s.shot} length={f(s.end) - f(s.start)} enter={SHOTS.findIndex((o) => o.shot === s.shot) === i} />
          )}
        </Shot>
      ))}
      <Hook />
      <Progress />
      {LINES.filter((l) => l.text && l.shot !== "end").map((l) => (
        <Bubble key={`${l.n}`} line={l} {...bubbleFor(l.shot)} />
      ))}
      {nexts.map((l) => (
        <Buzzer key={l.start} at={f(l.end) - 4} />
      ))}
      <Hired at={f(hired.start + (hired.end - hired.start) * 0.62)} until={f(scam.start) - 4} />
      {LINES.filter((l) => l.n !== undefined).map((l) => (
        <Sequence key={`vo${l.n}`} from={f(l.start)} layout="none">
          <Audio src={staticFile(`reel/vo_${l.n}.wav`)} />
        </Sequence>
      ))}
      {SHOTS.filter((s, i) => i > 0 && !(SHOTS[i - 1].shot.startsWith("rosa_") && s.shot.startsWith("rosa_"))).map((s) => (
        <Sound key={`whip${s.start}`} name="whip" at={f(s.start)} volume={0.35} />
      ))}
      {nexts.map((l) => (
        <Sound key={`buzz${l.start}`} name="buzzer" at={f(l.end) - 4} volume={0.45} />
      ))}
      <Sound name="rimshot" at={f(joke.end) + 1} volume={0.6} />
      <Sound name="stamp" at={f(hired.start + (hired.end - hired.start) * 0.62)} volume={0.7} />
      <Sound name="bell" at={f(hired.start + (hired.end - hired.start) * 0.62) + 3} volume={0.5} />
      <Sound name="scratch" at={f(scam.start) - 2} volume={0.6} />
      <Sound name="notify" at={f(scam.start) + 10} volume={0.6} />
      <Sound name="stamp" at={f(stop.start) + 8} volume={0.5} />
      <Sound name="bell" at={f(outro.start)} volume={0.4} />
    </AbsoluteFill>
  );
};
