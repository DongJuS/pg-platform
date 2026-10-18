package com.shinhan.pg.payment.application.port.out;

import com.shinhan.pg.payment.application.readmodel.VerifiedPgEventDraft;

/** [MVP2-F06,MVP2-F15] 원문 바이트 서명 검증을 먼저 수행하고 검증된 이벤트만 반환한다. 서명·리플레이 범위 계약은 미정. */
public interface PgWebhookDecoderPort {
    VerifiedPgEventDraft verifyAndDecode(byte[] rawPayload, String signature);
}
