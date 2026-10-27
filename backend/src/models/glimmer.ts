/** Muse Glimmer 30B served with vLLM (Baseten, Modal or a local machine). */
import { ChatClient, type ChatOptions } from "./chat";

export const GLIMMER_MODEL = "meta-models/Muse-Glimmer-30B";

/** Sampling recommended by the model card; greedy decoding is not reproducible on Glimmer. */
const SAMPLING = { temperature: 1.0, top_p: 0.95, top_k: 64 };

export type GlimmerOptions = Omit<ChatOptions, "model" | "defaults" | "instructionRole" | "instructionPrefix"> & {
  model?: string;
  reasoning?: "low" | "medium" | "high" | "xhigh";
};

export class GlimmerClient extends ChatClient {
  constructor({ reasoning = "low", ...options }: GlimmerOptions) {
    super({
      ...options,
      model: options.model ?? GLIMMER_MODEL,
      defaults: SAMPLING,
      instructionRole: "system",
      instructionPrefix: `Reasoning strength: ${reasoning}`,
      timeoutMs: options.timeoutMs ?? 60_000,
    });
  }
}

export function basetenUrl(modelId: string): string {
  return `https://model-${modelId}.api.baseten.co/environments/production/sync/v1`;
}

/** GLIMMER_BASE_URL (any vLLM server) wins over BASETEN_MODEL_ID. */
export function glimmerFromEnv(env: Record<string, string | undefined> = process.env): GlimmerClient | null {
  const baseUrl = env.GLIMMER_BASE_URL || (env.BASETEN_MODEL_ID ? basetenUrl(env.BASETEN_MODEL_ID) : "");
  const apiKey = env.GLIMMER_API_KEY || env.BASETEN_API_KEY || "";
  if (!baseUrl || !apiKey) return null;
  return new GlimmerClient({ baseUrl, apiKey, model: env.GLIMMER_MODEL });
}
