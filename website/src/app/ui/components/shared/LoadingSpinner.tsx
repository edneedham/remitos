'use client';

import React from 'react';
import { Loader2 } from 'lucide-react';

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  color?: string;
  className?: string;
}

export default function LoadingSpinner({
  size = 'md',
  color = 'text-blue-600',
  className = '',
}: LoadingSpinnerProps) {
  // Size mappings
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-8 h-8',
  };

  return (
    <div className={`${sizeClasses[size]} ${className}`} role="status">
      <Loader2 className={`w-full h-full animate-spin ${color}`} />
      <span className="sr-only">Loading...</span>
    </div>
  );
}
