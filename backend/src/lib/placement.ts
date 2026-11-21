export type PlacementSection = 'GRAMMAR' | 'VOCABULARY' | 'READING';

export type PlacementQuestion = {
  id: string;
  section: PlacementSection;
  prompt: string;
  passage?: string;
  options: string[];
  correctIndex: number;
  explanation: string;
};

export type PlacementSectionStat = {
  correct: number;
  total: number;
};

export type PlacementSectionStats = Record<PlacementSection, PlacementSectionStat>;

export const PLACEMENT_GRAMMAR: PlacementQuestion[] = [
  { id: 'g1', section: 'GRAMMAR', prompt: 'I ___ in Tashkent since 2020.', options: ['live', 'am live', 'have lived', 'lived'], correctIndex: 2, explanation: '"Since 2020" — boshlangan va hozir ham davom etayotgan holat: have lived.' },
  { id: 'g2', section: 'GRAMMAR', prompt: 'She is ___ honest student.', options: ['a', 'an', 'the', '—'], correctIndex: 1, explanation: '"Honest" unli bilan boshlanadi, shuning uchun "an honest student".' },
  { id: 'g3', section: 'GRAMMAR', prompt: 'The results ___ yesterday.', options: ['was published', 'published', 'publish', 'are publish'], correctIndex: 0, explanation: 'O’tmishdagi egalik (passive): was published.' },
  { id: 'g4', section: 'GRAMMAR', prompt: 'If it ___ tomorrow, we will stay at home.', options: ['will rain', 'rains', 'rain', 'rained'], correctIndex: 1, explanation: 'First conditional: If + present simple (rains), will + verb.' },
  { id: 'g5', section: 'GRAMMAR', prompt: 'This book is ___ than the one I read last year.', options: ['more interesting', 'more interest', 'most interesting', 'interested'], correctIndex: 0, explanation: 'Qiyoslash (comparative): more interesting than.' },
  { id: 'g6', section: 'GRAMMAR', prompt: 'He is responsible ___ the project.', options: ['to', 'with', 'for', 'about'], correctIndex: 2, explanation: 'Iboraga e\'tibor qiling: responsible for.' },
  { id: 'g7', section: 'GRAMMAR', prompt: 'By the time we arrived, the film ___.', options: ['had started', 'started', 'starts', 'will start'], correctIndex: 0, explanation: 'O’tmishdan oldin tugagan harakat: past perfect — had started.' },
  { id: 'g8', section: 'GRAMMAR', prompt: 'Students ___ use phones during the exam.', options: ['must not', 'do not have to', 'not to', 'can to'], correctIndex: 0, explanation: 'Ta\'qiqlangan harakat: must not.' },
  { id: 'g9', section: 'GRAMMAR', prompt: 'I wish I ___ more time to prepare.', options: ['have', 'had', 'will have', 'having'], correctIndex: 1, explanation: 'Wish + past simple: I wish I had.' },
  { id: 'g10', section: 'GRAMMAR', prompt: 'The teacher asked us ___ the exercise again.', options: ['to do', 'doing', 'do', 'to doing'], correctIndex: 0, explanation: 'Ask somebody to do something — to do.' },
];

const READING_PASSAGE =
  'When the new tram line opened across the city, many residents expected less traffic on the main roads. In the first month, however, the tram was often crowded in the morning, and some passengers complained that the buses had been reduced too quickly. The transport office responded by adding extra services at peak hours and by improving the timetable information at stations. By the end of the year, surveys showed that more people were travelling by tram, while car numbers on the main roads had fallen slightly. The office now says the next step is to connect the line to the railway station.';

