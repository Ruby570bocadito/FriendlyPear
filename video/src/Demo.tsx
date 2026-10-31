import { AbsoluteFill, Audio, Img, interpolate, OffthreadVideo, Sequence, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import durations from "../public/voice/durations.json";
import edit from "../public/voice/edicion.json";
import { Backdrop } from "./parts";
import { BODY, C, clamp, HEADING } from "./theme";

/**
 * The 3-minute demo for the jury (docs/GUION.md): Carmen's clay village tells the story, and each time the app acts,
 * the real app slides in on a phone, recorded on an Android emulator (docs/demo/tools/moments.sh). 1920x1080, 60 fps.
 * The voice is a draft (Windows), from video/public/voice/lines.json; the final one replaces its files.
 */
const FPS = 60;
const s = (seconds: number) => Math.round(seconds * FPS);

/** The voice's lines as written (the voice reads lines.json), and their lengths (durations.json, from scripts/voice.ps1). */
const LINES: Record<number, string> = {
  1: "This is Carmen. Every week, someone writes to her pretending to be her bank, her daughter or the post office. And this is Peri: a pear who moved into her phone to look after her.",
  2: "A new number asking for money? Peri warns her before she answers, and offers to call her daughter on the number she already trusts. One call, and the scam is over.",
  3: "When a stranger calls pretending to be the bank, Peri says so before she picks up. And if a code arrives during the call, Peri knows: no bank ever asks for it. Hang up.",
  4: "A sticker on a parking meter? Carmen shows Peri a picture. Peri reads the QR code on the phone without opening it, and Muse Spark looks at the rest of the picture: it's a trap.",
  5: "When something is risky, the family decides together. Ana's Peri gets an urgent request: a stranger just called her mother. Ana can say no, or call Mum right away.",
  6: "And when Carmen needs help, she only has to say so.",
  7: "Most of Peri lives on the phone: rules that flag 91 percent of scams they had never seen, with 4 percent false alarms. Only doubtful messages go to Meta's Muse Spark models for a second opinion. Muse Image drew every picture of Peri, and the family never reads Carmen's messages.",
  8: "FriendlyPear. A friend in the phone, so nobody is ever alone with a scammer.",
};
const VOICE = durations as Record<string, number>;

/** From the edit's recipe (video/scripts/editar.ps1): each scene's headline, and when each word of the person's voice is said. */
type Word = { w: string; from: number; to: number };
const HEADLINES = edit.headlines as Record<string, string>;
const WORDS = edit.words as Record<string, Word[]>;

/** Each scene: where it starts and ends (s), its place, when its voice starts, and its phone clip (s into the clip). */
const SCENES = [
  { n: 1, from: 0, to: 14, place: "village_sunset_wide", voiceAt: 0.3 },
  { n: 2, from: 14, to: 44, place: "living_day_wide", voiceAt: 1, clip: "m1_newnumber", clipFrom: 4, phoneAt: 4 },
  { n: 3, from: 44, to: 64, place: "living_evening_wide", voiceAt: 0.5, clip: "m3_code", clipFrom: 5, phoneAt: 2 },
  { n: 4, from: 64, to: 84, place: "street_parking_wide", voiceAt: 0.3, clip: "m4_picture", clipFrom: 2, phoneAt: 3 },
  { n: 5, from: 84, to: 112, place: "ana_home_wide", voiceAt: 0.7, clip: "m6_family_ana", clipFrom: 44, phoneAt: 3 },
  { n: 6, from: 112, to: 126, place: "kitchen_wide", voiceAt: 0.3, clip: "m5_fallen", clipFrom: 4, phoneAt: 1 },
  { n: 7, from: 126, to: 160, place: "peri_workshop_wide", voiceAt: 0.7 },
  { n: 8, from: 160, to: 180, place: "village_sunset_wide", voiceAt: 1 },
] as const;

export const DEMO_FRAMES = s(180);

export const Demo = () => (
  <AbsoluteFill style={{ background: C.cream }}>
    {SCENES.map((scene) => (
      <Sequence key={scene.n} from={s(scene.from)} durationInFrames={s(scene.to - scene.from)}>
        <Backdrop name={scene.place} length={s(scene.to - scene.from)} />
        <Cast n={scene.n} length={s(scene.to - scene.from)} />
        {"clip" in scene ? <DemoPhone clip={scene.clip} clipFrom={s(scene.clipFrom)} enterAt={s(scene.phoneAt)} /> : null}
        <Sequence from={s(scene.voiceAt)} layout="none">
          <Audio src={staticFile(`voice/vo_${scene.n}.wav`)} />
          {WORDS[scene.n] ? (
            <WordSubtitles words={WORDS[scene.n]} beside={"clip" in scene} />
          ) : (
            <Subtitles text={LINES[scene.n]} length={s(VOICE[scene.n])} beside={"clip" in scene} />
          )}
        </Sequence>
        {HEADLINES[scene.n] ? <Headline text={HEADLINES[scene.n]} /> : null}
        <FadeFromCream />
      </Sequence>
    ))}
  </AbsoluteFill>
);

/** Each scene opens softly from the cream of the app. */
const FadeFromCream = () => {
  const frame = useCurrentFrame();
  const opacity = interpolate(frame, [0, 18], [1, 0], clamp);
  return opacity > 0 ? <AbsoluteFill style={{ background: C.cream, opacity }} /> : null;
};

/** A picture of the story, cut out (video/scripts/cut.py), standing on the ground at [x] with its feet at [bottom]. */
const Figure = ({ names, every = 10, x, bottom = 40, height, enter = 0, from = "left" }: {
  names: string[]; every?: number; x: number; bottom?: number; height: number; enter?: number; from?: "left" | "right" | "below" | "pop";
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  if (frame < enter) return null;
  const t = frame - enter;
  const name = names[Math.floor(t / every) % names.length];
  const come = spring({ frame: t, fps, config: { damping: 18, stiffness: 90 } });
  const shift = from === "left" ? `${interpolate(come, [0, 1], [-700, 0])}px 0` : from === "right" ? `${interpolate(come, [0, 1], [700, 0])}px 0` : from === "below" ? `0 ${interpolate(come, [0, 1], [500, 0])}px` : "0 0";
  return (
    <Img
      src={staticFile(`cut/${name}.webp`)}
      style={{ position: "absolute", left: x, bottom, height, translate: shift, scale: from === "pop" ? come : 1, transformOrigin: "50% 100%" }}
    />
  );
};

/** One after another: [steps] is pairs of (picture, seconds it stays). */
const Steps = ({ steps, x, bottom = 40, height, enter = 0 }: { steps: [string, number][]; x: number; bottom?: number; height: number; enter?: number }) => {
  const frame = useCurrentFrame() - enter;
  if (frame < 0) return null;
  let at = 0;
  let name = steps[steps.length - 1][0];
  for (const [picture, seconds] of steps) {
    if (frame < at + s(seconds)) {
      name = picture;
      break;
    }
    at += s(seconds);
  }
  return <Img src={staticFile(`cut/${name}.webp`)} style={{ position: "absolute", left: x, bottom, height }} />;
};

/** Who is in each scene, on the left two thirds (the phone takes the right third). */
const Cast = ({ n, length }: { n: number; length: number }) => {
  switch (n) {
    case 1:
      return (
        <>
          <Figure names={["carmen_walk_01", "carmen_walk_02", "carmen_walk_03", "carmen_walk_04"]} every={9} x={420} height={620} from="left" />
          <Figure names={["story_wave_01", "story_wave_02", "story_wave_03", "story_wave_02"]} every={12} x={1130} height={560} enter={s(6)} from="pop" />
        </>
      );
    case 2:
      return (
        <>
          <Figure names={["carmen_answer_01"]} x={120} height={640} />
          <ScamSms />
          <Steps steps={[["story_phone_out_01", 0.5], ["story_phone_out_02", 0.5], ["story_phone_out_03", 0.5], ["story_phone_out_04", 30]]} x={600} height={520} enter={s(5)} />
        </>
      );
    case 3:
      return (
        <>
          <Figure names={["phone_ring_01", "phone_ring_02"]} every={8} x={700} bottom={420} height={300} />
          <Steps steps={[["carmen_answer_01", 6], ["carmen_answer_02", 7], ["carmen_hangup", 30]]} x={150} height={640} />
        </>
      );
    case 4:
      return (
        <>
          <Figure names={["qr_peel_01", "qr_peel_02"]} every={30} x={140} height={620} />
          <Figure names={["story_detective_01", "story_detective_02"]} every={24} x={640} height={560} enter={s(2)} from="right" />
        </>
      );
    case 5:
      return <Steps steps={[["ana_run", 9], ["ana_relief", 30]]} x={250} height={660} />;
    case 6:
      return <Figure names={["carmen_gasp"]} x={300} height={640} />;
    case 7:
      return (
        <>
          <Figure names={["story_present"]} x={70} height={520} from="pop" />
          <Workshop length={length} />
        </>
      );
    default:
      return (
        <>
          <Figure names={["carmen_hug_ana"]} x={190} height={660} from="pop" />
          <Figure names={["story_family_pears"]} x={760} height={480} enter={s(2)} from="pop" />
          <Logo />
        </>
      );
  }
};

/** The new-number text, as it arrives on Carmen's phone. */
const ScamSms = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const pop = spring({ frame: frame - 20, fps, config: { damping: 12, stiffness: 140 } });
  const leave = interpolate(frame, [s(5), s(5.4)], [1, 0], clamp);
  if (frame > s(5.4)) return null;
  return (
    <div style={{ position: "absolute", left: 560, top: 150, width: 560, background: C.white, borderRadius: 36, padding: "28px 34px",
      boxShadow: "0 24px 60px rgba(31, 42, 26, 0.30)", scale: interpolate(pop, [0, 1], [0.6, 1]), opacity: Math.min(pop, leave) }}>
      <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 24, color: C.inkSoft, marginBottom: 10 }}>+44 7700 900123 · now</div>
      <div style={{ fontFamily: BODY, fontSize: 34, lineHeight: 1.25, color: C.ink }}>Hi Mum, it's Ana. My phone broke and this is my new number. Can you send me 380 today?</div>
    </div>
  );
};

