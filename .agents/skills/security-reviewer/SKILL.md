---
name: security-reviewer
description: Review authentication, authorization, JWT, Spring Security, rate limiting, secrets, actuator exposure, and infra security posture for all-in-market.
---

# Security Reviewer

## When to use

Use this skill for review of security posture in the Spring Boot backend and supporting infrastructure. The goal is portfolio credibility, not a full penetration test.

## Required inputs

- `src/main/java/com/example/allinmarket/common/config/SecurityConfig.java`
- `src/main/java/com/example/allinmarket/common/security/**`
- Auth controllers and services under buyer/seller packages
- `src/main/resources/application*.yml` and `application.yaml`
- `infra/**`, `Dockerfile`, and `docker-compose.yml` when deployment posture is relevant
- Security-related tests under `src/test/java/**`

## Review checklist

- Authentication: JWT signing, expiration, token parsing, logout/blacklist behavior, fail-open or fail-closed choices.
- Authorization: URL matcher order, role boundaries, owner checks in services, public endpoints, method-level guards if present.
- Rate limiting: key choice, proxy IP handling, failure status detection, Redis failure posture, body limits, PII in logs.
- Secrets: env var usage, test-only secrets, Terraform SSM handling, accidental hardcoded production values.
- Actuator and observability exposure: public endpoints, health detail, metrics exposure, and deployment constraints.
- Web posture: CORS, CSRF rationale for stateless APIs, content size limits, upload paths, and error response leakage.
- Tests: security filter tests, authorization tests, and negative cases.

## Output format

```markdown
### Security

#### Must Fix
- [ ] Finding title
  - Evidence: `path/to/File.java:line`
  - Risk:
  - Suggested change:
  - Verification:

#### Nice to Have
- [ ] Finding title
  - Evidence:
  - Portfolio angle:
```

## Review guidance

- Separate real vulnerabilities from portfolio documentation gaps.
- Flag public actuator exposure only with deployment context and suggested narrowing.
- Treat proxy IP extraction as security-sensitive when `X-Forwarded-For` can be spoofed.
- Do not suggest adding stateful sessions unless the project direction changes.

## Verification commands

```bash
./gradlew test --tests '*LoginRateLimitFilterTest'
./gradlew test --tests '*AuthControllerTest'
./gradlew test
```
