'use client';

import Image from 'next/image';
import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import gsap from 'gsap';
import { ensureGsapScrollTrigger } from '../../../lib/gsapClient';
import BenefitsSectionMobile from './BenefitsSectionMobile';
import {
  FIRST_BENEFIT_LINE,
  PANEL_CAPTION_LINE,
  SECOND_BENEFIT_LINE,
  THIRD_BENEFIT_BEFORE,
  THIRD_BENEFIT_PRODUCT,
} from './benefitsContent';

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
    if (!window.matchMedia('(min-width: 768px)').matches) {
      return;
    }

    const section = sectionRef.current;
    const shot = screenshotRef.current;
    const benefitEl = benefitTextRef.current;
    const benefit2El = benefitText2Ref.current;
    const benefit3El = benefitText3Ref.current;
    if (!section || !shot || !benefitEl || !benefit2El || !benefit3El) {
      return;
    }

    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      ensureGsapScrollTrigger();
      gsap.set(shot, { autoAlpha: 1, y: 0, overwrite: 'auto' });
      gsap.set([benefitEl, benefit2El, benefit3El], {
        autoAlpha: 1,
        x: 0,
        overwrite: 'auto',
      });
      screenshotRevealedRef.current = true;
      firstBenefitRevealedRef.current = true;
      secondBenefitRevealedRef.current = true;
      thirdBenefitRevealedRef.current = true;
      panelSwapCompleteRef.current = true;
      queueMicrotask(() => setPanelOnlyLayout(true));
      return () => {};
    }

    ensureGsapScrollTrigger();

    gsap.set(shot, {
      autoAlpha: 0,
      y: 28,
      force3D: true,
    });

    gsap.set([benefitEl, benefit2El, benefit3El], {
      autoAlpha: 0,
      x: -36,
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
        advanceCooldownUntilRef.current = performance.now() + 420;
        maybeArmLock();
        return;
      }

      if (!lockArmedRef.current) return;
      if (e.deltaY < 0) {
        unlockScroll();
        return;
      }
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
      if (['ArrowUp', 'PageUp', 'Home'].includes(e.key)) {
        unlockScroll();
        return;
      }
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
    if (!window.matchMedia('(min-width: 768px)').matches) return;

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
      className="scroll-mt-0 border-b border-gray-200 bg-white py-8 px-4 sm:py-14 sm:px-6 lg:px-8 lg:py-20"
      aria-labelledby="benefits-heading"
    >
      <div className="mx-auto w-full max-w-[68.8rem]">
        <div className="mx-auto mb-8 max-w-content-prose text-left max-md:mb-6 md:mb-12 md:text-center">
          <h2
            id="benefits-heading"
            className="text-3xl font-bold text-gray-900 sm:text-4xl lg:text-5xl"
          >
            Menos carga manual, más control operativo
          </h2>
        </div>
      </div>

      <div className="md:hidden">
        <BenefitsSectionMobile />
      </div>

      <div className="mx-auto mt-10 hidden w-full max-w-[68.8rem] md:block">
        {!panelOnlyLayout ? (
          <div className="grid grid-cols-1 items-center gap-x-6 gap-y-6 md:grid-cols-[minmax(0,1fr)_auto] lg:gap-x-10">
            <div className="flex max-w-xl flex-col gap-6 justify-self-start md:col-start-1 md:self-center">
              <p
                ref={benefitTextRef}
                className="border-l-[3px] border-blue-600 pl-4 text-left text-xl font-semibold leading-snug tracking-tight text-gray-900 sm:text-2xl lg:text-3xl md:[visibility:hidden]"
              >
                {FIRST_BENEFIT_LINE}
              </p>
              <p
                ref={benefitText2Ref}
                className="border-l-[3px] border-blue-600 pl-4 text-left text-xl font-semibold leading-snug tracking-tight text-gray-900 sm:text-2xl lg:text-3xl md:[visibility:hidden]"
              >
                {SECOND_BENEFIT_LINE}
              </p>
              <p
                ref={benefitText3Ref}
                className="flex flex-wrap items-baseline gap-x-2 gap-y-1 border-l-[3px] border-blue-600 pl-4 text-left text-xl font-semibold leading-snug tracking-tight text-gray-900 sm:text-2xl lg:text-3xl md:[visibility:hidden]"
              >
                <span>{THIRD_BENEFIT_BEFORE}</span>
                <span className="inline-flex items-center gap-1.5">
                  <Image
                    src="/brands/google-drive-logo.png"
                    alt=""
                    width={28}
                    height={28}
                    className="size-7 shrink-0 translate-y-0.5"
                    aria-hidden
                  />
                  <span className="whitespace-nowrap">{THIRD_BENEFIT_PRODUCT}</span>
                </span>
                <span aria-hidden>.</span>
              </p>
            </div>

            <div className="justify-self-center md:col-start-2 md:row-start-1 md:justify-self-end">
              <div
                ref={screenshotRef}
                className="relative w-fit shrink-0 overflow-hidden rounded-2xl p-4 md:[visibility:hidden]"
              >
                <Image
                  src="/screenshots/dashboard.png"
                  alt="Vista del dashboard de la aplicación En Punto"
                  width={286}
                  height={611}
                  className="h-auto w-[264px] rounded-xl"
                  sizes="264px"
                />
              </div>
            </div>
          </div>
        ) : (
          <div ref={panelFullRef}>
            <div className="relative w-full overflow-hidden rounded-xl bg-white shadow-[0_20px_60px_rgba(15,23,42,0.28),0_8px_20px_rgba(15,23,42,0.18)] transition-transform duration-300 hover:scale-[1.01] hover:shadow-[0_28px_80px_rgba(15,23,42,0.34),0_12px_28px_rgba(15,23,42,0.22)]">
              <div className="pointer-events-none absolute inset-0 -z-10 rounded-2xl bg-gradient-to-br from-blue-200/40 via-transparent to-indigo-300/30 blur-2xl" />
              <Image
                src="/screenshots/web-panel-screenshot.png"
                alt="Panel web de administración En Punto"
                width={3002}
                height={1651}
                className="h-auto w-full rounded-xl object-contain"
                sizes="100vw"
                priority
              />
            </div>
            <p
              ref={panelCaptionRef}
              className="mx-auto mt-8 max-w-content-prose px-4 text-left text-xl font-semibold leading-snug tracking-tight text-gray-900 sm:text-2xl lg:text-3xl md:text-center"
            >
              {PANEL_CAPTION_LINE}
            </p>
          </div>
        )}
      </div>
    </section>
  );
}
