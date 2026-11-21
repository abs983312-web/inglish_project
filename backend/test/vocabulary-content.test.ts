import { describe, expect, it } from 'vitest';
import { VOCABULARY_SEED } from '../prisma/data/vocabulary.js';
import { VOCABULARY_SETS } from '../src/lib/vocabulary-sets.js';

describe('vocabulary library', () => {
  it('contains 500 unique words', () => {
    expect(VOCABULARY_SEED).toHaveLength(500);
    expect(new Set(VOCABULARY_SEED.map((item) => item.word)).size).toBe(500);
  });

  it('has 50 words in each of the ten sets', () => {
    expect(VOCABULARY_SETS).toHaveLength(10);

    for (const set of VOCABULARY_SETS) {
      const items = VOCABULARY_SEED.filter((item) => item.tags.includes(set.tag));
      expect(items, `${set.slug} should contain 50 words`).toHaveLength(50);
    }
  });

  it('has complete learning fields for every word', () => {
    const fields = [
      'word',
      'pos',
      'cefr',
      'definitionUz',
      'definitionEn',
      'exampleEn',
      'exampleUz',
    ] as const;

    for (const item of VOCABULARY_SEED) {
      for (const field of fields) {
        expect(item[field].trim(), `${item.word} is missing ${field}`).not.toBe('');
      }
      expect(
        item.tags.some((tag) => tag.startsWith('set-')),
        `${item.word} is missing a set tag`,
      ).toBe(true);
    }
  });
});
