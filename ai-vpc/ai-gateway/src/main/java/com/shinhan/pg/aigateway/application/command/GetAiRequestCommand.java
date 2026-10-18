package com.shinhan.pg.aigateway.application.command;

import java.util.UUID;

/** [MVP3-F02] 유스케이스 입력 초안. requestId는 조회 자원이고 correlationId가 추적용이다. 사용자/서비스 신원은 신뢰한 인증 문맥에서 전달한다. */
public record GetAiRequestCommand(UUID correlationId, String callerServiceId, UUID requestId) {}
