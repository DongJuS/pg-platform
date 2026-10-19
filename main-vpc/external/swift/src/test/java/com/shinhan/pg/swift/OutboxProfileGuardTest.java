package com.shinhan.pg.swift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.common.outbox.OutboxStore;
import com.shinhan.pg.swift.adapter.out.persistence.outbox.SwiftOutboxStore;
import com.shinhan.pg.swift.config.SwiftSkeletonConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * [WO-4] OutboxStore bean이 skeleton 프로필 외에서 등록되지 않음을 고정한다.
 * 인프라 스텁 패턴: libs/arch-rules testFixtures의
 * SkeletonWiringAssertions.InfrastructureStubConfiguration을 참조해 로컬에 동일하게 둔다
 * (원본은 package-private이라 직접 참조 불가).
 */
class OutboxProfileGuardTest {

    @Test
    void defaultProfileRegistersNoOutboxStore() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(SwiftSkeletonConfiguration.class);
            context.refresh();
            assertTrue(
                    context.getBeansOfType(OutboxStore.class).isEmpty(),
                    "OutboxStore must stay unregistered without the skeleton profile");
        }
    }

    @Test
    void awsProfileRegistersNoOutboxStore() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("aws");
            context.register(SwiftSkeletonConfiguration.class);
            context.refresh();
            assertTrue(
                    context.getBeansOfType(OutboxStore.class).isEmpty(),
                    "OutboxStore must stay unregistered under the aws profile");
        }
    }

    @Test
    void skeletonProfileRegistersExactlyOneServiceOutboxStore() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("skeleton");
            context.register(SwiftSkeletonConfiguration.class);
            context.register(InfrastructureStubConfiguration.class);
            context.refresh();
            var beans = context.getBeansOfType(OutboxStore.class);
            assertEquals(1, beans.size(), "skeleton profile must register exactly one OutboxStore");
            assertInstanceOf(
                    SwiftOutboxStore.class,
                    beans.values().iterator().next(),
                    "the single OutboxStore must be the service-local SwiftOutboxStore");
        }
    }

    /** SkeletonWiringAssertions.InfrastructureStubConfiguration과 동일한 mock 기반 DataSource 스텁. */
    @org.springframework.context.annotation.Configuration
    static class InfrastructureStubConfiguration {
        @org.springframework.context.annotation.Bean
        javax.sql.DataSource dataSource() {
            return org.mockito.Mockito.mock(javax.sql.DataSource.class);
        }
    }
}
