package vn.hoctoanai.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
class CoreApplicationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void healthIsUp() {
		assertThat(mvc.get().uri("/actuator/health"))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.status")
			.isEqualTo("UP");
	}

	@Test
	void probesAreUp() {
		assertThat(mvc.get().uri("/actuator/health/liveness")).hasStatusOk();
		assertThat(mvc.get().uri("/actuator/health/readiness")).hasStatusOk();
	}

}
