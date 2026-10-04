# syntax=docker/dockerfile:1
# services/core: build bằng Maven Wrapper trên JDK 25; chạy trên JRE 25 Alpine, người dùng không phải root,
# jar tách lớp (thư viện ít đổi nằm ở lớp dưới để cache). Ngữ cảnh build là gốc repo (cần data/ cho importer, T003b):
#   docker build -f services/core/Dockerfile .
# Dockerfile.dockerignore cạnh tệp này chỉ cho services/core/, data/supham/, data/v0/ vào ngữ cảnh.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY services/core/.mvn/ .mvn/
COPY services/core/mvnw services/core/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp dependency:go-offline
COPY services/core/src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp package -DskipTests \
 && java -Djarmode=tools -jar target/core-*.jar extract --layers --launcher --destination target/extracted

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S -g 1001 core && adduser -S -D -H -u 1001 -G core -s /sbin/nologin core
WORKDIR /app
# root sở hữu, người chạy chỉ đọc: ứng dụng không sửa được chính nó.
COPY --from=build /src/target/extracted/dependencies/ ./
COPY --from=build /src/target/extracted/spring-boot-loader/ ./
COPY --from=build /src/target/extracted/snapshot-dependencies/ ./
# Nội dung chủ đề cho importer (T012): dữ liệu lab Sư phạm và hằng nội dung của v0, của root nên người dùng 1001
# chỉ đọc. Đặt trước lớp mã ứng dụng (đổi thường xuyên hơn) để cache.
COPY data/supham/ /app/noi-dung/supham/
COPY data/v0/ /app/noi-dung/v0/
ENV APP_CONTENT_SOURCE=/app/noi-dung
COPY --from=build /src/target/extracted/application/ ./
USER 1001:1001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/liveness > /dev/null || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
