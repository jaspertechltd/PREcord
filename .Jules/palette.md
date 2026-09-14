## 2025-01-25 - Investigate Accessibility\n**Learning:** In Compose, some icons are missing or incorrectly setting `contentDescription = null` without adding alternative context for screen readers.\n**Action:** Replaced `contentDescription = null` with a meaningful description where relevant.
## 2024-09-11 - Making switch rows clickable
**Learning:** In Compose, users expect the entire row containing a label, description, and Switch to be clickable, not just the switch itself. Making only the switch clickable reduces accessibility and tap targets.
**Action:** Use `.clickable(role = Role.Switch) { onCheckedChange(!checked) }` on the parent `Row` and set the Switch's `onCheckedChange` to `null` to delegate events properly.
## 2025-01-26 - Accessible Toggles in Compose
**Learning:** Using `Modifier.clickable(role = Role.Switch)` on a toggleable component incorrectly announces the component to screen readers without keeping track of the state appropriately. Prefer `Modifier.toggleable` to ensure accurate and accessible announcements and state tracking for switches.
**Action:** Use `Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)` instead of `clickable` on toggleable row components.
