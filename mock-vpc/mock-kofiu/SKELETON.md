# MVP6 Mock KoFIU

`MockKofiuController`는 `aml-test` 및 `pg.aml.enabled=true`에서만 등록되는 로컬 개발 기관이다. 실제 KoFIU와 연결하지 않는다. `POST /mock/v1/kofiu/reports`는 고정 보고를 접수하고 `POST /mock/v1/kofiu/report-queries`는 reportId/reportKey/contentHash를 대조한 결과를 반환한다. FEP 전용 자격증명과 시나리오 제어 전용 자격증명은 분리한다.

파일 잠금·원자적 교체·fsync로 프로세스 재시작과 중복 전달을 보호한다. 같은 키 다른 내용/보고 ID는 충돌이다. 접수 횟수와 전달 횟수는 다르며 하나의 보고에는 최대 1회의 접수 효과만 있다. SUCCESS, PENDING, REJECT, UNAVAILABLE, 실제 지연 응답에 의한 RESPONSE_LOSS를 개발 운영 경로로 주입한다. PENDING 이후 최종 결과는 별도 결과 주입 후 조회로 확인한다. 보고 문서 원문은 저장하지 않는다.

접수 식별자·상태는 48시간 뒤 삭제하고 해시 tombstone만 남겨 재접수를 막는다. 조회에서도 만료를 즉시 검사한다. 보고/수정 보고 형식과 정책, 실제 검증 범위는 [AML 체크리스트](../../main-vpc/info/aml/SKELETON.md)를 따른다. 기존 Scenario/Skeleton 클래스는 미사용 과거 자리이며 활성 처리 경로가 아니다.
