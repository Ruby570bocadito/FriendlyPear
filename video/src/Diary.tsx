import type { ReactNode } from "react";
import { AbsoluteFill, Audio, Easing, Img, interpolate, Sequence, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import durations from "../public/diary/durations.json";
import lines from "../public/diary/lines.json";
import scenes from "../public/diary/scenes.json";
import visemes from "../public/diary/visemes.json";
import { BODY, clamp, HEADING } from "./theme";
import { img } from "./Minute";
import { AppPhone, BUBBLE, BUTTONS, CALLER, NOTE, NOTE_BEFORE, QR, SCAM_BOX, shown, Zoom } from "./Submission";
import type { Take } from "./Submission";
import { Actor, CHEER, Coins, DROP, Drawn, f, FOCUS, Floaties, FPS, H, INK, musicVolume, peri, PeriMoods, pic, POSES, script, ScriptContext, Slam, Snd, Sparks, SubtitleBand, timing } from "./cast";
import type { Eye, Line, Pose, Shape, Who } from "./cast";

/**
 * "Peri's diary", the second 3-minute submission video (16:9, 30 fps, exactly 3:00). At night Peri writes about her
 * day: the four tricks a crook tried on Rosa, each an entry at its hour (the place in Rosa's day, the people as
 * polaroids that talk, the real app on her phone), how Peri decides, the evening Rosa fell and Ana came, and goodnight.
 * The book opens at the start, its pages turn between the parts and it closes at the end. A camera moves in on what
 * matters and shakes when a trick is stamped out; the places breathe (dust in the light, steam, petals, fireflies) and
 * sound like themselves (scripts/diary_music.py). Pictures by Muse Image (scripts/diary_images.ts, heist_frames.ts,
 * heist_visemes.ts and the app's own), steadied by scripts/heist_stabilize.py; voices and lip sync from
 * scripts/final_voice.py --folder=diary; scene times in public/diary/scenes.json. No words over people: subtitles live
 * in their band, notes on paper beside the photos.
 */
export const DIARY_FRAMES = 180 * FPS;
type Name = keyof typeof scenes;
const AT = scenes as Record<Name, [number, number]>;
const DIARY = script(AT, lines as Record<Name, Line[]>, durations as Record<Name, number[]>, visemes as Record<string, string[]>);
const { say, ends } = timing(DIARY);
const length = (name: Name) => f(AT[name][1] - AT[name][0]);
/** How long a page takes to turn, in frames. */
const TURN = 22;
const PAPER = "#FFFBF0";
const BROWN = "#3B2F2A";
/** A steady pseudo-random number from 0 to 1 for a seed. */
const rand = (seed: number) => {
  const x = Math.sin(seed * 12.9898) * 43758.5453;
  return x - Math.floor(x);
};

const TAKES = {
  // Rosa's phone at home in the morning, before anything happens.
  title: { clip: "m1_newnumber", cuts: [[0.5, 4.0]], rate: 1 },
  // Android draws a blank white screen for two seconds while the call opens (20.4-22.9 s): cut from "I'm all ears" to "Calling...".
  e1: { clip: "m1_newnumber", cuts: [[0.5, 10.6], [19.6, 20.35], [22.9, 29.4], [30.9, 32.5]], rate: 1 },
  e2: { clip: "m3_code", cuts: [[1.2, 9.6], [10.7, 19.5]], rate: 1 },
  e3: { clip: "m4_picture", cuts: [[2.4, 33]], rate: 1 },
  e4: { clip: "m6_family_ana", cuts: [[45.6, 72]], rate: 1 },
  night: { clip: "m5_fallen", cuts: [[3.4, 6.4], [12.6, 14.35], [16.8, 18.9]], rate: 0.99 },
} satisfies Record<string, Take>;
/** On Ana's approval page: "This needs two guardians' yes: yours and one more. A single no stops it." */
const TWO_YES: [number, number, number, number] = [0.02, 0.19, 0.98, 0.33];

// ---------------------------------------------------------------- the diary's own poses

/** A pose whose frames were all made by heist_visemes.ts (heist/v/<pose>_<frame>). */
const drawn = (pose: string, base: string, mouths: Shape[], eyes: Eye[]): Pose => ({
  base,
  mouths: Object.fromEntries(mouths.map((n) => [n, `heist/v/${pose}_${n}`])),
  eyes: Object.fromEntries(eyes.map((n) => [n, `heist/v/${pose}_${n}`])),
  overlay: true,
});
Object.assign(POSES, {
  peri_desk: drawn("peri_desk", "diary/peri_desk", ["slight", "half", "open", "round"], ["blink", "blinkhalf", "lookleft", "lookright"]),
  rosa_fallen_night: drawn("rosa_fallen_night", "diary/rosa_fallen_night", ["slight", "half", "open", "round"], ["blink", "blinkhalf"]),
  rosa_kitchen_hangup: drawn("rosa_kitchen_hangup", "diary/rosa_kitchen_hangup", [], ["blink"]),
  rosa_qr_away: drawn("rosa_qr_away", "diary/rosa_qr_away", [], ["blink"]),
  rosa_garden_smile: drawn("rosa_garden_smile", "diary/rosa_garden_smile", [], ["blink"]),
  peri_worried: peri("worried"),
});
FOCUS.peri_desk = "50% 45%";
/** Where each face is, as fractions of its picture: a polaroid keeps it there and moves in on it. */
const FACES: Record<string, [number, number]> = {
  crook_plan: [0.44, 0.36], crook_desk: [0.49, 0.37], crook_phone: [0.47, 0.36], crook_stickers: [0.43, 0.36], crook_shock: [0.46, 0.36], crook_angry: [0.49, 0.39], crook_quit: [0.49, 0.62],
  rosa_reading: [0.51, 0.36], rosa_yes: [0.51, 0.36], rosa_kitchen: [0.49, 0.42], rosa_kitchen_hangup: [0.48, 0.4], rosa_qr: [0.48, 0.42], rosa_qr_away: [0.57, 0.42],
  rosa_garden_call: [0.51, 0.29], rosa_garden_smile: [0.52, 0.3], rosa_fallen_night: [0.42, 0.48],
  ana_call_rest: [0.53, 0.45], ana_no: [0.58, 0.46], ana_go_rest: [0.52, 0.45],
};
/** How close each polaroid is: tall pictures are already cropped closer. */
const ZOOM: Record<string, number> = { rosa_garden_call: 1.45, rosa_garden_smile: 1.45, crook_quit: 1.5 };

// ---------------------------------------------------------------- camera, light and life

/** A shot: from frame [at], ease over [ease] frames to [zoom] around the point (x, y) of the page. */
type Shot = { at: number; zoom: number; x?: number; y?: number; ease?: number };
/** The camera of a part: moves in on what matters, back out, and shakes for a moment at each of [shakes]. */
const Camera = ({ shots, shakes = [], children }: { shots: Shot[]; shakes?: number[]; children: ReactNode }) => {
  const fr = useCurrentFrame();
  let zoom = 1;
  let x = 960;
  let y = H / 2;
  for (const s of shots) {
    if (fr < s.at) break;
    const k = interpolate(fr, [s.at, s.at + (s.ease ?? 24)], [0, 1], { ...clamp, easing: Easing.inOut(Easing.cubic) });
    zoom += (s.zoom - zoom) * k;
    x += ((s.x ?? 960) - x) * k;
    y += ((s.y ?? H / 2) - y) * k;
  }
  let dx = 0;
  let dy = 0;
  let jolt = 0;
  for (const s of shakes) {
    const t = fr - s;
    if (t >= 0 && t < 10) {
      dx += Math.sin(t * 2.9) * (10 - t) * 1.6;
      dy += Math.cos(t * 3.7) * (10 - t) * 1;
      jolt = Math.max(jolt, 1 - t / 10);
    }
  }
  // While it shakes, it moves in a touch so the page's edges never show.
  return <div style={{ position: "absolute", left: 0, top: 0, width: 1920, height: H, transformOrigin: `${x}px ${y}px`, scale: `${zoom * (1 + 0.03 * jolt)}`, translate: `${dx}px ${dy}px` }}>{children}</div>;
};

/** Where a point of a wide Muse picture (2016 x 1152, as fractions) lands on the page, when it covers it from [focus]. */
const onPicture = (focusY: number, px: number, py: number): [number, number] => {
  const s = Math.max(1920 / 2016, H / 1152);
  return [(1920 - 2016 * s) / 2 + px * 2016 * s, (H - 1152 * s) * focusY + py * 1152 * s];
};

/** Little living things in a place: dust in the light, pollen, petals, leaves, fireflies, twinkling stars. */
type Mote = "dust" | "pollen" | "petals" | "leaves" | "fireflies" | "twinkles";
const Motes = ({ kind, count = 24, x = 0, y = 0, w = 1920, h = H }: { kind: Mote; count?: number; x?: number; y?: number; w?: number; h?: number }) => {
  const fr = useCurrentFrame();
  return (
    <>
      {Array.from({ length: count }, (_, i) => {
        const a = rand(i + 1);
        const b = rand(i + 101);
        const c = rand(i + 201);
        if (kind === "petals" || kind === "leaves") {
          const fall = (b * (h + 60) + fr * (0.7 + c * 0.9)) % (h + 60);
          const size = kind === "petals" ? 12 + c * 10 : 18 + c * 12;
          return (
            <div key={i} style={{ position: "absolute", left: x + a * w + Math.sin((fr + i * 40) / 45) * 50, top: y + fall - 30, width: size, height: size * 0.6, borderRadius: "50% 50% 50% 0", background: kind === "petals" ? ["#F7B6C8", "#FFD0DC", "#F49AB3"][i % 3] : ["#9CC45A", "#D9B44A", "#C98A3D"][i % 3], rotate: `${fr * (2 + c * 3) + i * 50}deg`, opacity: 0.85, boxShadow: "0 2px 3px rgba(0,0,0,0.12)" }} />
          );
        }
        if (kind === "fireflies") {
          const glow = Math.max(0, Math.sin((fr + i * 37) / 11));
          return (
            <div key={i} style={{ position: "absolute", left: x + a * w + Math.sin((fr * 0.6 + i * 50) / 40) * 70, top: y + b * h + Math.cos((fr * 0.5 + i * 30) / 50) * 40, width: 8, height: 8, borderRadius: "50%", background: "#F6FF9C", boxShadow: `0 0 ${10 + glow * 18}px ${4 + glow * 8}px rgba(220,255,120,${0.35 + glow * 0.5})`, opacity: 0.25 + glow * 0.75 }} />
          );
        }
        if (kind === "twinkles") {
          const s = Math.max(0, Math.sin((fr + i * 53) / 9));
          return <div key={i} style={{ position: "absolute", left: x + a * w, top: y + b * h, width: 4, height: 4, borderRadius: "50%", background: "#FFF6D8", boxShadow: `0 0 ${4 + s * 10}px ${1 + s * 3}px rgba(255,246,216,${0.3 + s * 0.6})`, opacity: 0.2 + s * 0.8 }} />;
        }
        // Dust and pollen drift slowly up and sideways, catching the light now and then.
        const rise = (b * h + fr * (0.25 + c * 0.35)) % h;
        const size = kind === "pollen" ? 4 + c * 5 : 3 + c * 4;
        const shine = 0.35 + 0.65 * Math.max(0, Math.sin((fr + i * 29) / 17));
        return (
          <div key={i} style={{ position: "absolute", left: x + a * w + Math.sin((fr + i * 31) / 60) * 36, top: y + h - rise, width: size, height: size, borderRadius: "50%", background: kind === "pollen" ? "#FFF7C2" : "#FFE6B0", boxShadow: `0 0 ${size * 2}px rgba(255,236,180,0.8)`, opacity: shine * 0.8 }} />
        );
      })}
    </>
  );
};

/** Steam curling up from a cup at (x, y). */
const Steam = ({ x, y }: { x: number; y: number }) => {
  const fr = useCurrentFrame();
  return (
    <>
      {Array.from({ length: 9 }, (_, j) => {
        const t = (fr * 1.2 + j * 13) % 110;
        return <div key={j} style={{ position: "absolute", left: x + Math.sin((fr + j * 20) / 18) * 12 + t * 0.2 - (14 + t * 0.3) / 2, top: y - t * 1.5, width: 14 + t * 0.3, height: 22 + t * 0.4, borderRadius: "50%", background: "rgba(255,255,255,0.75)", filter: "blur(7px)", opacity: 0.45 * (1 - t / 110) }} />;
      })}
    </>
  );
};

/** A warm light that flickers or breathes (a lantern, a window, a ringing phone). */
const Glow = ({ x, y, w, h, color, speed = 7, rotate = 0, strength = 0.6 }: { x: number; y: number; w: number; h: number; color: string; speed?: number; rotate?: number; strength?: number }) => {
  const fr = useCurrentFrame();
  const o = strength * (0.75 + 0.25 * Math.sin(fr / speed) * Math.sin(fr / (speed * 1.7) + 1));
  return <div style={{ position: "absolute", left: x - w / 2, top: y - h / 2, width: w, height: h, borderRadius: "50%", background: `radial-gradient(ellipse, ${color} 0%, transparent 70%)`, opacity: o, rotate: `${rotate}deg`, mixBlendMode: "screen" }} />;
};

/** A few birds crossing the sky. */
const Flock = ({ at, y, dur = 240, count = 4 }: { at: number; y: number; dur?: number; count?: number }) => {
  const fr = useCurrentFrame();
  const t = (fr - at) / dur;
  if (t < 0 || t > 1) return null;
  return (
    <svg style={{ position: "absolute", left: 0, top: 0 }} width={1920} height={H}>
      {Array.from({ length: count }, (_, i) => {
        const bx = -120 + t * 2160 - i * 70;
        const by = y + i * 22 + Math.sin((fr + i * 9) / 14) * 8;
        const flap = Math.sin((fr + i * 5) / 2.2) * 9;
        return <path key={i} d={`M ${bx - 16} ${by - flap} Q ${bx - 7} ${by - 4} ${bx} ${by} Q ${bx + 7} ${by - 4} ${bx + 16} ${by - flap}`} stroke="#4A3B2E" strokeWidth={3} fill="none" strokeLinecap="round" opacity={0.7} />;
      })}
    </svg>
  );
};

// ---------------------------------------------------------------- paper, tape and photos

/** A strip of washi tape. */
const Tape = ({ color = "#F7C9D9", rotate = -4, w = 150, left = "50%" }: { color?: string; rotate?: number; w?: number; left?: string }) => (
  <div style={{ position: "absolute", left, top: -20, width: w, height: 42, marginLeft: -w / 2, background: color, opacity: 0.88, rotate: `${rotate}deg`, boxShadow: "0 2px 5px rgba(0,0,0,0.12)", backgroundImage: "repeating-linear-gradient(90deg, rgba(255,255,255,0.28) 0 6px, transparent 6px 14px)", zIndex: 3 }} />
);

/** A notebook page: lined paper with a red margin. */
const Paper = ({ children, margin = true }: { children?: ReactNode; margin?: boolean }) => (
  <AbsoluteFill style={{ height: H, background: "#FBF3E1", overflow: "hidden" }}>
    <AbsoluteFill style={{ backgroundImage: "repeating-linear-gradient(180deg, transparent 0 58px, rgba(110,150,200,0.26) 58px 60px)", backgroundPosition: "0 22px" }} />
    {margin && <div style={{ position: "absolute", left: 150, top: 0, bottom: 0, width: 3, background: "rgba(229,83,61,0.42)" }} />}
    <AbsoluteFill style={{ background: "radial-gradient(circle at 50% 40%, rgba(255,255,255,0.4), rgba(160,110,40,0.13) 90%)" }} />
    {children}
  </AbsoluteFill>
);

/**
 * A place in Rosa's day, filling the page and drifting closer, with its own life laid on it ([children], in page
 * pixels); behind the photos it softens like a shallow focus.
 */
const Place = ({ src, focus = "50% 50%", soft = f(1.6), dim = true, name, children }: { src: string; focus?: string; soft?: number; dim?: boolean; name: Name; children?: ReactNode }) => {
  const fr = useCurrentFrame();
  const k = interpolate(fr, [0, length(name)], [1.0, 1.08]);
  const blur = interpolate(fr, [soft, soft + 14], [0, 4], clamp);
  return (
    <AbsoluteFill style={{ height: H, overflow: "hidden" }}>
      <div style={{ position: "absolute", inset: 0, scale: `${k}`, filter: blur ? `blur(${blur}px)${dim ? ` brightness(${1 - blur * 0.03})` : ""}` : undefined }}>
        <Img src={pic(src)} style={{ width: "100%", height: "100%", objectFit: "cover", objectPosition: focus }} />
        {children}
      </div>
    </AbsoluteFill>
  );
};

/** A paper note sliding in with a squash, and away again at [until]. */
const Note = ({ at, until, x, y, w, rotate = -2, tape, children }: { at: number; until?: number; x: number; y: number; w?: number; rotate?: number; tape?: string; children: ReactNode }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  if (fr < at || (until !== undefined && fr > until + 10)) return null;
  const p = spring({ frame: fr - at, fps, config: { damping: 13, stiffness: 160 } });
  const out = until === undefined ? 0 : interpolate(fr, [until, until + 10], [0, 1], clamp);
  return (
    <div style={{ position: "absolute", left: x, top: y, width: w, padding: "22px 30px", background: PAPER, borderRadius: 6, boxShadow: "0 14px 28px rgba(40,20,0,0.3)", rotate: `${rotate + (1 - p) * 10}deg`, scale: `${interpolate(p, [0, 1], [1.4, 1])}`, opacity: Math.min(1, p * 2.5) * (1 - out), translate: `0 ${out * 40}px`, zIndex: 40 }}>
      {tape && <Tape color={tape} />}
      {children}
    </div>
  );
};

/** A little clock whose hands run from one time of day to the next (minutes since midnight). */
const Clock = ({ from, to, at, size = 150 }: { from: number; to: number; at: number; size?: number }) => {
  const fr = useCurrentFrame();
  const t = interpolate(fr, [at, at + 32], [from, to], { ...clamp, easing: Easing.out(Easing.cubic) });
  const hand = (deg: number, long: number, thick: number, color: string) => (
    <div style={{ position: "absolute", left: size / 2 - thick / 2, top: size / 2 - long, width: thick, height: long, borderRadius: thick, background: color, transformOrigin: "50% 100%", rotate: `${deg}deg` }} />
  );
  return (
    <div style={{ position: "relative", width: size, height: size, borderRadius: "50%", background: "#FFFDF7", border: `${size * 0.055}px solid ${BROWN}`, boxShadow: "inset 0 -6px 0 rgba(0,0,0,0.06)", flex: "none" }}>
      {Array.from({ length: 12 }, (_, i) => (
        <div key={i} style={{ position: "absolute", left: size / 2 - 3, top: size * 0.06, width: 6, height: i % 3 ? size * 0.06 : size * 0.11, borderRadius: 3, background: BROWN, transformOrigin: `3px ${size / 2 - size * 0.06}px`, rotate: `${i * 30}deg` }} />
      ))}
      {hand(((t / 60) % 12) * 30, size * 0.26, 9, BROWN)}
      {hand((t % 60) * 6, size * 0.38, 6, "#C0392B")}
      <div style={{ position: "absolute", left: size / 2 - 8, top: size / 2 - 8, width: 16, height: 16, borderRadius: 8, background: BROWN }} />
    </div>
  );
};

/** The hour and what happened, with the clock running to it: big at first, then pinned in the top corner. */
const TimeCard = ({ time, title, ink, from, to }: { time: string; title: string; ink: string; from: number; to: number }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const pop = spring({ frame: fr - 6, fps, config: { damping: 12, stiffness: 150 } });
  const go = interpolate(fr, [44, 62], [0, 1], { ...clamp, easing: Easing.inOut(Easing.cubic) });
  return (
    <>
      <div style={{ position: "absolute", left: interpolate(go, [0, 1], [960, 330]), top: interpolate(go, [0, 1], [450, 78]), translate: "-50% -50%", scale: `${interpolate(go, [0, 1], [1, 0.46]) * interpolate(pop, [0, 1], [1.5, 1])}`, opacity: Math.min(1, pop * 2), rotate: `${interpolate(go, [0, 1], [-2, -3])}deg`, padding: "28px 56px 30px 36px", background: PAPER, borderRadius: 8, boxShadow: "0 18px 36px rgba(40,20,0,0.35)", whiteSpace: "nowrap", zIndex: 60, display: "flex", alignItems: "center", gap: 34 }}>
        <Tape color="#FFE07A" />
        <Clock from={from} to={to} at={8} />
        <div>
          <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 124, lineHeight: 1, color: ink }}>{time}</div>
          <div style={{ fontFamily: HEADING, fontWeight: 600, fontSize: 54, color: BROWN, marginTop: 8 }}>{title}</div>
        </div>
      </div>
      <Snd src="diary/sfx/clock" at={8} volume={0.45} />
    </>
  );
};

