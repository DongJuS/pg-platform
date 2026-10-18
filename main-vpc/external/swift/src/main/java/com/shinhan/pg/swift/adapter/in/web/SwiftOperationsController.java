package com.shinhan.pg.swift.adapter.in.web;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.swift.adapter.in.web.response.GetSwiftMessageHistoryResponse;
import com.shinhan.pg.swift.application.port.in.GetSwiftMessageHistoryUseCase;

/** 보류 API 파일 자리. Spring bean·HTTP mapping을 등록하지 않는다.
 * 필드명·상태·인증 방식은 각 메서드의 출처/미정을 확인한다.
 */
// 보류: @RestController
// 보류: @Profile("skeleton")
public final class SwiftOperationsController {
    private final GetSwiftMessageHistoryUseCase getSwiftMessageHistoryUseCase;

    public SwiftOperationsController(GetSwiftMessageHistoryUseCase getSwiftMessageHistoryUseCase) {
        this.getSwiftMessageHistoryUseCase =
                java.util.Objects.requireNonNull(getSwiftMessageHistoryUseCase);
    }

    /**
     * <pre>
     * [MVP5-F07] UETR 전문·재전송 이력 조회
     * API 초안: GET /internal/v1/swift/transfers/{uetr}/messages
     * 상태: 보류 — Controller bean/HTTP mapping 등록 금지
     * 인증/접근: 운영자 권한 / ADMIN
     * 입력 근거: UETR
     * 출력 근거: 메시지 이력·처리 결과·재전송 횟수
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 없는 UETR·접근 거절의 응답 계약은 후속 확정.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    // 보류: @GetMapping("/internal/v1/swift/transfers/{uetr}/messages")
    public GetSwiftMessageHistoryResponse getSwiftMessageHistory(
            String callerServiceId, String uetr) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        throw new FeatureNotImplementedException("MVP5-F07 (HELD)");
    }
}
