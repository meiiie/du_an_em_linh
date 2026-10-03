package vn.hoctapcanman.core.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Mỗi luật ở {@link KienTrucRules} phải bắt được lớp vi phạm cố ý của nó ({@code vn.hoctapcanman.mau.xau}) và để yên một
 * module đúng ({@code vn.hoctapcanman.mau.tot}). Lớp mẫu nằm ngoài {@code vn.hoctapcanman.core}: Spring không quét, test trên
 * mã thật không nhập.
 */
@DisplayName("Luật kiến trúc tự kiểm trên lớp mẫu")
class KienTrucRulesTuKiemTest {

    private static final JavaClasses MAU_XAU = new ClassFileImporter().importPackages("vn.hoctapcanman.mau.xau");
    private static final JavaClasses MAU_TOT = new ClassFileImporter().importPackages("vn.hoctapcanman.mau.tot");

    /** Luật → lớp vi phạm phải xuất hiện trong báo lỗi. */
    private static final Map<ArchRule, List<String>> VI_PHAM = Map.ofEntries(
        Map.entry(KienTrucRules.DOMAIN_KHONG_PHU_THUOC_INFRASTRUCTURE, List.of("DungHaTang")),
        Map.entry(KienTrucRules.DOMAIN_KHONG_PHU_THUOC_APPLICATION, List.of("xau.domain.repository.HocSinhRepository")),
        Map.entry(KienTrucRules.DOMAIN_KHONG_PHU_THUOC_SPRING, List.of("TinhDiem")),
        Map.entry(KienTrucRules.DOMAIN_KHONG_PHU_THUOC_JPA, List.of("CoEntity")),
        Map.entry(KienTrucRules.APPLICATION_KHONG_PHU_THUOC_INFRASTRUCTURE, List.of("CongNgoai", "GoiWeb")),
        Map.entry(KienTrucRules.USE_CASE_DOC_CHI_DUNG_PERSISTENCE, List.of("GetDanhSachUseCase")),
        Map.entry(KienTrucRules.APPLICATION_KHONG_PHU_THUOC_WEB, List.of("GoiWeb")),
        Map.entry(KienTrucRules.USE_CASE_DAT_TEN, List.of("usecase.TaoHocSinh")),
        Map.entry(KienTrucRules.CONTROLLER_DUNG_CHO_DUNG_TEN, List.of("SaiChoController", "HocSinhApi")),
        Map.entry(KienTrucRules.CONTROLLER_KHONG_PHU_THUOC_DOMAIN_PERSISTENCE, List.of("TraDomainController")),
        Map.entry(KienTrucRules.CONTROLLER_CHI_DUNG_RECORD_DTO,
            List.of("TraChuoiController.lay()", "TraMapController.lay()", "NhanChuoiController.tao(java.lang.String)",
                "TraDomainController.lay()")),
        Map.entry(KienTrucRules.ADAPTER_DAT_TEN, List.of("KhoHocSinh")),
        Map.entry(KienTrucRules.ENTITY_DAT_TEN_DUNG_CHO, List.of("HocSinhEntity", "LopJpaEntity", "CoEntity")),
        Map.entry(KienTrucRules.REPOSITORY_CHI_QUAN_LY_JPA_ENTITY,
            List.of("xau.infrastructure.persistence.HocSinhJpaRepository", "xau.infrastructure.persistence.LopHocJpaRepository")),
        Map.entry(KienTrucRules.MODULE_CHI_GOI_NHAU_QUA_CONG, List.of("GieoLop")));

    static Stream<Arguments> luatVaViPham() {
        return KienTrucRules.TAT_CA.stream().map(rule -> arguments(Named.of(rule.getDescription(), rule), VI_PHAM.get(rule)));
    }

    static Stream<Named<ArchRule>> tatCaLuat() {
        return KienTrucRules.TAT_CA.stream().map(rule -> Named.of(rule.getDescription(), rule));
    }

    @Test
    @DisplayName("Mọi luật đều có lớp vi phạm mẫu")
    void moiLuatDeuCoMau() {
        assertThat(VI_PHAM.keySet()).containsExactlyInAnyOrderElementsOf(KienTrucRules.TAT_CA);
    }

    @ParameterizedTest(name = "bắt được: {0}")
    @MethodSource("luatVaViPham")
    void batDuocViPham(ArchRule rule, List<String> lopViPham) {
        assertThatThrownBy(() -> rule.check(MAU_XAU))
            .isInstanceOf(AssertionError.class)
            .satisfies(loi -> lopViPham.forEach(lop -> assertThat(loi.getMessage()).contains(lop)));
    }

    @Test
    @DisplayName("Thư viện ngoài vn.hoctapcanman không phải module, dù gói có chữ domain / application")
    void thuVienNgoaiKhongPhaiModule() {
        assertThat(KienTrucRules.moduleCua("org.springframework.data.domain")).isEmpty();
        assertThat(KienTrucRules.moduleCua("com.example.application.port")).isEmpty();
        assertThat(KienTrucRules.moduleCua("vn.hoctapcanman.core.classroom.domain.model")).contains("vn.hoctapcanman.core.classroom");
    }

    @ParameterizedTest(name = "module đúng qua: {0}")
    @MethodSource("tatCaLuat")
    void moduleDungQuaMoiLuat(ArchRule rule) {
        rule.check(MAU_TOT);
    }

}
