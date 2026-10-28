import { AbsoluteFill, Img, interpolate, Sequence, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { Backdrop, Caption, Phone } from "./parts";
import { BODY, C, clamp } from "./theme";

/**
 * The first minute of Peri's story, as a teaser to decide the whole film on: Carmen's village, Peri moving into her
 * phone, the "new number" text and Peri's warning, then the same warning in the real app. 60 frames a second, so
 * every move is smooth; Peri moves with her own frames from the app (walk in eight phases, mouth, blink).
 */
export const StoryTeaser = () => {
  const { fps } = useVideoConfig();
  return (
    <AbsoluteFill style={{ background: C.cream }}>
      <Sequence durationInFrames={270} name="Village">
        <Backdrop name="sky" length={270} />
        <WalkIn />
        <Line who="" text="This is Carmen's village." from={10} to={130} />
        <Line who="" text="And this is Peri. She has just moved into Carmen's phone." from={136} to={270} />
      </Sequence>
      <Sequence from={260} durationInFrames={470} name="Living room">
        <FadeIn>
          <Backdrop name="home" length={470} />
          <Carmen relievedAt={380} />
          <Sms from={30} to={170} />
          <PeriPops at={150} />
          <Line who="Peri" text="Careful: it claims to be family from a new number and asks for money." from={185} to={330} />
          <Line who="Peri" text="Don't do anything yet; let's check by calling their usual number." from={336} to={470} />
        </FadeIn>
      </Sequence>
      <Sequence from={720} name="The real app">
        <FadeIn>
          <Backdrop name="home" length={240} />
          <Caption text="The same warning, in the real app." from={6} to={240} />
          <Phone clip="newnumber" trimBefore={3.4 * fps} enterAt={4} />
        </FadeIn>
      </Sequence>
    </AbsoluteFill>
  );
};

/** Each place comes in over the last one. */
const FadeIn = ({ children }: { children: React.ReactNode }) => {
  const frame = useCurrentFrame();
  return <AbsoluteFill style={{ opacity: interpolate(frame, [0, 20], [0, 1], clamp) }}>{children}</AbsoluteFill>;
};

/** Her walk, as in the app: still, half a step, a step, half a step, then the other foot. */
const WALK = ["", "_steplefthalf", "_stepleft", "_steplefthalf", "", "_steprighthalf", "_stepright", "_steprighthalf"];
const walking = (mood: string, t: number, every = 6) => `peri_${mood}${WALK[Math.floor(t / every) % WALK.length]}`;
/** Her mouth while she talks, with a blink now and then. */
const TALK = ["_talk", "_talkwide", "_talko", "_talk", "", "_talko", "_talkwide", ""];
const talking = (mood: string, t: number, every = 7) =>
  t % 150 > 140 ? `peri_${mood}_blink` : `peri_${mood}${TALK[Math.floor(t / every) % TALK.length]}`;

/** Her shadow on the ground (the cutouts have none, as in the app, which draws its own). */
const Shadow = ({ x, y, width }: { x: number; y: number; width: number }) => (
  <div
    style={{
      position: "absolute",
      left: x - width / 2,
      top: y - width * 0.08,
      width,
      height: width * 0.16,
      borderRadius: "50%",
      background: "radial-gradient(closest-side, rgba(31, 42, 26, 0.34), rgba(31, 42, 26, 0))",
    }}
  />
);

const Peri = ({ name, x, y, size, style }: { name: string; x: number; y: number; size: number; style?: React.CSSProperties }) => (
  <Img src={staticFile(`peri/${name}.webp`)} style={{ position: "absolute", left: x - size / 2, top: y - size, width: size, height: size, ...style }} />
);

/** Peri walks into the village from the right, stops in the middle and waves. */
const WalkIn = () => {
  const frame = useCurrentFrame();
  const arrive = 150;
  const x = interpolate(frame, [0, arrive], [1250, 540], { ...clamp, easing: (t) => 1 - Math.pow(1 - t, 2) });
  // A little bob with each step: up in the middle of it, down when the foot lands.
  const bob = frame < arrive ? Math.sin((Math.PI * (frame % 24)) / 24) * 14 : 0;
  const t = frame - arrive;
  const name = frame < arrive ? walking("calm", frame) : t < 90 ? (Math.floor(t / 12) % 2 === 0 ? "peri_calm_wave1" : "peri_calm_wave2") : "peri_calm";
  // Her feet are at 93% of each picture: the ground is there, and her shadow shrinks a little as she bobs up.
  return (
    <>
      <Shadow x={x} y={1500} width={300 * (1 - bob / 70)} />
      <Peri name={name} x={x} y={1540 - bob} size={620} />
    </>
  );
};

/** Carmen, worried over her phone; relieved once Peri has spoken. She breathes. */
const Carmen = ({ relievedAt }: { relievedAt: number }) => {
  const frame = useCurrentFrame();
  const relief = interpolate(frame, [relievedAt, relievedAt + 24], [0, 1], clamp);
  const breath = 1 + Math.sin(frame / 38) * 0.006;
  const style: React.CSSProperties = { position: "absolute", left: -40, top: 640, height: 1240, width: 827, scale: `1 ${breath}`, transformOrigin: "50% 100%" };
  return (
    <>
      <Shadow x={370} y={1828} width={440} />
      <Img src={staticFile("characters/carmen_cut.webp")} style={{ ...style, opacity: 1 - relief }} />
      <Img src={staticFile("characters/carmen_relieved_cut.webp")} style={{ ...style, opacity: relief }} />
    </>
  );
};

/** The scam text, over Carmen's head. */
const Sms = ({ from, to }: { from: number; to: number }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const pop = spring({ frame: frame - from, fps, config: { damping: 14, stiffness: 140 } });
  const leave = interpolate(frame, [to - 16, to], [1, 0], clamp);
  if (frame < from || frame > to) return null;
  return (
    <div
      style={{
        position: "absolute",
        left: 70,
        right: 70,
        top: 150,
        background: C.white,
        borderRadius: 48,
        padding: "36px 46px 42px",
        boxShadow: "0 30px 70px rgba(31, 42, 26, 0.30)",
        scale: interpolate(pop, [0, 1], [0.6, 1]),
        opacity: Math.min(pop, leave),
      }}
    >
      <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 32, color: C.inkSoft, marginBottom: 14 }}>+44 7700 900123 · now</div>
      <div style={{ fontFamily: BODY, fontSize: 50, lineHeight: 1.25, color: C.ink }}>
        Hi Mum, it's Ana. My phone broke and this is my new number. Can you send me 380 today?
      </div>
    </div>
  );
};

