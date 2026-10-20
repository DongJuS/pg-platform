package com.shinhan.pg.mockpg.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.mockpg.scenario.PgOperationStore;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Currency;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;

/**
 * Synthetic PG only. Scenario control requires provider authentication and never touches real
 * institutions.
 */
@RestController
public final class MockPgController {
    private final PgOperationStore store;
    private final RestClient callbacks;
    private final byte[] secret;
    private volatile String fileScenario = "NORMAL";
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    public MockPgController(
            DataSource source,
            Environment env,
            io.micrometer.observation.ObservationRegistry observation) {
        store = new PgOperationStore(source);
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
        factory.setConnectTimeout(1500);
        factory.setReadTimeout(10000);
        callbacks =
                RestClient.builder()
                        .observationRegistry(observation)
                        .baseUrl(
                                env.getProperty(
                                        "pg.clients.fep-adapter.base-url", "http://localhost:8091"))
                        .requestFactory(factory)
                        .build();
        String hex = env.getProperty("pg.provider.webhook-secret-hex", "");
        secret = hex.isBlank() ? new byte[0] : HexFormat.of().parseHex(hex);
    }

    @PostMapping("/mock/v1/pg/operations")
    public PgOperationResult submit(@RequestBody PgOperationRequest r) {
        if (r.operationId() == null
                || r.paymentId() == null
                || r.amount() == null
                || r.amount().signum() <= 0
                || r.kind() == null
                || r.currency() == null
                || !List.of("APPROVAL", "REFUND", "CANCEL").contains(r.kind()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        Money.of(r.amount(), Currency.getInstance(r.currency()));
        store.attempt(r.operationId());
        var old = store.find(r.operationId());
        if (old != null) return store.submit(r, old.outcome());
        String mode = store.takeScenario();
        if ("ERROR_BEFORE_COMMIT".equals(mode))
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
        if ("TIMEOUT_BEFORE_COMMIT".equals(mode)) {
            pause();
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT);
        }
        var result =
                store.submit(
                        r,
                        "DECLINE".equals(mode)
                                ? "DECLINED"
                                : "PENDING".equals(mode) ? "PENDING" : "APPROVED");
        if ("TIMEOUT_AFTER_COMMIT".equals(mode)) pause();
        return result;
    }

    @GetMapping("/mock/v1/pg/operations/{id}")
    public PgOperationResult query(@PathVariable UUID id) {
        var r = store.find(id);
        if (r == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return r;
    }

    @GetMapping("/mock/v1/pg/results")
    public List<PgOperationResult> results(@RequestParam LocalDate businessDate) {
        var rows = new java.util.ArrayList<>(store.results(businessDate));
        if (!rows.isEmpty()) {
            var r = rows.getFirst();
            if ("AMOUNT_MISMATCH".equals(fileScenario))
                rows.set(
                        0,
                        new PgOperationResult(
                                r.operationId(),
                                r.paymentId(),
                                r.kind(),
                                r.outcome(),
                                r.amount().add(java.math.BigDecimal.ONE),
                                r.currency(),
                                r.version(),
                                r.eventId(),
                                r.externalReference(),
                                r.occurredAt()));
            if ("DUPLICATE".equals(fileScenario)) rows.add(r);
            if ("MISSING".equals(fileScenario)) rows.removeFirst();
        }
        return rows;
    }

    @PostMapping("/mock/v1/pg/reconciliation-scenario")
    public void fileScenario(@RequestBody Scenario r) {
        if (!List.of("NORMAL", "AMOUNT_MISMATCH", "DUPLICATE", "MISSING").contains(r.mode()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        fileScenario = r.mode();
    }

    public record Scenario(String mode) {}

    @PostMapping("/mock/v1/pg/scenarios")
    public void scenario(@RequestBody Scenario r) {
        store.scenario(r.mode());
    }

    public record Confirmation(String outcome) {}

    @PostMapping("/mock/v1/pg/operations/{id}/confirm")
    public PgOperationResult confirm(@PathVariable UUID id, @RequestBody Confirmation c) {
        if (c.outcome() == null || !List.of("APPROVED", "DECLINED").contains(c.outcome()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return store.confirm(id, c.outcome());
    }

    public record Delivery(int copies, boolean older) {}

    @PostMapping("/mock/v1/pg/operations/{id}/webhooks")
    public void webhook(@PathVariable UUID id, @RequestBody Delivery d) throws Exception {
        if (d.copies() < 1 || d.copies() > 3 || secret.length < 32)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        var r = query(id);
        if (d.older())
            r =
                    new PgOperationResult(
                            r.operationId(),
                            r.paymentId(),
                            r.kind(),
                            "PENDING",
                            r.amount(),
                            r.currency(),
                            1,
                            UUID.randomUUID(),
                            r.externalReference(),
                            r.occurredAt());
        byte[] raw = json.writeValueAsBytes(r);
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(raw));
        for (int i = 0; i < d.copies(); i++)
            callbacks
                    .post()
                    .uri("/callbacks/v1/pg")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-PG-Signature", signature)
                    .body(raw)
                    .retrieve()
                    .toBodilessEntity();
    }

    private void pause() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
