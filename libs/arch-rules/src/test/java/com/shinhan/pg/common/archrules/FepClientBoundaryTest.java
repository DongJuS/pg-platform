package com.shinhan.pg.common.archrules;

import static org.junit.jupiter.api.Assertions.*;

import com.tngtech.archunit.core.importer.ClassFileImporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

import javax.tools.ToolProvider;

class FepClientBoundaryTest {
    @TempDir Path temp;

    private boolean violates(String folder) throws Exception {
        var type = "com.shinhan.pg.swift.adapter.out." + folder + ".Client";
        var file = temp.resolve(type.replace('.', '/') + ".java");
        Files.createDirectories(file.getParent());
        Files.writeString(
                file,
                "package com.shinhan.pg.swift.adapter.out."
                        + folder
                        + "; public class Client { java.net.http.HttpClient client; }");
        assertEquals(
                0,
                ToolProvider.getSystemJavaCompiler()
                        .run(null, null, null, "-d", temp.toString(), file.toString()));
        return PlatformRules.rule("R7", "com.shinhan.pg.swift", true)
                .evaluate(new ClassFileImporter().importPath(temp))
                .hasViolation();
    }

    @Test
    void permitsAnInternalFepClientAtTheCallerOwnedAdapter() throws Exception {
        assertFalse(violates("fep"));
    }

    @Test
    void continuesToRejectADirectExternalInstitutionClient() throws Exception {
        assertTrue(violates("institutions"));
    }
}
