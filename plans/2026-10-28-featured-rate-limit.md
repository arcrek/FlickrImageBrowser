# Featured photo request protection

Outcome: Featured loading and pagination must respect 100 API requests per rolling
60 seconds, retain loaded photos on failure, and allow explicit retry.

Constraints: Preserve API/repository signatures, use existing dependencies, keep
network work asynchronous. No credentials or live API traffic needed for tests.
Non-goals: UI redesign, persistent caching, or coordination between devices.

## Evidence and choice

`PixabayClient` has no request throttling. `SearchViewModel.loadNextPage` clears
its loading flag on failure without preventing subsequent scroll-triggered retries.
The Featured feed added in the recent discovery/pagination work uses this path.
Use a shared, synchronized network interceptor to count actual outgoing attempts
and refuse excess traffic locally; pause automatic pagination after failure.

## Phases

- [x] Scout: client, repository, Featured/search callers, unit tests.
- [x] Diagnose: unbounded API requests and repeated pagination after failure.
- [x] Implement: rolling budget, server cooldown, explicit pagination retry.
- [x] Verify: regression before/after, limiter boundary/concurrency/cooldown tests,
  affected unit tests and Android build/lint where available.
- [x] Review: inspect diff and affected callers; update architecture documentation.
- [x] Finalize: report evidence and limits. Journal disabled by user preference.

Acceptance: attempt 101 cannot reach the network within a rolling minute;
requests resume when capacity returns; 429 pauses further network requests;
scrolling cannot retry a failed page; explicit Retry still works.

## Verification evidence

The Featured error regression run against the original ViewModel fails with
`TooManyActualInvocations`: the failed page was requested three times instead of
once. The same regression and all other ViewModel/limiter checks pass with the
fix (17 focused JVM tests). Limiter coverage includes rolling-window expiry,
concurrency, 429 cooldown headers, failed attempts, and negative clock origins.

An initial full `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` run passed
using the existing WSL SDK with `ANDROID_HOME` and `ANDROID_SDK_ROOT` both set
to `$PWD/.gradle/wsl-sdk`. The default Windows SDK path fails under Linux.
No SDK/configuration or credential files were changed. No emulator or live API
checks were run; the request budget is process-local and resets on app restart.

A final incremental rerun hit missing generated-class errors alongside Gradle
cache `Input/output error` failures. A forced non-incremental rerun regenerated
bindings and compiled Java successfully, but failed at `:app:packageDebug` with
`java.io.IOException: No such file or directory` and cache I/O errors. These
repeat the WSL filesystem/cache issues recorded in the earlier codebase audit.
Final APK packaging is therefore not verified reliably in this environment.
The final ViewModel and interceptor were also compiled and run independently
against cached dependencies: all 17 focused tests passed.

## Shipping verification

Independent review caught a stale indefinite Retry snackbar across query changes.
The fragment now dismisses it on subsequent rendered states and view destruction;
two regression tests cover those transitions. Re-review found no remaining issues.

Fresh verification in an isolated Linux copy succeeded: the complete unit suite
(69 test methods across 10 classes), `lintDebug`, and `assembleDebug`, all with
tasks forced to rerun. The sandbox required a cached Byte Buddy Java agent for
Mockito and a temporary Android user home for debug signing. This supersedes the
mounted-workspace APK verification limitation above. No instrumentation or live
API checks were run.

Shipping issue: https://github.com/arcrek/FlickrImageBrowser/issues/16.
The repository has no configured GitHub Actions workflows. The unrelated
codebase-review plan is outside this fix and remains open.
