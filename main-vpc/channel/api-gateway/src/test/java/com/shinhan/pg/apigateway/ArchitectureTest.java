package com.shinhan.pg.apigateway;
import com.shinhan.pg.common.archrules.PlatformRules;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
class ArchitectureTest {
    @Test void boundariesWithoutHexagonalLayers() {
        var classes=new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages("com.shinhan.pg.apigateway");
        for (var code:java.util.List.of("R5","R6","R7")) PlatformRules.rule(code,"com.shinhan.pg.apigateway",true).check(classes);
    }
}
