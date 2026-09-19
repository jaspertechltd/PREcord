## 2025-01-25 - Investigate Accessibility\n**Learning:** In Compose, some icons are missing or incorrectly setting `contentDescription = null` without adding alternative context for screen readers.\n**Action:** Replaced `contentDescription = null` with a meaningful description where relevant.
## 2024-09-11 - Making switch rows clickable
**Learning:** In Compose, users expect the entire row containing a label, description, and Switch to be clickable, not just the switch itself. Making only the switch clickable reduces accessibility and tap targets.
**Action:** Use `.clickable(role = Role.Switch) { onCheckedChange(!checked) }` on the parent `Row` and set the Switch's `onCheckedChange` to `null` to delegate events properly.
## 2025-01-25 - Accurate accessibility announcements for Switch rows
**Learning:** Using `Modifier.clickable(role = Role.Switch)` on a parent container does not correctly announce the checked state to screen readers (like TalkBack) since `clickable` doesn't inherently track a toggled state. Screen readers may announce it simply as "Switch" or fail to state whether it's "On" or "Off".
**Action:** Always use `Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)` for parent containers of switch-like components to ensure correct state announcements along with the component role.
