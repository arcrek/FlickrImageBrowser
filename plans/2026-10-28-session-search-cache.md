# Session search cache

Outcome: avoid repeat Pixabay calls for previously viewed query/page combinations during one application process.

Constraints: RAM only, with a 30-minute per-page TTL requested after the initial session-cache implementation; no database changes. Preserve favorites, Featured, pagination and retry behavior. Pull-to-refresh and Search on the active query request fresh page 1. A successful refresh invalidates older pages of that query only; failures retain existing results and unexpired cache.

Non-goals: disk/offline persistence, image caching changes, API/provider changes.

## Delivery

- [x] Repository: cache successful and empty responses by trimmed query, clamped page and page size; merge current favorites on cache reads; share identical pending requests and prevent stale callbacks repopulating refreshed pages.
- [x] UI/ViewModel: pull-to-refresh and same-query Search; retain rows and pagination on refresh failure; reset pagination on refresh success.
- [x] Verify repository, ViewModel and fragment behavior; run broader unit/build/lint checks where environment permits; review and update architecture docs.
- [x] Add 30-minute expiry from successful HTTP receipt; cache reads/failures do not extend it, and expired pages fetch again on access. See [TTL verification](reports/cook-261028-0732-cache-ttl.md).

Acceptance: repeat unexpired query/page makes zero additional API calls; access at 30 minutes fetches again; refresh makes one call and leaves other queries cached; failures are retryable; favorites remain current; refresh controls stop loading on completion.

Verification: focused cache and caller tests passed; final `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` passed against an identical isolated source copy. See [delivery report](reports/cook-261028-0710-session-search-cache.md). Device gesture verification remains unperformed.
