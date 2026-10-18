package com.shinhan.pg.account.adapter.in.web.response;

import java.util.UUID;

/** [MVP1-F10] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record ChangeAccountStatusResponse(UUID accountId, String status) {}
