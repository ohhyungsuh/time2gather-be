# Time2Gather Infrastructure - Terraform

## 개요

AWS 인프라를 Terraform으로 관리합니다. **ALB 2대 → 1대 통합**으로 비용을 절감합니다.

### 예상 비용 절감

| 항목 | Before | After | 절감액 |
|------|--------|-------|--------|
| ALB | $16.25 (2대) | ~$8 (1대) | **-$8** |
| EC2 | $12.19 | $12.19 | - |
| Route53 | $1.51 | $1.51 | - |
| **합계** | ~$42 | **~$25** | **~$17/월** |

## 아키텍처

```
                    ┌─────────────────────────────────────┐
                    │            Route 53                  │
                    │  time2gather.org, api.time2gather.org│
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │         ALB (1대 - 통합)             │
                    │  - HTTPS (443)                       │
                    │  - HTTP (80) → HTTPS redirect        │
                    └──────────────┬──────────────────────┘
                                   │
              ┌────────────────────┴────────────────────┐
              │                                          │
    ┌─────────▼─────────┐                    ┌──────────▼─────────┐
    │  Host: *.org      │                    │  Host: api.*.org   │
    │  → FE Target Group│                    │  → BE Target Group │
    │  (Port 3000)      │                    │  (Port 8080)       │
    └─────────┬─────────┘                    └──────────┬─────────┘
              │                                          │
              └────────────────────┬─────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │         EC2 (t3.small)               │
                    │  - Frontend: :3000                   │
                    │  - Backend:  :8080                   │
                    └─────────────────────────────────────┘
```

## 파일 구조

```
terraform/
├── versions.tf          # Provider 설정
├── variables.tf         # 변수 정의
├── vpc.tf              # VPC, Subnet 설정
├── security_groups.tf  # Security Group 설정
├── ec2.tf              # EC2 인스턴스
├── alb.tf              # ALB (통합) 설정
├── route53.tf          # DNS 설정
├── outputs.tf          # 출력 값
├── terraform.tfvars.example  # 설정 예시
└── .gitignore
```

## 사전 요구사항

1. **Terraform 설치** (v1.0+)
   ```bash
   brew install terraform  # macOS
   ```

2. **AWS CLI 설정**
   ```bash
   aws configure
   # AWS Access Key ID, Secret Access Key 입력
   ```

3. **필요한 정보 준비**
   - ACM Certificate ARN (HTTPS용 인증서)
   - EC2 Key Pair 이름
   - 기존 VPC ID (선택사항)

## 마이그레이션 가이드

### Step 1: 설정 파일 생성

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
```

`terraform.tfvars` 수정:
```hcl
key_name                 = "your-key-pair-name"
existing_certificate_arn = "arn:aws:acm:ap-northeast-2:123456789:certificate/xxx"
# existing_vpc_id        = "vpc-xxx"  # 기존 VPC 사용시
```

### Step 2: 기존 리소스 확인

현재 AWS 콘솔에서 아래 정보 확인:
- VPC ID
- Subnet IDs
- ACM Certificate ARN
- EC2 Key Pair Name

### Step 3: Terraform 초기화 및 계획

```bash
# 초기화
terraform init

# 실행 계획 확인 (실제 변경 없음)
terraform plan
```

### Step 4: 기존 리소스 Import (선택)

기존 리소스를 Terraform으로 관리하려면:

```bash
# VPC Import
terraform import aws_vpc.main[0] vpc-xxxxxxxx

# EC2 Import
terraform import aws_instance.app i-xxxxxxxx

# 기타 리소스도 동일하게 import
```

### Step 5: 신규 ALB로 전환 (권장 방식)

**다운타임 최소화 전략:**

1. **새 ALB 생성** (Terraform apply)
2. **테스트**: 새 ALB DNS로 접속 테스트
3. **Route53 전환**: DNS를 새 ALB로 변경
4. **기존 ALB 삭제**: AWS 콘솔에서 수동 삭제

```bash
# 새 인프라 생성
terraform apply

# 출력된 ALB DNS로 테스트
curl https://<new-alb-dns>
```

## 적용

```bash
# Dry-run (변경사항 확인)
terraform plan

# 실제 적용
terraform apply

# 삭제 (주의!)
terraform destroy
```

## 주의사항

1. **terraform.tfvars는 절대 git에 커밋하지 마세요** (비밀 정보 포함)
2. **apply 전에 항상 plan을 먼저 실행하세요**
3. **기존 ALB 삭제는 DNS 전환 후에 수동으로 하세요**

## 비용 모니터링

적용 후 AWS Cost Explorer에서 비용 변화를 확인하세요:
- ELB 비용이 약 50% 감소해야 함 ($16 → $8)
