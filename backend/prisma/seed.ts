import {
  ExerciseType,
  LessonType,
  PrismaClient,
  Skill,
  SpeakingPart,
  WritingTaskType,
} from '@prisma/client';
import { VOCABULARY_SEED } from './data/vocabulary.js';

const prisma = new PrismaClient();

type SeedExercise = {
  type: ExerciseType;
  order: number;
  prompt: string;
  optionsJson?: unknown;
  answerJson: unknown;
  explanation?: string;
};

async function replaceLessons(
  unitId: string,
  lessons: Array<{
    slug: string;
    title: string;
    type: LessonType;
    order: number;
    estimatedMinutes: number;
    contentJson: unknown;
    exercises?: SeedExercise[];
  }>,
) {
  await prisma.lesson.deleteMany({ where: { unitId } });

  for (const lesson of lessons) {
    await prisma.lesson.create({
      data: {
        unitId,
        slug: lesson.slug,
        title: lesson.title,
        type: lesson.type,
        order: lesson.order,
        estimatedMinutes: lesson.estimatedMinutes,
        contentJson: lesson.contentJson as object,
        published: true,
        exercises: lesson.exercises
          ? {
              create: lesson.exercises.map((exercise) => ({
                type: exercise.type,
                order: exercise.order,
                prompt: exercise.prompt,
                optionsJson: exercise.optionsJson as object | undefined,
                answerJson: exercise.answerJson as object,
                explanation: exercise.explanation,
              })),
            }
          : undefined,
      },
    });
  }
}

