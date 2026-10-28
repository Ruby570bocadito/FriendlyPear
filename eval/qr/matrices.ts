// Payloads (JSON file) -> QR matrices (JSON file), with the backend's own encoder.
import { encodeQr } from "../../backend/src/qr.ts";

const payloads: Record<string, string> = JSON.parse(await Bun.file(process.argv[2]).text());
const out: Record<string, boolean[][]> = {};
for (const [name, text] of Object.entries(payloads)) out[name] = encodeQr(text);
await Bun.write(process.argv[3], JSON.stringify(out));
console.log(Object.keys(out).length, "matrices");
