import LicenseSection from './LicenseSection';
import type { License } from '../types';

interface LicensesProps {
  licenses: License[];
}

export default function Licenses({ licenses }: LicensesProps) {
  const sortedLicenses = [...licenses].sort((a, b) =>
    a.name.localeCompare(b.name),
  );

  return (
    <div className="space-y-2">
      {sortedLicenses.map((pkg) => (
        <LicenseSection
          key={pkg.name}
          packageName={pkg.name}
          license={pkg.license}
          licenseType={pkg.licenseType}
          repository={pkg.repository}
        />
      ))}
    </div>
  );
}
