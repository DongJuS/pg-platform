package com.shinhan.pg.payment.adapter.out.fep;

import com.shinhan.pg.payment.application.port.out.FepPaymentPort;
import com.shinhan.pg.payment.application.readmodel.PgResult;
import com.shinhan.pg.payment.domain.error.PaymentErrorCode;
import com.shinhan.pg.payment.domain.error.PaymentException;
import com.shinhan.pg.payment.domain.model.PaymentOperation;
import com.shinhan.pg.payment.domain.policy.PgCircuitBreakerPolicy;

import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Caller-owned FEP REST client. Mutations are sent once; result lookups never resubmit. */
public final class FepPaymentRestAdapter implements FepPaymentPort {
    private final RestClient client;
    private final String token;
    private final PgCircuitBreakerPolicy circuit;

    public FepPaymentRestAdapter(Environment env) {
        this(env, io.micrometer.observation.ObservationRegistry.NOOP);
    }

    public FepPaymentRestAdapter(
            Environment env, io.micrometer.observation.ObservationRegistry observation) {
        token = env.getProperty("pg.internal.service-token", "");
        var factory =
                new SimpleClientHttpRequestFactory() {
                    @Override
                    protected void prepareConnection(
                            java.net.HttpURLConnection connection, String method)
                            throws java.io.IOException {
                        super.prepareConnection(connection, method);
                        connection.setInstanceFollowRedirects(false);
                    }
                };
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(6000);
        client =
                RestClient.builder()
                        .observationRegistry(observation)
                        .baseUrl(
                                env.getProperty(
                                        "pg.clients.fep-adapter.base-url", "http://localhost:8091"))
                        .requestFactory(factory)
                        .build();
        circuit =
                new PgCircuitBreakerPolicy(
                        env.getProperty("pg.payment.circuit.failure-threshold", Integer.class, 5),
                        java.time.Duration.ofMillis(
                                env.getProperty(
                                        "pg.payment.circuit.cooldown-ms", Long.class, 30000L)));
    }

    public record Request(
            UUID operationId, UUID paymentId, String kind, BigDecimal amount, String currency) {}

    public record Receipt(UUID receiptId, String receiptStatus, PgResult result) {}

    @Override
    public PgResult submit(PaymentOperation op) {
        if (token.isBlank()) throw new PaymentException(PaymentErrorCode.PG_RESULT_UNKNOWN);
        if (!circuit.acquire(Instant.now()))
            throw new PaymentException(PaymentErrorCode.CIRCUIT_OPEN);
        try {
            var receipt =
                    client.post()
                            .uri("/internal/v1/fep/pg/operations")
                            .header("X-Internal-Caller", "payment")
                            .header("X-Internal-Service-Token", token)
                            .body(
                                    new Request(
                                            op.id(),
                                            op.paymentId(),
                                            op.kind(),
                                            op.amount().amount(),
                                            op.amount().currency().getCurrencyCode()))
                            .retrieve()
                            .body(Receipt.class);
            if (receipt == null
                    || receipt.receiptId() == null
                    || !"ACCEPTED".equals(receipt.receiptStatus())
                    || receipt.result() == null
                    || !op.id().equals(receipt.result().operationId()))
                throw new PaymentException(PaymentErrorCode.PG_RESULT_UNKNOWN);
            circuit.succeeded();
            return receipt.result();
        } catch (RestClientException | PaymentException failure) {
            circuit.failed(Instant.now());
            throw new PaymentException(PaymentErrorCode.PG_RESULT_UNKNOWN);
        }
    }

    @Override
    public Optional<PgResult> query(UUID id) {
        try {
            var receipt =
                    client.get()
                            .uri("/internal/v1/fep/pg/operations/{id}", id)
                            .header("X-Internal-Caller", "payment")
                            .header("X-Internal-Service-Token", token)
                            .retrieve()
                            .body(Receipt.class);
            return receipt == null ? Optional.empty() : Optional.ofNullable(receipt.result());
        } catch (RestClientResponseException failure) {
            if (failure.getStatusCode().value() == 404) return Optional.empty();
            throw new PaymentException(PaymentErrorCode.PG_RESULT_UNKNOWN);
        } catch (RestClientException failure) {
            throw new PaymentException(PaymentErrorCode.PG_RESULT_UNKNOWN);
        }
    }

    @Override
    public List<PgResult> results(LocalDate date) {
        try {
            var result =
                    client.get()
                            .uri("/internal/v1/fep/pg/results?businessDate={date}", date)
                            .header("X-Internal-Caller", "payment")
                            .header("X-Internal-Service-Token", token)
                            .retrieve()
                            .body(PgResult[].class);
            if (result == null) throw new PaymentException(PaymentErrorCode.PG_RESULT_UNKNOWN);
            return Arrays.asList(result);
        } catch (RestClientException failure) {
            throw new PaymentException(PaymentErrorCode.PG_RESULT_UNKNOWN);
        }
    }
}
