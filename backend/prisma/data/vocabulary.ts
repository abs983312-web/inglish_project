import { educationAndWorkVocabulary } from './vocabulary-education-work.js';
import { environmentAndTechnologyVocabulary } from './vocabulary-environment-technology.js';
import { healthAndSocietyVocabulary } from './vocabulary-health-society.js';
import { governmentAndUrbanVocabulary } from './vocabulary-government-urban.js';
import { mediaAndAcademicVocabulary } from './vocabulary-media-academic.js';
import type { SeedVocabularyItem } from './vocabulary-types.js';

/**
 * 500 curated IELTS words grouped into ten topic sets.
 * Set metadata lives in `src/lib/vocabulary-sets.ts`; each item carries
 * the matching `set-*` tag.
 */
export const VOCABULARY_SEED: SeedVocabularyItem[] = [
  ...educationAndWorkVocabulary,
  ...environmentAndTechnologyVocabulary,
  ...healthAndSocietyVocabulary,
  ...governmentAndUrbanVocabulary,
  ...mediaAndAcademicVocabulary,
];
