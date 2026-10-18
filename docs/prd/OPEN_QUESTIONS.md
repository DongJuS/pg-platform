# 남은 구현과 배포 입력

여섯 설계 선택은 [확정 결정과 체크리스트](../adr/0001-financial-integration-decisions.md)에 기록했다. 아래에서는 설계 미정과 구현 대기를 구분한다.

1. **AI VPC → 외부 LLM — 구조 반영·client 구현 대기**: AI 전용 NAT 출구로 확정했다. AI 전용 NAT·라우팅·외부 HTTPS 목적지 CIDR 제한과 hostname 검사 구조를 반영했다. NAT는 기본 비활성, 목적지 CIDR/hostname은 기본 비어 있다. 실제 LLM client에 hostname 검사·redirect 차단·timeout을 연결하고 제공사 목적지를 지정하는 작업이 남아 있다. data 서브넷 격리는 유지한다.
2. **AWS Redis IAM**: 비밀값을 Terraform state로 가져오지 않도록 IAM 인증과 TLS를 선택했다. 애플리케이션의 IAM 토큰 생성·갱신 provider와 키 접두사 정책은 미구현이다. `redis_iam_client_ready=false` 기본값이 미완성 ECS 배포를 차단한다. 로컬 Redis 연결은 검증했다.
3. **AWS 입력**: 기존 S3 state 버킷, 서비스별 Secrets Manager 이름(username/password), 불변 서비스 이미지 URI, 공개 게이트웨이를 켤 경우 ACM ARN이 필요하다. 서비스 DB 계정·권한은 준비하지 않았다. Terraform은 secret metadata만 읽으며, 값은 배포된 ECS에서 주입받는 코드다.
4. **AWS 관측성 목적지**: 로컬 Collector·Prometheus·Grafana는 검증했다. AWS Collector/trace 저장소의 배포 위치와 사설 수집 주소는 미정이다. 배포 시 OTLP 수집 주소를 제공해야 한다.

MariaDB + PostgreSQL 선택은 사용자 확인으로 해결됐다. account·ledger는 MariaDB, 나머지 저장소 사용 서비스 9개는 PostgreSQL이다. 예전 ERD 업무 테이블은 이번 빈 Flyway 기준선에 복사하지 않았다.

JWT RS256 발급·검증·철회, OTP, rate limit, 업무용 Outbox 저장 어댑터, API·이벤트 계약, 금액 계산, 외부 기관 송수신은 후속 업무 구현 단계다. Gateway는 검증 TODO 완료 전까지 health 외 요청을 기본 차단한다.

참고: [VPC peering 제한](https://docs.aws.amazon.com/vpc/latest/peering/vpc-peering-basics.html), [ElastiCache IAM](https://docs.aws.amazon.com/AmazonElastiCache/latest/dg/auth-iam.html).

보고서 동기 응답의 시간 예산·동시 생성 수는 설정과 값 검증만 있다. 실제 제한 집행, 금융 업무 엔티티·저장 SQL·Outbox 원자성·잠금 및 복구 보장은 후속이다. 현재 검증은 구조·기술 설정 범위이며 [최종 스켈레톤 안내](SKELETON_GUIDE.md)를 따른다.

서비스별 API/DTO·도메인·저장소 파일과 구현 TODO는 [API 목록](API_SKELETON_INDEX.md)·[기능별 파일 체크리스트](FEATURE_FILE_CHECKLIST.md)에 연결했다. 36개 개발용 경로의 501/400 검증과 5개 보류 API의 매핑 미등록을 확인했으며 정상 금융 응답·인증·최종 HTTP 계약·실제 ORM/SQL은 후속이다.
