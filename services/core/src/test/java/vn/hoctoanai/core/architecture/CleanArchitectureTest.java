package vn.hoctoanai.core.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Luật phụ thuộc Clean Architecture: domain ← application ← infrastructure.
 *
 * <p>Chép từ {@code LMS_hohulili@34c3f0f2:backend/src/test/java/com/example/lms/architecture/CleanArchitectureTest.java}
 * (MIT): đổi gói, bỏ danh sách nợ kỹ thuật của LMS. Luật «không phụ thuộc infrastructure» của LMS chỉ phủ use case ghi
 * và DTO; ở đây phủ mọi lớp application (port, dịch vụ, DTO, use case). Giữ ngoại lệ CQRS của LMS: use case đọc
 * ({@code Get*}, kể cả lớp lồng) được đọc thẳng tầng persistence.
 */
@DisplayName("Luật Clean Architecture")
class CleanArchitectureTest {

    private static final DescribedPredicate<JavaClass> QUERY_USE_CASES = new DescribedPredicate<>("use case đọc Get*") {
        @Override
        public boolean test(JavaClass javaClass) {
            JavaClass top = javaClass;
            while (top.getEnclosingClass().isPresent()) {
                top = top.getEnclosingClass().get();
            }
            return javaClass.getPackageName().contains(".application.usecase") && top.getSimpleName().startsWith("Get");
        }
    };

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("vn.hoctoanai.core");
    }

    @Nested
    @DisplayName("Tầng domain")
    class DomainLayerRules {

        @Test
        @DisplayName("Model domain không phụ thuộc infrastructure")
        void domainShouldNotDependOnInfrastructure() {
            noClasses().that()
                .resideInAPackage("..domain.model..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .because("model domain là Java thuần, không phụ thuộc hạ tầng")
                .check(importedClasses);
        }

        @Test
        @DisplayName("Repository (port) của domain không phụ thuộc infrastructure")
        void domainRepositoriesShouldNotDependOnInfrastructure() {
            noClasses().that()
                .resideInAPackage("..domain.repository..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .because("port của domain không import entity JPA hay Spring Data")
                .check(importedClasses);
        }

        @Test
        @DisplayName("Model domain không phụ thuộc Spring")
        void domainShouldNotDependOnSpring() {
            noClasses().that()
                .resideInAPackage("..domain.model..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("org.springframework..")
                .because("model domain độc lập framework")
                .check(importedClasses);
        }

        @Test
        @DisplayName("Model domain không phụ thuộc JPA / Hibernate")
        void domainShouldNotDependOnJpa() {
            noClasses().that()
                .resideInAPackage("..domain.model..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
                .because("model domain không dùng annotation JPA; entity là *JpaEntity ở infrastructure")
                .check(importedClasses);
        }

    }

    @Nested
    @DisplayName("Tầng application")
    class ApplicationLayerRules {

        @Test
        @DisplayName("Application không phụ thuộc infrastructure (trừ use case đọc Get*)")
        void applicationShouldNotDependOnInfrastructure() {
            noClasses().that()
                .resideInAPackage("..application..")
                .and(not(QUERY_USE_CASES))
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .because("port, dịch vụ, DTO và use case ghi chỉ dùng domain và port; adapter ở infrastructure hiện thực port")
                .check(importedClasses);
        }

    }

    @Nested
    @DisplayName("Tầng infrastructure")
    class InfrastructureLayerRules {

        @Test
        @DisplayName("Entity JPA nằm trong infrastructure.persistence")
        void jpaEntitiesShouldBeInCorrectPackage() {
            classes().that()
                .areAnnotatedWith("jakarta.persistence.Entity")
                .should()
                .resideInAPackage("..infrastructure.persistence..")
                .because("entity JPA thuộc tầng persistence; domain model không bao giờ là entity")
                .check(importedClasses);
        }

    }

}
