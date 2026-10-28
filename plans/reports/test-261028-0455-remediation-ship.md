# Test report — 2026-10-28 — remediation shipping gate

Outcome: verified for shipping within the local JVM, lint, and Android package-build scope. The current workspace inputs are byte-equivalent to the isolated source that passed the complete gate. Device execution remains unavailable and is not represented as passed.

Authority: the repair scope in `plans/reports/fix-261028-0443-codebase-review.md` and the repository's debug unit, lint, and instrumentation-package gates.

## Results

| Contract | Check | Observed result | Gap / follow-up |
| --- | --- | --- | --- |
| Current source is the tested source | `diff -qr app/src /tmp/flickr-fix-261028/source/app/src` | Exit 0 with no differences across 103 files | None |
| Build inputs still match | SHA-256 comparison of `app/build.gradle.kts`, root build/settings/properties, and the Gradle wrapper properties | All five workspace/snapshot hash pairs matched | None |
| Branch reference does not add untested inputs | `git rev-parse HEAD` and `git rev-parse origin/main` | Both `f6f1dd4d47f7cd3dbf1bde555f585edf94bcea39` | Remote was not fetched during this check |
| JVM regressions | `:app:testDebugUnitTest` | 80/80 passed, 0 failed, 0 skipped across 11 suites | None |
| Android static checks | `:app:lintDebug` | Passed | Existing nonfatal analytics warning only |
| Instrumentation package compiles and assembles | `:app:assembleDebugAndroidTest` | Passed | APK was not installed or executed |
| Patch hygiene | `git diff --check` | Passed | None |

Diff-aware mode analyzed 18 changed `app/src` files: 17 tracked modifications and one new unit test. The complete gate was retained because changes cross repository, ViewModel, fragment, resource, unit-test, and instrumentation-test boundaries.

## Source and execution

- Tested copy: `/tmp/flickr-fix-261028/source`.
- Raw successful output: `/tmp/flickr-fix-261028/verified.log`.
- Private Gradle cache: `/tmp/flickr-ship-KM7TsR/gradle-home`.
- Private Android state: `/tmp/flickr-fix-261028/android-user`.
- Environment: Eclipse Adoptium JDK 25, workspace compatibility SDK, offline dependencies, no Gradle daemon, no configuration cache, and the preloaded Byte Buddy 1.15.4 agent documented in the repair report.
- Command: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon --offline --no-configuration-cache`.
- Observed build result: `BUILD SUCCESSFUL in 36s`; 68 actionable tasks, 9 executed and 59 up-to-date. The test task executed and its XML results record 80 passing tests.

The successful evidence was reused instead of rerunning because the current 103-file `app/src` tree and all five checked Gradle/config inputs match the isolated tested copy exactly. The current local base and `origin/main` reference also match. No source or configuration change was made during this verification.

## Coverage, failures, and limitations

- Coverage instrumentation is not configured; no line, branch, or function percentage is claimed, and no repository threshold was found in the prior verification.
- No failure occurred in the final gate. Earlier failed attempts and their resolved import/environment causes remain preserved in the repair report and raw logs.
- No Android device or emulator is connected. `MainActivityNavigationTest`, including recreation and measured touch-target checks, compiled but did not execute. On-device rendering, tap routing, and live Pixabay/Picsum behavior remain unverified.
- The remote was not fetched. The freshness claim is limited to the local `origin/main` ref inspected during this gate.

## Cleanup

This verification started no Gradle process, daemon, emulator, ADB server, port listener, or background service. The existing `/tmp/flickr-fix-261028` and `/tmp/flickr-ship-KM7TsR` evidence/cache trees were intentionally retained for the shipping controller.

Unresolved questions: none. Device availability is the only remaining verification dependency.
