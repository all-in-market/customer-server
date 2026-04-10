# GitHub Rules

---

## Dev, Prod 전략

**main (prod)**

- 운영 단계 브랜치
- dev에서만 merge
- 팀 전원의 approve 필요
- PR Conversation 전부 Resolve 필요
- PR만 허용, 직접 push 불가능

---

**dev**

- ***feature*** 가 통합되어 모이는 단계
- 2명 이상의 approve 필요
- PR만 허용, 직접 push 불가능

---

**feature**

- 각자 기능 개발하는 개인 브랜치
- 기능 완성 후 삭제
- ***{feature/도메인-메서드/기능} 형식으로 네이밍***

---

## Commit Convention

| 작업 타입 | 작업 내용 |
| --- | --- |
| feat | 새로운 기능 추가 |
| fix | 버그 수정 |
| docs | 문서 수정 |
| style | 코드 포맷팅, 세미콜론 누락 |
| refactor | 기능 변경 없는 코드 개선 |
| test | 테스트 코드 추가/수정 |
| chore | 빌드 수정, 패키지 매니저 설정 외 기타 |
| release | 배포 준비 |

---

## Commit Message Rules

<aside>

feat: 벤더 상품 등록 API 구현

- 상품 기본 정보 및 옵션 등록

- 벤더별 상품 상태 관리 (PENDING/APPROVED/REJECTED)

- 이미지 업로드 S3 연동 Resolves: #42커밋 메시지 규칙:

</aside>

- subject는 50자 이하, 동사 원형 시작 (한글은 명사형 종결)
- body는 무엇을, 왜 변경했는지 기술 (어떻게는 코드로 설명)
- 한 커밋에 하나의 논리적 변경만 포함

---

## Commit Unit

- PR은 파일 10개 이하, diff 400줄 이하
- 커밋은 기능 단위

---

## Issue

- 마일스톤 (체크리스트) 진행 상황 확인
- 오류 시 담당자 지정 이슈 등록