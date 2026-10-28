# Cache TTL: 30 minutes

Added a fixed 30-minute TTL per successful Pixabay page, including empty Featured/search pages. This supersedes the earlier session-only, no-TTL decision. Existing query/page/size keys, explicit refresh, favorite merging and stale-callback protection remain unchanged.

TTL starts when the HTTP response arrives, measured with `SystemClock.elapsedRealtime()` so device sleep counts and wall-clock changes do not affect expiry. Cache reads and failures do not renew it. On access at or after 30 minutes, the expired page is removed and fetched again; duplicate pending requests still share a result. A new response starts a new TTL. Expiry is checked lazily: no timer, automatic background fetch, or forced replacement of ViewModel-held rows was added.

## Verification

- Added four deterministic clock-based tests: just before/exactly at 30 minutes and no sliding extension; response arrival versus request start and independent page expiry; empty Featured page expiry and failed retry; failed explicit refresh preserving the original expiry.
- Focused `SearchCacheTest` passed.
- Full `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon --no-configuration-cache --offline` passed in 47 seconds against `/tmp/flickr-cache-LYZlqG/source`. The same private JDK 25, Gradle cache, SDK bridge and temporary Maven workaround documented in the earlier session-cache delivery report were reused.
- Compared all 55 Java/layout/dependency files in the workspace and verification copy: identical. `git diff --check` passed.
- Reviewed TTL writes/reads, pending-result ordering and generation checks inline. No public repository signature or UI changes were needed. Updated `docs/ARCHITECTURE.md`.
- Journal skipped by preference. No commit, database change or background process was left running.

Not tested on a device/emulator or against the live API. Expired entries that are never read again can remain in RAM until the process ends; TTL controls reuse rather than proactive memory eviction.
