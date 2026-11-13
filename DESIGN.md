---
name: "WinKu 小账本"
description: "WinKu F2 identity applied to the local Android ledger"
colors:
  brand: "#0E8A4A"
  action-fill: "#0B6E3B"
  on-accent: "#FFFFFF"
  light-bg: "#F7F7F9"
  light-bg-soft: "#F2F2F5"
  light-surface: "#FFFFFF"
  light-surface-2: "#F0F0F4"
  light-surface-3: "#E8E8EE"
  light-line: "rgba(23,23,26,.09)"
  light-line-strong: "rgba(23,23,26,.18)"
  light-text: "#17171A"
  light-text-2: "#55555E"
  light-muted: "#666670"
  light-link: "#0B6E3B"
  light-income: "#1E7D46"
  light-expense: "#C0392B"
  light-warn: "#A56A08"
  dark-bg: "#0E0E10"
  dark-bg-soft: "#121214"
  dark-surface: "#17171A"
  dark-surface-2: "#1C1C20"
  dark-surface-3: "#242429"
  dark-line: "rgba(255,255,255,.09)"
  dark-line-strong: "rgba(255,255,255,.18)"
  dark-text: "#F5F5F7"
  dark-text-2: "#B4B4BE"
  dark-muted: "#A0A0AB"
  dark-link: "#4FBF86"
  dark-income: "#5FC79B"
  dark-expense: "#E8797F"
  dark-warn: "#E5B45C"
typography:
  headline: {fontFamily: 'Roboto, "Noto Sans CJK SC", sans-serif', fontSize: "23px", fontWeight: 700, letterSpacing: "-.02em"}
  automation-title: {fontFamily: 'Roboto, "Noto Sans CJK SC", sans-serif', fontSize: "23px", fontWeight: 700, lineHeight: 1.3, letterSpacing: "-.025em"}
  amount: {fontFamily: 'Roboto, "Noto Sans CJK SC", sans-serif', fontSize: "24px", fontWeight: 720, lineHeight: 1.2, letterSpacing: "-.035em"}
  section: {fontFamily: 'Roboto, "Noto Sans CJK SC", sans-serif', fontSize: "16px", fontWeight: 700}
  body: {fontFamily: 'Roboto, "Noto Sans CJK SC", sans-serif', fontSize: "14px", fontWeight: 400, lineHeight: 1.55}
  label: {fontFamily: 'Roboto, "Noto Sans CJK SC", sans-serif', fontSize: "13px", fontWeight: 600}
  caption: {fontFamily: 'Roboto, "Noto Sans CJK SC", sans-serif', fontSize: "12px", fontWeight: 400}
  navigation: {fontFamily: 'Roboto, "Noto Sans CJK SC", sans-serif', fontSize: "11px", fontWeight: 500}
rounded:
  small: "9px"
  control: "10px"
  icon-tile: "12px"
  status: "14px"
  card-mobile: "15px"
  surface: "16px"
  modal-mobile: "18px"
  inbox-tab: "24px"
spacing:
  gap-small: "8px"
  gap: "12px"
  inset: "16px"
  phone-content: "18px"
  wide-card: "20px"
  section: "26px"
  target: "48px"
  fab: "56px"
components:
  button-primary: {backgroundColor: "{colors.action-fill}", textColor: "{colors.on-accent}", typography: "{typography.label}", rounded: "{rounded.control}", padding: "9px 15px"}
  button-secondary: {backgroundColor: "{colors.light-surface-2}", textColor: "{colors.light-text-2}", typography: "{typography.label}", rounded: "{rounded.control}", padding: "9px 15px"}
  button-ghost: {backgroundColor: "transparent", textColor: "{colors.light-text-2}", typography: "{typography.label}", rounded: "{rounded.control}", padding: "9px 15px"}
  input-automation: {backgroundColor: "{colors.light-surface}", textColor: "{colors.light-text}", rounded: "{rounded.control}", padding: "10px 12px"}
  chip: {backgroundColor: "{colors.light-surface}", textColor: "{colors.light-text-2}", rounded: "{rounded.small}", padding: "6px 12px"}
  card-mobile: {backgroundColor: "{colors.light-surface}", textColor: "{colors.light-text}", rounded: "{rounded.card-mobile}", padding: "18px"}
  navigation-active: {backgroundColor: "{colors.light-surface-2}", textColor: "{colors.light-text}", typography: "{typography.navigation}", rounded: "{rounded.control}", padding: "7px 1px"}
  fab: {backgroundColor: "{colors.brand}", textColor: "{colors.on-accent}", rounded: "{rounded.surface}", size: "56px"}
  status-strip: {backgroundColor: "{colors.light-surface}", textColor: "{colors.light-text}", rounded: "{rounded.status}", padding: "13px 14px"}
