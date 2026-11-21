import type { SubscriptionPlan } from '@prisma/client';

/**
 * Google Play product IDs. They must match the subscription products created in
 * the Play Console exactly (base plan IDs are appended with `:basePlanId` in
 * purchase tokens, so we match on the prefix before the colon).
 */
export interface BillingProduct {
  productId: string;
  plan: Extract<SubscriptionPlan, 'PRO' | 'TEACHER'>;
  /** Nominal billing period, used to compute a fallback expiry when Play omits it. */
  periodDays: number;
  title: string;
  description: string;
}

export const BILLING_PRODUCTS: BillingProduct[] = [
  {
    productId: 'inglish_pro_monthly',
    plan: 'PRO',
    periodDays: 30,
    title: 'Pro - oylik',
    description: "Cheksizga yaqin Writing/Speaking kvotasi, Battle 10 o'yinchi",
  },
  {
    productId: 'inglish_pro_yearly',
    plan: 'PRO',
    periodDays: 365,
    title: 'Pro - yillik',
    description: "Yillik Pro: 12 oyga chegirma bilan",
  },
  {
    productId: 'inglish_teacher_monthly',
    plan: 'TEACHER',
    periodDays: 30,
    title: 'Teacher - oylik',
    description: "Sinf xonalari, 30 o'yinchi, yuqori AI kvotasi",
  },
];

const PRODUCT_BY_ID = new Map(BILLING_PRODUCTS.map((product) => [product.productId, product]));

/** Play returns `productId:basePlanId` for subscriptions; normalise it. */
export function normaliseProductId(raw: string): string {
  const colon = raw.indexOf(':');
  return colon === -1 ? raw : raw.slice(0, colon);
}

export function findProduct(productId: string): BillingProduct | null {
  return PRODUCT_BY_ID.get(normaliseProductId(productId)) ?? null;
}

export function planForProduct(productId: string): Extract<SubscriptionPlan, 'PRO' | 'TEACHER'> {
  const product = findProduct(productId);
  if (!product) {
    throw new Error(`Unknown billing product: ${productId}`);
  }
  return product.plan;
}

export function productCatalog() {
  return BILLING_PRODUCTS.map(({ productId, plan, title, description, periodDays }) => ({
    productId,
    plan,
    title,
    description,
    days: periodDays,
  }));
}
