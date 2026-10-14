package vn.hoctapcanman.core.tutor.infrastructure.ai;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import vn.hoctapcanman.core.tutor.application.port.HintModel;

/** Cấu hình qua môi trường; không bật autoconfig Spring AI hoặc theo redirect có thể lộ khóa. */
@Configuration(proxyBeanMethods = false)
public class GlmModelConfiguration {
    public static final String BASE_URL = "https://api.z.ai/api/coding/paas/v4";

    @Bean
    HintModel hintModel(@Value("${mathl.gia-su.api-key:}") String key,
        @Value("${mathl.gia-su.base-url:}") String baseUrl,
        @Value("${mathl.gia-su.model:}") String model,
        @Value("${mathl.gia-su.timeout:45s}") Duration timeout,
        @Value("${mathl.gia-su.first-token-timeout:20s}") Duration firstTokenTimeout,
        @Value("${mathl.gia-su.max-tokens:700}") int maxTokens, org.springframework.core.env.Environment environment) {
        HintModel offline=environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev", "test"))
            ? new OfflineHintModelAdapter() : new UnavailableHintModelAdapter();
        if (key.isBlank()) return new SelectedHintModelAdapter(new UnavailableHintModelAdapter(),offline);
        if (timeout.isZero() || timeout.isNegative() || firstTokenTimeout.isZero() || firstTokenTimeout.isNegative() || maxTokens < 1 || maxTokens > 1024) {
            throw new IllegalArgumentException("Thời gian chờ phải dương và giới hạn đầu ra từ 1 đến 1024 token.");
        }
        String url = baseUrl.isBlank() ? BASE_URL : baseUrl.strip().replaceAll("/+$", "");
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(timeout)
            .followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(timeout);
        return new SelectedHintModelAdapter(new GlmHintModelAdapter(RestClient.builder().baseUrl(url).requestFactory(factory).build(),
            key.strip(), model.isBlank() ? "glm-5.3" : model.strip(), timeout, maxTokens, firstTokenTimeout),offline);
    }
}
