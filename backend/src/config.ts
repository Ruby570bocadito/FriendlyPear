export interface Config {
  port: number;
  deviceToken: string;
  publicBaseUrl: string;
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
  };
}
