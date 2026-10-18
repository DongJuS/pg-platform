package com.shinhan.pg.common.archrules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.*;

import java.util.*;

public final class PlatformRules {
    private PlatformRules() {}

    private static final String MONEY = "com.shinhan.pg.common.commonmoney.Money";

    private static boolean in(JavaClass type, String part) {
        return type.getPackageName().contains("." + part + ".")
                || type.getPackageName().endsWith("." + part);
    }

    private static String adapterSlice(JavaClass type) {
        var name = type.getPackageName();
        var marker = ".adapter.";
        var index = name.indexOf(marker);
        if (index < 0) return "";
        var segments = name.substring(index + marker.length()).split("[.]");
        return segments.length < 2 ? name : segments[0] + "." + segments[1];
    }

    private static boolean network(String name) {
        return name.startsWith("java.net.http.")
                || name.equals("java.net.Socket")
                || name.endsWith("URLConnection")
                || name.startsWith("okhttp3.")
                || name.startsWith("org.apache.http.")
                || name.startsWith("org.apache.hc.")
                || name.startsWith("feign.")
                || name.startsWith("org.springframework.web.client.Rest")
                || name.startsWith("org.springframework.web.reactive.function.client.WebClient");
    }

    public static ArchRule rule(String code, String root, boolean mainVpc) {
        return classes()
                .should(
                        new ArchCondition<JavaClass>(code + " platform boundary") {
                            @Override
                            public void check(JavaClass origin, ConditionEvents events) {
                                if (code.equals("R6")) {
                                    if (!origin.getName().equals(MONEY)
                                            && origin.getAllRawSuperclasses().stream()
                                                    .anyMatch(s -> s.getName().equals(MONEY)))
                                        events.add(
                                                SimpleConditionEvent.violated(
                                                        origin,
                                                        "R6 Money must not be inherited: "
                                                                + origin.getName()));
                                    return;
                                }
                                for (var dependency : origin.getDirectDependenciesFromSelf()) {
                                    var target = dependency.getTargetClass();
                                    var name = target.getName();
                                    boolean violation =
                                            switch (code) {
                                                case "R1" ->
                                                        in(origin, "domain")
                                                                && (name.startsWith(
                                                                                "org.springframework.")
                                                                        || name.startsWith(
                                                                                "jakarta.persistence.")
                                                                        || name.startsWith(
                                                                                "javax.persistence.")
                                                                        || name.startsWith(
                                                                                "org.apache.kafka."));
                                                case "R2" ->
                                                        in(origin, "domain")
                                                                && (in(target, "application")
                                                                        || in(target, "adapter"));
                                                case "R3" ->
                                                        in(origin, "application")
                                                                && in(target, "adapter");
                                                case "R4" ->
                                                        in(origin, "adapter")
                                                                && in(target, "adapter")
                                                                && !adapterSlice(origin)
                                                                        .equals(
                                                                                adapterSlice(
                                                                                        target));
                                                case "R5" ->
                                                        name.startsWith("com.shinhan.pg.")
                                                                && !name.startsWith(
                                                                        "com.shinhan.pg.common.")
                                                                && !(target.getPackageName()
                                                                                .equals(root)
                                                                        || target.getPackageName()
                                                                                .startsWith(
                                                                                        root
                                                                                                + "."));
                                                case "R7" ->
                                                        mainVpc
                                                                && network(name)
                                                                && !allowedInternalClient(
                                                                        origin, root);
                                                default ->
                                                        throw new IllegalArgumentException(
                                                                "Unknown rule " + code);
                                            };
                                    if (violation)
                                        events.add(
                                                SimpleConditionEvent.violated(
                                                        origin,
                                                        code + ": " + dependency.getDescription()));
                                }
                            }
                        })
                .allowEmptyShould(true);
    }

    private static boolean allowedInternalClient(JavaClass origin, String root) {
        // Only callers inside a named internal-service adapter may use an HTTP client.
        // Runtime destination restrictions are enforced by the network/SG layer, not by bytecode.
        var internal =
                Set.of(
                        "auth",
                        "account",
                        "ledger",
                        "transfer",
                        "payment",
                        "settlement",
                        "swift",
                        "wealthreport",
                        "aml",
                        "aigateway",
                        "fep");
        return internal.stream()
                .anyMatch(
                        service ->
                                origin.getPackageName()
                                                .startsWith(root + ".adapter.out." + service + ".")
                                        || origin.getPackageName()
                                                .equals(root + ".adapter.out." + service));
    }

    public static void check(JavaClasses classes, String root, boolean mainVpc) {
        for (var code : List.of("R1", "R2", "R3", "R4", "R5", "R6", "R7"))
            rule(code, root, mainVpc).check(classes);
    }
}
