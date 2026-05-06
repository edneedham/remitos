import { describe, expect, it, beforeEach } from 'vitest';
import {
  allowContactoSubmission,
  resetContactoRateLimitsForTests,
} from './contactoRateLimit';

beforeEach(() => {
  resetContactoRateLimitsForTests();
});

describe('allowContactoSubmission', () => {
  it('allows requests under the limit', () => {
    for (let i = 0; i < 10; i++) {
      expect(allowContactoSubmission('192.168.1.1')).toBe(true);
    }
  });

  it('blocks after exceeding the per-window limit', () => {
    for (let i = 0; i < 10; i++) {
      allowContactoSubmission('10.0.0.1');
    }
    expect(allowContactoSubmission('10.0.0.1')).toBe(false);
  });

  it('tracks IPs independently', () => {
    for (let i = 0; i < 10; i++) {
      allowContactoSubmission('10.0.0.2');
    }
    expect(allowContactoSubmission('10.0.0.3')).toBe(true);
  });
});
