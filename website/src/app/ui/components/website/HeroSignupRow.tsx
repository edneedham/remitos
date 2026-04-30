'use client';

import Image from 'next/image';
import Link from 'next/link';
import { ArrowRight } from 'lucide-react';

export default function HeroSignupRow() {
  return (
    <div className="flex flex-wrap items-center gap-4">
      <Link
        href="/signup"
        className="inline-flex items-center px-6 py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition-colors duration-200"
      >
        Probar en{' '}
        <Image
          src="/brands/android-head_flat.svg"
          alt=""
          width={152}
          height={89}
          className="mx-1 inline-block h-[1.15em] w-auto shrink-0 align-[-0.12em]"
          unoptimized
          aria-hidden
        />
        Android
        <ArrowRight className="ml-2 h-4 w-4 shrink-0" />
      </Link>
    </div>
  );
}
