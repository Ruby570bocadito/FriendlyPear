"""
Product clips for the demo video, 1920x1080 at 30 fps: the real screen recordings of the emulators (one phone, or
Mum's and Ana's side by side), on Peri's cream, with an English caption per step and a fixed note that it's a
real recording. Times are the recordings' own timestamps (Android records at a variable frame rate).

    python docs/demo/tools/compose.py dist/demo/raw dist/demo [name ...]   (needs opencv-python and Pillow)
"""
import os
import sys

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

W, H, FPS = 1920, 1080, 30
CREAM, INK, INK_SOFT, GREEN, LINE = (255, 248, 236), (31, 42, 26), (63, 77, 57), (37, 84, 26), (216, 204, 178)
F = r"C:\Windows\Fonts"
BRAND = ImageFont.truetype(F + r"\segoeuib.ttf", 34)
TITLE = ImageFont.truetype(F + r"\segoeuib.ttf", 56)
LINE_F = ImageFont.truetype(F + r"\segoeui.ttf", 38)
LABEL = ImageFont.truetype(F + r"\segoeui.ttf", 28)
NOTE = ImageFont.truetype(F + r"\segoeui.ttf", 24)
PHONE_H = 920

# name: (phones, [(from_s, to_s, title, line)], phone labels)
CLIPS = {
    "m1_newnumber": (["m1_newnumber"], [
        (3.0, 7.0, "A text from a number Mum hasn't saved", "\"Hi Mum, it's Ana. My phone broke... can you send me 380?\""),
        (7.0, 20.5, "Peri: it claims to be family, from a new number", "Don't do anything yet. Let's call Ana on her usual number."),
        (20.5, 29.5, "Mum says \"yes\": Peri calls the real Ana", "Mini Peri stays beside the call."),
        (29.5, 99, "A shield for being careful", "Peri congratulates only once all is calm."),
    ], ["Mum's phone"]),
    "m2_call": (["m2_call"], [
        (2.0, 12.0, "A number Mum hasn't saved is calling", "Before she picks up, Peri warns: no money, no codes."),
        (12.0, 23.0, "She hangs up. Who did they say they were?", "Peri asks, so the call can be checked."),
        (23.0, 33.0, "\"The bank\"", "Hang up and call the official number yourself. What did they ask for?"),
        (33.0, 99, "\"They wanted the code they texted me\"", "That's the trick. Peri offers to call Ana."),
    ], ["Mum's phone"]),
    "m3_code": (["m3_code"], [
        (2.0, 12.0, "On the phone with a number she hasn't saved", "Peri warned her before she picked up."),
        (12.0, 99, "A code arrives during the call", "Peri: please hang up now. Nobody genuine asks for it."),
    ], ["Mum's phone"]),
    "m4_picture": (["m4_picture"], [
        (0.0, 9.0, "Mum shows Peri a letter", "Through Android's photo picker: no storage permission."),
        (9.0, 13.0, "Muse Spark reads it", "\"Let me look at it carefully...\""),
        (13.0, 99, "SAM 3.1 rings the QR code in red", "Don't scan it, don't open the link, don't pay anything."),
    ], ["Mum's phone"]),
    "m5_fallen": (["m5_fallen"], [
        (2.0, 14.0, "\"I've fallen\"", "Are you OK? Say \"cancel\", or in ten seconds I'll alert Ana."),
        (14.0, 99, "No answer: a text with her location, and a call to Ana", "Peri never dials emergency services by herself."),
    ], ["Mum's phone"]),
    "m6_family": (["m6_family_mum", "m6_family_ana"], [
        (0.0, 9.0, "A stranger calls Mum, \"from the bank\"", "Ana, her daughter, has FriendlyPear too."),
        (9.0, 21.0, "Mum hangs up. Peri asks who it was", "\"The bank\" - \"What did they ask you for?\""),
        (21.0, 34.0, "\"Tell Marisa the code is 7350\"", "Sending a code is delicate: Peri asks Ana first."),
        (34.0, 45.0, "URGENT: right after a stranger's call", "The text to Ana never contains the code."),
        (45.0, 53.0, "Ana's own Peri opens the request", "Checked with Peri's server, never trusted from the text."),
        (53.0, 99, "Ana: \"Call Mamá now\"", "Mum's Peri sends nothing and offers to call Ana."),
    ], ["Mum's phone", "Ana's phone (her daughter)"]),
    "m7_after": (["m7_after"], [
        (2.0, 18.0, "A fake Royal Mail text", "Peri: it says it's from Royal Mail, but it isn't."),
        (18.0, 36.0, "\"See the trap\": how it works, in three panels", "Made once with Muse Image, so nothing is sent to show them."),
        (36.0, 99, "\"Report it\": Muse Spark writes it up", "Ready for the police. Where to report is fixed text."),
    ], ["Mum's phone"]),
}


