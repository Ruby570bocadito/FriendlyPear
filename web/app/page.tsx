import BrandLogo from './brand-logo';
import HeroWorld from './hero-world';
import ScamGame from './scam-game';
import Reveal from './reveal';
import SiteFooter from './site-footer';

const REPO = 'https://github.com/Ruby570bocadito/FriendlyPear';

const STEPS = [
  {
    img: 'img/check.webp',
    title: 'She reads every message as it arrives',
    text: 'SMS, WhatsApp and email are checked on the phone the moment they land. Calls from numbers you haven’t saved are flagged before you pick up.',
  },
  {
    img: 'img/code.webp',
    title: 'Hard rules no model can override',
    text: 'A code arriving during a call is a scam, full stop. Look-alike bank links and links without a padlock always get a warning, even from family.',
  },
  {
    img: 'img/family.webp',
    title: 'She checks with your real family',
    text: 'Before anything risky, Peri calls the real person at the number already saved. One spoken “yes” and she dials.',
  },
];

const MOMENTS = [
  {
    tag: 'Text message',
    title: '“I have a new number”',
    text: 'An unknown number claims to be the daughter and asks for money. Peri warns, finds the real daughter in the contacts and offers to call her.',
    quote: 'If you say yes, I’ll call Laura.',
  },
  {
    tag: 'Phone call',
    title: '“Grandma, it’s Pablo”',
    text: 'A stranger rings. Before pickup Peri says not to give money or codes. After the call she checks: that number isn’t Pablo’s.',
    quote: 'Shall I call Pablo on their usual number to check?',
  },
  {
    tag: 'Code during a call',
    title: 'A one-time code arrives',
    text: 'While on the phone, an SMS with a code comes in. A hard rule fires at once, and the code is never read aloud.',
    quote: 'Nobody genuine will ask for it, so hang up and don’t tell anyone.',
  },
];

const MODELS = [
  ['Muse Spark Open', 'A second opinion on doubtful messages, worth at most ±2 points.'],
  ['Muse Spark', 'The investigator with tools, “look at this picture” with SAM 3.1, and the police report.'],
  ['Muse Voice Transcribe', 'Peri’s ears: she understands spoken orders in English and Spanish.'],
  ['Muse Image', 'Every picture of Peri, and the comics that show how each trap works.'],
  ['Muse Spark contributor', 'Training data and the red team that tried to fool her.'],
  ['Muse Wonder', 'Proposed Peri’s new lines, every one reviewed by hand.'],
];

