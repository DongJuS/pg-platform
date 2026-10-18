package com.shinhan.pg.ledger.adapter.in.web.request;

import com.shinhan.pg.ledger.application.command.PostingInstruction;

import java.util.List;
import java.util.UUID;

/** [MVP1-F13] 요청 DTO 초안. 신원/멱등키/경로 변수는 본문과 구분한다. 검증 제약은 주석의 계약을 따라 추가한다. */
public record PostJournalRequest(UUID transactionId, List<PostingInstruction> postings) {}
