# NIGHTLY TEST REPORT - 2026-09-15

## Critical Crashes & Bugs

### 🐛 Bug: Syntax Error in Unit Tests
**Location:** `app/src/test/java/com/example/precord/ui/main/MainScreenViewModelTest.kt` (lines 45-53)
**Trigger:** Running unit tests (`./gradlew test`).
**Result:** Compilation error due to syntax issues (expecting member declaration, unresolved reference `assertTrue`, missing `}`). Test suite fails to run.
**Required Fix:** Fix the syntax error in `MainScreenViewModelTest.kt`. The file seems to have a stray `import` and a `dummyTest` outside of the class structure or improperly formatted. Correct the class structure and import statements so the unit tests can compile and run successfully.

## General Improvements

### 🐛 Bug: Inconsistent Dependency Management
**Location:** `app/build.gradle.kts` (lines 64, 91)
**Trigger:** Running static analysis (`./gradlew lint`).
**Result:** Lint warning: "Use version catalog instead". Dependencies `androidx.compose.material:material-icons-extended` and `androidx.documentfile:documentfile:1.0.1` are declared directly in the build script instead of using the version catalog (`libs.versions.toml`).
**Required Fix:** Move the dependencies for `material-icons-extended` and `documentfile` to `gradle/libs.versions.toml` and reference them in `build.gradle.kts` using the version catalog alias.

### 🐛 Bug: Suboptimal KTX Usage
**Location:** Various files including `CloudUploadManager.kt`, `OnboardingScreen.kt`, and `PreferencesManager.kt`.
**Trigger:** Running static analysis (`./gradlew lint`).
**Result:** Lint warnings suggesting to use KTX extension functions (e.g., `String.toUri()` instead of `Uri.parse()`, and `SharedPreferences.edit` instead of explicitly using the editor).
**Required Fix:** Refactor code to use standard Kotlin KTX extension functions as suggested by the lint report to improve code idiomaticity and readability. For example, replace `Uri.parse(uriString)` with `uriString.toUri()`.
