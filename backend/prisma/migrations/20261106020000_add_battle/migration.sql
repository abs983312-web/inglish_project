-- CreateEnum
CREATE TYPE "BattleMode" AS ENUM ('BOT');

-- CreateEnum
CREATE TYPE "BattleStatus" AS ENUM ('ACTIVE', 'FINISHED');

-- CreateEnum
CREATE TYPE "BattleQuestionSource" AS ENUM ('EXERCISE', 'VOCABULARY');

-- CreateTable
CREATE TABLE "battle_rooms" (
    "id" UUID NOT NULL,
    "host_id" UUID NOT NULL,
    "mode" "BattleMode" NOT NULL DEFAULT 'BOT',
    "status" "BattleStatus" NOT NULL DEFAULT 'ACTIVE',
    "question_count" INTEGER NOT NULL,
    "current_position" INTEGER NOT NULL DEFAULT 1,
    "time_limit_ms" INTEGER NOT NULL DEFAULT 20000,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "finished_at" TIMESTAMP(3),

    CONSTRAINT "battle_rooms_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "battle_participants" (
    "id" UUID NOT NULL,
    "room_id" UUID NOT NULL,
    "user_id" UUID,
    "name" TEXT NOT NULL,
    "is_bot" BOOLEAN NOT NULL DEFAULT false,
    "score" INTEGER NOT NULL DEFAULT 0,
    "correct_count" INTEGER NOT NULL DEFAULT 0,
    "streak" INTEGER NOT NULL DEFAULT 0,
    "best_streak" INTEGER NOT NULL DEFAULT 0,
    "rank" INTEGER,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "battle_participants_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "battle_rounds" (
    "id" UUID NOT NULL,
    "room_id" UUID NOT NULL,
    "position" INTEGER NOT NULL,
    "source" "BattleQuestionSource" NOT NULL,
    "prompt" TEXT NOT NULL,
    "options_json" JSONB NOT NULL,
    "correct_index" INTEGER NOT NULL,
    "explanation" TEXT,
    "started_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "battle_rounds_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "battle_answers" (
    "id" UUID NOT NULL,
    "round_id" UUID NOT NULL,
    "participant_id" UUID NOT NULL,
    "answer_index" INTEGER NOT NULL,
    "is_correct" BOOLEAN NOT NULL,
    "response_ms" INTEGER NOT NULL,
    "score_gained" INTEGER NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "battle_answers_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "battle_rooms_host_id_created_at_idx" ON "battle_rooms"("host_id", "created_at");

-- CreateIndex
CREATE INDEX "battle_participants_room_id_idx" ON "battle_participants"("room_id");

-- CreateIndex
CREATE UNIQUE INDEX "battle_participants_room_id_user_id_key" ON "battle_participants"("room_id", "user_id");

-- CreateIndex
CREATE UNIQUE INDEX "battle_rounds_room_id_position_key" ON "battle_rounds"("room_id", "position");

-- CreateIndex
CREATE UNIQUE INDEX "battle_answers_round_id_participant_id_key" ON "battle_answers"("round_id", "participant_id");

-- CreateIndex
CREATE INDEX "battle_answers_participant_id_idx" ON "battle_answers"("participant_id");

-- AddForeignKey
ALTER TABLE "battle_rooms" ADD CONSTRAINT "battle_rooms_host_id_fkey" FOREIGN KEY ("host_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "battle_participants" ADD CONSTRAINT "battle_participants_room_id_fkey" FOREIGN KEY ("room_id") REFERENCES "battle_rooms"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "battle_participants" ADD CONSTRAINT "battle_participants_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "battle_rounds" ADD CONSTRAINT "battle_rounds_room_id_fkey" FOREIGN KEY ("room_id") REFERENCES "battle_rooms"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "battle_answers" ADD CONSTRAINT "battle_answers_round_id_fkey" FOREIGN KEY ("round_id") REFERENCES "battle_rounds"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "battle_answers" ADD CONSTRAINT "battle_answers_participant_id_fkey" FOREIGN KEY ("participant_id") REFERENCES "battle_participants"("id") ON DELETE CASCADE ON UPDATE CASCADE;
