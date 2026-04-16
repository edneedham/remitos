'use client';

import Link from 'next/link';
import { ArrowRight } from 'lucide-react';

export default function HeroSignupRow() {
  return (
    <div className="flex flex-wrap items-center gap-4">
      <Link
        href="/signup"
        className="inline-flex items-center px-6 py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition-colors duration-200"
      >
        Arrancá hoy
        <ArrowRight className="ml-2 h-4 w-4" />
      </Link>
      <Link
        href="#"
        className="inline-flex items-center px-6 py-3 bg-white text-gray-900 font-semibold rounded-lg border border-gray-900 hover:bg-gray-50 transition-colors duration-200"
      >
        Agendá una demo
      </Link>
    </div>
  );
}
