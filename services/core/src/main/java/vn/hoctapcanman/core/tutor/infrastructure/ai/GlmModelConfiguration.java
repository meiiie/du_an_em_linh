package vn.hoctapcanman.core.tutor.infrastructure.ai;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.hoctapcanman.core.tutor.application.port.HintModel;

/** Cấu hình qua môi trường; không bật autoconfig Spring AI hoặc theo redirect có thể lộ khóa. */
@Configuration(proxyBeanMethods = false)
public class GlmModelConfiguration {
    public static final String BASE_URL = "https://api.z.ai/api/coding/paas/v4";

    @Bean(destroyMethod="close")
    GlmConnectionPool glmConnectionPool(@Value("${mathl.gia-su.api-key:}") String key,
        @Value("${mathl.gia-su.base-url:}") String baseUrl,
        @Value("${mathl.gia-su.timeout:45s}") Duration timeout) {
        String url=baseUrl.isBlank()?BASE_URL:baseUrl.strip().replaceAll("/+$", "");
        return new GlmConnectionPool(url,timeout,!key.isBlank());
    }

    @Bean
    HintModel hintModel(@Value("${mathl.gia-su.api-key:}") String key,
        GlmConnectionPool pool,
        @Value("${mathl.gia-su.model:}") String model,
        @Value("${mathl.gia-su.timeout:45s}") Duration timeout,
        @Value("${mathl.gia-su.first-token-timeout:20s}") Duration firstTokenTimeout,
        @Value("${mathl.gia-su.max-tokens:700}") int maxTokens,
        @Value("${mathl.gia-su.temperature:0.8}") double temperature,
        @Value("${mathl.gia-su.reasoning-effort:low}") String reasoningEffort,
        @Value("${mathl.gia-su.top-p:0.9}") double topP, org.springframework.core.env.Environment environment, io.micrometer.core.instrument.MeterRegistry meters) {
        HintModel offline=environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev", "test"))
            ? new OfflineHintModelAdapter() : new UnavailableHintModelAdapter();
        if (key.isBlank()) return new SelectedHintModelAdapter(new UnavailableHintModelAdapter(),offline);
        if (timeout.isZero() || timeout.isNegative() || firstTokenTimeout.isZero() || firstTokenTimeout.isNegative() || maxTokens < 1 || maxTokens > 1024) {
            throw new IllegalArgumentException("Thời gian chờ phải dương và giới hạn đầu ra từ 1 đến 1024 token.");
        }
        return new SelectedHintModelAdapter(new GlmHintModelAdapter(pool.client(),
            key.strip(), model.isBlank() ? "glm-5.3" : model.strip(), timeout, maxTokens, firstTokenTimeout,temperature,topP,reasoningEffort,meters),offline);
    }
}
