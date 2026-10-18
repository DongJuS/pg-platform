# mock-clearing 구현 파일

api/MockClearingController.java에 필요한 기관 동작과 경로 후보를 주석으로 적었다. HTTP 경로/DTO/접수·최종 결과의 wire 계약은 아직 확정하지 않았으므로 Controller/Mapping/bean을 등록하지 않는다.

scenario/SuccessScenario.java, TimeoutScenario.java, FailureScenario.java에 구현 지점을 나눴다. Mock PG에는 DuplicateWebhookScenario.java와 OutOfOrderWebhookScenario.java도 있다. 실제 결과·외부 송신·지연을 실행하지 않고 명시적 미구현 예외를 던진다.

기존 *ApiSkeleton/*ScenarioSkeleton은 초기 자리이며 새 기능 구현은 이름이 붙은 Controller/Scenario 파일의 주석에서 시작한다. 금융 기관 실제 호출은 이 모듈의 범위가 아니다.
