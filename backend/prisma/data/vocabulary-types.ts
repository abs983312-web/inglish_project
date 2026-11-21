export type SeedVocabularyItem = {
  word: string;
  pos: string;
  cefr: 'B1' | 'B2' | 'C1';
  definitionUz: string;
  definitionEn: string;
  exampleEn: string;
  exampleUz: string;
  tags: string[];
};

export type SeedVocabularySet = {
  slug: string;
  title: string;
  description: string;
  tag: string;
};
