# Project Evaluation — FlickrImage Browser (Pexels API Edition)

## 1. Repository Inspection & Current State

Before modifying any project files, the repository was inspected to evaluate existing work, build configuration, and potential blockers:

- **Project Type:** Existing Android Studio project generated from the **Empty Activity (Kotlin + Jetpack Compose)** template, not yet initialized as a Git repository.
- **Application ID & Namespace:** `vn.edu.usth.flickrimagebrowser`
- **Build System & Versions:**
  - **Gradle Wrapper:** `9.6.0` (`gradle/wrapper/gradle-wrapper.properties`)
  - **Android Gradle Plugin (AGP):** `9.4.1` (`gradle/libs.versions.toml`)
  - **Daemon JVM Toolchain:** JDK `25` (`gradle/gradle-daemon-jvm.properties`), with Java compilation target set to `JavaVersion.VERSION_11` in `app/build.gradle.kts`.
  - **Android SDK Configuration:** `compileSdk = 37` (`android-37.0` installed locally), `targetSdk = 37`, and `minSdk = 36`.
- **Identified Blockers & Required Adjustments:**
  1. **Flickr API Paywall (Pro-Only Restriction):** Flickr changed its policy so that registering new API keys is restricted exclusively to paid Flickr Pro subscribers. To keep the project 100% free and accessible for university students and graders, the remote networking layer was migrated to the **Pexels REST API** (`https://api.pexels.com/v1/`), which provides instant free API keys (200 req/hr, 20,000 req/month) with full keyword search and pagination.
  2. **Kotlin & Compose vs. Project Constraints:** The project requires **Java-only application source code** and **Android Views / XML layouts**. Replacing the default Compose scaffolding with ViewBinding + Material Components satisfies the Java-only requirement.
  3. **`minSdk = 36` Compatibility Risk:** Lowering `minSdk` to `24` (Android 7.0) while keeping `compileSdk = 37` and `targetSdk = 37` maximizes device and emulator compatibility.
  4. **Theme Incompatibility:** Updated `app/src/main/res/values/themes.xml` to `Theme.MaterialComponents.DayNight.NoActionBar`.
  5. **Missing `INTERNET` Permission & API Key Setup:** Added `android.permission.INTERNET` to `AndroidManifest.xml` and configured `PEXELS_API_KEY` injection via `local.properties`.

---

## 2. Project Purpose & Scope

**FlickrImage Browser** is a native Android application written in Java that allows users to search and discover public photography via the **Pexels REST API**, inspect high-resolution photo details and photographer metadata, bookmark favorite photos for offline viewing, and organize saved photos into custom local collections.

The application is designed for a **4–8-week university final project**, balancing clean architectural separation (MVVM + Repository pattern) with pragmatic Java concurrency (`ExecutorService` + `LiveData`) and reliable automated testing.

---

## 3. Requirements & Acceptance Criteria

### Functional Requirements
1. **Public Photo Search:**
   - Users can search public photos by keyword using Pexels' `GET /v1/search` endpoint.
   - Supports pagination (`page`, `per_page`) and maps sized image URLs (`src.tiny` / `src.small` for thumbnails, `src.large` for detail view).
   - Displays explicit **Loading**, **Empty**, **Error (with Retry)**, and **Content** states.
2. **Photo Detail View:**
   - Selecting a photo opens a detail screen displaying a larger image (`src.large`), title (`alt`), photographer name (`photographer`), description, and a favorite toggle action.
3. **Favorites Persistence:**
   - Users can mark/unmark any photo as a favorite.
   - Favorites are persisted locally in Room (`photos` table) and remain accessible offline across app restarts.
4. **Collections Organization:**
   - Users can create local collections (`collections` table) and associate saved photos with collections via a many-to-many cross-reference table (`collection_photo_cross_ref`).

### Non-Functional & Technical Acceptance Criteria
- **Zero Kotlin in `app/src/`:** All main, unit test, and instrumentation test sources are written in Java (`.java`).
- **XML Views + ViewBinding:** All screens use Android XML layouts (`CoordinatorLayout`, `ConstraintLayout`, `RecyclerView`, `BottomNavigationView`) bound safely via `ViewBinding`.
- **Main-Thread Safety:** All network calls and Room database writes execute on background threads (`Retrofit.enqueue` and `AppExecutors`).
- **Configuration-Change Resilience:** Screen rotation preserves search results and UI state inside lifecycle-aware `ViewModel` + `LiveData` instances without re-triggering network calls.
- **API Key Hygiene:** `PEXELS_API_KEY` is injected from untracked `local.properties` into `BuildConfig.PEXELS_API_KEY`; no real API keys are committed to version control.

---

## 4. Proposed Architecture & Dependency Choices

### Architecture: Pragmatic MVVM + Repository
- **UI Layer (`ui/search`, `ui/detail`, `ui/favorites`, `ui/collections`):**
  - Single-Activity (`MainActivity`) hosting `SearchFragment`, `FavoritesFragment`, and `CollectionsFragment` via `BottomNavigationView`, plus `PhotoDetailActivity` for full photo details.
  - Fragments/Activities observe `LiveData<Resource<T>>` exposed by `ViewModel` subclasses created via `AppViewModelFactory`.
- **Domain Layer (`domain/model`):**
  - Immutable Java models (`Photo`, `Collection`) decoupled from Retrofit annotations and Room table details.
- **Data Layer (`data/remote`, `data/local`, `data/repository`):**
  - `PhotoRepository` interface and `PhotoRepositoryImpl` coordinate remote Pexels API responses (`PexelsApiService`) and local Room DAOs (`PhotoDao`, `CollectionDao`).

