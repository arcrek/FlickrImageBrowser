# Phase 1 — Fix Confirmed Defects

**Status:** IN PROGRESS — implementation and local gates complete; device checks pending
**Priority:** P2
**Estimate:** 1 day
**Dependency:** Local Gradle gates pass through the isolated verification route. Runtime checks require a connected Android device/emulator.

## Context and Data Flow

- `SearchViewModel` retains photos across recreation, while a new adapter receives lists only on `SUCCESS` ([`SearchFragment.java:160`](../../app/src/main/java/vn/edu/usth/flickrimagebrowser/ui/search/SearchFragment.java#L160), [`SearchFragment.java:168`](../../app/src/main/java/vn/edu/usth/flickrimagebrowser/ui/search/SearchFragment.java#L168), [`SearchFragment.java:200`](../../app/src/main/java/vn/edu/usth/flickrimagebrowser/ui/search/SearchFragment.java#L200)). Flow: repository `Resource` → retained ViewModel → observer → adapter/UI.
- Favorite DB emissions update the accumulator but re-emit only `SUCCESS`, so `ERROR` payloads remain stale ([`SearchViewModel.java:56`](../../app/src/main/java/vn/edu/usth/flickrimagebrowser/ui/search/SearchViewModel.java#L56), [`SearchViewModel.java:80`](../../app/src/main/java/vn/edu/usth/flickrimagebrowser/ui/search/SearchViewModel.java#L80)). Flow: Room favorites → ID set → copied photos → same-status `Resource`.
- Demo photo IDs contain normalized queries and are reused in Picsum URLs ([`PhotoRepositoryImpl.java:185`](../../app/src/main/java/vn/edu/usth/flickrimagebrowser/data/repository/PhotoRepositoryImpl.java#L185), [`PhotoRepositoryImpl.java:191`](../../app/src/main/java/vn/edu/usth/flickrimagebrowser/data/repository/PhotoRepositoryImpl.java#L191)). Flow: user query → local demo metadata/identity; only an opaque deterministic seed → Picsum.
- Collection writes catch every `RuntimeException` ([`PhotoRepositoryImpl.java:292`](../../app/src/main/java/vn/edu/usth/flickrimagebrowser/data/repository/PhotoRepositoryImpl.java#L292), [`PhotoRepositoryImpl.java:340`](../../app/src/main/java/vn/edu/usth/flickrimagebrowser/data/repository/PhotoRepositoryImpl.java#L340)). Flow: validated name → DAO; uniqueness conflicts are handled specifically; other failures reach the initiating UI.
- Touch targets are 34/40/40/44dp ([`item_photo.xml:47`](../../app/src/main/res/layout/item_photo.xml#L47), [`item_collection.xml:59`](../../app/src/main/res/layout/item_collection.xml#L59), [`item_collection_photo.xml:56`](../../app/src/main/res/layout/item_collection_photo.xml#L56), [`activity_photo_detail.xml:119`](../../app/src/main/res/layout/activity_photo_detail.xml#L119)).

## Files and Implementation

1. Modify `SearchFragment.java` to submit every current non-null payload to the adapter before status-specific visibility handling; keep loading/error messaging and retry behavior unchanged.
2. Modify `SearchViewModel.java` to re-emit copied favorite data with the current `SUCCESS`, `LOADING`, or `ERROR` status and preserve the error message. Do not change `Resource` or public ViewModel methods.
3. Modify `PhotoRepositoryImpl.java` so Picsum seeds are deterministic from non-query pagination/index data while query-derived IDs and demo metadata remain local. Narrow both catches to the expected uniqueness constraint failure and report other failures to the initiating ViewModel/UI. Preserve existing call sites; add the smallest operation-result observation if needed. Rethrowing on the executor alone is insufficient because it can crash the process.
4. Modify the four cited layouts so each image button has a 48dp actual touch region, adjusting padding/margins only as needed to retain the visual affordance. This avoids new helper code and dependencies.
5. Extend `SearchViewModelTest.java` for favorite changes during retained-data `ERROR`, asserting updated heart plus unchanged status/message. Extend `PhotoRepositoryTest.java` for query-free demo URLs and for uniqueness-specific handling versus observable unrelated write failures; verify the UI retains the form and reports failure.
6. Add the narrowest feasible recreation regression to `MainActivityNavigationTest.java`: retained rows remain rendered when recreation occurs during retained-data loading/error. If deterministic state injection is impossible without production hooks, document and perform the equivalent manual recreation check; do not add a test-only architecture or dependency.

## Test Matrix and Success Criteria

| Level | Check | Pass condition |
|---|---|---|
| Unit | Search favorite/error regression | Favorite changes; `ERROR` and its message remain unchanged |
| Unit | Demo URL/privacy and DAO failures | URL excludes query; uniqueness is handled; unrelated failure reaches UI without crashing |
| Instrumented | Recreation with retained loading/error data | Recycler remains visible and populated after recreation |
| Static/manual | Four touch targets | Measured bounds are at least 48×48dp; click actions remain correct |
| Project gate | `./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebugAndroidTest` | All tasks pass after environment repair |

## Risks, Mitigations, and Rollback

- **Medium likelihood / medium impact:** changing control bounds shifts compact rows. Keep icon/background styling and verify all four layouts at supported widths.
- **Low likelihood / high impact:** error reporting changes previously silent write behavior. Test specific constraint handling and UI-visible storage failures while preserving existing callers.
- **Medium likelihood / medium impact:** recreation instrumentation is timing-sensitive. Prefer deterministic retained-state setup; fall back to a documented manual check rather than flaky sleeps or new dependencies.
- Roll back the touched source, layouts, and tests together. No database/API migration exists, so rollback cannot strand data or callers.

## Completion Checklist

- [x] All five defects are fixed with demo mode and existing APIs preserved.
- [x] Focused tests and static 48dp XML checks pass.
- [x] Full Gradle gate passes through the isolated source/cache route; intermediate failures are recorded.

- [ ] Execute the added recreation and measured-bounds instrumentation tests on an Android device/emulator; inspect appearance and tap routing.

## Verification evidence

See the [fix report](../reports/fix-261028-0443-codebase-review.md) for failing-before/passing-after regressions, the successful final unit/lint/instrumentation-build gate, source equivalence, and the empty ADB device list. No schema or stored-data changes were made. The privacy fix also covers legacy saved URLs through PhotoMapper.
