### 🐛 Bug: Compilation Error in Unit Tests
**Location:** `app/src/test/java/com/example/precord/ui/main/MainScreenViewModelTest.kt`
**Trigger:** Running unit tests via `./gradlew test` or `./gradlew compileDebugUnitTestKotlin`.
**Result:** Build fails with `Compilation error. See log for more details`. The file contains syntax errors at lines 45-46 and a missing closing brace on line 53, plus an unused `assertTrue` import, preventing compilation.
**Required Fix:** Correct the syntax errors in `MainScreenViewModelTest.kt` by properly structuring the `MainScreenViewModelTest` class and removing duplicate/invalid declarations so the tests successfully compile and run.
