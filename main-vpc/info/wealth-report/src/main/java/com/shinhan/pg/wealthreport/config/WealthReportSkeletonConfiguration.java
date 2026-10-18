package com.shinhan.pg.wealthreport.config;

import com.shinhan.pg.wealthreport.adapter.in.batch.RecoverReportGenerationBatch;
import com.shinhan.pg.wealthreport.adapter.in.kafka.UpdateReportSnapshotConsumerSkeleton;
import com.shinhan.pg.wealthreport.adapter.out.aigateway.AiReportDraftRestSkeleton;
import com.shinhan.pg.wealthreport.adapter.out.pdf.PdfRenderingSkeleton;
import com.shinhan.pg.wealthreport.adapter.out.persistence.AssetReportPersistenceAdapter;
import com.shinhan.pg.wealthreport.adapter.out.persistence.AssetSnapshotPersistenceAdapter;
import com.shinhan.pg.wealthreport.adapter.out.persistence.ReportProgressSkeleton;
import com.shinhan.pg.wealthreport.adapter.out.persistence.ReportSnapshotReadSkeleton;
import com.shinhan.pg.wealthreport.adapter.out.persistence.ReportSnapshotWriteSkeleton;
import com.shinhan.pg.wealthreport.adapter.out.persistence.TokenMappingPersistenceAdapter;
import com.shinhan.pg.wealthreport.adapter.out.storage.ReportStorageSkeleton;
import com.shinhan.pg.wealthreport.adapter.out.tokenization.PseudonymizationSkeleton;
import com.shinhan.pg.wealthreport.application.port.in.RecoverReportGenerationUseCase;
import com.shinhan.pg.wealthreport.application.port.in.UpdateReportSnapshotUseCase;
import com.shinhan.pg.wealthreport.application.port.out.AiReportDraftPort;
import com.shinhan.pg.wealthreport.application.port.out.AssetReportRepository;
import com.shinhan.pg.wealthreport.application.port.out.AssetSnapshotRepository;
import com.shinhan.pg.wealthreport.application.port.out.PdfRenderingPort;
import com.shinhan.pg.wealthreport.application.port.out.PseudonymizationPort;
import com.shinhan.pg.wealthreport.application.port.out.ReportProgressPort;
import com.shinhan.pg.wealthreport.application.port.out.ReportSnapshotPort;
import com.shinhan.pg.wealthreport.application.port.out.ReportSnapshotWritePort;
import com.shinhan.pg.wealthreport.application.port.out.ReportStoragePort;
import com.shinhan.pg.wealthreport.application.port.out.TokenMappingRepository;
import com.shinhan.pg.wealthreport.application.service.WealthReportApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** 개발용 의존성 조립. 기본/local/aws에는 업무 스켈레톤 bean을 등록하지 않는다. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
@org.springframework.boot.context.properties.EnableConfigurationProperties(
        ReportExecutionProperties.class)
public class WealthReportSkeletonConfiguration {
    @Bean
    public ReportSnapshotPort reportSnapshotPort() {
        return new ReportSnapshotReadSkeleton();
    }

    @Bean
    public PseudonymizationPort pseudonymizationPort() {
        return new PseudonymizationSkeleton();
    }

    @Bean
    public AiReportDraftPort aiReportDraftPort() {
        return new AiReportDraftRestSkeleton();
    }

    @Bean
    public PdfRenderingPort pdfRenderingPort() {
        return new PdfRenderingSkeleton();
    }

    @Bean
    public ReportStoragePort reportStoragePort() {
        return new ReportStorageSkeleton();
    }

    @Bean
    public ReportProgressPort reportProgressPort() {
        return new ReportProgressSkeleton();
    }

    @Bean
    public ReportSnapshotWritePort reportSnapshotWritePort() {
        return new ReportSnapshotWriteSkeleton();
    }

    @Bean
    public AssetReportRepository assetReportRepository() {
        return new AssetReportPersistenceAdapter();
    }

    @Bean
    public AssetSnapshotRepository assetSnapshotRepository() {
        return new AssetSnapshotPersistenceAdapter();
    }

    @Bean
    public TokenMappingRepository tokenMappingRepository() {
        return new TokenMappingPersistenceAdapter();
    }

    @Bean
    public WealthReportApplicationService wealthReportApplicationService(
            ReportSnapshotPort reportSnapshotPort,
            PseudonymizationPort pseudonymizationPort,
            AiReportDraftPort aiReportDraftPort,
            PdfRenderingPort pdfRenderingPort,
            ReportStoragePort reportStoragePort,
            ReportProgressPort reportProgressPort,
            ReportSnapshotWritePort reportSnapshotWritePort,
            AssetReportRepository assetReportRepository,
            AssetSnapshotRepository assetSnapshotRepository,
            TokenMappingRepository tokenMappingRepository) {
        return new WealthReportApplicationService(
                reportSnapshotPort,
                pseudonymizationPort,
                aiReportDraftPort,
                pdfRenderingPort,
                reportStoragePort,
                reportProgressPort,
                reportSnapshotWritePort,
                assetReportRepository,
                assetSnapshotRepository,
                tokenMappingRepository);
    }

    @Bean
    public UpdateReportSnapshotConsumerSkeleton updateReportSnapshotKafka(
            UpdateReportSnapshotUseCase useCase) {
        return new UpdateReportSnapshotConsumerSkeleton(useCase);
    }

    @Bean
    public RecoverReportGenerationBatch recoverReportGenerationBatch(
            RecoverReportGenerationUseCase useCase) {
        return new RecoverReportGenerationBatch(useCase);
    }
}
