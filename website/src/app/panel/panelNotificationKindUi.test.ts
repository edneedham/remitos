import { describe, expect, it } from 'vitest';
import { Bell, Receipt } from 'lucide-react';
import { panelNotificationKindUi } from './panelNotificationKindUi';

describe('panelNotificationKindUi', () => {
  it('returns mapped icon for known kinds', () => {
    const ui = panelNotificationKindUi('invoice_paid');
    expect(ui.Icon).toBe(Receipt);
    expect(ui.tone).toBe('positive');
  });

  it('falls back to bell for unknown kinds', () => {
    const ui = panelNotificationKindUi('future_kind_from_api');
    expect(ui.Icon).toBe(Bell);
    expect(ui.tone).toBe('neutral');
  });
});
