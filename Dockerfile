# Một container: Next.js + FastAPI/SymPy. Postgres ở ngoài (Render free / Neon).
# Cổng công khai duy nhất = PORT (Render) hoặc 3000.

FROM python:3.12-slim-bookworm

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl ca-certificates xz-utils \
  && rm -rf /var/lib/apt/lists/* \
  && curl -fsSL https://nodejs.org/dist/v22.19.0/node-v22.19.0-linux-x64.tar.xz \
    | tar -xJ -C /usr/local --strip-components=1 \
  && corepack enable

WORKDIR /app

COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY apps/web/package.json apps/web/package.json
RUN pnpm install --frozen-lockfile --filter web...

COPY services/math /app/services/math
RUN pip install --no-cache-dir --upgrade pip \
  && pip install --no-cache-dir /app/services/math

COPY apps/web /app/apps/web
COPY data /app/data
COPY scripts/start-free.sh /app/scripts/start-free.sh

WORKDIR /app/apps/web
RUN pnpm build

WORKDIR /app
ENV NODE_ENV=production
ENV MATH_SERVICE_URL=http://127.0.0.1:8000
ENV SEED_IF_EMPTY=1
EXPOSE 3000
CMD ["bash", "/app/scripts/start-free.sh"]
