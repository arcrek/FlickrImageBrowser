# Test report — 2026-10-28 — featured rate limit

Outcome: verified for shipping within the local JVM/Android build scope. The final source snapshot passed the complete debug unit suite, Android lint, and debug APK assembly from an isolated source copy.

Authority: the current worktree's rate-limit, pagination retry, and Snackbar lifecycle behavior, plus the repository's Gradle debug release gates.

## Results

| Contract | Check | Observed result | Gap / follow-up |
| --- | --- | --- | --- |
| Shared rolling Pixabay request budget, server cooldown, and recovery | `PixabayRateLimitInterceptorTest` | 7/7 passed | None |
| Featured pagination stops automatic retry and explicit retry resumes it | `SearchViewModelTest` | 10/10 passed | None |
| A new render and view destruction dismiss a stale pagination retry Snackbar | `SearchFragmentTest` | 2/2 passed | None |
| Complete JVM regression suite | `:app:testDebugUnitTest --rerun-tasks` | 69/69 passed; 26/26 Gradle tasks executed; 44 s | No instrumentation/device suite was requested or run |
| Static Android checks | `:app:lintDebug --rerun-tasks` | Passed | Android analytics could not write to the sandbox home; lint itself completed |
| Debug package | `:app:assembleDebug --rerun-tasks` | Passed, including `validateSigningDebug` and `packageDebug` | Debug keystore was created only in the isolated Android user directory |

The lint and assemble invocation completed in 42 s with 47/47 Gradle tasks executed. Compilation reported an existing deprecated-API note in `FavoritesFragment.java`; no new compiler or lint failure was observed.

## Source and execution

- Base commit: `d983836e6d25b7f2028ea0f8c4dd3d5c8bb541cd` with the documented dirty/untracked feature files.
- Clean source copy: `/tmp/flickr-ship-KM7TsR/source`.
- Private Gradle cache: `/tmp/flickr-ship-KM7TsR/gradle-home`.
- Private Android user state: `/tmp/flickr-ship-KM7TsR/android-user`.
- Runner: Gradle 9.6.0, Eclipse Adoptium JDK 25, offline dependency resolution, configuration cache disabled, daemon disabled.
- SDK for both `ANDROID_HOME` and `ANDROID_SDK_ROOT`: the workspace `.gradle/wsl-sdk` compatibility SDK.
- The seven affected production/test files had identical SHA-256 fingerprints in the workspace and isolated copy immediately after all gates. Key final fingerprints were `730ce5a7...faf90` for `PixabayRateLimitInterceptor.java`, `3028b733...6ca0` for `SearchFragment.java`, and `56d9fc83...c7e2` for `SearchFragmentTest.java`.

Successful unit command environment:

```text
GRADLE_USER_HOME=/tmp/flickr-ship-KM7TsR/gradle-home
JAVA_HOME=/home/arcrek/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2
ANDROID_HOME=/mnt/c/Users/arcrek/AndroidStudioProjects/FlickrImageBrowser/.gradle/wsl-sdk
ANDROID_SDK_ROOT=/mnt/c/Users/arcrek/AndroidStudioProjects/FlickrImageBrowser/.gradle/wsl-sdk
JAVA_TOOL_OPTIONS=-javaagent:/tmp/flickr-ship-KM7TsR/gradle-home/caches/modules-2/files-2.1/net.bytebuddy/byte-buddy-agent/1.15.4/58e850dde88f3cf20f41f659440bef33f6c4fe02/byte-buddy-agent-1.15.4.jar
./gradlew :app:testDebugUnitTest --rerun-tasks --no-daemon --offline --no-configuration-cache
```

Successful lint/build command additionally set `ANDROID_USER_HOME=/tmp/flickr-ship-KM7TsR/android-user` and ran:

```text
./gradlew :app:lintDebug :app:assembleDebug --rerun-tasks --no-daemon --offline --no-configuration-cache
```

## Investigated failures and limitations

1. A workspace rerun stopped before configuration because the sandbox made `/home/arcrek/.gradle` read-only. This was classified as environment-only and avoided with the private Gradle cache.
2. The first clean focused run reached all 17 selected tests but failed them during Mockito initialization because sandbox policy prevented Byte Buddy's dynamic self-attachment. Preloading the already-resolved Byte Buddy agent made all 17 pass; the same agent was used for the final full suite. No assertion failed in the first run.
3. The first combined lint/build attempt completed lint analysis but failed `validateSigningDebug` because `/home/arcrek/.android` was read-only. Setting `ANDROID_USER_HOME` to the isolated run directory allowed the forced full rerun to pass. The analytics warning still appeared but did not affect lint or assembly.
4. Coverage instrumentation was not configured or run, and the repository has no configured coverage threshold. The measured result is test execution, not a line/branch/function coverage claim.
5. No Android instrumentation tests, emulator tests, or live Pixabay API calls were run. Unit tests cover the application-owned limiter and retry contracts without consuming live quota.

## Cleanup

No background services, ports, emulators, or daemons were left running. The isolated `/tmp/flickr-ship-KM7TsR` evidence tree remains available for the controller during this shipping session and contains no copied `local.properties` or credentials.

Unresolved questions: none for the requested local release gates.
