// A static site. On GitHub Pages it lives under /FriendlyPear/ (PAGES_BASE_PATH=/FriendlyPear npm run build).
/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'export',
  basePath: process.env.PAGES_BASE_PATH || '',
  images: { unoptimized: true },
};

export default nextConfig;
