package vn.hoctapcanman.core.tutor.infrastructure.ai;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Một pool dùng cho cả chat và HEAD không xác thực; không gọi endpoint sinh token. */
public final class GlmConnectionPool implements AutoCloseable {
    private final HttpClient http;
    private final ScheduledExecutorService timer;
    private final RestClient client;

    public GlmConnectionPool(String baseUrl,Duration timeout,boolean enabled) {
        http=HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build();
        var factory=new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(timeout);
        client=RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        timer=Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("glm-keepalive-",0).factory());
        if(enabled) {
            URI target=URI.create(baseUrl);
            // Không truyền khóa, không theo redirect, không ghi response/lỗi có thể chứa dữ liệu nhà.
            var request=HttpRequest.newBuilder(target).timeout(Duration.ofSeconds(5)).method("HEAD",HttpRequest.BodyPublishers.noBody()).build();
            timer.scheduleWithFixedDelay(() -> {
                try { http.send(request,HttpResponse.BodyHandlers.discarding()); }
                catch(InterruptedException ex) { Thread.currentThread().interrupt(); }
                catch(Exception ignored) { /* Warm-up không chặn startup hoặc lượt hỏi. */ }
            },0,4,TimeUnit.MINUTES);
        }
    }
    public RestClient client() { return client; }
    @Override public void close() { timer.shutdownNow();http.shutdownNow(); }
}
