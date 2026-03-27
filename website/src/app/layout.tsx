import type { Metadata } from 'next';
import './globals.css';
import Footer from './ui/components/website/Footer';
import Header from './ui/components/website/Header';

export const metadata: Metadata = {
  title: 'En Punto',
  description: 'En Punto - Soluciones digitales para tu negocio en Argentina',
};

export default function WebsiteLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="es">
      <body className="flex flex-col min-h-screen">
        <Header />
        <main className="grow">{children}</main>
        <Footer />
      </body>
    </html>
  );
}
