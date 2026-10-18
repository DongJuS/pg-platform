# dev Monthly Cost — 2026-10-18, Seoul, USD

The range estimate was updated from the original one-NAT budget to reflect the [decision to add an AI-only NAT](../adr/0001-financial-integration-decisions.md). It assumes two dev zonal NATs in total: one for FEP and one for AI. The NAT figure uses an approximate price from a public description for Seoul, so keep it separate from a confirmed per-SKU quote.

No real AWS resources were created. This work adds no AWS charges. All paid-resource toggles default to false. The idle state with the default configuration applied, which holds only an empty S3 bucket and a small amount of state, is expected to cost about $0~1 per month.

Below is an **idle development environment estimate** with all data stores, the 16 services and the endpoints running 730 hours a month. It is not a performance or cost quote for 10 million requests per day.

| Item | Assumption | USD/month |
| --- | --- | ---: |
| Fargate | 16 ARM × 0.25vCPU/0.5GiB | 132.66 |
| Interface endpoints | 4 types × 4 VPCs × 2 AZs = 32 AZ billing units | 303.68 |
| RDS | 2 × db.t4g.micro, Single-AZ, 40GB gp3 in total | 41.74 |
| Redis | 1 × cache.t4g.micro | 17.52 |
| MSK | 2 × kafka.t3.small, 20GB in total | 85.35 |
| Subtotal | Excludes NAT, ALB, logs, secrets, etc. | 580.95 |

Including two NATs, IPv4, an optional ALB and small amounts of logs/Secrets Manager/S3, the budget range is **about $700~800 per month**. This is a rough estimate that adds one AI NAT and one IPv4 address, about $46.72 per month, to the earlier range of $650~750. NAT is calculated as about $0.059/hour × 730 = $43.07, and IPv4 as $0.005/hour × 730 = $3.65. The NAT hourly rate is an approximate Seoul price from the latest official AWS description, not a SKU value looked up in the price list. Traffic, NAT processing, cross-AZ traffic, storage growth, LLM API fees and taxes are extra.

The largest fixed cost is the interface endpoints. The AI NAT was added in the AI VPC to keep it apart from the financial FEP egress. No separate managed firewall cost was added for the destination CIDR rules or the client check code itself, and configuring the real provider destinations is follow-up work. Changing the cost requires a separate design for endpoint placement and sharing.

Source for the approximate NAT price: [Official AWS comparison write-up, 2026-07-02](https://aws.amazon.com/cn/blogs/china/amazon-vpc-regional-nat-gateway-az-nat-gateway-comparison/). For IPv4, the APN2-PublicIPv4:InUseAddress item in the AmazonVPC Seoul price list below was used.

Public AWS Seoul price lists: [ECS](https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonECS/current/ap-northeast-2/index.json), [VPC](https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonVPC/current/ap-northeast-2/index.json), [RDS](https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonRDS/current/ap-northeast-2/index.json), [ElastiCache](https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonElastiCache/current/ap-northeast-2/index.json), [MSK](https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonMSK/current/ap-northeast-2/index.json). SKUs, unit prices and publication dates are in [aws-cost-estimate.json](aws-cost-estimate.json).

Changing `enable_*` changes the desired Terraform configuration; it is not a simple pause button. RDS deletion protection and the final snapshot are kept. apply was not run.
