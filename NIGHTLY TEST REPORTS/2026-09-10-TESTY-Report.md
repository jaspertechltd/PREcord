### 🐛 Bug: MainScreenViewModelTest fails to compile
**Location:** `app/src/test/java/com/example/precord/ui/main/MainScreenViewModelTest.kt`
**Trigger:** Running unit tests (`./gradlew test`).
**Result:** Compilation errors due to unresolved references (`MainScreenViewModel`, `MainScreenUiState`) and incorrect inheritance (`FakeMyModelRepository` implementing `DataRepository` instead of `PreferencesManager` or similar).
**Required Fix:** Update `MainScreenViewModelTest` to test the actual `MainViewModel` and `MainUiState` from `MainScreenViewModel.kt`. Replace `FakeMyModelRepository` with appropriate mocks or fakes for dependencies like `PreferencesManager` and `Application`.
