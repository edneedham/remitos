import { describe, expect, it } from 'vitest';
import { parseWebProfile, parseEntitlement } from './panelApi';

describe('panelApi parsers', () => {
  it('parseWebProfile accepts a complete profile', () => {
    const p = parseWebProfile({
      id: '1',
      username: 'a',
      company_id: 'c',
      company_name: 'Co',
      company_code: 'X',
      role: 'admin',
    });
    expect(p?.role).toBe('admin');
  });

  it('parseWebProfile rejects partial objects', () => {
    expect(parseWebProfile({ id: '1' })).toBeNull();
  });

  it('parseEntitlement requires can_download_app', () => {
    expect(
      parseEntitlement({
        can_download_app: true,
        subscription_plan: 'trial',
      }),
    ).toMatchObject({ can_download_app: true, subscription_plan: 'trial' });
    expect(parseEntitlement({})).toBeNull();
  });
});
