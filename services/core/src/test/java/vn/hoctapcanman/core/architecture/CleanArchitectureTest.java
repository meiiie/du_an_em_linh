package vn.hoctapcanman.core.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Luật phụ thuộc Clean Architecture (domain ← application ← infrastructure) trên mã thật.
 *
 * <p>Chép từ {@code LMS_hohulili@34c3f0f2:backend/src/test/java/com/example/lms/architecture/CleanArchitectureTest.java}
 * (MIT): đổi gói, bỏ danh sách nợ kỹ thuật của LMS, giữ ngoại lệ CQRS {@code Get*}; luật nằm ở {@link KienTrucRules}.
 */
@DisplayName("Luật Clean Architecture")
class CleanArchitectureTest {

    private static final JavaClasses MA_THAT = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("vn.hoctapcanman.core");

    static Stream<Named<ArchRule>> luat() {
        return KienTrucRules.CLEAN.stream().map(rule -> Named.of(rule.getDescription(), rule));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("luat")
    void maThatTuanThu(ArchRule rule) {
        rule.check(MA_THAT);
    }

}
