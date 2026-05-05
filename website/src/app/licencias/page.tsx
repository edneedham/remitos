import webLicensesData from './web-licenses.json';
import serverLicensesData from './server-licenses.json';
import Licenses from './components/Licenses';
import type { License } from './types';

export default function LicensesPage() {
  const webLicenses = webLicensesData as License[];
  const serverLicenses = serverLicensesData as License[];
  const totalLicenses = serverLicenses.length + webLicenses.length;

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-content mx-auto py-20 px-8">
        <h1 className="text-4xl font-bold text-blue-600 mb-4 text-left">
          Licencias de terceros
        </h1>
        <p className="text-gray-600 mb-12 leading-relaxed">
          Esta página reúne las bibliotecas de terceros utilizadas por En Punto
          (ROASAL S.A.S.) en la API y en la aplicación web, junto con sus
          licencias correspondientes.
        </p>
        <p className="text-sm text-gray-500 mb-12 leading-relaxed">
          Total de paquetes listados: {totalLicenses}.
        </p>

        {/* API/Server licenses */}
        <section className="bg-white p-8 mb-8 rounded-lg shadow-sm">
          <h2 className="text-3xl font-bold text-gray-800 mb-4">
            API (Servidor)
          </h2>
          <p className="text-gray-600 mb-6">
            {serverLicenses.length} paquetes de terceros
          </p>
          <Licenses licenses={serverLicenses} />
        </section>

        {/* Website licenses */}
        <section className="bg-white p-8 mb-8 rounded-lg shadow-sm">
          <h2 className="text-3xl font-bold text-gray-800 mb-4">
            Sitio web (Frontend)
          </h2>
          <p className="text-gray-600 mb-6">
            {webLicenses.length} paquetes de terceros
          </p>
          <Licenses licenses={webLicenses} />
        </section>
      </div>
    </div>
  );
}
