'use client';

import { useEffect, useRef, type ReactNode } from 'react';

// Fades a block up the first time it scrolls into view. Without JavaScript, or with
// "reduce motion", it is simply there (see .reveal in globals.css).
export default function Reveal({ className = '', children }: { className?: string; children: ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const element = ref.current!;
    element.classList.add('reveal-pending');
    const observer = new IntersectionObserver((entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          element.classList.add('reveal-in');
          observer.disconnect();
        }
      }
    }, { rootMargin: '0px 0px -8% 0px' });
    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  return <div ref={ref} className={`reveal ${className}`}>{children}</div>;
}
