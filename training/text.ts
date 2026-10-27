/** Tiny text helpers for the training scripts (mirrors core's Text.fold idea). */
export const Text = {
  /** Lowercase, no accents, collapsed spaces: for deduplicating near-identical messages. */
  fold(s: string): string {
    return s
      .normalize("NFD")
      .replace(/[̀-ͯ]/g, "")
      .toLowerCase()
      .replace(/[^a-z0-9€£$%]+/g, " ")
      .trim();
  },
};