const NAME: Record<Who, string> = { crook: "The crook", peri: "Peri", rosa: "Rosa", ana: "Ana" };
const TAPES = ["#F7C9D9", "#BDE68A", "#B4E2FF", "#FFE07A", "#E3D3FF"];

/** One polaroid: a person (alive, see Actor) or Peri on a soft green, with their name (or [caption]) underneath. */
const Polaroid = ({ scene, pose, who, k, caption }: { scene: Name; pose: string; who: Who; k: number; caption?: string }) => {
  const isPeri = who === "peri";
  return (
    <>
      <div style={{ position: "absolute", left: 22, top: 22, width: 496, height: 500, overflow: "hidden", borderRadius: 2, background: isPeri ? "radial-gradient(circle at 50% 35%, #FFFDF2, #D9EFC0 78%)" : "#2B2430" }}>
        <Actor pose={pose} who={who} scene={scene} x={0} y={isPeri ? 30 : 0} w={496} h={isPeri ? 470 : 500} contain={isPeri} enter={-999} rise={false} face={FACES[pose]} zoom={isPeri ? 1 : ZOOM[pose] ?? 1.7} />
      </div>
      <div style={{ position: "absolute", left: 0, right: 0, bottom: 30, textAlign: "center", fontFamily: HEADING, fontWeight: 600, fontSize: caption ? 50 : 44, color: BROWN, whiteSpace: "nowrap" }}>{caption ?? NAME[who]}</div>
      <Tape color={TAPES[k % TAPES.length]} rotate={k % 2 ? 4 : -5} />
    </>
  );
};