export const PLACEMENT_READING: PlacementQuestion[] = [
  { id: 'r1', section: 'READING', passage: READING_PASSAGE, prompt: 'What did many residents expect when the tram line opened?', options: ['Less traffic on the main roads', 'More buses', 'Higher fares', 'A new railway station'], correctIndex: 0, explanation: 'Matnda: "many residents expected less traffic on the main roads".' },
  { id: 'r2', section: 'READING', passage: READING_PASSAGE, prompt: 'What problem did passengers complain about?', options: ['The tram was too slow', 'The buses had been reduced too quickly', 'Stations were too small', 'Fares increased'], correctIndex: 1, explanation: 'Matnda: "some passengers complained that the buses had been reduced too quickly".' },
  { id: 'r3', section: 'READING', passage: READING_PASSAGE, prompt: 'What did the transport office do in response?', options: ['Stopped the tram service', 'Added extra services at peak hours', 'Reduced the timetable', 'Closed the stations'], correctIndex: 1, explanation: 'Matnda: "adding extra services at peak hours".' },
  { id: 'r4', section: 'READING', passage: READING_PASSAGE, prompt: 'What did the surveys show by the end of the year?', options: ['Fewer people used the tram', 'Car numbers on the main roads had fallen slightly', 'More people drove cars', 'Buses became more popular'], correctIndex: 1, explanation: 'Matnda: "car numbers on the main roads had fallen slightly".' },
  { id: 'r5', section: 'READING', passage: READING_PASSAGE, prompt: 'What is the next step mentioned in the text?', options: ['To buy more buses', 'To connect the line to the railway station', 'To reduce fares', 'To close the main roads'], correctIndex: 1, explanation: 'Matnda: "to connect the line to the railway station".' },
];

export const PLACEMENT_WRITING = {
  slug: 'placement-writing',
  taskType: 'TASK_2' as const,
  title: 'Placement yozuv',
  prompt:
    'Write a short paragraph (60-80 words) about the advantages of learning English for your future studies or work. Give one clear reason and one example.',
  minWords: 60,
  timeLimitMinutes: 5,
};

export const PLACEMENT_SPEAKING = {
  slug: 'placement-speaking',
  part: 'PART_1' as const,
  topic: 'Future plans',
  questions: [
    'What do you plan to do after your studies?',
    'Why is English important for your future?',
    'What would you like to improve in English?',
  ],
  prepTimeSeconds: 0,
  speakTimeSeconds: 60,
};

const SECTION_ORDER: PlacementSection[] = ['GRAMMAR', 'VOCABULARY', 'READING'];

export function emptySectionStats(): PlacementSectionStats {
  return {
    GRAMMAR: { correct: 0, total: 0 },
    VOCABULARY: { correct: 0, total: 0 },
    READING: { correct: 0, total: 0 },
  };
}

export function placementBandForPercentage(percentage: number): number {
  if (percentage < 30) return 4;
  if (percentage < 45) return 4.5;
  if (percentage < 60) return 5;
  if (percentage < 75) return 5.5;
  if (percentage < 90) return 6;
  return 6.5;
}

export function placementCefrForPercentage(percentage: number): 'B1' | 'B2' | 'C1' {
  if (percentage < 45) return 'B1';
  if (percentage < 75) return 'B2';
  return 'C1';
}

export function placementCefrForBand(band: number): 'B1' | 'B2' | 'C1' {
  if (band < 5) return 'B1';
  if (band < 6) return 'B2';
  return 'C1';
}

export function roundBand(value: number): number {
  return Math.round(value * 2) / 2;
}

export function weakestSection(stats: PlacementSectionStats): PlacementSection {
  let weakest: PlacementSection = 'GRAMMAR';
  let weakestRate = Infinity;

  for (const section of SECTION_ORDER) {
    const stat = stats[section];
    const rate = stat.total === 0 ? 0 : stat.correct / stat.total;
    if (rate < weakestRate) {
      weakest = section;
      weakestRate = rate;
    }
  }

  return weakest;
}

export function placementResult(correct: number, total: number, stats: PlacementSectionStats) {
  const percentage = total === 0 ? 0 : (correct / total) * 100;

  return {
    correct,
    total,
    percentage: Math.round(percentage * 10) / 10,
    cefrLevel: placementCefrForPercentage(percentage),
    estimatedBand: placementBandForPercentage(percentage),
    weakSkill: weakestSection(stats),
    sectionStats: stats,
  };
}
