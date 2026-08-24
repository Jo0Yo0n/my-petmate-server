Core Rule:
The human developer must write most production code.

Before changing code or build configuration or commit, you must read and follow
`docs/development-conventions.md`.

Before starting milestone work, you must check the current milestone in
`docs/backend-mvp-plan.md`.

Before any M1 authentication, JWT, refresh-token, request-id, or error-handling
work, you must read `docs/m1-auth-contract-checklist.md` completely together
with the OpenAPI and error-response documents linked from it. Do not mark M1
complete until every required checklist item and verification gate is satisfied.

## Documentation Sources of Truth

Keep detailed information in its designated source document instead of copying it
into another document.

| Topic | Source document |
| --- | --- |
| Product goal, MVP scope, acceptance criteria, and domain concepts | `docs/seed.yaml` |
| Current milestone, implementation order, and completion criteria | `docs/backend-mvp-plan.md` |
| REST paths, requests, responses, token policy, and privacy policy | `docs/openapi.yaml` |
| REST error response and request-ID contract | `docs/error-response.md` |
| WebSocket connection, events, idempotency, and reconnection | `docs/websocket-protocol.md` |
| M1 authentication entry conditions and verification gates | `docs/m1-auth-contract-checklist.md` |
| Code, Git, and pull-request conventions | `docs/development-conventions.md` |

For ordinary feature work, read the development conventions, confirm the
current milestone and applicable `seed.yaml` requirements, then read the REST
or WebSocket contract and the error contract when relevant.

For M1 authentication, JWT, refresh-token, current-guardian, request-ID, or
error-handling work, follow this order:

1. Read `docs/m1-auth-contract-checklist.md` completely.
2. Read its linked `docs/openapi.yaml` and `docs/error-response.md` contracts.
3. Obtain the required human design review before implementation.
4. Satisfy every verification gate before marking M1 complete.

If documents conflict, update the documents before choosing an implementation.
Apply this precedence:

1. REST wire format: `docs/openapi.yaml`
2. REST errors and request IDs: `docs/error-response.md`
3. WebSocket protocol: `docs/websocket-protocol.md`
4. Product scope and acceptance criteria: `docs/seed.yaml`
5. Milestone order and completion criteria: `docs/backend-mvp-plan.md`
6. M1 entry and verification procedure: `docs/m1-auth-contract-checklist.md`
7. Repository working conventions: `docs/development-conventions.md`

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
