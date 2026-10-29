import { expect, test } from "bun:test";
import { randomBytes } from "node:crypto";
import { mkdtempSync, readFileSync, rmSync, statSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { copyKey, keyText, opens } from "../src/keycopy";
import { Vault, readStore, writeStore } from "../src/vault";

const dir = (name: string) => mkdtempSync(join(tmpdir(), `${name}-`));

test("the restore drill: the server's key lost, its copy opens every sealed file again", () => {
  const home = dir("home");
  const keyFile = join(home, "data.key");
  const data = dir("data");
  // The server's first start makes the key, then seals what it keeps.
  const vault = Vault.fromEnv({ DATA_KEY_FILE: keyFile });
  writeStore(join(data, "pears.json"), { pears: [{ name: "Mamá" }] }, vault);
  writeStore(join(data, "guardians.json"), { links: 2 }, vault);

  const copy = copyKey(keyText({ DATA_KEY_FILE: keyFile })!, data, dir("usb"), new Date("2026-10-29T12:00:00Z"));
  expect(copy.endsWith("friendlypear-data-key-2026-10-29.key")).toBe(true);
  if (process.platform !== "win32") expect(statSync(copy).mode & 0o077).toBe(0);

  // The disk with the key is gone: the copy alone opens the data again.
  rmSync(home, { recursive: true });
  const restored = new Vault(Buffer.from(readFileSync(copy, "utf8").trim(), "base64"));
  expect(readStore(join(data, "pears.json"), restored)).toEqual({ pears: [{ name: "Mamá" }] });
  expect(opens(data, readFileSync(copy, "utf8").trim())).toEqual({ opened: 2, sealed: 2 });
});

test("a key that isn't the data's is never copied as if it were", () => {
  const data = dir("data");
  writeStore(join(data, "pears.json"), { pears: [] }, new Vault(randomBytes(32)));
  const other = randomBytes(32).toString("base64");
  expect(opens(data, other)).toEqual({ opened: 0, sealed: 1 });
  expect(() => copyKey(other, data, dir("usb"))).toThrow("isn't the data's key");
});

test("DATA_KEY wins over the key file, as on the server; no key yet is said plainly", () => {
  const key = randomBytes(32).toString("base64");
  expect(keyText({ DATA_KEY: key, DATA_KEY_FILE: "/nowhere" })).toBe(key);
  expect(keyText({ DATA_KEY_FILE: join(dir("home"), "missing.key") })).toBeNull();
});
