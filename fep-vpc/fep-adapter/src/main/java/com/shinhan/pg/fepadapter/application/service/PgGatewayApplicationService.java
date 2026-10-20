package com.shinhan.pg.fepadapter.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.fepadapter.application.port.out.PaymentCallbackPort;
import com.shinhan.pg.fepadapter.application.port.out.PgProviderPort;
import com.shinhan.pg.fepadapter.application.port.out.PgReceiptStore;
import com.shinhan.pg.fepadapter.domain.model.PgRequest;
import com.shinhan.pg.fepadapter.domain.model.PgResult;

import java.security.MessageDigest;
import java.util.Currency;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Synchronous PG receipt/result boundary. Callback ACK requires downstream financial confirmation.
 */
public final class PgGatewayApplicationService {
    private final PgReceiptStore store;
    private final PgProviderPort provider;
    private final PaymentCallbackPort callback;
    private final byte[] secret;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    public record Receipt(UUID receiptId, String receiptStatus, PgResult result) {}

    public PgGatewayApplicationService(
            PgReceiptStore store,
            PgProviderPort provider,
            PaymentCallbackPort callback,
            String secretHex) {
        this.store = store;
        this.provider = provider;
        this.callback = callback;
        secret = secretHex.isBlank() ? new byte[0] : HexFormat.of().parseHex(secretHex);
    }

    public Receipt submit(PgRequest r) {
        if (r == null
                || r.operationId() == null
                || r.paymentId() == null
                || r.amount() == null
                || r.amount().signum() <= 0
                || r.kind() == null
                || r.currency() == null
                || !List.of("APPROVAL", "REFUND", "CANCEL").contains(r.kind()))
            throw new IllegalArgumentException("Invalid operation");
        Money.of(r.amount(), Currency.getInstance(r.currency()));
        var receipt = store.reserve(r);
        if (store.claim(r.operationId())) {
            try {
                var result = provider.submit(r);
                if (result != null) {
                    validate(result);
                    if (!r.operationId().equals(result.operationId()))
                        throw new IllegalArgumentException("PG operation mismatch");
                    store.result(result);
                }
            } catch (IllegalStateException unknown) {
                /* Durable receipt remains UNKNOWN; no financial retransmission. */
            }
        }
        return new Receipt(receipt, "ACCEPTED", store.result(r.operationId()));
    }

    public Receipt query(UUID id) {
        var receipt = store.receipt(id);
        var result = store.result(id);
        if (result == null || "PENDING".equals(result.outcome())) {
            var queried = provider.query(id);
            if (queried.isPresent()) {
                validate(queried.get());
                store.result(queried.get());
                result = store.result(id);
            }
        }
        return new Receipt(receipt, "ACCEPTED", result);
    }

    public List<PgResult> results(java.time.LocalDate date) {
        return provider.results(date);
    }

    public void receive(byte[] raw, String signature) {
        try {
            if (secret.length < 32
                    || raw == null
                    || raw.length > 65536
                    || signature == null
                    || !signature.matches("[a-f0-9]{64}"))
                throw new IllegalArgumentException("Invalid signature");
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            if (!MessageDigest.isEqual(mac.doFinal(raw), HexFormat.of().parseHex(signature)))
                throw new IllegalArgumentException("Invalid signature");
            var result = json.readValue(raw, PgResult.class);
            validate(result);
            store.result(result);
            store.callback(result.eventId(), raw, signature);
        } catch (IllegalArgumentException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalArgumentException("Invalid signed PG event");
        }
        callback.forward(raw, signature);
    }

    private void validate(PgResult r) {
        if (r == null
                || r.operationId() == null
                || r.paymentId() == null
                || r.eventId() == null
                || r.occurredAt() == null
                || r.externalReference() == null
                || r.kind() == null
                || r.amount() == null
                || r.currency() == null
                || r.outcome() == null
                || r.version() < 1
                || !List.of("APPROVED", "DECLINED", "PENDING").contains(r.outcome()))
            throw new IllegalArgumentException("Invalid PG result");
    }
}
