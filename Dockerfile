# Một container: Next.js + FastAPI/SymPy. Postgres ở ngoài (Render free / Neon).
# Cổng công khai duy nhất = PORT (Render) hoặc 3000.

FROM python:3.12-slim-bookworm

# pnpm do corepack tải về nằm ở thư mục chung, đọc được bởi user không root lúc chạy
ENV COREPACK_HOME=/opt/corepack

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

# F-01: chạy bằng user không root. Chỉ thư mục dữ liệu tải lên và cache Next ghi được.
RUN useradd --system --uid 10001 --home-dir /app --shell /usr/sbin/nologin app \
  && mkdir -p /app/data/uploads /app/apps/web/.next/cache \
  && chown -R app:app /app/data /app/apps/web/.next \
  && chmod -R a+rX /opt/corepack

WORKDIR /app
ENV NODE_ENV=production
ENV MATH_SERVICE_URL=http://127.0.0.1:8000
ENV SEED_IF_EMPTY=1
EXPOSE 3000
USER app
CMD ["bash", "/app/scripts/start-free.sh"]
