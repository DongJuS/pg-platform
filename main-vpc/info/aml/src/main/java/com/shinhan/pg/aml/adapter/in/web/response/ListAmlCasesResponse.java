package com.shinhan.pg.aml.adapter.in.web.response;

import com.shinhan.pg.aml.application.readmodel.AmlCaseSummary;

import java.util.List;

/** [MVP6-F02] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record ListAmlCasesResponse(List<AmlCaseSummary> cases, Integer nextPage) {}