/**
 * The entry's photos, dealt onto a little pile on the left as the story goes (each with a papery flick): whoever is
 * speaking (or Rosa's face as it ends), the newest on top, the two before it peeking out underneath.
 */
type Snap = { at: number; pose: string; who: Who };
const Album = ({ scene, snaps, until }: { scene: Name; snaps: Snap[]; until?: number }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const gone = until === undefined ? 0 : interpolate(fr, [until, until + 12], [0, 1], { ...clamp, easing: Easing.in(Easing.cubic) });
  const dealt = snaps.filter((s) => fr >= s.at);
  const pile = dealt.slice(-3);
  return (
    <>
      {gone < 1 && (
        <div style={{ position: "absolute", inset: 0, translate: `${gone * -900}px 0` }}>
          {pile.map((s, i) => {
            const k = dealt.length - pile.length + i;
            const depth = pile.length - 1 - i;
            const p = spring({ frame: fr - s.at, fps, config: { damping: 14, stiffness: 120 } });
            const tilt = [-4, 3, -2, 4, -3, 2, -5, 3][k % 8];
            const nudge = [0, 18, -14, 22, -18, 10, -22, 14][k % 8];
            return (
              <div key={s.at} style={{ position: "absolute", left: 70 + nudge - depth * 26, top: 200 - depth * 8, width: 540, height: 640, background: "#FFFDF7", borderRadius: 5, boxShadow: `0 ${20 + (1 - p) * 30}px ${36 + (1 - p) * 30}px rgba(40,20,0,0.38)`, rotate: `${tilt - depth * 3 + (1 - p) * 16}deg`, translate: `${(1 - p) * -300}px ${(1 - p) * 560}px`, scale: `${1 - depth * 0.04 + (1 - p) * 0.06}`, zIndex: 10 + k }}>
                <Polaroid scene={scene} pose={s.pose} who={s.who} k={k} />
                {depth > 0 && <div style={{ position: "absolute", inset: 0, borderRadius: 5, background: `rgba(60,40,20,${depth * 0.1})` }} />}
              </div>
            );
          })}
        </div>
      )}
      {snaps.map((s) => (
        <Snd key={s.at} src="diary/sfx/flick" at={s.at} volume={0.5} />
      ))}
    </>
  );
};

/** One photo pinned on its own, sliding in at [at] (and out to the right at [until]); [scale] shrinks it. */
const Pinned = ({ scene, pose, who, at, until, x, y, tilt = 4, scale = 1, caption, k = 3 }: { scene: Name; pose: string; who: Who; at: number; until?: number; x: number; y: number; tilt?: number; scale?: number; caption?: string; k?: number }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  if (fr < at) return null;
  const p = spring({ frame: fr - at, fps, config: { damping: 13, stiffness: 130 } });
  const out = until === undefined ? 0 : interpolate(fr, [until, until + 12], [0, 1], { ...clamp, easing: Easing.in(Easing.cubic) });
  if (out >= 1) return null;
  return (
    <div style={{ position: "absolute", left: x, top: y, width: 540, height: 640, background: "#FFFDF7", borderRadius: 5, boxShadow: "0 20px 36px rgba(40,20,0,0.38)", rotate: `${tilt + (1 - p) * 14 + out * 20}deg`, translate: `${(1 - p) * 400 + out * 900}px ${(1 - p) * 300}px`, scale: `${scale * (1 + (1 - p) * 0.08)}`, transformOrigin: "0% 0%" }}>
      <Polaroid scene={scene} pose={pose} who={who} k={k} caption={caption} />
    </div>
  );
};

/** A round sticker slapped onto a photo's corner. */
const Sticker = ({ at, x, y, text, color = "#2E7D32" }: { at: number; x: number; y: number; text: string; color?: string }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  if (fr < at) return null;
  const p = spring({ frame: fr - at, fps, config: { damping: 9, stiffness: 260 } });
  return (
    <div style={{ position: "absolute", left: x - 62, top: y - 62, width: 124, height: 124, borderRadius: "50%", background: color, border: "6px solid #FFFDF7", boxShadow: "0 8px 14px rgba(0,0,0,0.3)", display: "grid", placeItems: "center", fontFamily: HEADING, fontWeight: 700, fontSize: 46, color: "#FFFDF7", rotate: `${-12 + (1 - p) * 40}deg`, scale: `${interpolate(p, [0, 1], [2.2, 1])}`, opacity: Math.min(1, p * 3), zIndex: 50 }}>
      {text}
    </div>
  );
};

const PhoneLabel = ({ text }: { text: string }) => (
  <div style={{ position: "absolute", right: 26, top: 14, whiteSpace: "nowrap", padding: "6px 18px", borderRadius: 999, background: "rgba(31,42,26,0.85)", color: "#FFF8EC", fontFamily: BODY, fontWeight: 700, fontSize: 22, letterSpacing: 3, zIndex: 30 }}>{text} · REAL APP</div>
);
const PHONE = { x: 1640, y: 492, h: 860 };

/** The phone's part of an entry, sliding away at its end (the Sequence is [length] long). */
const Leaving = ({ length: n, children }: { length: number; children: ReactNode }) => {
  const fr = useCurrentFrame();
  const out = interpolate(fr, [n - 10, n], [0, 1], { ...clamp, easing: Easing.in(Easing.cubic) });
  return <AbsoluteFill style={{ height: H, translate: `0 ${out * 1100}px`, rotate: `${out * 5}deg` }}>{children}</AbsoluteFill>;
};

/** The label over the phone, outside the camera so it never drifts off the page. */
const Label = ({ from, to, text }: { from: number; to: number; text: string }) => (
  <Sequence from={from} durationInFrames={to - from} layout="none">
    <Leaving length={to - from}>
      <PhoneLabel text={text} />
    </Leaving>
  </Sequence>
);

/** When a trick is beaten: a "STOPPED" stamp on a page, the tally, and Peri jumping for joy as she does in the app. */
const Victory = ({ at, n }: { at: number; n: number }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  if (fr < at) return null;
  const card = spring({ frame: fr - at, fps, config: { damping: 13, stiffness: 150 } });
  const stamp = spring({ frame: fr - at - 10, fps, config: { damping: 8, stiffness: 260 } });
  const ring = interpolate(fr - at - 10, [0, 14], [0, 1], clamp);
  return (
    <>
      <div style={{ position: "absolute", left: 700, top: 240, width: 520, padding: "44px 30px 34px", background: PAPER, borderRadius: 6, boxShadow: "0 18px 34px rgba(40,20,0,0.35)", rotate: `${-5 + (1 - card) * 18}deg`, translate: `0 ${(1 - card) * 700}px`, textAlign: "center", zIndex: 45 }}>
        <Tape color="#BDE68A" />
        <div style={{ position: "relative", display: "inline-block" }}>
          {fr >= at + 10 && ring < 1 && <div style={{ position: "absolute", left: "50%", top: "50%", width: 420, height: 160, marginLeft: -210, marginTop: -80, borderRadius: 30, border: "6px solid #2E7D32", opacity: 0.6 * (1 - ring), scale: `${1 + ring * 0.5}`, rotate: "-8deg" }} />}
          <div style={{ padding: "8px 26px", border: "8px solid #2E7D32", borderRadius: 18, color: "#2E7D32", fontFamily: HEADING, fontWeight: 700, fontSize: 88, letterSpacing: 4, rotate: "-8deg", scale: `${fr >= at + 10 ? interpolate(stamp, [0, 1], [2.4, 1]) : 0}`, opacity: Math.min(1, stamp * 2) * 0.92 }}>STOPPED</div>
        </div>
        <div style={{ marginTop: 24, fontFamily: HEADING, fontWeight: 600, fontSize: 40, color: INK }}>Trick {n} of 4 · $0 lost</div>
      </div>
      <Drawn keys={CHEER} at={at + 8} x={1540} y={930} size={520} />
      <Sparks at={at + 8 + f(0.76)} x={1540} y={680} kinds={["fx_star", "fx_star_green", "fx_confetti_dot", "fx_confetti_strip"]} count={12} />
      <Snd src="reel/sfx/stamp" at={at + 10} volume={0.6} />
      <Snd src="short/sfx/score" at={at + f(0.9)} volume={0.45} />
    </>
  );
};

// ---------------------------------------------------------------- the parts

/** The cover of Peri's diary, close up: [turn] from 0 (shut) to 1 (opened, turned away). */
const CoverFace = ({ zoom, turn, credits = 0 }: { zoom: number; turn: number; credits?: number }) => (
  <AbsoluteFill style={{ height: H, perspective: 2600, zIndex: 80 }}>
    <AbsoluteFill style={{ transformOrigin: "0% 50%", transform: turn > 0 ? `rotateY(${turn * 90}deg)` : undefined, overflow: "hidden" }}>
      <div style={{ position: "absolute", inset: 0, scale: `${zoom}`, transformOrigin: "48.5% 46%" }}>
        <Img src={pic("diary/cover")} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
        <div style={{ position: "absolute", left: 0, right: 40, top: 560, textAlign: "center", fontFamily: HEADING, fontWeight: 700, fontSize: 62, color: "#FFF4D6", textShadow: "0 3px 0 rgba(30,60,20,0.55), 0 0 10px rgba(255,230,160,0.35)", letterSpacing: 1 }}>Peri's Diary</div>
        {credits > 0 && (
          <div style={{ position: "absolute", left: 0, right: 0, top: 890, textAlign: "center", opacity: credits }}>
            <span style={{ padding: "8px 22px", borderRadius: 999, background: "rgba(37,84,26,0.92)", color: "#FFF8EC", fontFamily: BODY, fontWeight: 700, fontSize: 30 }}>FriendlyPear · github.com/Ruby570bocadito/FriendlyPear</span>
          </div>
        )}
      </div>
      <AbsoluteFill style={{ background: `linear-gradient(90deg, rgba(255,245,220,${0.18 * turn}) 0%, rgba(0,0,0,0) 40%, rgba(40,20,0,${turn * 0.55}) 100%)` }} />
    </AbsoluteFill>
  </AbsoluteFill>
);

/** The diary opening at the very start: close in on the cover, then it swings open. */
const Cover = () => {
  const fr = useCurrentFrame();
  if (fr >= 46) return null;
  const zoom = interpolate(fr, [6, 26], [1, 2.25], { ...clamp, easing: Easing.inOut(Easing.cubic) });
  const turn = interpolate(fr, [25, 46], [0, 1], { ...clamp, easing: Easing.inOut(Easing.cubic) });
  return <CoverFace zoom={zoom} turn={turn} />;
};

