package com.shinhan.pg.aml.application.result;

import com.shinhan.pg.aml.application.readmodel.AmlCaseSummary;

import java.util.List;

/** [MVP6-F02] 유스케이스 결과 초안. 업무 구현 완료 뒤에만 생성한다. HTTP/JSON 필수 필드는 최종 계약 전이다. */
public record ListAmlCasesResult(List<AmlCaseSummary> cases, Integer nextPage) {}
