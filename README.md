# FlickrImage Browser (Powered by Pexels API)

A native Android application written in **Java** (Android Views + XML layouts + `ViewBinding`) for discovering public photos via the **Pexels REST API** (migrated from Flickr due to Flickr restricting API keys to paid Pro subscribers), viewing photo details, saving favorites offline, and organizing photos into local collections.

---

## 1. Prerequisites

- **Android Studio:** Ladybug / Meerkat or newer (AGP `9.4.1`, Gradle `9.6.0`).
- **Android SDK:** `compileSdk 37` (`android-37.0`), `minSdk 24` (Android 7.0+).
- **JDK:** JDK 25 for the Gradle daemon (configured in `gradle/gradle-daemon-jvm.properties`), compiling Java 11 bytecode (`JavaVersion.VERSION_11`).
- **Pexels API Key:** Optional for building and running unit/database tests; required at runtime to query the live Pexels REST API (migrated from Flickr due to Flickr's Pro-only API restriction).

---

## 2. API Key Configuration & Security Note

1. Copy the template configuration file or append `PEXELS_API_KEY` to your untracked `local.properties` file in the project root:
   ```bash
   cp local.properties.example local.properties
   ```
2. Edit `local.properties` and set your free Pexels REST API key (obtainable instantly at [pexels.com/api](https://www.pexels.com/api/)):
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   PEXELS_API_KEY=your_actual_pexels_api_key
   ```
   *(Alternatively, export `PEXELS_API_KEY` as an environment variable before invoking Gradle.)*

> **Security & Licensing Note:**
> - `local.properties` is listed in `.gitignore` and must **never** be committed to version control.
> - At build time, `app/build.gradle.kts` injects `PEXELS_API_KEY` into `BuildConfig.PEXELS_API_KEY`. Be aware that **any API key embedded in a client-side APK can be extracted** by decompiling the distributed binary; client-side `BuildConfig` injection prevents source-repository leaks but does not make a key secret in production distributions.
> - Respect Pexels' Terms of Service and photographer attribution when viewing or caching images.

---

## 3. Project Structure

```text
app/src/
├── main/
│   ├── AndroidManifest.xml
│   ├── java/vn/edu/usth/flickrimagebrowser/
│   │   ├── FlickrApplication.java
│   │   ├── MainActivity.java
│   │   ├── data/
│   │   │   ├── local/        # Room AppDatabase, PhotoDao, CollectionDao, Entities
│   │   │   ├── remote/       # Retrofit PexelsApiService, PexelsClient, Pexels DTOs, PhotoMapper
│   │   │   └── repository/   # PhotoRepository interface & PhotoRepositoryImpl
│   │   ├── domain/model/     # Immutable Photo and Collection models
│   │   ├── ui/
│   │   │   ├── AppViewModelFactory.java
│   │   │   ├── common/       # Shared PhotoAdapter (Glide thumbnail loading)
│   │   │   ├── search/       # SearchFragment & SearchViewModel
│   │   │   ├── detail/       # PhotoDetailActivity & DetailViewModel
│   │   │   ├── favorites/    # FavoritesFragment & FavoritesViewModel
│   │   │   └── collections/  # CollectionsFragment, CollectionsViewModel, CollectionAdapter
│   │   └── util/             # Resource<T>, AppExecutors, AppContainer
│   └── res/                  # Material XML layouts, menus, vector drawables, themes
├── test/                     # Local JVM unit tests (JUnit 4 + Mockito + Arch Core Testing)
└── androidTest/              # Instrumented Room DAO tests (AndroidX Test + Room In-Memory DB)
```

Additional documentation:
- [docs/PROJECT_EVALUATION.md](docs/PROJECT_EVALUATION.md) — Repository audit, requirements, risks, scope boundaries, and 4–8-week roadmap.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — Component responsibilities, data flow diagrams, and key architectural decisions.
- [docs/TEAM_WORKLOAD_PLAN.md](docs/TEAM_WORKLOAD_PLAN.md) — Full 7-member team workload breakdown, TODOs, Java methods to implement, and expected verification results.

---

## 4. Build, Run & Test Commands

### Build the Debug APK
```bash
./gradlew assembleDebug
```
The compiled debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Run Local Unit Tests (DTO Mapper, Repository, ViewModel)
```bash
./gradlew testDebugUnitTest
```

### Run Instrumented Room DAO Tests (Requires Connected Device or Emulator)
```bash
./gradlew connectedDebugAndroidTest
```
*(To compile the instrumentation test APK without a running emulator, run `./gradlew assembleDebugAndroidTest`.)*

> **WSL / Linux CLI Note:** When building from WSL against the Windows Android SDK, pass JDK 25 and the workspace WSL SDK bridge:
> ```bash
> unset ANDROID_SDK_ROOT
> JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ANDROID_HOME="$PWD/.gradle/wsl-sdk" ./gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest
> ```
