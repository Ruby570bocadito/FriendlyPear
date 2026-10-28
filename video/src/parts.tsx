import { AbsoluteFill, Img, interpolate, OffthreadVideo, Sequence, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { BODY, C, clamp, HEADING } from "./theme";

/** A place of Peri's world, slowly coming closer (made with Muse Image: scripts/backdrops.ts). */
export const Backdrop = ({ name, length }: { name: string; length: number }) => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill>
      <Img
        src={staticFile(`backdrops/${name}.webp`)}
        style={{ width: "100%", height: "100%", objectFit: "cover", scale: interpolate(frame, [0, length], [1.04, 1.13], clamp) }}
      />
    </AbsoluteFill>
  );
};

/** A line of the ad on a cream card at the top, as Peri's own cards look; it comes in and leaves softly. */
export const Caption = ({ text, from, to }: { text: string; from: number; to: number }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const enter = spring({ frame: frame - from, fps, config: { damping: 200 } });
  const leave = interpolate(frame, [to - 8, to], [1, 0], clamp);
  if (frame < from || frame > to) return null;
  return (
    <div
      style={{
        position: "absolute",
        top: 110,
        left: 70,
        right: 70,
        display: "flex",
        justifyContent: "center",
        opacity: Math.min(enter, leave),
        translate: `0 ${interpolate(enter, [0, 1], [-36, 0])}px`,
      }}
    >
      <div
        style={{
          background: C.cream,
          color: C.ink,
          fontFamily: HEADING,
          fontWeight: 600,
          fontSize: 64,
          lineHeight: 1.12,
          padding: "26px 44px",
          borderRadius: 42,
          textAlign: "center",
          textWrap: "balance",
          boxShadow: "0 18px 44px rgba(31, 42, 26, 0.28)",
        }}
      >
        {text}
      </div>
    </div>
  );
};

const PHONE_H = 1400;
const PHONE_W = Math.round((PHONE_H * 1080) / 2400);
const BEZEL = 16;

/**
 * The real app, recorded on an Android emulator (scripts/record.sh), in a phone that springs up from below. The
 * clip starts [trimBefore] frames in, so the moment that matters comes right away.
 */
export const Phone = ({ clip, trimBefore, enterAt }: { clip: string; trimBefore: number; enterAt: number }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const up = spring({ frame: frame - enterAt, fps, config: { damping: 16, stiffness: 110 } });
  return (
    <div
      style={{
        position: "absolute",
        left: (1080 - PHONE_W - 2 * BEZEL) / 2,
        top: 380,
        width: PHONE_W + 2 * BEZEL,
        height: PHONE_H + 2 * BEZEL,
        padding: BEZEL,
        borderRadius: 74,
        background: C.ink,
        boxShadow: "0 44px 90px rgba(31, 42, 26, 0.38)",
        translate: `0 ${interpolate(up, [0, 1], [1600, 0])}px`,
      }}
    >
      <div style={{ width: PHONE_W, height: PHONE_H, borderRadius: 58, overflow: "hidden", background: C.cream }}>
        <Sequence from={enterAt} layout="none">
          <OffthreadVideo src={staticFile(`clips/${clip}.mp4`)} trimBefore={trimBefore} muted style={{ width: PHONE_W, height: PHONE_H }} />
        </Sequence>
      </div>
      <div
        style={{
          position: "absolute",
          top: -30,
          left: "50%",
          translate: "-50% 0",
          whiteSpace: "nowrap",
          background: C.greenDark,
          color: C.white,
          fontFamily: BODY,
          fontWeight: 700,
          fontSize: 30,
          padding: "12px 26px",
          borderRadius: 30,
        }}
      >
        Real app · recorded on Android
      </div>
    </div>
  );
};

/** The scam text as it would arrive: a stranger's number, words that sound like family. */
export const ScamText = ({ to }: { to: number }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const pop = spring({ frame: frame - 8, fps, config: { damping: 12, stiffness: 140 } });
  const leave = interpolate(frame, [to - 12, to], [1, 0], clamp);
  if (frame > to) return null;
  return (
    <div
      style={{
        position: "absolute",
        left: 90,
        right: 90,
        top: 640,
        background: C.white,
        borderRadius: 48,
        padding: "40px 48px 46px",
        boxShadow: "0 30px 70px rgba(31, 42, 26, 0.30)",
        scale: interpolate(pop, [0, 1], [0.6, 1]),
        opacity: Math.min(pop, leave),
        translate: `0 ${interpolate(leave, [0, 1], [-60, 0])}px`,
      }}
    >
      <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 34, color: C.inkSoft, marginBottom: 16 }}>+44 7700 900123 · now</div>
      <div style={{ fontFamily: BODY, fontSize: 52, lineHeight: 1.25, color: C.ink }}>
        Hi Mum, it's Ana. My phone broke and this is my new number. Can you send me 380 today?
      </div>
    </div>
  );
};

/** Peri waves goodbye with her own frames (Muse Image), then stands calm. */
export const PeriWave = ({ from }: { from: number }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const pop = spring({ frame: frame - from, fps, config: { damping: 11, stiffness: 120 } });
  const t = frame - from;
  const picture = t > 66 ? "peri_calm" : Math.floor(t / 8) % 2 === 0 ? "peri_calm_wave1" : "peri_calm_wave2";
  return (
    <Img
      src={staticFile(`peri/${picture}.webp`)}
      style={{ position: "absolute", left: 240, top: 330, width: 600, height: 600, scale: interpolate(pop, [0, 1], [0.3, 1]), opacity: pop }}
    />
  );
};
