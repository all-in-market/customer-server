---
name: persistence-performance-reviewer
description: Review JPA persistence, Flyway migrations, locking, indexes, N+1 risks, caching, batching, schedulers, and k6 evidence for the all-in-market backend.
---

# Persistence Performance Reviewer

## When to use

Use this skill when the review needs database credibility: transactions, migrations, query plans, index coverage, locking, N+1 behavior, Redis/cache usage, scheduler batch behavior, or load-test evidence.

## Required inputs

- `src/main/java/com/example/allinmarket/domain/**`
- `src/main/java/com/example/allinmarket/**/service/**`
- `src/main/java/com/example/allinmarket/common/scheduler/**`
- `src/main/resources/db/migration/**`
- `src/main/resources/application*.yml` and `application.yaml`
- `k6/**` and `docker-compose-k6.yml` when load evidence is in scope

## Review checklist

- Migrations: schema constraints, partial unique indexes, polling indexes, FK coverage, and migration portability.
- Repository queries: pageable joins, fetch joins, entity graphs, count-query behavior, LIKE scans, and projection opportunities.
- Locking: pessimistic/optimistic locks must match concurrency-sensitive writes and comments.
- Transactions: check isolation assumptions, `REQUIRES_NEW` side effects, external calls inside transactions, retries, and rollback behavior.
- N+1 risk: DTO mapping over lazy relations, pageable entity reads, and missing fetch plans.
- Batch and scheduler behavior: fixed page size, idempotency, ordering, dead-letter/retry policy, and duplicate processing under multiple nodes.
- Caching: Redis keys, invalidation, stale data policy, and failure posture.
- Performance evidence: k6 scripts, metric names, p95/p99 targets, and before/after results.

## Output format

```markdown
### Persistence and Performance

#### Must Fix
- [ ] Finding title
  - Evidence: `path/to/File.java:line`
  - Impact:
  - Suggested change:
  - Verification:

#### Nice to Have
- [ ] Finding title
  - Evidence:
  - Portfolio angle:
```

## Review guidance

- Be precise about missing indexes: name the query predicate and the migration that should support it.
- Flag comments that claim locking when the annotation is absent.
- Prefer adding focused tests or migration checks over broad rewrites.
- Treat k6 scripts as evidence only when results and thresholds are documented.

## Verification commands

```bash
./gradlew test
./gradlew test --tests '*Repository*'
docker compose -f docker-compose-k6.yml up --abort-on-container-exit
```
