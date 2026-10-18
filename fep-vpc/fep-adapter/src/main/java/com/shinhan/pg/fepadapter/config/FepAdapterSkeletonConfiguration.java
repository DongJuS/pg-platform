package com.shinhan.pg.fepadapter.config;

import com.shinhan.pg.fepadapter.adapter.out.institutions.clearing.ClearingInstitutionSkeleton;
import com.shinhan.pg.fepadapter.adapter.out.institutions.kofiu.KofiuInstitutionSkeleton;
import com.shinhan.pg.fepadapter.adapter.out.institutions.pg.PgInstitutionSkeleton;
import com.shinhan.pg.fepadapter.adapter.out.institutions.swift.SwiftInstitutionSkeleton;
import com.shinhan.pg.fepadapter.adapter.out.persistence.FepMessagePersistenceAdapter;
import com.shinhan.pg.fepadapter.adapter.out.persistence.FepMessageWriteSkeleton;
import com.shinhan.pg.fepadapter.adapter.out.persistence.FepTransmissionAttemptPersistenceAdapter;
import com.shinhan.pg.fepadapter.application.port.out.ClearingInstitutionPort;
import com.shinhan.pg.fepadapter.application.port.out.FepMessageRepository;
import com.shinhan.pg.fepadapter.application.port.out.FepMessageWritePort;
import com.shinhan.pg.fepadapter.application.port.out.FepTransmissionAttemptRepository;
import com.shinhan.pg.fepadapter.application.port.out.KofiuInstitutionPort;
import com.shinhan.pg.fepadapter.application.port.out.PgInstitutionPort;
import com.shinhan.pg.fepadapter.application.port.out.SwiftInstitutionPort;
import com.shinhan.pg.fepadapter.application.service.FepAdapterApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** 개발용 의존성 조립. 기본/local/aws에는 업무 스켈레톤 bean을 등록하지 않는다. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class FepAdapterSkeletonConfiguration {
    @Bean
    public PgInstitutionPort pgInstitutionPort() {
        return new PgInstitutionSkeleton();
    }

    @Bean
    public SwiftInstitutionPort swiftInstitutionPort() {
        return new SwiftInstitutionSkeleton();
    }

    @Bean
    public KofiuInstitutionPort kofiuInstitutionPort() {
        return new KofiuInstitutionSkeleton();
    }

    @Bean
    public ClearingInstitutionPort clearingInstitutionPort() {
        return new ClearingInstitutionSkeleton();
    }

    @Bean
    public FepMessageWritePort fepMessageWritePort() {
        return new FepMessageWriteSkeleton();
    }

    @Bean
    public FepMessageRepository fepMessageRepository() {
        return new FepMessagePersistenceAdapter();
    }

    @Bean
    public FepTransmissionAttemptRepository fepTransmissionAttemptRepository() {
        return new FepTransmissionAttemptPersistenceAdapter();
    }

    @Bean
    public FepAdapterApplicationService fepAdapterApplicationService(
            PgInstitutionPort pgInstitutionPort,
            SwiftInstitutionPort swiftInstitutionPort,
            KofiuInstitutionPort kofiuInstitutionPort,
            ClearingInstitutionPort clearingInstitutionPort,
            FepMessageWritePort fepMessageWritePort,
            FepMessageRepository fepMessageRepository,
            FepTransmissionAttemptRepository fepTransmissionAttemptRepository) {
        return new FepAdapterApplicationService(
                pgInstitutionPort,
                swiftInstitutionPort,
                kofiuInstitutionPort,
                clearingInstitutionPort,
                fepMessageWritePort,
                fepMessageRepository,
                fepTransmissionAttemptRepository);
    }
}
