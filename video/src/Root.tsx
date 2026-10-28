import { Composition } from "remotion";
import { Ad } from "./Ad";

/** The ad: 20 seconds, vertical, for X and the gallery. */
export const Root = () => <Composition id="Ad" component={Ad} durationInFrames={600} fps={30} width={1080} height={1920} />;
