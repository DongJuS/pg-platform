package com.shinhan.pg.swift.application.readmodel;

import java.util.UUID;

/** FEP 조회 응답을 모듈 내부로 변환한 초안. 접수/중간 결과와 최종 결과를 구분한다. */
public record SwiftAcknowledgementDraft(UUID messageId, String outcomeCode, boolean finalResult) {}
