package com.shinhan.pg.common.archrules;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** 기본 비활성화와 전체 유스케이스 조립을 실제 Spring context로 확인한다. */
public final class SkeletonWiringAssertions {
    private SkeletonWiringAssertions() {}

    public static void inactive(Class<?> configuration, Class<?>... inputPorts) {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(configuration);
            context.refresh();
            for (var inputPort : inputPorts)
                assertTrue(
                        context.getBeansOfType(inputPort).isEmpty(),
                        inputPort.getName() + " must stay inactive by default");
        }
    }

    public static void assembles(Class<?> configuration, Class<?>... inputPorts) {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("skeleton");
            context.register(configuration);
            context.refresh();
            for (var inputPort : inputPorts)
                assertEquals(
                        1,
                        context.getBeansOfType(inputPort).size(),
                        inputPort.getName() + " must assemble exactly once");
        }
    }
}
