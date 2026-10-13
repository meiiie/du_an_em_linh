package vn.hoctapcanman.core.tutor.infrastructure.ai;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.tutor.application.port.HintModel;

/** Chat Completions của Z.ai Coding Plan; không ghi request, response hoặc lỗi nhà vào log. */
public final class GlmHintModelAdapter implements HintModel {
    private static final Set<Integer> RETRYABLE = Set.of(429, 500, 502, 503, 504);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final RestClient client;
    private final String key;
    private final String model;
    private final Duration timeout;
    private final int maxTokens;

    public GlmHintModelAdapter(RestClient client, String key, String model, Duration timeout, int maxTokens) {
        this.client = client;
        this.key = key;
        this.model = model;
        this.timeout = timeout;
        this.maxTokens = maxTokens;
    }

    @Override public String provider() { return "GLM"; }

    @Override public Optional<String> draft(HintPrompt prompt) {
        // Một hạn chung cho cả hai lần gọi và thời gian chờ, kể cả khi HTTP chưa kết thúc sớm.
        FutureTask<Optional<String>> task = new FutureTask<>(() -> draftWithinBudget(prompt));
        Thread.startVirtualThread(task);
        try { return task.get(timeout.toNanos(), TimeUnit.NANOSECONDS); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        catch (TimeoutException | ExecutionException ignored) { /* Chỉ trả về thang đã kiểm, không lộ lỗi nhà. */ }
        finally { if (!task.isDone()) task.cancel(true); }
        return Optional.empty();
    }

    private Optional<String> draftWithinBudget(HintPrompt prompt) {
        long started = System.nanoTime();
        Map<String, Object> body = Map.of("model", model, "max_tokens", maxTokens, "stream", false,
            "thinking", Map.of("type", "disabled"), "messages", List.of(
                Map.of("role", "system", "content", prompt.system()), Map.of("role", "user", "content", prompt.user())));
        try {
            for (int attempt = 0; attempt < 2; attempt++) {
                if (Thread.currentThread().isInterrupted()) return Optional.empty();
                Reply reply = client.post().uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + key).contentType(MediaType.APPLICATION_JSON).body(body)
                    .exchange((request, response) -> new Reply(response.getStatusCode().value(),
                        response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER),
                        response.getStatusCode().is2xxSuccessful() ? response.bodyTo(String.class) : ""));
                if (reply.status() >= 200 && reply.status() < 300) return content(reply.body());
                if (attempt == 1 || !RETRYABLE.contains(reply.status())) return Optional.empty();
                Duration delay = retryDelay(reply.retryAfter());
                Duration remaining = timeout.minusNanos(System.nanoTime() - started);
                // Không chờ Retry-After quá ngân sách. Không gửi sớm hơn thời điểm nhà cho phép.
                if (remaining.isNegative() || delay.compareTo(remaining) >= 0) return Optional.empty();
                Thread.sleep(delay);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException ignored) {
            // Timeout, HTTP hoặc JSON lỗi: service chỉ dùng thang sau khi qua cổng.
        }
        return Optional.empty();
    }

    private static Optional<String> content(@Nullable String body) {
        if (body == null) return Optional.empty();
        var value = JSON.readTree(body).path("choices").path(0).path("message").path("content");
        if (!value.isString()) return Optional.empty();
        String text = value.asString().strip();
        return text.isEmpty() ? Optional.empty() : Optional.of(text);
    }

    private static Duration retryDelay(@Nullable String value) {
        if (value == null) return Duration.ofSeconds(1);
        try { return Duration.ofSeconds(Math.max(0, Long.parseLong(value.strip()))); }
        catch (NumberFormatException ignored) {
            try {
                Instant when = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                Duration delay = Duration.between(Instant.now(), when);
                return delay.isNegative() ? Duration.ZERO : delay;
            } catch (RuntimeException invalid) { return Duration.ofSeconds(1); }
        }
    }

    private record Reply(int status, @Nullable String retryAfter, @Nullable String body) {}
}
