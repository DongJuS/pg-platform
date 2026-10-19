package com.shinhan.pg.aml.config;

import com.shinhan.pg.aml.adapter.in.batch.RetryAmlReportBatchSkeleton;
import com.shinhan.pg.aml.adapter.in.kafka.EvaluateTransactionConsumerSkeleton;
import com.shinhan.pg.aml.adapter.out.fep.FepKofiuRestSkeleton;
import com.shinhan.pg.aml.adapter.out.persistence.AmlAtomicWriteSkeleton;
import com.shinhan.pg.aml.adapter.out.persistence.AmlCasePersistenceAdapter;
import com.shinhan.pg.aml.adapter.out.persistence.AmlReportPersistenceAdapter;
import com.shinhan.pg.aml.adapter.out.persistence.DetectionRulePersistenceAdapter;
import com.shinhan.pg.aml.application.port.in.EvaluateTransactionUseCase;
import com.shinhan.pg.aml.application.port.in.RetryAmlReportUseCase;
import com.shinhan.pg.aml.application.port.out.AmlAtomicWritePort;
import com.shinhan.pg.aml.application.port.out.AmlCaseRepository;
import com.shinhan.pg.aml.application.port.out.AmlReportRepository;
import com.shinhan.pg.aml.application.port.out.DetectionRuleRepository;
import com.shinhan.pg.aml.application.port.out.FepKofiuPort;
import com.shinhan.pg.aml.application.service.AmlApplicationService;
import com.shinhan.pg.aml.adapter.out.persistence.outbox.AmlOutboxStore;
import com.shinhan.pg.common.outbox.OutboxStore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/** 개발용 의존성 조립. 기본/local/aws에는 업무 스켈레톤 bean을 등록하지 않는다. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class AmlSkeletonConfiguration {
    @Bean
    public FepKofiuPort fepKofiuPort() {
        return new FepKofiuRestSkeleton();
    }

    @Bean
    public AmlAtomicWritePort amlAtomicWritePort() {
        return new AmlAtomicWriteSkeleton();
    }

    @Bean
    public AmlCaseRepository amlCaseRepository() {
        return new AmlCasePersistenceAdapter();
    }

    @Bean
    public AmlReportRepository amlReportRepository() {
        return new AmlReportPersistenceAdapter();
    }

    @Bean
    public DetectionRuleRepository detectionRuleRepository() {
        return new DetectionRulePersistenceAdapter();
    }

    @Bean
    public AmlApplicationService amlApplicationService(
            FepKofiuPort fepKofiuPort,
            AmlAtomicWritePort amlAtomicWritePort,
            AmlCaseRepository amlCaseRepository,
            AmlReportRepository amlReportRepository,
            DetectionRuleRepository detectionRuleRepository) {
        return new AmlApplicationService(
                fepKofiuPort,
                amlAtomicWritePort,
                amlCaseRepository,
                amlReportRepository,
                detectionRuleRepository);
    }

    @Bean
    public EvaluateTransactionConsumerSkeleton evaluateTransactionKafka(
            EvaluateTransactionUseCase useCase) {
        return new EvaluateTransactionConsumerSkeleton(useCase);
    }

    @Bean
    public RetryAmlReportBatchSkeleton retryAmlReportBatch(RetryAmlReportUseCase useCase) {
        return new RetryAmlReportBatchSkeleton(useCase);
    }

    @Bean
    public OutboxStore outboxStore(DataSource dataSource) {
        return new AmlOutboxStore(new JdbcTemplate(dataSource));
    }
}
