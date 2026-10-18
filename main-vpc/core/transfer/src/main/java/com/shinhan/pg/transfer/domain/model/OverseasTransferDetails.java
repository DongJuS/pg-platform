package com.shinhan.pg.transfer.domain.model;

import java.util.UUID;

/** [MVP5-F01,MVP5-F02,MVP5-F08] 순수 업무 모델 초안. 가상 수취인만 사용. UETR은 송금별로 재사용하며 최종 승인/거절과 중간 응답을 구분한다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record OverseasTransferDetails(
        UUID transferId,
        String uetr,
        String bic,
        String recipientAccount,
        String recipientName,
        String status) {
    @Override
    public String toString() {
        return "OverseasTransferDetails[REDACTED]";
    }
}
