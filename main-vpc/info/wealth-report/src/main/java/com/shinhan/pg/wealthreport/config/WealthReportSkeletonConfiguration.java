package com.shinhan.pg.wealthreport.config;

import com.shinhan.pg.wealthreport.adapter.in.batch.RecoverReportGenerationBatch;
import com.shinhan.pg.wealthreport.adapter.in.kafka.UpdateReportSnapshotConsumerSkeleton;
import com.shinhan.pg.wealthreport.adapter.out.aigateway.AiReportDraftRestSkeleton;
import com.shinhan.pg.wealthreport.adapter.out.execution.BoundedGenerationExecutor;
import com.shinhan.pg.wealthreport.adapter.out.ledger.LedgerReportSnapshotRestAdapter;
import com.shinhan.pg.wealthreport.adapter.out.pdf.PdfRenderingSkeleton;
import com.shinhan.pg.wealthreport.adapter.out.persistence.AssetReportPersistenceAdapter;
import com.shinhan.pg.wealthreport.adapter.out.persistence.PrivateReportStorageAdapter;
import com.shinhan.pg.wealthreport.adapter.out.persistence.ReportSnapshotWriteSkeleton;
import com.shinhan.pg.wealthreport.application.port.out.AiReportDraftPort;
import com.shinhan.pg.wealthreport.application.port.out.AssetReportRepository;
import com.shinhan.pg.wealthreport.application.port.out.GenerationExecutionPort;
import com.shinhan.pg.wealthreport.application.port.out.PdfRenderingPort;
import com.shinhan.pg.wealthreport.application.port.out.ReportSnapshotPort;
import com.shinhan.pg.wealthreport.application.port.out.ReportSnapshotWritePort;
import com.shinhan.pg.wealthreport.application.port.out.ReportStoragePort;
import com.shinhan.pg.wealthreport.application.service.WealthReportApplicationService;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/** Existing development profile. Never wires TokenMappingRepository or a live LLM client. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
@EnableConfigurationProperties(ReportExecutionProperties.class)
@org.springframework.scheduling.annotation.EnableScheduling
public class WealthReportSkeletonConfiguration {
    @Bean
    public AssetReportRepository assetReportRepository(DataSource ds) {
        return new AssetReportPersistenceAdapter(ds);
    }

    @Bean
    public ReportStoragePort reportStoragePort(DataSource ds) {
        return new PrivateReportStorageAdapter(ds);
    }

    @Bean
    public ReportSnapshotPort reportSnapshotPort(Environment env) {
        return new LedgerReportSnapshotRestAdapter(env);
    }

    @Bean
    public ReportSnapshotWritePort reportSnapshotWritePort(DataSource ds) {
        return new ReportSnapshotWriteSkeleton(ds);
    }

    @Bean
    @Profile("!report-test")
    public AiReportDraftPort aiReportDraftPort() {
        return new AiReportDraftRestSkeleton();
    }

    @Bean
    public PdfRenderingPort pdfRenderingPort() {
        return new PdfRenderingSkeleton();
    }

    @Bean(destroyMethod = "close")
    public BoundedGenerationExecutor generationExecutionPort(ReportExecutionProperties p) {
        return new BoundedGenerationExecutor(p.maximumConcurrentGenerations());
    }

    @Bean
    public WealthReportApplicationService wealthReportApplicationService(
            ReportSnapshotPort s,
            AiReportDraftPort ai,
            PdfRenderingPort pdf,
            ReportStoragePort storage,
            AssetReportRepository reports,
            ReportSnapshotWritePort events,
            GenerationExecutionPort executor,
            ReportExecutionProperties p,
            Environment env) {
        return new WealthReportApplicationService(
                s,
                ai,
                pdf,
                storage,
                reports,
                events,
                executor,
                p.totalTimeout(),
                p.llmTimeout(),
                p.pdfTimeout(),
                p.storageTimeout(),
                p.maximumConcurrentGenerations(),
                env.getProperty("platform.report.policy.confirmed", Boolean.class, false));
    }

    @Bean
    public UpdateReportSnapshotConsumerSkeleton updateReportSnapshotKafka(
            WealthReportApplicationService service) {
        return new UpdateReportSnapshotConsumerSkeleton(service);
    }

    @Bean
    public RecoverReportGenerationBatch recoverReportGenerationBatch(
            WealthReportApplicationService service) {
        return new RecoverReportGenerationBatch(service);
    }
}
