---
name: android-detekt-ktlint-standards
description: Enforces static code analysis, formatting rules, and code complexity limits with Detekt and Ktlint in CI.
version: 1.0.0
author: airmouse-skills
tags: [android, lint, quality]
related_skills: [ci-github-actions-android, android-build-logic-convention-plugins]
applies_to: android
---

# Code Quality Enforcement with Detekt and Ktlint

## When to use
- Enforcing uniform Kotlin formatting across all developers and automated agents.
- Catching complex code smells (Wildcard imports, LongParameterList, MagicNumber).
- Running pre-commit hooks and CI gates before merging PRs.

## Prerequisites
- detekt.yml configuration file in config/detekt/
- io.gitlab.arturbosch.detekt Gradle plugin
- Git pre-commit hook script

## Step-by-step instructions
1. **Generate Detekt Baseline**
   Run ./gradlew detektGenerateConfig to generate the baseline configuration file.
2. **Configure AirMouse Rulesets**
   Enable Ktlint formatting ruleset and configure cyclomatic complexity and method length limits.
3. **Hook into CI Workflow**
   Add ./gradlew detekt to GitHub Actions workflow as a mandatory pull-request gate.

## Code examples

### Implementation Example (`config/detekt/detekt.yml`)
```yaml
# File: config/detekt/detekt.yml
formatting:
  active: true
  NoWildcardImports:
    active: true
  FinalNewline:
    active: true
  TrailingCommaOnCallSite:
    active: true

complexity:
  LongMethod:
    threshold: 60
  ComplexCondition:
    threshold: 4
  TooManyFunctions:
    threshold: 15

style:
  MagicNumber:
    active: true
    ignoreNumbers: ['-1', '0', '1', '2']
```

## Verification
Execute the following verification gate to ensure compliance:
```bash
./gradlew detekt
```
Expected output:
```text
BUILD SUCCESSFUL - 0 code smell violations reported
```

## Common pitfalls
- **Anti-pattern / Wrong:**
  Allowing wildcard imports (import foo.bar.*) in production codebase.
  **Recommended / Correct:**
  Explicitly list all imports to prevent symbol collisions and namespace ambiguity.
- **Anti-pattern / Wrong:**
  Suppressing Detekt warnings with @Suppress without explaining the rationale.
  **Recommended / Correct:**
  Refactor the complex logic or provide a clear comment justifying the suppression.
- **Anti-pattern / Wrong:**
  Running Detekt without formatting rules enabled, allowing messy spacing to pass CI.
  **Recommended / Correct:**
  Enable the formatting ruleset to enforce Ktlint-compatible layout automatically.

## Cross-references
- Related skills: `ci-github-actions-android`, `android-build-logic-convention-plugins`
- Protocol V2 specification: `docs/PROTOCOL_V2.md#1-wire-envelope`
- Master audit verification ID: `[DEP-02](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)`
