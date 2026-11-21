import { describe, expect, it } from 'vitest';
import {
  PLACEMENT_GRAMMAR,
  PLACEMENT_READING,
  emptySectionStats,
  placementBandForPercentage,
  placementCefrForPercentage,
  placementResult,
  weakestSection,
} from '../src/lib/placement.js';

describe('placement scoring', () => {
  it('maps percentages to IELTS practice estimates', () => {
    expect(placementBandForPercentage(20)).toBe(4);
    expect(placementBandForPercentage(40)).toBe(4.5);
    expect(placementBandForPercentage(55)).toBe(5);
    expect(placementBandForPercentage(70)).toBe(5.5);
    expect(placementBandForPercentage(85)).toBe(6);
    expect(placementBandForPercentage(95)).toBe(6.5);
  });

  it('maps percentages to CEFR levels', () => {
    expect(placementCefrForPercentage(40)).toBe('B1');
    expect(placementCefrForPercentage(60)).toBe('B2');
    expect(placementCefrForPercentage(80)).toBe('C1');
  });

  it('picks the weakest section by accuracy', () => {
    const stats = emptySectionStats();
    stats.GRAMMAR = { correct: 9, total: 10 };
    stats.VOCABULARY = { correct: 5, total: 10 };
    stats.READING = { correct: 2, total: 5 };

    expect(weakestSection(stats)).toBe('READING');
  });

  it('produces a rounded result object', () => {
    const stats = emptySectionStats();
    stats.GRAMMAR = { correct: 8, total: 10 };
    stats.VOCABULARY = { correct: 7, total: 10 };
    stats.READING = { correct: 4, total: 5 };

    const result = placementResult(19, 25, stats);

    expect(result.percentage).toBe(76);
    expect(result.cefrLevel).toBe('C1');
    expect(result.estimatedBand).toBe(6);
    expect(result.weakSkill).toBe('VOCABULARY');
  });

  it('ships a valid static question bank', () => {
    expect(PLACEMENT_GRAMMAR).toHaveLength(10);
    expect(PLACEMENT_READING).toHaveLength(5);

    for (const question of [...PLACEMENT_GRAMMAR, ...PLACEMENT_READING]) {
      expect(question.options.length).toBeGreaterThanOrEqual(4);
      expect(question.correctIndex).toBeGreaterThanOrEqual(0);
      expect(question.correctIndex).toBeLessThan(question.options.length);
      expect(question.explanation.length).toBeGreaterThan(0);
    }
  });
});
