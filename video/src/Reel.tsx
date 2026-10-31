import { AbsoluteFill, Audio, Img, interpolate, Sequence, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import durations from "../public/submission/durations.json";
import { BODY, clamp, HEADING } from "./theme";
import { Burst, CONFETTI, img, LOOK, Peri, Scene, Words } from "./Minute";
import { AppPhone, BUBBLE, BUTTONS, CALLER, Place, SCAM_BOX, Sfx, shown, Zoom } from "./Submission";
import type { Take } from "./Submission";

/**
 * The Instagram reel: a 30-second vertical demo (1080x1920, 30 fps) with the same pieces as the submission video,
 * the 30-second music (scripts/minute_music.py thirty) and a few of Peri's lines. Words stay inside the area the
 * app's buttons and caption don't cover (roughly 200 to 1550 px from the top).
 */
export const REEL_FRAMES = 900;
const FPS = 30;
const INK = "#1F2A1A";

const T = {
  hook: [0, 120],
  meet: [120, 240],
  number: [240, 420],
  code: [420, 600],
  family: [600, 720],
  end: [720, 900],
} as const;

const TAKES = {
  number: { clip: "m1_newnumber", cuts: [[4.4, 9.4], [20.4, 21.6], [23.3, 26.0]], rate: 1.36 },
  code: { clip: "m3_code", cuts: [[3.4, 8.0], [12.4, 17.5]], rate: 1.49 },
  family: { clip: "m6_family_ana", cuts: [[45.5, 72]], rate: 1 },
} satisfies Record<string, Take>;

/** Peri's lines (from the submission's voice): which file, when (seconds) and what she says. */
const LENGTH = durations as Record<string, number[]>;
const VOICE = [
  { file: "hook_0", at: 0.2, text: "This text says it's from Ana." },
  { file: "hook_1", at: 2.35, text: "It isn't." },
  { file: "meet_0", at: 4.4, text: "Hi! I'm Peri, a clay pear who lives in Rosa's phone." },
  { file: "end_1", at: 24.8, text: "FriendlyPear. You relax. I'll keep watch." },
].map((v) => {
  const [scene, n] = v.file.split("_");
  return { ...v, end: v.at + LENGTH[scene][Number(n)] };
});

const Title = ({ text, delay, color = INK, shade = "transparent", top = 210, size = 84 }: { text: string; delay: number; color?: string; shade?: string; top?: number; size?: number }) => (
  <Words text={text} delay={delay} size={size} color={color} shade={shade} style={{ left: 70, top, width: 940, justifyContent: "center" }} />
);

const SMS = "Hi Mum, it's Ana. My phone broke and this is my new number. Can you send me 380 today for a bill? I'll pay you back tomorrow x";

const Hook = () => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  const nope = Math.round(2.35 * FPS);
  const card = spring({ frame: f - 6, fps, config: { damping: 12, stiffness: 110 } });
  const stamp = spring({ frame: f - nope, fps, config: { damping: 9, stiffness: 170 } });
  return (
    <>
      <Place name="scammer" veil="linear-gradient(180deg, rgba(26,12,33,0.85) 0%, rgba(26,12,33,0.55) 50%, rgba(26,12,33,0.85) 100%)" position="40% 50%" />
      <Title text="Is this text really from Ana?" delay={4} color="#FFF8EC" shade="#1A0C21" />
      <div style={{ position: "absolute", left: 70, top: 560, width: 940, opacity: card, translate: `0 ${(1 - card) * 80}px`, scale: `${0.9 + 0.1 * card}`, rotate: "-2deg" }}>
        <div style={{ background: "#FFFFFF", borderRadius: 48, padding: "34px 42px 40px", boxShadow: "0 24px 60px rgba(0,0,0,0.4)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 18, marginBottom: 18 }}>
            <div style={{ width: 66, height: 66, borderRadius: 33, background: "#D8DEE8", display: "grid", placeItems: "center", fontFamily: HEADING, fontWeight: 700, fontSize: 32, color: "#5B6575" }}>?</div>
            <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 38, color: "#2A2F38" }}>+44 7700 900123</div>
          </div>
          <div style={{ fontFamily: BODY, fontSize: 50, lineHeight: 1.3, color: "#1E2228" }}>{SMS}</div>
        </div>
        {f >= nope && (
          <div style={{ position: "absolute", right: -10, top: -70, padding: "16px 34px", borderRadius: 24, border: "9px solid #FF8A7A", color: "#FF8A7A", background: "rgba(255,248,236,0.94)", fontFamily: HEADING, fontWeight: 700, fontSize: 76, rotate: "12deg", scale: `${interpolate(stamp, [0, 1], [2.4, 1])}`, opacity: Math.min(1, stamp * 2) }}>
            NOT ANA
          </div>
        )}
      </div>
      <Sfx name="buzz" at={2} volume={0.55} />
      <Sfx name="ding" at={6} volume={0.5} />
      <Sfx name="thud" at={nope} volume={0.7} />
    </>
  );
};

