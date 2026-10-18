package com.shinhan.pg.account.application.command;

import java.util.UUID;

/** [MVP1-F09] 유스케이스 입력 초안. requestId는 추적용이며 멱등키가 아니다. 사용자/서비스 신원은 신뢰한 인증 문맥에서 전달한다. */
public record GetAccountCommand(UUID requestId, UUID actorUserId, UUID accountId) {}