/** How Peri works, one card at a time, as her voice goes through it. */
const CARDS = [
  ["On the phone", "Rules: 91 % of unseen scams caught, 4 % false alarms (200 messages)"],
  ["Muse Spark Open", "A second opinion on doubtful messages"],
  ["Muse Spark + tools", "The investigator"],
  ["SAM 3.1", "What's wrong in a picture"],
  ["Muse Voice Transcribe", "Peri's ears"],
  ["Muse Image", "Every picture of Peri, and the trap comics"],
  ["Open source", "The family never reads your messages"],
];
const Workshop = ({ length }: { length: number }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const every = Math.floor((length - s(4)) / CARDS.length);
  return (
    <div style={{ position: "absolute", left: 560, top: 90, width: 1250, display: "flex", flexWrap: "wrap", gap: 22 }}>
      {CARDS.map(([title, text], i) => {
        const come = spring({ frame: frame - s(0.8) - i * every, fps, config: { damping: 16 } });
        return (
          <div key={title} style={{ width: 600, background: i === 0 ? C.greenDark : C.cream, color: i === 0 ? C.white : C.ink, borderRadius: 30,
            padding: "22px 28px", boxShadow: "0 16px 40px rgba(31, 42, 26, 0.24)", opacity: come, scale: interpolate(come, [0, 1], [0.8, 1]) }}>
            <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 36 }}>{title}</div>
            <div style={{ fontFamily: BODY, fontSize: 27, lineHeight: 1.25, marginTop: 6 }}>{text}</div>
          </div>
        );
      })}
    </div>
  );
};

