package com.shinhan.pg.account.adapter.in.web.response;

import com.shinhan.pg.account.application.readmodel.AccountSummary;

import java.util.List;

/** [MVP1-F08] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record ListAccountsResponse(List<AccountSummary> accounts, Integer nextPage) {}
