# Release Process

This document outlines the complete process for releasing a new version of the Keep Alive Android app.

## Prerequisites

Before starting a release, ensure you have:

1. **GitHub Repository Access**: Write access to the repository
2. **GitHub Secrets Configured**: The following secrets must be set in the repository settings:
   - `KEYSTORE_BASE64`: Base64-encoded production keystore file
   - `KEYSTORE_PASSWORD`: Password for the keystore
   - `KEY_ALIAS`: Alias of the key inside the keystore
   - `KEY_PASSWORD`: Password for the key (if different from keystore password)
3. **Local Development Setup**: Android Studio with the project configured
4. **Git Command Line**: For tagging and pushing

## Release Steps

### 1. Prepare the Version

Update the version in `app/build.gradle.kts`:

```kotlin
versionCode = 17  // Increment by 1
versionName = "2.3"  // Update semantic version
```

Commit the version change:
```bash
git add app/build.gradle.kts
git commit -m "[BUMP] Prepare for v2.3 release"
git push origin main
```

### 2. Generate Changelog

Review commits since the last release to create release notes:

```bash
# View commits since last release
git log v2.2..HEAD --oneline

# Filter for meaningful changes
git log v2.2..HEAD --pretty=format:"%s" | grep -E "^(feat|fix|FIXED|style|refactor|perf|test|docs|chore)"
```

### 3. Create and Push Git Tag

Create an annotated tag for the release:

```bash
# Create annotated tag
git tag -a v2.3 -m "Release v2.3 - Brief description"

# Push the tag to GitHub
git push origin v2.3
```

### 4. Create GitHub Release

1. Go to https://github.com/hossain-khan/android-keep-alive/releases/new
2. Select the tag you just pushed (e.g., `v2.3`)
3. Set the release title (e.g., `v2.3`)
4. Add release notes following this template:

```markdown
## What's New in v2.3

### ✨ Features
- List new features with PR references

### 🐛 Bug Fixes
- List bug fixes

### 🔧 Improvements
- List improvements and optimizations

**Full Changelog**: https://github.com/hossain-khan/android-keep-alive/compare/v2.2...v2.3
```

5. Click **"Publish release"**

### 5. Automated Build

Once the release is published:

1. The GitHub Actions workflow (`.github/workflows/android-release.yml`) automatically triggers
2. It builds a signed release APK using the production keystore from secrets
3. The APK is automatically uploaded and attached to the release
4. APK naming convention: `keep-alive-release-v{version}.apk` (e.g., `keep-alive-release-v2.3.apk`)

You can monitor the build progress in the [Actions tab](https://github.com/hossain-khan/android-keep-alive/actions).

### 6. Verify Release

After the workflow completes:

1. Check that the APK is attached to the release page
2. Download and test the APK on a device
3. Verify the version number matches in the app's About screen

## Manual Build (Optional)

To manually trigger the release build workflow for testing:

1. Go to the [Actions tab](https://github.com/hossain-khan/android-keep-alive/actions)
2. Select "Android Release Build" workflow
3. Click "Run workflow" button
4. Select the branch and run

## Version Numbering

Follow [Semantic Versioning](https://semver.org/):

- **MAJOR**: Incompatible API changes or major feature overhauls (e.g., 1.0.0 → 2.0.0)
- **MINOR**: New features in a backwards-compatible manner (e.g., 2.1.0 → 2.2.0)
- **PATCH**: Backwards-compatible bug fixes (e.g., 2.1.0 → 2.1.1)

`versionCode` should always increment by 1 for each release, regardless of version type.

## Rollback

If you need to rollback a release:

1. Delete the GitHub release (this does not delete the tag)
2. Delete the tag locally and remotely:
   ```bash
   git tag -d v2.3
   git push origin :refs/tags/v2.3
   ```
3. Fix the issues
4. Start the release process again with a new version

## Post-Release

After a successful release:

1. Update project documentation if needed
2. Close related GitHub issues and PRs
3. Announce the release (if applicable)
4. Monitor for any crash reports or issues

## Reference

- [GitHub Release Workflow Documentation](.github/workflows/README.md)
- [Android App Signing Documentation](https://developer.android.com/studio/publish/app-signing)
- [Contributing Guidelines](CONTRIBUTING.md)
