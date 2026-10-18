package com.shinhan.pg.fepadapter.domain.model;

import java.util.UUID;

/** [MVP5-F04,MVP7-F03] 순수 업무 모델 초안. 접수와 기관 최종 결과를 구분한다. 채널+messageKey로 중복 전송을 통제한다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record FepMessage(
        UUID id,
        String channelCode,
        String messageKey,
        String messageHash,
        String acceptanceStatus,
        String outcomeCode,
        Boolean finalResult) {}
