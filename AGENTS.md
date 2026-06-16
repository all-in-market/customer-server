## WHAT
This repository is a Java/Spring Boot backend portfolio project for high-traffic e-commerce service named 'all-in-market'.

## WHY
The goal is to demonstrate production-oriented backend engineering:
clean architecture, reliable persistence, secure APIs, testability, performance awareness, and clear technical documentation.

## HOW
- Prefer small, reviewable changes.
- Do not rewrite the entire project unless explicitly requested.
- After code changes, run `./gradlew test` when possible.
- For API changes, update related docs or examples.
- For persistence changes, check transaction boundaries, indexes, N+1 risks, and migration impact.
- For portfolio documentation, explain problem, cause, solution, result, and lesson learned.
- Harness skill source of truth: `.agents/skills/harness/SKILL.md`; `.codex/skills/harness/SKILL.md` is only a forwarding stub.
