## 2024-05-30 - Compose Toggleable Accessibility
**Learning:** In Jetpack Compose, when building custom switches or toggle rows, `Modifier.toggleable` is preferred over `Modifier.clickable(role = Role.Switch)`. `toggleable` natively manages and communicates the checked state to accessibility services (like TalkBack), whereas `clickable` alone with a role does not reliably announce state changes without manual semantics overrides.
**Action:** Always use `Modifier.toggleable` for components representing on/off or true/false states.
