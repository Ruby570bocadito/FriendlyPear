/**
 * SAM 3.1 on the Meta Model API (`/v1/responses`): finds every match of a short noun phrase ("QR code") in a
 * picture and returns its box. The answer is special-token text, one record per match:
 * `<|box;x1=..;y1=..;x2=..;y2=..;w=<W>;h=<H>[;c=<conf>]|><|mask;…|>`. Boxes are in source pixels; masks are
 * not used here (the phone draws a soft highlight around each box).
 */
import { ModelError } from "./chat";

export const SAM_MODEL = "sam-3.1";

export interface Box {
  /** Corners as fractions of the picture's width and height, 0..1, so the phone can draw them at any size. */
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  confidence: number | null;
}

const BOX = /<\|box;x1=(\d+(?:\.\d+)?);y1=(\d+(?:\.\d+)?);x2=(\d+(?:\.\d+)?);y2=(\d+(?:\.\d+)?);w=(\d+);h=(\d+)(?:;c=([\d.]+))?\|>/g;

/** The boxes in SAM's output text, normalised; anything malformed is skipped. */
export function parseBoxes(text: string): Box[] {
  const boxes: Box[] = [];
  for (const m of text.matchAll(BOX)) {
    const [x1, y1, x2, y2, w, h] = m.slice(1, 7).map(Number);
    if (!(w > 0 && h > 0) || x2 <= x1 || y2 <= y1) continue;
    const clamp = (v: number) => Math.min(1, Math.max(0, v));
    boxes.push({
      x1: clamp(x1 / w),
      y1: clamp(y1 / h),
      x2: clamp(x2 / w),
      y2: clamp(y2 / h),
      confidence: m[7] === undefined ? null : Number(m[7]),
    });
  }
  return boxes;
}

export class SamClient {
  private readonly doFetch: typeof fetch;

  constructor(
    private readonly options: { baseUrl: string; apiKey: string; fetch?: typeof fetch; timeoutMs?: number },
  ) {
    if (!options.apiKey) throw new Error("API key is empty");
    this.doFetch = options.fetch ?? fetch;
  }

  /** One concept per request, as SAM's guide asks. [imageUrl] is a data URL. */
  async find(phrase: string, imageUrl: string): Promise<Box[]> {
    const res = await this.doFetch(`${this.options.baseUrl.replace(/\/+$/, "")}/responses`, {
      method: "POST",
      headers: { authorization: `Bearer ${this.options.apiKey}`, "content-type": "application/json" },
      body: JSON.stringify({
        model: SAM_MODEL,
        input: [{ type: "message", role: "user", content: [{ type: "input_text", text: phrase }, { type: "input_image", image_url: imageUrl }] }],
      }),
      signal: AbortSignal.timeout(this.options.timeoutMs ?? 20_000),
    });
    if (!res.ok) throw new ModelError(`${SAM_MODEL} answered ${res.status}`, res.status);
    const data: any = await res.json();
    const text = (data.output ?? [])
      .flatMap((o: any) => o.content ?? [])
      .filter((c: any) => c.type === "output_text")
      .map((c: any) => c.text)
      .join("\n");
    return parseBoxes(text);
  }
}

export function samFromEnv(env: Record<string, string | undefined> = process.env): SamClient | null {
  if (!env.META_API_KEY || !env.META_API_BASE) return null;
  return new SamClient({ baseUrl: env.META_API_BASE, apiKey: env.META_API_KEY });
}
