package com.shinhan.pg.payment.adapter.in.web.request;

import java.time.LocalDate;

/** [MVP2-F08] 요청 DTO 초안. 신원/멱등키/경로 변수는 본문과 구분한다. 검증 제약은 주석의 계약을 따라 추가한다. */
public record StartReconciliationRequest(LocalDate businessDate) {}
