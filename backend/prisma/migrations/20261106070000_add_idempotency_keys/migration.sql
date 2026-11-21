-- Replayable responses for client-supplied Idempotency-Key headers.
-- Only successful (2xx) responses are stored; rows expire after 24 hours.

CREATE TABLE "idempotency_keys" (
    "id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "key" TEXT NOT NULL,
    "method" TEXT NOT NULL,
    "path" TEXT NOT NULL,
    "request_hash" TEXT NOT NULL,
    "status_code" INTEGER NOT NULL,
    "content_type" TEXT NOT NULL,
    "response_body" TEXT NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "expires_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "idempotency_keys_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "idempotency_keys_user_id_key_key"
    ON "idempotency_keys"("user_id", "key");

CREATE INDEX "idempotency_keys_expires_at_idx"
    ON "idempotency_keys"("expires_at");

ALTER TABLE "idempotency_keys"
    ADD CONSTRAINT "idempotency_keys_user_id_fkey"
    FOREIGN KEY ("user_id") REFERENCES "users"("id")
    ON DELETE CASCADE ON UPDATE CASCADE;
