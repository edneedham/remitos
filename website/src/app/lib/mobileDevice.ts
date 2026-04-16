/**
 * Best-effort client-side detection for phones / tablets where we want the
 * mobile signup flow. Not used for security—only UX routing.
 */
type NavigatorWithUaData = Navigator & {
  userAgentData?: { mobile?: boolean };
};

export function isLikelyMobileDevice(): boolean {
  if (typeof navigator === 'undefined') return false;

  const nav = navigator as NavigatorWithUaData;
  const ua = nav.userAgent;

  if (nav.userAgentData?.mobile === true) {
    return true;
  }

  if (/Android/i.test(ua)) return true;
  if (/iPhone|iPod/i.test(ua)) return true;
  if (/iPad/i.test(ua)) return true;

  // iPadOS 13+ often reports as Mac with touch
  if (/Macintosh/i.test(ua) && nav.maxTouchPoints > 1) {
    return true;
  }

  if (/webOS|BlackBerry|IEMobile|Opera Mini/i.test(ua)) return true;

  return false;
}
