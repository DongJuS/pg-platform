package com.shinhan.pg.fepadapter.config;

import com.shinhan.pg.fepadapter.adapter.out.institutions.pg.PgProviderRestAdapter;
import com.shinhan.pg.fepadapter.adapter.out.payment.PaymentCallbackRestAdapter;
import com.shinhan.pg.fepadapter.adapter.out.persistence.PgReceiptPersistenceAdapter;
import com.shinhan.pg.fepadapter.application.service.PgGatewayApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class PgGatewayConfiguration {
    @Bean
    PgGatewayApplicationService pgGateway(
            DataSource source,
            Environment env,
            org.springframework.beans.factory.ObjectProvider<
                            io.micrometer.core.instrument.MeterRegistry>
                    registries,
            org.springframework.beans.factory.ObjectProvider<
                            io.micrometer.observation.ObservationRegistry>
                    observations) {
        return new PgGatewayApplicationService(
                new PgReceiptPersistenceAdapter(source),
                new PgProviderRestAdapter(
                        env,
                        registries.getIfAvailable(),
                        observations.getIfAvailable(
                                () -> io.micrometer.observation.ObservationRegistry.NOOP)),
                new PaymentCallbackRestAdapter(
                        env,
                        observations.getIfAvailable(
                                () -> io.micrometer.observation.ObservationRegistry.NOOP)),
                env.getProperty("pg.provider.webhook-secret-hex", ""));
    }
}
