package com.shinhan.pg.payment.adapter.out.ledger;

import com.shinhan.pg.payment.application.port.out.LedgerPostingPort;
import com.shinhan.pg.payment.domain.error.PaymentErrorCode;
import com.shinhan.pg.payment.domain.error.PaymentException;
import com.shinhan.pg.payment.domain.model.PaymentDetails;
import com.shinhan.pg.payment.domain.model.PaymentOperation;

import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Ledger is the sole owner of balances and journals. One operation ID = one journal transaction ID.
 */
public final class PaymentLedgerRestAdapter implements LedgerPostingPort {
    private final RestClient client;
    private final String token;

    public PaymentLedgerRestAdapter(Environment env) {
        this(env, io.micrometer.observation.ObservationRegistry.NOOP);
    }

    public PaymentLedgerRestAdapter(
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
        factory.setReadTimeout(5000);
        client =
                RestClient.builder()
                        .observationRegistry(observation)
                        .baseUrl(
                                env.getProperty(
                                        "pg.clients.ledger.base-url", "http://localhost:8083"))
                        .requestFactory(factory)
                        .build();
    }

    public record Posting(UUID accountId, BigDecimal signedAmount, String currency) {}

    public record Request(UUID transactionId, List<Posting> postings) {}

    public record Receipt(UUID transactionId, UUID journalId, UUID eventId, String status) {}

    @Override
    public UUID post(PaymentDetails p, PaymentOperation op) {
        if (token.isBlank()) throw new PaymentException(PaymentErrorCode.LEDGER_PENDING);
        var from = "APPROVAL".equals(op.kind()) ? p.sourceAccountId() : p.destinationAccountId();
        var to = "APPROVAL".equals(op.kind()) ? p.destinationAccountId() : p.sourceAccountId();
        var amount = op.amount().amount();
        var currency = op.amount().currency().getCurrencyCode();
        try {
            var receipt =
                    client.post()
                            .uri("/internal/v1/ledger/postings")
                            .header("X-Internal-Caller", "payment")
                            .header("X-Internal-Service-Token", token)
                            .body(
                                    new Request(
                                            op.id(),
                                            List.of(
                                                    new Posting(from, amount.negate(), currency),
                                                    new Posting(to, amount, currency))))
                            .retrieve()
                            .body(Receipt.class);
            return verified(receipt, op.id());
        } catch (RestClientResponseException failure) {
            if (failure.getStatusCode().value() == 422)
                throw new PaymentException(PaymentErrorCode.INVALID_STATE);
            throw new PaymentException(PaymentErrorCode.LEDGER_PENDING);
        } catch (RestClientException failure) {
            throw new PaymentException(PaymentErrorCode.LEDGER_PENDING);
        }
    }

    @Override
    public Optional<UUID> find(UUID id) {
        try {
            var r =
                    client.get()
                            .uri("/internal/v1/ledger/transactions/{id}", id)
                            .header("X-Internal-Caller", "payment")
                            .header("X-Internal-Service-Token", token)
                            .retrieve()
                            .body(Receipt.class);
            return Optional.of(verified(r, id));
        } catch (RestClientResponseException failure) {
            if (failure.getStatusCode().value() == 404) return Optional.empty();
            throw new PaymentException(PaymentErrorCode.LEDGER_PENDING);
        } catch (RestClientException failure) {
            throw new PaymentException(PaymentErrorCode.LEDGER_PENDING);
        }
    }

    @Override
    public com.shinhan.pg.payment.application.readmodel.LedgerAudit audit(UUID id) {
        try {
            var result =
                    client.get()
                            .uri("/internal/v1/ledger/transactions/{id}/postings", id)
                            .header("X-Internal-Caller", "payment")
                            .header("X-Internal-Service-Token", token)
                            .retrieve()
                            .body(com.shinhan.pg.payment.application.readmodel.LedgerAudit.class);
            if (result == null || !id.equals(result.transactionId()) || result.postings() == null)
                throw new PaymentException(PaymentErrorCode.LEDGER_PENDING);
            return result;
        } catch (RestClientException failure) {
            throw new PaymentException(PaymentErrorCode.LEDGER_PENDING);
        }
    }

    private UUID verified(Receipt receipt, UUID id) {
        if (receipt == null || !id.equals(receipt.transactionId()) || receipt.journalId() == null)
            throw new PaymentException(PaymentErrorCode.LEDGER_PENDING);
        return receipt.journalId();
    }
}
