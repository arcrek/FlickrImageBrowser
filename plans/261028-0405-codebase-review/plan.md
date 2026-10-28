---
title: "Codebase review remediation"
description: "Remediate five source-verified P2 defects while preserving existing callers and demo mode."
status: in-progress
priority: P2
effort: 1d
branch: main
tags: [bugfix, frontend, backend, accessibility, security]
blockedBy: []
blocks: []
created: 2026-10-28
---

# Codebase Review Remediation

**Plan status:** IN PROGRESS — all repairs and local gates are complete; on-device verification is pending. Implementation was authorized by the user’s `ak-fix` request on 2026-10-28.

## Outcome

Fix the five confirmed P2 findings in the [code review report](../reports/code-review-261028-0405-codebase.md): retained search data after recreation, favorite updates during error states, query disclosure in demo image URLs, overbroad Room exception handling, and undersized image-button touch targets. Preserve the intentional classroom demo path documented in [`docs/TEAM_WORKLOAD_PLAN.md:80`](../../docs/TEAM_WORKLOAD_PLAN.md#L80), existing interfaces, and current behavior outside these defects.

## Scope and Dependencies

- One implementation phase; no new dependencies, breaking API changes, schema changes, or removal of demo mode.
- The local gate now passes using the isolated source/cache route documented in the [fix report](../reports/fix-261028-0443-codebase-review.md). Device verification depends on connecting an Android device or emulator.
- Previous command: `./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebugAndroidTest`; it stopped before tests/lint because resource tasks hit `NullPointerException: File.getParentFile` plus Gradle-cache I/O errors.

## Phase

| Phase | Status | Depends on | Deliverable |
|---|---|---|---|
| 1. [Fix confirmed defects](./phase-01-fix-confirmed-defects.md) | Local gates passed; device checks pending | Connected Android device/emulator for runtime checks | Minimal fixes, focused regression tests, and accessibility validation |

## Acceptance Criteria

- Recreated search UI renders retained data in `LOADING` and `ERROR`, and favorite changes update rows without losing the active resource status/message.
- Demo mode remains available, but no user query text is included in requests to Picsum.
- Only expected uniqueness conflicts are suppressed; unrelated persistence failures reach the initiating UI without crashing or dismissing the form as successful.
- All identified image buttons in the four layouts expose at least 48dp touch regions without adding dependencies.
- Focused tests pass, followed by `testDebugUnitTest`, `lintDebug`, and `assembleDebugAndroidTest` after environment repair.

## Rollback

Revert the single phase as one scoped change; no data migration, public contract, or dependency rollback is required.

## Unresolved Questions

None.

## Execution

Scout and diagnosis complete: the five reported source paths are unchanged. Implementation → focused regressions → full Gradle gate → local diff review → finalize. Existing void write methods remain compatible; per-operation observable results will be added for collection forms. No schema or stored data changes.

Implementation, focused regressions, full local Gradle gate, diff review and documentation sync are complete. Legacy saved demo URLs are sanitized on read without a database migration. Instrumentation recreation/bounds tests compile, but no device is connected to execute them. See the [verification report](../reports/fix-261028-0443-codebase-review.md).
