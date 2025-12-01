# Copilot Instructions for android-keep-alive

## Project Overview

**Keep Alive** is an Android app that monitors and automatically restarts user-configured applications. It runs a foreground service (`WatchdogService`) that periodically checks if selected apps are running and attempts to restart them if not. The app is designed for secondary devices (not daily use phones) due to high battery consumption.

**Key Features:** App monitoring, auto-restart, remote logging via Airtable, health check pings via healthchecks.io.

## Technology Stack

- **Language:** Kotlin
- **Build System:** Gradle 8.7 with Kotlin DSL
- **Min SDK:** 24 (Android 7 Nougat), **Target SDK:** 34 (Android 14)
- **UI:** Jetpack Compose with Material3
- **Architecture:** Single-module Android app (no strict architecture pattern)
- **Key Libraries:** Jetpack Compose, Navigation Compose, DataStore, OkHttp, Timber, kotlinx-serialization

## Build Commands

**Always run these commands from the repository root directory.**

### Quick Validation (CI Pipeline)
```bash
./gradlew :app:ktlintCheck :app:assembleDebug
```
This is the primary CI build command. It takes ~3 minutes on first run.

### Run Unit Tests
```bash
./gradlew :app:test
```
Takes ~40 seconds (incremental build).

### Full Build with Lint
```bash
./gradlew clean :app:build
```
Takes ~2-3 minutes. Runs ktlint, lint, assembles debug and release APKs, and runs tests.

### Format Code
```bash
./gradlew :app:ktlintFormat
```
Run this before committing to auto-fix ktlint issues.

### Assemble Release APK
```bash
./gradlew assembleRelease
```
Uses `secret.template.properties` with debug keystore for local builds. CI uses environment secrets.

## Important Notes

1. **Run `chmod +x gradlew` once** after cloning the repository if gradle wrapper permissions are not set.
2. **JDK 21 is required** (uses Temurin distribution in CI).
3. **No secret.properties file locally** - The build automatically falls back to `secret.template.properties` which uses the debug keystore.
4. The warning `"Unable to strip the following libraries..."` during builds is normal and can be ignored.

## Project Structure

```
android-keep-alive/
├── app/                           # Main Android application module
│   ├── build.gradle.kts           # App-level build config with ktlint, signing
│   ├── src/main/
│   │   ├── java/dev/hossain/keepalive/
│   │   │   ├── MainActivity.kt    # Entry point, handles permissions
│   │   │   ├── KeepAliveApplication.kt  # App initialization, logging setup
│   │   │   ├── service/WatchdogService.kt  # Core monitoring service
│   │   │   ├── broadcast/         # Boot receiver, notification actions
│   │   │   ├── data/              # DataStore, settings, models
│   │   │   ├── ui/screen/         # Compose screens and viewmodels
│   │   │   ├── util/              # Helpers (permissions, launcher, notifications)
│   │   │   └── log/               # Remote logging (Airtable)
│   │   ├── res/                   # Resources (layouts, strings, drawables)
│   │   └── AndroidManifest.xml    # Permissions and components
│   └── src/test/                  # Unit tests
├── gradle/libs.versions.toml      # Centralized dependency versions
├── build.gradle.kts               # Root build file
├── settings.gradle.kts            # Project settings
├── gradle.properties              # Gradle/Android properties
├── keystore/                      # Contains debug.keystore for CI
├── secret.template.properties     # Template for signing config (used in CI)
└── .github/workflows/
    ├── android.yml                # CI: ktlint + assembleDebug on PRs
    └── android-release.yml        # Release: signed APK on GitHub releases
```

## Configuration Files

- **Linting:** `.editorconfig` (ktlint config), `app/build.gradle.kts` (ktlint plugin)
- **Code Style:** ktlint with Android profile, trailing commas allowed
- **Signing:** `secret.template.properties` for debug builds; CI uses GitHub secrets

## CI/CD Workflows

### `android.yml` - PR Validation
- Triggers on: push to `main`, PRs to `main`
- Runs: `./gradlew :app:ktlintCheck :app:assembleDebug`
- Full build (`clean build`) only on push to main

### `android-release.yml` - Release Build
- Triggers on: GitHub release published
- Produces signed APK with production keystore from secrets

## Testing

Run unit tests with:
```bash
./gradlew :app:test
```

Test files are in `app/src/test/java/dev/hossain/keepalive/`. Tests cover utilities, data classes, and UI components.

## Making Changes

1. **Before committing:** Run `./gradlew :app:ktlintFormat` to format code
2. **Before PR:** Run `./gradlew :app:ktlintCheck :app:assembleDebug` to validate
3. **Version changes:** Update `versionCode` and `versionName` in `app/build.gradle.kts`
4. **New dependencies:** Add to `gradle/libs.versions.toml`

## Known Limitations

- **Android 16+ (API 36)** is not supported due to background-start and overlay restrictions
- The app requires multiple sensitive permissions - see README.md for details
- `RECEIVE_BOOT_COMPLETED` may not trigger until device is unlocked

## Trust These Instructions

These instructions have been validated against the actual repository. Only perform additional searches if information is incomplete or found to be incorrect during execution.
