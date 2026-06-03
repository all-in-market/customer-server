---
name: portfolio-docs-reviewer
description: Review README, API docs, diagrams, performance evidence, and technical-decision writing for backend job application credibility.
---

# Portfolio Docs Reviewer

## When to use

Use this skill when reviewing how well the repository communicates backend engineering value to recruiters, interviewers, and senior reviewers.

## Required inputs

- `README.md`
- `docs/asciidoc/**`
- `docs/rules/**`
- `docs/samples/**`
- `INFRA_CICD_SETUP_GUIDE.md`
- `k6/**`, `infra/**`, and observability dashboards when referenced in docs
- Review findings from other specialist skills

## Review checklist

- First impression: project purpose, architecture, tech stack, how to run, and what problem the service solves.
- Evidence: diagrams, API docs, test commands, k6 results, metrics dashboards, Terraform, CI/CD, and migration strategy.
- Technical decisions: problem, cause, solution, result, and lesson learned.
- Honesty: distinguish implemented features, mocked services, planned improvements, and production assumptions.
- API docs: RestDocs coverage should match main endpoints and generated docs should be discoverable.
- Portfolio fit: emphasize backend judgment over feature volume.
- Readability: avoid overloaded root docs; point to deeper docs for details.

## Output format

```markdown
### Portfolio Documentation

#### Must Fix
- [ ] Finding title
  - Evidence: `path/to/File.md:line`
  - Credibility impact:
  - Suggested change:
  - Verification:

#### Nice to Have
- [ ] Finding title
  - Evidence:
  - Portfolio angle:
```

## Review guidance

- Prefer a small README improvement plan over a full rewrite unless explicitly requested.
- If docs claim production-grade behavior, require links to code, tests, migration, k6 result, or infra.
- Suggest adding "known limitations" when it makes the project more credible.
- Keep recommendations usable by a junior developer preparing for interviews.

## Verification commands

```bash
./gradlew asciidoctor
./gradlew test
```
