package com.shinhan.pg.fepadapter.application.command;

import java.util.UUID;

/** 접수와 최종 결과를 구분하는 내부 초안. 실제 HTTP/기관 wire 계약은 후속 구현한다. */
public record QueryInstitutionResultCommand(
        UUID requestId, String channelCode, String messageKey) {}
