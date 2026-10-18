# settlement 내부 구현 파일

DB: PostgreSQL. application/service/SettlementApplicationService.java의 메서드별 TODO를 채워 구현한다. HTTP Controller → input port → ApplicationService → domain/output port → persistence/client 방향이다.

## API 파일

| 기능 ID | 메서드·경로 | adapter/in/web 파일 | 상태 |
| --- | --- | --- | --- |
| MVP7-F04 | GET /internal/v1/settlements/{date} | SettlementController.java | 개발용 501 |
| MVP7-F04 | POST /internal/v1/settlements/{date}/rerun | SettlementController.java | 개발용 501 |

## 도메인·저장소 파일

- domain/model: SettlementCycle, SettlementLine, SettlementFile
- application/port/out Repository: SettlementCycleRepository, SettlementLineRepository, SettlementFileRepository
- domain/policy: NettingCalculator, SettlementCutoffPolicy, SettlementRecoveryPolicy
- adapter/out/persistence: 모델별 PersistenceAdapter, entity/*Entity, mapper/*EntityMapper.
- Entity의 ORM annotation·물리 컬럼/관계·고유 키는 미정 주석이며 업무 migration을 추가하지 않았다.

## 기능 진입점

- CloseSettlementUseCase
- GetSettlementUseCase
- RerunSettlementUseCase

기본/local/aws에는 스켈레톤 업무 bean/Controller가 없다. 유효한 개발용 HTTP 요청은 501, 형식 오류는 400이다. 실제 인증·도메인 검증·DB/외부 client·오류 계약·금융 동시성은 TODO다. 보류 API는 annotation을 주석으로 두고 Spring bean을 등록하지 않는다.

전체 경로·계약은 프로젝트 docs/prd/API_SKELETON_INDEX.md, 기능 근거는 FEATURE_FILE_CHECKLIST.md를 확인한다.