---

# Design System: WinKu 小账本

## Overview

WinKu 小账本 applies the existing WinKu F2 identity to the user-supplied ledger. The ledger reference supplies functions and financial layouts; the WinKu theme supplies green accents on neutral light and dark surfaces. This is an Operate-mode record of the implemented system. No creative metaphor was selected in the available contract; none is introduced here.

The Android app embeds a local ledger UI alongside a native notification collector. The visual system favors compact financial information, restrained depth, local system typography, and persistent phone navigation.

**Key Characteristics:**

- Neutral light and dark surfaces with inherited WinKu green.
- Compact ledger cards, aligned amounts, and five phone destinations.
- Local fonts and SVG icons.

## Colors

Frontmatter records effective values after the `winku.css` cascade. Theme roles pair by light/dark prefixes. Component entries show light defaults; runtime components inherit the matching dark roles.

### Primary

- **WinKu green** (`brand`): inherited F2 identity, brand mark, checkbox accent, and entry FAB.
- **Deep action green** (`action-fill`): filled text buttons in both themes, preserving white lettering.
- **Link green** (`light-link` / `dark-link`): links, selected navigation icons, and visible focus outlines.

### Neutral

- **Canvas and soft canvas** (`*-bg`, `*-bg-soft`): page and sidebar foundations.
- **Surface layers** (`*-surface`, `*-surface-2`, `*-surface-3`): cards, controls, and selected tonal states.
- **Main, secondary, and muted text** (`*-text`, `*-text-2`, `*-muted`): reading hierarchy. The final light muted override is recorded instead of its earlier declaration.
- **Fine and strong lines** (`*-line`, `*-line-strong`): card dividers and control boundaries.

Income, expense, and warning roles communicate semantic states. Transfer amounts use secondary text. Keep textual labels alongside state color.

**The Brand Role Rule.** Preserve the inherited brand green; use the deeper action-fill role for white-text filled buttons.

## Typography

Use the local font stack in the frontmatter. No remote font is introduced. Amounts use tabular numerals. Summary amounts use the amount role on phones; the inherited wider layout permits a responsive size up to 31px. Captions and destination labels retain the incumbent compact hierarchy.

Android WebView text zoom follows the system font scale. CSS sizes are baseline values rather than fixed physical text sizes. Allow status descriptions to wrap.

## Layout

The phone has five persistent destinations: 总览, 明细, 自动记账, 报表, 更多. Accounts and budgets are reached from More/settings. At the inherited 680px breakpoint, navigation becomes a fixed bottom bar; the WinKu overrides at 720px adjust mobile density. These are distinct breakpoints in the current implementation. Wider layouts retain a sidebar, with compact-sidebar behavior at 900px and grid changes at 1180px.

Phone content uses the phone-content horizontal inset, with 14px top and 12px bottom content insets. Cards use 18px padding; summary cards use 16px. Four summary values form two columns on phones. Automation has a 900px maximum content width. Setup rows align instructions and actions with a gap; descriptions wrap and the page scrolls. The phone main area reserves 90px for bottom navigation; the FAB sits 94px above the viewport bottom.

