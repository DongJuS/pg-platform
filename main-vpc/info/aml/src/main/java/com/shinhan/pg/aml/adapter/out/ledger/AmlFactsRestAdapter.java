package com.shinhan.pg.aml.adapter.out.ledger;

import com.shinhan.pg.aml.application.port.out.AmlSourcePort;
import com.shinhan.pg.aml.domain.model.FinancialFact;
import com.shinhan.pg.common.amlcontracts.AmlProblem;
import com.shinhan.pg.common.amlcontracts.AmlSourceFact;
import com.shinhan.pg.common.amlcontracts.LedgerEvidence;
import com.shinhan.pg.common.commonerror.ErrorType;

import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

/** Uses source-owned REST only. HTTP completes before AML transaction; no cross-database reads. */
public final class AmlFactsRestAdapter implements AmlSourcePort {
    private final RestClient ledger, transfer, payment;

    public AmlFactsRestAdapter(Environment e) {
        ledger = client(e, "ledger", 8083);
        transfer = client(e, "transfer", 8084);
        payment = client(e, "payment", 8085);
    }

    private static RestClient client(Environment e, String service, int port) {
        String base =
                e.getProperty(
                        "pg.clients." + service + ".base-url", "http://" + service + ":" + port);
        URI u = URI.create(base);
        AmlProblem.require(
                "http".equals(u.getScheme())
                        && java.util.Set.of("localhost", "127.0.0.1", service).contains(u.getHost())
                        && u.getUserInfo() == null
                        && u.getQuery() == null
                        && u.getFragment() == null
                        && (u.getPath().isEmpty() || "/".equals(u.getPath())));
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(2))
                                .followRedirects(HttpClient.Redirect.NEVER)
                                .build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        String token = e.getProperty("pg.aml.caller-tokens.aml", "");
        AmlProblem.require(!token.isBlank());
        return RestClient.builder()
                .baseUrl(base)
                .requestFactory(factory)
                .defaultHeader("X-Internal-Caller", "aml")
                .defaultHeader("X-Internal-Service-Token", token)
                .build();
    }

    public Resolution resolve(UUID transaction, UUID expectedJournal) {
        try {
            var l =
                    ledger.get()
                            .uri("/internal/v1/ledger/aml-facts/{id}", transaction)
                            .retrieve()
                            .body(LedgerEvidence.class);
            if (l == null
                    || !transaction.equals(l.transactionId())
                    || l.postedAt() == null
                    || l.journalId() == null
                    || (expectedJournal != null && !expectedJournal.equals(l.journalId())))
                throw AmlProblem.conflict();
            if (l.overseasTransferId() != null && "HOLD".equals(l.overseasPhase()))
                return new Resolution(null, "OVERSEAS_HOLD");
            UUID lookup = l.overseasTransferId() == null ? transaction : l.overseasTransferId();
            AmlSourceFact candidate =
                    optional(transfer, "/internal/v1/transfers/aml-facts/{id}", lookup);
            if (candidate == null)
                candidate = optional(payment, "/internal/v1/payments/aml-facts/{id}", transaction);
            if (candidate == null) throw missing();
            final AmlSourceFact s = candidate;
            boolean reversed = l.overseasTransferId() != null && "RJCT".equals(l.overseasOutcome());
            if (!java.util.Set.of("COMPLETED", "REJECTED").contains(s.status()))
                throw new AmlProblem("AML-SOURCE-NOT-FINAL", ErrorType.EXTERNAL_FAILURE);
            if ("REJECTED".equals(s.status()) && !reversed) throw missing();
            if (s.amount() == null
                    || s.currency() == null
                    || s.customerId() == null
                    || s.sourceAccountId() == null
                    || l.lines() == null
                    || l.lines().size() != 2) throw missing();
            var minus = l.lines().stream().filter(x -> x.signedAmount().signum() < 0).toList();
            var plus = l.lines().stream().filter(x -> x.signedAmount().signum() > 0).toList();
            if (minus.size() != 1
                    || plus.size() != 1
                    || l.lines().stream().anyMatch(x -> !sCurrency(s).equals(x.currency()))
                    || minus.getFirst().signedAmount().negate().compareTo(s.amount()) != 0
                    || plus.getFirst().signedAmount().compareTo(s.amount()) != 0)
                throw AmlProblem.conflict();
            if (l.overseasTransferId() == null) {
                UUID debit =
                        s.originalBusinessId() == null
                                ? s.sourceAccountId()
                                : s.destinationAccountId();
                UUID credit =
                        s.originalBusinessId() == null
                                ? s.destinationAccountId()
                                : s.sourceAccountId();
                if (!minus.getFirst().accountId().equals(debit)
                        || !plus.getFirst().accountId().equals(credit)
                        || !transaction.equals(s.transactionId())) throw AmlProblem.conflict();
            } else if (!l.overseasTransferId().equals(s.businessId())) throw AmlProblem.conflict();
            return new Resolution(
                    new FinancialFact(
                            transaction,
                            s.businessId(),
                            reversed ? "OVERSEAS_REVERSAL" : s.kind(),
                            s.customerId(),
                            s.sourceAccountId(),
                            s.destinationAccountId(),
                            s.amount(),
                            s.currency(),
                            l.postedAt(),
                            reversed ? s.businessId() : s.originalBusinessId()),
                    null);
        } catch (AmlProblem e) {
            throw e;
        } catch (Exception e) {
            throw missing();
        }
    }

    private static String sCurrency(AmlSourceFact s) {
        return s.currency();
    }

    private static AmlSourceFact optional(RestClient client, String path, UUID id) {
        try {
            return client.get().uri(path, id).retrieve().body(AmlSourceFact.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) return null;
            throw missing();
        }
    }

    private static AmlProblem missing() {
        return new AmlProblem("AML-SOURCE-UNAVAILABLE", ErrorType.EXTERNAL_FAILURE);
    }
}
