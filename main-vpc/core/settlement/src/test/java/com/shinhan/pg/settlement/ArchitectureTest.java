package com.shinhan.pg.settlement;
import com.shinhan.pg.common.archrules.PlatformRules;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
class ArchitectureTest {
    @Test void boundaries() {
        var classes = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages("com.shinhan.pg.settlement");
        PlatformRules.check(classes, "com.shinhan.pg.settlement", true);
    }
}
