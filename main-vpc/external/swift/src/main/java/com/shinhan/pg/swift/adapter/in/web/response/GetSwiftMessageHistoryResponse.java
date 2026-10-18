package com.shinhan.pg.swift.adapter.in.web.response;

import com.shinhan.pg.swift.application.readmodel.SwiftMessageView;

import java.util.List;

/** [MVP5-F07] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record GetSwiftMessageHistoryResponse(List<SwiftMessageView> messages) {}
