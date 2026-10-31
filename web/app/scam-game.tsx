'use client';

import { useEffect, useRef, useState } from 'react';
import LivePeri, { type Mood, type PeriHandle } from './live-peri';
import lines from './peri-lines.json';

// "Scam or not?", the app's game (the same 20 messages, clues and Peri's lines): five messages, trap or fine. Peri
// reacts like her state machine in the app: she thinks while you read, cheers with stars when you're right and frets,
// kindly, when you're not, then explains the clue.

type Question = (typeof lines.quiz)[number];
const ROUND = 5;
const CHANNEL: Record<string, string> = { sms: 'Text message', whatsapp: 'WhatsApp', call: 'Phone call' };
const pick = <T,>(list: T[]) => list[Math.floor(Math.random() * list.length)];

function shuffle<T>(list: T[]) {
  const copy = [...list];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

// Three traps and two fine messages, so "it's a trap" every time doesn't win.
const newRound = () => shuffle([...shuffle(lines.quiz.filter((q) => q.scam)).slice(0, 3), ...shuffle(lines.quiz.filter((q) => !q.scam)).slice(0, 2)]);

// Links in the messages are pretend: shown as links, but plain text.
function MessageText({ text }: { text: string }) {
  return <>{text.split(/(\{[^}]+\})/).map((part, i) => part.startsWith('{') ? <span key={i} className="fake-link">{part.slice(1, -1)}</span> : part)}</>;
}

export default function ScamGame() {
  const [round, setRound] = useState<Question[]>(() => lines.quiz.slice(0, ROUND));
  const [index, setIndex] = useState(0);
  const [answer, setAnswer] = useState<boolean | null>(null);
  const [score, setScore] = useState(0);
  const [mood, setMood] = useState<Mood>('calm');
  const [line, setLine] = useState('I’ll show you some messages. You decide if they’re a trap.');
  const peri = useRef<PeriHandle>(null);
  const done = index >= ROUND;
  const question = round[Math.min(index, ROUND - 1)];

  useEffect(() => setRound(newRound()), []);

  const choose = (saysScam: boolean) => {
    const right = saysScam === question.scam;
    setAnswer(saysScam);
    if (right) {
      setScore((s) => s + 1);
      setMood('happy');
      setLine(pick(lines.quizRight));
      peri.current?.celebrate();
    } else {
      setMood('worried');
      setLine(pick(lines.quizWrong));
      peri.current?.hop();
    }
  };
  const next = () => {
    const last = index + 1 >= ROUND;
    setIndex(index + 1);
    setAnswer(null);
    if (last) {
      const final = score;
      setMood(final >= 4 ? 'happy' : 'calm');
      setLine(pick(final >= 4 ? lines.quizEndGreat : final >= 3 ? lines.quizEndGood : lines.quizEndLow));
      if (final >= 4) peri.current?.celebrate();
    } else {
      setMood('thinking');
      setLine('Hmm… what do you think of this one?');
    }
  };
  const again = () => {
    setRound(newRound());
    setIndex(0);
    setScore(0);
    setAnswer(null);
    setMood('calm');
    setLine('Here we go again! Take your time.');
  };

  const right = answer !== null && answer === question.scam;

  return (
    <div className="game">
      <div className="game-peri">
        <p className="game-bubble" aria-live="polite">{line}</p>
        <LivePeri ref={peri} mood={mood} label="Peri" />
      </div>

      <div className="game-card">
        {!done ? (
          <>
            <div className="game-top">
              <span className="game-progress">{index + 1} of {ROUND}</span>
              <span className="game-score">Right: {score}</span>
            </div>
            <div className="message">
              <div className="message-head">
                <span className={`channel channel-${question.channel}`}>{CHANNEL[question.channel]}</span>
                <b>{question.sender}</b>
              </div>
              <p className="message-text"><MessageText text={question.text} /></p>
            </div>
            {answer === null ? (
              <div className="game-buttons">
                <button type="button" className="btn btn-trap" onClick={() => choose(true)}>It’s a trap</button>
                <button type="button" className="btn btn-light" onClick={() => choose(false)}>It’s fine</button>
              </div>
            ) : (
              <div className={`verdict ${right ? 'verdict-right' : 'verdict-wrong'}`}>
                <b>{question.scam ? 'It was a trap' : 'It was fine'}</b>
                <p>{question.clue}</p>
                <button type="button" className="btn" onClick={next}>{index + 1 >= ROUND ? 'See my result' : 'Next'}</button>
              </div>
            )}
            <p className="game-note">Links here are pretend: tapping them does nothing.</p>
          </>
        ) : (
          <div className="game-end">
            <span className="game-progress">Your result</span>
            <b className="game-final">{score} of {ROUND}</b>
            <p>In the app, a round played well earns Peri a shield.</p>
            <button type="button" className="btn" onClick={again}>Play again</button>
          </div>
        )}
      </div>
    </div>
  );
}
