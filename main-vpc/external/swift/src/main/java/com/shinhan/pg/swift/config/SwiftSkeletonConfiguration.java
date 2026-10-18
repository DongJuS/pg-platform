package com.shinhan.pg.swift.config;

import com.shinhan.pg.swift.adapter.in.batch.RetrySwiftBatchSkeleton;
import com.shinhan.pg.swift.adapter.in.kafka.SubmitSwiftConsumerSkeleton;
import com.shinhan.pg.swift.adapter.out.fep.FepSwiftRestSkeleton;
import com.shinhan.pg.swift.adapter.out.persistence.SwiftAtomicWriteSkeleton;
import com.shinhan.pg.swift.adapter.out.persistence.SwiftMessagePersistenceAdapter;
import com.shinhan.pg.swift.application.port.in.RetrySwiftUseCase;
import com.shinhan.pg.swift.application.port.in.SubmitSwiftUseCase;
import com.shinhan.pg.swift.application.port.out.FepSwiftPort;
import com.shinhan.pg.swift.application.port.out.SwiftAtomicWritePort;
import com.shinhan.pg.swift.application.port.out.SwiftMessageRepository;
import com.shinhan.pg.swift.application.service.SwiftApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** 개발용 의존성 조립. 기본/local/aws에는 업무 스켈레톤 bean을 등록하지 않는다. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class SwiftSkeletonConfiguration {
    @Bean
    public FepSwiftPort fepSwiftPort() {
        return new FepSwiftRestSkeleton();
    }

    @Bean
    public SwiftAtomicWritePort swiftAtomicWritePort() {
        return new SwiftAtomicWriteSkeleton();
    }

    @Bean
    public SwiftMessageRepository swiftMessageRepository() {
        return new SwiftMessagePersistenceAdapter();
    }

    @Bean
    public SwiftApplicationService swiftApplicationService(
            FepSwiftPort fepSwiftPort,
            SwiftAtomicWritePort swiftAtomicWritePort,
            SwiftMessageRepository swiftMessageRepository) {
        return new SwiftApplicationService(
                fepSwiftPort, swiftAtomicWritePort, swiftMessageRepository);
    }

    @Bean
    public SubmitSwiftConsumerSkeleton submitSwiftKafka(SubmitSwiftUseCase useCase) {
        return new SubmitSwiftConsumerSkeleton(useCase);
    }

    @Bean
    public RetrySwiftBatchSkeleton retrySwiftBatch(RetrySwiftUseCase useCase) {
        return new RetrySwiftBatchSkeleton(useCase);
    }
}
