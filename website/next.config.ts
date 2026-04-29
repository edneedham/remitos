import path from 'path';
import { fileURLToPath } from 'url';
import type { NextConfig } from 'next';

const websiteRoot = path.dirname(fileURLToPath(import.meta.url));

const nextConfig: NextConfig = {
  turbopack: {
    root: websiteRoot,
  },
  devIndicators: false,
  async redirects() {
    return [
      { source: '/account', destination: '/dashboard', permanent: true },
      {
        source: '/account/:path*',
        destination: '/dashboard/:path*',
        permanent: true,
      },
      {
        source: '/download',
        destination: '/dashboard/app',
        permanent: true,
      },
    ];
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
