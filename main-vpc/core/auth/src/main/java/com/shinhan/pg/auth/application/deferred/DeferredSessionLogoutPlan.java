package com.shinhan.pg.auth.application.deferred;

/**
 * <pre>
 * [MVP1-F18] 보류 상태를 보존하는 파일 자리.
 * 기본 access jti 철회와 별개로 refresh 세션·전체 기기 로그아웃은 보류. Controller/Service 메서드를 활성화하지 않는다.
 * TODO: 사용자 확정 전 API annotation·Spring bean·동작을 추가하지 않는다.
 * </pre>
 */
public final class DeferredSessionLogoutPlan {
    private DeferredSessionLogoutPlan() {}
}
