-- User-generated Kahoot-style battle packs: a YouTube lesson + questions.

CREATE TYPE "BattleQuestionType" AS ENUM ('MCQ', 'TRUE_FALSE');

ALTER TYPE "BattleQuestionSource" ADD VALUE IF NOT EXISTS 'PACK';

CREATE TABLE "battle_packs" (
  "id" UUID NOT NULL,
  "creator_id" UUID NOT NULL,
  "title" VARCHAR(120) NOT NULL,
  "description" VARCHAR(1000),
  "video_provider" VARCHAR(24) NOT NULL DEFAULT 'YOUTUBE',
  "video_id" VARCHAR(32) NOT NULL,
  "video_url" VARCHAR(500) NOT NULL,
  "video_title" VARCHAR(300),
  "thumbnail_url" VARCHAR(500),
  "duration_sec" INTEGER,
  "tags" TEXT[] DEFAULT ARRAY[]::TEXT[],
  "is_public" BOOLEAN NOT NULL DEFAULT true,
  "play_count" INTEGER NOT NULL DEFAULT 0,
  "like_count" INTEGER NOT NULL DEFAULT 0,
  "question_count" INTEGER NOT NULL DEFAULT 0,
  "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP(3) NOT NULL,

  CONSTRAINT "battle_packs_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "battle_pack_questions" (
  "id" UUID NOT NULL,
  "pack_id" UUID NOT NULL,
  "position" INTEGER NOT NULL,
  "type" "BattleQuestionType" NOT NULL DEFAULT 'MCQ',
  "prompt" VARCHAR(400) NOT NULL,
  "options_json" JSONB NOT NULL,
  "correct_index" INTEGER NOT NULL,
  "explanation" VARCHAR(600),
  "time_limit_ms" INTEGER NOT NULL DEFAULT 20000,
  "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT "battle_pack_questions_pkey" PRIMARY KEY ("id")
);

ALTER TABLE "battle_rooms" ADD COLUMN "pack_id" UUID;

CREATE INDEX "battle_packs_creator_id_created_at_idx" ON "battle_packs"("creator_id", "created_at");
CREATE INDEX "battle_packs_is_public_play_count_idx" ON "battle_packs"("is_public", "play_count");
CREATE INDEX "battle_packs_created_at_idx" ON "battle_packs"("created_at");
CREATE UNIQUE INDEX "battle_pack_questions_pack_id_position_key" ON "battle_pack_questions"("pack_id", "position");
CREATE INDEX "battle_pack_questions_pack_id_idx" ON "battle_pack_questions"("pack_id");
CREATE INDEX "battle_rooms_pack_id_idx" ON "battle_rooms"("pack_id");

ALTER TABLE "battle_packs"
  ADD CONSTRAINT "battle_packs_creator_id_fkey"
  FOREIGN KEY ("creator_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "battle_pack_questions"
  ADD CONSTRAINT "battle_pack_questions_pack_id_fkey"
  FOREIGN KEY ("pack_id") REFERENCES "battle_packs"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "battle_rooms"
  ADD CONSTRAINT "battle_rooms_pack_id_fkey"
  FOREIGN KEY ("pack_id") REFERENCES "battle_packs"("id") ON DELETE SET NULL ON UPDATE CASCADE;