/** The four tricks of the day, as photos of the crook, each with its name; then "$0" on every one. */
const HOOK = [
  { pose: "crook_desk", caption: "Fake daughter", x: 1000, y: 140, tilt: -4 },
  { pose: "crook_phone", caption: "Fake bank", x: 1330, y: 160, tilt: 3 },
  { pose: "crook_stickers", caption: "Lying sticker", x: 1020, y: 470, tilt: 2 },
  { pose: "crook_plan", caption: "Keep her talking", x: 1350, y: 490, tilt: -3 },
];

/**
 * 0:00 Night. Peri opens her diary at her desk; a crook tried to rob Rosa four times (four photos land, one for each
 * trick), and got nothing ($0 on every one).
 */
const Open = () => {
  const fr = useCurrentFrame();
  const four = say("open", 1, "Four") - 4;
  const cent = say("open", 2) + 2;
  return (
    <>
      <Camera shots={[{ at: 0, zoom: 1 }, { at: 44, zoom: 1.06, x: 640, y: 560, ease: 110 }, { at: say("open", 1) - 10, zoom: 1, ease: 30 }]} shakes={HOOK.map((_, i) => cent + i * 6 + 2)}>
        <Actor pose="peri_desk" who="peri" scene="open" x={0} y={0} w={1920} h={H} enter={-999} rise={false} />
        <Motes kind="dust" count={22} x={980} y={60} w={900} h={760} />
        <Glow x={1640} y={430} w={520} h={420} color="rgba(255,200,120,0.55)" speed={9} strength={0.5} />
        <Note at={f(1.6)} x={990} y={22} w={320} rotate={-3} tape="#E3D3FF">
          <div style={{ fontFamily: HEADING, fontWeight: 600, fontSize: 38, color: BROWN, textAlign: "center" }}>Thursday, late</div>
        </Note>
        <Pinned scene="open" pose="crook_plan" who="crook" at={say("open", 1) - 6} until={four - 6} x={1290} y={190} tilt={5} scale={0.9} />
        {HOOK.map((h, i) => (
          <Pinned key={h.pose} scene="open" pose={h.pose} who="crook" at={four + i * 5} x={h.x} y={h.y} tilt={h.tilt} scale={0.5} caption={h.caption} k={i} />
        ))}
        {HOOK.map((h, i) => (
          <Sticker key={h.pose} at={cent + i * 6} x={h.x + 262} y={h.y + 14} text="$0" />
        ))}
      </Camera>
      {fr < 46 && <Cover />}
      <Snd src="diary/sfx/page" at={26} volume={0.6} />
      <Snd src="diary/sfx/pencil" at={f(1.1)} volume={0.35} />
      <Snd src="diary/sfx/flick" at={say("open", 1) - 6} volume={0.5} />
      {HOOK.map((_, i) => (
        <Snd key={`f${i}`} src="diary/sfx/flick" at={four + i * 5} volume={0.5} />
      ))}
      {HOOK.map((_, i) => (
        <Snd key={`s${i}`} src="reel/sfx/stamp" at={cent + i * 6} volume={0.35} />
      ))}
    </>
  );
};

/** Where Peri stands on her hill, in the title's picture. */
const HILL = { x: 1090, y: 600, size: 560 };

/** 0:11 Morning on Peri's hill: she drops in as she does when the app opens, shows where she lives, and says what she does. */
const Title = () => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const dropAt = 22;
  const landed = dropAt + f(1.08) + 2;
  const logo = spring({ frame: fr - 26, fps, config: { damping: 10, stiffness: 150 } });
  const phoneFrom = say("title", 0) + 4;
  const phoneTo = say("title", 1) + 4;
  return (
    <>
      <Camera shots={[{ at: 0, zoom: 1 }, { at: landed, zoom: 1.04, x: HILL.x, y: HILL.y - 220, ease: 160 }]}>
        <Place src="diary/hill_morning" focus="50% 20%" soft={99999} name="title">
          <Motes kind="pollen" count={26} />
        </Place>
        <Flock at={4} y={120} dur={260} />
        <Drawn keys={DROP} at={dropAt} until={landed} x={HILL.x} y={HILL.y} size={HILL.size} />
        {fr >= landed && (
          <PeriMoods scene="title" parts={[{ from: landed, pose: "peri_happy" }, { from: say("title", 1) - 3, pose: "peri_protective" }, { from: say("title", 1, "anything risky") - 3, pose: "peri_calling" }]} x={HILL.x - HILL.size / 2} y={HILL.y - HILL.size} w={HILL.size} h={HILL.size} enter={-30} />
        )}
        <Sparks at={dropAt + f(0.5)} x={HILL.x} y={HILL.y - 40} kinds={["fx_star", "fx_star_green"]} count={8} spread={300} />
        <div style={{ position: "absolute", left: 70, top: 64, display: "flex", alignItems: "center", gap: 20, scale: `${interpolate(logo, [0, 1], [2, 1])}`, opacity: Math.min(1, logo * 2), transformOrigin: "0% 50%" }}>
          <Img src={img("icon")} style={{ width: 120, height: 120, borderRadius: "50%", boxShadow: "0 8px 0 rgba(0,0,0,0.12)" }} />
          <span style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 104, color: INK, textShadow: "0 7px 0 rgba(255,255,255,0.7)" }}>FriendlyPear</span>
        </div>
        <Sequence from={phoneFrom} durationInFrames={phoneTo - phoneFrom} layout="none">
          <Leaving length={phoneTo - phoneFrom}>
            <AppPhone take={TAKES.title} x={1660} y={520} h={760} tilt={4} />
          </Leaving>
        </Sequence>
        <Note at={say("title", 1) + 2} x={90} y={290} w={600} rotate={-3} tape="#BDE68A">
          <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 46, color: "#25541A", lineHeight: 1.1 }}>Stops scams before money moves</div>
        </Note>
        <Note at={say("title", 1, "anything risky")} x={110} y={480} w={600} rotate={2} tape="#B4E2FF">
          <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 46, color: "#1F4E79", lineHeight: 1.1 }}>Anything risky needs the family's yes</div>
        </Note>
      </Camera>
      <Label from={phoneFrom} to={phoneTo} text="ROSA'S PHONE" />
      <Snd src="diary/sfx/twinkle" at={dropAt} volume={0.4} />
    </>
  );
};

/** 0:22 What tricks like his cost, written on a page, with the money falling away. */
const Stakes = () => {
  const fr = useCurrentFrame();
  const n = interpolate(fr, [10, 54], [0, 7.7], { ...clamp, easing: Easing.out(Easing.cubic) });
  const swipe = interpolate(fr, [54, 66], [0, 1], clamp);
  return (
    <Camera shots={[{ at: 0, zoom: 1 }, { at: 8, zoom: 1.05, x: 760, y: 420, ease: 150 }]} shakes={[54]}>
      <Paper>
        <Coins count={14} />
        <div style={{ position: "absolute", left: 220, top: 190, width: 1260 }}>
          <div style={{ fontFamily: HEADING, fontWeight: 600, fontSize: 52, color: BROWN }}>Lost to fraud by Americans over 60, in 2025 alone:</div>
          <div style={{ position: "relative", display: "inline-block", marginTop: 26 }}>
            <div style={{ position: "absolute", left: -20, right: -20, top: "40%", height: "46%", background: "#FFE07A", opacity: 0.85, transformOrigin: "0% 50%", scale: `${swipe} 1`, rotate: "-1.5deg", borderRadius: 12 }} />
            <div style={{ position: "relative", fontFamily: HEADING, fontWeight: 700, fontSize: 176, lineHeight: 1, color: "#25541A", whiteSpace: "nowrap" }}>${n.toFixed(1)} billion</div>
          </div>
          <div style={{ fontFamily: BODY, fontSize: 28, color: "rgba(59,47,42,0.7)", marginTop: 30 }}>Source: FBI Internet Crime Complaint Center (IC3), 2025</div>
        </div>
        <Actor pose="peri_worried" who="peri" scene="stakes" x={1460} y={470} w={460} h={500} contain enter={4} />
        <Snd src="submission/sfx/ticks" at={10} volume={0.3} />
        <Snd src="final/sfx/slam" at={54} volume={0.6} />
      </Paper>
    </Camera>
  );
};

/** 0:29 8:42 a.m. A text from a "new number" that says it's Ana. */
const E1 = () => {
  const T = TAKES.e1;
  const phoneFrom = f(1.8);
  const at = (s: number) => phoneFrom + shown(T, s);
  const result = say("e1", 4) - 6;
  const [px, py] = onPicture(0.6, 0.5, 0.5);
  return (
    <>
      <Camera shots={[{ at: 0, zoom: 1 }, { at: say("e1", 1) - 10, zoom: 1.05, x: 1250, y: 520, ease: 30 }, { at: say("e1", 2) - 10, zoom: 1.02, x: 700, y: 500 }, { at: say("e1", 3) - 10, zoom: 1.05, x: 1100, y: 560 }, { at: result - 4, zoom: 1, ease: 16 }]} shakes={[result + 10]}>
        <Place src="diary/town_morning" focus="50% 60%" name="e1">
          <Motes kind="pollen" count={22} />
          <Glow x={px} y={py - 120} w={900} h={500} color="rgba(255,236,170,0.5)" speed={12} strength={0.35} />
        </Place>
        <Flock at={0} y={90} dur={200} count={3} />
        <Album
          scene="e1"
          snaps={[
            { at: say("e1", 0) - 8, pose: "crook_desk", who: "crook" },
            { at: say("e1", 1) - 8, pose: "peri_worried", who: "peri" },
            { at: say("e1", 2) - 8, pose: "rosa_yes", who: "rosa" },
            { at: say("e1", 3) - 10, pose: "ana_call_rest", who: "ana" },
            { at: result, pose: "crook_shock", who: "crook" },
            { at: ends("e1", 4) + 8, pose: "rosa_yes", who: "rosa" },
          ]}
        />
        <Sequence from={phoneFrom} durationInFrames={result - phoneFrom} layout="none">
          <Leaving length={result - phoneFrom}>
            <AppPhone take={T} x={PHONE.x} y={PHONE.y} h={PHONE.h} tilt={3} marks={[{ region: BUBBLE, from: shown(T, 4.9), to: shown(T, 10.6) }, { region: CALLER, from: shown(T, 23.6), to: shown(T, 28.4), color: "#6EC6FF" }]} />
          </Leaving>
          <Zoom take={T} region={BUBBLE} x={800} y={430} w={600} from={shown(T, 4.9)} to={shown(T, 10.6) - 2} label="Peri, at once" />
          <Zoom take={T} region={CALLER} x={820} y={470} w={560} from={shown(T, 23.6)} to={shown(T, 28.4)} label="Calling the real Ana" color="#6EC6FF" />
        </Sequence>
        <Note at={ends("e1", 2)} until={result - 2} x={720} y={150} w={560} rotate={-3} tape="#B4E2FF">
          <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 48, color: "#1F4E79", lineHeight: 1.08 }}>One “yes” calls the real Ana</div>
        </Note>
        <Victory at={result} n={1} />
      </Camera>
      <Label from={phoneFrom} to={result} text="ROSA'S PHONE" />
      <TimeCard time="8:42 a.m." title="A text from a new number" ink="#1F6FA8" from={6 * 60} to={8 * 60 + 42} />
      <Snd src="short/sfx/typing" at={say("e1", 0) + 10} volume={0.4} />
      <Snd src="short/sfx/send" at={at(4.1)} volume={0.4} />
      <Snd src="short/sfx/ding" at={at(4.3)} volume={0.45} />
      <Snd src="submission/sfx/listen" at={at(19.7)} volume={0.4} />
      <Snd src="submission/sfx/ringback" at={at(23.3)} volume={0.28} length={shown(T, 25.0) - shown(T, 23.3)} />
      <Snd src="submission/sfx/answer" at={at(25.0)} volume={0.4} />
    </>
  );
};

