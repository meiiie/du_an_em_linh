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
    @Test void rejectsNonSseSuccessAndToolCalls() {
        server.expect(requestTo(BASE+"/chat/completions")).andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));assertTrue(adapter.draft(prompt).isEmpty());server.verify();
        setup();server.expect(requestTo(BASE+"/chat/completions")).andRespond(withSuccess("data: {\"choices\":[{\"delta\":{\"tool_calls\":[]}}]}\n\n",MediaType.TEXT_EVENT_STREAM));
        assertTrue(adapter.draft(prompt).isEmpty());server.verify();
    }
}
