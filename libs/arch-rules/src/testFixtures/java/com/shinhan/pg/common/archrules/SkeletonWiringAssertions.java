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

    /** 구현 단계의 서비스 조립. DataSource·RedisConnectionFactory 스텁을 함께 등록한다.
     *  스텁은 bean 생성까지만 하며 실제 연결을 사용하지 않는다. */
    public static void assemblesWithInfrastructure(Class<?> configuration, Class<?>... inputPorts) {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("skeleton");
            context.register(configuration);
            context.register(InfrastructureStubConfiguration.class);
            context.refresh();
            for (var inputPort : inputPorts)
                assertEquals(
                        1,
                        context.getBeansOfType(inputPort).size(),
                        inputPort.getName() + " must assemble exactly once");
        }
    }

    @org.springframework.context.annotation.Configuration
    static class InfrastructureStubConfiguration {
        @org.springframework.context.annotation.Bean
        javax.sql.DataSource dataSource() {
            return org.mockito.Mockito.mock(javax.sql.DataSource.class);
        }

        @org.springframework.context.annotation.Bean
        org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate() {
            return new org.springframework.data.redis.core.StringRedisTemplate(
                    org.mockito.Mockito.mock(
                            org.springframework.data.redis.connection.RedisConnectionFactory.class));
        }
    }
}
