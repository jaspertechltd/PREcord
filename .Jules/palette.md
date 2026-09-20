## 2025-01-25 - Investigate Accessibility\n**Learning:** In Compose, some icons are missing or incorrectly setting `contentDescription = null` without adding alternative context for screen readers.\n**Action:** Replaced `contentDescription = null` with a meaningful description where relevant.
## 2024-09-11 - Making switch rows clickable
**Learning:** In Compose, users expect the entire row containing a label, description, and Switch to be clickable, not just the switch itself. Making only the switch clickable reduces accessibility and tap targets.
**Action:** Use `.clickable(role = Role.Switch) { onCheckedChange(!checked) }` on the parent `Row` and set the Switch's `onCheckedChange` to `null` to delegate events properly.
## 2025-01-25 - Improve Accessibility of Selection Controls
**Learning:** In Jetpack Compose, using `Modifier.clickable` with `Role.RadioButton` or `Role.Switch` on parent containers doesn't fully propagate state to screen readers.
**Action:** Use `Modifier.selectable` for parent containers of `RadioButton`s and `Modifier.toggleable` for parent containers of `Switch`es. This ensures accurate semantic roles, state announcements, and larger tap targets for users relying on assistive technologies.
