### 🐛 Bug: Test Compilation Failure
**Location:** `app/src/test/java/com/example/precord/ui/main/MainScreenViewModelTest.kt`
**Trigger:** Running unit tests using `./gradlew test`
**Result:** `Syntax error: Expecting member declaration` at lines 45 and 46. The test file appears to contain duplicate/appended class declarations.
**Required Fix:** Correct the syntax in `MainScreenViewModelTest.kt` by removing the duplicate imports and duplicate class declaration at the end of the file. Ensure the file has valid Kotlin syntax and the tests compile.