/** Peri springs out of Carmen's phone with her umbrella open, lands beside her and talks. */
const PeriPops = ({ at }: { at: number }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  if (frame < at) return null;
  const jump = spring({ frame: frame - at, fps, config: { damping: 13, stiffness: 90 } });
  // From the phone in Carmen's hands to the floor at her side, over a small arc.
  const x = interpolate(jump, [0, 1], [330, 800]);
  const y = interpolate(jump, [0, 1], [1010, 1800]) - Math.sin(Math.min(jump, 1) * Math.PI) * 260;
  const size = interpolate(jump, [0, 1], [60, 470]);
  const t = frame - at;
  const name = t < 36 ? "peri_protective" : talking("protective", t);
  // Her shadow waits where she lands, and grows as she comes down.
  return (
    <>
      <Shadow x={800} y={1778} width={230 * Math.min(jump, 1)} />
      <Peri name={name} x={x} y={y} size={size} />
    </>
  );
};

/** What is said, as a subtitle at the bottom: the speaker's name in green when it's Peri. */
const Line = ({ who, text, from, to }: { who: string; text: string; from: number; to: number }) => {
  const frame = useCurrentFrame();
  if (frame < from || frame > to) return null;
  const opacity = Math.min(interpolate(frame, [from, from + 10], [0, 1], clamp), interpolate(frame, [to - 10, to], [1, 0], clamp));
  return (
    <div style={{ position: "absolute", left: 60, right: 60, bottom: 70, display: "flex", justifyContent: "center", opacity }}>
      <div
        style={{
          background: "rgba(255, 248, 236, 0.95)",
          borderRadius: 34,
          padding: "22px 36px 26px",
          textAlign: "center",
          textWrap: "balance",
          fontFamily: BODY,
          fontSize: 46,
          lineHeight: 1.25,
          color: C.ink,
          boxShadow: "0 16px 40px rgba(31, 42, 26, 0.22)",
        }}
      >
        {who ? <span style={{ fontWeight: 700, color: C.greenDark }}>{who}: </span> : null}
        {text}
      </div>
    </div>
  );
};
