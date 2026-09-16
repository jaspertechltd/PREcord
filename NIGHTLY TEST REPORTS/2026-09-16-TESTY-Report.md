### 🐛 Bug: Syntax error in MainScreenViewModelTest
**Location:** `app/src/test/java/com/example/precord/ui/main/MainScreenViewModelTest.kt`
**Trigger:** Running unit tests (`./gradlew test`).
**Result:** Compilation error due to invalid Kotlin syntax (duplicate class declaration and unresolved `assertTrue`).
**Required Fix:** Fix the syntax errors by removing the duplicate `MainScreenViewModelTest` class declaration at the end of the file, merging its contents or removing the dummy test entirely.