/** 0:49 11:05 a.m. A call from "the bank", and a code during the call. */
const E2 = () => {
  const T = TAKES.e2;
  const phoneFrom = f(1.4);
  const at = (s: number) => phoneFrom + shown(T, s);
  const fire = at(13.0);
  const result = say("e2", 4) - 6;
  const [cx, cy] = onPicture(0.6, 0.23, 0.67);
  const [gx, gy] = onPicture(0.6, 0.61, 0.815);
  return (
    <>
      <Camera shots={[{ at: 0, zoom: 1 }, { at: say("e2", 1) - 10, zoom: 1.04, x: 1200, y: 520, ease: 30 }, { at: say("e2", 2) - 10, zoom: 1.02, x: 700, y: 500 }, { at: fire - 2, zoom: 1.09, x: 1250, y: 420, ease: 8 }, { at: result - 4, zoom: 1, ease: 16 }]} shakes={[fire + 2, result + 10]}>
        <Place src="diary/kitchen_noon" focus="50% 60%" name="e2">
          <Steam x={cx} y={cy} />
          <Glow x={gx} y={gy} w={420} h={190} color="rgba(120,240,160,0.95)" speed={3} rotate={-10} strength={0.9} />
          <Motes kind="dust" count={18} x={0} y={0} w={1300} h={600} />
        </Place>
        <Album
          scene="e2"
          snaps={[
            { at: say("e2", 0) - 8, pose: "crook_phone", who: "crook" },
            { at: ends("e2", 0) + 6, pose: "rosa_kitchen", who: "rosa" },
            { at: say("e2", 1) - 8, pose: "peri_thinking", who: "peri" },
            { at: say("e2", 2) - 8, pose: "crook_phone", who: "crook" },
            { at: say("e2", 3) - 8, pose: "peri_protective", who: "peri" },
            { at: result, pose: "crook_angry", who: "crook" },
            { at: ends("e2", 4) + 8, pose: "rosa_kitchen_hangup", who: "rosa" },
          ]}
        />
        <Sequence from={phoneFrom} durationInFrames={result - phoneFrom} layout="none">
          <Leaving length={result - phoneFrom}>
            <AppPhone take={T} x={PHONE.x} y={PHONE.y} h={PHONE.h} tilt={-3} marks={[{ region: BUBBLE, from: shown(T, 4.6), to: shown(T, 9.6) }, { region: BUBBLE, from: shown(T, 13.2), to: result - phoneFrom, color: "#FFE07A" }]} />
          </Leaving>
          <Zoom take={T} region={BUBBLE} x={800} y={430} w={600} from={shown(T, 4.6)} to={shown(T, 9.6) - 2} label="Before Rosa answers" />
          <Zoom take={T} region={BUBBLE} x={800} y={430} w={600} from={shown(T, 13.2)} to={result - phoneFrom} label="A code arrived during the call" color="#FFE07A" />
        </Sequence>
        <Slam text="HANG UP." at={fire} until={result - 2} x={760} y={90} size={130} color="#FFF8EC" shade="#8E1F18" rotate={-5} />
        <Note at={say("e2", 3, "A code")} until={result - 2} x={780} y={250} w={470} rotate={2} tape="#F7C9D9">
          <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 38, color: "#8E1F18", lineHeight: 1.1 }}>Hard rule: no AI can overrule it</div>
        </Note>
        <Victory at={result} n={2} />
      </Camera>
      <Label from={phoneFrom} to={result} text="ROSA'S PHONE" />
      <TimeCard time="11:05 a.m." title="A call from “the bank”" ink="#C2412D" from={8 * 60 + 42} to={11 * 60 + 5} />
      <Snd src="submission/sfx/ring" at={6} volume={0.25} length={phoneFrom - 6} />
      <Snd src="submission/sfx/ring" at={at(3.8)} volume={0.35} length={shown(T, 7.8) - shown(T, 3.8)} />
      <Snd src="submission/sfx/answer" at={at(7.8)} volume={0.4} />
      <Snd src="short/sfx/ding" at={at(12.8)} volume={0.45} />
      <Snd src="submission/sfx/alert" at={fire} volume={0.6} />
      <Snd src="short/sfx/boing" at={say("e2", 4, "PEAR")} volume={0.55} />
    </>
  );
};

/** 1:09 3:30 p.m. A QR sticker on a parking meter, with a hidden note telling the AI it's safe. */
const E3 = () => {
  const T = TAKES.e3;
  const phoneFrom = f(1.6);
  const verdict = shown(T, 9.05);
  const result = say("e3", 3) - 8;
  return (
    <>
      <Camera shots={[{ at: 0, zoom: 1 }, { at: phoneFrom + shown(T, 4.2) - 6, zoom: 1.05, x: 1150, y: 520, ease: 30 }, { at: say("e3", 1) - 6, zoom: 1.08, x: 1200, y: 420, ease: 10 }, { at: say("e3", 2) - 6, zoom: 1.03, x: 1100, y: 600 }, { at: result - 4, zoom: 1, ease: 16 }]} shakes={[say("e3", 1, "A note"), result + 10]}>
        <Place src="diary/street_afternoon" focus="50% 55%" name="e3">
          <Motes kind="leaves" count={9} x={900} y={0} w={1000} h={H} />
          <Motes kind="dust" count={16} />
        </Place>
        <Album
          scene="e3"
          snaps={[
            { at: say("e3", 0) - 8, pose: "crook_stickers", who: "crook" },
            { at: ends("e3", 0) + 6, pose: "rosa_qr", who: "rosa" },
            { at: say("e3", 1) - 8, pose: "peri_protective", who: "peri" },
            { at: say("e3", 2) - 8, pose: "peri_thinking", who: "peri" },
            { at: result, pose: "crook_shock", who: "crook" },
            { at: ends("e3", 3) + 8, pose: "rosa_qr_away", who: "rosa" },
          ]}
        />
        <Sequence from={phoneFrom} durationInFrames={result - phoneFrom} layout="none">
          <Leaving length={result - phoneFrom}>
            <AppPhone take={T} x={PHONE.x} y={PHONE.y} h={PHONE.h} tilt={3} marks={[{ region: NOTE_BEFORE, from: shown(T, 4.2), to: verdict }, { region: QR, from: verdict, to: result - phoneFrom, color: "#FF8A7A" }]} />
          </Leaving>
          <Zoom take={T} region={NOTE_BEFORE} x={790} y={440} w={620} from={shown(T, 4.2)} to={verdict - 9} label="Hidden in the sticker, for the AI" />
          <Zoom take={T} region={QR} x={800} y={430} w={340} from={verdict + 6} to={result - phoneFrom} label="SAM 3.1 circles it" color="#FF8A7A" />
          <Zoom take={T} region={NOTE} x={800} y={800} w={560} from={verdict + 6} to={result - phoneFrom} label="...and Peri ignores it" />
        </Sequence>
        <Slam text="NOT FOOLED." at={say("e3", 1, "A note")} until={result - 2} x={760} y={90} size={100} color="#C0392B" shade="rgba(255,255,255,0.75)" rotate={-6} />
        <Note at={say("e3", 2)} until={result - 2} x={1000} y={250} w={420} rotate={2} tape="#FFE07A">
          <div style={{ fontFamily: HEADING, fontWeight: 600, fontSize: 32, color: INK, lineHeight: 1.15 }}>Muse Spark reads the picture. The phone has the last word.</div>
        </Note>
        <Victory at={result} n={3} />
      </Camera>
      <Label from={phoneFrom} to={result} text="ROSA'S PHONE" />
      <TimeCard time="3:30 p.m." title="A sticker with a secret" ink="#B07400" from={11 * 60 + 5} to={15 * 60 + 30} />
      <Snd src="submission/sfx/shutter" at={phoneFrom + shown(T, 3.7)} volume={0.45} />
      <Snd src="submission/sfx/alert" at={phoneFrom + verdict} volume={0.45} />
      <Snd src="reel/sfx/stamp" at={say("e3", 1, "A note")} volume={0.55} />
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

const FamilyKey = ({ from, until }: { from: number; until: number }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  if (fr < from || fr >= until + 14) return null;
  const k = fr - from;
  const slide = interpolate(fr, [from, from + 12], [-1, 0], { ...clamp, easing: Easing.out(Easing.cubic) }) + interpolate(fr, [until, until + 14], [0, -1], { ...clamp, easing: Easing.in(Easing.cubic) });
  const asked = say("e4", 4) - from;
  const no = say("e4", 5, "A single no") - from;
  const beam = interpolate(k, [asked, asked + 18], [0, 1], clamp);
  const card = (name: string, label: string, x: number, y: number, size: number, at: number, extra?: ReactNode) => {
    const s = spring({ frame: k - at, fps, config: { damping: 10, stiffness: 170 } });
    if (k < at) return null;
    return (
      <div style={{ position: "absolute", left: x - size / 2, top: y, width: size, textAlign: "center", scale: `${s}` }}>
        <div style={{ width: size, height: size, borderRadius: 30, background: "#FFFDF7", boxShadow: "0 10px 20px rgba(40,20,0,0.22)", display: "grid", placeItems: "center", position: "relative" }}>
          <Img src={pic(`heist/${name}_cut`)} style={{ width: size * 0.9, height: size * 0.9, objectFit: "contain", translate: `0 ${Math.sin((k + x) / 12) * 4}px` }} />
          {extra}
        </div>
        <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 34, color: INK, marginTop: 10 }}>{label}</div>
      </div>
    );
  };
  const lock = (at: number) => (k >= at ? <div style={{ position: "absolute", right: -16, bottom: -16, width: 64, height: 64, borderRadius: 32, background: "#25541A", color: "#FFF8EC", display: "grid", placeItems: "center", fontFamily: HEADING, fontWeight: 700, fontSize: 32, scale: `${spring({ frame: k - at, fps, config: { damping: 8, stiffness: 220 } })}` }}>✓</div> : null);
  const nope = k >= no ? <div style={{ position: "absolute", left: -18, top: -18, padding: "4px 14px", borderRadius: 14, border: "6px solid #C0392B", background: "#FFF8EC", color: "#C0392B", fontFamily: HEADING, fontWeight: 700, fontSize: 34, rotate: "-12deg", scale: `${spring({ frame: k - no, fps, config: { damping: 8, stiffness: 220 } })}` }}>NO</div> : null;
  return (
    <div style={{ position: "absolute", left: 0, top: 0, width: 1240, height: H, translate: `${slide * 1300}px 0`, boxShadow: "18px 0 40px rgba(40,20,0,0.35)", zIndex: 70 }}>
      <Paper>
        <div style={{ position: "absolute", left: 190, top: 34, fontFamily: HEADING, fontWeight: 700, fontSize: 70, color: "#2A1A5E" }}>The family key</div>
        <div style={{ position: "absolute", left: 194, top: 124, width: 470, fontFamily: BODY, fontWeight: 700, fontSize: 29, lineHeight: 1.25, color: "#3F3566" }}>Two-step verification, where the second key is the family.</div>
        <PeriMoods scene="e4" parts={[{ from, pose: "peri_thinking" }, { from: say("e4", 4) - 4, pose: "peri_calling" }, { from: say("e4", 5) - 4, pose: "peri_waiting" }, { from: say("e4", 6) - 4, pose: "peri_calm" }]} x={150} y={330} w={500} h={H - 330} />
        <svg style={{ position: "absolute", left: 0, top: 0 }} width={1240} height={H}>
          {[740, 1080].map((x2) => (
            <line key={x2} x1={910} y1={250} x2={910 + (x2 - 910) * beam} y2={250 + (330 - 250) * beam} stroke="#A98BFF" strokeWidth={8} strokeDasharray="18 14" strokeDashoffset={-k * 3} strokeLinecap="round" />
          ))}
        </svg>
        {card("pear_rosa", "Rosa", 910, 40, 200, 6)}
        {card("pear_ana", "Ana", 740, 330, 170, asked + 10, <>{lock(say("e4", 5, "each confirmed") - from)}{nope}</>)}
        {card("pear_pablo", "Pablo", 1080, 330, 170, asked + 16, lock(say("e4", 5, "each confirmed") - from + 8))}
        {STEPS.map((s, i) => {
          const at = say("e4", s.line, s.word) - from;
          if (k < at) return null;
          const p = spring({ frame: k - at, fps, config: { damping: 11, stiffness: 180 } });
          return (
            <div key={s.text} style={{ position: "absolute", left: 640, top: 590 + i * 62, display: "flex", alignItems: "center", gap: 14, translate: `${(1 - p) * 120}px 0`, opacity: p }}>
              <div style={{ width: 40, height: 40, borderRadius: 20, background: s.color, display: "grid", placeItems: "center", fontFamily: HEADING, fontWeight: 700, fontSize: 24, color: INK, flex: "none" }}>{i + 1}</div>
              <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 28, color: INK, whiteSpace: "nowrap" }}>{s.text}</div>
            </div>
          );
        })}
      </Paper>
    </div>
  );
};

