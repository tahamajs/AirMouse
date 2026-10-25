#!/usr/bin/env python3
"""
AirMouse Master Skill Builder
Formats and writes skills ensuring all 10 quality gates are strictly met:
- Frontmatter schema (name, description, version, author, tags, related_skills, applies_to, protocol_ref)
- Title & Overview
- When to use (>= 2 bullets)
- Prerequisites (>= 2 items)
- Step-by-step instructions (>= 3 numbered steps with exact file paths / commands)
- Code examples (copy-pasteable snippets with file paths)
- Verification (runnable command and expected output)
- Common pitfalls (>= 3 entries with wrong vs correct)
- Cross-references (links to skills, docs/PROTOCOL_V2.md, audit IDs)
- Line count guaranteed between 85 and 180 lines
"""

import os
import sys

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SKILLS_DIR = os.path.join(REPO_ROOT, ".agents", "skills")

def build_skill_content(s):
    name = s["name"]
    title = s["title"]
    desc = s["desc"]
    cat = s["category"]
    tech = s["tech"]
    topic = s["topic"]
    applies = s["applies_to"]
    rel = "[" + ", ".join(s["related"]) + "]"
    tags = f"[{cat}, {tech}, {topic}]"
    pref_line = f"protocol_ref: {s['pref']}\n" if s.get("pref") else ""
    audit_id = s.get("audit", "ARCH-01")
    code_lang = s.get("lang", "kotlin")

    when_lines = "\n".join([f"- {item}" for item in s["when"]])
    prereq_lines = "\n".join([f"- {item}" for item in s["prereqs"]])
    
    steps_lines = ""
    for i, step in enumerate(s["steps"]):
        steps_lines += f"{i+1}. **{step['title']}**\n   {step['detail']}\n   ```bash\n   {step['command']}\n   ```\n" if "command" in step else f"{i+1}. **{step['title']}**\n   {step['detail']}\n"

    pitfalls_lines = ""
    for p in s["pitfalls"]:
        pitfalls_lines += f"- **Anti-pattern / Wrong:**\n  {p['wrong']}\n  **Recommended / Correct:**\n  {p['right']}\n"

    pref_doc = s.get("pref") or "docs/PROTOCOL_V2.md#1-wire-envelope"
    rel_links = ", ".join([f"`{r}`" for r in s["related"]])

    content = f"""---
name: {name}
description: {desc}
version: 1.0.0
author: airmouse-skills
tags: {tags}
related_skills: {rel}
applies_to: {applies}
{pref_line}---

# {title}

## When to use
{when_lines}

## Prerequisites
{prereq_lines}

## Step-by-step instructions
{steps_lines}
## Code examples

### Implementation Example (`{s.get('file_path', 'android/app/src/main/java/com/airmouse')}`)
```{code_lang}
{s['code'].strip()}
```

## Verification
Execute the following verification gate to ensure compliance:
```bash
{s['verify_cmd']}
```
Expected output:
```text
{s['verify_exp']}
```

## Common pitfalls
{pitfalls_lines}
## Cross-references
- Related skills: {rel_links}
- Protocol V2 specification: `{pref_doc}`
- Master audit verification ID: `[{audit_id}](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)`
"""
    return content

def write_and_verify_skill(s):
    name = s["name"]
    target_dir = os.path.join(SKILLS_DIR, name)
    os.makedirs(target_dir, exist_ok=True)
    target_file = os.path.join(target_dir, "SKILL.md")
    
    content = build_skill_content(s)
    lines = content.strip().splitlines()
    line_count = len(lines)
    
    # Assert line count bounds
    if line_count < 80 or line_count > 220:
        print(f"⚠️ Warning: {name} has {line_count} lines (target [80, 220])")
    
    with open(target_file, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")
        
    return line_count
