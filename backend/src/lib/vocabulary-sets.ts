export type VocabularySet = {
  slug: string;
  title: string;
  description: string;
  tag: string;
};

/**
 * The vocabulary library is grouped into ten IELTS topic sets.
 * Each item in the database carries the matching `set-*` tag.
 */
export const VOCABULARY_SETS: VocabularySet[] = [
  {
    slug: 'education',
    title: 'Ta’lim',
    description: 'Universitet, maktab va o‘qish jarayoni haqidagi so‘zlar.',
    tag: 'set-education',
  },
  {
    slug: 'work',
    title: 'Ish va karyera',
    description: 'Ishga qabul, maosh, ofis va karyera bo‘yicha lug‘at.',
    tag: 'set-work',
  },
  {
    slug: 'environment',
    title: 'Atrof-muhit',
    description: 'Iqlim, ifloslanish, energiya va tabiatni muhofaza qilish.',
    tag: 'set-environment',
  },
  {
    slug: 'technology',
    title: 'Texnologiya',
    description: 'Raqamli qurilmalar, internet, xavfsizlik va innovatsiya.',
    tag: 'set-technology',
  },
  {
    slug: 'health',
    title: 'Salomatlik',
    description: 'Tibbiyot, kasalliklar, ovqatlanish va sog‘lom turmush.',
    tag: 'set-health',
  },
  {
    slug: 'society',
    title: 'Jamiyat va madaniyat',
    description: 'Migratsiya, qadriyatlar, tenglik va madaniy meros.',
    tag: 'set-society',
  },
  {
    slug: 'government',
    title: 'Davlat va iqtisod',
    description: 'Siyosat, soliqlar, byudjet va iqtisodiy tushunchalar.',
    tag: 'set-government',
  },
  {
    slug: 'urban',
    title: 'Shahar va transport',
    description: 'Transport, uy-joy, infratuzilma va shahar hayoti.',
    tag: 'set-urban',
  },
  {
    slug: 'media',
    title: 'Media va muloqot',
    description: 'Yangiliklar, reklama, ijtimoiy tarmoqlar va axborot.',
    tag: 'set-media',
  },
  {
    slug: 'academic',
    title: 'Akademik fe’llar',
    description: 'Writing Task 1 va Task 2 uchun tahlil fe’llari va bog‘lovchilar.',
    tag: 'set-academic',
  },
];
