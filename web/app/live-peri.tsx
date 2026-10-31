'use client';

import { useEffect, useImperativeHandle, useRef, forwardRef } from 'react';
import rig from './peri-rig.json';

// Peri as a live character, the way a Rive state machine would drive her: in her calm mood she is a rig (body, two
// eyes and their shines, from web/scripts/peri_rig.py), so her eyes follow the cursor and blink; every mood breathes,
// leans towards the cursor, blinks and jumps with clay stars when tapped. "Reduce motion" keeps her still.

export type Mood = 'calm' | 'happy' | 'worried' | 'protective' | 'thinking' | 'listening';
export type PeriHandle = { celebrate: () => void; hop: () => void };

const EYE_TRAVEL = 0.26; // how far an eye moves, as a share of its radius
const SHINE_TRAVEL = 0.62;
const STARS = 7;

const pct = (v: number) => `${(v * 100).toFixed(3)}%`;

const LivePeri = forwardRef<PeriHandle, { mood?: Mood; label: string; onTap?: () => void; className?: string }>(
  function LivePeri({ mood = 'calm', label, onTap, className = '' }, handle) {
    const root = useRef<HTMLButtonElement>(null);
    const lean = useRef<HTMLDivElement>(null);
    const eyes = useRef<(HTMLDivElement | null)[]>([]);
    const shines = useRef<(HTMLImageElement | null)[]>([]);
    const face = useRef<HTMLImageElement>(null);
    const stars = useRef<HTMLDivElement>(null);
    const moodRef = useRef(mood);
    moodRef.current = mood;

    const burst = () => {
      const layer = stars.current;
      if (!layer || matchMedia('(prefers-reduced-motion: reduce)').matches) return;
      layer.replaceChildren();
      for (let i = 0; i < STARS; i++) {
        const star = document.createElement('img');
        star.src = 'peri/star.webp';
        star.alt = '';
        const angle = -Math.PI / 2 + (i / (STARS - 1) - 0.5) * Math.PI * 1.3;
        const reach = 300 + Math.random() * 220; // as a share of a star's own width
        star.style.setProperty('--dx', `${Math.cos(angle) * reach}%`);
        star.style.setProperty('--dy', `${Math.sin(angle) * reach}%`);
        star.style.setProperty('--spin', `${Math.round(Math.random() * 240 - 120)}deg`);
        star.style.animationDelay = `${i * 25}ms`;
        layer.append(star);
      }
    };
    const hop = () => {
      const el = root.current;
      if (!el || matchMedia('(prefers-reduced-motion: reduce)').matches) return;
      el.classList.remove('peri-hop');
      void el.offsetWidth; // restart the animation
      el.classList.add('peri-hop');
    };
    useImperativeHandle(handle, () => ({ hop, celebrate: () => { hop(); burst(); } }));

    useEffect(() => {
      const el = root.current!;
      const still = matchMedia('(prefers-reduced-motion: reduce)');
      let target = { x: 0, y: 0 };
      let gaze = { x: 0, y: 0 };
      let frame = 0;
      let lastPointer = 0;
      let blinkTimer = 0;
      let glanceTimer = 0;
      let disposed = false;

      const render = () => {
        frame = 0;
        // A soft spring towards where she wants to look.
        gaze = { x: gaze.x + (target.x - gaze.x) * 0.18, y: gaze.y + (target.y - gaze.y) * 0.18 };
        rig.eyes.forEach((eye, i) => {
          const r = eye.r / eye.size; // radius as a share of the eye's own box
          eyes.current[i]?.style.setProperty('translate', `${(gaze.x * EYE_TRAVEL * r * 100).toFixed(2)}% ${(gaze.y * EYE_TRAVEL * r * 100).toFixed(2)}%`);
          const shine = shines.current[i];
          if (shine) shine.style.translate = `${((gaze.x * SHINE_TRAVEL * eye.r) / eye.shine.w * 100).toFixed(1)}% ${((gaze.y * SHINE_TRAVEL * eye.r) / eye.shine.h * 100).toFixed(1)}%`;
        });
        if (!still.matches) lean.current?.style.setProperty('--lean', `${(gaze.x * 3).toFixed(2)}deg`);
        if (Math.abs(target.x - gaze.x) + Math.abs(target.y - gaze.y) > 0.002) frame = requestAnimationFrame(render);
      };
      const look = (x: number, y: number) => {
        target = { x, y };
        if (!frame) frame = requestAnimationFrame(render);
      };
      const onPointer = (event: PointerEvent) => {
        if (event.pointerType === 'touch') return;
        lastPointer = performance.now();
        const box = el.getBoundingClientRect();
        if (!box.width || box.bottom < 0 || box.top > innerHeight) return;
        const cx = box.left + box.width * 0.5;
        const cy = box.top + box.height * rig.eyes[0].y;
        const dx = event.clientX - cx;
        const dy = event.clientY - cy;
        const distance = Math.hypot(dx, dy) || 1;
        const pull = Math.min(1, distance / (box.width * 0.9));
        look((dx / distance) * pull, (dy / distance) * pull);
      };

      const blink = () => {
        if (disposed) return;
        el.classList.add('peri-blink');
        if (face.current && moodRef.current !== 'calm') face.current.src = `peri/${moodRef.current}_blink.webp`;
        setTimeout(() => {
          el.classList.remove('peri-blink');
          if (face.current && moodRef.current !== 'calm') face.current.src = `peri/${moodRef.current}.webp`;
          // Now and then a double blink, like in the app.
          if (Math.random() < 0.2) setTimeout(() => { el.classList.add('peri-blink'); setTimeout(() => el.classList.remove('peri-blink'), 110); }, 180);
        }, 120);
        blinkTimer = window.setTimeout(blink, 2400 + Math.random() * 3600);
      };
      const glance = () => {
        if (disposed) return;
        // Without a cursor (a phone, or a still mouse), she looks around on her own.
        if (performance.now() - lastPointer > 2500) {
          const away = Math.random() < 0.35;
          look(away ? 0 : Math.random() * 1.6 - 0.8, away ? 0 : Math.random() * 0.8 - 0.3);
        }
        glanceTimer = window.setTimeout(glance, 1800 + Math.random() * 2600);
      };

      blinkTimer = window.setTimeout(blink, 1500);
      glanceTimer = window.setTimeout(glance, 2500);
      window.addEventListener('pointermove', onPointer, { passive: true });
      return () => {
        disposed = true;
        cancelAnimationFrame(frame);
        clearTimeout(blinkTimer);
        clearTimeout(glanceTimer);
        window.removeEventListener('pointermove', onPointer);
      };
    }, []);

    useEffect(() => {
      if (face.current) face.current.src = `peri/${mood}.webp`;
    }, [mood]);

    return (
      <button
        ref={root}
        type="button"
        className={`peri ${className}`}
        aria-label={label}
        onClick={() => { hop(); burst(); onTap?.(); }}
        onAnimationEnd={(e) => { if (e.animationName === 'peri-hop') root.current?.classList.remove('peri-hop'); }}
      >
        <span className="peri-shadow" aria-hidden="true" />
        <div ref={lean} className="peri-lean" aria-hidden="true">
          <div className="peri-breathe">
            <div className={`peri-rig${mood === 'calm' ? '' : ' peri-rig-hidden'}`}>
              <img className="peri-body" src="peri/body.webp" alt="" width="720" height="720" draggable={false} />
              {rig.eyes.map((eye, i) => (
                <div
                  key={i}
                  className="peri-eye"
                  style={{ left: pct(eye.x - eye.size / 2), top: pct(eye.y - eye.size / 2), width: pct(eye.size), height: pct(eye.size) }}
                >
                  <div ref={(n) => { eyes.current[i] = n; }} className="peri-eye-ball">
                    <img src={`peri/eye-${i}.webp`} alt="" draggable={false} />
                    <img
                      ref={(n) => { shines.current[i] = n; }}
                      className="peri-shine"
                      src={`peri/shine-${i}.webp`}
                      alt=""
                      draggable={false}
                      style={{
                        left: pct(0.5 + (eye.shine.x - eye.shine.w / 2) / eye.size),
                        top: pct(0.5 + (eye.shine.y - eye.shine.h / 2) / eye.size),
                        width: pct(eye.shine.w / eye.size),
                        height: pct(eye.shine.h / eye.size),
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>
            {mood !== 'calm' && <img ref={face} className="peri-mood" src={`peri/${mood}.webp`} alt="" width="720" height="720" draggable={false} />}
          </div>
        </div>
        <div ref={stars} className="peri-stars" aria-hidden="true" />
      </button>
    );
  },
);

export default LivePeri;
