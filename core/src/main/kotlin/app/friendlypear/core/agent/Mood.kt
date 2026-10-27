package app.friendlypear.core.agent

/**
 * Peri's face, one illustration each (assets/peri/). The agent sets it as it speaks; the phone adds
 * LISTENING while the microphone is open and SLEEPING at night when nothing is going on.
 */
enum class Mood { CALM, LISTENING, THINKING, WORRIED, PROTECTIVE, CALLING, WAITING, HAPPY, SLEEPING }
