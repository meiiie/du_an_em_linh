package vn.hoctoanai.core.shared.infrastructure.math;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** {@code app.math.*}: địa chỉ {@code services/math} ({@code APP_MATH_BASE_URL}) và hết giờ kết nối. */
@ConfigurationProperties("app.math")
public record MathServiceProperties(
        @DefaultValue("http://localhost:8000") URI baseUrl,
        @DefaultValue("PT2S") Duration connectTimeout) {}