class Phone:
    """A recording read in order, giving the frame on screen at any time (by the file's own timestamps)."""

    def __init__(self, path: str):
        self.cap = cv2.VideoCapture(path)
        self.frame = None
        self.next_frame, self.next_t = self._read()
        self.done = False

    def _read(self):
        ok, f = self.cap.read()
        if not ok:
            return None, None
        return f, self.cap.get(cv2.CAP_PROP_POS_MSEC) / 1000.0

    def at(self, t: float):
        while self.next_frame is not None and self.next_t <= t:
            self.frame = self.next_frame
            self.next_frame, self.next_t = self._read()
        if self.next_frame is None:
            self.done = True
        return self.frame

    @property
    def length(self) -> float:
        n = self.cap.get(cv2.CAP_PROP_FRAME_COUNT)
        fps = self.cap.get(cv2.CAP_PROP_FPS) or 30
        return n / fps


def rounded_mask(w: int, h: int, r: int) -> np.ndarray:
    m = Image.new("L", (w, h), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, w - 1, h - 1), r, fill=255)
    return (np.asarray(m, dtype=np.float32) / 255.0)[..., None]


def wrap(draw, text, font, width):
    words, lines, cur = text.split(), [], ""
    for w in words:
        test = (cur + " " + w).strip()
        if draw.textlength(test, font=font) <= width:
            cur = test
        else:
            lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)
    return lines


def background(n: int, labels, caption) -> np.ndarray:
    im = Image.new("RGB", (W, H), CREAM)
    d = ImageDraw.Draw(im)
    pw = int(PHONE_H * 1080 / 2400)
    xs = phone_xs(n, pw)
    for x, label in zip(xs, labels):
        d.text((x + 6, 40), label, font=LABEL, fill=INK_SOFT)
        d.rounded_rectangle((x - 2, 88, x + pw + 1, 88 + PHONE_H + 1), 34, outline=LINE, width=2)
    tx = xs[-1] + pw + 90
    tw = W - tx - 80
    d.text((tx, 110), "FriendlyPear", font=BRAND, fill=GREEN)
    if caption:
        title, line = caption
        y = 300
        for l in wrap(d, title, TITLE, tw):
            d.text((tx, y), l, font=TITLE, fill=INK)
            y += 70
        y += 24
        for l in wrap(d, line, LINE_F, tw):
            d.text((tx, y), l, font=LINE_F, fill=GREEN)
            y += 52
    # Under the phones, across the whole width, so it never gets cut off.
    d.text((xs[0], H - 52), "Recorded live on Android emulators; voice orders typed in, as the emulator has no microphone.", font=NOTE, fill=INK_SOFT)
    return cv2.cvtColor(np.asarray(im), cv2.COLOR_RGB2BGR)


def phone_xs(n: int, pw: int):
    return [90] if n == 1 else [70, 70 + pw + 40]


def compose(name: str, src: str, out: str):
    files, captions, labels = CLIPS[name]
    phones = [Phone(os.path.join(src, f + ".mp4")) for f in files]
    length = max(p.length for p in phones)
    pw = int(PHONE_H * 1080 / 2400)
    xs = phone_xs(len(phones), pw)
    mask = rounded_mask(pw, PHONE_H, 32)
    writer = cv2.VideoWriter(os.path.join(out, name + ".mp4"), cv2.VideoWriter_fourcc(*"mp4v"), FPS, (W, H))
    cache = {}
    last = [None] * len(phones)
    t = 0.0
    frames = 0
    while t < length:
        cap = next(((a, b) for (s, e, a, b) in captions if s <= t < e), None)
        if cap not in cache:
            cache[cap] = background(len(phones), labels, cap)
        canvas = cache[cap].copy()
        for i, p in enumerate(phones):
            f = p.at(t)
            if f is not None:
                last[i] = cv2.resize(f, (pw, PHONE_H), interpolation=cv2.INTER_AREA)
            if last[i] is not None:
                region = canvas[89:89 + PHONE_H, xs[i]:xs[i] + pw].astype(np.float32)
                canvas[89:89 + PHONE_H, xs[i]:xs[i] + pw] = (last[i] * mask + region * (1 - mask)).astype(np.uint8)
        writer.write(canvas)
        frames += 1
        t = frames / FPS
    writer.release()
    print(f"{name}: {frames / FPS:.1f}s")


if __name__ == "__main__":
    src, out = sys.argv[1], sys.argv[2]
    os.makedirs(out, exist_ok=True)
    for name in sys.argv[3:] or CLIPS:
        compose(name, src, out)
