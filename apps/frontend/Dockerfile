# syntax=docker/dockerfile:1
# apps/frontend: build Angular bằng pnpm trên Node 24, phục vụ bằng nginx không root.
# Ngữ cảnh build là gốc repo (cần lockfile của workspace); Dockerfile.dockerignore cạnh file này lọc ngữ cảnh.
# /api/ chuyển tới services/core (CORE_URL) để trình duyệt gọi cùng gốc, không cần CORS.

FROM node:24-alpine AS build
WORKDIR /src
RUN corepack enable
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY apps/frontend/package.json apps/frontend/package.json
RUN --mount=type=cache,id=pnpm-store,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile --filter frontend
COPY apps/frontend/ apps/frontend/
RUN pnpm --filter frontend build

FROM nginxinc/nginx-unprivileged:1.30-alpine
ENV CORE_URL=http://core:8080
COPY apps/frontend/nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /src/apps/frontend/dist/frontend/browser/ /usr/share/nginx/html/
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/ > /dev/null || exit 1
