package com.shinhan.pg.payment.application.port.out;

import com.shinhan.pg.payment.application.readmodel.PgResult;

public interface PgWebhookDecoderPort {
    PgResult verifyAndDecode(byte[] payload, String signature);
}
