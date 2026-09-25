## 2025-01-25 - Investigate Accessibility\n**Learning:** In Compose, some icons are missing or incorrectly setting `contentDescription = null` without adding alternative context for screen readers.\n**Action:** Replaced `contentDescription = null` with a meaningful description where relevant.
## 2024-09-11 - Making switch rows clickable
**Learning:** In Compose, users expect the entire row containing a label, description, and Switch to be clickable, not just the switch itself. Making only the switch clickable reduces accessibility and tap targets.
**Action:** Use `.clickable(role = Role.Switch) { onCheckedChange(!checked) }` on the parent `Row` and set the Switch's `onCheckedChange` to `null` to delegate events properly.
## 2025-01-25 - Improve Accessibility for Toggleable and Selectable Components
**Learning:** In Jetpack Compose, using `Modifier.toggleable` (for Checkboxes/Switches) and `Modifier.selectable` (for RadioButtons) instead of just `Modifier.clickable` with or without roles provides correct state checking and accurate state announcements for screen readers. Parent containers for interactive components should properly expose their semantic states.
**Action:** Replace `Modifier.clickable` with `Modifier.toggleable` or `Modifier.selectable` appropriately on parent containers (Rows) for toggleable and selectable items.
