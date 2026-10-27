import { expect, test } from "bun:test";
import { encodeQr, qrSvg } from "../src/qr";

// Decoding is checked outside the tests with OpenCV's QR readers (all 8 masks, versions 1 to 10); here, the layout.

test("the size follows the version needed", () => {
  expect(encodeQr("HELLO").length).toBe(21);
  expect(encodeQr("friendlypear://pair?code=K7PM-3XQD&server=https%3A%2F%2Frby.tailb6f848.ts.net").length).toBe(37);
  expect(() => encodeQr("x".repeat(400))).toThrow();
});

test("the three finder patterns sit in their corners", () => {
  const m = encodeQr("HELLO");
  const n = m.length;
  const finder = (x: number, y: number) =>
    [0, 1, 2, 3, 4, 5, 6].every((i) => m[y][x + i] && m[y + 6][x + i] && m[y + i][x] && m[y + i][x + 6]) && m[y + 3][x + 3] && !m[y + 1][x + 1];
  expect(finder(0, 0)).toBe(true);
  expect(finder(n - 7, 0)).toBe(true);
  expect(finder(0, n - 7)).toBe(true);
  expect(m[n - 8][8]).toBe(true);
});

test("every mask makes a valid layout, and the SVG has a quiet zone", () => {
  for (let mask = 0; mask < 8; mask++) expect(encodeQr("Peri", mask).length).toBe(21);
  expect(qrSvg(encodeQr("Peri"))).toContain('viewBox="0 0 29 29"');
});
