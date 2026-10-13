package vn.hoctapcanman.core.shared.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Dựng từ gốc repo bằng Dockerfile core, rồi chạy Maven với -Dcore.image=tên-ảnh để kiểm tra ảnh độc lập. */
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfSystemProperty(named="core.image",matches=".+")
class CoreImageTest {
    @Container
    static final GenericContainer<?> core = new GenericContainer<>(DockerImageName.parse(
        System.getProperty("core.image","hoc-toan-core:test")))
        .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("sh","-c"))
        .withCommand("echo ready; sleep 300")
        .waitingFor(Wait.forLogMessage(".*ready.*\\n",1));

    @Test void anhDocDuNoiDungSeedDevKhongCanGanThuMucMayChu() throws Exception {
        var source = core.execInContainer("printenv","APP_CONTENT_SOURCE");
        assertThat(source.getExitCode()).isZero();
        assertThat(source.getStdout().strip()).isEqualTo("/app/noi-dung");
        for (String file : new String[]{"danh-muc.json","ky-nang.json","khung-buoc.json","tai-lieu.json",
                "bang-dao-ham.json","bang-chuong.json","bai-tap.json","bai-lam-lich-su.json"}) {
            assertThat(core.execInContainer("test","-r","/app/noi-dung/dev/nguyen-trai/"+file).getExitCode())
                .as("Dữ liệu dev đọc được: %s",file).isZero();
        }
        for (String directory : new String[]{"supham","v0"}) {
            assertThat(core.execInContainer("test","-d","/app/noi-dung/"+directory).getExitCode()).isZero();
        }
    }
}
