import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

let scrollTriggerRegistered = false;

/** Register ScrollTrigger once per browser session (safe across lazy sections). */
export function ensureGsapScrollTrigger(): void {
  if (typeof window === 'undefined' || scrollTriggerRegistered) return;
  gsap.registerPlugin(ScrollTrigger);
  scrollTriggerRegistered = true;
}
