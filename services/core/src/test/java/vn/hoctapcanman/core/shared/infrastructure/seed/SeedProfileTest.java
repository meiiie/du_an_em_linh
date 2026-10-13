package vn.hoctapcanman.core.shared.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vn.hoctapcanman.core.workspace.infrastructure.seed.WorkspaceNguyenTraiSeedAdapter;
import vn.hoctapcanman.core.classroom.infrastructure.seed.LopNguyenTraiSeeder;
import vn.hoctapcanman.core.content.infrastructure.nhap.NoiDungNguyenTraiSeeder;
import vn.hoctapcanman.core.identity.infrastructure.seed.TaiKhoanNguyenTraiSeeder;
import vn.hoctapcanman.core.mastery.infrastructure.seed.MucHieuNguyenTraiSeeder;
import vn.hoctapcanman.core.practice.infrastructure.seed.GiaoBaiNguyenTraiSeeder;
import vn.hoctapcanman.core.practice.infrastructure.seed.LichSuNguyenTraiSeeder;

class SeedProfileTest {
    @Test void prodKhongTaoSeederVaKhongCanBatKyCongDbCasHayMatKhau() {
        new ApplicationContextRunner().withUserConfiguration(TaiKhoanNguyenTraiSeeder.class,LopNguyenTraiSeeder.class,
            NoiDungNguyenTraiSeeder.class,GiaoBaiNguyenTraiSeeder.class,LichSuNguyenTraiSeeder.class,MucHieuNguyenTraiSeeder.class,WorkspaceNguyenTraiSeedAdapter.class)
            .withPropertyValues("spring.profiles.active=prod","app.content.import-on-startup=true")
            .run(context -> {
                assertThat(context).hasNotFailed();
                for(Class<?> type:List.of(TaiKhoanNguyenTraiSeeder.class,LopNguyenTraiSeeder.class,NoiDungNguyenTraiSeeder.class,
                    GiaoBaiNguyenTraiSeeder.class,LichSuNguyenTraiSeeder.class,MucHieuNguyenTraiSeeder.class,WorkspaceNguyenTraiSeedAdapter.class))
                    assertThat(context).doesNotHaveBean(type);
            });
    }
    @Test void rosterKhongTrungTaiKhoanVaDungPhanCong() {
        var roster=TruongNguyenTrai.taiKhoan();
        assertThat(roster).hasSize(35);
        assertThat(roster.stream().map(TruongNguyenTrai.TaiKhoan::email)).doesNotHaveDuplicates();
        for(String name:TruongNguyenTrai.LOP) {
            assertThat(roster).filteredOn(t -> t.lop().contains(name)&&t.vaiTro().equals("STUDENT")).hasSize(10);
            assertThat(roster).filteredOn(t -> t.lop().contains(name)&&t.vaiTro().equals("TEACHER")).hasSize(2);
        }
    }
}
