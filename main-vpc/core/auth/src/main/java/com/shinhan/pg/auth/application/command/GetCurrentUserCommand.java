package com.shinhan.pg.auth.application.command;

import java.util.UUID;

/** [MVP1-F05] 유스케이스 입력 초안. requestId는 추적용이며 멱등키가 아니다. 사용자/서비스 신원은 신뢰한 인증 문맥에서 전달한다. */
public record GetCurrentUserCommand(UUID requestId, UUID actorUserId) {}
