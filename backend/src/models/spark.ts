/** Muse Spark on the Meta Model API. */
import { ChatClient, type ChatOptions } from "./chat";

export const SPARK_MODEL = "muse-spark-1.3";

export type SparkOptions = Omit<ChatOptions, "model" | "defaults" | "instructionRole"> & { model?: string };

export class SparkClient extends ChatClient {
  constructor(options: SparkOptions) {
    super({ ...options, model: options.model ?? SPARK_MODEL, defaults: { reasoning_effort: "minimal" }, instructionRole: "developer" });
  }
}

export function sparkFromEnv(env: Record<string, string | undefined> = process.env): SparkClient | null {
  if (!env.META_API_KEY || !env.META_API_BASE) return null;
  return new SparkClient({ baseUrl: env.META_API_BASE, apiKey: env.META_API_KEY, model: env.SPARK_MODEL });
}
