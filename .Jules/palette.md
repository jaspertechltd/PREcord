## 2025-01-25 - Investigate Accessibility\n**Learning:** In Compose, some icons are missing or incorrectly setting `contentDescription = null` without adding alternative context for screen readers.\n**Action:** Replaced `contentDescription = null` with a meaningful description where relevant.
## 2024-09-11 - Making switch rows clickable
**Learning:** In Compose, users expect the entire row containing a label, description, and Switch to be clickable, not just the switch itself. Making only the switch clickable reduces accessibility and tap targets.
**Action:** Use `.clickable(role = Role.Switch) { onCheckedChange(!checked) }` on the parent `Row` and set the Switch's `onCheckedChange` to `null` to delegate events properly.
## 2024-05-18 - [Accessibility] Improve screen reader support for toggleable rows
**Learning:** In Jetpack Compose, prefer `Modifier.toggleable` over `Modifier.clickable(role = Role.Switch)` for toggleable components to ensure correct state checking and accurate announcements for screen readers.
**Action:** Use `Modifier.toggleable` with `value` and `onValueChange` parameters for custom toggle rows instead of `clickable` with `Role.Switch`.
