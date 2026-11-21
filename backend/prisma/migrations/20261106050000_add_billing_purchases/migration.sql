-- Add billing lifecycle statuses used by Google Play RTDN.
ALTER TYPE "SubscriptionStatus" ADD VALUE IF NOT EXISTS 'GRACE';
ALTER TYPE "SubscriptionStatus" ADD VALUE IF NOT EXISTS 'ON_HOLD';

-- Raw Google Play purchase records. `purchase_token` is globally unique so a
-- token can never be replayed across accounts.
CREATE TABLE "purchases" (
    "id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "provider" TEXT NOT NULL DEFAULT 'GOOGLE_PLAY',
    "product_id" TEXT NOT NULL,
    "purchase_token" TEXT NOT NULL,
    "order_id" TEXT,
    "package_name" TEXT NOT NULL,
    "status" TEXT NOT NULL DEFAULT 'PENDING',
    "acknowledged" BOOLEAN NOT NULL DEFAULT false,
    "purchase_time" TIMESTAMP(3) NOT NULL,
    "expiry_time" TIMESTAMP(3),
    "raw_json" JSONB NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "purchases_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "purchases_purchase_token_key" ON "purchases"("purchase_token");
CREATE INDEX "purchases_user_id_status_idx" ON "purchases"("user_id", "status");

ALTER TABLE "purchases"
    ADD CONSTRAINT "purchases_user_id_fkey"
    FOREIGN KEY ("user_id") REFERENCES "users"("id")
    ON DELETE CASCADE ON UPDATE CASCADE;
