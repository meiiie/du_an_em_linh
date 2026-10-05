package vn.hoctapcanman.core.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Luật kiến trúc của {@code services/core}, dùng chung cho test trên mã thật ({@link CleanArchitectureTest},
 * {@link DddArchitectureTest}) và test tự kiểm trên lớp mẫu ({@link KienTrucRulesTuKiemTest}): mỗi luật phải bắt được
 * lớp vi phạm cố ý của nó, nên luật gõ sai gói không thể «xanh lặng lẽ».
 *
 * <p>Gốc từ {@code LMS_hohulili@34c3f0f2:backend/src/test/java/com/example/lms/architecture/} (MIT), mở rộng qua review
 * #65: phủ cả tầng thay vì chỉ {@code domain.model} / {@code application.usecase}; chữ ký controller chỉ dùng record
 * DTO; repository Spring Data chỉ quản lý {@code *JpaEntity}.
 */
final class KienTrucRules {

    private static final String REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController";
    private static final String REQUEST_MAPPING = "org.springframework.web.bind.annotation.RequestMapping";
    private static final String REQUEST_BODY = "org.springframework.web.bind.annotation.RequestBody";
    private static final String SPRING_DATA_REPOSITORY = "org.springframework.data.repository.Repository";
    private static final String JPA_ENTITY = "jakarta.persistence.Entity";

    /** Kiểu bọc ngoài payload; bóc ra để kiểm kiểu bên trong. */
    private static final Set<String> KIEU_BOC = Set.of(
        "org.springframework.http.ResponseEntity",
        "org.springframework.http.HttpEntity",
        "java.util.List",
        "java.util.Set",
        "java.util.Collection",
        "java.util.Optional");

    /** Không phải payload: không có thân, hoặc luồng SSE trạng thái gia sư (ADR 010). */
    private static final Set<String> KHONG_LA_PAYLOAD = Set.of(
        "void",
        "java.lang.Void",
        "org.springframework.web.servlet.mvc.method.annotation.SseEmitter");

    // Ngoại lệ CQRS của LMS: use case đọc (Get*, kể cả lớp lồng) được đọc thẳng persistence.
    private static final DescribedPredicate<JavaClass> USE_CASE_DOC = new DescribedPredicate<>("use case đọc Get*") {
        @Override
        public boolean test(JavaClass javaClass) {
            JavaClass top = javaClass;
            while (top.getEnclosingClass().isPresent()) {
                top = top.getEnclosingClass().get();
            }
            return javaClass.getPackageName().contains(".application.usecase") && top.getSimpleName().startsWith("Get");
        }
    };

    // ---- Clean Architecture: hướng phụ thuộc -------------------------------------------------------------------

    static final ArchRule DOMAIN_KHONG_PHU_THUOC_INFRASTRUCTURE = noClasses().that()
        .resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
        .as("Domain không phụ thuộc infrastructure")
        .because("model, port, dịch vụ, sự kiện của domain là Java thuần");

    static final ArchRule DOMAIN_KHONG_PHU_THUOC_APPLICATION = noClasses().that()
        .resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("..application..")
        .as("Domain không phụ thuộc application");

    static final ArchRule DOMAIN_KHONG_PHU_THUOC_SPRING = noClasses().that()
        .resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("org.springframework..")
        .as("Domain không phụ thuộc Spring")
        .because("domain độc lập framework; nối bean ở infrastructure");

    static final ArchRule DOMAIN_KHONG_PHU_THUOC_JPA = noClasses().that()
        .resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
        .as("Domain không phụ thuộc JPA / Hibernate")
        .because("entity là *JpaEntity ở infrastructure; model domain không bao giờ là entity");

    static final ArchRule APPLICATION_KHONG_PHU_THUOC_INFRASTRUCTURE = noClasses().that()
        .resideInAPackage("..application..").and(not(USE_CASE_DOC))
        .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
        .as("Application không phụ thuộc infrastructure (trừ use case đọc Get*)")
        .because("port, dịch vụ, DTO, use case ghi chỉ dùng domain và port; adapter ở infrastructure hiện thực port");

    static final ArchRule USE_CASE_DOC_CHI_DUNG_PERSISTENCE = noClasses().that(USE_CASE_DOC)
        .should().dependOnClassesThat(resideInAPackage("..infrastructure..").and(not(resideInAPackage("..infrastructure.persistence.."))))
        .as("Use case đọc Get* chỉ được dùng infrastructure.persistence")
        .because("ngoại lệ CQRS chỉ để đọc thẳng CSDL; client, web, cấu hình vẫn qua port");

