import type { Metadata } from 'next';
import './globals.css';
export const metadata: Metadata = {
  title: 'FriendlyPear — Peri stops scams',
  description: 'An AI voice agent that protects anyone, and especially older adults, from scams, and keeps their family in the loop.',
};
export default function RootLayout({children}: Readonly<{children: React.ReactNode}>) {
  return <html lang="en"><body>{children}</body></html>;
}
