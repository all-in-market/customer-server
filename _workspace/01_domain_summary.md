# Domain Summary

## Project Identity

`all-in-market` is a Java 21 / Spring Boot backend portfolio for a high-traffic e-commerce service with buyer and seller flows. The project is structured around order, payment, cart, product, refund, restock notification, settlement, payout, dashboard, and auth use cases.

## Main Surfaces

- Application source: `src/main/java/com/example/allinmarket/**`
- Domain model and repositories: `src/main/java/com/example/allinmarket/domain/**`
- Buyer and seller API layers: `src/main/java/com/example/allinmarket/buyer/**`, `src/main/java/com/example/allinmarket/seller/**`
- Security and configuration: `src/main/java/com/example/allinmarket/common/config/**`, `src/main/java/com/example/allinmarket/common/security/**`
- Persistence migrations: `src/main/resources/db/migration/**`
- Tests: `src/test/java/com/example/allinmarket/**`
- API docs: `docs/asciidoc/**`
- Performance/infra evidence: `k6/**`, `infra/**`, `infra-grafana/**`, `docker-compose-k6.yml`

## Current Strengths

- Clear e-commerce domain breadth: buyer cart/order/payment/refund and seller product/dashboard/settlement/payout.
- Flyway migrations exist, including unique constraints and targeted indexes such as payment success uniqueness and outbox polling indexes.
- RestDocs and Asciidoctor are wired through Gradle for API documentation.
- Security has JWT, role-based URL matchers, password encoding, logout handling, and a login rate-limit filter.
- Observability and performance assets exist: Actuator, Micrometer Prometheus/CloudWatch, Grafana Terraform, and k6 scenarios.
- Some high-risk workflows already show production-oriented thinking: outbox-style dashboard/history work, scheduled batch jobs, retries, and stock release on payment failure.

## Current Credibility Risks

- Some source comments and method names claim locking, but the product stock query has the pessimistic lock annotation commented out.
- Tests are plentiful, but several critical persistence and transaction guarantees appear to rely on mocks or H2 instead of PostgreSQL/Flyway behavior.
- README explains flows and APIs but does not yet foreground architecture decisions, operational evidence, known limitations, or exact verification commands.
- Security posture needs clearer evidence around actuator exposure, proxy IP handling, and public endpoint rationale.
- Performance work exists but needs a stronger bridge from k6 scripts/metrics to documented results and lessons learned.

## Recommended Harness Shape

Use a `Pipeline + Fan-out/Fan-in` harness:

1. Summarize domain and evidence.
2. Review architecture, persistence/performance, tests, security, and docs independently.
3. Synthesize must-fix and nice-to-have findings.
4. Convert findings into a small, reviewable improvement plan.
