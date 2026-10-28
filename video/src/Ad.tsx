import { AbsoluteFill, interpolate, Sequence, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { Backdrop, Caption, Phone, PeriWave, ScamText } from "./parts";
import { BODY, C, clamp, HEADING } from "./theme";

/**
 * The ad, 20 seconds: a "new number" text, Peri warning on the real app, a QR trap on a parking meter read without
 * opening it, and Peri saying goodbye. Every screen of the app is a real recording; the places and Peri herself are
 * Muse Image pictures.
 */
export const Ad = () => {
  const { fps } = useVideoConfig();
  return (
    <AbsoluteFill style={{ background: C.cream }}>
      <Sequence durationInFrames={290} name="New number">
        <Backdrop name="home" length={290} />
        <ScamText to={104} />
        <Caption text="Sound familiar?" from={14} to={100} />
        <Phone clip="newnumber" trimBefore={3.2 * fps} enterAt={100} />
        <Caption text="Peri spots the trap before you reply." from={108} to={290} />
      </Sequence>
      <Sequence from={280} durationInFrames={190} name="QR trap">
        <FadeIn>
          <Backdrop name="street" length={190} />
          <Caption text="A QR code on a parking meter?" from={4} to={84} />
          <Phone clip="qrtrap" trimBefore={6.5 * fps} enterAt={6} />
          <Caption text="Peri reads it without opening it." from={88} to={190} />
        </FadeIn>
      </Sequence>
      <Sequence from={460} name="Goodbye">
        <FadeIn>
          <Backdrop name="sky" length={140} />
          <PeriWave from={6} />
          <EndCard />
        </FadeIn>
      </Sequence>
    </AbsoluteFill>
  );
};

/** Each place comes in over the last one. */
const FadeIn = ({ children }: { children: React.ReactNode }) => {
  const frame = useCurrentFrame();
  return <AbsoluteFill style={{ opacity: interpolate(frame, [0, 12], [0, 1], clamp) }}>{children}</AbsoluteFill>;
};

const EndCard = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const rise = spring({ frame: frame - 18, fps, config: { damping: 200 } });
  return (
    <>
      <div
        style={{
          position: "absolute",
          left: 90,
          right: 90,
          top: 960,
          padding: "44px 40px 48px",
          borderRadius: 52,
          background: C.cream,
          boxShadow: "0 30px 70px rgba(31, 42, 26, 0.25)",
          textAlign: "center",
          opacity: rise,
          translate: `0 ${interpolate(rise, [0, 1], [60, 0])}px`,
        }}
      >
        <div style={{ fontFamily: HEADING, fontWeight: 700, fontSize: 118, color: C.greenDark, lineHeight: 1 }}>FriendlyPear</div>
        <div style={{ fontFamily: BODY, fontWeight: 700, fontSize: 54, color: C.ink, marginTop: 22 }}>Your family's scam shield</div>
        <div style={{ fontFamily: BODY, fontSize: 36, color: C.inkSoft, marginTop: 22 }}>Built with Meta's Muse models · Open source</div>
      </div>
      <div
        style={{
          position: "absolute",
          left: 80,
          right: 80,
          bottom: 60,
          padding: "18px 30px",
          borderRadius: 28,
          background: "rgba(255, 248, 236, 0.92)",
          textAlign: "center",
          textWrap: "balance",
          fontFamily: BODY,
          fontSize: 29,
          lineHeight: 1.35,
          color: C.ink,
          opacity: interpolate(frame, [30, 44], [0, 1], clamp),
        }}
      >
        The app's screens are real recordings on Android emulators. Peri and the places are made with Muse Image.
      </div>
    </>
  );
};
