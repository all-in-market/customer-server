---
name: spring-architecture-reviewer
description: Review Spring Boot architecture, API boundaries, service/facade layering, transaction ownership, and domain consistency for the all-in-market backend portfolio.
---

# Spring Architecture Reviewer

## When to use

Use this skill for architecture and API review of the `all-in-market` Spring Boot backend. Focus on whether the code demonstrates clean, production-oriented backend engineering for a junior developer portfolio.

## Required inputs

- `src/main/java/com/example/allinmarket/**`
- Controller, service, facade, repository, entity, DTO, scheduler, and config classes
- `docs/asciidoc/**` and `README.md` when API shape or documentation consistency matters

## Review checklist

- Package boundaries: buyer, seller, common, and domain packages should have clear ownership.
- API design: endpoints, roles, response wrappers, pagination defaults, status codes, validation, and error semantics should be consistent.
- Service layering: controllers delegate to services/facades; services should not become mixed orchestration, persistence, and presentation objects without a reason.
- Transaction ownership: write flows should have explicit transaction boundaries; external calls and `REQUIRES_NEW` usage should be intentional and explained.
- Domain model: entity methods should express business state changes; DTO mapping should not hide lazy-load or N+1 risk.
- Scheduler and async flows: scheduled jobs, retries, and outbox-like processing should be idempotent and failure-aware.
- Portfolio credibility: strong decisions should be documented as trade-offs, not only implemented.

## Output format

Return findings using this shape:

```markdown
### Spring Architecture

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

- Prefer concrete examples from order, payment, cart, settlement, dashboard, auth, and product flows.
- Treat comments like "lock" or "N+1" as claims that need matching implementation evidence.
- When a behavior is good, note it as a strength only if it can be shown in source, tests, docs, or migration files.
- Do not recommend large rewrites. Favor small refactors, tests, or docs that make the design easier to trust.

## Verification commands

Use these for follow-up implementation work:

```bash
./gradlew test
./gradlew test --tests '*BuyerOrderServiceTest'
./gradlew test --tests '*BuyerPaymentServiceTest'
```
