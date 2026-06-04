---
name: backend-review-orchestrator
description: Orchestrate a portfolio-quality backend review for the all-in-market Spring Boot repository and synthesize specialist findings into reusable handoff artifacts.
---

# Backend Review Orchestrator

## When to use

Use this skill when reviewing `all-in-market` as a backend developer portfolio project, especially before job applications, README updates, interview preparation, or technical improvement planning.

Do not use it to make code changes. The orchestrator produces review artifacts, coordinates specialist skills, and proposes small follow-up changes.

## Required inputs

- Repository root: `$REPO_ROOT` when set; otherwise use the current working directory (`./`) and confirm it is the repository root before reading or writing artifacts.
- Current user goal or review scope
- Existing `AGENTS.md` constraints
- Source, test, migration, documentation, k6, and infra files relevant to the review

## Usage notes

Run this skill from the repository root when possible:

```bash
cd "$REPO_ROOT"
```

If `$REPO_ROOT` is not set, treat `./` as the fallback repository root and verify expected paths such as `AGENTS.md`, `build.gradle`, and `src/main/java` exist.

## Workflow

1. Confirm scope
   - Treat the project as a Java 21, Spring Boot, PostgreSQL, Redis, JWT, RestDocs, k6, and Terraform backend portfolio.
   - Keep `AGENTS.md` short and unchanged unless the user explicitly asks for repo-wide instruction changes.
   - Do not rewrite the project or edit application code during review-only runs.

2. Produce the domain summary
   - Inspect package boundaries, main flows, persistence model, test strategy, infra, observability, and docs.
   - Write `_workspace/01_domain_summary.md`.

3. Fan out to specialists
   - Use `spring-architecture-reviewer` for package boundaries, service/facade layering, API shape, transaction boundaries, and domain consistency.
   - Use `persistence-performance-reviewer` for JPA mappings, migrations, indexes, locking, N+1 risk, batching, caching, and scheduler/outbox behavior.
   - Use `test-coverage-reviewer` for unit, slice, integration, RestDocs, concurrency, migration, and failure-path coverage.
   - Use `security-reviewer` for Spring Security, JWT, auth flows, rate limiting, secrets, actuator exposure, CORS/CSRF posture, and infra security clues.
   - Use `portfolio-docs-reviewer` for README, API docs, evidence quality, diagrams, trade-off explanations, and interview credibility.

4. Synthesize findings
   - Write `_workspace/02_review_findings.md`.
   - Separate `Must Fix` from `Nice to Have`.
   - Prefer concrete file paths and line numbers.
   - Include evidence, impact, and suggested verification for each important finding.

5. Plan improvements
   - Write `_workspace/03_improvement_plan.md`.
   - Sequence work into small, reviewable changes.
   - Include verification commands such as `./gradlew test`, targeted tests, RestDocs generation, and optional k6 runs.
   - Mark docs-only improvements separately from code-risk improvements.

## Output contract

The orchestrator owns these files:

- `_workspace/01_domain_summary.md`
- `_workspace/02_review_findings.md`
- `_workspace/03_improvement_plan.md`

Each output must be readable by a human reviewer without running another agent. Use direct paths, concise evidence, and portfolio framing.

## Quality bar

- Findings must be specific enough to turn into GitHub issues or small PRs.
- Claims about performance, security, or architecture must cite code, migrations, docs, tests, or configs.
- Do not imply production readiness where evidence is missing.
- Do not bury serious risks under documentation polish.
- Portfolio guidance should explain problem, cause, solution, result, and lesson learned.

## Verification

For review-only runs, verify artifacts rather than changing code:

```bash
test -f _workspace/01_domain_summary.md
test -f _workspace/02_review_findings.md
test -f _workspace/03_improvement_plan.md
test -f docs/harness/backend-review/team-spec.md
find .agents/skills -path '*/SKILL.md' -maxdepth 4 -print
```

For implementation follow-ups, prefer:

```bash
./gradlew test
./gradlew asciidoctor
```
