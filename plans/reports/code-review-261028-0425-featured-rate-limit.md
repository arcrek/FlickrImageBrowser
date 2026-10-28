# Code Review Summary

## Scope

- Focus: pending featured-feed rate limiting and pagination retry changes.
- Reviewed: `PixabayRateLimitInterceptor`, its unit tests, `PixabayClient`, `PhotoRepositoryImpl`, `SearchViewModel`, `SearchFragment`, relevant ViewModel/API tests, and `docs/ARCHITECTURE.md`.
- Size inspected: 1,346 lines across the directly relevant production, test, and documentation files. The two untracked interceptor files were read separately because `git diff` does not include them.
- Verification evidence reused: the controller reports one prior full build/test/lint pass and 17 focused tests passing; later full checks were blocked by WSL cache failures. No full build was rerun in this review, per task ownership.

## Overall Assessment

The rolling-window limiter is synchronized correctly, uses a monotonic clock, counts failed and concurrent outgoing network attempts, and applies the documented minimum/extended 429 cooldown. The pagination guard also prevents scroll-triggered retry loops and explicit retry clears the guard before reloading the failed page.

The one user-visible Snackbar state bug found in the initial review has been resolved in the follow-up diff. No security, authorization, data-exposure, API-contract, limiter race, or remaining ship blocker was found in the inspected scope.

## Critical Issues

None under the pre-landing critical checklist.

## High Priority

None remaining.

### Resolved During Review

- [`SearchFragment.java:167`] `renderState` now dismisses and clears the previously retained pagination-error Snackbar before processing each subsequent non-null UI state. A new-query `LOADING` emission therefore removes the stale error/action before `lastQuery` can be retried from it.
- [`SearchFragment.java:250`] `onDestroyView` also dismisses and clears the retained Snackbar before releasing the binding, closing the lifecycle leak path.
- [`SearchFragmentTest.java:19`] The focused JVM regression test reflectively injects a mocked binding and retained Snackbar, renders the new-query `LOADING` state, and verifies both `dismiss()` and field clearing. It supplies mocks for all binding fields touched by that branch.
- [`SearchFragmentTest.java:40`] A second focused test verifies `onDestroyView()` dismisses and clears the retained Snackbar. Source review found no defect in either test; execution is pending the tester's run.

## Medium Priority

None.

## Low Priority

None.

## Edge Cases Scouted

- Concurrent calls share one synchronized deque; at most 100 attempts acquire capacity within the rolling window.
- Network failures consume budget before `chain.proceed`, matching the documented outgoing-attempt policy.
- A 429 starts at least a 60-second cooldown and takes the maximum of the remaining cooldown, numeric `Retry-After`, and numeric `X-RateLimit-Reset`.
- Invalid/negative headers fall back to the 60-second minimum; numeric overflow also safely falls back rather than weakening that minimum.
- `addNetworkInterceptor` applies the limiter to actual OkHttp network attempts/follow-ups; the static instance shares the process budget across services.
- Initial-page errors still retry page 1, while pagination errors preserve accumulated photos and only retry the failed next page after explicit action.
- New-query submission resets the pagination failure guard. The defect is confined to the fragment retaining the old visual/action surface.

## Pre-Landing Checklist

Pre-Landing Review: No unresolved issues found.

- Concurrency: shared limiter state is synchronized; ViewModel mutation remains on its established LiveData/UI-thread path.
- Error boundaries: local limiter `IOException` propagates through Retrofit to the repository error resource; 429 responses remain available to repository handling.
- Backwards compatibility: no exported interface or schema change.
- Input/auth/data safety: no new trust boundary, persisted input, secret exposure, or authorization path.
- Query/performance: no database loop or unbounded query added; limiter operations are bounded to 100 deque entries.

## Recommended Actions

1. Run the two new fragment tests with the focused ViewModel/interceptor tests. Reuse the earlier full-gate result unless the fix touches shared contracts; otherwise rerun the relevant broader gate once the WSL cache is available.

## Metrics

- Type coverage: not measured (Java project; no configured type-coverage metric inspected).
- Test coverage: not measured; 17 limiter/ViewModel tests reported passing, with 2 focused fragment regression tests pending execution.
- Linting issues: none reported by the earlier successful lint run; this review did not rerun lint.

## Unresolved Questions

- The follow-up fix and its two direct regression tests are source-verified; final execution status is pending the tester's output.
