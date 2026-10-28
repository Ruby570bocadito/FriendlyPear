import { loadFont } from "@remotion/fonts";
import { continueRender, delayRender, staticFile } from "remotion";

/** The app's own colours (android/.../ui/peri/Style.kt). */
export const C = {
  cream: "#FFF8EC",
  white: "#FFFFFF",
  ink: "#1F2A1A",
  inkSoft: "#3F4D39",
  green: "#7CC242",
  greenDark: "#25541A",
  sun: "#FFC83D",
  danger: "#9C1F17",
  line: "#EDE4D0",
};

/** The app's own fonts: Fredoka for headings (variable, 300 to 700), Atkinson Hyperlegible for reading. */
export const HEADING = "Fredoka";
export const BODY = "Atkinson Hyperlegible";

const fonts = delayRender("The app's fonts");
Promise.all([
  loadFont({ family: HEADING, url: staticFile("fonts/fredoka.ttf"), weight: "300 700" }),
  loadFont({ family: BODY, url: staticFile("fonts/atkinson_regular.ttf"), weight: "400" }),
  loadFont({ family: BODY, url: staticFile("fonts/atkinson_bold.ttf"), weight: "700" }),
]).then(() => continueRender(fonts));

export const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
