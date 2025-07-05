# Android Release Workflow

This workflow automates the process of building and distributing signed release APKs when a GitHub release is published.

## How it works

1. **Trigger**: The workflow triggers automatically when a GitHub release is published
2. **Build**: It builds a signed release APK using the production keystore
3. **Upload**: The APK is automatically attached to the GitHub release

## Prerequisites

The following GitHub secrets must be configured in your repository:

- `KEYSTORE_BASE64`: Base64-encoded production keystore file
- `KEYSTORE_PASSWORD`: Password for the keystore
- `KEY_ALIAS`: Alias of the key inside the keystore  
- `KEY_PASSWORD`: Password for the key (if different from keystore password)

## APK Naming Convention

The generated APK will be named: `keep-alive-release-v{version}.apk`

For example: `keep-alive-release-v2.2.apk`

## Manual Testing

The workflow can also be triggered manually for testing purposes:

1. Go to the "Actions" tab in your repository
2. Select "Android Release Build" workflow
3. Click "Run workflow" button

## Usage

To create a new release with automatic APK attachment:

1. Create a new release on GitHub
2. The workflow will automatically build and attach the signed APK
3. Users can download the APK directly from the release page

## Files Created

- Workflow file: `.github/workflows/android-release.yml`
- The workflow uses the existing keystore setup from the test workflow
- No additional configuration files needed