# Contributing to Android Keep Alive

## Current State & AI Development
At its current state, the app does not follow all well-known Android architecture patterns, making the app somewhat harder to understand and contribute to. Most code is written with assistance from AI copilots like GitHub Copilot, Gemini, and ChatGPT.

## Development with GitHub Copilot
This project is optimized for GitHub Copilot development. Key files for context:
- `.copilot-instructions.md` - Project-specific guidelines and constraints
- `.copilot-dev-env.md` - Development environment setup
- `.copilot-firewall.md` - Network access requirements

## Architecture Guidelines
While not following strict patterns, the project maintains:
- **Repository Pattern**: `SettingsRepository` for data access
- **MVVM**: ViewModels with Compose UI state management
- **Single Activity**: Compose Navigation architecture
- **Reactive Data**: DataStore with Flow-based updates

## Code Style & Quality
- **Formatting**: Use ktlint (`./gradlew ktlintFormat`)
- **Language**: Kotlin with modern coroutines and Flow
- **UI**: Jetpack Compose with Material3 design
- **Testing**: Unit tests for business logic, Compose tests for UI

## Important Constraints
⚠️ **Critical**: Do not target Android API 35 - it breaks foreground service functionality
- Current target: API 34
- Minimum: API 28
- Use `SYSTEM_ALERT_WINDOW` permission carefully

## Contribution Workflow
1. **Discuss First**: Open an issue to discuss features or major changes
2. **Environment**: Set up development environment per `.copilot-dev-env.md`
3. **Branch**: Create feature branch from main
4. **Develop**: Use GitHub Copilot with project context files
5. **Test**: Verify on physical device (permissions required)
6. **Format**: Run `./gradlew ktlintFormat` before committing
7. **PR**: Submit with clear description and testing notes

## Testing Requirements
- Unit tests for repository and utility classes
- Compose UI tests for screen interactions
- Manual testing on device for permission-dependent features
- Verify foreground service behavior

## Areas for Improvement
- Better separation of concerns in UI components
- More comprehensive error handling
- Enhanced testing coverage
- Migration to newer architecture patterns (MVI, Clean Architecture)

Feel free to open an issue first to get specific guidelines on how to approach feature work or bugfixes.

Thank you for your support! 🎉
