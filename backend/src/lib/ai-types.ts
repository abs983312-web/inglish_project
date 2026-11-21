export const AI_DISCLAIMER =
  'This is an AI practice estimate, not an official IELTS score. Use it to track progress, not for formal decisions.';

export type WritingTaskTypeValue = 'TASK_1' | 'TASK_2';
export type SpeakingPartValue = 'PART_1' | 'PART_2' | 'PART_3';

export type WritingScoreInput = {
  prompt: string;
  taskType: WritingTaskTypeValue;
  minWords: number;
  text: string;
};

export type SpeakingScoreInput = {
  topic: string;
  part: SpeakingPartValue;
  questions: unknown;
  transcript: string;
  durationSeconds?: number;
};

export type Criterion = {
  name: string;
  band: number | null;
  comment: string;
};

export type Correction = {
  original: string;
  corrected: string;
  explanation: string;
};

export type WritingFeedback = {
  overallBand: number;
  confidenceLow: number;
  confidenceHigh: number;
  criteria: Criterion[];
  strengths: string[];
  improvements: string[];
  corrections: Correction[];
  nextStep: string;
  disclaimer: string;
};

export type SpeakingFeedback = {
  overallBand: number;
  confidenceLow: number;
  confidenceHigh: number;
  criteria: Criterion[];
  strengths: string[];
  improvements: string[];
  corrections: Correction[];
  nextStep: string;
  pronunciationNote: string;
  disclaimer: string;
};

export interface AiScorer {
  readonly provider: string;
  scoreWriting(input: WritingScoreInput): Promise<WritingFeedback>;
  scoreSpeaking(input: SpeakingScoreInput): Promise<SpeakingFeedback>;
}