/** 1:26 6:15 p.m. Keep her talking and get the code: Peri pauses it, and the family key does the rest. */
const E4 = () => {
  const T = TAKES.e4;
  const keyFrom = say("e4", 3) - 8;
  const anaFrom = say("e4", 7) - 10;
  const result = say("e4", 8) - 6;
  return (
    <>
      <Camera shots={[{ at: 0, zoom: 1 }, { at: say("e4", 1) - 10, zoom: 1.04, x: 500, y: 500, ease: 30 }, { at: say("e4", 2) - 6, zoom: 1.07, x: 900, y: 380, ease: 10 }, { at: keyFrom - 4, zoom: 1, ease: 14 }, { at: anaFrom - 4, zoom: 1.05, x: 1100, y: 600, ease: 20 }, { at: result - 4, zoom: 1, ease: 16 }]} shakes={[say("e4", 2), result + 10]}>
        <Place src="diary/garden_sunset" focus="50% 50%" name="e4">
          <Motes kind="petals" count={16} />
          <Motes kind="fireflies" count={10} y={420} h={500} />
        </Place>
        <Album
          scene="e4"
          snaps={[
            { at: say("e4", 0) - 8, pose: "crook_phone", who: "crook" },
            { at: say("e4", 1) - 8, pose: "rosa_garden_call", who: "rosa" },
            { at: say("e4", 2) - 8, pose: "peri_protective", who: "peri" },
            { at: anaFrom, pose: "ana_no", who: "ana" },
            { at: result, pose: "crook_quit", who: "crook" },
            { at: ends("e4", 8) + 8, pose: "rosa_garden_smile", who: "rosa" },
          ]}
        />
        <Slam text="PAUSED." at={say("e4", 2)} until={keyFrom - 2} x={760} y={100} size={120} color="#FFF8EC" shade="#2A1A5E" rotate={-5} />
        <Note at={say("e4", 2) + 12} until={keyFrom - 2} x={790} y={300} w={420} rotate={2} tape="#E3D3FF">
          <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 36, color: "#2A1A5E", lineHeight: 1.12 }}>A code during a stranger's call needs the family's yes.</div>
        </Note>
        <Sequence from={keyFrom} durationInFrames={result - keyFrom} layout="none">
          <Leaving length={result - keyFrom}>
            <AppPhone take={T} x={PHONE.x} y={PHONE.y} h={PHONE.h} tilt={-3} marks={[{ region: SCAM_BOX, from: say("e4", 4, "Ana and Pablo") - keyFrom, to: say("e4", 5) - keyFrom }, { region: TWO_YES, from: say("e4", 5) - keyFrom, to: anaFrom - keyFrom, color: "#6EC6FF" }, { region: BUTTONS, from: anaFrom - keyFrom, to: result - keyFrom, color: "#A98BFF" }]} />
          </Leaving>
          <Zoom take={T} region={BUTTONS} x={800} y={440} w={560} from={anaFrom - keyFrom} to={result - keyFrom} label="One tap" color="#A98BFF" />
        </Sequence>
        <FamilyKey from={keyFrom} until={anaFrom} />
        <Slam text="FAMILY SAYS NO." at={ends("e4", 7)} until={result - 2} x={760} y={100} size={76} color="#FFF8EC" bg="#2A1A5E" rotate={-5} />
        <Victory at={result} n={4} />
      </Camera>
      <Label from={keyFrom} to={result} text="ANA'S PHONE" />
      {/* The hour stays pinned until the family key's page slides over it. */}
      <Sequence from={0} durationInFrames={keyFrom + 6} layout="none">
        <TimeCard time="6:15 p.m." title="Keep her talking" ink="#6A4BC4" from={15 * 60 + 30} to={18 * 60 + 15} />
      </Sequence>
      <Snd src="submission/sfx/alert" at={say("e4", 2)} volume={0.5} />
      <Snd src="short/sfx/ding" at={keyFrom + 12} volume={0.45} />
      <Snd src="final/sfx/slam" at={say("e4", 5, "each confirmed")} volume={0.4} />
      <Snd src="reel/sfx/stamp" at={say("e4", 5, "A single no")} volume={0.5} />
      <Snd src="reel/sfx/stamp" at={ends("e4", 7)} volume={0.55} />
      <Snd src="diary/sfx/page" at={keyFrom} volume={0.4} />
    </>
  );
};

const GATES = [
  { title: "Rules on the phone", text: "Work offline", color: "#BDE68A", word: "Rules" },
  { title: "Hard rules", text: "No AI can overrule them", color: "#FFB8AA", word: "Hard rules" },
  { title: "Muse Spark Open", text: "A second opinion, ±2 at most", color: "#B4E2FF", word: "A second" },
  { title: "The family", text: "The final yes", color: "#D9CBFF", word: "And the family" },
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

/** Every picture in this story (and the app) was made with Muse Image: a wall of them. */
const MUSE = [
  "peri7/calm.webp", "short/crook_plan", "peri7/dressed_crown.webp", "diary/town_morning", "peri7/trap_family_1.webp", "heist/rosa_kitchen", "peri7/calm_hero.webp", "peri7/protective.webp",
  "heist/ana_call", "peri7/calm_tea.webp", "diary/kitchen_noon", "peri7/dressed_flowers.webp", "short/crook_desk", "peri7/trap_code_1.webp", "peri7/calm_medal.webp", "submission/rosa_morning",
  "peri7/thinking.webp", "diary/street_afternoon", "peri7/calm_party.webp", "short/crook_phone", "peri7/dressed_bow.webp", "heist/rosa_qr", "peri7/trap_link_1.webp", "peri7/calling.webp",
  "peri7/calm_love.webp", "diary/garden_sunset", "short/crook_stickers", "peri7/calm_scarf.webp", "heist/rosa_garden_call", "peri7/trap_official_1.webp", "peri7/dressed_glasses.webp", "peri7/waiting.webp",
  "heist/crook_angry", "peri7/calm_idea.webp", "diary/cottage_night", "peri7/happy.webp", "short/ana_no", "peri7/trap_payment_1.webp", "peri7/calm_detective.webp", "short/crook_shock",
  "peri7/calm_butterfly.webp", "diary/ana_hug", "peri7/listening.webp", "peri7/trap_callback_1.webp", "heist/rosa_yes", "peri7/calm_flower.webp", "short/crook_quit", "peri7/worried.webp",
  "diary/peri_desk", "peri7/calm_clap.webp", "heist/pear_ana_cut", "peri7/trap_prize_1.webp", "diary/rosa_fallen_night", "peri7/calm_peekaboo.webp", "heist/pear_pablo_cut", "diary/hill_morning",
  "peri7/calm_glasses.webp", "heist/pear_rosa_cut", "peri7/trap_romance_1.webp", "diary/cover",
];
const MuseWall = ({ at }: { at: number }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const k = fr - at;
  const head = spring({ frame: k, fps, config: { damping: 12, stiffness: 150 } });
  const count = Math.round(interpolate(k, [6, 96], [0, 1000], { ...clamp, easing: Easing.out(Easing.cubic) }));
  return (
    <Paper margin={false}>
      <div style={{ position: "absolute", left: 0, right: 0, top: 26, textAlign: "center", scale: `${interpolate(head, [0, 1], [1.6, 1])}`, opacity: Math.min(1, head * 2) }}>
        <span style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 70, color: INK }}>Every picture: Muse Image </span>
        <span style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 70, color: "#7B5CD6" }}>· {count >= 1000 ? "1,000+" : count} made for FriendlyPear</span>
      </div>
      {MUSE.map((src, i) => {
        const col = i % 12;
        const row = Math.floor(i / 12);
        const when = 6 + ((i * 37) % MUSE.length) * 1.4;
        if (k < when) return null;
        const s = spring({ frame: k - when, fps, config: { damping: 11, stiffness: 190 } });
        const sprite = src.endsWith(".webp") || src.endsWith("_cut");
        return (
          <div key={src} style={{ position: "absolute", left: 22 + col * 157, top: 152 + row * 162, width: 146, height: 154, padding: "8px 8px 14px", background: "#FFFDF7", borderRadius: 4, boxShadow: "0 6px 12px rgba(40,20,0,0.25)", rotate: `${((i * 53) % 9) - 4}deg`, scale: `${interpolate(s, [0, 1], [1.8, 1])}`, opacity: Math.min(1, s * 3) }}>
            <Img src={pic(src)} style={{ width: 130, height: 130, objectFit: sprite ? "contain" : "cover", background: sprite ? "#EEF6E2" : undefined }} />
          </div>
        );
      })}
      {[0, 8, 16, 24, 32, 40, 48, 56, 64].map((d) => (
        <Snd key={d} src="diary/sfx/flick" at={at + 6 + d} volume={0.3} />
      ))}
    </Paper>
  );
};

