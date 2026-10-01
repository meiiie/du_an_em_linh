package vn.hoctoanai.core.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JSpecify: {@code @NullMarked} ở gói không lan sang gói con, nên mỗi gói cần {@code package-info.java} riêng.
 */
@DisplayName("Mọi gói có @NullMarked")
class NullMarkedPackagesTest {

	private static final ArchCondition<JavaClass> IN_NULL_MARKED_PACKAGE = new ArchCondition<>("nằm trong gói có @NullMarked") {
		@Override
		public void check(JavaClass javaClass, ConditionEvents events) {
			if (!javaClass.getPackage().isAnnotatedWith(NullMarked.class)) {
				String message = "gói %s thiếu @NullMarked trong package-info.java (lớp %s)"
					.formatted(javaClass.getPackageName(), javaClass.getName());
				events.add(SimpleConditionEvent.violated(javaClass, message));
			}
		}
	};

	@Test
	void everyPackageIsNullMarked() {
		classes().should(IN_NULL_MARKED_PACKAGE)
			.check(new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackages("vn.hoctoanai.core"));
	}

}