const Meet = () => {
  const L = LOOK.meet;
  return (
    <>
      <Place name="meadow" veil="linear-gradient(180deg, rgba(255,248,236,0.92) 0%, rgba(255,248,236,0.6) 35%, rgba(255,248,236,0) 60%)" position="30% 50%" />
      <Title text="Meet Peri." delay={6} size={150} color={L.ink} shade={L.shade} top={220} />
      <Title text="A clay pear in your phone that stops scams." delay={22} size={58} color={L.ink} top={430} />
      <Peri name="wave" x={540} y={1560} size={980} delay={4} />
      <Burst x={540} y={760} delay={16} colors={CONFETTI} />
    </>
  );
};

const NewNumber = () => {
  const K = TAKES.number;
  const L = LOOK.number;
  return (
    <>
      <Title text="A new number says it's family?" delay={4} color={L.ink} shade={L.shade} size={74} />
      <AppPhone take={K} x={540} y={1130} h={1180} tilt={-3} marks={[{ region: BUBBLE, from: shown(K, 4.8), to: shown(K, 9.4) }, { region: CALLER, from: shown(K, 23.5), to: 186, color: "#6EC6FF" }]} />
      <Zoom take={K} region={BUBBLE} x={60} y={440} w={960} from={shown(K, 4.8)} to={shown(K, 9.4) - 2} label="Peri warns at once" />
      <Zoom take={K} region={CALLER} x={110} y={440} w={860} from={shown(K, 23.5)} to={186} label="Then calls the real Ana" color="#6EC6FF" />
      <Sfx name="ding" at={shown(K, 4.6)} volume={0.5} />
      <Sfx name="ringback" at={shown(K, 23.3)} volume={0.3} length={shown(K, 25.0) - shown(K, 23.3)} />
      <Sfx name="answer" at={shown(K, 25.0)} volume={0.5} />
    </>
  );
};

const Code = () => {
  const f = useCurrentFrame();
  const K = TAKES.code;
  const L = LOOK.code;
  const fire = shown(K, 13.0);
  return (
    <>
      <Title text={f < fire ? "A stranger calls..." : "A code during the call?"} delay={4} color={L.ink} shade={L.shade} size={74} />
      <AppPhone take={K} x={540} y={1130} h={1180} tilt={3} shake={f >= fire && f < fire + 18 ? 7 : 0} marks={[{ region: BUBBLE, from: shown(K, 4.6), to: shown(K, 8.0) }, { region: BUBBLE, from: shown(K, 13.2), to: 186, color: "#FFE07A" }]} />
      <Zoom take={K} region={BUBBLE} x={60} y={440} w={960} from={shown(K, 4.6)} to={shown(K, 8.0) - 2} label="Before answering" />
      <Zoom take={K} region={BUBBLE} x={60} y={440} w={960} from={shown(K, 13.2)} to={186} label="Hang up. It's always a scam." color="#FFE07A" />
      <Sfx name="ring" at={shown(K, 3.8)} volume={0.45} length={shown(K, 7.8) - shown(K, 3.8)} />
      <Sfx name="ding" at={shown(K, 12.8)} volume={0.5} />
      <Sfx name="alert" at={fire} volume={0.7} />
    </>
  );
};

