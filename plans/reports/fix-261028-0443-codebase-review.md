# Codebase review repairs

All five defects are implemented and pass the local verification gate. Device verification remains pending because no Android device or emulator is connected. No commit or publication was requested or performed.

## Cause and repair

| Finding | Confirmed cause | Repair |
|---|---|---|
| Retained search rows disappear | A fresh adapter received data only on success. | SearchFragment submits retained payloads before status-specific rendering. |
| Favorites remain stale | SearchViewModel republished favorite changes only on success. | Favorite snapshots preserve loading/error status and the error message. |
| Demo query disclosure | Query-derived local IDs were embedded in Picsum URLs. | Generated URLs use query-independent numeric seeds; PhotoMapper also sanitizes legacy saved demo URLs on read. IDs, metadata and stored rows remain intact. |
| Collection failures disappear | Repository writes swallowed RuntimeException; forms dismissed immediately. | Per-operation LiveData results distinguish duplicate names from storage/other constraint errors. Both forms await success, retain input on failure and allow retry. Existing void methods remain available. |
| Small action targets | Four ImageButtons were smaller than 48dp. | All four are 48×48dp; favorite icon sizes are retained with padding. |

No dependencies, schema migrations, stored-data updates or demo-mode removal were introduced. The repository interface gains result methods for the forms; existing signatures and callers are preserved. Architecture documentation points to the changed owners.

## Verification

- Before repair, the favorite/loading and demo-URL regressions both failed with assertions. The legacy saved-URL regression also failed before its mapper repair.
- Focused search, repository, collection ViewModel/form and mapper suites passed after repair. They cover retained loading/error payloads, status/message preservation, duplicate constraints versus other failures, failed reads, missing rename targets, form retry/dismissal and legacy URL identity preservation.
- Final gate: `:app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon --offline --no-configuration-cache` passed, with `BUILD SUCCESSFUL in 36s`.
- XML assertions confirmed all ImageButtons in the four affected layouts are 48×48dp. New instrumentation tests cover actual recreation in loading/error states and measured button bounds; they compiled successfully but were not executed.
- Final local diff review found no unresolved source defects. `git diff --check` passed. All 103 application source/resource/test files matched the isolated copy by SHA-256.

### Reproducible build route

The build used source copy `/tmp/flickr-fix-261028/source`, excluding repository metadata, caches, generated output and local credential configuration. It reused the previously established compatibility SDK and cache:

```text
GRADLE_USER_HOME=/tmp/flickr-ship-KM7TsR/gradle-home
JAVA_HOME=/home/arcrek/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2
ANDROID_HOME=/mnt/c/Users/arcrek/AndroidStudioProjects/FlickrImageBrowser/.gradle/wsl-sdk
ANDROID_SDK_ROOT=/mnt/c/Users/arcrek/AndroidStudioProjects/FlickrImageBrowser/.gradle/wsl-sdk
ANDROID_USER_HOME=/tmp/flickr-fix-261028/android-user
JAVA_TOOL_OPTIONS=-javaagent:/tmp/flickr-ship-KM7TsR/gradle-home/caches/modules-2/files-2.1/net.bytebuddy/byte-buddy-agent/1.15.4/58e850dde88f3cf20f41f659440bef33f6c4fe02/byte-buddy-agent-1.15.4.jar
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon --offline --no-configuration-cache
```

Logs are retained under `/tmp/flickr-fix-261028/`: `before.log`, `focused.log`, `legacy-before.log`, `legacy-after.log`, and `verified.log`. An intermediate gate failed because an import cleanup omitted Retrofit/OkHttp test imports; those imports were restored before the successful final gate. The existing read-only Android analytics warning remains nonfatal. Workspace build configuration was not changed.

## Remaining validation

The sandboxed Windows ADB bridge initially failed with a WSL socket error. An authorized check outside the sandbox succeeded and returned an empty device list. Connect a device/emulator, install the app and instrumentation APK, and execute MainActivityNavigationTest, especially the two recreation tests and measured touch-target test. On-device layout appearance and tap routing remain unverified; no live Pixabay/Picsum requests were made.

Gradle used single-use daemons configured to stop at build end. No emulator or background service was started. Journaling was skipped because `journal.auto=false`.

Unresolved questions: none; device availability is the remaining verification dependency.
