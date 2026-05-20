# SeekerClaw Skill Protocol

This document defines the YAML schema, protocol rules, and selection mechanism for SeekerClaw skills.

## YAML Frontmatter Schema

Each skill MUST begin with a YAML frontmatter block delimited by `---`.

### Required Fields

- `name` (String): Unique identifier for the skill (e.g., `web-search-pro`).
- `description` (String): A clear, concise description of what the skill does. The AI uses this to decide when to trigger the skill.
- `version` (String): Semantic version of the skill (e.g., `1.0.0`).

### Optional Fields

- `category` (String): Grouping category for the UI (default: `"General"`).
- `enabled` (Boolean): Whether the skill is active and available to the AI (default: `true`).
- `emoji` (String): A single emoji representing the skill.
- `image` (String): URL to a skill logo image.
- `triggers` (List<String>): List of keyword triggers (legacy support).
- `requires`: Structured requirements for the skill.
    - `bins` (List<String>): List of required shell binaries.
    - `env` (List<String>): List of required environment variables.
    - `config` (List<String>): List of required internal config keys.

### Example

```yaml
---
name: web-search-pro
description: "Advanced multi-source research and synthesis using web_search and web_fetch."
version: "1.0.0"
emoji: "🔍"
category: "Research"
enabled: true
requires:
  env:
    - BRAVE_API_KEY
---
```

## Selection Mechanism

SeekerClaw uses a semantic selection mechanism to choose the most relevant skill for a user's request.

1.  **Loading**: Only skills where `enabled: true` and all `requires` are met are loaded into the AI's context.
2.  **Available Skills Block**: Loaded skills are presented to the AI in the `<available_skills>` block within the system prompt.
3.  **AI Decision**: The AI scans the descriptions of all available skills.
    - If exactly one skill clearly applies, it uses `skill_read` to load it.
    - If multiple skills apply, it chooses the most specific one.
    - If none apply, it responds normally.

## Development Rules

- **Backward Compatibility**: Skills without `category` or `enabled` fields default to `"General"` and `true` respectively.
- **Atomic Updates**: When updating a skill's state (e.g., enabling/disabling via UI), the `SKILL.md` file is updated atomically.
- **Self-Awareness**: Always update the skill's description if its capabilities change, as this directly affects selection accuracy.
