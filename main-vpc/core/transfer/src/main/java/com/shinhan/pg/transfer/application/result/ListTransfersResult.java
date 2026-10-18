package com.shinhan.pg.transfer.application.result;

import com.shinhan.pg.transfer.application.readmodel.TransferSummary;

import java.util.List;

/** [MVP1-F17] 유스케이스 결과 초안. 업무 구현 완료 뒤에만 생성한다. HTTP/JSON 필수 필드는 최종 계약 전이다. */
public record ListTransfersResult(List<TransferSummary> transfers, Integer nextPage) {}
