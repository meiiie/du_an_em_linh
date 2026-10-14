package vn.hoctapcanman.core.tutor;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.hoctapcanman.core.tutor.application.service.TutorSessionService;
import vn.hoctapcanman.core.tutor.infrastructure.web.*;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;
import vn.hoctapcanman.core.workspace.infrastructure.web.WorkspaceExceptionHandler;

class GiaSuControllerTest {
    @Test void rateLimitReturns429BeforeSseStarts() throws Exception {
        var tutor=mock(TutorSessionService.class);
        UUID student=UUID.randomUUID();
        when(tutor.startLegacy(eq(student),any())).thenThrow(new vn.hoctapcanman.core.tutor.application.exception.TutorLimitException(12));
        var mvc=MockMvcBuilders.standaloneSetup(new OfflineTutorController(tutor),new TutorSessionController(tutor))
            .setControllerAdvice(new WorkspaceExceptionHandler(),new TutorExceptionHandler()).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        var jwt=Jwt.withTokenValue("tổng-hợp").header("alg","none").subject(student.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        try {
            mvc.perform(post("/api/hs/gia-su").accept(MediaType.TEXT_EVENT_STREAM).contentType(MediaType.APPLICATION_JSON)
                .content("{\"maBai\":\"DH1\",\"maBuoc\":\"B.DH.TXD\",\"cap\":1,\"cauHoi\":\"Em bị kẹt\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After","12")).andExpect(request().asyncNotStarted());
        }finally{SecurityContextHolder.clearContext();}
    }
}
