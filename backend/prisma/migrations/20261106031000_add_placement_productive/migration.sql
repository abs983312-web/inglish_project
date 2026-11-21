-- AlterTable
ALTER TABLE "placement_sessions" ADD COLUMN "writing_task_json" JSONB NOT NULL DEFAULT '{}';
ALTER TABLE "placement_sessions" ADD COLUMN "speaking_prompt_json" JSONB NOT NULL DEFAULT '{}';
ALTER TABLE "placement_sessions" ADD COLUMN "writing_text" TEXT;
ALTER TABLE "placement_sessions" ADD COLUMN "speaking_transcript" TEXT;
ALTER TABLE "placement_sessions" ADD COLUMN "writing_band" DOUBLE PRECISION;
ALTER TABLE "placement_sessions" ADD COLUMN "speaking_band" DOUBLE PRECISION;
ALTER TABLE "placement_sessions" ADD COLUMN "writing_feedback_json" JSONB;
ALTER TABLE "placement_sessions" ADD COLUMN "speaking_feedback_json" JSONB;
