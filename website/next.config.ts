import path from 'path';
import { fileURLToPath } from 'url';
import type { NextConfig } from 'next';

const websiteRoot = path.dirname(fileURLToPath(import.meta.url));

const nextConfig: NextConfig = {
  turbopack: {
    root: websiteRoot,
  },
  experimental: {
    optimizePackageImports: ['lucide-react'],
  },
  devIndicators: false,
  async headers() {
    const apiOrigin = (() => {
      const raw = process.env.NEXT_PUBLIC_API_URL;
      if (!raw) return '';
      try {
        return new URL(raw).origin;
      } catch {
        return '';
      }
    })();

    const connectSrc = [
      "'self'",
      apiOrigin,
      'https://*.mercadopago.com',
      'https://api.mercadopago.com',
      'https://www.mercadopago.com',
      'https://secure.mlstatic.com',
      'https://*.googleapis.com',
      'https://*.gstatic.com',
    ]
      .filter(Boolean)
      .join(' ');

    const csp = [
      "default-src 'self'",
      "base-uri 'self'",
      "frame-ancestors 'none'",
      `connect-src ${connectSrc}`,
      "img-src 'self' data: blob: https:",
      "font-src 'self' data:",
      "style-src 'self' 'unsafe-inline'",
      // Next.js dev/prod may rely on inline scripts; tighten further when using nonce-based CSP.
      "script-src 'self' 'unsafe-inline' 'unsafe-eval'",
      'frame-src https://*.mercadopago.com https://www.mercadopago.com https://secure.mlstatic.com',
    ].join('; ');

    return [
      {
        source: '/:path*',
        headers: [
          { key: 'Content-Security-Policy', value: csp },
          { key: 'X-Frame-Options', value: 'DENY' },
          { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
        ],
      },
    ];
  },
  async redirects() {
    return [];
  },
  async rewrites() {
    const apiUrl = process.env.API_URL || 'http://localhost:8080';
    return [
      {
        source: '/api/v1/:path*',
        destination: `${apiUrl}/api/v1/:path*`,
      },
    ];
  },
};

export default nextConfig;
