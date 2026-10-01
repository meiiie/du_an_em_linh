package vn.hoctoanai.core.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Luật DDD: hướng phụ thuộc giữa các tầng và quy ước đặt tên.
 *
 * <p>Chép từ {@code LMS_hohulili@34c3f0f2:backend/src/test/java/com/example/lms/architecture/DddArchitectureTest.java}
 * (MIT): đổi gói; bỏ hậu tố {@code V2}/{@code V3} của LMS; luật tên adapter trước chỉ là ghi chú nay được kiểm;
 * domain → application và application → web phủ cả tầng thay vì chỉ {@code domain.model} / {@code application.usecase}.
 * Thêm: controller chỉ dùng DTO; mọi {@code @Entity} phải tên {@code *JpaEntity} (LMS chỉ kiểm vị trí của lớp đã đúng tên).
 */
@DisplayName("Luật DDD")
class DddArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("vn.hoctoanai.core");
    }

    @Nested
    @DisplayName("Hướng phụ thuộc giữa các tầng")
    class LayerDependencyRules {

        @Test
        @DisplayName("Domain không phụ thuộc infrastructure")
        void domainShouldNotDependOnInfrastructure() {
            noClasses().that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .check(importedClasses);
        }

        @Test
        @DisplayName("Domain không phụ thuộc application")
        void domainShouldNotDependOnApplication() {
            noClasses().that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..application..")
                .check(importedClasses);
        }

        @Test
        @DisplayName("Application không phụ thuộc tầng web")
        void applicationShouldNotDependOnWeb() {
            noClasses().that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure.web..")
                .check(importedClasses);
        }

    }

    @Nested
    @DisplayName("Quy ước đặt tên")
    class NamingConventionRules {

        @Test
        @DisplayName("Lớp use case kết thúc bằng UseCase")
        void useCaseShouldHaveProperName() {
            classes().that()
                .resideInAPackage("..application.usecase..")
                .and()
                .areTopLevelClasses() // bỏ qua record Command lồng bên trong
                .should()
                .haveSimpleNameEndingWith("UseCase")
                .check(importedClasses);
        }

        @Test
        @DisplayName("Controller kết thúc bằng Controller")
        void controllersShouldHaveProperName() {
            classes().that()
                .resideInAPackage("..infrastructure.web..")
                .and()
                .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should()
                .haveSimpleNameEndingWith("Controller")
                .check(importedClasses);
        }

        @Test
        @DisplayName("Adapter hiện thực repository của domain kết thúc bằng Adapter")
        void repositoryAdaptersShouldHaveProperName() {
            classes().that()
                .resideInAPackage("..infrastructure.persistence..")
                .and()
                .implement(resideInAPackage("..domain.repository.."))
                .should()
                .haveSimpleNameEndingWith("Adapter")
                .check(importedClasses);
        }

    }

    @Nested
    @DisplayName("Biên web")
    class WebBoundaryRules {

        @Test
        @DisplayName("Controller chỉ làm việc với DTO: không phụ thuộc domain hay persistence")
        void controllersOnlyUseDtos() {
            noClasses().that()
                .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..domain..", "..infrastructure.persistence..")
                .because("controller nhận / trả record DTO của application; ánh xạ domain ↔ DTO nằm ở use case")
                .check(importedClasses);
        }

    }

    @Nested
    @DisplayName("Entity JPA")
    class JpaEntityRules {

        @Test
        @DisplayName("Mọi @Entity tên *JpaEntity và nằm trong infrastructure.persistence.entity")
        void jpaEntitiesShouldBeNamedAndPlaced() {
            classes().that()
                .areAnnotatedWith("jakarta.persistence.Entity")
                .should()
                .haveSimpleNameEndingWith("JpaEntity")
                .andShould()
                .resideInAPackage("..infrastructure.persistence.entity..")
                .because("JpaRepository<XJpaEntity, UUID>: tên phân biệt entity với model domain")
                .check(importedClasses);
        }

    }

}
