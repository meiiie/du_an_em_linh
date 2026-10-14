package vn.hoctapcanman.core.tutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import java.time.Duration;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
                {"model":"glm-4.7","max_tokens":350,"stream":true,"temperature":0.8,"top_p":0.9,"thinking":{"type":"disabled"},
                "messages":[{"role":"system","content":"Chỉ dẫn"},{"role":"user","content":"Câu hỏi"}]}
                """,true)).andRespond(withSuccess(SUCCESS,MediaType.TEXT_EVENT_STREAM));
        var chunks=new ArrayList<String>();var usage=adapter.stream(prompt,chunks::add);
        assertEquals(List.of("Em đã ","thử gì?"),chunks);assertEquals(25,usage.inputTokens());assertEquals(7,usage.outputTokens());assertFalse(usage.estimated());server.verify();
    }
    @Test void summaryUsesLowTemperatureInsteadOfTutorSampling() {
        server.expect(requestTo(BASE+"/chat/completions"))
            .andExpect(jsonPath("$.temperature").value(0.2))
            .andRespond(withSuccess(SUCCESS,MediaType.TEXT_EVENT_STREAM));
        adapter.stream(new HintPrompt("Tóm tắt hội thoại học toán bằng tiếng Việt", "Lịch sử"),ignored -> {});
        server.verify();
    }
    @Test void customSamplingChangesTutorRequestButNotSummaryTemperature() {
        var builder=RestClient.builder().baseUrl(BASE);
        server=MockRestServiceServer.bindTo(builder).build();
        adapter=new GlmHintModelAdapter(builder.build(),"khóa-thử","glm-4.7",Duration.ofSeconds(2),350,Duration.ofSeconds(1),0.6,0.7);
        server.expect(requestTo(BASE+"/chat/completions"))
            .andExpect(jsonPath("$.temperature").value(0.6)).andExpect(jsonPath("$.top_p").value(0.7))
            .andRespond(withSuccess(SUCCESS,MediaType.TEXT_EVENT_STREAM));
        server.expect(requestTo(BASE+"/chat/completions"))
            .andExpect(jsonPath("$.temperature").value(0.2)).andExpect(jsonPath("$.top_p").value(0.7))
            .andRespond(withSuccess(SUCCESS,MediaType.TEXT_EVENT_STREAM));
        adapter.stream(prompt,ignored -> {});
        adapter.stream(new HintPrompt("Tóm tắt hội thoại học toán bằng tiếng Việt", "Lịch sử"),ignored -> {});
        server.verify();
    }
    @Test void supportsCrLfAndEstimatesAbsentUsage() {
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(withSuccess(SUCCESS.replace("\n","\r\n").replace(",\"usage\":{\"prompt_tokens\":25,\"completion_tokens\":7}",""),MediaType.TEXT_EVENT_STREAM));
        assertTrue(adapter.stream(prompt,ignored -> {}).estimated());server.verify();
    }
    @Test void lengthFinishKeepsStreamedText() {
        // Cắt vì max_tokens vẫn là kết thúc hợp lệ: chữ đã phát tới em, không biến thành lỗi.
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(withSuccess(SUCCESS.replace("\"stop\"","\"length\""),MediaType.TEXT_EVENT_STREAM));
        var chunks=new ArrayList<String>();adapter.stream(prompt,chunks::add);assertFalse(chunks.isEmpty());server.verify();
    }
    @Test void partialTransportEofOrMalformedDataCannotComplete() {
        for(String response:List.of("data: {\n\n",SUCCESS.replace("data: [DONE]",""),"data: [DONE]\n\n", "data: {\"choices\":[{\"delta\":{\"content\":42}}]}\n\n")) {
            setup();server.expect(requestTo(BASE+"/chat/completions")).andRespond(withSuccess(response,MediaType.TEXT_EVENT_STREAM));
            assertThrows(RuntimeException.class,() -> adapter.stream(prompt,ignored -> {}));server.verify();
        }
    }
    @Test void refusalOfConsumerStopsProviderWithoutRetry() {
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(withSuccess(SUCCESS,MediaType.TEXT_EVENT_STREAM));
        assertThrows(java.util.concurrent.CancellationException.class,
            () -> adapter.stream(prompt,ignored -> {throw new java.util.concurrent.CancellationException();}));server.verify();
    }
    @Test void providerErrorsDoNotExposeResponseOrFallback() {
        for(HttpStatus status:List.of(HttpStatus.BAD_REQUEST,HttpStatus.UNAUTHORIZED,HttpStatus.FORBIDDEN,HttpStatus.NOT_FOUND,HttpStatus.UNPROCESSABLE_CONTENT,HttpStatus.TOO_MANY_REQUESTS,HttpStatus.SERVICE_UNAVAILABLE)) {
            setup();server.expect(requestTo(BASE+"/chat/completions")).andRespond(withStatus(status).body("dữ liệu nhà không được lộ"));
            if(status==HttpStatus.TOO_MANY_REQUESTS || status==HttpStatus.SERVICE_UNAVAILABLE)
                server.expect(requestTo(BASE+"/chat/completions")).andRespond(withStatus(status).body("dữ liệu nhà không được lộ"));
            var ex=assertThrows(IllegalStateException.class,() -> adapter.stream(prompt,ignored -> {}));assertEquals("MODEL_UNAVAILABLE",ex.getMessage());server.verify();
        }
    }
    @Test void enforcesWholeTransportDeadlinePerAttempt() throws Exception {
        var builder=RestClient.builder().baseUrl(BASE);server=MockRestServiceServer.bindTo(builder).build();
        adapter=new GlmHintModelAdapter(builder.build(),"khóa-thử","glm-4.7",Duration.ofMillis(50),350);
        server.expect(org.springframework.test.web.client.ExpectedCount.twice(),requestTo(BASE+"/chat/completions")).andRespond(request -> {
            try{Thread.sleep(300);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
            return withSuccess(SUCCESS,MediaType.TEXT_EVENT_STREAM).createResponse(request);
        });
        var chunks=new ArrayList<String>();
        assertTimeout(Duration.ofSeconds(2),() -> assertThrows(IllegalStateException.class,() -> adapter.stream(prompt,chunks::add)));
        Thread.sleep(350); // Cả hai response muộn đều phải bị child cancellation chặn.
        assertTrue(chunks.isEmpty());server.verify();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints={429,500,501,503,599})
    void retriesOnceAfterTransientStatusWithBackoff(int status) {
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(withStatus(HttpStatusCode.valueOf(status)));
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(withSuccess(SUCCESS,MediaType.TEXT_EVENT_STREAM));
        long start=System.nanoTime();
        var chunks=new ArrayList<String>();adapter.stream(prompt,chunks::add);
        assertTrue(System.nanoTime()-start>=TimeUnit.MILLISECONDS.toNanos(640));
        assertEquals(List.of("Em đã ","thử gì?"),chunks);server.verify();
    }
    @Test void retriesTransportFailureBeforeFirstToken() {
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(withException(new IOException("Lỗi vận chuyển")));
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(withSuccess(SUCCESS,MediaType.TEXT_EVENT_STREAM));
        assertEquals("Em đã thử gì?",adapter.draft(prompt).orElseThrow());server.verify();
    }
    @Test void retriesTransportFailureWhileReadingBeforeFirstToken() {
        expectStream(new StalledStream("data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"chờ\"}}]}\n\n",true));
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(withSuccess(SUCCESS,MediaType.TEXT_EVENT_STREAM));
        assertEquals("Em đã thử gì?",adapter.draft(prompt).orElseThrow());server.verify();
    }
    @Test void malformedUtf8CannotTriggerTransportRetry() {
        expectStream(new ByteArrayInputStream(new byte[]{(byte)0xC3,(byte)0x28}));
        assertThrows(IllegalStateException.class,() -> adapter.stream(prompt,ignored -> fail("Không được phát token")));server.verify();
    }
    @Test void malformedChunkIsTerminalEvenIfTheStreamThenStalls() {
        var builder=RestClient.builder().baseUrl(BASE);server=MockRestServiceServer.bindTo(builder).build();
        adapter=new GlmHintModelAdapter(builder.build(),"khóa-thử","glm-4.7",Duration.ofSeconds(2),350,Duration.ofMillis(80));
        var input=new StalledStream("data: {\"choices\":[{\"delta\":42}]}\n\n",false);
        expectStream(input);
        assertThrows(IllegalStateException.class,() -> adapter.stream(prompt,ignored -> fail("Không được phát token")));
        assertEquals(0,input.closed.getCount());server.verify();
    }
    @Test void healthyStreamResetsIdleTimeoutAfterFirstToken() {
        var builder=RestClient.builder().baseUrl(BASE);server=MockRestServiceServer.bindTo(builder).build();
        adapter=new GlmHintModelAdapter(builder.build(),"khóa-thử","glm-4.7",Duration.ofSeconds(2),350,Duration.ofMillis(200));
        var chunks=List.of(
            "data: {\"choices\":[{\"delta\":{\"content\":\"Em đã \"}}]}\n\n",
            ": ping\n\n", ": ping\n\n", ": ping\n\n", ": ping\n\n",
            "data: {\"choices\":[{\"delta\":{\"content\":\"thử gì?\"},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n");
        expectStream(new InputStream() {
            private int position;
            @Override public int read(byte[] bytes,int offset,int length) throws IOException {
                if(position==chunks.size())return -1;
                if(position>0)try { Thread.sleep(60); }
                catch(InterruptedException ex) { throw new IOException(ex); }
                byte[] chunk=chunks.get(position++).getBytes(StandardCharsets.UTF_8);
                assertTrue(length>=chunk.length);System.arraycopy(chunk,0,bytes,offset,chunk.length);return chunk.length;
            }
            @Override public int read() { throw new AssertionError("Reader phải đọc chunk"); }
        });
        assertEquals("Em đã thử gì?",adapter.draft(prompt).orElseThrow());server.verify();
    }
    @Test void retriesStallBeforeFirstTokenAndClosesOnlyTheAttempt() {
        var builder=RestClient.builder().baseUrl(BASE);server=MockRestServiceServer.bindTo(builder).build();
        adapter=new GlmHintModelAdapter(builder.build(),"khóa-thử","glm-4.7",Duration.ofSeconds(2),350,Duration.ofMillis(80));
        var stalled=new StalledStream(": ping\n\ndata: {\"choices\":[{\"delta\":{\"reasoning_content\":\"chờ\"}}]}\n\n",false);
        expectStream(stalled);
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(withSuccess(SUCCESS,MediaType.TEXT_EVENT_STREAM));
        var cancellation=new vn.hoctapcanman.core.tutor.application.port.HintModel.Cancellation();
        var chunks=new ArrayList<String>();
        assertTimeout(Duration.ofSeconds(2),() -> adapter.stream(prompt,chunks::add,cancellation));
        assertEquals(0,stalled.closed.getCount());assertFalse(stalled.interrupted.get());
        cancellation.check();assertEquals(List.of("Em đã ","thử gì?"),chunks);server.verify();
    }
    @Test void secondStallIsTerminalWithoutCancellingTheTurn() {
        var builder=RestClient.builder().baseUrl(BASE);server=MockRestServiceServer.bindTo(builder).build();
        adapter=new GlmHintModelAdapter(builder.build(),"khóa-thử","glm-4.7",Duration.ofSeconds(2),350,Duration.ofMillis(80));
        var first=new StalledStream("",false);var second=new StalledStream("",false);
        expectStream(first);expectStream(second);
        var cancellation=new vn.hoctapcanman.core.tutor.application.port.HintModel.Cancellation();
        assertThrows(IllegalStateException.class,() -> adapter.stream(prompt,ignored -> fail("Không được phát token"),cancellation));
        assertEquals(0,first.closed.getCount());assertEquals(0,second.closed.getCount());cancellation.check();server.verify();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void neverRetriesStallOrTransportFailureAfterFirstToken(boolean transportFailure) {
        var builder=RestClient.builder().baseUrl(BASE);server=MockRestServiceServer.bindTo(builder).build();
        adapter=new GlmHintModelAdapter(builder.build(),"khóa-thử","glm-4.7",Duration.ofSeconds(2),350,Duration.ofMillis(80));
        var stalled=new StalledStream("data: {\"choices\":[{\"delta\":{\"content\":\"Em đã \"}}]}\n\n",transportFailure);
        expectStream(stalled);
        var chunks=new ArrayList<String>();
        assertThrows(IllegalStateException.class,() -> adapter.stream(prompt,chunks::add));
        assertEquals(List.of("Em đã "),chunks);assertEquals(0,stalled.closed.getCount());server.verify();
    }
    @Test void cancellationDuringBackoffStopsWithoutAnotherRequest() throws Exception {
        var responseClosed=new CountDownLatch(1);
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(request ->
            new org.springframework.mock.http.client.MockClientHttpResponse(new byte[0],HttpStatus.SERVICE_UNAVAILABLE) {
                @Override public void close() { super.close();responseClosed.countDown(); }
            });
        var cancellation=new vn.hoctapcanman.core.tutor.application.port.HintModel.Cancellation();
        var result=new FutureTask<>(() -> adapter.stream(prompt,ignored -> fail("Không được phát token"),cancellation));
        Thread.startVirtualThread(result);
        assertTrue(responseClosed.await(2,TimeUnit.SECONDS));
        Thread.sleep(100); // Backoff tối thiểu 640 ms; lúc này attempt đã kết thúc.
        cancellation.cancel();
        var failure=assertThrows(ExecutionException.class,() -> result.get(300,TimeUnit.MILLISECONDS));
        assertInstanceOf(CancellationException.class,failure.getCause());server.verify();
    }
    private void expectStream(InputStream input) {
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(request -> {
            var response=new org.springframework.mock.http.client.MockClientHttpResponse(input,HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);return response;
        });
    }
    private static final class StalledStream extends InputStream {
        private final byte[] prefix;
        private final boolean transportFailure;
        private int position;
        final CountDownLatch closed=new CountDownLatch(1);
        final AtomicBoolean interrupted=new AtomicBoolean();
        StalledStream(String prefix,boolean transportFailure) {
            this.prefix=prefix.getBytes(StandardCharsets.UTF_8);this.transportFailure=transportFailure;
        }
        @Override public int read(byte[] bytes,int offset,int length) throws IOException {
            if(position<prefix.length) {
                int count=Math.min(length,prefix.length-position);
                System.arraycopy(prefix,position,bytes,offset,count);position+=count;return count;
            }
            if(transportFailure)throw new IOException("Kết nối đã mất");
            try { if(!closed.await(3,TimeUnit.SECONDS))throw new IOException("Stream chưa được đóng"); }
            catch(InterruptedException ex) { interrupted.set(true);throw new IOException(ex); }
            return -1;
        }
        @Override public int read() throws IOException {
            byte[] value=new byte[1];return read(value,0,1)<0?-1:Byte.toUnsignedInt(value[0]);
        }
        @Override public void close() { closed.countDown(); }
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
