# Member 3: Week 2 Room data contracts

Date: 2026-10-23
Branch: `feat/database-schema`

The user confirmed Week 2, making Member 3's Room data-layer work active under [the team workload plan](../../docs/TEAM_WORKLOAD_PLAN.md). Implementation is ready for build and device verification; the milestone remains open.

## Changes and decisions

- Extended `PhotoDao` with offline favorite search by title/owner and saved-date/title ordering. Replaced conflict replacement with Room `@Upsert` so updating an existing photo preserves its collection memberships.
- Extended `CollectionDao` with transactional observation of one collection, the alternate method name used by the plan, renaming with the existing unique-name constraint, and observable photo counts.
- Expanded `PhotoDaoTest` and added `CollectionDaoTest` plus `LiveDataTestUtil`. Coverage includes search/sorting, reactive changes, reopening a persisted database, collection uniqueness, foreign-key cascades, membership preservation, and orphan retention.
- Retained the existing entity keys, foreign keys, indices, version 1 schema export, and future `MIGRATION_1_2` placeholder. These DAO changes do not alter the persisted schema, so a version bump or migration is unnecessary. No destructive migration was introduced.

## Verification and remaining work

Independent source review found no actionable findings. `git diff --check` and 18 static checks using SQLite 3.51.0, the exported v1 schema, and the exact DAO SQL passed. These checks do not verify Room-generated code or LiveData behavior.

The Gradle wrapper initially failed to resolve `services.gradle.org`; a network-enabled retry downloaded Gradle 9.6 and started it with JDK 25. The aggregate verification command then exited 1 after 1m 18s while determining dependencies for `:app:compileDebugJavaWithJavac`: `SDK location not found`. `assembleDebug`, `testDebugUnitTest`, and `assembleDebugAndroidTest` never executed. `connectedDebugAndroidTest` was skipped because no Android SDK/emulator/device was available. No Android build or test pass is claimed. All Member 3 checklist items remain unchecked, including TODO 3.5 until the instrumented tests pass.

Run from the repository root on Windows:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest assembleDebugAndroidTest
.\gradlew.bat connectedDebugAndroidTest
```

Configure an Android SDK through `ANDROID_HOME` or `sdk.dir` in untracked `local.properties` before rerunning. The second command also requires an Android emulator or connected device. After successful verification, reconcile Member 3's checkboxes against the plan's expected results. UI/repository integration remains with the owning members and later scheduled weeks.

## Follow-up — 2026-10-23: Android verification passed

The user installed Android Studio and the Android SDK, resolving the earlier SDK blocker. With SDK platform 37.0, JDK 25, and Gradle 9.6, `assembleDebug testDebugUnitTest assembleDebugAndroidTest` completed successfully in 1m 44s. All 11 JVM tests passed (mapper 5, repository 4, search ViewModel 2), with zero failures, errors, or skips; see the [local JVM report](../../app/build/reports/tests/testDebugUnitTest/index.html).

`connectedDebugAndroidTest` completed successfully in 21s on Pixel 10 Pro / Android 17. The XML report confirms all 12 Room tests passed (`PhotoDaoTest`: 7; `CollectionDaoTest`: 5), with zero failures, errors, or skips; see the [local instrumentation report](../../app/build/reports/androidTests/connected/debug/index.html). Room-generated upsert code was also inspected to confirm existing rows are updated rather than replaced. No production-code change was required after the earlier review.

The debug app was installed and launched, and Search/Favorites/Collections navigation passed a Mobile MCP emulator smoke check. Live API searching was not exercised. Mobile MCP 1.0.12's stdio handshake and device listing were verified; exposing its native tools in Codex requires a restart.

Reconciled TODO 3.1–3.5 as complete in the Member 3 section of the team plan. Existing entities, exported schema v1, and the future `MIGRATION_1_2` placeholder remain unchanged. This completes Member 3's current data contracts and their verification; other members and later integration milestones were not marked complete. The reports above are generated local artifacts and are not committed.
