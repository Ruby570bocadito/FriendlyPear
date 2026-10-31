'use client';

import { useEffect, useRef, useState } from 'react';
import LivePeri, { type PeriHandle } from './live-peri';
import lines from './peri-lines.json';

// Peri in her world, as in the app: the sky follows the visitor's hour (dawn, day, dusk, night with stars), clay
// hills rise to her feet and clouds drift by. She greets by the time of day, and every tap gets a line of hers:
// a reaction, a joke, a tip or a fact, in turn (the app's own words, from android/app/src/main/res/values/strings.xml).

type Period = 'dawn' | 'day' | 'dusk' | 'night';
const periodOf = (hour: number): Period => (hour >= 6 && hour < 9 ? 'dawn' : hour >= 9 && hour < 18 ? 'day' : hour >= 18 && hour < 21 ? 'dusk' : 'night');
const greetingOf = (hour: number) => (hour >= 5 && hour < 12 ? lines.greet.morning : hour >= 12 && hour < 18 ? lines.greet.afternoon : hour >= 18 && hour < 22 ? lines.greet.evening : lines.greet.night);
const pick = <T,>(list: T[]) => list[Math.floor(Math.random() * list.length)];
const TURNS = [lines.taps, lines.jokes, lines.tips, lines.facts];

export default function HeroWorld() {
  const [period, setPeriod] = useState<Period>('day');
  const [line, setLine] = useState('Hello! I’m Peri. Tap me!');
  const [pop, setPop] = useState(0);
  const turn = useRef(0);
  const peri = useRef<PeriHandle>(null);

  useEffect(() => {
    const hour = new Date().getHours();
    setPeriod(periodOf(hour));
    setLine(pick(greetingOf(hour)));
  }, []);

  const tapped = () => {
    setLine(pick(TURNS[turn.current % TURNS.length]));
    turn.current += 1;
    setPop((n) => n + 1);
  };

  return (
    <div className={`world world-${period}`}>
      <div className="world-sky" aria-hidden="true">
        <span className="world-sun" />
        {period === 'night' && <span className="world-stars" />}
        <span className="cloud cloud-1" />
        <span className="cloud cloud-2" />
        <span className="cloud cloud-3" />
      </div>
      <svg className="world-hills" viewBox="0 0 600 260" preserveAspectRatio="none" aria-hidden="true">
        <path className="hill-far" d="M0 120 C 90 70 170 80 250 110 C 330 140 420 60 600 90 L600 260 L0 260Z" />
        <path className="hill-mid" d="M0 170 C 120 120 220 150 300 165 C 400 185 480 120 600 140 L600 260 L0 260Z" />
        <path className="hill-near" d="M0 215 C 140 180 220 195 300 192 C 390 188 470 178 600 205 L600 260 L0 260Z" />
      </svg>
      <p key={pop} className="world-bubble" aria-live="polite">{line}</p>
      <LivePeri ref={peri} className="world-peri" label="Peri. Tap her and she says something" onTap={tapped} />
      <span className="world-hint" aria-hidden="true">Tap Peri</span>
    </div>
  );
}