/** The name, and what it was built with. */
const Logo = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const come = spring({ frame: frame - s(4), fps, config: { damping: 14 } });
  return (
    <div style={{ position: "absolute", right: 110, top: 150, width: 640, textAlign: "center", opacity: come, scale: interpolate(come, [0, 1], [0.7, 1]) }}>
      <Img src={staticFile("cut/icon.webp")} style={{ width: 200, height: 200 }} />
      <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 96, color: C.greenDark, textShadow: "0 4px 0 rgba(255,255,255,0.7)" }}>FriendlyPear</div>
      <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 34, color: C.ink, background: C.cream, borderRadius: 26, padding: "12px 26px", marginTop: 14 }}>
        Open source · Built with Meta's Muse models
      </div>
    </div>
  );
};

const PHONE_H = 900;
const PHONE_W = Math.round((PHONE_H * 1080) / 2400);
const BEZEL = 12;

/** The real app on a phone at the right, springing up from below, with the label that says it's real. */
const DemoPhone = ({ clip, clipFrom, enterAt }: { clip: string; clipFrom: number; enterAt: number }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const up = spring({ frame: frame - enterAt, fps, config: { damping: 16, stiffness: 110 } });
  return (
    <div style={{ position: "absolute", right: 150, top: 120, width: PHONE_W + 2 * BEZEL, height: PHONE_H + 2 * BEZEL, padding: BEZEL, borderRadius: 54,
      background: C.ink, boxShadow: "0 36px 80px rgba(31, 42, 26, 0.40)", translate: `0 ${interpolate(up, [0, 1], [1100, 0])}px` }}>
      <div style={{ width: PHONE_W, height: PHONE_H, borderRadius: 44, overflow: "hidden", background: C.cream }}>
        <Sequence from={enterAt} layout="none">
          <OffthreadVideo src={staticFile(`clips/${clip}.mp4`)} trimBefore={clipFrom} muted style={{ width: PHONE_W, height: PHONE_H }} />
        </Sequence>
      </div>
      <div style={{ position: "absolute", top: -26, left: "50%", translate: "-50% 0", whiteSpace: "nowrap", background: C.greenDark, color: C.white,
        fontFamily: BODY, fontWeight: 700, fontSize: 24, padding: "10px 22px", borderRadius: 24 }}>
        Real app · recorded on Android
      </div>
    </div>
  );
};

