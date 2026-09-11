### 🐛 Bug: Compilation Error in `MainScreenViewModelTest.kt`
**Location:** `app/src/test/java/com/example/precord/ui/main/MainScreenViewModelTest.kt`
**Trigger:** Running unit tests via `./gradlew test` or `./gradlew compileDebugUnitTestKotlin`
**Result:** Multiple compilation errors due to unresolved references to `MainScreenViewModel`, `MainScreenUiState`, and overriding a non-open method/val in `DataRepository`.
**Required Fix:** Fix `MainScreenViewModelTest.kt` by removing or correctly implementing the tests as `MainScreenViewModel` and `MainScreenUiState` do not exist in the project scope. Also fix `FakeMyModelRepository` as it incorrectly implements `DataRepository`.
