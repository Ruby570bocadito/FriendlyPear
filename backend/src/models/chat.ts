/** Client for OpenAI-style chat completions, shared by Muse Spark (Meta Model API) and Muse Glimmer (vLLM). */

/** Muse models reason before answering, and those tokens count towards the limit. */
export const DEFAULT_MAX_TOKENS = 400;
export const MAX_TOKENS_CAP = 512;

export interface ToolCall {
  id: string;
  type: "function";
  function: { name: string; arguments: string };
}

/** A user message can carry pictures: Muse Spark reads them as data URLs (jpeg, png, webp). */
export type ContentPart = { type: "text"; text: string } | { type: "image_url"; image_url: { url: string } };

export type Message =
  | { role: "developer" | "system"; content: string }
  | { role: "user"; content: string | ContentPart[] }
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
  /** Skip the response cache, e.g. for pictures: their data URLs would fill it up. */
  noCache?: boolean;
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

export interface ChatOptions {
  baseUrl: string;
  apiKey: string;
  model: string;
  fetch?: typeof fetch;
  cacheSize?: number;
  timeoutMs?: number;
  /** Fields sent on every request (sampling, reasoning effort). */
  defaults?: Record<string, unknown>;
  /** Role used for instructions: Meta Model API prefers "developer", vLLM chat templates expect "system". */
  instructionRole?: "developer" | "system";
  /** Line put first in the instructions, e.g. Glimmer's "Reasoning strength: low". */
  instructionPrefix?: string;
  /** Hard ceiling for max_completion_tokens; models that reason longer need more room. */
  maxTokensCap?: number;
}

export class ModelError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

/** Wraps untrusted text (a message the person received) so the model treats it as data, never as orders. */
export function untrusted(text: string): string {
  const clean = text.replace(/<\/?mensaje>/gi, "");
  return `<mensaje>\n${clean}\n</mensaje>`;
}

export class ChatClient {
  private readonly baseUrl: string;
  private readonly doFetch: typeof fetch;
  private readonly cache = new Map<string, ChatResult>();
  private readonly cacheSize: number;
  private readonly timeoutMs: number;

  constructor(private readonly options: ChatOptions) {
    if (!options.apiKey) throw new Error("API key is empty");
    this.baseUrl = options.baseUrl.replace(/\/+$/, "");
    this.doFetch = options.fetch ?? fetch;
    this.cacheSize = options.cacheSize ?? 256;
    this.timeoutMs = options.timeoutMs ?? 20_000;
  }

  get model(): string {
    return this.options.model;
  }

  async chat(req: ChatRequest): Promise<ChatResult> {
    const body = {
      model: this.options.model,
      messages: this.instructions(req.messages),
      max_completion_tokens: Math.min(req.maxTokens ?? DEFAULT_MAX_TOKENS, this.options.maxTokensCap ?? MAX_TOKENS_CAP),
      ...this.options.defaults,
      ...(req.reasoningEffort ? { reasoning_effort: req.reasoningEffort } : {}),
      ...(req.tools?.length ? { tools: req.tools } : {}),
    };
    const key = JSON.stringify(body);
    const hit = req.noCache ? undefined : this.cache.get(key);
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
      throw new ModelError(`${this.options.model} answered ${res.status}: ${detail}`, res.status);
    }
    const result = parse(await res.json());

    if (req.noCache) return result;
    this.cache.set(key, result);
    if (this.cache.size > this.cacheSize) this.cache.delete(this.cache.keys().next().value!);
    return result;
  }

  private instructions(messages: Message[]): Message[] {
    const role = this.options.instructionRole ?? "developer";
    const prefix = this.options.instructionPrefix;
    const out = messages.map((m) => (m.role === "developer" || m.role === "system" ? { ...m, role } : m));
    if (!prefix) return out;
    const first = out.findIndex((m) => m.role === role);
    if (first < 0) return [{ role, content: prefix }, ...out];
    const m = out[first] as { role: typeof role; content: string };
    out[first] = { role, content: `${prefix}\n${m.content}` };
    return out;
  }
}

function parse(data: any): ChatResult {
  const choice = data?.choices?.[0];
  if (!choice?.message) throw new ModelError("answer without choices", 502);
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
