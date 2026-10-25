#!/usr/bin/env python3
"""
AirMouse Skill Validator
Validates that every SKILL.md in .agents/skills/ satisfies all quality gates.
"""

import os
import sys
import re

REQUIRED_FIELDS = ["name", "description", "version", "author", "tags", "related_skills", "applies_to"]
REQUIRED_SECTIONS = [
    "## When to use",
    "## Prerequisites",
    "## Step-by-step instructions",
    "## Code examples",
    "## Verification",
    "## Common pitfalls",
    "## Cross-references"
]

def parse_frontmatter(content):
    if not content.startswith("---"):
        return None, "File does not start with YAML frontmatter delimiter (---)"
    parts = content.split("---", 2)
    if len(parts) < 3:
        return None, "Malformed YAML frontmatter (missing closing ---)"
    fm_text = parts[1]
    body = parts[2]
    
    # Simple YAML key-value parser for known fields
    fm = {}
    current_key = None
    for line in fm_text.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if ":" in line:
            k, v = line.split(":", 1)
            k = k.strip()
            v = v.strip()
            if v.startswith("[") and v.endswith("]"):
                items = [x.strip().strip("'\"") for x in v[1:-1].split(",") if x.strip()]
                fm[k] = items
            else:
                fm[k] = v.strip("'\"")
            current_key = k
    return fm, body

def validate_skill(skill_dir, skill_path, all_skill_names, protocol_sections):
    errors = []
    with open(skill_path, "r", encoding="utf-8") as f:
        lines = f.readlines()
    
    total_lines = len(lines)
    if total_lines < 80 or total_lines > 220:
        errors.append(f"Line count ({total_lines}) out of bounds [80, 220]")
    
    content = "".join(lines)
    fm, err_or_body = parse_frontmatter(content)
    if not fm:
        errors.append(f"Frontmatter error: {err_or_body}")
        return errors
    
    body = err_or_body
    
    # Check required frontmatter fields
    for field in REQUIRED_FIELDS:
        if field not in fm:
            errors.append(f"Missing required frontmatter field: {field}")
            
    # Check skill name matches directory
    dir_name = os.path.basename(skill_dir)
    if fm.get("name") != dir_name:
        errors.append(f"Frontmatter name '{fm.get('name')}' does not match directory name '{dir_name}'")
        
    # Check related_skills exist
    rel_skills = fm.get("related_skills", [])
    if isinstance(rel_skills, list):
        for rs in rel_skills:
            if rs not in all_skill_names:
                errors.append(f"Referenced related_skill '{rs}' does not exist")
    else:
        errors.append("related_skills must be a list")
        
    # Check protocol_ref if present
    pref = fm.get("protocol_ref")
    if pref:
        # e.g. docs/PROTOCOL_V2.md#section-name
        if "#" in pref:
            section = pref.split("#", 1)[1].lower().replace("-", " ").replace(".", "")
            # check if any section matches
            matched = any(section in ps.lower().replace("-", " ").replace(".", "") for ps in protocol_sections)
            if not matched:
                errors.append(f"protocol_ref '{pref}' anchor '{section}' not found in docs/PROTOCOL_V2.md")
                
    # Check required sections
    for sec in REQUIRED_SECTIONS:
        if sec not in body:
            errors.append(f"Missing required section heading: '{sec}'")
            
    # Check 'When to use' has at least 2 bullet points
    if "## When to use" in body:
        wtu_part = body.split("## When to use")[1].split("## Prerequisites")[0]
        bullets = [l for l in wtu_part.splitlines() if l.strip().startswith("-") or l.strip().startswith("*")]
        if len(bullets) < 2:
            errors.append("## When to use section must contain at least 2 bullet points")
            
    return errors

# Baseline 10 skills in .agents/skills/ that belong to the initial 43 skills and must NOT be modified
EXISTING_BASELINE_SKILLS = {
    "airmouse-protocol-v2",
    "sensor-fusion-dsp",
    "go-desktop-daemon",
    "python-airmouse-core",
    "mobile-web-client",
    "cross-platform-release-builder",
    "ui-screen-navigation-orchestration",
    "compose-ui-polish-craftsmanship",
    "design-system-standardization",
    "multiplatform-ui-state-sync"
}

def main():
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))
    skills_root = os.path.join(repo_root, ".agents", "skills")
    protocol_path = os.path.join(repo_root, "docs", "PROTOCOL_V2.md")
    
    protocol_sections = []
    if os.path.exists(protocol_path):
        with open(protocol_path, "r", encoding="utf-8") as f:
            for line in f:
                if line.startswith("#"):
                    clean = line.lstrip("#").strip().strip("`")
                    protocol_sections.append(clean)
                    
    skill_dirs = [
        d for d in os.listdir(skills_root)
        if os.path.isdir(os.path.join(skills_root, d)) and os.path.exists(os.path.join(skills_root, d, "SKILL.md"))
    ]
    all_skill_names = set(skill_dirs)
    
    new_skill_dirs = [d for d in skill_dirs if d not in EXISTING_BASELINE_SKILLS]
    print(f"Total skills in workspace: {len(skill_dirs)} (Baseline: {len(EXISTING_BASELINE_SKILLS)}, New to validate: {len(new_skill_dirs)})")
    total_errors = 0
    
    for d in sorted(new_skill_dirs):
        s_dir = os.path.join(skills_root, d)
        s_path = os.path.join(s_dir, "SKILL.md")
        errs = validate_skill(s_dir, s_path, all_skill_names, protocol_sections)
        if errs:
            print(f"❌ FAIL: {d}")
            for e in errs:
                print(f"   - {e}")
            total_errors += len(errs)
            
    if total_errors > 0:
        print(f"\nValidation failed with {total_errors} errors across skills.")
        sys.exit(1)
    else:
        print(f"\n✅ All {len(new_skill_dirs)} new skills passed all quality gates!")
        sys.exit(0)

if __name__ == "__main__":
    main()
