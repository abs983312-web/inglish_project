import { describe, expect, it } from 'vitest';
import {
  BILLING_PRODUCTS,
  findProduct,
  normaliseProductId,
  planForProduct,
  productCatalog,
} from '../src/modules/billing/products.js';
import {
  decodeRtdn,
  parseMockToken,
  statusFromState,
  type PlaySubscriptionState,
} from '../src/modules/billing/verify.js';

describe('billing products', () => {
  it('maps every configured product to a paid plan', () => {
    for (const product of BILLING_PRODUCTS) {
      expect(['PRO', 'TEACHER']).toContain(product.plan);
      expect(product.periodDays).toBeGreaterThan(0);
    }
  });

  it('normalises Play subscription IDs with a base plan suffix', () => {
    expect(normaliseProductId('inglish_pro_monthly:monthly-base')).toBe(
      'inglish_pro_monthly',
    );
    expect(normaliseProductId('inglish_teacher_monthly')).toBe('inglish_teacher_monthly');
  });

  it('resolves plans from normalised product IDs', () => {
    expect(planForProduct('inglish_pro_monthly:abc')).toBe('PRO');
    expect(planForProduct('inglish_teacher_monthly:abc')).toBe('TEACHER');
    expect(() => planForProduct('unknown')).toThrow();
  });

  it('exposes a safe public catalog without internal fields', () => {
    for (const item of productCatalog()) {
      expect(item).not.toHaveProperty('periodDays');
      expect(item.productId).toBeTruthy();
    }
  });

  it('rejects unknown products', () => {
    expect(findProduct('nope')).toBeNull();
  });
});

describe('mock purchase tokens', () => {
  it('grants the requested plan for the requested number of days', () => {
    const verified = parseMockToken('mock:inglish_pro_monthly:7');
    expect(verified).not.toBeNull();
    expect(verified?.productId).toBe('inglish_pro_monthly');
    expect(verified?.mock).toBe(true);
    expect(verified?.state).toBe('SUBSCRIPTION_STATE_ACTIVE');

    const days =
      (verified!.expiryTime!.getTime() - verified!.startTime.getTime()) / 86_400_000;
    expect(Math.round(days)).toBe(7);
  });

  it('falls back to 30 days for invalid durations', () => {
    const verified = parseMockToken('mock:inglish_pro_monthly:not-a-number');
    const days =
      (verified!.expiryTime!.getTime() - verified!.startTime.getTime()) / 86_400_000;
    expect(Math.round(days)).toBe(30);
  });

  it('rejects unknown products and non-mock tokens', () => {
    expect(parseMockToken('mock:does_not_exist:30')).toBeNull();
    expect(parseMockToken('real-token')).toBeNull();
  });
});

describe('subscription state mapping', () => {
  const cases: Array<[PlaySubscriptionState, string]> = [
    ['SUBSCRIPTION_STATE_ACTIVE', 'ACTIVE'],
    ['SUBSCRIPTION_STATE_IN_GRACE_PERIOD', 'GRACE'],
    ['SUBSCRIPTION_STATE_ON_HOLD', 'ON_HOLD'],
    ['SUBSCRIPTION_STATE_CANCELED', 'CANCELED'],
    ['SUBSCRIPTION_STATE_PAUSED', 'CANCELED'],
    ['SUBSCRIPTION_STATE_PENDING', 'CANCELED'],
    ['SUBSCRIPTION_STATE_EXPIRED', 'EXPIRED'],
    ['SUBSCRIPTION_STATE_UNSPECIFIED', 'CANCELED'],
  ];

  it.each(cases)('maps %s to %s', (state, expected) => {
    expect(statusFromState(state)).toBe(expected);
  });
});

describe('RTDN decoding', () => {
  it('decodes a Pub/Sub base64 payload', () => {
    const notification = {
      packageName: 'uz.inglish.app',
      subscriptionNotification: {
        notificationType: 2,
        purchaseToken: 'token-1',
        subscriptionId: 'inglish_pro_monthly',
      },
    };
    const body = {
      message: {
        data: Buffer.from(JSON.stringify(notification), 'utf8').toString('base64'),
        messageId: '1',
      },
      subscription: 'projects/p/subscriptions/s',
    };

    expect(decodeRtdn(body)).toEqual(notification);
  });

  it('returns null for malformed payloads', () => {
    expect(decodeRtdn({})).toBeNull();
    expect(decodeRtdn({ message: { data: 'not-base64-json' } })).toBeNull();
  });
});
