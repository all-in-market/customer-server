---
name: test-coverage-reviewer
description: Review unit, slice, integration, RestDocs, concurrency, persistence, and security test coverage for the all-in-market Spring Boot portfolio.
---

# Test Coverage Reviewer

## When to use

Use this skill to judge whether the repository's tests convincingly support a backend portfolio: important flows, failure paths, transaction behavior, security filters, and documentation generation.

## Required inputs

- `src/test/java/com/example/allinmarket/**`
- `src/test/resources/application-test.yml`
- `build.gradle`
- `docs/asciidoc/**`
- `src/main/java/**` for code paths being tested

## Review checklist

- Coverage balance: service unit tests, controller/RestDocs tests, repository/integration tests, security tests, scheduler tests.
- Critical flows: signup/login/logout, cart, order creation, stock decrease/release, payment confirm/refund, settlement/payout, dashboard/outbox, restock notifications.
- Failure paths: unauthorized access, owner mismatch, duplicate operations, invalid status transitions, missing data, serialization failure, Redis/database failure.
- Transaction/concurrency behavior: locks, optimistic retries, duplicate payment prevention, inventory races, scheduler idempotency.
- Test realism: excessive mocks should not be the only evidence for JPA, migrations, or transaction behavior.
- Documentation tests: generated snippets should match published API docs.
- Build reliability: `./gradlew test` should be the default verification command.

## Output format

```markdown
### Test Coverage

#### Must Fix
- [ ] Finding title
  - Evidence: `path/to/Test.java:line`
  - Missing risk:
  - Suggested test:
  - Verification:

#### Nice to Have
- [ ] Finding title
  - Evidence:
  - Portfolio angle:
```

## Review guidance

- Do not demand 100 percent coverage.
- Prioritize tests that prove high-risk behavior and tell a strong backend story.
- Call out when H2 plus `ddl-auto: create-drop` cannot validate PostgreSQL-specific Flyway migrations or indexes.
- Recommend Testcontainers or focused repository tests only where they add concrete confidence.

## Verification commands

```bash
./gradlew test
./gradlew test --tests '*LoginRateLimitFilterTest'
./gradlew test --tests '*BuyerPaymentServiceTest'
./gradlew asciidoctor
```