/** 2:03 How Peri decides, drawn on a page: four layers, six Meta models, the results, and the wall of her pictures. */
const How = () => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const teamAt = say("how", 2) - 6;
  const resultsAt = say("how", 3) - 2;
  const wallAt = say("how", 4) - 6;
  const gates = GATES.map((g) => say("how", 1, g.word));
  const reached = gates.filter((t) => fr >= t).length;
  const cards = TEAM.map((_, i) => teamAt + 8 + i * 10);
  const writing = interpolate(fr, [2, 20], [0, 1], clamp);
  return (
    <Camera shots={[{ at: 0, zoom: 1 }, { at: say("how", 1) - 6, zoom: 1.04, x: 900, y: 400, ease: 60 }, { at: teamAt - 6, zoom: 1, ease: 12 }, { at: wallAt, zoom: 1.15, x: 960, y: 520, ease: 1 }, { at: wallAt + 2, zoom: 1, ease: 110 }]}>
      {fr >= wallAt ? (
        <MuseWall at={wallAt} />
      ) : (
        <Paper>
          {fr < teamAt && (
            <>
              <div style={{ position: "absolute", left: 190, top: 40, fontFamily: HEADING, fontWeight: 700, fontSize: 76, color: INK, clipPath: `inset(0 ${(1 - writing) * 100}% 0 0)` }}>How Peri decides</div>
              <svg style={{ position: "absolute", left: 0, top: 0 }} width={1920} height={H}>
                <path d="M 196 138 C 420 152, 600 128, 830 142" stroke="#E5533D" strokeWidth={6} fill="none" strokeLinecap="round" strokeDasharray={700} strokeDashoffset={interpolate(fr, [18, 34], [700, 0], clamp)} />
                {[0, 1, 2].map((i) => {
                  const t = gates[i + 1];
                  const k = interpolate(fr, [t - 4, t + 8], [0, 1], clamp);
                  const x0 = 520 + i * 370;
                  if (k <= 0) return null;
                  return <path key={i} d={`M ${x0} 400 q 30 -40 64 0`} stroke={INK} strokeWidth={5} fill="none" strokeLinecap="round" strokeDasharray={120} strokeDashoffset={(1 - k) * 120} markerEnd={k >= 1 ? "url(#tip)" : undefined} />;
                })}
                <defs>
                  <marker id="tip" markerWidth="10" markerHeight="10" refX="6" refY="5" orient="auto">
                    <path d="M0,0 L10,5 L0,10 z" fill={INK} />
                  </marker>
                </defs>
              </svg>
              {GATES.map((g, i) => {
                if (fr < gates[i]) return null;
                const p = spring({ frame: fr - gates[i], fps, config: { damping: 11, stiffness: 170 } });
                const lit = reached - 1 === i;
                return (
                  <div key={g.title} style={{ position: "absolute", left: 230 + i * 370, top: 210, width: 310, height: 330, background: g.color, boxShadow: lit ? "0 18px 30px rgba(40,20,0,0.35)" : "0 8px 16px rgba(40,20,0,0.22)", rotate: `${[-3, 2, -2, 3][i]}deg`, scale: `${interpolate(p, [0, 1], [1.5, lit ? 1.04 : 1])}`, opacity: Math.min(1, p * 2.5), padding: 22, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", textAlign: "center" }}>
                    <Tape color="rgba(255,255,255,0.7)" w={110} />
                    <div style={{ width: 70, height: 70, borderRadius: 35, background: "rgba(255,255,255,0.85)", display: "grid", placeItems: "center", fontFamily: HEADING, fontWeight: 700, fontSize: 42, color: INK }}>{i + 1}</div>
                    <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 42, lineHeight: 1.05, color: INK, marginTop: 16 }}>{g.title}</div>
                    <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 26, color: "#3F4D39", marginTop: 10 }}>{g.text}</div>
                  </div>
                );
              })}
              {reached > 0 && (
                <div style={{ position: "absolute", top: 600, left: interpolate(spring({ frame: fr - gates[reached - 1], fps, config: { damping: 14 } }), [0, 1], [230 + Math.max(0, reached - 2) * 370, 230 + (reached - 1) * 370]) + 155, translate: "-50% 0", padding: "14px 26px", borderRadius: "28px 28px 28px 6px", background: reached === 4 && fr > gates[3] + 20 ? "#C0392B" : "#FFFDF7", color: reached === 4 && fr > gates[3] + 20 ? "#FFF8EC" : INK, fontFamily: BODY, fontWeight: 700, fontSize: 28, whiteSpace: "nowrap", boxShadow: "0 10px 22px rgba(40,20,0,0.3)" }}>
                  {reached === 4 && fr > gates[3] + 20 ? "TRAP. Stopped." : "“Hi Mum, new number...”"}
                </div>
              )}
              <PeriMoods scene="how" parts={[{ from: 0, pose: "peri_happy" }, { from: say("how", 1) - 4, pose: "peri_thinking" }]} x={1450} y={560} w={420} h={424} enter={4} />
              {gates.map((t, i) => (
                <Snd key={i} src="diary/sfx/flick" at={t} volume={0.45} />
              ))}
              <Snd src="diary/sfx/pencil" at={2} volume={0.35} />
            </>
          )}
          {fr >= teamAt && (
            <>
              <div style={{ position: "absolute", left: 190, top: 40, fontFamily: HEADING, fontWeight: 700, fontSize: 70, color: INK }}>Six Meta models, one job each</div>
              <div style={{ position: "absolute", left: 20, top: 200, width: 640, height: 640 }}>
                {TEAM.map((m, i) => {
                  if (fr < cards[i]) return null;
                  const s = spring({ frame: fr - cards[i], fps, config: { damping: 8, stiffness: 180 } });
                  const x = ROBOTS_X[m.robot] * 640;
                  return (
                    <div key={m.name}>
                      <div style={{ position: "absolute", left: x - 74, top: 0.86 * 640 - 82, width: 148, height: 148, borderRadius: "50%", background: `radial-gradient(circle, ${m.color} 0%, ${m.color}99 38%, transparent 70%)`, scale: `${s}` }} />
                      <div style={{ position: "absolute", left: x - 50, top: 0.86 * 640 + 44, width: 100, height: 22, borderRadius: "50%", background: m.color, opacity: 0.85, scale: `${s}` }} />
                    </div>
                  );
                })}
                <Img src={img("conductor")} style={{ position: "absolute", inset: 0, width: "100%", height: "100%", filter: "drop-shadow(0 16px 20px rgba(40,20,0,0.3))" }} />
              </div>
              {TEAM.map((m, i) => {
                if (fr < cards[i]) return null;
                const s = spring({ frame: fr - cards[i], fps, config: { damping: 10, stiffness: 200 } });
                return (
                  <div key={m.name} style={{ position: "absolute", left: 700 + (i % 2) * 600, top: 170 + Math.floor(i / 2) * 170, width: 560, padding: "18px 24px", background: "#FFFDF7", borderRadius: 6, boxShadow: `0 8px 0 ${m.color}, 0 18px 28px rgba(40,20,0,0.22)`, rotate: `${[-1.5, 1, 1.5, -1, -1.2, 1.4][i]}deg`, scale: `${interpolate(s, [0, 1], [1.5, 1])}`, opacity: Math.min(1, s * 3), display: "flex", gap: 18, alignItems: "center" }}>
                    <div style={{ width: 50, height: 50, borderRadius: 25, background: m.color, flex: "none" }} />
                    <div>
                      <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 36, color: INK, lineHeight: 1.05 }}>{m.name}</div>
                      <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 26, color: "#3F4D39", marginTop: 4 }}>{m.job}</div>
                    </div>
                  </div>
                );
              })}
              {fr >= resultsAt && (
                <div style={{ position: "absolute", left: 720, top: 700, width: 1140, display: "flex", alignItems: "center", gap: 30, padding: "18px 34px", background: "#FFF3B8", borderRadius: 8, boxShadow: "0 12px 26px rgba(40,20,0,0.25)", rotate: "-1.5deg", scale: `${interpolate(spring({ frame: fr - resultsAt, fps, config: { damping: 9, stiffness: 200 } }), [0, 1], [1.6, 1])}` }}>
                  <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 150, lineHeight: 1, color: "#25541A" }}>91%</div>
                  <div style={{ fontFamily: HEADING, fontWeight: 600, fontSize: 38, lineHeight: 1.15, color: INK }}>of 200 unseen scams caught on the phone, before any model · 4% false alarms</div>
                </div>
              )}
              {cards.map((t, i) => (
                <Snd key={i} src="diary/sfx/flick" at={t} volume={0.35} />
              ))}
              <Snd src="final/sfx/impact" at={resultsAt} volume={0.45} />
            </>
          )}
        </Paper>
      )}
    </Camera>
  );
};

/**
 * 2:28 9:40 p.m. A quiet evening, until Rosa falls: Peri checks on her, calls Ana when she doesn't answer, and Ana
 * comes. The day ends with a hug.
 */
