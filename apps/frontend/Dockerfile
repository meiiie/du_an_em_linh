# syntax=docker/dockerfile:1
# apps/frontend: build Angular bằng pnpm trên Node 24, phục vụ bằng nginx không root.
# Ngữ cảnh build là gốc repo (cần lockfile của workspace); Dockerfile.dockerignore cạnh file này lọc ngữ cảnh.
# /api/ chuyển tới services/core (CORE_HOSTPORT) để trình duyệt gọi cùng gốc, không cần CORS.

FROM node:24-alpine AS build
WORKDIR /src
RUN corepack enable
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY apps/frontend/package.json apps/frontend/package.json
RUN --mount=type=cache,id=s/61cad3de-f3a8-40c3-89db-57d386e0fbd6-pnpm-store,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile --filter frontend
COPY apps/frontend/ apps/frontend/
ARG GIT_SHA=development
ARG RENDER_GIT_COMMIT
RUN pnpm --filter frontend build
RUN node -e "const fs=require('fs');fs.writeFileSync('apps/frontend/dist/frontend/browser/version.json',JSON.stringify({revision:process.env.GIT_SHA==='development'?(process.env.RENDER_GIT_COMMIT||'development'):process.env.GIT_SHA}));"

FROM nginxinc/nginx-unprivileged:1.30-alpine
ENV CORE_HOSTPORT=core:8080
# Entrypoint của image ghi NGINX_LOCAL_RESOLVERS từ /etc/resolv.conf cho chỉ thị resolver trong template.
ENV NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1
# URL công khai của trang (canonical, Open Graph, sitemap); đặt lại khi triển khai.
ENV SITE_URL=http://localhost:4200
COPY apps/frontend/nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /src/apps/frontend/dist/frontend/browser/ /usr/share/nginx/html/
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/ > /dev/null || exit 1
