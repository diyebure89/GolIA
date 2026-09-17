# Code Quality Configuration

This document describes the code quality tools and configurations set up for the GolIA Android project.

## Tools Overview

### 1. Detekt - Static Code Analysis for Kotlin
- **Purpose**: Analyze Kotlin code for code smells, complexity issues, and style violations
- **Configuration**: `config/detekt/detekt.yml`
- **Run**: `./gradlew detekt`
- **Reports**: `build/reports/detekt/`

### 2. KtLint - Kotlin Linter
- **Purpose**: Enforce Kotlin coding standards and formatting
- **Configuration**: Built-in rules with Android extensions
- **Check**: `./gradlew ktlintCheck`
- **Format**: `./gradlew ktlintFormat`
- **Reports**: `build/reports/ktlint/`

### 3. Spotless - Code Formatter
- **Purpose**: Automatic code formatting and license header management
- **Configuration**: `code-quality.gradle`
- **Check**: `./gradlew spotlessCheck`
- **Apply**: `./gradlew spotlessApply`

### 4. Android Lint
- **Purpose**: Android-specific static analysis
- **Configuration**: `config/lint.xml`
- **Run**: `./gradlew lintDebug` or `./gradlew lintRelease`
- **Reports**: `app/build/reports/lint/`

## Quality Gates

### Build-time Checks
The following checks are automatically enforced during builds:

1. **Strict Mode**: All warnings are treated as errors
2. **Maximum Issues**: Detekt fails if more than 10 issues are found
3. **Formatting**: Code must follow formatting rules
4. **License Headers**: All files must have proper license headers

### CI/CD Integration
GitHub Actions workflow (`/.github/workflows/code-quality.yml`) runs:
- Weekly scheduled checks
- On every push to main/develop branches
- On every pull request

## Code Style Conventions

### Kotlin Style
- **Indentation**: 4 spaces (no tabs)
- **Line Length**: 120 characters maximum
- **Naming**:
  - Classes: `PascalCase`
  - Functions/Variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- **Imports**: Sorted by package with Android/AndroidX first

### Architecture Rules
1. **ViewModel Rules**: No data classes in ViewModels
2. **Coroutine Rules**: Proper coroutine scope management
3. **Security Rules**: No hardcoded secrets, proper permission handling
4. **Performance Rules**: Avoid memory leaks, optimize resource usage

## Commands

### Run All Quality Checks
```bash
./gradlew codeQualityCheck
```

### Format Code
```bash
./gradlew codeQualityFormat
```

### Individual Tools
```bash
# Static analysis
./gradlew detekt

# Linting
./gradlew ktlintCheck
./gradlew ktlintFormat

# Formatting
./gradlew spotlessCheck
./gradlew spotlessApply

# Android lint
./gradlew lintDebug
```

## Configuration Files

- `config/detekt/detekt.yml` - Detekt rules and thresholds
- `config/lint.xml` - Android lint configuration
- `config/code-style.xml` - IDE code style settings
- `code-quality.gradle` - Gradle plugin configurations
- `/.github/workflows/code-quality.yml` - CI/CD pipeline

## Custom Rules

### Security Rules
- No hardcoded API keys or secrets
- Proper encryption for sensitive data
- Certificate pinning for API calls
- Secure storage using Android Keystore

### Performance Rules
- Avoid memory leaks in ViewModels
- Proper coroutine cancellation
- Efficient database queries
- Image loading optimization

### Architecture Rules
- Clean Architecture separation
- Proper dependency injection
- Single responsibility principle
- Interface segregation

## Baseline Files

When introducing quality tools to an existing codebase, baseline files are used:
- `config/lint-baseline.xml` - Android lint baseline
- Detekt automatically creates baseline on first run

To update baselines after fixing issues:
```bash
./gradlew lintDebug -PupdateLintBaseline=true
```

## IDE Integration

### Android Studio/IntelliJ IDEA
1. Import code style: `File → Settings → Editor → Code Style → Import Scheme`
2. Select `config/code-style.xml`
3. Enable Detekt plugin: `Settings → Plugins → Detekt`
4. Configure auto-format on save

### Pre-commit Hooks (Optional)
```bash
#!/bin/bash
# .git/hooks/pre-commit
./gradlew ktlintFormat spotlessApply
git add .
```

## Troubleshooting

### Common Issues

1. **Detekt fails with too many issues**
   - Run with `--build-upon-default-config` flag
   - Create baseline: `detektBaseline`
   - Adjust thresholds in `detekt.yml`

2. **KtLint formatting conflicts**
   - Run `ktlintFormat` first
   - Then run `spotlessApply`
   - Check import ordering rules

3. **Android lint warnings**
   - Review `lint.xml` configuration
   - Update baseline if needed
   - Check for false positives

4. **Build performance**
   - Use Gradle caching
   - Run tools only on changed files
   - Configure parallel execution