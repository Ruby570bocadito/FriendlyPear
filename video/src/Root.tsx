import { Composition } from "remotion";
import { Ad } from "./Ad";
import { Demo, DEMO_FRAMES } from "./Demo";
import { Final, FINAL_FRAMES } from "./Final";
import { Diary, DIARY_FRAMES } from "./Diary";
import { Interview, INTERVIEW_FRAMES } from "./Interview";
import { Minute, MINUTE_FRAMES } from "./Minute";
import { Post } from "./Post";
import { Reel, REEL_FRAMES } from "./Reel";
import { Short, SHORT_FRAMES } from "./Short";
import { StoryTeaser } from "./Story";
import { Submission, SUBMISSION_FRAMES } from "./Submission";
import { Thirty, THIRTY_FRAMES } from "./Thirty";

/** The ad (20 s, for X and the gallery) and the first minute of Peri's story, both vertical; the 3-minute demo, 16:9. */
export const Root = () => (
  <>
    <Composition id="Ad" component={Ad} durationInFrames={600} fps={30} width={1080} height={1920} />
    <Composition id="StoryTeaser" component={StoryTeaser} durationInFrames={960} fps={60} width={1080} height={1920} />
    <Composition id="Demo" component={Demo} durationInFrames={DEMO_FRAMES} fps={60} width={1920} height={1080} />
    <Composition id="Minute" component={Minute} durationInFrames={MINUTE_FRAMES} fps={30} width={1920} height={1080} />
    <Composition id="Thirty" component={Thirty} durationInFrames={THIRTY_FRAMES} fps={30} width={1920} height={1080} />
    <Composition id="Final" component={Final} durationInFrames={FINAL_FRAMES} fps={30} width={1920} height={1080} />
    <Composition id="Diary" component={Diary} durationInFrames={DIARY_FRAMES} fps={30} width={1920} height={1080} />
    <Composition id="Submission" component={Submission} durationInFrames={SUBMISSION_FRAMES} fps={30} width={1920} height={1080} />
    <Composition id="Interview" component={Interview} durationInFrames={INTERVIEW_FRAMES} fps={30} width={1080} height={1920} />
    <Composition id="Short" component={Short} durationInFrames={SHORT_FRAMES} fps={30} width={1080} height={1920} />
    <Composition id="Post" component={Post} durationInFrames={1} fps={30} width={1080} height={1350} />
    <Composition id="Reel" component={Reel} durationInFrames={REEL_FRAMES} fps={30} width={1080} height={1920} />
  </>
);
