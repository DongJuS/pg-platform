package com.shinhan.pg.payment.config;

import com.shinhan.pg.common.outbox.OutboxStore;
import com.shinhan.pg.payment.adapter.out.account.AccountAuthorityRestAdapter;
import com.shinhan.pg.payment.adapter.out.fep.FepPaymentRestAdapter;
import com.shinhan.pg.payment.adapter.out.ledger.PaymentLedgerRestAdapter;
import com.shinhan.pg.payment.adapter.out.persistence.PaymentPersistenceAdapter;
import com.shinhan.pg.payment.adapter.out.persistence.outbox.PaymentOutboxStore;
import com.shinhan.pg.payment.adapter.out.webhook.PgWebhookDecoderAdapter;
import com.shinhan.pg.payment.application.port.out.PaymentStore;
import com.shinhan.pg.payment.application.service.PaymentApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/** Real MVP2 flow assembly; production activation remains a separate deployment decision. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class PaymentSkeletonConfiguration {
    @Bean
    public OutboxStore outboxStore(DataSource source) {
        return new PaymentOutboxStore(new JdbcTemplate(source));
    }

    @Bean
    public PaymentStore paymentStore(DataSource source, OutboxStore outbox) {
        return new PaymentPersistenceAdapter(source, outbox);
    }

    @Bean
    public com.shinhan.pg.payment.application.service.PaymentReconciliationService
            paymentReconciliationService(
                    PaymentStore store,
                    OutboxStore outbox,
                    DataSource source,
                    Environment env,
                    org.springframework.beans.factory.ObjectProvider<
                                    io.micrometer.core.instrument.MeterRegistry>
                            registries,
                    org.springframework.beans.factory.ObjectProvider<
                                    io.micrometer.observation.ObservationRegistry>
                            observations) {
        var registry = registries.getIfAvailable();
        var counter =
                registry == null ? null : registry.counter("pg.payment.reconciliation.mismatch");
        return new com.shinhan.pg.payment.application.service.PaymentReconciliationService(
                store,
                new com.shinhan.pg.payment.adapter.out.persistence
                        .ReconciliationRunPersistenceAdapter(source, outbox),
                new FepPaymentRestAdapter(
                        env,
                        observations.getIfAvailable(
                                () -> io.micrometer.observation.ObservationRegistry.NOOP)),
                new PaymentLedgerRestAdapter(
                        env,
                        observations.getIfAvailable(
                                () -> io.micrometer.observation.ObservationRegistry.NOOP)),
                count -> {
                    if (counter != null) counter.increment(count);
                });
    }

    @Bean
    public PaymentApplicationService paymentApplicationService(
            PaymentStore store,
            Environment env,
            org.springframework.beans.factory.ObjectProvider<
                            io.micrometer.observation.ObservationRegistry>
                    observations) {
        return new PaymentApplicationService(
                store,
                new FepPaymentRestAdapter(
                        env,
                        observations.getIfAvailable(
                                () -> io.micrometer.observation.ObservationRegistry.NOOP)),
                new PaymentLedgerRestAdapter(
                        env,
                        observations.getIfAvailable(
                                () -> io.micrometer.observation.ObservationRegistry.NOOP)),
                new AccountAuthorityRestAdapter(
                        env,
                        observations.getIfAvailable(
                                () -> io.micrometer.observation.ObservationRegistry.NOOP)),
                new PgWebhookDecoderAdapter(env.getProperty("pg.provider.webhook-secret-hex", "")),
                env.getProperty("pg.payment.recovery-queries-enabled", Boolean.class, false));
    }
}