const Night = () => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const T = TAKES.night;
  const phoneFrom = say("night", 2) - 12;
  const anaAt = say("night", 3) - 8;
  const hugAt = ends("night", 3) + 4;
  const hug = spring({ frame: fr - hugAt, fps, config: { damping: 16, stiffness: 90 } });
  const [lx, ly] = onPicture(0.6, 0.415, 0.44);
  const [w1x, w1y] = onPicture(0.6, 0.375, 0.47);
  const [w2x, w2y] = onPicture(0.6, 0.62, 0.47);
  return (
    <>
      <Camera shots={[{ at: 0, zoom: 1 }, { at: say("night", 1) - 10, zoom: 1.05, x: 450, y: 520, ease: 30 }, { at: say("night", 2) - 10, zoom: 1.02, x: 1100, y: 500 }, { at: hugAt - 4, zoom: 1, ease: 12 }]}>
        <Place src="diary/cottage_night" focus="50% 60%" dim={false} name="night">
          <Glow x={lx} y={ly} w={260} h={260} color="rgba(255,190,90,0.9)" speed={2.5} strength={0.75} />
          <Glow x={w1x} y={w1y} w={220} h={220} color="rgba(255,190,100,0.8)" speed={14} strength={0.5} />
          <Glow x={w2x} y={w2y} w={220} h={220} color="rgba(255,190,100,0.8)" speed={17} strength={0.5} />
          <Motes kind="twinkles" count={18} y={0} h={240} />
          <Motes kind="fireflies" count={16} y={430} h={520} />
        </Place>
        <Album
          scene="night"
          until={hugAt + 6}
          snaps={[
            { at: say("night", 0) - 8, pose: "peri_calm", who: "peri" },
            { at: say("night", 1) - 8, pose: "rosa_fallen_night", who: "rosa" },
            { at: say("night", 2) - 8, pose: "peri_calling", who: "peri" },
            { at: anaAt, pose: "ana_go_rest", who: "ana" },
          ]}
        />
        <Sequence from={phoneFrom} durationInFrames={hugAt - phoneFrom} layout="none">
          <Leaving length={hugAt - phoneFrom}>
            <AppPhone take={T} x={PHONE.x} y={PHONE.y} h={PHONE.h} tilt={-2} marks={[{ region: BUBBLE, from: shown(T, 4.0), to: shown(T, 6.4) }, { region: CALLER, from: shown(T, 16.9), to: hugAt - phoneFrom, color: "#6EC6FF" }]} />
          </Leaving>
          <Zoom take={T} region={BUBBLE} x={790} y={420} w={600} from={shown(T, 4.2)} to={shown(T, 6.4)} label="Peri checks" />
          <Zoom take={T} region={CALLER} x={800} y={440} w={580} from={shown(T, 16.9)} to={hugAt - phoneFrom} label="No answer: calling Ana" color="#6EC6FF" />
        </Sequence>
        {fr >= hugAt && (
          <AbsoluteFill style={{ height: H, opacity: Math.min(1, hug * 1.6), zIndex: 70 }}>
            <Img src={pic("diary/ana_hug")} style={{ width: "100%", height: "100%", objectFit: "cover", objectPosition: "47% 44%", scale: `${interpolate(fr, [hugAt, hugAt + 150], [1.04, 1.12])}` }} />
            <Floaties x={60} w={1800} kinds={["fx_heart_pink", "fx_heart_coral", "fx_star"]} count={10} />
            <Img src={pic("peri7/calm_love.webp")} style={{ position: "absolute", left: 1500, top: 560, width: 400, height: 400, translate: `0 ${(1 - hug) * 200}px` }} />
          </AbsoluteFill>
        )}
      </Camera>
      <Label from={phoneFrom} to={hugAt} text="ROSA'S PHONE" />
      <Sequence from={0} durationInFrames={hugAt} layout="none">
        <TimeCard time="9:40 p.m." title="A quiet evening" ink="#2A4B8C" from={18 * 60 + 15} to={21 * 60 + 40} />
      </Sequence>
      <Snd src="submission/sfx/listen" at={phoneFrom + shown(T, 13.0)} volume={0.4} />
      <Snd src="submission/sfx/ringback" at={phoneFrom + shown(T, 16.8)} volume={0.28} length={Math.max(1, hugAt - phoneFrom - shown(T, 16.8))} />
    </>
  );
};

/** 2:45 Back at her desk: the tally of the day, who built her, goodnight, and the diary closes. */
const Close = () => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const yawnAt = ends("close", 2) + 2;
  const sleepAt = yawnAt + 22;
  const closeAt = sleepAt + 40;
  const credits = say("close", 1);
  const logo = spring({ frame: fr - credits, fps, config: { damping: 11, stiffness: 140 } });
  const swing = interpolate(fr, [closeAt, closeAt + 12], [1, 0], { ...clamp, easing: Easing.out(Easing.cubic) });
  const back = interpolate(fr, [closeAt + 12, closeAt + 30], [2.25, 1], { ...clamp, easing: Easing.inOut(Easing.cubic) });
  return (
    <>
      <Camera shots={[{ at: 0, zoom: 1 }, { at: say("close", 2) - 10, zoom: 1.05, x: 640, y: 560, ease: 90 }]}>
        <Actor pose="peri_desk" who="peri" scene="close" x={0} y={0} w={1920} h={H} enter={-999} rise={false} />
        {fr >= yawnAt && <Img src={pic("diary/peri_desk_yawn")} style={{ position: "absolute", left: 0, top: 0, width: 1920, height: H, objectFit: "cover", objectPosition: "50% 45%", scale: "1.07", transformOrigin: "50% 85%", opacity: interpolate(fr, [yawnAt, yawnAt + 6], [0, 1], clamp) }} />}
        {fr >= sleepAt && <Img src={pic("diary/peri_desk_sleep")} style={{ position: "absolute", left: 0, top: 0, width: 1920, height: H, objectFit: "cover", objectPosition: "50% 45%", scale: "1.07", transformOrigin: "50% 85%", opacity: interpolate(fr, [sleepAt, sleepAt + 10], [0, 1], clamp) }} />}
        <Motes kind="dust" count={22} x={980} y={60} w={900} h={760} />
        <Glow x={1640} y={430} w={520} h={420} color="rgba(255,200,120,0.55)" speed={9} strength={0.5} />
        <Note at={say("close", 0) + 6} x={1180} y={70} w={640} rotate={-2} tape="#BDE68A">
          <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 52, color: "#25541A", textAlign: "center", lineHeight: 1.15 }}>4 tricks · $0 lost</div>
          <div style={{ fontFamily: HEADING, fontWeight: 600, fontSize: 38, color: BROWN, textAlign: "center", marginTop: 6 }}>1 family, a little closer</div>
        </Note>
        {fr >= credits && (
          <div style={{ position: "absolute", left: 1150, top: 330, width: 700, padding: "30px 34px", background: PAPER, borderRadius: 8, boxShadow: "0 18px 34px rgba(40,20,0,0.35)", rotate: "1.5deg", scale: `${interpolate(logo, [0, 1], [1.3, 1])}`, opacity: Math.min(1, logo * 2), zIndex: 40 }}>
            <Tape color="#FFE07A" />
            <div style={{ display: "flex", alignItems: "center", gap: 18, justifyContent: "center" }}>
              <Img src={img("icon")} style={{ width: 96, height: 96, borderRadius: "50%" }} />
              <span style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 80, color: INK }}>FriendlyPear</span>
            </div>
            <div style={{ fontFamily: HEADING, fontWeight: 600, fontSize: 36, color: "#25541A", textAlign: "center", marginTop: 14 }}>Built by Rafa in 9 days</div>
            <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 26, color: BROWN, textAlign: "center", marginTop: 10 }}>Meta Global AI Developer Hackathon · Indie track</div>
            <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 26, color: BROWN, textAlign: "center", marginTop: 6 }}>Real app · Android · English & Spanish</div>
            <div style={{ margin: "16px auto 0", width: "fit-content", padding: "8px 20px", borderRadius: 999, background: "#25541A", color: "#FFF8EC", fontFamily: BODY, fontWeight: 700, fontSize: 26 }}>github.com/Ruby570bocadito/FriendlyPear</div>
          </div>
        )}
        {fr >= sleepAt && <Floaties x={560} w={260} kinds={["fx_music_note", "fx_star"]} count={4} />}
      </Camera>
      {fr >= closeAt && <CoverFace zoom={back} turn={swing} credits={interpolate(fr, [closeAt + 24, closeAt + 34], [0, 1], clamp)} />}
      <Snd src="diary/sfx/pencil" at={say("close", 0) - 10} volume={0.3} />
      <Snd src="diary/sfx/close" at={closeAt + 11} volume={0.7} />
    </>
  );
};

const BODIES: Record<Name, ReactNode> = {
  open: <Open />,
  title: <Title />,
  stakes: <Stakes />,
  e1: <E1 />,
  e2: <E2 />,
  e3: <E3 />,
  e4: <E4 />,
  how: <How />,
  night: <Night />,
  close: <Close />,
};

/**
 * A page of the diary; at [turnAt] it turns away to the left around its spine, showing the next one underneath, with
 * the shadow it casts sweeping across the new page.
 */
const Page = ({ turnAt, children }: { turnAt?: number; children: ReactNode }) => {
  const fr = useCurrentFrame();
  const t = turnAt === undefined ? 0 : interpolate(fr, [turnAt, turnAt + TURN], [0, 1], { ...clamp, easing: Easing.inOut(Easing.cubic) });
  if (t >= 1) return null;
  const angle = t * 90;
  const r = (angle * Math.PI) / 180;
  // Where the page's free edge appears as it swings away (perspective 2600 px, spine at the left).
  const edge = (1920 * Math.cos(r)) / (1 + (1920 * Math.sin(r)) / 2600);
  return (
    <div style={{ position: "absolute", left: 0, top: 0, width: 1920, height: H }}>
      <div style={{ position: "absolute", inset: 0, perspective: 2600 }}>
        <div style={{ position: "absolute", inset: 0, overflow: "hidden", transformOrigin: "0% 50%", transform: t > 0 ? `rotateY(${angle}deg)` : undefined }}>
          {children}
          {t > 0 && <AbsoluteFill style={{ background: `linear-gradient(90deg, rgba(255,250,235,${0.2 * Math.sin(t * Math.PI)}) 0%, rgba(0,0,0,0) 35%, rgba(50,25,0,${0.55 * t}) 100%)` }} />}
        </div>
      </div>
      {t > 0 && <div style={{ position: "absolute", top: 0, height: H, left: edge, width: 460, background: `linear-gradient(90deg, rgba(40,20,0,${0.45 * Math.sin(t * Math.PI)}), rgba(40,20,0,0))` }} />}
    </div>
  );
};

/** The look over every page: a soft warm vignette and a little film grain, like an illustrated book. */
const Finish = () => {
  const fr = useCurrentFrame();
  return (
    <div style={{ position: "absolute", left: 0, top: 0, width: 1920, height: H, zIndex: 90, pointerEvents: "none" }}>
      <AbsoluteFill style={{ background: "radial-gradient(ellipse at 50% 46%, rgba(0,0,0,0) 58%, rgba(50,28,8,0.26) 100%)" }} />
      <AbsoluteFill style={{ backgroundImage: `url(${staticFile("diary/grain.png")})`, backgroundPosition: `${Math.floor(rand(fr + 1) * 512)}px ${Math.floor(rand(fr + 999) * 512)}px`, opacity: 0.07, mixBlendMode: "overlay" }} />
    </div>
  );
};

const NAMES = Object.keys(AT) as Name[];

export const Diary = () => (
  <ScriptContext.Provider value={DIARY}>
    <AbsoluteFill style={{ background: "#3A2A1C" }}>
      <Audio src={staticFile("diary/music.wav")} volume={musicVolume(DIARY.spoken)} />
      {NAMES.map((name, i) => {
        const last = i === NAMES.length - 1;
        return (
          <Sequence key={name} from={f(AT[name][0])} durationInFrames={length(name) + (last ? 0 : TURN)} style={{ zIndex: NAMES.length - i }}>
            <Page turnAt={last ? undefined : length(name)}>{BODIES[name]}</Page>
            {!last && <Snd src="diary/sfx/page" at={length(name) - 2} volume={0.55} />}
          </Sequence>
        );
      })}
      <Finish />
      <div style={{ position: "absolute", inset: 0, zIndex: 100 }}>
        <SubtitleBand paper />
      </div>
      {DIARY.spoken.map((s) => (
        <Sequence key={`${s.scene}${s.n}`} from={f(s.start)} layout="none">
          <Audio src={staticFile(`diary/vo_${s.scene}_${s.n}.wav`)} />
        </Sequence>
      ))}
    </AbsoluteFill>
  </ScriptContext.Provider>
);