The native host applies system-bar, cutout, and keyboard insets to WebView bounds. The established interaction requirement is 48dp hitboxes. Main/small buttons, icon buttons, chips, inbox tabs, switches, and navigation have explicit CSS minimums; this does not establish that every inherited input or inline action meets the requirement. General selects retain an inherited 38px height; automation settings fields explicitly use a 48px minimum. Review actual hitboxes when adding or changing controls.

## Elevation & Depth

Cards and the FAB are flat. Neutral fills and fine borders establish separation. Modals and toast feedback retain the shared shadow: light uses `0 12px 30px rgba(23,23,26,.10)` and dark uses `0 12px 30px rgba(0,0,0,.2)`. Standard controls retain short state transitions; page entrance animation and body transitions are removed. Reduced-motion preference disables animations, transitions, and smooth scrolling.

## Shapes

The shared surface radius is recorded in frontmatter. Inherited phone cards use the separate card-mobile radius; setup panels and the FAB retain the shared surface radius. Controls use modest corners, status strips slightly softer corners, and inbox tabs pill-like corners. One-pixel lines define containers. SVG line icons remain local; category/account placeholders are small outlined rounded squares in the current implementation.

## Components

### Buttons

Primary actions use deep action green with white text; secondary buttons use secondary surfaces; ghost buttons are transparent at rest. Small buttons retain tighter padding but share the minimum target height. Hover raises standard buttons by 1px; active returns them to baseline. Visible keyboard focus uses a 2px link-green outline offset by 3px. Destructive actions use expense-colored text, a tinted background, and a tinted border.

### Chips

Filter chips use a surface fill and fine border. Selected filters use the inherited brand-tinted background and border. Platform controls are labeled checkboxes in rounded surface containers, with a 48px minimum height. Inbox tabs use the third surface layer for selection and retain explicit labels/counts.

### Cards / Containers

Phone cards use the card-mobile shape, surface fill, fine border, and no shadow. Setup panels use the shared surface radius and 16px padding on phones. Rows divide related setup actions with fine rules. Preserve the reference ledger's charts, account summaries, and amount hierarchy.

### Inputs / Fields

General ledger fields use the second surface layer, fine border, control radius, and inherited padding. Automation detail fields use the main surface, stronger border, and a 48px minimum height. Focus adds a brand-tinted border and a subtle 3px focus shadow; keyboard focus adds the common outline. Placeholders use the effective muted role. Native checkboxes use the inherited green accent.

### Navigation

Phone destinations use icon-over-label composition, a 64px minimum row height, and 21px icons. Selection receives a second-surface background, main text, and link-green icon. Keep all five destinations visible. The local entry FAB is 56px on phones; `native.js` suppresses it on automation, reports, and settings.

### Automation status and inbox

The dashboard status strip opens automation. An income/warning-colored dot accompanies explicit status text distinguishing pause, missing permission, an active connection, and waiting for connection. The inbox separates pending items from all notifications. Reason labels, platform, time, and amount supply context; uncertain notifications expose review and ignore actions. Privacy mode conceals amounts and notification content.

## Do's and Don'ts

### Do:

- Do preserve the WinKu F2 brand role and the deeper filled-button role.
- Do use semantic text, income, expense, warning, and surface roles in both themes.
- Do maintain the established 48dp touch-target requirement and verify actual native hitboxes.
- Do check scrolling, keyboard insets, and large Android font settings on native captures.
- Do keep status text explicit about permission, connection, and pause state.

### Don't:

- Don't add the entry FAB to automation, reports, or settings.
- Don't replace the user-supplied ledger structure with an invented visual identity.
- Don't use green alone to communicate service health or transaction direction.
- Don't interpret these tokens or sample captures as full accessibility compliance.

Evidence: `app/src/main/assets/index.html`, `winku.css`, `native.js`, and `MainActivity.java` were inspected. Native captures `delivery/automation-light.png`, `dashboard-dark.png`, and `automation-large-font.png` were visually checked for palette, density, navigation, and scaled-text behavior. `settings-light.png`, `transactions-light.png`, and additional large-font scroll captures are available as delivery evidence. This document records the inspected implementation and contract; it does not certify complete accessibility or device coverage.
