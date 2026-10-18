package com.shinhan.pg.auth.adapter.in.web.response;

import java.util.UUID;

/** [MVP1-F01] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record RegisterUserResponse(UUID userId) {}