const vocabulary = [
  {
    word: 'significant',
    pos: 'adjective',
    cefr: 'B2',
    definitionUz: 'muhim, sezilarli',
    definitionEn: 'large or important enough to have an effect',
    exampleEn: 'There has been a significant increase in online learning.',
    exampleUz: 'Onlayn ta’limda sezilarli o‘sish kuzatildi.',
    tags: ['academic', 'writing-task-2'],
  },
  {
    word: 'substantial',
    pos: 'adjective',
    cefr: 'B2',
    definitionUz: 'katta, jiddiy',
    definitionEn: 'large in amount, value, or importance',
    exampleEn: 'The government allocated a substantial budget to education.',
    exampleUz: 'Hukumat ta’limga katta mablag‘ ajratdi.',
    tags: ['academic', 'writing-task-2'],
  },
  {
    word: 'consequently',
    pos: 'adverb',
    cefr: 'B2',
    definitionUz: 'natijada',
    definitionEn: 'as a result',
    exampleEn: 'Many people move to cities; consequently, housing prices rise.',
    exampleUz: 'Ko‘pchilik shaharlarga ko‘chadi; natijada uy narxlari oshadi.',
    tags: ['linker', 'writing-task-2'],
  },
  {
    word: 'moreover',
    pos: 'adverb',
    cefr: 'B2',
    definitionUz: 'bundan tashqari',
    definitionEn: 'in addition to what has been said',
    exampleEn: 'Moreover, remote work can reduce commuting costs.',
    exampleUz: 'Bundan tashqari, masofadan ishlash yo‘l xarajatlarini kamaytiradi.',
    tags: ['linker', 'writing-task-2'],
  },
  {
    word: 'nevertheless',
    pos: 'adverb',
    cefr: 'B2',
    definitionUz: 'shunga qaramay',
    definitionEn: 'despite what has just been said',
    exampleEn: 'The plan is expensive; nevertheless, it may be worth it.',
    exampleUz: 'Reja qimmat; shunga qaramay, u arzirli bo‘lishi mumkin.',
    tags: ['linker', 'writing-task-2'],
  },
  {
    word: 'detrimental',
    pos: 'adjective',
    cefr: 'C1',
    definitionUz: 'zararli',
    definitionEn: 'causing harm or damage',
    exampleEn: 'Excessive screen time can be detrimental to children’s health.',
    exampleUz: 'Ortiqcha ekran vaqti bolalar salomatligiga zararli bo‘lishi mumkin.',
    tags: ['academic', 'writing-task-2'],
  },
  {
    word: 'beneficial',
    pos: 'adjective',
    cefr: 'B2',
    definitionUz: 'foydali',
    definitionEn: 'having a good or useful effect',
    exampleEn: 'Regular exercise is beneficial for both body and mind.',
    exampleUz: 'Muntazam mashq tana va aql uchun foydali.',
    tags: ['academic', 'writing-task-2'],
  },
  {
    word: 'mitigate',
    pos: 'verb',
    cefr: 'C1',
    definitionUz: 'yumshatmoq, kamaytirmoq',
    definitionEn: 'to make something less severe or harmful',
    exampleEn: 'Planting trees can help mitigate the effects of air pollution.',
    exampleUz: 'Daraxt ekish havo ifloslanishi oqibatlarini yumshatishga yordam beradi.',
    tags: ['academic', 'environment'],
  },
  {
    word: 'exacerbate',
    pos: 'verb',
    cefr: 'C1',
    definitionUz: 'yomonlashtirmoq',
    definitionEn: 'to make a problem worse',
    exampleEn: 'Cutting public transport funding would exacerbate traffic congestion.',
    exampleUz: 'Jamoat transportini moliyalashtirishni qisqartirish tirbandlikni yomonlashtiradi.',
    tags: ['academic', 'urban-life'],
  },
  {
    word: 'feasible',
    pos: 'adjective',
    cefr: 'C1',
    definitionUz: 'amalga oshirish mumkin',
    definitionEn: 'possible and practical to do',
    exampleEn: 'A four-day working week may be feasible for some companies.',
    exampleUz: 'To‘rt kunlik ish haftasi ba’zi kompaniyalar uchun amalga oshirilishi mumkin.',
    tags: ['academic', 'work'],
  },
  {
    word: 'inevitable',
    pos: 'adjective',
    cefr: 'B2',
    definitionUz: 'muqarrar',
    definitionEn: 'certain to happen and impossible to avoid',
    exampleEn: 'Some degree of automation is inevitable in modern industries.',
    exampleUz: 'Zamonaviy sanoatda ma’lum darajada avtomatlashtirish muqarrar.',
    tags: ['academic', 'technology'],
  },
  {
    word: 'compelling',
    pos: 'adjective',
    cefr: 'C1',
    definitionUz: 'ishonchli, jozibali',
    definitionEn: 'very convincing or interesting',
    exampleEn: 'The report presents compelling evidence for stricter regulations.',
    exampleUz: 'Hisobot qat’iyroq qoidalar uchun ishonchli dalillar keltiradi.',
    tags: ['academic', 'writing-task-2'],
  },
  {
    word: 'ambiguous',
    pos: 'adjective',
    cefr: 'C1',
    definitionUz: 'noaniq',
    definitionEn: 'having more than one possible meaning',
    exampleEn: 'The question is ambiguous, so candidates may interpret it differently.',
    exampleUz: 'Savol noaniq, shuning uchun nomzodlar uni turlicha talqin qilishi mumkin.',
    tags: ['academic', 'exam-skills'],
  },
  {
    word: 'comprehensive',
    pos: 'adjective',
    cefr: 'B2',
    definitionUz: 'har tomonlama',
    definitionEn: 'including all or nearly all aspects of something',
    exampleEn: 'The course offers a comprehensive introduction to academic writing.',
    exampleUz: 'Kurs akademik yozuvga har tomonlama kirishni taklif qiladi.',
    tags: ['academic', 'education'],
  },
  {
    word: 'deteriorate',
    pos: 'verb',
    cefr: 'C1',
    definitionUz: 'yomonlashmoq',
    definitionEn: 'to become worse',
    exampleEn: 'Air quality may deteriorate if industrial emissions are not controlled.',
    exampleUz: 'Agar sanoat chiqindilari nazorat qilinmasa, havo sifati yomonlashishi mumkin.',
    tags: ['academic', 'environment'],
  },
  {
    word: 'enhance',
    pos: 'verb',
    cefr: 'B2',
    definitionUz: 'yaxshilamoq',
    definitionEn: 'to improve the quality or value of something',
    exampleEn: 'Feedback from teachers can enhance students’ writing skills.',
    exampleUz: 'O‘qituvchilarning fikr-mulohazalari o‘quvchilarning yozuv ko‘nikmalarini yaxshilaydi.',
    tags: ['academic', 'education'],
  },
  {
    word: 'allocate',
    pos: 'verb',
    cefr: 'C1',
    definitionUz: 'ajratmoq',
    definitionEn: 'to give money, time, or resources for a particular purpose',
    exampleEn: 'Governments should allocate more funds to public healthcare.',
    exampleUz: 'Hukumatlar davlat sog‘liqni saqlashga ko‘proq mablag‘ ajratishi kerak.',
    tags: ['academic', 'government'],
  },
  {
    word: 'sustainable',
    pos: 'adjective',
    cefr: 'B2',
    definitionUz: 'barqaror',
    definitionEn: 'able to continue over time without damaging the environment',
    exampleEn: 'Cities need sustainable transport systems to reduce pollution.',
    exampleUz: 'Shaharlar ifloslanishni kamaytirish uchun barqaror transport tizimlariga muhtoj.',
    tags: ['academic', 'environment'],
  },
  {
    word: 'advocate',
    pos: 'verb',
    cefr: 'C1',
    definitionUz: 'targ‘ib qilmoq, yoqlamoq',
    definitionEn: 'to publicly support or recommend a policy or idea',
    exampleEn: 'Many experts advocate a shorter working week.',
    exampleUz: 'Ko‘plab mutaxassislar qisqaroq ish haftasini yoqlaydi.',
    tags: ['academic', 'work'],
  },
  {
    word: 'undermine',
    pos: 'verb',
    cefr: 'C1',
    definitionUz: 'zaiflashtirmoq',
    definitionEn: 'to gradually make something less effective or confident',
    exampleEn: 'Constant criticism can undermine a child’s confidence.',
    exampleUz: 'Doimiy tanqid bolaning ishonchini zaiflashtirishi mumkin.',
    tags: ['academic', 'education'],
  },
];

