import { env } from '../config/env.js';
import { createGeminiScorer } from './gemini.js';
import {
  AI_DISCLAIMER,
  type AiScorer,
  type Correction,
  type SpeakingFeedback,
  type SpeakingScoreInput,
  type WritingFeedback,
  type WritingScoreInput,
} from './ai-types.js';
import { countWords } from './text.js';

export type {
  AiScorer,
  Criterion,
  Correction,
  SpeakingFeedback,
  SpeakingPartValue,
  SpeakingScoreInput,
  WritingFeedback,
  WritingTaskTypeValue,
  WritingScoreInput,
} from './ai-types.js';
export { AI_DISCLAIMER } from './ai-types.js';

function clampBand(value: number): number {
  return Math.max(4, Math.min(7.5, Math.round(value * 2) / 2));
}

function roundOne(value: number): number {
  return Math.round(value * 10) / 10;
}

function uniqueWordRatio(text: string): number {
  const words = text
    .toLowerCase()
    .replace(/[^a-z'\s]/gu, ' ')
    .split(/\s+/u)
    .filter(Boolean);
  if (words.length === 0) {
    return 0;
  }
  return new Set(words).size / words.length;
}

function sentenceCount(text: string): number {
  return text
    .split(/[.!?]+/u)
    .map((sentence) => sentence.trim())
    .filter((sentence) => sentence.length > 0).length;
}

export const mockAiScorer: AiScorer = {
  provider: 'mock',

  async scoreWriting(input: WritingScoreInput): Promise<WritingFeedback> {
    const words = countWords(input.text);
    const sentences = sentenceCount(input.text);
    const variety = uniqueWordRatio(input.text);

    let band = 6;
    if (words < input.minWords * 0.6) {
      band -= 1;
    } else if (words < input.minWords) {
      band -= 0.5;
    } else if (words >= input.minWords + 40) {
      band += 0.25;
    }

    if (variety >= 0.55) {
      band += 0.25;
    }
    if (sentences < 5) {
      band -= 0.25;
    }
    if (sentences > 0 && words / sentences > 32) {
      band -= 0.25;
    }

    const overallBand = clampBand(band);
    // Keep the mock internally consistent: the overall estimate should match
    // the average of the criteria shown to the learner.
    const criterionBand = clampBand(overallBand);

    const strengths: string[] = [];
    if (words >= input.minWords) {
      strengths.push(`You reached the expected length (${words} words).`);
    }
    if (variety >= 0.5) {
      strengths.push('You used a reasonable range of vocabulary rather than repeating the same words.');
    }
    if (sentences >= 6) {
      strengths.push('Your answer is divided into several sentences, which helps readability.');
    }
    if (strengths.length === 0) {
      strengths.push('You produced a complete draft. The next step is to develop the ideas further.');
    }

    const improvements: string[] = [];
    if (words < input.minWords) {
      improvements.push(
        `Add about ${input.minWords - words} more words. Develop one idea with a reason and a specific example.`,
      );
    }
    if (sentences < 6) {
      improvements.push('Use more sentences so each main idea has room to develop.');
    }
    if (sentences > 0 && words / sentences > 32) {
      improvements.push('Some sentences are long. Split them so the reader can follow your argument.');
    }
    if (variety < 0.45) {
      improvements.push('Replace repeated words with precise synonyms and topic-specific vocabulary.');
    }
    improvements.push('Check that every body paragraph has a clear topic sentence and a supporting example.');

    const corrections: Correction[] = [];
    if (/\bvery\s+\w+/iu.test(input.text)) {
      const match = input.text.match(/\bvery\s+\w+/iu)?.[0] ?? 'very good';
      corrections.push({
        original: match,
        corrected: `a stronger adjective than “${match}”`,
        explanation: 'A precise adjective is usually more effective than “very + adjective”.',
      });
    }

    return {
      overallBand,
      confidenceLow: roundOne(Math.max(4, overallBand - 0.5)),
      confidenceHigh: roundOne(Math.min(8, overallBand + 0.5)),
      criteria: [
        {
          name: 'task_response',
          band: clampBand(overallBand),
          comment:
            words < input.minWords
              ? 'The answer is under the expected length, so some parts of the task may be underdeveloped.'
              : 'The answer addresses the task, but the argument could be extended with more specific support.',
        },
        {
          name: 'coherence_cohesion',
          band: criterionBand,
          comment: 'Ideas are generally understandable. Use clearer paragraph signposting and a wider range of linkers.',
        },
        {
          name: 'lexical_resource',
          band: criterionBand,
          comment:
            variety >= 0.5
              ? 'Vocabulary is varied enough for the task, with some imprecise word choices.'
              : 'Vocabulary is repetitive in places; add topic-specific collocations.',
        },
        {
          name: 'grammatical_range_accuracy',
          band: criterionBand,
          comment: 'A mix of simple and complex structures is present. Check articles, plurals, and verb forms.',
        },
      ],
      strengths,
      improvements,
      corrections,
      nextStep:
        'Rewrite the introduction with a clearer thesis statement, then add one concrete example to each body paragraph.',
      disclaimer: AI_DISCLAIMER,
    };
  },

  async scoreSpeaking(input: SpeakingScoreInput): Promise<SpeakingFeedback> {
    const words = countWords(input.transcript);
    const variety = uniqueWordRatio(input.transcript);

    let band = 6;
    if (words < 40) {
      band -= 1;
    } else if (words < 80) {
      band -= 0.5;
    } else if (words >= 180) {
      band += 0.25;
    }
    if (variety >= 0.55) {
      band += 0.25;
    }
    if (words > 0 && words / Math.max(1, input.durationSeconds ?? 60) < 0.8) {
      band -= 0.25;
    }

    const overallBand = clampBand(band);
    // Keep the mock internally consistent: the overall estimate should match
    // the average of the criteria shown to the learner.
    const criterionBand = clampBand(overallBand);

    const strengths: string[] = [];
    if (words >= 80) {
      strengths.push('You produced an extended answer rather than a one-line response.');
    }
    if (variety >= 0.5) {
      strengths.push('You used some varied vocabulary across the answer.');
    }
    if (strengths.length === 0) {
      strengths.push('You gave a complete answer. The next step is to extend each idea with a reason or example.');
    }

    const improvements: string[] = [];
    if (words < 80) {
      improvements.push('Extend your answer with a reason, an example, or a contrast from the past.');
    }
    if (variety < 0.45) {
      improvements.push('Paraphrase the question and use more topic-specific vocabulary.');
    }
    improvements.push('Use a range of linking phrases to connect ideas naturally, not just “and” or “because”.');
    improvements.push('Practise speaking for the full time limit without stopping to plan the next sentence.');

    return {
      overallBand,
      confidenceLow: roundOne(Math.max(4, overallBand - 0.5)),
      confidenceHigh: roundOne(Math.min(8, overallBand + 0.5)),
      criteria: [
        {
          name: 'fluency_coherence',
          band: criterionBand,
          comment: 'The answer is understandable. Work on extending ideas smoothly and reducing long pauses.',
        },
        {
          name: 'lexical_resource',
          band: criterionBand,
          comment:
            variety >= 0.5
              ? 'Some good vocabulary is used, but more precise collocations would raise the score.'
              : 'Vocabulary is limited in range; build topic-specific word groups.',
        },
        {
          name: 'grammatical_range_accuracy',
          band: criterionBand,
          comment: 'Basic structures are controlled. Practise complex sentences and accurate verb tenses.',
        },
        {
          name: 'pronunciation',
          band: null,
          comment: 'Pronunciation is not assessed from a transcript. Record audio for a future pronunciation review.',
        },
      ],
      strengths,
      improvements,
      corrections: [],
      nextStep: 'Record the same answer again and aim to add one extra detail to each bullet point.',
      pronunciationNote:
        'This estimate uses a transcript only. It cannot judge pronunciation, stress, or intonation reliably.',
      disclaimer: AI_DISCLAIMER,
    };
  },
};

export function getAiScorer(): AiScorer {
  if (env.AI_SCORING_MODE === 'mock') {
    return mockAiScorer;
  }
  if (env.AI_SCORING_MODE === 'gemini' && !env.GEMINI_API_KEY) {
    throw new Error('GEMINI_API_KEY is required when AI_SCORING_MODE=gemini');
  }
  if (env.AI_SCORING_MODE === 'gemini' || env.GEMINI_API_KEY) {
    return createGeminiScorer();
  }
  return mockAiScorer;
}
