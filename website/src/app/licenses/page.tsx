import webLicensesData from './web-licenses.json';
import serverLicensesData from './server-licenses.json';
import Licenses from './components/Licenses';
import type { License } from './types';

export default function LicensesPage() {
  const webLicenses = webLicensesData as License[];
  const serverLicenses = serverLicensesData as License[];

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto py-20 px-8">
        <h1 className="text-4xl font-bold text-blue-600 mb-4 text-left">
          Licencias de Software de Terceros
        </h1>
        <p className="text-gray-600 mb-12 leading-relaxed">
          En Punto utiliza software de código abierto y de terceros. A
          continuación se detallan todas las dependencias y sus respectivas
          licencias.
        </p>

        {/* Backend/Server Licenses */}
        <section className="bg-white p-8 mb-8 rounded-lg shadow-sm">
          <h2 className="text-3xl font-bold text-gray-800 mb-4">
            Backend (Servidor)
          </h2>
          <p className="text-gray-600 mb-6">
            {serverLicenses.length} paquetes de terceros
          </p>
          <Licenses licenses={serverLicenses} />
        </section>

        {/* Frontend/Web Licenses */}
        <section className="bg-white p-8 mb-8 rounded-lg shadow-sm">
          <h2 className="text-3xl font-bold text-gray-800 mb-4">
            Frontend (Aplicación Web)
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
