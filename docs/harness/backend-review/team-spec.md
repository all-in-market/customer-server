# Backend Review Harness Team Spec

## Problem

`all-in-market`는 이커머스 백엔드 포트폴리오로 보여줄 소재가 많지만, 리뷰 관점이 한 파일에 섞이면 역할, 산출물, 우선순위가 흐려질 수 있다. 특히 아키텍처, JPA 성능, 트랜잭션, 보안, 테스트, 문서 품질을 같은 기준으로 훑으면 구체적인 파일 경로와 검증 명령이 빠지기 쉽다.

## Cause

단일 리뷰 스펙만으로는 각 전문 영역의 질문이 충분히 분리되지 않는다. 그 결과 리뷰 산출물이 매번 다른 이름으로 생기거나, must-fix와 nice-to-have가 섞이거나, 포트폴리오 문서에 필요한 “문제, 원인, 해결, 결과, 배운 점” 흐름이 누락될 수 있다.

## Solution

이 harness는 `Pipeline + Fan-out/Fan-in` 패턴을 사용한다.

1. Pipeline: `_workspace/01_domain_summary.md`로 도메인과 증거를 먼저 정리하고, `_workspace/02_review_findings.md`에서 findings를 합성한 뒤, `_workspace/03_improvement_plan.md`로 작은 PR 계획을 만든다.
2. Fan-out/Fan-in: 같은 코드 스냅샷을 기준으로 specialist가 독립 리뷰를 수행하고 orchestrator가 우선순위를 합친다.

역할은 다음처럼 분리한다.

| Role | Skill | Focus |
| --- | --- | --- |
| Orchestrator | `.agents/skills/backend-review-orchestrator/SKILL.md` | 산출물 순서, 우선순위, 최종 합성 |
| Spring architecture reviewer | `.agents/skills/spring-architecture-reviewer/SKILL.md` | 패키지 구조, API, 서비스 경계, 트랜잭션 |
| Persistence performance reviewer | `.agents/skills/persistence-performance-reviewer/SKILL.md` | JPA, migration, index, lock, N+1, cache |
| Test coverage reviewer | `.agents/skills/test-coverage-reviewer/SKILL.md` | 단위/통합/RestDocs/동시성 테스트 |
| Security reviewer | `.agents/skills/security-reviewer/SKILL.md` | 인증, 인가, rate limit, secret, actuator |
| Portfolio docs reviewer | `.agents/skills/portfolio-docs-reviewer/SKILL.md` | README, API docs, 기술 의사결정 서술 |

## Result

핵심 산출물은 번호가 붙은 `_workspace` 파일로 고정된다.

- `_workspace/01_domain_summary.md`: 도메인 요약, 강점, 리스크
- `_workspace/02_review_findings.md`: 파일 경로가 포함된 우선순위 findings
- `_workspace/03_improvement_plan.md`: 작은 PR 단위의 개선 계획과 검증 명령
- `_workspace/04_implementation_roadmap.md`: 테스트 우선 구현 순서

이 구조는 리뷰 결과를 GitHub issue, PR 계획, README 개선으로 바로 옮길 수 있게 한다.

## Lesson Learned

- 리뷰 전용 harness도 포트폴리오 문서처럼 문제와 결과를 설명해야 재사용 가치가 커진다.
- `AGENTS.md`에는 짧은 repo-wide 규칙만 두고, 세부 역할과 산출물 규칙은 repo-local skills와 harness docs에 둔다.
- 실패하거나 미검증인 항목은 숨기지 말고 `_workspace` 산출물에 명령, 원인, 다음 검증 방법을 남긴다.
- 향후에는 specialist별 중간 산출물이 많아질 때 `_workspace/NN_{role}_{artifact}.md` 형식으로 확장한다.
