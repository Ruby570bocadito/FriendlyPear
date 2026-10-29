import { Composition } from "remotion";
import { Ad } from "./Ad";
import { Demo, DEMO_FRAMES } from "./Demo";
import { StoryTeaser } from "./Story";

/** The ad (20 s, for X and the gallery) and the first minute of Peri's story, both vertical; the 3-minute demo, 16:9. */
export const Root = () => (
  <>
    <Composition id="Ad" component={Ad} durationInFrames={600} fps={30} width={1080} height={1920} />
    <Composition id="StoryTeaser" component={StoryTeaser} durationInFrames={960} fps={60} width={1080} height={1920} />
    <Composition id="Demo" component={Demo} durationInFrames={DEMO_FRAMES} fps={60} width={1920} height={1080} />
  </>
);
