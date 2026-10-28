# Test report — 2026-10-28 — Pixabay leftovers

Outcome: verified for shipping within the requested local JVM, lint, debug-build, and offline proxy-self-check scope. The final app source snapshot passed 26/26 focused tests, 103/103 full debug unit tests, `lintDebug`, and `assembleDebug`.

Authority: the current dirty worktree changes to Pixabay image URL mapping, browser-challenge error classification, detail-image fallback, branding resources, their tests, and the repository's documented debug gates.

## Diff-aware scope and results

Diff-aware mode analyzed six changed app source/test files. `PhotoMapper.java` mapped to `PhotoMapperTest.java` by co-location and import ownership; `PhotoRepositoryImpl.java` mapped to `PhotoRepositoryTest.java`. `PhotoDetailActivity.java` and `strings.xml` have no direct unit tests, so lint and debug assembly covered their compile/resource integration. The explicit shipping request broadened verification to the complete unit suite.

| Contract | Check | Observed result | Gap / follow-up |
| --- | --- | --- | --- |
| Signed Pixabay `/get/` thumbnails fall back to CDN previews and persist correctly | Focused `PhotoMapperTest` | 8/8 passed | None |
| Cloudflare browser challenges are distinguished from ordinary 403/429 errors | Focused `PhotoRepositoryTest` | 18/18 passed | None |
| Complete JVM regression suite | `:app:testDebugUnitTest --rerun-tasks` | 103/103 passed across 12 suites; 0 failed, 0 errored, 0 skipped; 30 s | No device/emulator suite was requested |
| Android static checks | `:app:lintDebug --rerun-tasks` | Passed; 30/30 Gradle tasks executed; 42 s | Existing analytics-home warning did not fail lint |
| Debug APK packaging | `:app:assembleDebug --rerun-tasks` | Passed; 37/37 Gradle tasks executed; 37 s | None |
| Proxy script works safely without credentials or network | Python 3.14.4 compile plus no-key execution | Passed syntax and built-in offline assertions; expected exit 1 with the exact configuration message and no stdout/live request | Live Pixabay behavior was intentionally not tested |

The focused run completed in 39 s with 26/26 Gradle tasks executed. Compilation in all Gradle gates repeated an existing deprecated-API note for `FavoritesFragment.java`; no compiler, assertion, lint, or packaging failure remained.

## Source and execution

- Base commit: `5472caf377c531c802940989268d15b038628f9c` with the documented dirty/untracked inputs.
- Isolated source: `/tmp/flickr-leftovers-e03qJI/source`; `local.properties` and build caches were not copied.
- Final workspace and isolated-copy `app/src` fingerprint: `42309bc5e70aed366d73f18feba6cc95a3e500e8cd267d641f236ab4e1700b53` before/after the successful gates.
- Final workspace and tested-copy proxy-script fingerprint: `b65f5421b0a3f951cb6d83151dc1bf1d0f85e5775ebe54c062694d91e80a4572`.
- Runner: Gradle 9.6.0, Eclipse Adoptium JDK 25, workspace `.gradle/wsl-sdk`, no daemon, configuration cache disabled, and Byte Buddy 1.15.4 preloaded for Mockito on JDK 25.
- The successful focused run resolved one missing AndroidX artifact with approved network access. All later full gates ran offline from `/tmp/flickr-cache-LYZlqG/gradle-home`.
- Raw logs: `/tmp/flickr-leftovers-e03qJI/focused-4.log`, `full-unit.log`, `lint.log`, `assemble.log`, and `proxy-selfcheck-3.stderr`.

## Investigated failures

1. Two offline focused attempts stopped before compilation because the reused caches lacked `androidx.swiperefreshlayout:swiperefreshlayout:1.2.0`. No test ran. This was a stale-cache/tooling failure.
2. The first online retry stopped on sandbox DNS resolution for `dl.google.com`. The required escalated retry downloaded the artifact and passed all 26 focused tests. No product assertion failed in any attempt.
3. A freshness check found that the untracked proxy script changed concurrently after the first offline probe. That result was discarded; the latest script was copied with checksum comparison, rerun, and fingerprint-matched afterward.

## Coverage, risks, and recommendations

No line, branch, or function coverage tool or repository threshold is configured, so no percentage is claimed. The changed mapper and repository branches have focused happy/error-path tests. `PhotoDetailActivity`'s nested Glide fallback has compile/lint coverage but no behavioral UI test; this is the main remaining test gap.

No blocking issue remains in the requested scope. Keep the current focused tests and add a UI/instrumented assertion for detail-image fallback only if that behavior becomes regression-prone; no new test is required for this ship decision.

## Cleanup

No owned Gradle daemon, proxy server, port, emulator, or other background process remains. The isolated `/tmp/flickr-leftovers-e03qJI` evidence directory remains for controller review and contains no copied `local.properties` or credential.

Unresolved questions: none for the requested local release gates.
