# dev 월 비용 — 2026-10-18, 서울, USD

최초 NAT 1개 예산에서 [AI 전용 NAT 추가 결정](../adr/0001-financial-integration-decisions.md)을 반영해 범위 추정을 갱신했다. FEP 1개+AI 1개, 총 2개의 개발용 zonal NAT를 전제로 한다. NAT는 서울 공개 설명의 약값을 사용하므로 SKU별 확정 견적과 구분한다.

실제 AWS 리소스를 생성하지 않았다. 이번 작업에 따른 추가 AWS 청구는 없다. 유료 리소스 토글 기본값은 모두 false다. 빈 S3 버킷/state 소량 저장만 있는 기본 구성을 적용한 유휴 상태는 월 약 $0~1을 예상한다.

아래는 모든 저장소와 서비스 16개, endpoint를 월 730시간 켠 **유휴 개발 환경 추정**이다. 일 1,000만 요청의 성능·비용 견적이 아니다.

| 항목 | 전제 | 월 USD |
| --- | --- | ---: |
| Fargate | ARM 16개 × 0.25vCPU/0.5GiB | 132.66 |
| Interface endpoints | 4종 × 4 VPC × 2 AZ = 32 AZ 과금 단위 | 303.68 |
| RDS | db.t4g.micro 2개, Single-AZ, gp3 합계 40GB | 41.74 |
| Redis | cache.t4g.micro 1개 | 17.52 |
| MSK | kafka.t3.small 2개, 합계 20GB | 85.35 |
| 소계 | NAT·ALB·로그·secret 등 제외 | 580.95 |

NAT 2개·IPv4·선택적 ALB·소량 로그/Secrets Manager/S3까지 포함한 예산 범위는 **월 약 $700~800**다. 기존 범위 $650~750에 AI NAT 1개와 IPv4 1개를 월 약 $46.72 추가하는 간략 추정이다. NAT는 약 $0.059/시간 × 730 = $43.07, IPv4는 $0.005/시간 × 730 = $3.65로 계산했다. NAT 시간 단가는 최신 AWS 공식 설명의 서울 약값이며 원가표 SKU 조회값은 아니다. 트래픽·NAT 처리·AZ 간 통신·저장소 증가·LLM API 사용료·세금은 별도다.

가장 큰 고정비는 interface endpoint다. AI NAT를 별도 구역에 추가해 금융 FEP 출구와 분리했다. 목적지 CIDR 규칙과 client 검사 코드 자체에 별도 관리형 방화벽 비용을 더하지 않았으며 실제 제공사 목적지 설정은 후속이다. 비용을 바꾸려면 endpoint 배치·공유를 별도 설계해야 한다.

NAT 약값 근거: [AWS 공식 비교 설명, 2026-07-02](https://aws.amazon.com/cn/blogs/china/amazon-vpc-regional-nat-gateway-az-nat-gateway-comparison/). IPv4는 아래 AmazonVPC 서울 가격표의 APN2-PublicIPv4:InUseAddress 항목을 사용했다.

AWS 공개 서울 가격표: [ECS](https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonECS/current/ap-northeast-2/index.json), [VPC](https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonVPC/current/ap-northeast-2/index.json), [RDS](https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonRDS/current/ap-northeast-2/index.json), [ElastiCache](https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonElastiCache/current/ap-northeast-2/index.json), [MSK](https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonMSK/current/ap-northeast-2/index.json). SKU·단가·발행일은 [aws-cost-estimate.json](aws-cost-estimate.json)에 있다.

`enable_*` 변경은 원하는 Terraform 구성을 바꾸며 단순 일시 정지 버튼이 아니다. RDS 삭제 보호와 최종 스냅샷은 유지했다. apply는 실행하지 않았다.
