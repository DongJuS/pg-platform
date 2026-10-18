package com.shinhan.pg.common.archrules;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class RuleDetectionTest {
    @TempDir Path temp;
    private void rejects(String rule,Map<String,String> sources) throws Exception {
        var args=new ArrayList<String>(); args.add("-d"); args.add(temp.toString());
        for (var entry:sources.entrySet()) {
            var file=temp.resolve(entry.getKey().replace('.','/')+".java"); Files.createDirectories(file.getParent());
            Files.writeString(file,entry.getValue()); args.add(file.toString());
        }
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,args.toArray(String[]::new)));
        var classes=new ClassFileImporter().importPath(temp);
        var result=PlatformRules.rule(rule,"com.shinhan.pg.transfer",true).evaluate(classes);
        assertTrue(result.hasViolation(),"Deliberate "+rule+" violation must be detected");
        System.out.println("EXPECTED VIOLATION DETECTED: "+rule+" "+result.getFailureReport());
    }
    @Test void R1DetectsFrameworkDependencyInDomain() throws Exception {
        rejects("R1",Map.of("org.springframework.stereotype.Component","package org.springframework.stereotype; public @interface Component {}",
                "com.shinhan.pg.transfer.domain.Bad","package com.shinhan.pg.transfer.domain; @org.springframework.stereotype.Component public class Bad {}"));
    }
    @Test void R5DetectsAnotherServiceImport() throws Exception {
        rejects("R5",Map.of("com.shinhan.pg.account.Foreign","package com.shinhan.pg.account; public class Foreign {}",
                "com.shinhan.pg.transfer.domain.Bad","package com.shinhan.pg.transfer.domain; import com.shinhan.pg.account.Foreign; public class Bad { Foreign other; }"));
    }
    @Test void R6DetectsSubclassIfCompilerFinalGuardWereRemoved() throws Exception {
        // Isolated fixture shadow, never the production Money class. Production Money is final.
        rejects("R6",Map.of("com.shinhan.pg.common.commonmoney.Money","package com.shinhan.pg.common.commonmoney; public class Money {}",
                "com.shinhan.pg.transfer.domain.Bad","package com.shinhan.pg.transfer.domain; public class Bad extends com.shinhan.pg.common.commonmoney.Money {}"));
    }
    @Test void R7DetectsDirectExternalNetworkClient() throws Exception {
        rejects("R7",Map.of("com.shinhan.pg.transfer.adapter.out.pg.Bad","package com.shinhan.pg.transfer.adapter.out.pg; public class Bad { java.net.http.HttpClient client; }"));
    }
}