    static final ArchRule APPLICATION_KHONG_PHU_THUOC_WEB = noClasses().that()
        .resideInAPackage("..application..")
        .should().dependOnClassesThat().resideInAPackage("..infrastructure.web..")
        .as("Application không phụ thuộc tầng web");

    // ---- DDD: đặt tên, biên web, entity, repository ------------------------------------------------------------

    static final ArchRule USE_CASE_DAT_TEN = classes().that()
        .resideInAPackage("..application.usecase..").and().areTopLevelClasses().and().doNotHaveSimpleName("package-info")
        .should().haveSimpleNameEndingWith("UseCase")
        .as("Lớp use case kết thúc bằng UseCase");

    static final ArchRule CONTROLLER_DUNG_CHO_DUNG_TEN = classes().that()
        .areAnnotatedWith(REST_CONTROLLER)
        .should().resideInAPackage("..infrastructure.web..")
        .andShould().haveSimpleNameEndingWith("Controller")
        .as("Mọi @RestController nằm trong infrastructure.web và kết thúc bằng Controller")
        .because("application không được dính Spring Web");

    static final ArchRule CONTROLLER_KHONG_PHU_THUOC_DOMAIN_PERSISTENCE = noClasses().that()
        .areAnnotatedWith(REST_CONTROLLER)
        .should().dependOnClassesThat().resideInAnyPackage("..domain..", "..infrastructure.persistence..")
        .as("Controller không phụ thuộc domain hay persistence")
        .because("ánh xạ domain ↔ DTO nằm ở use case");

    static final ArchRule CONTROLLER_CHI_DUNG_RECORD_DTO = methods().that()
        .areDeclaredInClassesThat().areAnnotatedWith(REST_CONTROLLER)
        .and(laEndpoint())
        .should(chiNhanVaTraRecordDto())
        .as("Endpoint chỉ nhận (@RequestBody) và trả record trong application.dto")
        .because("controller nhận / trả record DTO (rule spring-core); bóc ResponseEntity, List, Optional… để kiểm kiểu bên trong");

    static final ArchRule ADAPTER_DAT_TEN = classes().that()
        .resideInAPackage("..infrastructure.persistence..").and().implement(resideInAPackage("..domain.repository.."))
        .should().haveSimpleNameEndingWith("Adapter")
        .as("Adapter hiện thực repository của domain kết thúc bằng Adapter");

    static final ArchRule ENTITY_DAT_TEN_DUNG_CHO = classes().that()
        .areAnnotatedWith(JPA_ENTITY)
        .should().haveSimpleNameEndingWith("JpaEntity")
        .andShould().resideInAPackage("..infrastructure.persistence.entity..")
        .as("Mọi @Entity tên *JpaEntity và nằm trong infrastructure.persistence.entity");

    static final ArchRule REPOSITORY_CHI_QUAN_LY_JPA_ENTITY = classes().that()
        .areInterfaces().and().areAssignableTo(SPRING_DATA_REPOSITORY)
        .should(chiQuanLyJpaEntity())
        .as("Repository Spring Data chỉ quản lý @Entity *JpaEntity trong infrastructure.persistence.entity")
        .because("JpaRepository<DomainModel, …> làm hỏng khởi động: «Not a managed type» (bài học LMS)");

    // ---- Đề cho học sinh không mang lời giải (FR-006, ADR 003, T015) ------------------------------------------

    static final ArchRule DTO_HOC_SINH_KHONG_MANG_LOI_GIAI = classes().that().resideInAPackage("..application.dto.hocsinh..")
        .should().onlyDependOnClassesThat().resideInAnyPackage("..application.dto.hocsinh..", "java..", "org.jspecify..")
        .as("DTO của học sinh chỉ dùng kiểu Java và DTO học sinh khác")
        .because("FR-006, ADR 003: đề cho học sinh khi đang làm không bao giờ mang lời giải, đáp án hay dữ kiện bảo vệ; "
            + "DTO không tham chiếu được Solution hay bất kỳ kiểu domain nào");

    // ---- Lời giải sau khi nộp chỉ qua một cửa (FR-006, ADR 003, T020) -------------------------------------------

    private static final DescribedPredicate<JavaClass> CONG_LOI_GIAI = new DescribedPredicate<>("cổng LoiGiaiSauKhiNop của nội dung") {
        @Override
        public boolean test(JavaClass c) {
            return c.getSimpleName().equals("LoiGiaiSauKhiNop") && c.getPackageName().endsWith(".content.application.port");
        }
    };

