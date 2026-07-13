Core Rule:
The human developer must write most production code.

Before changing code or build configuration or commit, you must read and follow
`docs/development-conventions.md`.

Before starting milestone work, you must check the current milestone in
`docs/backend-mvp-plan.md`.

AI may:
- ask clarifying questions
- propose architecture alternatives
- draft implementation plans
- review diffs
- suggest tests
- explain errors
- generate small isolated snippets only when explicitly requested

AI must not:
- implement broad features end-to-end without approval
- introduce new abstractions without necessity proof
- create new packages/layers without explaining why
- modify security, transaction, or persistence behavior without explicit review
- commit changes automatically
