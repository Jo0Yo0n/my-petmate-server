<!--
Sync Impact Report
- Version change: unratified template -> 1.0.0
- Modified principles:
  - Template Principle 1 -> I. Scope and Governing Sources
  - Template Principle 2 -> II. Stable Backend Platform
  - Template Principle 3 -> III. Simple, Feature-Local Design
  - Template Principle 4 -> IV. Secure and Private Boundaries
  - Template Principle 5 -> V. Verifiable Incremental Delivery
- Added sections:
  - Project Constraints
  - Development Workflow and Quality Gates
- Removed sections: none; template placeholders and example comments were replaced
- Template consistency:
  - .specify/templates/plan-template.md: unchanged; Constitution Check already applies these gates
  - .specify/templates/spec-template.md: unchanged; scenarios, requirements,
    and success criteria align
  - .specify/templates/tasks-template.md: unchanged; generated tasks can include
    required verification
  - .specify/templates/checklist-template.md: unchanged; checklist categories
    are generated per feature
- Follow-up TODOs: none
-->

# My Petmate Server Constitution

## Core Principles

### I. Scope and Governing Sources

Every feature MUST remain within the MVP scope defined by `docs/seed.yaml` and the
current milestone in `docs/backend-mvp-plan.md`. Before planning milestone work,
the current milestone and its completion criteria MUST be checked. Before changing
code or build configuration, `AGENTS.md` and `docs/development-conventions.md` MUST
be read and followed.

The governing sources have distinct responsibilities:

- `docs/seed.yaml` defines the product-level MVP scope and exclusions.
- `docs/backend-mvp-plan.md` defines milestone order, current status, APIs, and
  milestone completion criteria.
- `docs/development-conventions.md` defines implementation and delivery rules.
- `AGENTS.md` defines agent permissions and operating boundaries.
- `specs/<feature>/` refines one approved feature without silently expanding any
  governing source.

When documents conflict, the more specific source applies only within its stated
responsibility. A feature specification MUST NOT override product exclusions,
the current milestone, development conventions, or agent permissions.

### II. Stable Backend Platform

The backend MUST use Java 21 LTS, Spring Boot 3.x, and Gradle Kotlin DSL. MVP
persistence MUST use PostgreSQL with Flyway migrations. Formatting MUST use
Spotless with Google Java Format, and `./gradlew check` MUST include formatting
verification.

Plans MUST preserve these platform decisions unless an explicit constitution
amendment and human review approve a change. A feature MUST NOT add an alternate
database, build system, or application framework for convenience.

### III. Simple, Feature-Local Design

The first correct implementation MUST remain local to the feature and use the
smallest structure that satisfies verified requirements. A new package, layer,
factory, interface, or shared abstraction MUST have a concrete current consumer
and a written necessity rationale in the implementation plan.

Types and methods SHOULD remain package-private unless another package has a
demonstrated need. Speculative extensibility, organizational-only abstractions,
and architecture changes unrelated to the current feature are prohibited.

### IV. Secure and Private Boundaries

Inputs MUST be validated at API and message boundaries. Authorization MUST be
verified against the authenticated guardian and the selected dog or match before
protected data is read or changed. Passwords and refresh tokens MUST be stored
only in their approved hashed forms.

Exact real-time location MUST NOT be exposed. Responses may contain only the
approved approximate distance and primary activity area. Security,
authentication, transaction, database schema, and persistence behavior changes
MUST receive explicit review and MUST be identified in the plan, tasks, and pull
request checklist.

### V. Verifiable Incremental Delivery

Each feature MUST define independently verifiable user scenarios, measurable
success criteria, failure and boundary behavior, and a manual validation path.
Plans and tasks MUST trace work back to those scenarios and to the active
milestone's completion and test criteria.

Before code changes are handed off, `./gradlew spotlessApply` MUST be run. Before
a pull request is opened, `./gradlew check` MUST pass. Relevant API or application
behavior MUST also be exercised manually, and the commands and observed results
MUST be recorded in the pull request Verification section. A failed gate MUST be
fixed or documented as a pre-existing blocker; it MUST NOT be silently bypassed.

## Project Constraints

- This repository contains the backend API only. Mobile application work is out
  of scope for this repository.
- PostgreSQL is the only MVP database.
- Recommendation, like, match, chat, and appointment flows operate on one
  selected dog pair at a time.
- Recommendations are calculated on request and MUST NOT depend on a precomputed
  batch job for the MVP.
- Exact location disclosure, social login, phone verification, push
  notifications, map exploration, calendar integration, payments, ratings,
  group walks, an admin web UI, and document proof review remain out of scope
  unless `docs/seed.yaml` is explicitly amended.
- Branches, commits, and pull requests MUST follow
  `docs/development-conventions.md` and `.github/pull_request_template.md`.

## Development Workflow and Quality Gates

1. Confirm the active milestone in `docs/backend-mvp-plan.md` before creating or
   changing a feature specification.
2. Create one focused feature directory under `specs/` and define user-visible
   behavior and exclusions in `spec.md` before choosing implementation details.
3. Resolve material ambiguities before planning. Any remaining assumption MUST be
   explicit and testable in the specification.
4. In `plan.md`, complete the Constitution Check and identify data, API,
   authorization, transaction, persistence, privacy, and migration impacts.
5. Generate `tasks.md` with exact file paths, dependencies, user-story mapping,
   verification steps, and explicit review tasks for sensitive changes.
6. Analyze `spec.md`, `plan.md`, and `tasks.md` for contradictions and missing
   coverage before implementation begins.
7. Implement only the approved task scope, then run formatting, automated checks,
   and the feature's manual validation path.
8. Use the repository pull request template and record security, persistence,
   transaction, verification, and agent-assisted change details where applicable.

## Governance

This constitution governs all Spec Kit specifications, plans, tasks, checklists,
and implementation reviews in this repository. The governing sources named in
Principle I remain authoritative for their stated responsibilities; generated
artifacts MUST NOT weaken them.

Amendments require an explicit proposal that explains the affected principles,
the reason for the change, and any required migration of existing specifications
or templates. Every amendment MUST update the Sync Impact Report, version, and
Last Amended date.

Versioning follows semantic versioning:

- MAJOR: a principle is removed or redefined incompatibly.
- MINOR: a principle or governance requirement is added or materially expanded.
- PATCH: wording is clarified without changing required behavior.

Every implementation plan MUST pass the Constitution Check before design work
proceeds and MUST be checked again after design artifacts are produced. A
necessary exception MUST be recorded in the plan's Complexity Tracking section
with its rationale and the simpler alternative that was rejected. Reviewers MUST
verify constitution compliance before accepting implementation work.

**Version**: 1.0.0 | **Ratified**: 2026-07-13 | **Last Amended**: 2026-07-13
