process.env.NODE_ENV = 'test';
process.env.DATABASE_URL ??= 'postgresql://test:test@localhost:5432/inglish_test?schema=public';
process.env.JWT_SECRET ??= 'test-secret-test-secret-test-secret-1234';
process.env.CORS_ORIGIN ??= '*';
