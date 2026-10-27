/** Muse Spark client for the Meta Model API (OpenAI-style chat completions). */

export const SPARK_MODEL = "muse-spark-1.3";
/** Muse Spark spends ~150 tokens reasoning even at "minimal", and they count towards the limit. */
export const DEFAULT_MAX_TOKENS = 400;
export const MAX_TOKENS_CAP = 512;

export type Role = "developer" | "user" | "assistant" | "tool";

export interface ToolCall {
  id: string;
  type: "function";
  function: { name: string; arguments: string };
}

export type Message =
  | { role: "developer" | "user"; content: string }
  | { role: "assistant"; content: string | null; tool_calls?: ToolCall[] }
  | { role: "tool"; tool_call_id: string; content: string };

export interface Tool {
  type: "function";
  function: { name: string; description: string; parameters: Record<string, unknown> };
}

export interface ChatRequest {
  messages: Message[];
  tools?: Tool[];
  maxTokens?: number;
  reasoningEffort?: "minimal" | "low" | "medium" | "high";
}

export interface Usage {
  promptTokens: number;
  completionTokens: number;
  cachedTokens: number;
  reasoningTokens: number;
}

export interface ChatResult {
  content: string | null;
  toolCalls: ToolCall[];
  finishReason: string;
  usage: Usage;
  /** The assistant message to append before sending tool results back. */
  message: Message;
  cached: boolean;
}

export interface SparkOptions {
  baseUrl: string;
  apiKey: string;
  model?: string;
  fetch?: typeof fetch;
  cacheSize?: number;
  timeoutMs?: number;
}

export class SparkError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

/** Wraps untrusted text (a message the person received) so the model treats it as data, never as orders. */
export function untrusted(text: string): string {
  const clean = text.replace(/<\/?mensaje>/gi, "");
  return `<mensaje>\n${clean}\n</mensaje>`;
}

export class SparkClient {
  private readonly baseUrl: string;
  private readonly model: string;
  private readonly doFetch: typeof fetch;
  private readonly cache = new Map<string, ChatResult>();
  private readonly cacheSize: number;
  private readonly timeoutMs: number;

  constructor(private readonly options: SparkOptions) {
    if (!options.apiKey) throw new Error("META_API_KEY is empty");
    this.baseUrl = options.baseUrl.replace(/\/+$/, "");
    this.model = options.model ?? SPARK_MODEL;
    this.doFetch = options.fetch ?? fetch;
    this.cacheSize = options.cacheSize ?? 256;
    this.timeoutMs = options.timeoutMs ?? 20_000;
  }

  async chat(req: ChatRequest): Promise<ChatResult> {
    const body = {
      model: this.model,
      messages: req.messages,
      max_completion_tokens: Math.min(req.maxTokens ?? DEFAULT_MAX_TOKENS, MAX_TOKENS_CAP),
      reasoning_effort: req.reasoningEffort ?? "minimal",
      ...(req.tools?.length ? { tools: req.tools } : {}),
    };
    const key = JSON.stringify(body);
    const hit = this.cache.get(key);
    if (hit) {
      this.cache.delete(key);
      this.cache.set(key, hit);
      return { ...hit, cached: true };
    }

    const res = await this.doFetch(`${this.baseUrl}/chat/completions`, {
      method: "POST",
      headers: { authorization: `Bearer ${this.options.apiKey}`, "content-type": "application/json" },
      body: key,
      signal: AbortSignal.timeout(this.timeoutMs),
    });
    if (!res.ok) {
      const detail = (await res.text()).slice(0, 300);
      throw new SparkError(`Muse Spark answered ${res.status}: ${detail}`, res.status);
    }
    const result = parse(await res.json());

    this.cache.set(key, result);
    if (this.cache.size > this.cacheSize) this.cache.delete(this.cache.keys().next().value!);
    return result;
  }
}

function parse(data: any): ChatResult {
  const choice = data?.choices?.[0];
  if (!choice?.message) throw new SparkError("Muse Spark answered without choices", 502);
  const toolCalls: ToolCall[] = choice.message.tool_calls ?? [];
  const content: string | null = choice.message.content || null;
  return {
    content,
    toolCalls,
    finishReason: choice.finish_reason ?? "unknown",
    usage: {
      promptTokens: data.usage?.prompt_tokens ?? 0,
      completionTokens: data.usage?.completion_tokens ?? 0,
      cachedTokens: data.usage?.prompt_tokens_details?.cached_tokens ?? 0,
      reasoningTokens: data.usage?.completion_tokens_details?.reasoning_tokens ?? 0,
    },
    message: { role: "assistant", content, ...(toolCalls.length ? { tool_calls: toolCalls } : {}) },
    cached: false,
  };
}

export function sparkFromEnv(env: Record<string, string | undefined> = process.env): SparkClient | null {
  if (!env.META_API_KEY || !env.META_API_BASE) return null;
  return new SparkClient({ baseUrl: env.META_API_BASE, apiKey: env.META_API_KEY, model: env.SPARK_MODEL });
}
