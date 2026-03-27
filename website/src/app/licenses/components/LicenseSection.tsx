'use client';

import { useState } from 'react';

interface LicenseSectionProps {
  packageName: string;
  license?: string;
  licenseType?: string;
  repository?: string;
}

export default function LicenseSection({
  packageName,
  license,
  licenseType,
  repository,
}: LicenseSectionProps) {
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <div className="border-b border-gray-200 py-4">
      <div className="flex items-center justify-between">
        <div className="flex-1">
          <h3 className="font-semibold text-gray-900">{packageName}</h3>
          <div className="flex gap-4 text-sm text-gray-600 mt-1">
            {licenseType && (
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                {licenseType}
              </span>
            )}
          </div>
          {repository && (
            <a
              href={repository}
              target="_blank"
              rel="noopener noreferrer"
              className="text-sm text-blue-600 hover:text-blue-700 mt-1 inline-block"
            >
              Repository →
            </a>
          )}
        </div>
        {license && (
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="ml-4 px-4 py-2 text-sm font-medium text-blue-600 hover:text-blue-700"
          >
            {isExpanded ? 'Ocultar Licencia' : 'Mostrar Licencia'}
          </button>
        )}
      </div>

      {isExpanded && license && (
        <div className="mt-4 p-4 bg-gray-50 rounded-lg overflow-auto max-h-96">
          <pre className="text-xs text-gray-700 whitespace-pre-wrap font-mono">
            {license}
          </pre>
        </div>
      )}
    </div>
  );
}