async function main() {
  const writingUnit = await prisma.unit.upsert({
    where: { slug: 'b2-writing-opinion-essays' },
    update: {
      title: 'Writing Task 2 — Opinion Essays',
      description: 'Learn to plan, structure, and write a clear opinion essay for IELTS Task 2.',
      skill: Skill.WRITING,
      cefrLevel: 'B2',
      order: 1,
      published: true,
    },
    create: {
      slug: 'b2-writing-opinion-essays',
      title: 'Writing Task 2 — Opinion Essays',
      description: 'Learn to plan, structure, and write a clear opinion essay for IELTS Task 2.',
      skill: Skill.WRITING,
      cefrLevel: 'B2',
      order: 1,
      published: true,
    },
  });

  await replaceLessons(writingUnit.id, [
    {
      slug: 'b2-writing-opinion-1',
      title: 'Understanding the Task',
      type: LessonType.CONTENT,
      order: 1,
      estimatedMinutes: 8,
      contentJson: {
        intro:
          'An opinion essay asks you to discuss a statement and give your own view. Before you write, identify the topic, the task words, and the position you will take.',
        sections: [
          {
            heading: 'Step 1 — Find the topic',
            body: 'Underline the main topic and the specific issue. For example, in a question about remote work, the topic is employment and the issue is whether working from home is better.',
          },
          {
            heading: 'Step 2 — Find the task words',
            body: 'Task words tell you what to do: discuss both views, give your opinion, agree or disagree, or explain why. Missing a task word lowers Task Response.',
          },
          {
            heading: 'Step 3 — Take a clear position',
            body: 'Your opinion should appear in the introduction and be developed in the body paragraphs. Do not sit on the fence unless the question asks you to.',
          },
        ],
        keyTakeaway:
          'A strong essay answers every part of the question and keeps one clear position from start to finish.',
      },
      exercises: [
        {
          type: ExerciseType.MULTIPLE_CHOICE,
          order: 1,
          prompt:
            'Question: “Some people believe that working from home is better for employees, while others think it is better to work in an office. Discuss both views and give your own opinion.” Which task words must you address?',
          optionsJson: [
            'Only give your opinion.',
            'Discuss both views and give your opinion.',
            'Describe your own workplace.',
            'Compare salaries in both settings.',
          ],
          answerJson: { correctIndex: 1 },
          explanation:
            'The question has two instructions: discuss both views and give your own opinion. Both must be visible in the essay.',
        },
        {
          type: ExerciseType.GAP_FILL,
          order: 2,
          prompt:
            'Complete the sentence with a suitable linker: “Remote work saves commuting time. ______, it can make teamwork more difficult.”',
          answerJson: { accepted: ['However', 'Nevertheless', 'On the other hand'] },
          explanation:
            'The second sentence contrasts with the first, so a contrast linker is needed.',
        },
        {
          type: ExerciseType.SHORT_ANSWER,
          order: 3,
          prompt:
            'Write a one-sentence thesis statement for the remote-work question. State your position clearly.',
          answerJson: {
            sample:
              'Although remote work offers flexibility, I believe that a balanced hybrid model is more beneficial for most employees.',
          },
          explanation:
            'A thesis statement should state your position and, where possible, signal the main reason or qualification.',
        },
      ],
    },
    {
      slug: 'b2-writing-opinion-2',
      title: 'Building a Clear Structure',
      type: LessonType.CONTENT,
      order: 2,
      estimatedMinutes: 10,
      contentJson: {
        intro:
          'A predictable four-paragraph structure helps the examiner follow your argument and keeps you within the time limit.',
        sections: [
          {
            heading: 'Introduction',
            body: 'Paraphrase the question, briefly introduce both views, and state your thesis. Two or three sentences are enough.',
          },
          {
            heading: 'Body paragraph 1',
            body: 'Explain the first view with a clear topic sentence, one or two supporting ideas, and a short example.',
          },
          {
            heading: 'Body paragraph 2',
            body: 'Explain the second view or your main supporting argument. Use linkers to show the relationship between ideas.',
          },
          {
            heading: 'Conclusion',
            body: 'Restate your position and summarise the main reasons. Do not introduce a new argument.',
          },
        ],
        keyTakeaway:
          'One paragraph = one main idea. Topic sentence first, explanation second, example third.',
      },
      exercises: [
        {
          type: ExerciseType.MULTIPLE_CHOICE,
          order: 1,
          prompt: 'Which sentence is the best topic sentence for a body paragraph?',
          optionsJson: [
            'For example, my brother works from home every Friday.',
            'The first advantage of remote work is greater flexibility.',
            'I will now discuss the next point.',
            'Working from home is a topic that many people discuss.',
          ],
          answerJson: { correctIndex: 1 },
          explanation:
            'A topic sentence states the paragraph’s main idea directly. The other options are an example, a filler, or a vague statement.',
        },
        {
          type: ExerciseType.GAP_FILL,
          order: 2,
          prompt:
            'Complete the sentence: “______ the initial costs, solar energy becomes cheaper over time.”',
          answerJson: { accepted: ['Despite', 'In spite of'] },
          explanation:
            '“Despite” and “in spite of” are followed by a noun phrase and introduce a contrast.',
        },
      ],
    },
    {
      slug: 'b2-writing-opinion-3',
      title: 'Practice: Opinion Essay',
      type: LessonType.PRACTICE,
      order: 3,
      estimatedMinutes: 25,
      contentJson: {
        intro:
          'Use the planning steps from this unit to write a full essay. Aim for 250–280 words and leave two minutes to check grammar and spelling.',
        checklist: [
          'Did you answer every part of the question?',
          'Is your opinion clear in the introduction and conclusion?',
          'Does each body paragraph have one main idea?',
          'Did you use a range of linking words?',
          'Did you check articles, plurals, and verb forms?',
        ],
      },
      exercises: [
        {
          type: ExerciseType.SHORT_ANSWER,
          order: 1,
          prompt:
            'Write a 4-sentence plan for this question: “Some people think university education should be free for everyone. To what extent do you agree or disagree?”',
          answerJson: {
            sample:
              'Introduction: free university education can widen access, but it is expensive. Position: partially agree, with government funding for low-income students. Body 1: social benefits of free education. Body 2: financial sustainability and targeted support. Conclusion: targeted free education is better than universal free education.',
          },
          explanation:
            'A plan should include your position, your two main ideas, and the order of your paragraphs.',
        },
      ],
    },
  ]);

  const speakingUnit = await prisma.unit.upsert({
    where: { slug: 'b2-speaking-part2' },
    update: {
      title: 'Speaking Part 2 — Cue Cards',
      description: 'Speak for up to two minutes about a person, place, object, or event.',
      skill: Skill.SPEAKING,
      cefrLevel: 'B2',
      order: 1,
      published: true,
    },
    create: {
      slug: 'b2-speaking-part2',
      title: 'Speaking Part 2 — Cue Cards',
      description: 'Speak for up to two minutes about a person, place, object, or event.',
      skill: Skill.SPEAKING,
      cefrLevel: 'B2',
      order: 1,
      published: true,
    },
  });

  await replaceLessons(speakingUnit.id, [
    {
      slug: 'b2-speaking-part2-1',
      title: 'Cue Card Strategy',
      type: LessonType.CONTENT,
      order: 1,
      estimatedMinutes: 8,
      contentJson: {
        intro:
          'In Part 2 you receive a cue card, one minute to prepare, and up to two minutes to speak. The goal is fluent, extended speech — not a perfect answer.',
        sections: [
          {
            heading: 'Use the preparation minute',
            body: 'Write four or five keywords, not full sentences. Cover each bullet point and decide on one detail you can extend.',
          },
          {
            heading: 'Structure your answer',
            body: 'Start with a direct answer, then add background, details, and a short personal reaction. Use time markers such as “when I was younger” or “these days”.',
          },
          {
            heading: 'Keep going',
            body: 'If you run out of ideas, add an example, contrast the past and present, or explain why the person or place matters to you.',
          },
        ],
        keyTakeaway:
          'A calm, organised two-minute answer beats a memorised speech. Practise extending each bullet point with one extra detail.',
      },
      exercises: [
        {
          type: ExerciseType.MULTIPLE_CHOICE,
          order: 1,
          prompt: 'What is the best use of the one-minute preparation time?',
          optionsJson: [
            'Write the full answer word for word.',
            'Write a few keywords and plan one extra detail.',
            'Read the cue card silently without planning.',
            'Memorise a template from the internet.',
          ],
          answerJson: { correctIndex: 1 },
          explanation:
            'Keywords help you remember the structure while leaving room for natural speech. Memorised answers often sound unnatural.',
        },
      ],
    },
    {
      slug: 'b2-speaking-part2-2',
      title: 'Practice: Describe a Place',
      type: LessonType.PRACTICE,
      order: 2,
      estimatedMinutes: 15,
      contentJson: {
        intro:
          'Record yourself answering the cue card below. Listen once for fluency, then again for grammar and vocabulary.',
        cueCard: {
          topic: 'Describe a place you like to visit',
          bullets: [
            'where it is',
            'how often you go there',
            'what you do there',
            'and explain why you like it',
          ],
        },
      },
      exercises: [
        {
          type: ExerciseType.SHORT_ANSWER,
          order: 1,
          prompt:
            'Write five keywords you would use to answer the cue card. Then record a 90-second answer.',
          answerJson: {
            sample:
              'park near my home / every weekend / walk and read / quiet / good place to relax',
          },
          explanation:
            'Keywords should cover each bullet point and include at least one detail you can extend.',
        },
      ],
    },
  ]);

  const vocabularyUnit = await prisma.unit.upsert({
    where: { slug: 'b2-vocabulary-academic-set-1' },
    update: {
      title: 'IELTS Vocabulary Library',
      description: '520 core words across 10 IELTS topics, reviewed with spaced repetition.',
      skill: Skill.VOCABULARY,
      cefrLevel: 'B2',
      order: 1,
      published: true,
    },
    create: {
      slug: 'b2-vocabulary-academic-set-1',
      title: 'IELTS Vocabulary Library',
      description: '520 core words across 10 IELTS topics, reviewed with spaced repetition.',
      skill: Skill.VOCABULARY,
      cefrLevel: 'B2',
      order: 1,
      published: true,
    },
  });

  await replaceLessons(vocabularyUnit.id, [
    {
      slug: 'b2-vocabulary-academic-set-1-lesson',
      title: 'How to Review Vocabulary',
      type: LessonType.CONTENT,
      order: 1,
      estimatedMinutes: 6,
      contentJson: {
        intro:
          'Use spaced repetition: review a word just before you are likely to forget it. Rate your recall honestly, and always say the example sentence aloud.',
        steps: [
          'Look at the English word and try to recall the Uzbek meaning.',
          'Turn the card over and check the definition and example.',
          'Say the example sentence aloud with natural stress.',
          'Rate the review from 1 (forgot) to 4 (easy).',
          'Return when the app schedules the next review.',
        ],
      },
    },
  ]);

  for (const item of vocabulary) {
    await prisma.vocabularyItem.upsert({
      where: { word: item.word },
      update: item,
      create: item,
    });
  }

  for (const item of VOCABULARY_SEED) {
    await prisma.vocabularyItem.upsert({
      where: { word: item.word },
      update: item,
      create: item,
    });
  }

  await prisma.writingTask.upsert({
    where: { slug: 'task2-opinion-remote-work' },
    update: {
      type: WritingTaskType.TASK_2,
      title: 'Remote work vs office work',
      prompt:
        'Some people believe that working from home is better for employees, while others think it is better to work in an office. Discuss both views and give your own opinion.',
      minWords: 250,
      timeLimitMinutes: 40,
      published: true,
    },
    create: {
      slug: 'task2-opinion-remote-work',
      type: WritingTaskType.TASK_2,
      title: 'Remote work vs office work',
      prompt:
        'Some people believe that working from home is better for employees, while others think it is better to work in an office. Discuss both views and give your own opinion.',
      minWords: 250,
      timeLimitMinutes: 40,
      published: true,
    },
  });

  await prisma.writingTask.upsert({
    where: { slug: 'task1-internet-access-line-graph' },
    update: {
      type: WritingTaskType.TASK_1,
      title: 'Internet access line graph',
      prompt:
        'The line graph shows the percentage of households with internet access in three countries between 2000 and 2020. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.',
      minWords: 150,
      timeLimitMinutes: 20,
      published: true,
      imageUrl: null,
    },
    create: {
      slug: 'task1-internet-access-line-graph',
      type: WritingTaskType.TASK_1,
      title: 'Internet access line graph',
      prompt:
        'The line graph shows the percentage of households with internet access in three countries between 2000 and 2020. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.',
      minWords: 150,
      timeLimitMinutes: 20,
      published: true,
      imageUrl: null,
    },
  });

  const speakingPrompts = [
    {
      slug: 'part1-hometown',
      part: SpeakingPart.PART_1,
      topic: 'Hometown',
      questionsJson: {
        questions: [
          'Where is your hometown?',
          'What do you like most about it?',
          'Has it changed much in recent years?',
          'Would you like to live there in the future?',
        ],
      },
      prepTimeSeconds: 0,
      speakTimeSeconds: 60,
    },
    {
      slug: 'part2-describe-a-person',
      part: SpeakingPart.PART_2,
      topic: 'Describe a person who has influenced you',
      questionsJson: {
        cueCard: {
          topic: 'Describe a person who has influenced you',
          bullets: [
            'who this person is',
            'how you know them',
            'what they have done',
            'and explain how they influenced you',
          ],
        },
      },
      prepTimeSeconds: 60,
      speakTimeSeconds: 120,
    },
    {
      slug: 'part3-education',
      part: SpeakingPart.PART_3,
      topic: 'Education',
      questionsJson: {
        questions: [
          'What changes would you like to see in education?',
          'Should university education be free? Why or why not?',
          'How might technology change the way people learn?',
          'What skills should schools teach beyond academic subjects?',
        ],
      },
      prepTimeSeconds: 0,
      speakTimeSeconds: 180,
    },
  ];

  for (const prompt of speakingPrompts) {
    await prisma.speakingPrompt.upsert({
      where: { slug: prompt.slug },
      update: { ...prompt, published: true },
      create: { ...prompt, published: true },
    });
  }

  // Give every existing user a FREE subscription row (idempotent).
  const users = await prisma.user.findMany({ select: { id: true } });
  for (const user of users) {
    await prisma.subscription.upsert({
      where: { userId: user.id },
      update: {},
      create: { userId: user.id, plan: 'FREE', status: 'ACTIVE' },
    });
  }

  console.log(
    `Seed complete: units=${await prisma.unit.count()}, lessons=${await prisma.lesson.count()}, vocabulary=${await prisma.vocabularyItem.count()}, writingTasks=${await prisma.writingTask.count()}, speakingPrompts=${await prisma.speakingPrompt.count()}`,
  );
}

main()
  .catch((error) => {
    console.error(error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
