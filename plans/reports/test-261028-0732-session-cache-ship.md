# Ship test report — 2026-10-28 — search cache TTL

Outcome: verified for the requested JVM, lint, and debug-assembly gates. The isolated source matched the workspace before and after the run, and Gradle completed with no failures.

Authority: `README.md`, `plans/reports/cook-261028-0732-cache-ttl.md`, and the ship-gate command supplied for this task.

## Source identity

- Workspace commit: `0e1ca78abbbe2041eda4d2be7d23fda97e3cd142`, with the cache/TTL work present as dirty and untracked inputs.
- Compared `app/src` recursively plus `app/build.gradle.kts`, root `build.gradle.kts`, and `gradle/libs.versions.toml` against `/tmp/flickr-cache-LYZlqG/source`; all were identical before and after execution.
- The isolated `settings.gradle.kts` differed only by the documented temporary repository: `maven { url = uri("/tmp/flickr-cache-LYZlqG/maven") }`.
- `git diff --check` passed after execution. No source or configuration file was synchronized or changed by this test task.

## Results

Command:

```text
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --rerun-tasks --offline --no-daemon --no-configuration-cache
```

Environment: isolated Gradle and Android user homes under `/tmp/flickr-cache-LYZlqG`, JDK 25, workspace WSL SDK bridge, offline dependencies, and the supplied Byte Buddy Java agent. No `local.properties` was copied or read.

| Gate | Result |
| --- | --- |
| `:app:testDebugUnitTest` | Passed |
| `:app:lintDebug` | Passed; HTML and SARIF reports were generated in the isolated copy |
| `:app:assembleDebug` | Passed |
| Overall | `BUILD SUCCESSFUL in 1m`; 51 actionable Gradle tasks, all 51 executed |

Gradle's console did not emit the JUnit case count. The environment's performance hook blocked direct reads under `app/build`, so the exact passed-test count was not collected and the global hook was not changed. No coverage gate or repository threshold was requested or measured.

## Warnings and limits

- Android analytics metrics could not initialize because `/home/arcrek/.android/analytics.settings` is read-only. This did not fail lint or the build.
- `FavoritesFragment.java` uses or overrides a deprecated API.
- Byte Buddy 1.15.4 called a terminally deprecated `sun.misc.Unsafe` method under JDK 25.
- Device/emulator tests, live Pixabay API behavior, performance, and proactive eviction of expired in-memory entries were not tested.

No owned background process or port remains; Gradle used a single-use daemon and stopped it at build completion.
