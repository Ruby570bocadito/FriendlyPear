/** Muse Spark Open, Meta's open-weight model, served by Baseten's Model APIs (OpenAI-style). */
import { ChatClient, type ChatOptions } from "./chat";

export const SPARK_OPEN_MODEL = "meta-models/muse-spark-open";
export const BASETEN_BASE = "https://inference.baseten.co/v1";

export type SparkOpenOptions = Omit<ChatOptions, "model" | "instructionRole" | "maxTokensCap"> & { model?: string };

export class SparkOpenClient extends ChatClient {
  constructor(options: SparkOpenOptions) {
    super({
      ...options,
      model: options.model ?? SPARK_OPEN_MODEL,
      instructionRole: "system",
      maxTokensCap: 1024,
      timeoutMs: options.timeoutMs ?? 30_000,
    });
  }
}

export function sparkOpenFromEnv(env: Record<string, string | undefined> = process.env): SparkOpenClient | null {
  if (!env.BASETEN_API_KEY) return null;
  return new SparkOpenClient({
    baseUrl: env.BASETEN_BASE_URL || BASETEN_BASE,
    apiKey: env.BASETEN_API_KEY,
    model: env.SPARK_OPEN_MODEL,
  });
}
