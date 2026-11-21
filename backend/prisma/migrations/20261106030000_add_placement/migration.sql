-- CreateEnum
CREATE TYPE "PlacementStatus" AS ENUM ('ACTIVE', 'FINISHED');

-- CreateEnum
CREATE TYPE "PlacementSection" AS ENUM ('GRAMMAR', 'VOCABULARY', 'READING');

-- CreateTable
CREATE TABLE "placement_sessions" (
    "id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "status" "PlacementStatus" NOT NULL DEFAULT 'ACTIVE',
    "questions_json" JSONB NOT NULL,
    "answers_json" JSONB,
    "correct_count" INTEGER,
    "total_count" INTEGER,
    "percentage" DOUBLE PRECISION,
    "cefr_level" TEXT,
    "estimated_band" DOUBLE PRECISION,
    "weak_skill" "PlacementSection",
    "section_stats_json" JSONB,
    "started_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "finished_at" TIMESTAMP(3),
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "placement_sessions_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "placement_sessions_user_id_created_at_idx" ON "placement_sessions"("user_id", "created_at");

-- AddForeignKey
ALTER TABLE "placement_sessions" ADD CONSTRAINT "placement_sessions_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;
