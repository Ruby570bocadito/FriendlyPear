import { createContext, useContext } from "react";
import type { CSSProperties } from "react";
import { AbsoluteFill, Audio, Img, interpolate, Sequence, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { BODY, clamp, HEADING } from "./theme";

/**
 * The people of the 3-minute videos ("the heist", Final.tsx, and "Peri's diary", Diary.tsx) and what they share: the
 * crook, Rosa, Ana and Peri with their mouths following their voices, blinking and breathing (see face() and Actor),
 * Peri's animations and effects from the app, and the building blocks around them (sounds, flashes, slammed words, the
 * subtitle band). Each video passes its own script (scenes, lines, how long each line lasts and its mouth shapes)
 * through ScriptContext, and can add its own poses to POSES.
 */
export const FPS = 30;
export const H = 984; // the picture, above the subtitle band
export const SPLIT = 760; // people on the left, the app on the right
export const INK = "#1F2A1A";
export const CREAM = "#FFF8EC";
export const f = (seconds: number) => Math.round(seconds * FPS);
export const pic = (path: string) => staticFile(/\.\w+$/.test(path) ? path : `${path}.png`);

export type Who = "crook" | "peri" | "rosa" | "ana";
export type Line = { at: number; who: Who; text: string };
/** A line as the video plays it: its scene, its place there, who says it, and when it starts and ends in seconds. */
export type Spoken = { scene: string; n: number; who: Who; text: string; start: number; end: number };
export type Script = { at: Record<string, [number, number]>; spoken: Spoken[]; visemes: Record<string, string[]> };

/** Every line, placed at its time or just after the one before (as the music places them too). */
export const script = (scenes: Record<string, [number, number]>, lines: Record<string, Line[]>, durations: Record<string, number[]>, visemes: Record<string, string[]>): Script => ({
  at: scenes,
  visemes,
  spoken: Object.keys(scenes).flatMap((scene) => {
    let end = 0;
    return lines[scene].map((line, n) => {
      const start = Math.max(line.at, end + 0.2);
      end = start + durations[scene][n];
      return { scene, n, who: line.who, text: line.text, start: scenes[scene][0] + start, end: scenes[scene][0] + end };
    });
  }),
});
/** When line n of a scene starts (or reaches a word of it), and when it ends, in frames from the scene's start. */
export const timing = (s: Script) => ({
  say: (scene: string, n: number, word?: string) => {
    const x = s.spoken.find((l) => l.scene === scene && l.n === n)!;
    const into = word ? (Math.max(0, x.text.indexOf(word)) / x.text.length) * (x.end - x.start) : 0;
    return f(x.start - s.at[scene][0] + into);
  },
  ends: (scene: string, n: number) => f(s.spoken.find((l) => l.scene === scene && l.n === n)!.end - s.at[scene][0]),
});
export const ScriptContext = createContext<Script>({ at: {}, spoken: [], visemes: {} });
const useScript = () => useContext(ScriptContext);

export type Shape = "closed" | "slight" | "half" | "open" | "wide" | "round" | "smile";
export type Eye = "blink" | "blinkhalf" | "lookleft" | "lookright";
/**
 * Each pose's frames (scripts/heist_visemes.ts, steadied by scripts/heist_stabilize.py): the pose itself, its mouth
 * shapes (the whole picture, only the mouth changed) and its eye frames (just the eyes on a transparent picture, laid
 * over any mouth, so people blink while they talk; Peri's are whole pictures, so she blinks between lines). A pose
 * whose mouth hangs open at rest only nearly shuts it between words ("shut").
 */
export type Pose = { base: string; mouths: Partial<Record<Shape, string>>; eyes: Partial<Record<Eye, string>>; overlay: boolean; shut?: Shape };
const made = (pose: string, names: string[]) => Object.fromEntries(names.map((n) => [n, `heist/v/${pose}_${n}`]));
export const person = (pose: string, base: string, mouths: Shape[], eyes: Eye[], blink?: string, shut?: Shape): Pose => ({
  base,
  mouths: Object.fromEntries(mouths.map((n) => [n, n === "open" ? `heist/${pose}_talk` : `heist/v/${pose}_${n}`])),
  eyes: { ...made(pose, eyes), ...(blink ? { blink } : {}) },
  overlay: true,
  shut,
});
export const CROOK: Shape[] = ["slight", "half", "open", "wide", "round", "smile"];
export const PEOPLE: Shape[] = ["slight", "half", "open", "wide", "round"];
export const GENTLE: Shape[] = ["slight", "half", "open", "round"];
export const GLANCING: Eye[] = ["blinkhalf", "lookleft", "lookright"];
/** Peri as the app draws her (scripts/peri_app.py): a mood or an outfit, its talking frames, blinks and glances. */
export const peri = (look: string, glances = true): Pose => {
  const at = (frame: string) => `peri7/${look}${frame}.webp`;
  return {
    base: at(""),
    mouths: { slight: at("_talko"), round: at("_talko"), half: at("_talk"), open: at("_talk"), smile: at("_talk"), wide: at("_talkwide") },
    eyes: { blink: at("_blink"), blinkhalf: at("_blinkhalf"), ...(glances ? { lookleft: at("_glanceleft"), lookright: at("_glanceright") } : {}) },
    overlay: false,
  };
};
export const OUTFITS = ["crown", "flowers", "bow", "glasses"];
export const POSES: Record<string, Pose> = {
  crook_plan: person("crook_plan", "short/crook_plan", CROOK, GLANCING, "heist/crook_plan_blink"),
  crook_desk: person("crook_desk", "short/crook_desk", CROOK, GLANCING, "heist/crook_desk_blink"),
  crook_phone: person("crook_phone", "short/crook_phone", CROOK, GLANCING, "heist/crook_phone_blink"),
  crook_stickers: person("crook_stickers", "short/crook_stickers", CROOK, GLANCING, "heist/crook_stickers_blink"),
  crook_shock: person("crook_shock", "short/crook_shock", CROOK, ["blink", "blinkhalf"], undefined, "slight"),
  crook_angry: person("crook_angry", "heist/crook_angry", CROOK, ["blink", "blinkhalf"]),
  crook_quit: person("crook_quit", "short/crook_quit", CROOK, ["blink", "blinkhalf"]),
  rosa_home: person("rosa_morning", "submission/rosa_morning", PEOPLE, ["blinkhalf"], "heist/rosa_morning_blink"),
  ana_no: person("ana_no", "short/ana_no", PEOPLE, ["blink", "blinkhalf"]),
  // Rosa somewhere new in every round, and Ana with her mouth closed between words.
  rosa_reading: person("rosa_reading", "heist/rosa_reading", [], GLANCING, "heist/rosa_reading_blink"),
  rosa_yes: person("rosa_yes", "heist/rosa_yes", GENTLE, ["blinkhalf"], "heist/rosa_yes_blink"),
  rosa_kitchen: person("rosa_kitchen", "heist/rosa_kitchen", [], GLANCING, "heist/rosa_kitchen_blink"),
  rosa_qr: person("rosa_qr", "heist/rosa_qr", [], GLANCING, "heist/rosa_qr_blink"),
  rosa_garden_call: person("rosa_garden_call", "heist/rosa_garden_call", GENTLE, GLANCING, "heist/rosa_garden_call_blink"),
  rosa_fallen: person("rosa_fallen", "heist/rosa_fallen", GENTLE, ["blinkhalf"], "heist/rosa_fallen_blink"),
  ana_call_rest: person("ana_call_rest", "heist/ana_call_rest", GENTLE, ["blinkhalf"], "heist/ana_call_rest_blink"),
  ana_go_rest: person("ana_go_rest", "heist/ana_go_rest", GENTLE, ["blinkhalf"], "heist/ana_go_rest_blink"),
  // Peri in the app's moods, and dressed up from her wardrobe.
  peri_calm: peri("calm"),
  peri_happy: peri("happy"),
  peri_listening: peri("listening"),
  peri_thinking: peri("thinking"),
  peri_protective: peri("protective"),
  peri_calling: peri("calling"),
  peri_waiting: peri("waiting"),
  ...Object.fromEntries(OUTFITS.map((o) => [`dressed_${o}`, peri(`dressed_${o}`, false)])),
};
export const FOCUS: Record<string, string> = {
  rosa_home: "34% 40%", rosa_call: "36% 38%", ana_no: "50% 38%", rosa_reading: "34% 40%", rosa_yes: "34% 40%", rosa_kitchen: "38% 40%",
  rosa_qr: "48% 40%", rosa_garden_call: "50% 22%", rosa_fallen: "42% 55%", ana_call_rest: "45% 35%", ana_go_rest: "45% 35%",
};
/** How wide each of them opens their mouth: Rosa and Ana talk gently; the crook opens wide only when he shouts. */
const widest = (who: Who, text: string): Shape => (who === "ana" ? "half" : who === "rosa" || (who === "crook" && !/!|\b[A-Z]{3,}\b/.test(text)) ? "open" : "wide");
/** When a pose lacks a shape, the nearest one it has. */
const NEAR: Record<Shape, Shape[]> = { closed: [], slight: ["half", "open"], half: ["slight", "open"], open: ["half", "wide"], wide: ["open", "half"], round: ["half", "slight", "open"], smile: ["open", "half"] };
const mouthFor = (p: Pose, shape: Shape) => (shape === "closed" ? p.base : [shape, ...NEAR[shape]].map((x) => p.mouths[x]).find(Boolean) ?? p.base);
const OPEN: Record<Shape, number> = { closed: 0, slight: 1, round: 1.5, half: 2, smile: 2.5, open: 3, wide: 4 };
/** The drawing an animator would add between two shapes far apart; neighbours simply cut. */
const inbetween = (a: Shape, b: Shape): Shape | undefined => {
  if (Math.abs(OPEN[a] - OPEN[b]) < 2) return undefined;
  const mid = (OPEN[a] + OPEN[b]) / 2;
  return (["slight", "half", "open"] as Shape[]).reduce((best, s) => (Math.abs(OPEN[s] - mid) < Math.abs(OPEN[best] - mid) ? s : best));
};
const BLINK: Eye[] = ["blinkhalf", "blink", "blink", "blinkhalf"];

/**
 * What a face shows at a frame of the whole video. Speaking: the mouth shape of their voice on that frame
 * (visemes.json, one a frame, each held at least four; a frame ahead of the sound, as animators do), with an
 * in-between drawing on the first frame of a far jump. Always: a blink every few seconds through half-closed eyes;
 * quiet, now and then a glance aside. "talking" eases from 0 to 1 around their lines, for a gentle sway.
 */
const face = (pose: string, who: Who, frame: number, script: Script): { mouth: string; eye?: string; talking: number } => {
  const p = POSES[pose];
  const seed = pose.length * 7 + pose.charCodeAt(1);
  const cycle = (frame + seed * 37) % (100 + (seed % 4) * 14);
  const blink = cycle < 4 ? p.eyes[BLINK[cycle]] ?? p.eyes.blink : undefined;
  const ahead = frame + 1;
  const mine = script.spoken.filter((s) => s.who === who);
  const talking = Math.max(0, ...mine.map((s) => Math.min(1, (ahead - f(s.start) + 8) / 12, (f(s.end) + 10 - ahead) / 14)));
  const line = mine.find((s) => ahead >= f(s.start) && ahead < f(s.end + 0.1));
  if (line) {
    const said = script.visemes[`${line.scene}_${line.n}`] ?? [];
    const most = widest(who, line.text);
    const shape = (k: number) => {
      const s = ((said[k] ?? "closed") === "closed" ? p.shut ?? "closed" : said[k]) as Shape;
      return OPEN[s] > OPEN[most] ? most : s;
    };
    const k = ahead - f(line.start);
    const step = shape(k) !== shape(k - 1) ? inbetween(shape(k - 1), shape(k)) : undefined;
    return { mouth: mouthFor(p, step ?? shape(k)), eye: p.overlay ? blink : undefined, talking };
  }
  const glance = (frame + seed * 53) % 260;
  const eye = blink ?? (glance >= 130 && glance < 158 ? p.eyes.lookleft : glance >= 214 && glance < 236 ? p.eyes.lookright : undefined);
  return p.overlay ? { mouth: p.base, eye, talking } : { mouth: eye ?? p.base, talking };
};

/**
 * A person (or Peri) alive on screen: their face (see face()), breathing like a stop-motion puppet and leaning in a
 * little while they talk; they fade in at [enter] (rising a little, unless [rise] is off), and [bounce] squashes them
 * like clay when their pose changes. For a portrait, [face] (where their face is, as fractions of the picture) keeps it
 * at the same place in the frame and [zoom] moves in on it.
 */
export const Actor = ({ pose, who, scene, x = 0, y = 0, w = SPLIT, h = H, contain = false, enter = 0, rise = true, bounce, face: at, zoom = 1 }: { pose: string; who: Who; scene: string; x?: number; y?: number; w?: number; h?: number; contain?: boolean; enter?: number; rise?: boolean; bounce?: number; face?: [number, number]; zoom?: number }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  const p = POSES[pose];
  const script = useScript();
  const { mouth, eye, talking } = face(pose, who, fr + f(script.at[scene][0]), script);
  const seed = pose.length * 7 + pose.charCodeAt(0);
  const pop = spring({ frame: fr - enter, fps, config: { damping: 12, stiffness: 150 } });
  const breathe = Math.sin(fr / 21 + seed) * 0.006;
  const since = bounce === undefined ? 99 : fr - bounce;
  const squash = since >= 0 && since < 12 ? Math.sin((since / 12) * Math.PI * 2) * 0.06 * (1 - since / 12) : 0;
  const size = (contain ? 1 : 1.07) + breathe + talking * 0.006;
  const whole = [...new Set([p.base, ...Object.values(p.mouths), ...(p.overlay ? [] : Object.values(p.eyes))])];
  const over = p.overlay ? [...new Set(Object.values(p.eyes))] : [];
  // With the picture placed by where the face is, the face sits at that same place in the frame, whatever the crop.
  const focus = at ? `${at[0] * 100}% ${at[1] * 100}%` : contain ? "50% 100%" : FOCUS[pose] ?? "50% 28%";
  const layer = (src: string, shown: boolean, zIndex: number) => (
    <Img key={src} src={pic(src)} style={{ position: "absolute", inset: 0, width: "100%", height: "100%", objectFit: contain ? "contain" : "cover", objectPosition: focus, opacity: shown ? 1 : 0, zIndex }} />
  );
  const pictures = (
    <>
      {whole.map((src) => layer(src!, src === mouth, 1))}
      {over.map((src) => layer(src!, src === eye, 2))}
    </>
  );
  return (
    <div style={{ position: "absolute", left: x, top: y, width: w, height: h, overflow: "hidden", opacity: Math.min(1, pop * 2) }}>
      <div style={{ position: "absolute", inset: 0, scale: `${size + squash} ${size - squash}`, translate: `${Math.sin(fr / 67 + seed) * 2}px ${Math.sin(fr / 43 + seed) * 3 - talking * 4 + (1 - pop) * (rise ? 40 : 0)}px`, rotate: `${Math.sin(fr / 53 + seed) * 0.12}deg`, transformOrigin: "50% 85%" }}>
        {zoom === 1 ? pictures : <div style={{ position: "absolute", inset: 0, scale: `${zoom}`, transformOrigin: focus }}>{pictures}</div>}
      </div>
    </div>
  );
};

/**
 * The people's half of a round: whoever the story is about at each moment, each one cross-fading in over the one
 * before (who stays until they're gone), so the picture never dips to black.
 */
export const Cast = ({ scene, parts }: { scene: string; parts: { from: number; pose: string; who: Who }[] }) => {
  const fr = useCurrentFrame();
  const n = Math.max(0, parts.filter((p) => fr >= p.from).length - 1);
  const part = parts[n];
  const before = n > 0 && fr < part.from + 10 ? parts[n - 1] : undefined;
  return (
    <>
      {before && <Actor key={`${before.from}${before.pose}`} pose={before.pose} who={before.who} scene={scene} enter={before.from} rise={false} />}
      <Actor key={`${part.from}${part.pose}`} pose={part.pose} who={part.who} scene={scene} enter={part.from} rise={n === 0} />
    </>
  );
};

/** Peri changing mood (or outfit) as she talks: one Peri, squashing like clay at each change. */
export const PeriMoods = ({ scene, parts, x, y, w, h, enter }: { scene: string; parts: { from: number; pose: string }[]; x: number; y: number; w: number; h: number; enter?: number }) => {
  const fr = useCurrentFrame();
  const part = [...parts].reverse().find((p) => fr >= p.from) ?? parts[0];
  return <Actor pose={part.pose} who="peri" scene={scene} x={x} y={y} w={w} h={h} contain enter={enter ?? parts[0].from} bounce={part.from} />;
};

/**
 * One of Peri's frame-by-frame animations from the app (ui/peri/PeriFace.kt, scripts/peri_app.py): when each picture
 * comes on, in milliseconds, and how high she is then, as a share of her picture; she rises easing out and falls easing
 * in. Drawn standing on (x, y), the last picture kept.
 */
export type Key = { ms: number; src?: string; lift?: number };
export const DROP: Key[] = [
  { ms: 0, src: "anim_drop_01", lift: 0.8 },
  { ms: 420, src: "anim_drop_02" },
  { ms: 500, src: "anim_drop_03", lift: 0 },
  { ms: 720, src: "anim_drop_04", lift: 0 },
  { ms: 830, lift: 0.07 },
  { ms: 940, src: "anim_drop_05", lift: 0 },
  { ms: 1080, src: "anim_drop_06", lift: 0 },
];
export const CHEER: Key[] = [
  { ms: 0, src: "anim_cheer_01", lift: 0 },
  { ms: 230, src: "anim_cheer_02", lift: 0 },
  { ms: 480, src: "anim_cheer_03", lift: 0.22 },
  { ms: 640, lift: 0.19 },
  { ms: 760, src: "anim_cheer_04", lift: 0 },
];
export const Drawn = ({ keys, at, until, x, y, size }: { keys: Key[]; at: number; until?: number; x: number; y: number; size: number }) => {
  const fr = useCurrentFrame();
  if (fr < at || (until !== undefined && fr >= until)) return null;
  const ms = ((fr - at) / FPS) * 1000;
  const picture = [...keys].reverse().find((k) => k.src && k.ms <= ms)!.src!;
  const heights = keys.filter((k) => k.lift !== undefined);
  const from = [...heights].reverse().find((k) => k.ms <= ms)!;
  const to = heights.find((k) => k.ms > ms);
  let lift = from.lift!;
  if (to) {
    const p = (ms - from.ms) / (to.ms - from.ms);
    lift += (to.lift! - from.lift!) * (to.lift! > from.lift! ? 1 - (1 - p) ** 2 : p * p);
  }
  return <Img src={pic(`peri7/${picture}.webp`)} style={{ position: "absolute", left: x - size / 2, top: y - size * (1 + lift), width: size, height: size }} />;
};

/** Peri's little effects from the app (stars, hearts, notes, confetti) bursting out of a point and falling away. */
export const Sparks = ({ at, x, y, kinds, count = 10, spread = 280 }: { at: number; x: number; y: number; kinds: string[]; count?: number; spread?: number }) => {
  const fr = useCurrentFrame();
  const t = fr - at;
  if (t < 0 || t > 42) return null;
  return (
    <>
      {Array.from({ length: count }, (_, i) => {
        const angle = (i / count) * Math.PI * 2 + 0.4;
        const reach = spread * (0.6 + ((i * 37) % 10) / 25) * (1 - Math.exp(-t / 7));
        const size = 64 + ((i * 53) % 36);
        return (
          <Img key={i} src={pic(`peri7/${kinds[i % kinds.length]}.webp`)} style={{ position: "absolute", left: x + Math.cos(angle) * reach - size / 2, top: y + Math.sin(angle) * reach * 0.75 + t * t * 0.06 - size / 2, width: size, height: size, rotate: `${t * (i % 2 ? 5 : -5)}deg`, opacity: interpolate(t, [0, 3, 30, 42], [0, 1, 1, 0], clamp) }} />
        );
      })}
    </>
  );
};

/** Hearts and notes drifting up behind Peri while she's just being a friend. */
export const Floaties = ({ x, w, kinds, count = 9 }: { x: number; w: number; kinds: string[]; count?: number }) => {
  const fr = useCurrentFrame();
  return (
    <>
      {Array.from({ length: count }, (_, i) => {
        const t = (fr + i * 23) % 120;
        const size = 56 + ((i * 41) % 30);
        return (
          <Img key={i} src={pic(`peri7/${kinds[i % kinds.length]}.webp`)} style={{ position: "absolute", left: x + ((i * 0.37) % 1) * w + Math.sin((fr + i * 40) / 14) * 18, top: H - 80 - t * 7, width: size, height: size, opacity: interpolate(t, [0, 12, 95, 120], [0, 0.9, 0.9, 0], clamp) }} />
        );
      })}
    </>
  );
};

// ---------------------------------------------------------------- building blocks

export const Snd = ({ src, at, volume = 0.6, length }: { src: string; at: number; volume?: number; length?: number }) => (
  <Sequence from={Math.max(0, at)} durationInFrames={length} layout="none">
    <Audio src={staticFile(`${src}.wav`)} volume={volume} />
  </Sequence>
);

export const Flash = ({ at = 0 }: { at?: number }) => {
  const fr = useCurrentFrame();
  const o = interpolate(fr - at, [0, 1, 7], [0.85, 0.85, 0], clamp);
  if (fr < at || o <= 0) return null;
  return <AbsoluteFill style={{ background: "#FFFFFF", opacity: o }} />;
};

/** Big words slammed onto an empty part of the screen, with a little shake. */
export const Slam = ({ text, at, until, x, y, size = 120, color = CREAM, shade = "rgba(0,0,0,0.35)", rotate = -4, bg, center = false, style }: { text: string; at: number; until?: number; x: number; y: number; size?: number; color?: string; shade?: string; rotate?: number; bg?: string; center?: boolean; style?: CSSProperties }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  if (fr < at || (until !== undefined && fr > until + 8)) return null;
  const p = spring({ frame: fr - at, fps, config: { damping: 10, stiffness: 200 } });
  const out = until === undefined ? 1 : interpolate(fr, [until, until + 8], [1, 0], clamp);
  const shake = fr - at < 8 ? Math.sin((fr - at) * 3) * (8 - (fr - at)) * 2 : 0;
  return (
    <div style={{ position: "absolute", left: x, top: y, translate: center ? "-50% 0" : undefined, opacity: out, ...style }}>
      <div style={{ scale: `${interpolate(p, [0, 1], [2.1, 1])}`, rotate: `${rotate}deg`, translate: `${shake}px 0`, opacity: Math.min(1, p * 2.5), padding: bg ? "10px 30px" : 0, background: bg, borderRadius: 24, fontFamily: HEADING, fontWeight: 700, fontSize: size, lineHeight: 1.04, color, textShadow: bg ? undefined : `0 ${size * 0.07}px 0 ${shade}`, whiteSpace: "pre", textAlign: center ? "center" : "left" }}>
        {text}
      </div>
    </div>
  );
};

/** The subtitle band under the picture: who speaks, and what they say. */
const VOICE: Record<Who, { bg: string; fg: string; name: string }> = {
  crook: { bg: "#3B2A4A", fg: "#C9B7E8", name: "THE CROOK" },
  peri: { bg: "#25541A", fg: "#BDE68A", name: "PERI" },
  rosa: { bg: "#4A3566", fg: "#E3D3FF", name: "ROSA" },
  ana: { bg: "#1E4A6B", fg: "#B4E2FF", name: "ANA" },
};
const FIGURES: [string, string][] = [
  ["seven point seven billion dollars", "$7.7 billion"],
  ["over sixty", "over 60"],
  ["Ninety-one percent", "91%"],
  ["nine days", "9 days"],
];
/** The subtitle band, dark (the heist) or as the bottom of a notebook page ([paper], Peri's diary). */
export const SubtitleBand = ({ paper = false }: { paper?: boolean }) => {
  const fr = useCurrentFrame();
  const t = fr / FPS;
  const now = useScript().spoken.find((s) => t >= s.start && t < s.end + 0.15);
  const quiet = !now;
  return (
    <div style={{ position: "absolute", left: 0, right: 0, top: H, height: 1080 - H, background: paper ? "#FBF3E1" : "#0E0A18", borderTop: paper ? "3px solid #E9B8B0" : undefined, display: "flex", alignItems: "center", justifyContent: "center" }}>
      {!quiet && (
        <div style={{ display: "flex", alignItems: "center", gap: 16, maxWidth: 1700 }}>
          <span style={{ padding: "4px 14px", borderRadius: 999, background: paper ? VOICE[now.who].bg : VOICE[now.who].fg, color: paper ? VOICE[now.who].fg : VOICE[now.who].bg, fontFamily: BODY, fontWeight: 700, fontSize: 22, letterSpacing: 2, flex: "none" }}>{VOICE[now.who].name}</span>
          <span style={{ color: paper ? INK : CREAM, fontFamily: BODY, fontWeight: 700, fontSize: 32, lineHeight: 1.2 }}>{FIGURES.reduce((s, [a, b]) => s.replace(a, b), now.text)}</span>
        </div>
      )}
    </div>
  );
};

/** A cut-out pose popping in with a squash. */
export const Pop = ({ src, x, y, size, at = 0 }: { src: string; x: number; y: number; size: number; at?: number }) => {
  const fr = useCurrentFrame();
  const { fps } = useVideoConfig();
  if (fr < at) return null;
  const p = spring({ frame: fr - at, fps, config: { damping: 9, stiffness: 170 } });
  const breathe = Math.sin((fr - at) / 11) * 0.015;
  return <Img src={src} style={{ position: "absolute", left: x - size / 2, top: y - size, width: size, height: size, translate: `0 ${(1 - p) * 300}px`, scale: `${1 + breathe} ${1 - breathe}`, transformOrigin: "50% 100%", opacity: Math.min(1, p * 3), filter: "drop-shadow(0 18px 20px rgba(0,0,0,0.25))" }} />;
};

/** Gold coins tumbling down and away, the money scams drain. */
export const Coins = ({ count = 16 }: { count?: number }) => {
  const fr = useCurrentFrame();
  return (
    <>
      {Array.from({ length: count }, (_, i) => {
        const size = 54 + ((i * 29) % 40);
        const speed = 5 + ((i * 7) % 5);
        const y = ((fr * speed + i * 157) % (H + 300)) - 150;
        return (
          <div key={i} style={{ position: "absolute", left: ((i * 0.61 + 0.07) % 1) * 1860, top: y, width: size, height: size, borderRadius: "50%", background: "radial-gradient(circle at 35% 30%, #FFF0A8, #FFC83D 45%, #D99400)", border: `${size * 0.07}px solid #B37600`, boxShadow: "0 8px 14px rgba(0,0,0,0.25)", display: "grid", placeItems: "center", fontFamily: HEADING, fontWeight: 700, fontSize: size * 0.55, color: "#9A6200", rotate: `${fr * (i % 2 ? 4 : -3) + i * 40}deg`, scale: `${0.55 + 0.45 * Math.abs(Math.cos((fr + i * 11) / 9))} 1`, opacity: 0.85 }}>
            $
          </div>
        );
      })}
    </>
  );
};

/** The music, quieter under the voices and silent at the very end. */
export const musicVolume = (spoken: Spoken[]) => (fr: number) => {
  const t = fr / FPS;
  let duck = 0;
  for (const s of spoken) {
    if (t >= s.start - 0.1 && t <= s.end + 0.05) {
      duck = 1;
      break;
    }
    const near = Math.min(Math.abs(t - s.start + 0.1), Math.abs(t - s.end - 0.05));
    if (near < 0.3) duck = Math.max(duck, 1 - near / 0.3);
  }
  return (0.55 - 0.32 * duck) * interpolate(t, [178.5, 180], [1, 0], clamp);
};
