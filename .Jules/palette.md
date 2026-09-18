## 2025-01-25 - Investigate Accessibility\n**Learning:** In Compose, some icons are missing or incorrectly setting `contentDescription = null` without adding alternative context for screen readers.\n**Action:** Replaced `contentDescription = null` with a meaningful description where relevant.
## 2024-09-11 - Making switch rows clickable
**Learning:** In Compose, users expect the entire row containing a label, description, and Switch to be clickable, not just the switch itself. Making only the switch clickable reduces accessibility and tap targets.
**Action:** Use `.clickable(role = Role.Switch) { onCheckedChange(!checked) }` on the parent `Row` and set the Switch's `onCheckedChange` to `null` to delegate events properly.
## 2026-09-18 - Making toggleable rows accessible
**Learning:** In Jetpack Compose, using `Modifier.clickable(role = Role.Switch)` on toggleable components can cause incorrect state checking and inaccurate announcements for screen readers.
**Action:** Use `Modifier.toggleable` instead of `Modifier.clickable(role = Role.Switch)` to ensure accurate state handling and screen reader announcements.