    private static final DescribedPredicate<JavaClass> CUA_MO_LOI_GIAI = new DescribedPredicate<>("MoLoiGiai của practice") {
        @Override
        public boolean test(JavaClass c) {
            return c.getSimpleName().equals("MoLoiGiai") && c.getPackageName().endsWith(".practice.application.service");
        }
    };

    private static final DescribedPredicate<JavaClass> HIEN_THUC_CONG_LOI_GIAI = new DescribedPredicate<>("lớp hiện thực cổng") {
        @Override
        public boolean test(JavaClass c) {
            return c.getSimpleName().equals("LoiGiaiSauKhiNopService") && c.getPackageName().endsWith(".content.application.service");
        }
    };

    static final ArchRule LOI_GIAI_CHI_QUA_CUA_MO = noClasses().that(not(CONG_LOI_GIAI))
        .and(not(HIEN_THUC_CONG_LOI_GIAI)).and(not(CUA_MO_LOI_GIAI))
        .should().dependOnClassesThat(CONG_LOI_GIAI)
        .as("Ngoài lớp hiện thực cổng, chỉ MoLoiGiai của practice dùng cổng lời giải sau khi nộp")
        .because("FR-006, ADR 003: cổng chỉ kiểm phát hành và phiên bản; MoLoiGiai kiểm thêm đã nộp, không làm lại, cờ lớp. "
            + "Nơi gọi khác là đường tắt tới lời giải");

    // ---- Ranh giới module (research R1, #111) -----------------------------------------------------------------

    /** Chỉ gói của dự án (mã thật và lớp mẫu) mới chia module. */
    private static final String GOC_DU_AN = "vn.hoctapcanman.";
    /** Tầng của một module; gói gốc của module là phần đứng trước tầng đầu tiên. */
    private static final Set<String> TANG = Set.of("domain", "application", "infrastructure");
    /** Phần một module mở cho module khác. */
    private static final List<String> CONG_MO = List.of("application.port", "application.dto", "application.exception");

    static final ArchRule MODULE_CHI_GOI_NHAU_QUA_CONG = classes()
        .should(chiGoiModuleKhacQuaCong())
        .as("Module chỉ dùng module khác qua application.port, application.dto, application.exception")
        .because("research R1: module không đọc domain, repository, use case hay persistence của module khác; "
            + "shared là phần dùng chung");

    static final List<ArchRule> CLEAN = List.of(
        DOMAIN_KHONG_PHU_THUOC_INFRASTRUCTURE,
        DOMAIN_KHONG_PHU_THUOC_APPLICATION,
        DOMAIN_KHONG_PHU_THUOC_SPRING,
        DOMAIN_KHONG_PHU_THUOC_JPA,
        APPLICATION_KHONG_PHU_THUOC_INFRASTRUCTURE,
        USE_CASE_DOC_CHI_DUNG_PERSISTENCE,
        APPLICATION_KHONG_PHU_THUOC_WEB);

    static final List<ArchRule> DDD = List.of(
        USE_CASE_DAT_TEN,
        CONTROLLER_DUNG_CHO_DUNG_TEN,
        CONTROLLER_KHONG_PHU_THUOC_DOMAIN_PERSISTENCE,
        CONTROLLER_CHI_DUNG_RECORD_DTO,
        ADAPTER_DAT_TEN,
        ENTITY_DAT_TEN_DUNG_CHO,
        REPOSITORY_CHI_QUAN_LY_JPA_ENTITY,
        DTO_HOC_SINH_KHONG_MANG_LOI_GIAI,
        LOI_GIAI_CHI_QUA_CUA_MO,
        MODULE_CHI_GOI_NHAU_QUA_CONG);

    static final List<ArchRule> TAT_CA = concat(CLEAN, DDD);

    private KienTrucRules() {}

    private static DescribedPredicate<JavaMethod> laEndpoint() {
        return new DescribedPredicate<>("là endpoint (@GetMapping, @PostMapping… hoặc @RequestMapping)") {
            @Override
            public boolean test(JavaMethod method) {
                return method.isAnnotatedWith(REQUEST_MAPPING) || method.isMetaAnnotatedWith(REQUEST_MAPPING);
            }
        };
    }

