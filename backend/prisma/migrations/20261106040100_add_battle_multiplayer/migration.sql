-- 6-digit room codes, lobby/round timing, ready state, and presence.
ALTER TABLE "battle_rooms"
  ADD COLUMN "code" TEXT,
  ADD COLUMN "reveal_ms" INTEGER NOT NULL DEFAULT 4000,
  ADD COLUMN "max_players" INTEGER NOT NULL DEFAULT 10,
  ADD COLUMN "round_ends_at" TIMESTAMP(3),
  ADD COLUMN "reveal_ends_at" TIMESTAMP(3),
  ADD COLUMN "started_at" TIMESTAMP(3);

ALTER TABLE "battle_rooms" ALTER COLUMN "status" SET DEFAULT 'LOBBY';

ALTER TABLE "battle_participants"
  ADD COLUMN "is_ready" BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN "last_seen_at" TIMESTAMP(3),
  ADD COLUMN "left_at" TIMESTAMP(3);

CREATE UNIQUE INDEX "battle_rooms_code_key" ON "battle_rooms"("code");
CREATE INDEX "battle_rooms_status_created_at_idx" ON "battle_rooms"("status", "created_at");
