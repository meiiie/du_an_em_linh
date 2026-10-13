package vn.hoctapcanman.core.tutor;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.tutor.application.service.*;
import vn.hoctapcanman.core.tutor.infrastructure.ai.OfflineHintModelAdapter;
import vn.hoctapcanman.core.tutor.infrastructure.web.OfflineTutorController;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.port.*;
import vn.hoctapcanman.core.workspace.infrastructure.web.WorkspaceExceptionHandler;

class GiaSuControllerTest {
    @Test void rateLimitReturns429ProblemBeforeSseStarts() throws Exception {
        var store = mock(WorkspaceStore.class);
        var membership = mock(ClassMembership.class);
        var gate = mock(ContentWorkflow.class);
        UUID student = UUID.randomUUID(), cl = UUID.randomUUID(), problem = UUID.randomUUID();
        var limiter = new TutorRateLimiter(Clock.systemUTC(), 1, Duration.ofMinutes(10));
        var service = new OfflineTutorService(store, membership, gate, new OfflineHintModelAdapter(), limiter);
        when(membership.lopHoc(student)).thenReturn(Optional.of(cl));
        when(store.tutorContext(eq(student), eq(cl), any())).thenReturn(Optional.of(
            new TutorContext(problem, "DH1", "Đề bài", "x", "B.DH.TXD", "Em thử xem lại nhé.")));
        when(gate.filter(any(), any(), anyString(), anyString(), anyList())).thenReturn(true);
        var mvc = MockMvcBuilders.standaloneSetup(new OfflineTutorController(service))
            .setControllerAdvice(new WorkspaceExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        var jwt = Jwt.withTokenValue("tổng-hợp").header("alg", "none").subject(student.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        String body = """
            {"maBai":"DH1","maBuoc":"B.DH.TXD","cap":1,"cauHoi":"Em bị kẹt"}
            """;
        try {
            var started = mvc.perform(post("/api/hs/gia-su").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(request().asyncStarted()).andReturn();
            started.getAsyncResult(3000);
            mvc.perform(asyncDispatch(started)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:xong")));
            mvc.perform(post("/api/hs/gia-su").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests()).andExpect(request().asyncNotStarted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Em đã hỏi nhiều lần trong thời gian ngắn. Em chờ một chút rồi thử lại nhé."));
            verify(store, times(1)).tutorContext(eq(student), eq(cl), any());
        } finally { SecurityContextHolder.clearContext(); }
    }
}
