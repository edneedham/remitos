'use client';

import Image from 'next/image';
import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import gsap from 'gsap';

const FIRST_BENEFIT_LINE = 'Encontrá documentos al toque.';
const SECOND_BENEFIT_LINE =
  'Generá checklists para tus entregas sin errores.';
const THIRD_BENEFIT_LINE =
  'Subí tus datos donde los necesites con nuestra integración de Google Drive.';
const PANEL_CAPTION_LINE = 'Gestioná tu cuenta desde la web.';

/** Section is “pinned” at the top of the viewport (desktop snap target). */
function isPinnedAtTop(el: HTMLElement | null): boolean {
  if (!el) return false;
  const top = el.getBoundingClientRect().top;
  return top >= -4 && top <= 12;
}

/** User is scrolling through this section but it isn’t pinned yet — snap on wheel. */
function needsSnapDown(el: HTMLElement | null): boolean {
  if (!el) return false;
  const rect = el.getBoundingClientRect();
  return rect.top > 12 && rect.top < window.innerHeight * 0.85 && rect.bottom > 80;
}

function smoothScrollToY(targetY: number, durationMs = 520): Promise<void> {
  const startY = window.scrollY;
  const dy = targetY - startY;
  if (Math.abs(dy) < 2) return Promise.resolve();

  return new Promise((resolve) => {
    const t0 = performance.now();
    function frame(now: number) {
      const t = Math.min((now - t0) / durationMs, 1);
      const eased = 1 - Math.pow(1 - t, 3);
      window.scrollTo({ top: startY + dy * eased, left: 0 });
      if (t < 1) {
        requestAnimationFrame(frame);
      } else {
        resolve();
      }
    }
    requestAnimationFrame(frame);
  });
}