export default function Home() {
  return (
    <>
      <header className="site-header">
        <div className="wrap header-row">
          <a href="#top" className="header-logo" aria-label="FriendlyPear, back to top"><BrandLogo /></a>
          <nav className="header-nav" aria-label="Sections">
            <a href="#how">How it works</a>
            <a href="#play">Play</a>
            <a href="#family">Family</a>
            <a href="#privacy">Privacy</a>
            <a href="#models">Meta models</a>
          </nav>
          <a className="btn btn-small" href={REPO}>GitHub</a>
        </div>
      </header>

      <main id="top">
        <section className="hero">
          <div className="wrap hero-grid">
            <div className="hero-copy">
              <span className="chip">Meta Global AI Developer Hackathon · Indie track</span>
              <h1>Peri stops scams before money moves.</h1>
              <p className="lead">A friendly pear that lives in your phone. She reads your messages aloud, calls whoever you ask, and when something smells like a scam she checks with your real family before you lose a cent.</p>
              <div className="actions">
                <a className="btn" href={REPO}>See the code on GitHub</a>
                <a className="btn btn-light" href="#how">How it works</a>
              </div>
              <ul className="facts" aria-label="At a glance">
                <li>Android 12 or later</li>
                <li>English and Spanish</li>
                <li>Rules run on the phone</li>
              </ul>
            </div>
            <HeroWorld />
          </div>
        </section>

        <section className="stats" aria-label="Results">
          <div className="wrap stats-grid">
            <Reveal className="stat"><b>91%</b><span>of 200 unseen scams flagged, before any model</span></Reveal>
            <Reveal className="stat"><b>4%</b><span>false alarms on ordinary messages</span></Reveal>
            <Reveal className="stat"><b>6</b><span>Meta models, each with one job</span></Reveal>
            <Reveal className="stat"><b>7:1</b><span>text contrast, made for low vision</span></Reveal>
          </div>
        </section>

        <section className="why">
          <div className="wrap split">
            <Reveal className="split-art"><img src="img/grandma.webp" alt="An older woman in an armchair looking at her phone, with Peri on the armrest pointing at the screen" width="1200" height="800" loading="lazy" /></Reveal>
            <Reveal className="split-copy">
              <span className="eyebrow">Why</span>
              <h2>Scams isolate and rush. Peri breaks both.</h2>
              <p>Scammers cut people off from the ones they’d ask, and push them to act now. Voice assistants can call and read but don’t judge; spam filters warn about everything or nothing. Peri checks through a second channel: the real family member, at the number already saved.</p>
              <div className="bubbles">
                <p className="bubble">“Mum, I broke my phone, this is my new number, can you send me money?”</p>
                <p className="bubble">“Your bank here: read me the code you just got.”</p>
              </div>
            </Reveal>
          </div>
        </section>

        <section id="how" className="how">
          <div className="wrap">
            <Reveal className="section-head">
              <span className="eyebrow">How it works</span>
              <h2>Three layers between a scammer and your money</h2>
            </Reveal>
            <div className="cards-3">
              {STEPS.map((step, i) => (
                <Reveal className="card step" key={step.title}>
                  <img src={step.img} alt="" width="1000" height="1000" loading="lazy" />
                  <span className="step-num">{i + 1}</span>
                  <h3>{step.title}</h3>
                  <p>{step.text}</p>
                </Reveal>
              ))}
            </div>
          </div>
        </section>

        <section id="play" className="play">
          <div className="wrap">
            <Reveal className="section-head">
              <span className="eyebrow">Try it</span>
              <h2>Scam or not?</h2>
              <p>The game from Peri’s Learn section, with the app’s own messages. Five messages: you decide, Peri explains.</p>
            </Reveal>
            <Reveal><ScamGame /></Reveal>
          </div>
        </section>

        <section className="moments">
          <div className="wrap">
            <Reveal className="section-head">
              <span className="eyebrow">Tested on the emulator, in English and Spanish</span>
              <h2>What Peri does when it matters</h2>
            </Reveal>
            <div className="cards-3">
              {MOMENTS.map((m) => (
                <Reveal className="moment" key={m.title}>
                  <span className="moment-tag">{m.tag}</span>
                  <h3>{m.title}</h3>
                  <p>{m.text}</p>
                  <p className="moment-quote">{m.quote}</p>
                </Reveal>
              ))}
            </div>
          </div>
        </section>

        <section id="family" className="family">
          <div className="wrap split split-reverse">
            <Reveal className="split-copy">
              <span className="eyebrow">For the family</span>
              <h2>A double-check, not surveillance</h2>
              <p>Risky actions wait for a guardian. Each one gets a one-time link, valid for 15 minutes, with three answers. Silence counts as no.</p>
              <div className="answers" aria-label="The guardian's three answers">
                <span className="answer answer-no">No</span>
                <span className="answer">Ask them to call me first</span>
                <span className="answer answer-yes">Yes, I approve</span>
              </div>
              <ul className="ticks">
                <li>Flagged <b>URGENT</b> if a stranger just called; it never says who or what.</li>
                <li>The riskiest requests need two guardians’ yes, and a single no stops them.</li>
                <li>If the guardian has FriendlyPear too, it opens <b>Peri to Peri</b> in their own app.</li>
              </ul>
            </Reveal>
            <Reveal className="phone-art">
              <img src="img/guardian.webp" alt="The guardian’s page: Peri holding a phone, the request, a possible-scam alert and three answers" width="469" height="1015" loading="lazy" />
            </Reveal>
          </div>
        </section>

        <section className="app">
          <div className="wrap">
            <Reveal className="section-head">
              <span className="eyebrow">The app</span>
              <h2>A friend people want to keep</h2>
              <p>Nine moods in soft clay, her own world that follows the hour and the season, a game to learn the traps, and big type in Fredoka and Atkinson Hyperlegible.</p>
            </Reveal>
            <Reveal className="screens">
              <img src="img/screens.webp" alt="Four screens: Peri at home in her meadow, Peri warning about a fake bank text, the security checkup, and a new badge" width="1272" height="666" loading="lazy" />
            </Reveal>
            <Reveal className="moods">
              <img src="img/moods.webp" alt="Peri’s nine moods: calm, listening, thinking, worried, protective, calling, waiting, happy and sleeping" width="1800" height="240" loading="lazy" />
            </Reveal>
          </div>
        </section>

        <section id="privacy" className="privacy">
          <div className="wrap split">
            <Reveal className="split-art split-art-small"><img src="img/privacy.webp" alt="Peri hugging a big green padlock" width="1000" height="1000" loading="lazy" /></Reveal>
            <Reveal className="split-copy">
              <span className="eyebrow">Privacy and safety</span>
              <h2>Built to be hard to abuse</h2>
              <ul className="ticks">
                <li>The family never reads messages, calls, location or apps.</li>
                <li>A yes needs the phone’s own lock, and every phone is paired in person with its own key.</li>
                <li>Fake “Peri” texts are caught; scam messages are never read aloud.</li>
                <li>Pictures you show her are never stored.</li>
                <li>A <b>“What Peri can do”</b> screen lists every permission, how to switch it off, and a log of everything she did.</li>
              </ul>
            </Reveal>
          </div>
        </section>

        <section id="models" className="models">
          <div className="wrap">
            <Reveal className="section-head">
              <span className="eyebrow">Under the hood</span>
              <h2>Six Meta models, each with one job</h2>
              <p>A model can move the score by at most ±2, and Peri always speaks her own sentence, never the model’s.</p>
            </Reveal>
            <div className="models-grid">
              {MODELS.map(([name, job]) => (
                <Reveal className="model" key={name}>
                  <h3>{name}</h3>
                  <p>{job}</p>
                </Reveal>
              ))}
            </div>
          </div>
        </section>
      </main>

      <SiteFooter repo={REPO} />
    </>
  );
}
