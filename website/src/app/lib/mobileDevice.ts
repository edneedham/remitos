/**
 * Best-effort client-side detection for phones / tablets where we want the
 * mobile signup flow. Not used for security—only UX routing.
 */
type NavigatorWithUaData = Navigator & {
  userAgentData?: { mobile?: boolean };
};

export type DevicePlatform = 'desktop' | 'android' | 'ios' | 'other_mobile';

export function detectDevicePlatform(): DevicePlatform {
  if (typeof navigator === 'undefined') return 'desktop';
  const nav = navigator as NavigatorWithUaData;
  const ua = nav.userAgent;

  if (/Android/i.test(ua)) return 'android';
  if (/iPhone|iPod|iPad/i.test(ua)) return 'ios';
  // iPadOS 13+ often reports as Mac with touch
  if (/Macintosh/i.test(ua) && nav.maxTouchPoints > 1) return 'ios';

  const mobileHint = nav.userAgentData?.mobile === true;
  if (mobileHint || /webOS|BlackBerry|IEMobile|Opera Mini/i.test(ua)) {
    return 'other_mobile';
  }
  return 'desktop';
}

export function isLikelyMobileDevice(): boolean {
  return detectDevicePlatform() !== 'desktop';
}
