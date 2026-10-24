#!/usr/bin/env bash
# scripts/install_hooks.sh
# Installs and verifies Git hooks for the AirMouse repository.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOOKS_DIR="$ROOT_DIR/.git/hooks"

echo "=============================================="
echo "Installing AirMouse Git Hooks"
echo "=============================================="

if [ ! -d "$ROOT_DIR/.git" ]; then
    echo "❌ Error: .git directory not found at $ROOT_DIR. Is this a Git repository?"
    exit 1
fi

mkdir -p "$HOOKS_DIR"

PRE_COMMIT_HOOK="$HOOKS_DIR/pre-commit"

cat << 'EOF' > "$PRE_COMMIT_HOOK"
#!/usr/bin/env bash
# .git/hooks/pre-commit
# AirMouse Pre-Commit Hook: Rejects any staged file exceeding 5MB (5,242,880 bytes).
# Protects the repository against accidental binary and dataset bloat.

set -euo pipefail

MAX_BYTES=5242880 # 5MB limit
MAX_MB=5

# Empty tree hash for new repositories or initial commit
EMPTY_TREE="4b825dc642cb6eb9a060e54bf8d69288fbee4904"
if git rev-parse --verify HEAD >/dev/null 2>&1; then
    AGAINST="HEAD"
else
    AGAINST="$EMPTY_TREE"
fi

echo "🔍 Running AirMouse pre-commit file size checks (Limit: ${MAX_MB}MB)..."

BLOCKED_FILES=()

# Check staged files against the limit using git diff-index
# Output format: :<old-mode> <new-mode> <old-sha> <new-sha> <status>\t<path>
while IFS=$'\t' read -r meta file; do
    [ -z "$meta" ] && continue
    status=$(echo "$meta" | awk '{print $5}')
    blob=$(echo "$meta" | awk '{print $4}')

    # Ignore deleted files
    if [ "$status" = "D" ]; then
        continue
    fi

    size=0
    # If the blob hash is valid and not all zeros, query git cat-file
    if [ -n "$blob" ] && [ "$blob" != "0000000000000000000000000000000000000000" ]; then
        size=$(git cat-file -s "$blob" 2>/dev/null || echo 0)
    fi

    # Fallback to local file size if cat-file didn't give size
    if [ "$size" -eq 0 ] && [ -f "$file" ]; then
        size=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null || wc -c < "$file" 2>/dev/null || echo 0)
    fi

    if [ "$size" -gt "$MAX_BYTES" ]; then
        size_mb=$(awk "BEGIN {printf \"%.2f\", $size / 1048576}")
        echo "❌ [REJECTED] Staged file '$file' is ${size_mb}MB (Exceeds ${MAX_MB}MB threshold)"
        BLOCKED_FILES+=("$file")
    fi
done < <(git diff-index --cached "$AGAINST")

if [ ${#BLOCKED_FILES[@]} -ne 0 ]; then
    echo ""
    echo "🚨 COMMIT BLOCKED: One or more staged files exceed the maximum allowed size of ${MAX_MB}MB."
    echo "To avoid permanent Git repository bloat, large binaries, datasets, or build artifacts"
    echo "must not be committed to Git history."
    echo ""
    echo "Recommended actions:"
    echo "  1. Unstage the large file(s):"
    for f in "${BLOCKED_FILES[@]}"; do
        echo "     git rm --cached \"$f\""
    done
    echo "  2. Add the file/pattern to .gitignore"
    echo "  3. Use external release storage, Git LFS, or S3/R2 for large assets."
    exit 1
fi

echo "✅ Pre-commit hook passed: No files exceed ${MAX_MB}MB."
exit 0
EOF

chmod +x "$PRE_COMMIT_HOOK"

echo "✅ Pre-commit hook installed to: $PRE_COMMIT_HOOK"
echo "All commits are now guarded against adding files > 5MB."