/** The scene's name, top left in Peri's yellow, bouncing in and leaving after a few seconds. */
const Headline = ({ text }: { text: string }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const pop = spring({ frame: frame - s(0.4), fps, config: { damping: 8, stiffness: 160 } });
  const leave = interpolate(frame, [s(3.6), s(4)], [1, 0], clamp);
  if (frame > s(4)) return null;
  return (
    <div style={{ position: "absolute", left: 80, top: 60, fontFamily: HEADING, fontWeight: 700, fontSize: 76, color: C.sun,
      WebkitTextStroke: `14px ${C.ink}`, paintOrder: "stroke fill", textShadow: "0 8px 0 rgba(31, 42, 26, 0.35)",
      scale: interpolate(pop, [0, 1], [0.3, 1]), rotate: `${interpolate(pop, [0, 1], [-8, -2])}deg`, transformOrigin: "0% 50%", opacity: Math.min(pop, leave) }}>
      {text}
    </div>
  );
};

/** The person's words, two or three at a time as they say them, the one being said in Peri's green. */
const WordSubtitles = ({ words, beside }: { words: Word[]; beside: boolean }) => {
  const frame = useCurrentFrame();
  const now = frame / FPS;
  const groups: Word[][] = [];
  for (const word of words) {
    const group = groups[groups.length - 1];
    if (group && group.length < 3 && !/[.,?!:]$/.test(group[group.length - 1].w)) group.push(word);
    else groups.push([word]);
  }
  const group = groups.find((g, i) => now < (groups[i + 1]?.[0].from ?? g[g.length - 1].to + 0.4) && now >= g[0].from - 0.05);
  if (!group) return null;
  const saying = group.filter((word) => now >= word.from - 0.05).length - 1;
  return (
    <div style={{ position: "absolute", left: beside ? 80 : 160, right: beside ? 700 : 160, bottom: 60, display: "flex", justifyContent: "center", gap: 22 }}>
      {group.map((word, i) => (
        <span key={`${groups.indexOf(group)}-${i}`} style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 72, color: i === saying ? C.green : C.white,
          WebkitTextStroke: `12px ${C.ink}`, paintOrder: "stroke fill", textShadow: "0 6px 0 rgba(31, 42, 26, 0.35)" }}>
          {word.w}
        </span>
      ))}
    </div>
  );
};

/** The voice's words at the bottom, a sentence at a time, each for its share of the line; [beside] the phone, left of it. */
const Subtitles = ({ text, length, beside }: { text: string; length: number; beside: boolean }) => {
  const frame = useCurrentFrame();
  const sentences = text.match(/[^.?!:]+[.?!:]?/g)?.map((x) => x.trim()).filter(Boolean) ?? [text];
  const total = sentences.reduce((n, x) => n + x.length, 0);
  let at = 0;
  for (const sentence of sentences) {
    const lasts = (length * sentence.length) / total;
    if (frame < at + lasts) {
      return (
        <div style={{ position: "absolute", left: beside ? 80 : 160, right: beside ? 700 : 160, bottom: 46, display: "flex", justifyContent: "center" }}>
          <div style={{ background: "rgba(31, 42, 26, 0.82)", color: C.white, fontFamily: BODY, fontSize: 38, lineHeight: 1.3, padding: "12px 28px",
            borderRadius: 22, textAlign: "center", textWrap: "balance" }}>
            {sentence}
          </div>
        </div>
      );
    }
    at += lasts;
  }
  return null;
};
