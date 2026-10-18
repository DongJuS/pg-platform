# API Gateway 구현 파일

- config/GatewayRoutesConfiguration.java: 외부 경로와 대상 서비스, Ledger 상세 경로 우선 매칭, 내부 경로 제외·동기 보고서 timeout 연결 TODO.
- filter/GatewayJwtFilter.java: RS256 검증·만료·철회·신뢰한 사용자 문맥 전파 자리.
- filter/GatewayRateLimitFilter.java: Redis 원자 카운터·TTL·429·실패 모드 자리.
- filter/InternalPathBlockFilter.java: 내부 경로와 외부 catch-all의 경계 검증 자리.
- config/DeferredOperationsApiPlan.java: 보류 운영 API 두 개의 후보 주석.

위 신규 파일은 실제 route/filter bean을 등록하지 않는다. 기존 health 외 기본 차단을 유지한다. 금융 Controller·도메인·Repository를 Gateway에 두지 않는다.
