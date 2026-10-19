package com.shinhan.pg.auth.adapter.in.web;

import com.shinhan.pg.auth.adapter.in.web.response.GetCurrentUserResponse;
import com.shinhan.pg.auth.application.command.GetCurrentUserCommand;
import com.shinhan.pg.auth.application.port.in.GetCurrentUserUseCase;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 개발용 API 스켈레톤. 기본/local/aws에는 등록하지 않으며 성공한 금융 결과를 생성하지 않는다.
 * 필드명·상태·인증 방식은 각 메서드의 출처/미정을 확인한다.
 */
@RestController
@Profile("skeleton")
public final class UserProfileController {
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    public UserProfileController(GetCurrentUserUseCase getCurrentUserUseCase) {
        this.getCurrentUserUseCase = java.util.Objects.requireNonNull(getCurrentUserUseCase);
    }

    /**
     * <pre>
     * [MVP1-F05] 내 정보 조회
     * API 초안: GET /api/v1/users/me
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: —
     * 출력 근거: userId, 이름, 권한
     * 정상 응답 후보: 200
     * 오류 후보: 401
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981019060eb224973005c
     * </pre>
     */
    @GetMapping("/api/v1/users/me")
    @ResponseStatus(HttpStatus.OK)
    public GetCurrentUserResponse getCurrentUser(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                getCurrentUserUseCase.getCurrentUser(
                        new GetCurrentUserCommand(UUID.randomUUID(), actorUserId));
        return new GetCurrentUserResponse(result.userId(), result.name(), result.roles());
    }
}
