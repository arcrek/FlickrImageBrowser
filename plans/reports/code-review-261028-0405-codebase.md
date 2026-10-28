# Codebase review

Reviewed: 2026-10-28 UTC. Revision: `d983836e6d25b7f2028ea0f8c4dd3d5c8bb541cd`.
Scope: production Java, resources, manifest, Gradle configuration, project requirements, and all test source groups. No production code was changed. Findings below are verified by source tracing; runtime reproduction was blocked by the build failure described below.

## Findings

### P2 — Recreating Search during pagination loses the visible retained results

[SearchFragment.java:168](../../app/src/main/java/vn/edu/usth/flickrimagebrowser/ui/search/SearchFragment.java#L168), also lines 212–220.

Load page one, start page two, then rotate while it is loading or after it fails. `onViewCreated()` creates a fresh empty adapter. The `LOADING` and `ERROR` branches show the recycler but never submit the retained `Resource.data`; only `SUCCESS` submits it. The grid becomes blank despite the ViewModel retaining the photos, and stays blank after a failed retry. Submit retained data in every state that carries it. Verify view recreation during both loading and failure.

### P2 — Favorite buttons stop reflecting changes after a pagination error

[SearchViewModel.java:80](../../app/src/main/java/vn/edu/usth/flickrimagebrowser/ui/search/SearchViewModel.java#L80).

After page two fails, tap a favorite heart on page one. Room updates and `onFavoritesChanged()` updates its internal list, but only republishes when the current state is `SUCCESS`. The displayed `ERROR` snapshot stays stale indefinitely; tapping again because the first tap appears ineffective toggles the database back. Publish updated data while retaining the current request status/message, and ensure the fragment binds that data. Add a ViewModel regression for favorite changes under `ERROR` and `LOADING`.

### P2 — Demo image requests disclose query-derived text to another service

[PhotoRepositoryImpl.java:185](../../app/src/main/java/vn/edu/usth/flickrimagebrowser/data/repository/PhotoRepositoryImpl.java#L185), lines 191–192.

With a demo or placeholder key, the query is normalized into `photoId` and embedded in remote Picsum image URLs. Searching `medical condition`, for example, produces a seed containing `demo_medical_condition_`; Glide sends that path when displaying the image. This exposes search-derived text to the image provider even though demo search needs no remote keyword lookup. Use fixed, query-independent image seeds while preserving the intentional demo feature and existing saved photo IDs.

### P2 — Collection writes silently swallow storage failures

[PhotoRepositoryImpl.java:294](../../app/src/main/java/vn/edu/usth/flickrimagebrowser/data/repository/PhotoRepositoryImpl.java#L294), also line 342.

Create and rename catch every `RuntimeException` as a duplicate-name conflict. If the write fails because storage is full or the database is corrupt, the UI dismisses the form and receives no failure signal; the requested change is never saved. Handle expected constraint failures specifically and propagate other write failures to the initiating UI. Verify DAO write failures do not produce a success-like dismissal.

### P2 — Frequent photo actions have small touch regions

[item_photo.xml:49](../../app/src/main/res/layout/item_photo.xml#L49), [item_collection.xml:61](../../app/src/main/res/layout/item_collection.xml#L61), [item_collection_photo.xml:58](../../app/src/main/res/layout/item_collection_photo.xml#L58), [activity_photo_detail.xml:121](../../app/src/main/res/layout/activity_photo_detail.xml#L121).

Favorite controls are 34dp and 44dp; collection menu/remove controls are 40dp. These are plain `ImageButton` views with no expanded touch delegate in the Java sources. Missed favorite taps can activate the surrounding photo card instead. Provide at least 48dp touch regions while keeping icons visually small if desired. Validate target bounds on a device.

## Additional observations

- Bottom-tab selection replaces destinations with new fragment instances (`MainActivity.java:73–85, 131–135`). Search results and Favorites filter/sort state reset after a tab round trip. The specification explicitly guarantees configuration-change retention but does not settle tab retention, so this is a product decision rather than a required correction.
- White on the configured primary mint (`#00BFA5`) has approximately 2.34:1 contrast. The Search/New Collection controls and active navigation deserve accessibility verification alongside touch targets.
- Superseded Retrofit calls are detached by the ViewModel but never cancelled, so obsolete manual searches still perform network and mapping work. Collection summaries load full photo relations just to calculate counts. Neither path was load-tested; no measured performance claim is made.
- Adding a photo and its collection relationship uses separate writes. A nonexistent positive collection ID would fail its foreign key after the photo upsert. The existing UI normally supplies observed IDs; no normal single-window reproduction of a stale ID was established. Consider a transactional operation with explicit failure handling when that contract is revisited.

## Verification

- Source inventory contains 60 JVM tests and 15 instrumentation tests. Existing coverage includes mapping, repository happy paths, ViewModels, DAO relationships/orphan cleanup, and navigation chrome. It does not exercise the restoration and error-state favorite cases above.
- Attempted the documented WSL environment with JDK 25 and `ANDROID_HOME=$PWD/.gradle/wsl-sdk`: `./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebugAndroidTest`.
- Initial sandbox attempt could not create the global Gradle wrapper cache lock. An authorized retry reached resource processing but failed in `:app:packageDebugResources` and `:app:mergeDebugResources` with `NullPointerException` involving `File.getParentFile()`, alongside repeated Gradle cache `Input/output error` exceptions. Gradle also warned that configured `sdk.dir` does not exist.
- Unit tests, lint, and instrumentation compilation did not complete. No emulator tests or live Pixabay/Picsum calls were run. These environment/build errors have not been attributed to application code.
- The Gradle invocation used `--no-daemon`; its single-use daemon is configured to stop at build end. No dev server or emulator was started.

## Review decisions

The team workload plan explicitly permits classroom demo mode (`docs/TEAM_WORKLOAD_PLAN.md:80`), and the UI walkthrough describes demo images. An older architecture paragraph instead requires placeholder-key rejection. This review preserves demo mode and flags only its avoidable query disclosure; it does not recommend removing an accepted feature. Missing XML focusability alone was insufficient evidence of a keyboard defect because runtime click/focus behavior was not tested.

[Proposed remediation plan](../261028-0405-codebase-review/plan.md). Implementation is not authorized by this review request.

## Unresolved questions

- Should switching bottom tabs preserve each destination’s query, scroll position, filter, and sort?
