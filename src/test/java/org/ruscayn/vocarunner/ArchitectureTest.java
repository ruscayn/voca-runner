package org.ruscayn.vocarunner;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("구조 검사 : 계층 · 기능 간 의존 방향")
class ArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("org.ruscayn.vocarunner");

    @Test
    @DisplayName("도메인은 Spring · JPA · Hibernate에 의존하지 않는다")
    void domainDoesNotDependOnFrameworks() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..")
                .because("업무 규칙은 프레임워크 없이 순수 자바로 둔다")
                .check(CLASSES);
    }

    @Test
    @DisplayName("안쪽 계층은 바깥쪽 계층을 모른다 (adapter → application → domain)")
    void innerLayersDoNotDependOnOuterLayers() {
        layeredArchitecture().consideringOnlyDependenciesInLayers()
                .layer("adapter").definedBy("..adapter..")
                .layer("application").definedBy("..application..")
                .layer("domain").definedBy("..domain..")
                .whereLayer("adapter").mayNotBeAccessedByAnyLayer()
                .whereLayer("application").mayOnlyBeAccessedByLayers("adapter")
                .whereLayer("domain").mayOnlyBeAccessedByLayers("application", "adapter")
                .check(CLASSES);
    }

    @Test
    @DisplayName("단어 기능은 시험 기능에 의존하지 않는다 (exam → word 한 방향)")
    void wordDoesNotDependOnExam() {
        noClasses().that().resideInAPackage("org.ruscayn.vocarunner.word..")
                .should().dependOnClassesThat().resideInAPackage("org.ruscayn.vocarunner.exam..")
                .because("단어 출제 목록은 시험과 무관하게 재사용할 수 있어야 한다")
                .check(CLASSES);
    }

    @Test
    @DisplayName("어댑터는 서비스를 직접 부르지 않고 포트를 거친다")
    void adaptersUsePortsNotServices() {
        noClasses().that().resideInAPackage("..adapter..")
                .should().dependOnClassesThat().resideInAPackage("..application.service..")
                .because("어댑터는 들어오는 포트(유스케이스) · 나가는 포트에만 의존한다")
                .check(CLASSES);
    }

}
