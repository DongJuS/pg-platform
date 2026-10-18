package com.shinhan.pg.payment.config;

import com.shinhan.pg.payment.adapter.in.batch.RecoverPaymentBatch;
import com.shinhan.pg.payment.adapter.in.kafka.ApplyPaymentConfirmationConsumerSkeleton;
import com.shinhan.pg.payment.adapter.out.fep.FepPaymentRestSkeleton;
import com.shinhan.pg.payment.adapter.out.ledger.PaymentLedgerRestSkeleton;
import com.shinhan.pg.payment.adapter.out.persistence.PaymentAtomicWriteSkeleton;
import com.shinhan.pg.payment.adapter.out.persistence.PaymentPersistenceAdapter;
import com.shinhan.pg.payment.adapter.out.persistence.PgEventPersistenceAdapter;
import com.shinhan.pg.payment.adapter.out.persistence.ReconciliationRunPersistenceAdapter;
import com.shinhan.pg.payment.adapter.out.persistence.RefundPersistenceAdapter;
import com.shinhan.pg.payment.adapter.out.webhook.PgWebhookDecoderAdapter;
import com.shinhan.pg.payment.application.port.in.ApplyPaymentConfirmationUseCase;
import com.shinhan.pg.payment.application.port.in.RecoverPaymentUseCase;
import com.shinhan.pg.payment.application.port.out.FepPaymentPort;
import com.shinhan.pg.payment.application.port.out.LedgerPostingPort;
import com.shinhan.pg.payment.application.port.out.PaymentAtomicWritePort;
import com.shinhan.pg.payment.application.port.out.PaymentRepository;
import com.shinhan.pg.payment.application.port.out.PgEventRepository;
import com.shinhan.pg.payment.application.port.out.PgWebhookDecoderPort;
import com.shinhan.pg.payment.application.port.out.ReconciliationRunRepository;
import com.shinhan.pg.payment.application.port.out.RefundRepository;
import com.shinhan.pg.payment.application.service.PaymentApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** 개발용 의존성 조립. 기본/local/aws에는 업무 스켈레톤 bean을 등록하지 않는다. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class PaymentSkeletonConfiguration {
    @Bean
    public FepPaymentPort fepPaymentPort() {
        return new FepPaymentRestSkeleton();
    }

    @Bean
    public LedgerPostingPort ledgerPostingPort() {
        return new PaymentLedgerRestSkeleton();
    }

    @Bean
    public PaymentAtomicWritePort paymentAtomicWritePort() {
        return new PaymentAtomicWriteSkeleton();
    }

    @Bean
    public PgWebhookDecoderPort pgWebhookDecoderPort() {
        return new PgWebhookDecoderAdapter();
    }

    @Bean
    public PaymentRepository paymentRepository() {
        return new PaymentPersistenceAdapter();
    }

    @Bean
    public RefundRepository refundRepository() {
        return new RefundPersistenceAdapter();
    }

    @Bean
    public PgEventRepository pgEventRepository() {
        return new PgEventPersistenceAdapter();
    }

    @Bean
    public ReconciliationRunRepository reconciliationRunRepository() {
        return new ReconciliationRunPersistenceAdapter();
    }

    @Bean
    public PaymentApplicationService paymentApplicationService(
            FepPaymentPort fepPaymentPort,
            LedgerPostingPort ledgerPostingPort,
            PaymentAtomicWritePort paymentAtomicWritePort,
            PgWebhookDecoderPort pgWebhookDecoderPort,
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            PgEventRepository pgEventRepository,
            ReconciliationRunRepository reconciliationRunRepository) {
        return new PaymentApplicationService(
                fepPaymentPort,
                ledgerPostingPort,
                paymentAtomicWritePort,
                pgWebhookDecoderPort,
                paymentRepository,
                refundRepository,
                pgEventRepository,
                reconciliationRunRepository);
    }

    @Bean
    public ApplyPaymentConfirmationConsumerSkeleton applyPaymentConfirmationKafka(
            ApplyPaymentConfirmationUseCase useCase) {
        return new ApplyPaymentConfirmationConsumerSkeleton(useCase);
    }

    @Bean
    public RecoverPaymentBatch recoverPaymentBatch(RecoverPaymentUseCase useCase) {
        return new RecoverPaymentBatch(useCase);
    }
}
