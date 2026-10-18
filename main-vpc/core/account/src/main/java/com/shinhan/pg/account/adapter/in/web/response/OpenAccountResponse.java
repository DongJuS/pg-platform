package com.shinhan.pg.account.adapter.in.web.response;

import java.util.UUID;

/** [MVP1-F07] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record OpenAccountResponse(
        UUID accountId, String accountNumber, String currency, String status) {}
