# Backend Review Harness Team Spec

## Purpose

This harness reviews `all-in-market` as a junior backend developer portfolio project. It helps turn a broad Spring Boot codebase into concrete credibility signals for backend job applications: architecture, APIs, persistence, transactions, performance, tests, security, observability, and documentation.

## Pattern

Use `Pipeline + Fan-out/Fan-in`.

1. Pipeline: summarize the domain first, synthesize findings second, create an improvement plan third.
2. Fan-out/Fan-in: run specialist reviews independently from the same repository snapshot, then merge them into one prioritized findings artifact.

This keeps the workflow shallow, reusable, and auditable through markdown handoffs.

## Roles

| Role | Skill | Primary output |
| --- | --- | --- |
| Orchestrator | `.agents/skills/backend-review-orchestrator/SKILL.md` | `_workspace/01_domain_summary.md`, `_workspace/02_review_findings.md`, `_workspace/03_improvement_plan.md` |
| Spring architecture reviewer | `.agents/skills/spring-architecture-reviewer/SKILL.md` | Architecture/API/transaction findings |
| Persistence performance reviewer | `.agents/skills/persistence-performance-reviewer/SKILL.md` | Persistence, migration, index, lock, N+1, cache, and load findings |
| Test coverage reviewer | `.agents/skills/test-coverage-reviewer/SKILL.md` | Test gaps and verification strategy |
| Security reviewer | `.agents/skills/security-reviewer/SKILL.md` | Auth, authorization, rate-limit, secret, actuator, and infra security findings |
| Portfolio docs reviewer | `.agents/skills/portfolio-docs-reviewer/SKILL.md` | README, API docs, decision writing, and evidence findings |

## Handoff Files

- `_workspace/01_domain_summary.md`: repository map, strengths, risks, and reusable review scope.
- `_workspace/02_review_findings.md`: prioritized concrete findings, separated into `Must Fix` and `Nice to Have`.
- `_workspace/03_improvement_plan.md`: small PR-sized plan with verification commands.

Specialist notes may be embedded directly in `_workspace/02_review_findings.md` unless a future review needs separate branch artifacts.

## Review Rules

- Do not edit application code during a review-only run.
- Do not rewrite the whole project.
- Keep `AGENTS.md` short; use this team spec and skills for deeper guidance.
- Prefer file paths and line numbers over broad claims.
- Mark serious correctness, security, transaction, and data-integrity issues as `Must Fix`.
- Mark polish, docs, performance experiments, and optional refactors as `Nice to Have` unless they hide a real production risk.
- Include verification commands, especially `./gradlew test`.

## Failure Policy

- If the repository cannot build or tests cannot run, record the exact command and failure in `_workspace/02_review_findings.md` or `_workspace/03_improvement_plan.md`.
- If a specialist cannot determine impact from static inspection, label the finding as "needs verification" and propose a specific test, query plan, or command.
- If findings conflict, prioritize user safety and data correctness first, then portfolio clarity.

## Acceptance Checklist

- Every generated `SKILL.md` has YAML frontmatter with `name` and `description`.
- The three `_workspace` handoff files exist.
- Findings include concrete paths.
- Must-fix and nice-to-have items are separated.
- The improvement plan is reviewable as small follow-up PRs.
- Verification commands are included.