export default function BenefitsSection() {
  const sectionRef = useRef<HTMLElement | null>(null);
  const screenshotRef = useRef<HTMLDivElement | null>(null);
  const panelFullRef = useRef<HTMLDivElement | null>(null);
  const panelCaptionRef = useRef<HTMLParagraphElement | null>(null);
  const benefitTextRef = useRef<HTMLParagraphElement | null>(null);
  const benefitText2Ref = useRef<HTMLParagraphElement | null>(null);
  const benefitText3Ref = useRef<HTMLParagraphElement | null>(null);

  const lockArmedRef = useRef(false);
  const screenshotRevealedRef = useRef(false);
  const firstBenefitRevealedRef = useRef(false);
  const secondBenefitRevealedRef = useRef(false);
  const thirdBenefitRevealedRef = useRef(false);
  const panelSwapCompleteRef = useRef(false);
  const panelTransitionBusyRef = useRef(false);
  const snapBusyRef = useRef(false);
  const wheelBurstActiveRef = useRef(false);
  const wheelBurstResetTimerRef = useRef<number | null>(null);
  const advanceCooldownUntilRef = useRef(0);

  const unlockScrollRef = useRef<(() => void) | null>(null);

  const [panelOnlyLayout, setPanelOnlyLayout] = useState(false);

  useLayoutEffect(() => {
    const section = sectionRef.current;
    const shot = screenshotRef.current;
    const benefitEl = benefitTextRef.current;
    const benefit2El = benefitText2Ref.current;
    const benefit3El = benefitText3Ref.current;
    if (!section || !shot || !benefitEl || !benefit2El || !benefit3El) {
      return;
    }

    gsap.set(shot, {
      autoAlpha: 0,
      y: 28,
      force3D: true,
    });

    gsap.set([benefitEl, benefit2El], {
      autoAlpha: 0,
      x: -36,
      force3D: true,
    });

    gsap.set(benefit3El, {
      autoAlpha: 0,
      x: 36,
      force3D: true,
    });

    const html = document.documentElement;
    const body = document.body;
    let overflowSnapshot: {
      html: string;
      body: string;
      touch: string;
    } | null = null;

    const unlockScroll = () => {
      if (overflowSnapshot) {
        html.style.overflow = overflowSnapshot.html;
        body.style.overflow = overflowSnapshot.body;
        body.style.touchAction = overflowSnapshot.touch;
        overflowSnapshot = null;
      }
      lockArmedRef.current = false;
    };

    unlockScrollRef.current = unlockScroll;

    const lockScroll = () => {
      if (overflowSnapshot) return;
      overflowSnapshot = {
        html: html.style.overflow,
        body: body.style.overflow,
        touch: body.style.touchAction,
      };
      html.style.overflow = 'hidden';
      body.style.overflow = 'hidden';
      body.style.touchAction = 'none';
      lockArmedRef.current = true;
    };

    const revealScreenshot = () => {
      if (screenshotRevealedRef.current || !screenshotRef.current) return;
      screenshotRevealedRef.current = true;
      gsap.to(screenshotRef.current, {
        autoAlpha: 1,
        y: 0,
        duration: 0.85,
        ease: 'power3.out',
        overwrite: 'auto',
      });
    };

    const revealFirstBenefitText = () => {
      if (firstBenefitRevealedRef.current || !benefitTextRef.current) return;
      firstBenefitRevealedRef.current = true;
      gsap.to(benefitTextRef.current, {
        autoAlpha: 1,
        x: 0,
        duration: 0.78,
        ease: 'power3.out',
        overwrite: 'auto',
      });
    };

    const revealSecondBenefitText = () => {
      if (secondBenefitRevealedRef.current || !benefitText2Ref.current) return;
      secondBenefitRevealedRef.current = true;
      gsap.to(benefitText2Ref.current, {
        autoAlpha: 1,
        x: 0,
        duration: 0.78,
        ease: 'power3.out',
        overwrite: 'auto',
      });
    };

    const revealThirdBenefitText = () => {
      if (thirdBenefitRevealedRef.current || !benefitText3Ref.current) return;
      thirdBenefitRevealedRef.current = true;
      gsap.to(benefitText3Ref.current, {
        autoAlpha: 1,
        x: 0,
        duration: 0.78,
        ease: 'power3.out',
        overwrite: 'auto',
      });
    };

    const transitionToPanel = () => {
      if (panelSwapCompleteRef.current || panelTransitionBusyRef.current) return;
      if (
        !benefitTextRef.current ||
        !benefitText2Ref.current ||
        !benefitText3Ref.current ||
        !screenshotRef.current
      ) {
        return;
      }
      panelTransitionBusyRef.current = true;

      const tl = gsap.timeline({
        onComplete: () => {
          panelTransitionBusyRef.current = false;
          setPanelOnlyLayout(true);
        },
      });

      tl.to(
        [benefitTextRef.current, benefitText2Ref.current, benefitText3Ref.current],
        {
          autoAlpha: 0,
          duration: 0.4,
          stagger: 0.05,
          ease: 'power2.in',
        },
      );

      tl.to(
        screenshotRef.current,
        {
          autoAlpha: 0,
          duration: 0.45,
          ease: 'power2.inOut',
        },
        '-=0.15',
      );
    };

    const allStepsComplete = () =>
      screenshotRevealedRef.current &&
      firstBenefitRevealedRef.current &&
      secondBenefitRevealedRef.current &&
      thirdBenefitRevealedRef.current &&
      panelSwapCompleteRef.current;

    const advanceStep = () => {
      if (!screenshotRevealedRef.current) {
        revealScreenshot();
        return;
      }
      if (!firstBenefitRevealedRef.current) {
        revealFirstBenefitText();
        return;
      }
      if (!secondBenefitRevealedRef.current) {
        revealSecondBenefitText();
        return;
      }
      if (!thirdBenefitRevealedRef.current) {
        revealThirdBenefitText();
        return;
      }
      if (!panelSwapCompleteRef.current) {
        transitionToPanel();
      }
    };

    const maybeArmLock = () => {
      if (allStepsComplete()) return;
      if (!screenshotRevealedRef.current && isPinnedAtTop(section)) {
        if (!lockArmedRef.current) lockScroll();
      } else if (lockArmedRef.current && !screenshotRevealedRef.current) {
        unlockScroll();
      }
    };

    const handleWheel = async (e: WheelEvent) => {
      const desktop = window.matchMedia('(pointer: fine)').matches;

      if (
        desktop &&
        e.deltaY > 0 &&
        needsSnapDown(section) &&
        !screenshotRevealedRef.current
      ) {
        e.preventDefault();
        if (snapBusyRef.current) return;
        snapBusyRef.current = true;
        const rect = section.getBoundingClientRect();
        const targetScroll = window.scrollY + rect.top;
        await smoothScrollToY(Math.max(0, targetScroll));
        snapBusyRef.current = false;
        // Ignore inertial wheel continuation from the snap animation.
        advanceCooldownUntilRef.current = performance.now() + 420;
        maybeArmLock();
        return;
      }

      if (!lockArmedRef.current) return;
      if (e.deltaY <= 0) return;
      e.preventDefault();
      const now = performance.now();
      if (now < advanceCooldownUntilRef.current) return;
      if (wheelBurstActiveRef.current) return;

      wheelBurstActiveRef.current = true;
      advanceCooldownUntilRef.current = now + 360;
      advanceStep();

      if (wheelBurstResetTimerRef.current) {
        window.clearTimeout(wheelBurstResetTimerRef.current);
      }
      wheelBurstResetTimerRef.current = window.setTimeout(() => {
        wheelBurstActiveRef.current = false;
        wheelBurstResetTimerRef.current = null;
      }, 130);
    };

    const handleKeyDown = (e: KeyboardEvent) => {
      if (!lockArmedRef.current) return;
      if (!['ArrowDown', 'PageDown', ' ', 'Spacebar'].includes(e.key)) return;
      e.preventDefault();
      advanceStep();
    };

    const onScrollOrResize = () => {
      if (allStepsComplete()) return;
      if (!screenshotRevealedRef.current) {
        maybeArmLock();
      }
    };

    window.addEventListener('wheel', handleWheel, { passive: false });
    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('scroll', onScrollOrResize, { passive: true });
    window.addEventListener('resize', onScrollOrResize);
    onScrollOrResize();

    return () => {
      window.removeEventListener('wheel', handleWheel);
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('scroll', onScrollOrResize);
      window.removeEventListener('resize', onScrollOrResize);
      if (wheelBurstResetTimerRef.current) {
        window.clearTimeout(wheelBurstResetTimerRef.current);
        wheelBurstResetTimerRef.current = null;
      }
      unlockScroll();
      unlockScrollRef.current = null;
    };
  }, []);

  useEffect(() => {
    if (!panelOnlyLayout || !panelFullRef.current || !panelCaptionRef.current) return;

    gsap.set(panelFullRef.current, { autoAlpha: 0, y: 20 });
    gsap.set(panelCaptionRef.current, { autoAlpha: 0, y: 10 });

    gsap.to(panelFullRef.current, {
      autoAlpha: 1,
      y: 0,
      duration: 0.7,
      ease: 'power3.out',
      onComplete: () => {
        if (!panelCaptionRef.current) return;
        gsap.to(panelCaptionRef.current, {
          autoAlpha: 1,
          y: 0,
          duration: 0.45,
          ease: 'power2.out',
          onComplete: () => {
            panelSwapCompleteRef.current = true;
            unlockScrollRef.current?.();
          },
        });
      },
    });
  }, [panelOnlyLayout]);

  return (
    <section
      ref={sectionRef}
      className="scroll-mt-0 border-b border-gray-200 bg-white py-20 px-4 sm:px-6 lg:px-8"
      aria-labelledby="benefits-heading"
    >
      <div className="mx-auto w-full max-w-[70%]">
        <div className="mx-auto mb-12 max-w-content-prose text-center">
          <h2
            id="benefits-heading"
            className="mb-4 text-3xl font-bold text-gray-900 sm:text-4xl"
          >
            Menos carga manual, más control operativo
          </h2>
          <p className="text-lg text-gray-600">
            Pasá menos tiempo cargando datos y más en lo que importa.
          </p>
        </div>
      </div>

      {!panelOnlyLayout ? (
        <div className="mx-auto mt-10 w-full max-w-[70%]">
          <div className="grid grid-cols-1 gap-10 md:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] md:items-center md:gap-x-6 lg:gap-x-10">
            <div className="flex max-w-md flex-col gap-5 justify-self-start md:col-start-1 md:self-center">
              <p
                ref={benefitTextRef}
                className="text-left text-[calc(1.125rem*1.3)] leading-relaxed text-gray-800 [visibility:hidden]"
              >
                {FIRST_BENEFIT_LINE}
              </p>
              <p
                ref={benefitText2Ref}
                className="text-left text-[calc(1.125rem*1.3)] leading-relaxed text-gray-800 [visibility:hidden]"
              >
                {SECOND_BENEFIT_LINE}
              </p>
            </div>

            <div
              ref={screenshotRef}
              className="relative mx-auto w-fit shrink-0 justify-self-center overflow-hidden rounded-2xl p-4 [visibility:hidden] md:col-start-2 md:row-start-1"
            >
              <Image
                src="/screenshots/dashboard.png"
                alt="Vista del dashboard de la aplicación En Punto"
                width={286}
                height={611}
                className="h-auto w-[286px] rounded-xl"
                sizes="286px"
              />
            </div>

            <div className="max-w-md md:col-start-3 md:row-start-1 md:self-center">
              <p
                ref={benefitText3Ref}
                className="justify-self-end text-right text-[calc(1.125rem*1.3)] leading-relaxed text-gray-800 [visibility:hidden] md:justify-self-end"
              >
                {THIRD_BENEFIT_LINE}
              </p>
            </div>
          </div>
        </div>
      ) : (
        <div ref={panelFullRef} className="mx-auto mt-10 w-full md:w-1/2">
          <div className="relative w-full overflow-hidden rounded-xl border-2 border-gray-800">
            <Image
              src="/screenshots/panel.png"
              alt="Panel web de administración En Punto"
              width={1920}
              height={1080}
              className="h-auto w-full rounded-xl object-contain"
              sizes="100vw"
              priority
            />
          </div>
          <p
            ref={panelCaptionRef}
            className="mx-auto mt-8 max-w-content-prose px-4 text-center text-[calc(1.125rem*1.3)] font-medium leading-relaxed text-gray-800"
          >
            {PANEL_CAPTION_LINE}
          </p>
        </div>
      )}
    </section>
  );
}
