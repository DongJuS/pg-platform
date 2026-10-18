package com.shinhan.pg.swift.application.result;

import com.shinhan.pg.swift.application.readmodel.SwiftMessageView;

import java.util.List;

/** [MVP5-F07] 유스케이스 결과 초안. 업무 구현 완료 뒤에만 생성한다. HTTP/JSON 필수 필드는 최종 계약 전이다. */
public record GetSwiftMessageHistoryResult(List<SwiftMessageView> messages) {}
