# Pre-Landing Code Review: Session Search Cache + TTL

## Scope

- Review target: full `git diff origin/main`, plus untracked `SearchCacheTest.java` and connected repository/Search sources.
- Files: 10 tracked changed files and 1 new test file; tracked diff is 267 insertions / 34 deletions, with 253 lines in the new cache test.
- Design evidence: `docs/ARCHITECTURE.md`, `plans/2026-10-28-session-search-cache.md`, and `plans/reports/cook-261028-0732-cache-ttl.md`.
- Focus: correctness, concurrency ordering, cache/API contracts, backward compatibility, input boundaries, data exposure, performance, and regression coverage.
- Validation commands were intentionally not rerun because the independent test agent owns execution. `git diff --check origin/main` passed during review.

## Overall Assessment

Pre-Landing Review: No issues found (0 critical, 0 informational).

The implementation matches the accepted cache/refresh contract. The cache is process-local and keyed by trimmed query plus clamped page/page size; successful HTTP responses, including empty pages, are cached from response receipt for 30 minutes; cache hits remap raw DTOs against current favorite IDs. Explicit refresh bypasses cached data, invalidates only the refreshed query after a usable success, and uses per-query generations plus synchronized map access to prevent detached callbacks from repopulating invalidated pages. HTTP/network failures preserve existing cached pages, while the ViewModel retains displayed rows and pagination state on refresh failure.

## Specification Compliance

| Requirement | Status | Evidence |
|---|---|---|
| RAM cache by query/page/per-page | PASS | `PhotoRepositoryImpl.java:63-78,132-157` |
| 30-minute receipt-based monotonic TTL; no sliding reads | PASS | `PhotoRepositoryImpl.java:55,142-149,197-214`; deterministic boundary tests in `SearchCacheTest.java:166-218` |
| Cache empty successful responses | PASS | `PhotoRepositoryImpl.java:201-214,246-253`; `SearchCacheTest.java:146-164,201-209` |
| Merge current favorites on every cache read | PASS | `PhotoRepositoryImpl.java:241-253`; `SearchCacheTest.java:61-74` |
| Share ordinary identical in-flight requests | PASS | `PhotoRepositoryImpl.java:137-140,157`; `SearchCacheTest.java:77-83` |
| Same-query Search and swipe refresh bypass cache | PASS | `SearchFragment.java:129-167`; `SearchViewModel.java:52-55,157-188` |
| Successful refresh invalidates only that query's old pages | PASS | `PhotoRepositoryImpl.java:205-214`; `SearchCacheTest.java:100-118` |
| Refresh error preserves displayed rows, pagination, and prior cache | PASS | `SearchViewModel.java:159-178,236-246,255-258`; `SearchCacheTest.java:131-144`; `SearchViewModelTest.java:101-134` |
| Detached callbacks cannot repopulate refreshed cache | PASS | `PhotoRepositoryImpl.java:151-157,205-216`; `SearchCacheTest.java:100-128` |
| Lazy expiry only; no proactive fetch | PASS | Cache age is checked only inside `searchPhotos` at `PhotoRepositoryImpl.java:137-149`; no timer or observer was introduced. |
| No schema/disk-cache change | PASS | Diff contains no Room entity, DAO, migration, or database version change. |

## Pass 1 — Critical / Blocking

None.

- Concurrency: shared cache state is accessed through synchronized repository methods/blocks; generation identity prevents stale normal or refresh callbacks from writing after refresh.
- Error boundaries: HTTP, Retrofit failure, and favorite-read runtime failures produce explicit `Resource.ERROR`; refresh cache invalidation happens only after a usable mapped result.
- Security/input: no new external trust boundary, persistence, authorization surface, secret logging, or raw exception/stack trace exposure was introduced.
- Data safety: cached DTO lists are copied from the Retrofit response and mapped into fresh domain lists before exposure.

## Pass 2 — Informational / Non-Blocking

None.

- API contracts: the new force-refresh overload has one production implementation and all callers resolve to the intended overload; the original three-argument method remains intact.
- Performance: lookup is bounded per key and avoids duplicate ordinary calls. The acknowledged lazy-eviction/process-lifetime memory behavior is part of the accepted design, not a newly discovered defect.
- Regression coverage: repository tests cover key clamping, Featured, empty success, HTTP/network errors, favorite-read failures, pending sharing, query-scoped invalidation, callback ordering, exact TTL boundaries, independent page expiry, and failed refresh TTL preservation. ViewModel/Fragment tests cover retained rows, spinner completion, duplicate refresh suppression, retry behavior, and same-query Search routing.

## Scout Findings

- Only `PhotoRepositoryImpl` implements `PhotoRepository`; no alternate production implementer is broken by the added overload.
- Search calls originate from `SearchViewModel`; direct API calls remain isolated in `PhotoRepositoryImpl`.
- Production disk work is serialized by `AppExecutors`' single-thread executor, while repository synchronization also protects calls arriving from Retrofit/UI threads.
- Existing favorites observation continues to refresh displayed domain objects independently of cache reads.

## Metrics

- Type coverage: N/A (Java project; no configured type-coverage metric inspected).
- Test coverage: N/A (coverage percentage not generated in this review).
- Lint issues: not rerun; supplied delivery evidence reports the full lint/build/unit gate passed on an identical source copy.
- Review findings: 0 critical, 0 informational.

## Recommended Actions

1. Proceed to the test agent's independent gate and shipping workflow if that gate confirms the reviewed revision.

## Unresolved Questions

- None affecting landing. Device/emulator pull-gesture behavior and the live Pixabay API remain unverified, as already disclosed in the delivery evidence.
