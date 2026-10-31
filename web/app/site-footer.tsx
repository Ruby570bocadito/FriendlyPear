import FooterBackground from './footer-background';
import BrandLogo from './brand-logo';

export default function SiteFooter({ repo }: { repo: string }) {
  return (
    <footer className="footer" aria-label="Footer">
      <FooterBackground />
      <div className="jobs">
        <span className="tag">got a weird message?</span>
        <span className="headline job-title">ask Peri<br />before you pay</span>
        <nav className="footer-nav" aria-label="Footer">
          <a href="#how">How it works</a>
          <a href="#family">Family</a>
          <a href="#privacy">Privacy</a>
          <a href="#models">Meta models</a>
        </nav>
      </div>
      <div className="logo" role="img" aria-label="FriendlyPear">
        <BrandLogo />
      </div>
      <div className="contact">
        <span className="tag">say hi to Peri</span>
        <div className="headline contact-links">
          <span>a pear in your phone</span>
          <span>that stops scams*</span>
        </div>
        <p className="note">*built for the Meta Global AI Developer Hackathon.</p>
        <div className="socials">
          <a href={repo} aria-label="FriendlyPear on GitHub"><img src="github.svg" alt="" width="35" height="35" /></a>
          <span aria-label="Android app"><img src="android2.svg" alt="" width="35" height="35" /></span>
        </div>
      </div>
    </footer>
  );
}
