
---

## `docs/TROUBLESHOOTING.md`

```markdown
# Air Mouse – Troubleshooting Guide

This is the **ultimate list of issues** you may encounter, with exact fixes.

## 1. PC Server Issues

### 1.1 `pip install pyautogui` fails with SOCKS error

**Symptoms:** `ERROR: Missing dependencies for SOCKS support`

**Fix:** Unset proxy variables:
```bash
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY all_proxy ALL_PROXY