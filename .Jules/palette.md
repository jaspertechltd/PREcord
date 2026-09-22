## 2025-01-25 - Investigate Accessibility\n**Learning:** In Compose, some icons are missing or incorrectly setting `contentDescription = null` without adding alternative context for screen readers.\n**Action:** Replaced `contentDescription = null` with a meaningful description where relevant.
## 2024-09-11 - Making switch rows clickable
**Learning:** In Compose, users expect the entire row containing a label, description, and Switch to be clickable, not just the switch itself. Making only the switch clickable reduces accessibility and tap targets.
**Action:** Use `.clickable(role = Role.Switch) { onCheckedChange(!checked) }` on the parent `Row` and set the Switch's `onCheckedChange` to `null` to delegate events properly.
## 2025-01-25 - Use toggleable instead of clickable for switch rows
**Learning:** In Compose, `Modifier.toggleable` should be preferred over `Modifier.clickable(role = Role.Switch)` for custom switch rows. Using `toggleable` ensures accurate state announcements (checked/unchecked) for screen readers.
**Action:** Always use `Modifier.toggleable` on the parent container when creating switch rows, and pass the `checked` value and `onValueChange` lambda to it. Set the inner `Switch`'s `onCheckedChange` to `null`.