### Dependency Choices
| Category | Library | Version | Rationale |
|---|---|---|---|
| UI & Compat | `androidx.appcompat:appcompat`, `com.google.android.material:material`, `androidx.recyclerview:recyclerview`, `androidx.constraintlayout:constraintlayout` | `1.7.0` / `1.12.0` / `1.4.0` / `2.2.1` | Standard, stable Android Views and Material Design components |
| Lifecycle | `androidx.lifecycle:lifecycle-viewmodel`, `androidx.lifecycle:lifecycle-livedata` | `2.8.7` | First-class Java `ViewModel` and `LiveData` support without Kotlin coroutines |
| Networking | `com.squareup.retrofit2:retrofit`, `converter-gson`, `com.squareup.okhttp3:okhttp`, `logging-interceptor` | `2.11.0` / `4.12.0` | Industry-standard type-safe HTTP client and JSON parsing |
| Image Loading | `com.github.bumptech.glide:glide` + `compiler` | `4.16.0` | Memory/disk caching, lifecycle-aware image loading, Java `annotationProcessor` support |
| Persistence | `androidx.room:room-runtime` + `room-compiler` | `2.6.1` | Compile-time verified SQLite queries, native `LiveData` observation, Java `annotationProcessor` support |
| Testing | `junit:junit`, `org.mockito:mockito-core`, `androidx.arch.core:core-testing`, `androidx.room:room-testing`, `androidx.test.ext:junit`, `espresso-core` | `4.13.2` / `5.14.2` / `2.2.0` / `2.6.1` / `1.2.1` / `3.6.1` | Unit, ViewModel `LiveData`, mocked repository, and Room DAO testing |

---

## 5. Key Risks, Assumptions & Technical Trade-Offs

1. **HTTP Authentication & Rate Limit Handling:**
   - *Risk:* Invalid Pexels API keys return HTTP `401`/`403`, and exceeding 200 requests/hour returns HTTP `429`.
   - *Mitigation:* `PhotoRepositoryImpl` explicitly inspects HTTP status codes (`401`, `403`, `429`) and surfaces descriptive, actionable messages to the UI error state.
2. **Client-Side API Key Exposure:**
   - *Risk:* Any key compiled into `BuildConfig` can be extracted from a distributed APK using `apktool` or `jadx`.
   - *Trade-off:* Avoiding a custom proxy backend keeps the project within a 4–8-week student scope. Keeping the key in untracked `local.properties` prevents leaking credentials in public Git repositories.
3. **Manual Dependency Injection vs. Hilt/Dagger:**
   - *Trade-off:* Using a lightweight `AppContainer` + `AppViewModelFactory` avoids annotation-processor build complexity while keeping `PhotoRepository` and `ViewModel` dependencies 100% mockable through constructor injection.
4. **Database Schema Evolution:**
   - *Risk:* Using `fallbackToDestructiveMigration()` silently wipes user favorites and collections on schema updates.
   - *Mitigation:* `AppDatabase` registers explicit `Migration` objects (with a documented `MIGRATION_1_2` template) instead of destructive migrations.

---

## 6. In-Scope vs. Out-of-Scope Features

### In-Scope (Project & Boilerplate)
- Public photo search via Pexels `GET /v1/search` with keyword query and pagination parameters.
- Thumbnail grid display (`src.tiny` / `src.small`) and detail photo display (`src.large`) via Glide.
- Saving/removing favorite photos in Room and viewing them offline.
- Local collection entities, cross-reference junction table, DAO queries, and Collections UI skeleton.
- Loading, empty, error, and retry UI states.
- Unit tests (DTO mapper, Repository, ViewModel) and Room DAO instrumentation tests.

### Out-of-Scope
- OAuth user authentication or write operations against remote accounts.
- Custom backend server, cloud synchronization, or multi-device user accounts.
- Full-resolution original (`src.original`) image redistribution or bulk downloading without license checks.
- AI-powered visual search or complex image editing.

---

## 7. 4–8-Week Implementation Roadmap

| Timeline | Phase | Deliverables |
|---|---|---|
| **Week 1** | **Phase 1: Bootstrap & Architecture Skeleton** | Strip Kotlin/Compose template, configure Gradle (`minSdk 24`), establish MVVM package structure, wire Retrofit/Room/Glide boilerplate, and pass initial unit/DAO tests. |
| **Week 2** | **Phase 2: Pexels Search & Pagination** | Complete keyword search UX, recent search suggestions, endless scroll pagination in `SearchFragment`, and error/retry edge cases. |
| **Week 3** | **Phase 3: Photo Detail & Metadata View** | Enhance `PhotoDetailActivity` with pinch-to-zoom or full metadata display (photographer, dimensions, Pexels URL) and instant favorite state synchronization. |
| **Week 4** | **Phase 4: Offline Favorites Management** | Polish `FavoritesFragment` grid, swipe-to-remove with Snackbar undo, and offline image caching verification with Glide. |
| **Weeks 5–6** | **Phase 5: Collections Feature** | Build create/rename/delete collection dialogs in `CollectionsFragment`, "Add to Collection" bottom sheet on photo detail, and collection detail photo grid. |
| **Weeks 7–8** | **Phase 6: QA, Testing & Final Polish** | Expand JUnit/Mockito/Espresso test coverage, profile main-thread/memory performance, verify accessibility/night mode, and finalize project documentation. |

For the complete **7-member team role assignment, per-member TODO checklists, Java method signatures, and expected verification results**, see [docs/TEAM_WORKLOAD_PLAN.md](TEAM_WORKLOAD_PLAN.md).
