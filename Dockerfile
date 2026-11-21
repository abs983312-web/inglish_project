# syntax=docker/dockerfile:1
FROM node:20-slim AS builder

WORKDIR /app

# Install openssl and dependencies needed for Prisma and native modules
RUN apt-get update -y && apt-get install -y openssl ca-certificates python3 make g++ && rm -rf /var/lib/apt/lists/*

COPY backend/package*.json ./
COPY backend/prisma ./prisma/

RUN npm ci

COPY backend/tsconfig.json ./
COPY backend/src ./src/

RUN npx prisma generate
RUN npm run build

# Production runner stage
FROM node:20-slim AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV HOST=0.0.0.0
ENV PORT=3000

RUN apt-get update -y && apt-get install -y openssl ca-certificates && rm -rf /var/lib/apt/lists/*

COPY backend/package*.json ./
COPY backend/prisma ./prisma/

RUN npm ci --omit=dev
RUN npx prisma generate

COPY --from=builder /app/dist ./dist

EXPOSE 3000

CMD ["sh", "-c", "npx prisma migrate deploy && node dist/server.js"]

