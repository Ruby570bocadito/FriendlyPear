/**
 * Muse Voice Transcribe on the Meta Model API (`POST /v1/asr/transcribe`): one short WAV clip of a voice order,
 * push-to-talk, back as text. $0.18 per hour of audio; a 4-second order costs about $0.0002.
 */
import { ModelError } from "./chat";

export const VOICE_MODEL = "muse-voice-transcribe-1.0";

export class VoiceClient {
  private readonly doFetch: typeof fetch;

  constructor(private readonly options: { baseUrl: string; apiKey: string; fetch?: typeof fetch; timeoutMs?: number }) {
    if (!options.apiKey) throw new Error("API key is empty");
    this.doFetch = options.fetch ?? fetch;
  }

  get model(): string {
    return VOICE_MODEL;
  }

  async transcribe(wav: Uint8Array): Promise<{ text: string; durationMs: number }> {
    const form = new FormData();
    form.append("request", new Blob([JSON.stringify({ model: VOICE_MODEL, audioEncoding: "WAV", mode: "PUSH_TO_TALK" })], { type: "application/json" }));
    form.append("audio", new Blob([wav], { type: "audio/wav" }), "order.wav");
    const res = await this.doFetch(`${this.options.baseUrl.replace(/\/+$/, "")}/asr/transcribe`, {
      method: "POST",
      headers: { authorization: `Bearer ${this.options.apiKey}`, accept: "application/json" },
      body: form,
      signal: AbortSignal.timeout(this.options.timeoutMs ?? 20_000),
    });
    if (!res.ok) throw new ModelError(`${VOICE_MODEL} answered ${res.status}`, res.status);
    const data: any = await res.json();
    return { text: typeof data.transcript === "string" ? data.transcript.trim() : "", durationMs: Number(data.audioDurationMs) || 0 };
  }
}

export function voiceFromEnv(env: Record<string, string | undefined> = process.env): VoiceClient | null {
  if (!env.META_API_KEY || !env.META_API_BASE) return null;
  return new VoiceClient({ baseUrl: env.META_API_BASE, apiKey: env.META_API_KEY });
}

/** A WAV the phone recorded: RIFF/WAVE header, PCM, mono, 16-bit. Anything else is refused before Meta sees it. */
export function isVoiceWav(b: Uint8Array): boolean {
  if (b.length < 44) return false;
  const text = (at: number) => String.fromCharCode(...b.subarray(at, at + 4));
  const view = new DataView(b.buffer, b.byteOffset, b.byteLength);
  return text(0) === "RIFF" && text(8) === "WAVE" && view.getUint16(20, true) === 1 && view.getUint16(22, true) === 1 && view.getUint16(34, true) === 16;
}
