/** Muse Spark on the Meta Model API. */
import { ChatClient, type ChatOptions } from "./chat";

export const SPARK_MODEL = "muse-spark-1.3";
/** Ceiling for any Spark answer, thinking included; each call asks for less. */
export const SPARK_MAX_TOKENS = 1_536;

export type SparkOptions = Omit<ChatOptions, "model" | "defaults" | "instructionRole"> & { model?: string };

export class SparkClient extends ChatClient {
  constructor(options: SparkOptions) {
    // Spark thinks before it writes, even at "minimal": a longer answer (a report) needs room for both.
    super({ ...options, model: options.model ?? SPARK_MODEL, defaults: { reasoning_effort: "minimal" }, instructionRole: "developer", maxTokensCap: options.maxTokensCap ?? SPARK_MAX_TOKENS });
  }
}

export function sparkFromEnv(env: Record<string, string | undefined> = process.env): SparkClient | null {
  if (!env.META_API_KEY || !env.META_API_BASE) return null;
  return new SparkClient({ baseUrl: env.META_API_BASE, apiKey: env.META_API_KEY, model: env.SPARK_MODEL });
}
