package vn.hoctapcanman.core.tutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.hoctapcanman.core.tutor.infrastructure.web.*;
import vn.hoctapcanman.core.workspace.infrastructure.web.WorkspaceExceptionHandler;

class TutorSessionControllerTest {
    TutorSessionServiceTest fixture;
    MockMvc mvc;
    UUID session;
    @BeforeEach void setup() {
        fixture=new TutorSessionServiceTest();fixture.setup();session=fixture.service.open(fixture.student,fixture.assignment).id();
        mvc=MockMvcBuilders.standaloneSetup(new TutorSessionController(fixture.service),new OfflineTutorController(fixture.service))
            .setControllerAdvice(new WorkspaceExceptionHandler(),new TutorExceptionHandler()).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        var jwt=Jwt.withTokenValue("tổng-hợp").header("alg","none").subject(fixture.student.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    String body(UUID request){return "{\"requestId\":\""+request+"\",\"maBuoc\":\"B.DH.TXD\",\"cap\":1,\"cauHoi\":\"Em đã thử cách này\"}";}
    @Test void openReturnsDesignSessionIdAndCursorWithoutCallingModel() throws Exception {
        mvc.perform(post("/api/hs/gia-su/v2/phien").contentType(MediaType.APPLICATION_JSON)
            .content("{\"giaoBaiId\":\""+fixture.assignment+"\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.sessionId").value(session.toString())).andExpect(jsonPath("$.lastSeq").value(0))
            .andExpect(jsonPath("$.status").value("idle"));
        org.mockito.Mockito.verify(fixture.model,org.mockito.Mockito.never()).draft(org.mockito.ArgumentMatchers.any());
    }
    @Test void historyExposesCompactionSummaryAndSourceCursor() throws Exception {
        var current=fixture.store.snapshot(session);
        fixture.store.compact(current,"Tóm tắt đã kiểm.",7,120);
        mvc.perform(get("/api/hs/gia-su/v2/phien/{id}/luot",session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.session.summaryThrough").value(7))
            .andExpect(jsonPath("$.session.tomTat").value("Tóm tắt đã kiểm."))
            .andExpect(jsonPath("$.session.tokenCount").value(120));
    }
    @Test void durableStartDeltaDoneAndResumeFromEitherCursor() throws Exception {
        var response=mvc.perform(post("/api/hs/gia-su/v2/phien/{id}/luot",session).contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID())))
            .andExpect(request().asyncStarted()).andReturn();response.getAsyncResult(5000);
        String events=mvc.perform(asyncDispatch(response)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-cache, no-transform"))
            .andExpect(header().string("X-Accel-Buffering","no")).andReturn().getResponse().getContentAsString();
        assertTrue(events.contains("event:start"));assertTrue(events.contains("event:delta"));assertTrue(events.contains("event:done"));assertTrue(events.contains("id:"+session+":2"));
        var resumed=mvc.perform(get("/api/hs/gia-su/v2/phien/{id}/su-kien",session).header("Last-Event-ID",session+":1"))
            .andExpect(request().asyncStarted()).andReturn();resumed.getAsyncResult(5000);
        String replay=mvc.perform(asyncDispatch(resumed)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(replay.contains("event:start"));assertTrue(replay.contains("event:delta"));assertTrue(replay.contains("event:done"));
    }
    @Test void flushesRawPartialTokenWhileProviderIsStillStreaming() throws Exception {
        var emitted=new java.util.concurrent.CountDownLatch(1);
        var finishProvider=new java.util.concurrent.CountDownLatch(1);
        org.mockito.Mockito.doAnswer(i -> {
            java.util.function.Consumer<String> chunks=i.getArgument(1);
            chunks.accept("Em thử $x");emitted.countDown();
            assertTrue(finishProvider.await(5,java.util.concurrent.TimeUnit.SECONDS));
            chunks.accept("^2$");
            return new vn.hoctapcanman.core.tutor.application.dto.TutorSessionDto.ModelUsage(20,10,false);
        }).when(fixture.model).stream(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        try {
            var response=mvc.perform(post("/api/hs/gia-su/v2/phien/{id}/luot",session).contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID())))
                .andExpect(request().asyncStarted()).andReturn();
            assertTrue(emitted.await(3,java.util.concurrent.TimeUnit.SECONDS));
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
            while(!response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains("Em thử $x") && System.nanoTime()<deadline)Thread.sleep(10);
            String partial=response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(partial.contains("event:delta"));assertTrue(partial.contains("Em thử $x"));assertFalse(partial.contains("event:done"));
            assertFalse(partial.contains("sentenceIndex"));assertFalse(partial.contains("event:regen"));
            org.mockito.Mockito.verifyNoInteractions(fixture.gate);
            finishProvider.countDown();response.getAsyncResult(5000);
            mvc.perform(asyncDispatch(response)).andExpect(status().isOk());
        } finally {finishProvider.countDown();}
    }
    @Test void mismatchedOrFutureCursorReturns409BeforeStream() throws Exception {
        mvc.perform(get("/api/hs/gia-su/v2/phien/{id}/su-kien",session).param("lastSeq","0").header("Last-Event-ID",session+":1"))
            .andExpect(status().isConflict()).andExpect(request().asyncNotStarted());
        mvc.perform(get("/api/hs/gia-su/v2/phien/{id}/su-kien",session).param("lastSeq","99"))
            .andExpect(status().isConflict()).andExpect(request().asyncNotStarted());
    }
    @Test void legacyJsonUsesTheSameDurableSession() throws Exception {
        mvc.perform(post("/api/hs/gia-su/tra-loi").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .content("{\"maBai\":\"DH1\",\"maBuoc\":\"B.DH.TXD\",\"cap\":1,\"cauHoi\":\"Em bị kẹt\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nguon").value("MO_HINH"));
        assertEquals(1,fixture.store.turns(session,0,50).size());assertEquals(List.of("start","delta","done"),fixture.store.events(session,0).stream().map(e -> e.type()).toList());
    }
    @Test void legacyFetchWithoutAcceptStillGetsSse() throws Exception {
        var response=mvc.perform(post("/api/hs/gia-su").contentType(MediaType.APPLICATION_JSON)
            .content("{\"maBai\":\"DH1\",\"maBuoc\":\"B.DH.TXD\",\"cap\":1,\"cauHoi\":\"Em bị kẹt\"}"))
            .andExpect(request().asyncStarted()).andReturn();response.getAsyncResult(5000);
        mvc.perform(asyncDispatch(response)).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("event:xong")));
    }
    @Test void invalidLevelNeverStartsModel() throws Exception {
        mvc.perform(post("/api/hs/gia-su/v2/phien/{id}/luot",session).contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID()).replace("\"cap\":1","\"cap\":4")))
            .andExpect(status().isBadRequest()).andExpect(request().asyncNotStarted());assertTrue(fixture.store.turns(session,0,50).isEmpty());
    }
}
