package vn.hoctoanai.core.architecture;

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
 * Luật DDD trên mã thật: đặt tên, biên web (controller chỉ dùng record DTO), entity JPA, repository Spring Data.
 *
 * <p>Chép từ {@code LMS_hohulili@34c3f0f2:backend/src/test/java/com/example/lms/architecture/DddArchitectureTest.java}
 * (MIT): đổi gói, bỏ hậu tố {@code V2}/{@code V3}, mở rộng qua review #65; luật nằm ở {@link KienTrucRules}.
 */
@DisplayName("Luật DDD")
class DddArchitectureTest {

    private static final JavaClasses MA_THAT = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("vn.hoctoanai.core");

    static Stream<Named<ArchRule>> luat() {
        return KienTrucRules.DDD.stream().map(rule -> Named.of(rule.getDescription(), rule));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("luat")
    void maThatTuanThu(ArchRule rule) {
        rule.check(MA_THAT);
    }

}
