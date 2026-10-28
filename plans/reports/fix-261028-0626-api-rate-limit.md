# API rate-limit investigation and repair

Outcome: fixed a reproducible gap in server-quota handling. A live Featured request
using the configured key succeeded; the user's earlier live error was not reproduced.

## Scope and cause

Preserve the shared rolling budget, existing 429 protection, explicit pagination
retry, API signatures, and stored data. No new dependencies or credential changes.
Acceptance: a successful response reporting zero remaining requests must prevent
another network attempt until the server reset delay expires.

`PixabayRateLimitInterceptor.intercept` previously started cooldown only for HTTP
429. It ignored successful responses with `X-RateLimit-Remaining: 0`, so the next
request could exceed the server quota even below the process-local budget. This
is exposed when the shared API key has requests outside this process or a different
window history. It is a gap in the earlier limiter, not evidence of a new server
limit. Featured, keyword search, and photo-by-ID requests share this interceptor.

[Pixabay's API documentation](https://pixabay.com/api/docs/) confirms that quotas
are per API key and `X-RateLimit-Reset` is a remaining delay in seconds.

## Changes and evidence

- The shared interceptor now observes zero remaining quota on any response and
  honors the reset delay, falling back to 60 seconds for missing or invalid delays.
  Existing longer cooldowns and the minimum 60-second HTTP 429 protection remain.
- Two added regression tests failed before the production change with
  `AssertionError`: the next request was allowed instead of throwing `IOException`.
  They cover recovery at the exact reset boundary and fallback for invalid reset.
- The live Featured page-one request returned HTTP 200, 30 photos, limit 100,
  remaining 99, and reset 60 seconds. The key stayed in memory and was not printed.
  Sandbox network restrictions required an approved escalated request.
- Complete `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` passed offline
  with no configuration cache or persistent daemon (`BUILD SUCCESSFUL in 1m 26s`).
  The credential-free isolated copy at `/tmp/pixabay-rate-limit-261028/source`
  matched all 103 workspace app source files. Logs: `before.log` and `after.log`
  in `/tmp/pixabay-rate-limit-261028/`. Verification reused the existing WSL SDK,
  cached JDK/dependencies, and Byte Buddy agent documented in the prior repair report.
- Direct diff review and `git diff --check` passed. Architecture documentation
  now describes server-quota handling. Journaling was skipped by preference.

## Limits

No on-device run or deliberate live quota exhaustion was performed. The direct
live API request verifies the key and endpoint, not the installed APK. Already
in-flight requests and other clients sharing the key can still produce HTTP 429.
The existing budget remains process-local.

## Shipping review

The user subsequently requested shipping to main with an admin merge. Created
`fix/server-rate-limit` from fetched main at `63bed4b`. Independent code review
requested a short-reset HTTP 429 test; added it to preserve the existing 60-second
minimum explicitly, and re-review approved with no remaining findings. A docs
delegate confirmed the architecture update is sufficient. The tester confirmed
the original verification inputs matched. The updated full unit suite and lint
passed (`BUILD SUCCESSFUL in 26s`), recorded in
`/tmp/pixabay-rate-limit-261028/ship-tests.log`; production/APK inputs were unchanged.

Automatic approval review rejected separate GitHub issue creation as publication
outside the requested PR destination; shipping continues without a separate issue.
The optional `ak plan resolve` lookup failed with a generic command error; no active
plan was supplied and no plan state was changed. The PR-body validator initially
received a filename rather than stdin; rerunning with stdin passed. Repository
settings confirm no GitHub Actions workflows are configured.

Unresolved question: the exact earlier error and trigger remain unspecified; the
live check could not establish whether this confirmed defect caused that incident.
