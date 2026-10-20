package com.shinhan.pg.swift.config;

import com.shinhan.pg.common.outbox.OutboxStore;
import com.shinhan.pg.swift.adapter.in.batch.RetrySwiftBatchSkeleton;
import com.shinhan.pg.swift.adapter.in.kafka.SubmitSwiftConsumerSkeleton;
import com.shinhan.pg.swift.adapter.out.fep.FepSwiftRestSkeleton;
import com.shinhan.pg.swift.adapter.out.persistence.outbox.SwiftOutboxStore;
import com.shinhan.pg.swift.application.port.in.RetrySwiftUseCase;
import com.shinhan.pg.swift.application.port.in.SubmitSwiftUseCase;
import com.shinhan.pg.swift.application.port.out.FepSwiftPort;
import com.shinhan.pg.swift.application.service.SwiftApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/** 개발용 의존성 조립. 기본/local/aws에는 업무 스켈레톤 bean을 등록하지 않는다. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class SwiftSkeletonConfiguration {
    @Bean
    public FepSwiftPort fepSwiftPort(org.springframework.core.env.Environment env) {
        return env.getProperty("pg.swift.enabled", Boolean.class, false)
                ? new com.shinhan.pg.swift.adapter.out.fep.FepSwiftRestAdapter(env)
                : new FepSwiftRestSkeleton();
    }

    @Bean
    public com.shinhan.pg.swift.application.port.out.SwiftSubmissionStore swiftSubmissionStore(
            DataSource ds, OutboxStore outbox, org.springframework.core.env.Environment env) {
        return new com.shinhan.pg.swift.adapter.out.persistence.SwiftSubmissionJdbcAdapter(
                ds, outbox, env.getProperty("pg.swift.payload-key", ""));
    }

    @Bean
    public SwiftApplicationService swiftApplicationService(
            FepSwiftPort fepSwiftPort,
            com.shinhan.pg.swift.application.port.out.SwiftSubmissionStore store,
            org.springframework.core.env.Environment env) {
        return new SwiftApplicationService(
                fepSwiftPort,
                store,
                new com.shinhan.pg.swift.adapter.out.transfer.TransferInstructionRestAdapter(env),
                env.getProperty("pg.swift.enabled", Boolean.class, false),
                env.getProperty("pg.swift.retransmit-enabled", Boolean.class, false),
                env.getProperty("pg.swift.max-send-attempts", Integer.class, 3),
                env.getProperty("pg.swift.minimum-retransmit-seconds", Long.class, 30L));
    }

    @Bean
    public SubmitSwiftConsumerSkeleton submitSwiftKafka(SubmitSwiftUseCase useCase) {
        return new SubmitSwiftConsumerSkeleton(useCase);
    }

    @Bean
    public RetrySwiftBatchSkeleton retrySwiftBatch(
            RetrySwiftUseCase useCase, SwiftApplicationService service) {
        return new RetrySwiftBatchSkeleton(useCase, service::recover);
    }

    @Bean
    public OutboxStore outboxStore(DataSource dataSource) {
        return new SwiftOutboxStore(new JdbcTemplate(dataSource));
    }
}
