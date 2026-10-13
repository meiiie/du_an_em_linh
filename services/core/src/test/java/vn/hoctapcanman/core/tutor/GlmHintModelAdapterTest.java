package vn.hoctapcanman.core.tutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import java.net.SocketTimeoutException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.tutor.infrastructure.ai.GlmHintModelAdapter;

class GlmHintModelAdapterTest {
    static final String BASE = "https://api.z.ai/api/coding/paas/v4";
    static final String SUCCESS = "{\"choices\":[{\"message\":{\"content\":\"Em thử xem dòng [1] nhé.\"}}]}";
    MockRestServiceServer server;
    GlmHintModelAdapter adapter;
    final HintPrompt prompt = new HintPrompt("Chỉ dẫn", "Câu hỏi");
    @BeforeEach void setup() {
        var builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new GlmHintModelAdapter(builder.build(), "khóa-thử-không-có-quyền", "glm-5.3", Duration.ofSeconds(2), 350);
    }
    @Test void sendsCompatibleRequestWithThinkingDisabled() {
        server.expect(requestTo(BASE + "/chat/completions"))
            .andExpect(method(HttpMethod.POST)).andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer khóa-thử-không-có-quyền"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                {"model":"glm-5.3","max_tokens":350,"stream":false,"thinking":{"type":"disabled"},
                "messages":[{"role":"system","content":"Chỉ dẫn"},{"role":"user","content":"Câu hỏi"}]}
                """, true)).andRespond(withSuccess(SUCCESS, MediaType.APPLICATION_JSON));
        assertEquals("Em thử xem dòng [1] nhé.", adapter.draft(prompt).orElseThrow());
        server.verify();
    }
    @Test void retriesOnlyOnceForTransientStatuses() {
        for (int status : new int[] {429, 500, 502, 503, 504}) {
            setup();
            server.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withStatus(HttpStatusCode.valueOf(status)).header("Retry-After", "0"));
            server.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withSuccess(SUCCESS, MediaType.APPLICATION_JSON));
            assertTrue(adapter.draft(prompt).isPresent(), "Mã " + status);
            server.verify();
        }
    }
    @Test void respectsRetryAfterBeforeNextAttempt() {
        server.expect(requestTo(BASE + "/chat/completions")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "1"));
        server.expect(requestTo(BASE + "/chat/completions")).andRespond(withSuccess(SUCCESS, MediaType.APPLICATION_JSON));
        long before = System.nanoTime();
        assertTrue(adapter.draft(prompt).isPresent());
        assertTrue(Duration.ofNanos(System.nanoTime() - before).compareTo(Duration.ofMillis(900)) >= 0);
        server.verify();
    }
    @Test void stopsAfterSecondFailure() {
        server.expect(requestTo(BASE + "/chat/completions")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).header("Retry-After", "0"));
        server.expect(requestTo(BASE + "/chat/completions")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertTrue(adapter.draft(prompt).isEmpty());
        server.verify();
    }
    @Test void doesNotRetryAuthErrorsOrTimeout() {
        server.expect(requestTo(BASE + "/chat/completions")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertTrue(adapter.draft(prompt).isEmpty());
        server.verify();
        setup();
        server.expect(requestTo(BASE + "/chat/completions")).andRespond(withException(new SocketTimeoutException()));
        assertTrue(adapter.draft(prompt).isEmpty());
        server.verify();
    }
    @Test void retryAfterBeyondBudgetFallsBackWithoutRetry() {
        server.expect(requestTo(BASE + "/chat/completions")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "60"));
        assertTimeout(Duration.ofSeconds(1), () -> assertTrue(adapter.draft(prompt).isEmpty()));
        server.verify();
    }
    @Test void boundsEntireCallIncludingBlockedTransport() {
        var builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new GlmHintModelAdapter(builder.build(), "khóa-thử", "glm-5.3", Duration.ofMillis(50), 350);
        server.expect(requestTo(BASE + "/chat/completions")).andRespond(request -> {
            try { Thread.sleep(300); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            return withSuccess(SUCCESS, MediaType.APPLICATION_JSON).createResponse(request);
        });
        assertTimeout(Duration.ofMillis(200), () -> assertTrue(adapter.draft(prompt).isEmpty()));
        server.verify();
    }
    @Test void retryAfterSupportsHttpDate() {
        server.expect(requestTo(BASE + "/chat/completions")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", "Wed, 14 Oct 2099 00:00:00 GMT"));
        assertTimeout(Duration.ofSeconds(1), () -> assertTrue(adapter.draft(prompt).isEmpty()));
        server.verify();
    }
    @Test void emptyContentOrMalformedResponseIsNotAUsableDraft() {
        for (String body : new String[] {"{}", "{", "{\"choices\":[]}", "{\"choices\":[{\"message\":{\"content\":\"  \"}}]}",
            "{\"choices\":[{\"message\":{\"content\":42}}]}"}) {
            setup();
            server.expect(requestTo(BASE + "/chat/completions")).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            assertTrue(adapter.draft(prompt).isEmpty());
            server.verify();
        }
    }
}