const Family = () => {
  const K = TAKES.family;
  const L = LOOK.family;
  return (
    <>
      <Title text="Something risky? Peri asks the family." delay={4} color={L.ink} shade={L.shade} size={70} />
      <AppPhone take={K} x={540} y={1130} h={1180} tilt={-3} marks={[{ region: SCAM_BOX, from: 10, to: 60 }, { region: BUTTONS, from: 60, to: 126, color: "#A98BFF" }]} />
      <Zoom take={K} region={SCAM_BOX} x={60} y={440} w={960} from={10} to={58} label="On Ana's phone" />
      <Zoom take={K} region={BUTTONS} x={190} y={440} w={700} from={60} to={126} label="One tap. No spying." color="#A98BFF" />
      <Sfx name="ding" at={14} volume={0.5} />
    </>
  );
};

const End = () => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  const L = LOOK.end;
  const logo = spring({ frame: f - 8, fps, config: { damping: 10, stiffness: 120 } });
  return (
    <>
      <div style={{ position: "absolute", left: 0, right: 0, top: 220, display: "flex", justifyContent: "center", alignItems: "center", gap: 22, scale: `${logo}` }}>
        <Img src={img("icon")} style={{ width: 120, height: 120, borderRadius: "50%" }} />
        <span style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 96, color: L.ink, textShadow: `0 7px 0 ${L.shade}` }}>FriendlyPear</span>
      </div>
      <Title text="You relax. I'll keep watch." delay={24} size={72} color={L.ink} top={390} />
      <Peri name="hero" x={540} y={1460} size={860} delay={4} />
      <Burst x={540} y={720} delay={12} count={34} colors={CONFETTI} />
      <Title text="Built for the Meta AI Hackathon" delay={60} size={44} color={L.ink} top={1490} />
      <Sfx name="sparkle" at={12} volume={0.45} />
    </>
  );
};

/** Peri's line on a pill, just above where Instagram puts its caption. */
const Caption = () => {
  const f = useCurrentFrame();
  const t = f / FPS;
  const now = VOICE.find((v) => t >= v.at && t < v.end + 0.2);
  if (!now) return null;
  return (
    <div style={{ position: "absolute", left: 60, right: 60, top: 1560, display: "flex", justifyContent: "center" }}>
      <div style={{ display: "flex", alignItems: "center", gap: 16, padding: "12px 30px 12px 12px", borderRadius: 999, background: "rgba(20, 27, 18, 0.82)" }}>
        <Img src={img("icon")} style={{ width: 60, height: 60, borderRadius: "50%", scale: `${1 + Math.abs(Math.sin(f / 3)) * 0.06}` }} />
        <span style={{ color: "#FFF8EC", fontFamily: BODY, fontWeight: 700, fontSize: 38, lineHeight: 1.2 }}>{now.text}</span>
      </div>
    </div>
  );
};

/** The music, quieter while Peri speaks. */
const musicVolume = (f: number) => {
  const t = f / FPS;
  const speaking = VOICE.some((v) => t >= v.at - 0.2 && t <= v.end + 0.1);
  return (speaking ? 0.22 : 0.55) * interpolate(t, [29, 30], [1, 0], clamp);
};

export const Reel = () => (
  <AbsoluteFill style={{ background: "#2B1436" }}>
    <Audio src={staticFile("minute/music30.wav")} volume={musicVolume} />
    <Scene at={T.hook} look={LOOK.hook} origin="50% 40%">
      <Hook />
    </Scene>
    <Scene at={T.meet} look={LOOK.meet} origin="50% 70%" rays>
      <Meet />
    </Scene>
    <Scene at={T.number} look={LOOK.number} origin="50% 60%">
      <NewNumber />
    </Scene>
    <Scene at={T.code} look={LOOK.code} origin="50% 60%">
      <Code />
    </Scene>
    <Scene at={T.family} look={LOOK.family} origin="50% 60%">
      <Family />
    </Scene>
    <Scene at={T.end} look={LOOK.end} origin="50% 60%" rays last>
      <End />
    </Scene>
    {VOICE.map((v) => (
      <Sequence key={v.file} from={Math.round(v.at * FPS)} layout="none">
        <Audio src={staticFile(`submission/vo_${v.file}.mp3`)} />
      </Sequence>
    ))}
    <Caption />
  </AbsoluteFill>
);
