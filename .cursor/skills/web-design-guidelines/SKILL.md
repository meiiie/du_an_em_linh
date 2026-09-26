---
name: web-design-guidelines
description: Review UI code for Web Interface Guidelines compliance. Use when asked to "review my UI", "check accessibility", "audit design", "review UX", or "check my site against best practices".
metadata:
  author: vercel
  version: "1.0.0"
---

# Web Interface Guidelines

Review files for compliance with Web Interface Guidelines.

## How It Works

1. Read the local copy of the rules in [command.md](command.md) (same content as the upstream Vercel guidelines).
2. Read the specified files (or every file under `apps/web/app` and `apps/web/components` if none specified).
3. Check against all rules in those guidelines.
4. Output findings in the terse `file:line` format.

## Guidelines Source

Upstream (fetch only when refreshing the local copy):

```
https://raw.githubusercontent.com/vercel-labs/web-interface-guidelines/main/command.md
```

This repo keeps a snapshot at `.cursor/skills/web-design-guidelines/command.md` so reviews work offline.

## Product notes

- UI language is Vietnamese. Prefer sentence case over Chicago Title Case.
- Keep existing `data-testid` values when restyling.
- Skip link target is `#noi-dung`.
- `Link` for navigation, `button` for actions.
