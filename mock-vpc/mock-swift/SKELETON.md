# Mock SWIFT 구현

실제 기관 연결 없이 `swift-test` + `pg.swift.enabled=true`에서만 동작한다. `MockSwiftController`가 공식 XSD 부분집합 검증, FEP 전용 인증, 디스크의 원자적 파일 교체/파일 잠금/flush와 같은 UETR·전문 hash 중복 방지를 구현한다. private XML은 암호화하며 48시간 후 삭제한다. UETR·hash·효과 횟수는 재시작 중복 방지를 위해 유지한다.

| 합성 수취계좌 값 | 시나리오 |
| --- | --- |
| MOCK_SUCCESS / 일반 합성 식별자 | ACSC |
| MOCK_REJECT | RJCT, 정산 효과 없음 |
| MOCK_PENDING | ACSP 유지 |
| MOCK_DELAY | ACSP 후 2초 이후 ACSC |
| MOCK_LOST_RESPONSE | 처리·저장 후 HTTP 응답 지연으로 FEP read timeout 유도, 조회는 ACSC |
| MOCK_UNKNOWN | 결과 없이 불명 유지 |
| MOCK_RETRY | 최초 시도는 효과 없이 불명, 같은 UETR 재전송 시 한 번 처리 |

FEP 전용 `POST /mock/v1/swift/messages`, `GET /mock/v1/swift/messages/{uetr}/result`. 테스트 운영자 전용 `GET /mock/v1/swift/messages/{uetr}`는 전달/효과 횟수를 반환하고 `POST .../{uetr}/results`는 중복·역순·상충 상태를 주입한다. 모든 경로는 전용 토큰을 요구한다.

옛 scenario/* 자리 파일은 미조립 템플릿이다. 실제 실행 경로·한계·재전송/권한/보존 계약은 [MVP5 체크리스트](../../main-vpc/external/swift/SKELETON.md)를 따른다. 단일 Mock 로컬 파일 저장이며 운영용 다중 노드 금융기관 저장소가 아니다.
