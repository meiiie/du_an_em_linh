package vn.hoctapcanman.core.tutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.tutor.infrastructure.ai.GlmHintModelAdapter;

class GlmHintModelAdapterTest {
    static final String BASE="https://api.z.ai/api/coding/paas/v4";
    static final String SUCCESS="""
        : ping

        data: {"choices":[{"delta":{"reasoning_content":"bí mật"}}]}

        data: {"choices":[{"delta":{"content":"Em đã "}}]}

        data: {"choices":[{"delta":{"content":"thử gì?"}}]}

        data: {"choices":[{"delta":{},"finish_reason":"stop"}],"usage":{"prompt_tokens":25,"completion_tokens":7}}

        data: [DONE]

        """;
    MockRestServiceServer server;
    GlmHintModelAdapter adapter;
    final HintPrompt prompt=new HintPrompt("Chỉ dẫn","Câu hỏi");
    @BeforeEach void setup() {
        var builder=RestClient.builder().baseUrl(BASE);
        server=MockRestServiceServer.bindTo(builder).build();
        adapter=new GlmHintModelAdapter(builder.build(),"khóa-thử-không-có-quyền","glm-4.7",Duration.ofSeconds(2),350);
    }
    @Test void streamsContentWithThinkingDisabledAndRealUsage() {
        server.expect(requestTo(BASE+"/chat/completions")).andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""
                {"model":"glm-4.7","max_tokens":350,"stream":true,"thinking":{"type":"disabled"},
                "messages":[{"role":"system","content":"Chỉ dẫn"},{"role":"user","content":"Câu hỏi"}]}
                """,true)).andRespond(withSuccess(SUCCESS,MediaType.TEXT_EVENT_STREAM));
        var chunks=new ArrayList<String>();var usage=adapter.stream(prompt,chunks::add);
        assertEquals(List.of("Em đã ","thử gì?"),chunks);assertEquals(25,usage.inputTokens());assertEquals(7,usage.outputTokens());assertFalse(usage.estimated());server.verify();
    }
    @Test void supportsCrLfAndEstimatesAbsentUsage() {
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(withSuccess(SUCCESS.replace("\n","\r\n").replace(",\"usage\":{\"prompt_tokens\":25,\"completion_tokens\":7}",""),MediaType.TEXT_EVENT_STREAM));
        assertTrue(adapter.stream(prompt,ignored -> {}).estimated());server.verify();
    }
    @Test void partialTransportEofOrMalformedDataCannotComplete() {
        for(String response:List.of("data: {\n\n",SUCCESS.replace("data: [DONE]",""),SUCCESS.replace("\"stop\"","\"length\""),"data: [DONE]\n\n")) {
            setup();server.expect(requestTo(BASE+"/chat/completions")).andRespond(withSuccess(response,MediaType.TEXT_EVENT_STREAM));
            assertThrows(RuntimeException.class,() -> adapter.stream(prompt,ignored -> {}));server.verify();
        }
    }
    @Test void refusalOfConsumerStopsProviderWithoutRetry() {
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(withSuccess(SUCCESS,MediaType.TEXT_EVENT_STREAM));
        assertThrows(vn.hoctapcanman.core.tutor.application.service.OfflineTutorService.Rejected.class,
            () -> adapter.stream(prompt,ignored -> {throw new vn.hoctapcanman.core.tutor.application.service.OfflineTutorService.Rejected("GATE_REJECTED");}));server.verify();
    }
    @Test void providerErrorsDoNotExposeResponseOrFallback() {
        for(HttpStatus status:List.of(HttpStatus.UNAUTHORIZED,HttpStatus.TOO_MANY_REQUESTS,HttpStatus.SERVICE_UNAVAILABLE)) {
            setup();server.expect(requestTo(BASE+"/chat/completions")).andRespond(withStatus(status).body("dữ liệu nhà không được lộ"));
            var ex=assertThrows(IllegalStateException.class,() -> adapter.stream(prompt,ignored -> {}));assertEquals("MODEL_UNAVAILABLE",ex.getMessage());server.verify();
        }
    }
    @Test void enforcesWholeTransportDeadline() {
        var builder=RestClient.builder().baseUrl(BASE);server=MockRestServiceServer.bindTo(builder).build();
        adapter=new GlmHintModelAdapter(builder.build(),"khóa-thử","glm-4.7",Duration.ofMillis(50),350);
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(request -> {
            try{Thread.sleep(300);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
            return withSuccess(SUCCESS,MediaType.TEXT_EVENT_STREAM).createResponse(request);
        });
        assertTimeout(Duration.ofMillis(200),() -> assertThrows(IllegalStateException.class,() -> adapter.stream(prompt,ignored -> {})));server.verify();
    }
    @Test void transportTimeoutDoesNotInterruptConsumerDoingDatabaseWork() throws Exception {
        var builder=RestClient.builder().baseUrl(BASE);server=MockRestServiceServer.bindTo(builder).build();
        adapter=new GlmHintModelAdapter(builder.build(),"khóa-thử","glm-4.7",Duration.ofMillis(100),350);
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(withSuccess(SUCCESS,MediaType.TEXT_EVENT_STREAM));
        var release=new java.util.concurrent.CountDownLatch(1);var finished=new java.util.concurrent.CountDownLatch(1);
        var interrupted=new java.util.concurrent.atomic.AtomicBoolean();
        try {
            assertThrows(IllegalStateException.class,() -> adapter.stream(prompt,ignored -> {
                try { release.await(3,java.util.concurrent.TimeUnit.SECONDS); }
                catch(InterruptedException ex) { interrupted.set(true); }
                finally { finished.countDown(); }
            }));
        } finally { release.countDown(); }
        assertTrue(finished.await(3,java.util.concurrent.TimeUnit.SECONDS));
        assertFalse(interrupted.get(),"Deadline không được ngắt callback đang ghi JDBC");server.verify();
    }
    @Test void cooperativeCancellationClosesBlockedHttpStreamWithoutInterruptingReader() throws Exception {
        var reading=new java.util.concurrent.CountDownLatch(1);var closed=new java.util.concurrent.CountDownLatch(1);
        var interrupted=new java.util.concurrent.atomic.AtomicBoolean();
        var input=new java.io.InputStream() {
            @Override public int read() {
                reading.countDown();
                try { closed.await(3,java.util.concurrent.TimeUnit.SECONDS); }
                catch(InterruptedException ex) { interrupted.set(true); }
                return -1;
            }
            @Override public void close() { closed.countDown(); }
        };
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(request -> {
            var response=new org.springframework.mock.http.client.MockClientHttpResponse(input,HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);return response;
        });
        var cancellation=new vn.hoctapcanman.core.tutor.application.port.HintModel.Cancellation();
        var result=new java.util.concurrent.FutureTask<>(() -> adapter.stream(prompt,ignored -> fail("Không được phát delta"),cancellation));
        Thread.startVirtualThread(result);
        assertTrue(reading.await(3,java.util.concurrent.TimeUnit.SECONDS));
        cancellation.cancel();
        assertTrue(closed.await(1,java.util.concurrent.TimeUnit.SECONDS));
        var failure=assertThrows(java.util.concurrent.ExecutionException.class,() -> result.get(1,java.util.concurrent.TimeUnit.SECONDS));
        assertInstanceOf(java.util.concurrent.CancellationException.class,failure.getCause());
        assertFalse(interrupted.get());server.verify();
    }
    @Test void rejectsNonSseSuccessAndToolCalls() {
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));assertTrue(adapter.draft(prompt).isEmpty());server.verify();
        setup();server.expect(requestTo(BASE+"/chat/completions")).andRespond(withSuccess("data: {\"choices\":[{\"delta\":{\"tool_calls\":[]}}]}\n\n",MediaType.TEXT_EVENT_STREAM));
        assertTrue(adapter.draft(prompt).isEmpty());server.verify();
    }
}
