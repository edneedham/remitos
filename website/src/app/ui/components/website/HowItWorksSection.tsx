'use client';

import Image from 'next/image';
import { useEffect, useRef } from 'react';
import gsap from 'gsap';
import { ensureGsapScrollTrigger } from '../../../lib/gsapClient';

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

const MOBILE_MAX_WIDTH_MQ = '(max-width: 767px)';

function HowItWorksStep({ step, index }: { step: Step; index: number }) {
  const ref = useRef<HTMLElement | null>(null);
  const headerRef = useRef<HTMLElement | null>(null);
  const visualRef = useRef<HTMLDivElement | null>(null);
  const textRef = useRef<HTMLDivElement | null>(null);
  const isPdfStep = step.id === '3';
  const visualOnRight = index % 2 === 1;
  /** Match step-1 proportions: phone always in a 320px track, copy always in 1fr (swap column *order* when image is on the right). */
  const stepGridCols = isPdfStep
    ? 'md:grid-cols-1'
    : visualOnRight
      ? 'md:grid-cols-[minmax(0,1fr)_320px]'
      : 'md:grid-cols-[320px_minmax(0,1fr)]';
  const visualGridPlacement = !isPdfStep && visualOnRight ? 'md:col-start-2 md:row-start-1' : '';
  const textGridPlacement = !isPdfStep && visualOnRight ? 'md:col-start-1 md:row-start-1' : '';
  const visualAlignClass = visualOnRight
    ? 'mx-auto md:ml-auto md:mr-0'
    : 'mx-auto md:mr-auto md:ml-0';
  const textAlignClass = visualOnRight ? 'md:mr-0 md:ml-auto' : 'md:ml-0 md:mr-auto';

  useEffect(() => {
    if (!ref.current || !headerRef.current || !visualRef.current || !textRef.current) {
      return;
    }

    const prefersReduce =
      typeof window !== 'undefined' &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReduce) {
      gsap.set([headerRef.current, visualRef.current], {
        x: 0,
        y: 0,
        opacity: 1,
      });
      gsap.set(textRef.current, { opacity: 1 });
      return;
    }

    ensureGsapScrollTrigger();

    const isMobile = window.matchMedia(MOBILE_MAX_WIDTH_MQ).matches;
    const slideFromX = visualOnRight ? 48 : -48;
    const ctx = gsap.context(() => {
      if (isMobile) {
        gsap.set([headerRef.current, visualRef.current], {
          x: 0,
          y: 22,
          opacity: 0,
        });
        gsap.set(textRef.current, { opacity: 0, x: 0, y: 0 });

        const tl = gsap.timeline({
          scrollTrigger: {
            trigger: ref.current,
            start: 'top 82%',
            toggleActions: 'play none none reverse',
            invalidateOnRefresh: true,
          },
        });

        tl.to([headerRef.current, visualRef.current], {
          y: 0,
          opacity: 1,
          duration: 0.72,
          ease: 'power3.out',
          stagger: 0.1,
        }).to(
          textRef.current,
          {
            opacity: 1,
            duration: 0.55,
            ease: 'power1.out',
          },
          0.18,
        );
        return;
      }

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
          invalidateOnRefresh: true,
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
      className="relative min-h-0 py-8 md:min-h-screen md:py-10 md:snap-start"
    >
      <header
        ref={headerRef}
        className={`mb-5 md:mb-8 ${
          isPdfStep
            ? 'w-full text-left'
            : visualOnRight
              ? 'text-left md:ml-auto md:w-[58%] md:text-right'
              : 'text-left md:w-[58%]'
        }`}
      >
        <p className="mb-2 text-xl font-semibold tracking-wide text-blue-600 md:mb-3 md:text-2xl lg:text-3xl">
          {step.id}
        </p>
        <h3 className="text-2xl font-bold leading-tight text-gray-900 sm:text-3xl md:text-4xl lg:text-5xl">
          {step.title}
        </h3>
      </header>

      <div className={`grid grid-cols-1 items-center gap-5 md:gap-8 lg:gap-14 ${stepGridCols}`}>
        <div
          ref={visualRef}
          className={`min-w-0 max-md:flex max-md:w-full max-md:justify-center ${visualGridPlacement} ${
            isPdfStep ? 'max-md:order-1 md:order-2 max-md:mt-0 md:mt-10' : ''
          }`}
        >
          {isPdfStep ? (
            <div className="relative mx-auto flex w-full max-md:justify-center md:ml-0 md:mr-auto md:block">
              <div className="hidden ml-auto w-[68%] md:block">
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
                className="mx-auto h-auto w-[220px] max-w-full md:absolute md:left-0 md:right-auto md:top-1/2 md:mx-0 md:w-[286px] md:max-w-none md:-translate-y-1/2"
              />
            </div>
          ) : (
            <Image
              src={step.imageSrc}
              alt={step.imageAlt}
              width={286}
              height={611}
              className={`h-auto w-[220px] max-w-full md:w-[286px] ${visualAlignClass}`}
            />
          )}
        </div>

        <div
          ref={textRef}
          className={`min-w-0 ${textGridPlacement} ${isPdfStep ? 'max-md:order-2 md:order-1 max-md:mt-2' : ''}`}
        >
          <p
            className={`w-full max-w-none text-lg leading-relaxed text-gray-600 sm:text-xl md:text-2xl lg:text-4xl lg:leading-snug ${textAlignClass}`}
          >
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
      className="overflow-x-clip border-b border-gray-200 bg-gray-50 py-12 px-4 sm:px-6 md:py-20 lg:px-8"
      aria-labelledby="how-it-works-heading"
    >
      <div className="mx-auto w-full max-w-5xl">
        <div className="mb-8 w-full max-w-content-prose text-left md:mb-14">
          <h2
            id="how-it-works-heading"
            className="mb-3 text-3xl font-bold text-gray-900 sm:mb-4 sm:text-4xl md:mb-6 md:text-5xl lg:text-5xl"
          >
            Cómo funciona, paso a paso
          </h2>
          <p className="text-lg leading-relaxed text-gray-600 sm:text-xl md:text-2xl lg:text-3xl">
            Un flujo de trabajo simple para pasar del remito a una operación lista
            para ruta y cierre administrativo.
          </p>
        </div>

        <div className="md:snap-y md:snap-mandatory">
          {steps.map((step, index) => (
            <HowItWorksStep key={step.id} step={step} index={index} />
          ))}
        </div>
      </div>
    </section>
  );
}
