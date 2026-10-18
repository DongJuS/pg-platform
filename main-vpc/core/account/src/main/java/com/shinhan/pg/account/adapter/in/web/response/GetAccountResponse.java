package com.shinhan.pg.account.adapter.in.web.response;

import java.util.UUID;

/** [MVP1-F09] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record GetAccountResponse(
        UUID accountId,
        UUID ownerId,
        String accountNumber,
        String alias,
        String currency,
        String status) {}
