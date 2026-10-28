export interface Config {
  port: number;
  /** The shared test token in debug builds; also what `bun pair` uses unless ADMIN_TOKEN is set. */
  deviceToken: string;
  publicBaseUrl: string;
  /** Who may create pairing codes and list or disconnect phones: ADMIN_TOKEN, else the shared token. */
  adminToken: string;
  /** The admin token before the last `bun rotate`, accepted until [previousAdminUntil] so nothing stops meanwhile. */
  previousAdminToken: string | null;
  previousAdminUntil: number;
  /** ALLOW_SHARED_TOKEN=0 once every phone is paired: then only per-device keys open the API. */
  allowSharedToken: boolean;
}

export function loadConfig(env: Record<string, string | undefined> = process.env): Config {
  const deviceToken = env.DEVICE_TOKEN ?? "";
  if (deviceToken.length < 32) {
    throw new Error("DEVICE_TOKEN must be a random value of at least 32 characters (see .env.example)");
  }
  return {
    port: Number(env.PORT ?? 8787),
    deviceToken,
    publicBaseUrl: (env.PUBLIC_BASE_URL ?? "http://localhost:8787").replace(/\/+$/, ""),
    adminToken: env.ADMIN_TOKEN && env.ADMIN_TOKEN.length >= 32 ? env.ADMIN_TOKEN : deviceToken,
    previousAdminToken: env.ADMIN_TOKEN_PREVIOUS && env.ADMIN_TOKEN_PREVIOUS.length >= 32 ? env.ADMIN_TOKEN_PREVIOUS : null,
    previousAdminUntil: Date.parse(env.ADMIN_TOKEN_PREVIOUS_UNTIL ?? "") || 0,
    allowSharedToken: env.ALLOW_SHARED_TOKEN !== "0",
  };
}
