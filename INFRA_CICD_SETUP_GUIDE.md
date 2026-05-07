# 자동 배포 (CI/CD + Terraform) 가이드

---

## 1. Terraform 설치

- Terraform 설치  
  https://developer.hashicorp.com/terraform/install

- 설치된 terraform.exe을 원하는 폴더에 위치
- 환경변수 설정에 들어가서 path에 해당 exe파일이 위치한 경로 추가
- 아래 명령어를 통해 설치 확인
```bash
terraform -version
```

---

## 2. IAM 생성 (Terraform용 - AWS 콘솔에서 직접 생성)

- AWS IAM 사용자 생성
- 권한:
    - 학습: AdministratorAccess
    - 실무: 최소 권한만 부여
- IAM 생성 후 Access Key 발급

---

## 3. AWS CLI 인증
- 아래 명령어를 통해 터미널에서 AWS CLI 인증 절차 진행
```bash
aws configure
```

---

## 4. Terraform Backend (bootstrap)
- infra-bootstrap 디렉토리로 이동
- S3, KMS, DynamoDB 기본 인프라 우선적으로 생성
```bash
cd infra-bootstrap
terraform init
terraform apply
```

---

## 5. backend.tf 설정

- 4단계에서 apply 후 출력된 아래 세개의 output 값을 infra/backend.tf 에 넣어서 수정

```
bucket
dynamodb_table 
kms_key_id
```

---

## 6. NS 등록

- 4단계에서 apply 후 출력된 route53_name_servers output 값을 가비아에 등록
- Route 53에서 도메인을 등록한 경우 생략 가능



---

## 7. Slack 알림 연동

- Amazon Q Developer in chat applications 콘솔에서 "클라이언트 구성" 클릭
- AWS와 연동할 Slack의 workspace 선택
- OAuth 인증을 통해 AWS가 해당 Slack workspace에 접근할 권한 승인
- AWS 콘솔에서 생성된 chat client의 WorkSpace ID 값을 terraform.tfvars 파일의 slack_team_id 값에 저장
- 연동시킨 slack workspace의 채널 중 알림을 받을 채널을 선택하여 들어간 후 주소창 확인
- 주소창의 WorkSpace ID 값 뒤에 나오는 채널 id 값을 terraform.tfvars 파일의 slack_channel_id 값에 저장
```
예시: https://app.slack.com/client/T0B171BDCP9/C0B1LDTFZ4N?ssb_vid=.80639255a1f4295e6f7b049bd41c9315

채널 id 값 -> C0B1LDTFZ4N
```

---
## 8. IAM Identity Center 활성화
- Grafana 연동을 위해서 IAM Identity Center 활성화 필요
- AWS IAM Identity Center 콘솔에 들어가서 Region 선택 후, 활성화 버튼 클릭
- Grafana 연동을 위해 추후 작업은 인프라 생성 후 11 단계에서 이어서 진행

---
## 9. 인프라 생성
- infra 디렉토리에서 아래 명령어를 통해 메인 인프라 생성
```bash
cd infra
terraform init -migrate-state
terraform apply
```

---
## 10. IAM Identity Center 사용자 비밀번호 설정
- 인프라가 생성되면 IAM Identity Center 콘솔로 이동
- 사용자 탭에 들어가서 생성된 사용자를 클릭 후, '이메일 확인 링크 전송' 버튼을 클릭 (해당 이메일은 terraform.tfvars 파일의 grafana_admin_email 으로 등록한 값)
- 해당 이메일로 발송된 메일을 통해 인증 진행
- 다시 IAM Identity Center 콘솔로 이동하여 새로고침 후 '암호 재설정' 버튼을 클릭
- 다시 발송된 메일을 통해 비밀번호 설정

---

## 11. Amazon Grafana 로그인

- Amazon Grafana 콘솔로 이동
- 생성된 워크스페이스의 'Grafana 워크스페이스 URL' 링크 클릭
- 10 단계에서 설정한 이메일과 비밀번호를 통해 로그인

---


## 12. GitHub Secrets 설정
- 9 단계에서 apply 후 출력된 output을 확인하여 GitHub Repository에서 아래와 같이 GitHub Secrets으로 등록

```
AWS_ROLE_ARN = terraform output 에서의 github_actions_role_arn 값
AWS_REGION = ap-northeast-2
ECR_REPOSITORY = ECR repo 이름
ECS_CLUSTER = ECS 클러스터 이름
ECS_SERVICE = ECS 서비스 이름
ECS_CONTAINER_NAME = task definition 안 container name
```

---

## 13. main / dev 브랜치에 merge

- main / dev 브랜치에 merge 시 CI /CD 절차 진행

---

## 14. CI

- main / dev 에 PR, push 시 CI 실행
1. GitHub Actions 실행
2. 소스코드 checkout
3. 테스트 실행


---

## 15. CD

- main / dev에  push 시 실행

1. GitHub Actions 실행
2. 소스코드 checkout
3. Docker 이미지 빌드
4. AWS 인증
5. ECR 로그인
6. 이미지를 ECR에 push
7. 기존 task definition JSON을 기준으로 새 이미지 URI를 넣은 새 revision 생성
8. ECS service를 새 task definition revision으로 update
9. ECS가 새 태스크를 띄우고 서비스의 상태가 안정화될때까지 대기

---

## 16.  배포 확인 

- 주소창에 http://alb_dns_name/actuator/health 확인
- AWS 콘솔에서 ECS 서비스 상태 확인
- AWS 콘솔에서 Target group health 확인
- AWS 콘솔에서 CloudWatch 로그 확인



