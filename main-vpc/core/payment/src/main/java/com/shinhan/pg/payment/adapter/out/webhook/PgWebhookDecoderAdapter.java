package com.shinhan.pg.payment.adapter.out.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.payment.application.port.out.PgWebhookDecoderPort;
import com.shinhan.pg.payment.application.readmodel.PgResult;
import com.shinhan.pg.payment.domain.error.PaymentErrorCode;
import com.shinhan.pg.payment.domain.error.PaymentException;

import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Verify exact raw bytes before decoding. Never log raw body, HMAC key or supplied signature. */
public final class PgWebhookDecoderAdapter implements PgWebhookDecoderPort {
    private final byte[] secret;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    public PgWebhookDecoderAdapter(String secretHex) {
        secret = secretHex.isBlank() ? new byte[0] : HexFormat.of().parseHex(secretHex);
    }

    @Override
    public PgResult verifyAndDecode(byte[] payload, String signature) {
        try {
            if (secret.length < 32
                    || payload == null
                    || payload.length > 65536
                    || signature == null
                    || !signature.matches("[a-f0-9]{64}")) throw new IllegalArgumentException();
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            if (!MessageDigest.isEqual(mac.doFinal(payload), HexFormat.of().parseHex(signature)))
                throw new IllegalArgumentException();
            var r = json.readValue(payload, PgResult.class);
            if (r == null
                    || r.operationId() == null
                    || r.paymentId() == null
                    || r.kind() == null
                    || r.outcome() == null
                    || r.amount() == null
                    || r.amount().signum() <= 0
                    || r.currency() == null
                    || r.version() < 1
                    || r.eventId() == null
                    || r.externalReference() == null
                    || r.occurredAt() == null) throw new IllegalArgumentException();
            com.shinhan.pg.common.commonmoney.Money.of(
                    r.amount(), java.util.Currency.getInstance(r.currency()));
            return r;
        } catch (Exception invalid) {
            throw new PaymentException(PaymentErrorCode.INVALID_WEBHOOK);
        }
    }
}
