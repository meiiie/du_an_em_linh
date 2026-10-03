# syntax=docker/dockerfile:1
# v0 web (Next.js) cho docker-compose.yml: cùng một image chạy job `migrate` (migration + seed khi CSDL trống) và `web`.
# Chạy bằng user không root `node`; khóa nhà AI nhận qua tệp bí mật (docker-entrypoint.sh), không qua image.
FROM node:22-bookworm-slim

# pnpm do corepack tải lúc build nằm ở thư mục chung, user không root đọc được lúc chạy.
ENV COREPACK_HOME=/opt/corepack
RUN corepack enable

WORKDIR /app
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY apps/web/package.json apps/web/package.json
RUN pnpm install --frozen-lockfile --filter web...

COPY apps/web apps/web
COPY data data

WORKDIR /app/apps/web
RUN pnpm build \
  && mkdir -p /app/data/uploads .next/cache \
  && chown -R node:node /app/data .next \
  && chmod -R a+rX /opt/corepack

COPY --chmod=755 apps/web/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

ENV NODE_ENV=production
USER node
EXPOSE 3000
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["pnpm", "start"]
