import type { NextRequest } from 'next/server';

/** Sliding window length for counting submissions per client. */
const WINDOW_MS = 60_000;

/** Max POST /api/contacto requests per IP per window (burst-friendly). */
const MAX_PER_WINDOW = 10;

/** Trim stale bucket entries when the map grows this large. */
const MAX_TRACKED_IPS = 5_000;

type Bucket = number[];

const buckets = new Map<string, Bucket>();

function pruneBucket(now: number, timestamps: Bucket): Bucket {
  return timestamps.filter((t) => now - t < WINDOW_MS);
}

function pruneMapIfNeeded(): void {
  if (buckets.size <= MAX_TRACKED_IPS) return;
  const now = Date.now();
  for (const [ip, ts] of buckets) {
    const next = pruneBucket(now, ts);
    if (next.length === 0) buckets.delete(ip);
    else buckets.set(ip, next);
  }
}

/**
 * Client IP for rate limiting (trusts x-forwarded-for / x-real-ip when present).
 */
export function getContactoClientIP(request: NextRequest): string {
  const forwarded = request.headers.get('x-forwarded-for');
  if (forwarded) {
    const first = forwarded.split(',')[0]?.trim();
    if (first) return first;
  }
  const realIp = request.headers.get('x-real-ip')?.trim();
  if (realIp) return realIp;
  return 'unknown';
}

/**
 * Returns true if the request is allowed; false if rate limited.
 * Best-effort only on multi-instance deploys (each instance has its own map).
 */
export function allowContactoSubmission(ip: string): boolean {
  const now = Date.now();
  pruneMapIfNeeded();

  let timestamps = buckets.get(ip);
  if (!timestamps) {
    timestamps = [];
    buckets.set(ip, timestamps);
  }

  timestamps = pruneBucket(now, timestamps);

  if (timestamps.length >= MAX_PER_WINDOW) {
    buckets.set(ip, timestamps);
    return false;
  }

  timestamps.push(now);
  buckets.set(ip, timestamps);
  return true;
}

/** Clears in-memory state (Vitest only). */
export function resetContactoRateLimitsForTests(): void {
  buckets.clear();
}
