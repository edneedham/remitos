'use client';

import Image from 'next/image';
import { useEffect, useRef } from 'react';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

type Step = {
  id: string;
  title: string;
  description: string;
  imageSrc: string;
  imageAlt: string;
};

const steps: Step[] = [
  {
    id: '1',
    title: 'Escaneá el remito',
    description:
      'Capturá la foto del remito y obtené los datos base para reducir carga manual desde el primer minuto.',
    imageSrc: '/screenshots/scan.png',
    imageAlt: 'Pantalla de escaneo y carga de remitos',
  },
  {
    id: '2',
    title: 'Validá y completá',
    description:
      'Revisá la información clave antes de salir a ruta para evitar errores de tipeo y correcciones posteriores.',
    imageSrc: '/screenshots/save-form.png',
    imageAlt: 'Pantalla de guardado y validación del formulario',
  },
  {
    id: '3',
    title: 'Armá la hoja de reparto',
    description:
      'Asigná bultos y generá un PDF claro para el chofer con lo necesario para ejecutar la entrega.',
    imageSrc: '/screenshots/reparto-pdf.png',
    imageAlt: 'Vista previa de la hoja de reparto en PDF',
  },
  {
    id: '4',
    title: 'Exportá y cerrá',
    description:
      'Exportá en CSV para administración, contabilidad u otros sistemas sin rehacer trabajo en otra planilla.',
    imageSrc: '/screenshots/exportar-csv.png',
    imageAlt: 'Pantalla de opciones para exportar datos',
  },
];

function HowItWorksStep({ step, index }: { step: Step; index: number }) {
  const ref = useRef<HTMLElement | null>(null);
  const headerRef = useRef<HTMLElement | null>(null);
  const visualRef = useRef<HTMLDivElement | null>(null);
  const textRef = useRef<HTMLDivElement | null>(null);
  const isPdfStep = step.id === '3';
  const visualOnRight = index % 2 === 1;
  const visualOrderClass = visualOnRight ? 'md:order-2' : 'md:order-1';
  const textOrderClass = visualOnRight ? 'md:order-1' : 'md:order-2';
  const visualAlignClass = visualOnRight ? 'md:ml-auto md:mr-0' : 'md:mr-auto md:ml-0';
  const textAlignClass = visualOnRight ? 'md:mr-0 md:ml-auto' : 'md:ml-0 md:mr-auto';

  useEffect(() => {
    if (!ref.current || !headerRef.current || !visualRef.current || !textRef.current) {
      return;
    }

    gsap.registerPlugin(ScrollTrigger);

    const slideFromX = visualOnRight ? 48 : -48;
    const ctx = gsap.context(() => {
      gsap.set([headerRef.current, visualRef.current], {
        x: slideFromX,
        opacity: 0,
      });
      gsap.set(textRef.current, { opacity: 0 });

      const tl = gsap.timeline({
        scrollTrigger: {
          trigger: ref.current,
          start: 'top 72%',
          end: 'top 38%',
          toggleActions: 'play none none reverse',
        },
      });

      tl.to([headerRef.current, visualRef.current], {
        x: 0,
        opacity: 1,
        duration: 0.7,
        ease: 'power2.out',
        stagger: 0.08,
      }).to(
        textRef.current,
        {
          opacity: 1,
          duration: 0.55,
          ease: 'power1.out',
        },
        0.22,
      );
    }, ref);

    return () => ctx.revert();
  }, [visualOnRight]);

  return (
    <article
      ref={ref}
      className="relative min-h-[88vh] snap-start py-10 md:min-h-screen"
    >
      <header
        ref={headerRef}
        className={`mb-8 ${
          visualOnRight ? 'text-right md:ml-auto md:w-[58%]' : 'text-left md:w-[58%]'
        }`}
      >
        <p className="mb-2 text-xl font-semibold tracking-wide text-blue-600">
          {step.id}
        </p>
        <h3 className="text-4xl font-bold text-gray-900">{step.title}</h3>
      </header>

      <div
        className={`grid grid-cols-1 items-center gap-8 md:gap-14 ${
          isPdfStep ? 'md:grid-cols-1' : 'md:grid-cols-[320px_minmax(0,1fr)]'
        }`}
      >
        <div ref={visualRef} className={visualOrderClass}>
          {isPdfStep ? (
            <div className="relative ml-0 mr-auto w-full">
              <div className="w-[76.8%]">
                <Image
                  src={step.imageSrc}
                  alt={step.imageAlt}
                  width={1400}
                  height={900}
                  className="h-auto w-full drop-shadow-[10px_14px_20px_rgba(15,23,42,0.28)]"
                />
              </div>
              <Image
                src="/screenshots/repartos-history.png"
                alt="Pantalla de historial de repartos en la app"
                width={286}
                height={611}
                className="absolute bottom-0 right-0 h-auto w-[240px] rounded-[28px] shadow-[0_10px_24px_rgba(0,0,0,0.22)]"
              />
            </div>
          ) : (
            <Image
              src={step.imageSrc}
              alt={step.imageAlt}
              width={286}
              height={611}
              className={`h-auto w-[286px] ${visualAlignClass}`}
            />
          )}
        </div>

        <div ref={textRef} className={textOrderClass}>
          <p className={`max-w-2xl text-2xl leading-relaxed text-gray-600 ${textAlignClass}`}>
            {step.description}
          </p>
        </div>
      </div>
    </article>
  );
}

export default function HowItWorksSection() {
  return (
    <section
      className="border-b border-gray-200 bg-gray-50 py-20 px-4 sm:px-6 lg:px-8"
      aria-labelledby="how-it-works-heading"
    >
      <div className="mx-auto w-full max-w-[80vw]">
        <div className="mx-auto mb-14 max-w-content-prose text-center">
          <h2
            id="how-it-works-heading"
            className="mb-4 text-3xl font-bold text-gray-900 sm:text-4xl"
          >
            Cómo funciona, paso a paso
          </h2>
          <p className="text-lg text-gray-600">
            Un flujo de trabajo simple para pasar del remito a una operación lista
            para ruta y cierre administrativo.
          </p>
        </div>

        <div className="mx-auto max-w-5xl snap-y snap-mandatory">
          {steps.map((step, index) => (
            <HowItWorksStep key={step.id} step={step} index={index} />
          ))}
        </div>
      </div>
    </section>
  );
}
