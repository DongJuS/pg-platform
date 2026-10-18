package com.shinhan.pg.payment.application.result;

import com.shinhan.pg.payment.application.readmodel.RefundSummary;

import java.util.List;

/** [MVP2-F05] 유스케이스 결과 초안. 업무 구현 완료 뒤에만 생성한다. HTTP/JSON 필수 필드는 최종 계약 전이다. */
public record ListRefundsResult(List<RefundSummary> refunds) {}
