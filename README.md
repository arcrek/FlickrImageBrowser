# FlickrImage Browser (Powered by Pixabay API)

A native Android application written in **Java** (Android Views + XML layouts + `ViewBinding`) for discovering public photos via the **Pixabay REST API** (migrated from Flickr and Pexels due to API signup/approval restrictions), viewing photo details, saving favorites offline, and organizing photos into local collections.

---

## 1. Prerequisites

- **Android Studio:** Ladybug / Meerkat or newer (AGP `9.4.1`, Gradle `9.6.0`).
- **Android SDK:** `compileSdk 37` (`android-37.0`), `minSdk 24` (Android 7.0+).
- **JDK:** JDK 25 for the Gradle daemon (configured in `gradle/gradle-daemon-jvm.properties`), compiling Java 11 bytecode (`JavaVersion.VERSION_11`).
- **Pixabay API Key:** Optional for building and running unit/database tests; required at runtime to query the live Pixabay REST API.

---

## 2. API Key Configuration & Security Note

1. Copy the template configuration file or append `PIXABAY_API_KEY` to your untracked `local.properties` file in the project root:
   ```bash
   cp local.properties.example local.properties
   ```
2. Edit `local.properties` and set your free Pixabay REST API key (obtainable instantly upon signing in at [pixabay.com/api/docs/](https://pixabay.com/api/docs/)):
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   PIXABAY_API_KEY=your_actual_pixabay_api_key
   ```
   *(Alternatively, export `PIXABAY_API_KEY` as an environment variable before invoking Gradle.)*

> **Security & Licensing Note:**
> - `local.properties` is listed in `.gitignore` and must **never** be committed to version control.
> - At build time, `app/build.gradle.kts` injects `PIXABAY_API_KEY` into `BuildConfig.PIXABAY_API_KEY`. Be aware that **any API key embedded in a client-side APK can be extracted** by decompiling the distributed binary; client-side `BuildConfig` injection prevents source-repository leaks but does not make a key secret in production distributions.
> - Respect Pixabay's Content License and API Terms of Service (including 24-hour caching guidelines and creator attribution) when viewing or caching images.

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
│   │   │   ├── remote/       # Retrofit PixabayApiService, PixabayClient, Pixabay DTOs, PhotoMapper
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
- [docs/PRESENTATION_CHEATSHEET.md](docs/PRESENTATION_CHEATSHEET.md) — Short presentation cheatsheet covering intro, architecture, activities, networking, UI overdraw, demo flow, and conclusions.

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

### Probe Pixabay Through a Temporary Python Proxy

Run `python3 scripts/test-pixabay-proxy.py` with Python 3 and the existing API-key configuration. By default, the script uses standard-library HTTP requests, starts a loopback proxy on port `18765`, and tests search, Featured, and image downloads. It prints status/content-type/challenge metadata without the key, verifies that the proxy preserves upstream responses, and stops the server automatically. A completed probe returns exit code `1` when at least one route fails; it does not connect the Android app to the proxy or prove access from a hosted server.

The probe sends Edge 156 navigation/client-hint headers based on the supplied HAR, advertises gzip compression, and decodes gzip responses. By default its upstream transport is HTTP/1.1. With `curl_cffi` installed in the active Python environment, run `python scripts/test-pixabay-proxy.py --http3` to require HTTP/3 without fallback. Output includes the negotiated upstream HTTP version and transport errors. This mode uses curl_cffi's default TLS/QUIC configuration rather than reproducing the Edge fingerprint.

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
