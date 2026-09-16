## 2025-01-25 - Investigate Accessibility\n**Learning:** In Compose, some icons are missing or incorrectly setting `contentDescription = null` without adding alternative context for screen readers.\n**Action:** Replaced `contentDescription = null` with a meaningful description where relevant.
## 2024-09-11 - Making switch rows clickable
**Learning:** In Compose, users expect the entire row containing a label, description, and Switch to be clickable, not just the switch itself. Making only the switch clickable reduces accessibility and tap targets.
**Action:** Use `.clickable(role = Role.Switch) { onCheckedChange(!checked) }` on the parent `Row` and set the Switch's `onCheckedChange` to `null` to delegate events properly.
## 2025-02-12 - Using Modifier.toggleable for switch rows
**Learning:** In Jetpack Compose, using `Modifier.toggleable` instead of `Modifier.clickable(role = Role.Switch)` on switch rows is better for accessibility. It provides accurate state announcements for screen readers (e.g. "on/off" instead of just acting like a button) by binding directly to the switch state.
**Action:** Use `.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)` on the parent `Row` of toggleable components instead of `clickable` to ensure correct state checking and accessibility announcements.
