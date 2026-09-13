## 2025-01-25 - Investigate Accessibility\n**Learning:** In Compose, some icons are missing or incorrectly setting `contentDescription = null` without adding alternative context for screen readers.\n**Action:** Replaced `contentDescription = null` with a meaningful description where relevant.
## 2024-09-11 - Making switch rows clickable
**Learning:** In Compose, users expect the entire row containing a label, description, and Switch to be clickable, not just the switch itself. Making only the switch clickable reduces accessibility and tap targets.
**Action:** Use `.clickable(role = Role.Switch) { onCheckedChange(!checked) }` on the parent `Row` and set the Switch's `onCheckedChange` to `null` to delegate events properly.

## 2026-09-13 - Improve Switch Accessibility
**Learning:** In Compose, prefer `Modifier.toggleable` over `Modifier.clickable(role = Role.Switch)` for toggleable components (like rows with a Switch) to ensure correct state checking and accurate announcements for screen readers.
**Action:** Use `.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)` instead of `clickable`.
