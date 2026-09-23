MainScreenViewModel requires an Application context and SharedPreferences mocking (e.g. Mockito) for robust UI state testing without crashing.

When parsing triage folders, verify file existence before attempting to parse. For example `CODEFIXES` and `GENERAL CHANGES` directories might be missing.
