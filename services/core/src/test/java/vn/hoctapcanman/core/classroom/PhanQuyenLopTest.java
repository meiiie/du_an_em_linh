package vn.hoctapcanman.core.classroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.classroom.application.dto.CanhBaoDto;
import vn.hoctapcanman.core.classroom.application.dto.CapNhatCaiDatLopRequest;
import vn.hoctapcanman.core.classroom.application.exception.CanhBaoKhongTimThayException;
import vn.hoctapcanman.core.classroom.application.exception.KhongThuocLopException;
import vn.hoctapcanman.core.classroom.application.port.CanhBaoGiaoVien;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.classroom.application.usecase.CapNhatCaiDatLopUseCase;
import vn.hoctapcanman.core.classroom.application.usecase.GetCaiDatLopUseCase;
import vn.hoctapcanman.core.classroom.application.usecase.GetCanhBaoCuaLopUseCase;
import vn.hoctapcanman.core.classroom.application.usecase.GetHocSinhCuaLopUseCase;
import vn.hoctapcanman.core.classroom.application.usecase.XuLyCanhBaoUseCase;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.ClassRole;
import vn.hoctapcanman.core.classroom.domain.model.ClassSettings;
import vn.hoctapcanman.core.classroom.domain.model.Enrollment;
import vn.hoctapcanman.core.classroom.domain.model.SchoolClass;
import vn.hoctapcanman.core.classroom.domain.repository.ClassSettingsRepository;
import vn.hoctapcanman.core.classroom.domain.repository.EnrollmentRepository;
import vn.hoctapcanman.core.classroom.domain.repository.SchoolClassRepository;
import vn.hoctapcanman.core.identity.domain.model.Email;
import vn.hoctapcanman.core.identity.domain.model.Role;
import vn.hoctapcanman.core.identity.domain.model.User;
import vn.hoctapcanman.core.identity.domain.repository.UserRepository;