    private static ArchCondition<JavaMethod> chiNhanVaTraRecordDto() {
        return new ArchCondition<>("chỉ nhận và trả record trong application.dto") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                List<JavaClass> payloads = new ArrayList<>();
                boc(method.getReturnType(), payloads);
                for (JavaParameter parameter : method.getParameters()) {
                    if (parameter.isAnnotatedWith(REQUEST_BODY)) {
                        boc(parameter.getType(), payloads);
                    }
                }
                for (JavaClass payload : payloads) {
                    if (!laRecordDto(payload)) {
                        String message = "%s dùng %s — không phải record trong application.dto"
                            .formatted(method.getFullName(), payload.getName());
                        events.add(SimpleConditionEvent.violated(method, message));
                    }
                }
            }
        };
    }

    private static void boc(JavaType type, List<JavaClass> payloads) {
        JavaClass raw = type.toErasure();
        if (type instanceof JavaParameterizedType parameterized && KIEU_BOC.contains(raw.getName())) {
            for (JavaType argument : parameterized.getActualTypeArguments()) {
                boc(argument, payloads);
            }
        } else if (!KHONG_LA_PAYLOAD.contains(raw.getName())) {
            payloads.add(raw);
        }
    }

    private static boolean laRecordDto(JavaClass javaClass) {
        return javaClass.isRecord() && javaClass.getPackageName().contains(".application.dto");
    }

    private static ArchCondition<JavaClass> chiQuanLyJpaEntity() {
        return new ArchCondition<>("chỉ quản lý @Entity *JpaEntity trong infrastructure.persistence.entity") {
            @Override
            public void check(JavaClass repository, ConditionEvents events) {
                for (JavaType supertype : repository.getInterfaces()) {
                    if (!(supertype instanceof JavaParameterizedType parameterized)
                        || !parameterized.toErasure().isAssignableTo(SPRING_DATA_REPOSITORY)
                        || parameterized.getActualTypeArguments().isEmpty()) {
                        continue;
                    }
                    JavaClass entity = parameterized.getActualTypeArguments().getFirst().toErasure();
                    boolean dung = entity.isAnnotatedWith(JPA_ENTITY)
                        && entity.getSimpleName().endsWith("JpaEntity")
                        && entity.getPackageName().contains(".infrastructure.persistence.entity");
                    if (!dung) {
                        String message = "%s quản lý %s — phải là @Entity tên *JpaEntity trong infrastructure.persistence.entity"
                            .formatted(repository.getName(), entity.getName());
                        events.add(SimpleConditionEvent.violated(repository, message));
                    }
                }
            }
        };
    }

    /**
     * Gói gốc của module chứa gói này (phần trước tầng đầu tiên); rỗng nếu gói không thuộc tầng nào của một module, hay
     * nằm ngoài dự án: thư viện như {@code org.springframework.data.domain} không phải module.
     */
    static Optional<String> moduleCua(String goi) {
        if (!goi.startsWith(GOC_DU_AN)) {
            return Optional.empty();
        }
        String[] phan = goi.split("\\.");
        for (int i = 1; i < phan.length; i++) {
            if (TANG.contains(phan[i])) {
                return Optional.of(String.join(".", Arrays.copyOfRange(phan, 0, i)));
            }
        }
        return Optional.empty();
    }

    private static ArchCondition<JavaClass> chiGoiModuleKhacQuaCong() {
        return new ArchCondition<>("chỉ phụ thuộc module khác qua application.port, application.dto, application.exception") {
            @Override
            public void check(JavaClass lop, ConditionEvents events) {
                Optional<String> cuaLop = moduleCua(lop.getPackageName());
                if (cuaLop.isEmpty()) {
                    return;
                }
                for (Dependency phuThuoc : lop.getDirectDependenciesFromSelf()) {
                    JavaClass dich = phuThuoc.getTargetClass();
                    String goiDich = (dich.isArray() ? dich.getBaseComponentType() : dich).getPackageName();
                    Optional<String> cuaDich = moduleCua(goiDich);
                    if (cuaDich.isEmpty() || cuaDich.equals(cuaLop) || cuaDich.get().endsWith(".shared")) {
                        continue;
                    }
                    String phanTrong = goiDich.substring(cuaDich.get().length() + 1);
                    boolean quaCong = CONG_MO.stream().anyMatch(c -> phanTrong.equals(c) || phanTrong.startsWith(c + "."));
                    if (!quaCong) {
                        events.add(SimpleConditionEvent.violated(phuThuoc, phuThuoc.getDescription()));
                    }
                }
            }
        };
    }

    private static List<ArchRule> concat(List<ArchRule> first, List<ArchRule> second) {
        List<ArchRule> all = new ArrayList<>(first);
        all.addAll(second);
        return List.copyOf(all);
    }
}
