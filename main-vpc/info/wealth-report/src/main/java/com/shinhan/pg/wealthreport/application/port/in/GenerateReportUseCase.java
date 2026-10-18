package com.shinhan.pg.wealthreport.application.port.in;

import com.shinhan.pg.wealthreport.application.command.GenerateReportCommand;
import com.shinhan.pg.wealthreport.application.result.GenerateReportResult;

/**
 * <pre>
 * [MVP4-F01] 보고서 생성 요청
 * API 초안: POST /api/v1/wealth/reports
 * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 사용자 JWT / PUBLIC
 * 입력 근거: 기간
 * 출력 근거: reportId, 상태(접수)
 * 정상 응답 후보: 201 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 409 진행 중 보고서 있음
 * TODO: 최신 사용자 4-A: PDF 생성·S3 저장까지 기다려 완성 결과를 동기 응답한다. 과거 202/작업 접수 응답으로 되돌리지 않는다.
 * TODO: Java 지표 계산과 기준 시점을 고정한 뒤 가명처리한다. AI는 설명을 생성하고 숫자는 계산 결과와 대조한다.
 * TODO: 요청 키의 헤더명·필수 여부와 동기 성공 200/201 세부 계약은 초안이다. timeout·동시 생성 수는 기존 설정을 실제 집행하는 코드로 연결한다.
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a49816f8110d5e30813050b
 * </pre>
 */
public interface GenerateReportUseCase {
    GenerateReportResult generateReport(GenerateReportCommand command);
}