/**
 * F-08 của v0 chép sang core ({@code apps/web/tests/e2e/phan-quyen-lop.spec.ts}): lớp B (giáo viên B, học sinh Dũng)
 * cạnh lớp A «12A1 thử» (giáo viên A, An, Bình, Chi). Giáo viên mỗi lớp chỉ thấy học sinh và cài đặt của lớp mình;
 * giáo viên lớp khác và học sinh bị từ chối ở use case (FR-032). Mỗi test chạy trong giao dịch rồi hoàn tác.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
@DisplayName("Phân quyền theo lớp (F-08)")
class PhanQuyenLopTest {

    private static final Instant NOW = Instant.parse("2026-10-02T08:00:00Z");

    @Autowired
    private UserRepository users;

    @Autowired
    private SchoolClassRepository classes;

    @Autowired
    private EnrollmentRepository enrollments;

    @Autowired
    private ClassSettingsRepository settings;

    @Autowired
    private ClassMembership membership;

    @Autowired
    private CanhBaoGiaoVien canhBao;

    @Autowired
    private GetHocSinhCuaLopUseCase hocSinhCuaLop;

    @Autowired
    private GetCaiDatLopUseCase caiDat;

    @Autowired
    private CapNhatCaiDatLopUseCase capNhatCaiDat;

    @Autowired
    private GetCanhBaoCuaLopUseCase canhBaoCuaLop;

    @Autowired
    private XuLyCanhBaoUseCase xuLyCanhBao;

    private UUID gvA;
    private UUID gvB;
    private UUID an;
    private UUID binh;
    private UUID chi;
    private UUID dung;
    private UUID lopA;
    private UUID lopB;

    @BeforeEach
    void dungHaiLop() {
        gvA = nguoi("gv.a@demo.local", "Giáo viên A", Role.TEACHER);
        gvB = nguoi("gv.b@demo.local", "Giáo viên B", Role.TEACHER);
        an = nguoi("hs.an.f08@demo.local", "An", Role.STUDENT);
        binh = nguoi("hs.binh.f08@demo.local", "Bình", Role.STUDENT);
        chi = nguoi("hs.chi.f08@demo.local", "Chi", Role.STUDENT);
        dung = nguoi("hs.dung@demo.local", "Dũng Lớp B", Role.STUDENT);
        lopA = lop("12A1 thử", gvA, List.of(an, binh, chi), false);
        lopB = lop("12B thử", gvB, List.of(dung), true);
    }

    @Test
    @DisplayName("Giáo viên lớp B chỉ thấy học sinh và cài đặt lớp B")
    void giaoVienLopBChiThayLopB() {
        assertThat(hocSinhCuaLop.execute(gvB, lopB)).containsExactly(dung);
        assertThat(caiDat.execute(gvB, lopB).moLoiGiaiSauKhiNop()).isTrue();
        assertThat(membership.lopDay(gvB)).containsExactly(lopB);
    }

    @Test
    @DisplayName("Giáo viên lớp A không thấy học sinh lớp B; cài đặt lớp A không đổi")
    void giaoVienLopAKhongThayLopB() {
        assertThat(hocSinhCuaLop.execute(gvA, lopA)).containsExactlyInAnyOrder(an, binh, chi).doesNotContain(dung);
        assertThat(caiDat.execute(gvA, lopA).moLoiGiaiSauKhiNop()).isFalse();
        assertThat(membership.giaoVienDayHocSinh(gvA, an)).isTrue();
        assertThat(membership.giaoVienDayHocSinh(gvA, dung)).isFalse();
    }

    @Test
    @DisplayName("Giáo viên lớp khác bị từ chối mọi thao tác trên lớp; cài đặt lớp đó không đổi")
    void giaoVienLopKhacBiTuChoi() {
        assertThatThrownBy(() -> hocSinhCuaLop.execute(gvA, lopB)).isInstanceOf(KhongThuocLopException.class);
        assertThatThrownBy(() -> caiDat.execute(gvA, lopB)).isInstanceOf(KhongThuocLopException.class);
        assertThatThrownBy(() -> capNhatCaiDat.execute(gvA, lopB, new CapNhatCaiDatLopRequest(false, "offline", false)))
            .isInstanceOf(KhongThuocLopException.class);
        assertThatThrownBy(() -> canhBaoCuaLop.execute(gvA, lopB, true)).isInstanceOf(KhongThuocLopException.class);
        assertThat(caiDat.execute(gvB, lopB).moLoiGiaiSauKhiNop()).isTrue();
        assertThat(membership.lopDangDay(gvA, lopB)).isEmpty();
        assertThat(membership.lopDangDay(gvA, null)).contains(lopA);
    }

    @Test
    @DisplayName("Học sinh không dùng được thao tác của giáo viên, kể cả trên lớp mình")
    void hocSinhBiTuChoi() {
        assertThat(membership.lopHoc(dung)).contains(lopB);
        assertThatThrownBy(() -> hocSinhCuaLop.execute(dung, lopB)).isInstanceOf(KhongThuocLopException.class);
        assertThatThrownBy(() -> caiDat.execute(dung, lopB)).isInstanceOf(KhongThuocLopException.class);
        assertThatThrownBy(() -> capNhatCaiDat.execute(dung, lopB, new CapNhatCaiDatLopRequest(false, "offline", false)))
            .isInstanceOf(KhongThuocLopException.class);
    }

    @Test
    @DisplayName("Giáo viên của lớp đổi cài đặt; ghi người đổi")
    void giaoVienDoiCaiDatLopMinh() {
        assertThat(capNhatCaiDat.execute(gvA, lopA, new CapNhatCaiDatLopRequest(true, "openrouter", false)))
            .satisfies(c -> {
                assertThat(c.moLoiGiaiSauKhiNop()).isTrue();
                assertThat(c.nhaAi()).isEqualTo("openrouter");
            });
        assertThat(settings.findByClassId(new ClassId(lopA))).hasValueSatisfying(s -> assertThat(s.updatedBy()).isEqualTo(gvA));
        assertThat(caiDat.execute(gvB, lopB).nhaAi()).isEqualTo("offline");
    }

    @Test
    @DisplayName("Cảnh báo tới đúng giáo viên của lớp học sinh; lớp khác không thấy, không xử lý được")
    void canhBaoChiToiGiaoVienCuaLop() {
        assertThat(canhBao.ghiNhoGiaoVien(dung, "T12.DH.03", "B12-01", "B.DH.XETDAU", "Em nhờ thầy cô ở bước xét dấu.")).isTrue();
        assertThat(canhBao.ghiNhoGiaoVien(dung, "T12.DH.03", "B12-01", "B.DH.XETDAU", "Em nhờ thầy cô lần nữa.")).isFalse();
        assertThat(canhBao.ghiKet(an, "T12.DH.03", null, null, "Kẹt 3 lượt ở T12.DH.03.")).isTrue();
        UUID chuaVaoLop = nguoi("hs.moi@demo.local", "Học sinh mới", Role.STUDENT);
        assertThat(canhBao.ghiKet(chuaVaoLop, "T12.DH.03", null, null, "Kẹt.")).isFalse();

        List<CanhBaoDto> lopBMo = canhBaoCuaLop.execute(gvB, lopB, true);
        assertThat(lopBMo).singleElement().satisfies(c -> {
            assertThat(c.hocSinhId()).isEqualTo(dung);
            assertThat(c.loai()).isEqualTo("NHO_GV");
            assertThat(c.maBuoc()).isEqualTo("B.DH.XETDAU");
        });
        assertThat(canhBaoCuaLop.execute(gvA, lopA, true)).extracting(CanhBaoDto::hocSinhId).containsExactly(an);

        UUID id = lopBMo.getFirst().id();
        assertThatThrownBy(() -> xuLyCanhBao.execute(gvA, id)).isInstanceOf(CanhBaoKhongTimThayException.class);
        assertThatThrownBy(() -> xuLyCanhBao.execute(dung, id)).isInstanceOf(CanhBaoKhongTimThayException.class);
        assertThatThrownBy(() -> xuLyCanhBao.execute(gvB, UUID.randomUUID())).isInstanceOf(CanhBaoKhongTimThayException.class);
        assertThat(xuLyCanhBao.execute(gvB, id).daXuLy()).isTrue();
        assertThat(canhBaoCuaLop.execute(gvB, lopB, true)).isEmpty();
        assertThat(canhBao.ghiNhoGiaoVien(dung, "T12.DH.03", "B12-01", null, "Em lại nhờ thầy cô.")).isTrue();
    }

    private UUID nguoi(String email, String ten, Role role) {
        return users.save(User.create(new Email(email), "{bcrypt}x", ten, role, true, NOW)).id().value();
    }

    private UUID lop(String ten, UUID giaoVien, List<UUID> hocSinh, boolean moLoiGiai) {
        ClassId id = classes.save(SchoolClass.create(ten, 12, "2026-2027", NOW)).id();
        enrollments.save(new Enrollment(id, giaoVien, ClassRole.TEACHER, NOW));
        hocSinh.forEach(hs -> enrollments.save(new Enrollment(id, hs, ClassRole.STUDENT, NOW)));
        settings.save(ClassSettings.macDinh(id, NOW).capNhat(moLoiGiai, "offline", false, giaoVien, NOW));
        return id.value();
    }
}
