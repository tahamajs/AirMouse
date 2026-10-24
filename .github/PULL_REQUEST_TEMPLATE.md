## Summary of Changes
<!-- A clear and concise description of what this PR does. -->

## Type of Change
- [ ] 🐛 Bug fix (non-breaking change fixing an issue)
- [ ] ✨ New feature (non-breaking change adding functionality)
- [ ] 💥 Breaking change (fix or feature causing existing behavior to change)
- [ ] 📝 Documentation update
- [ ] ⚡ Performance optimization / Signal smoothing
- [ ] 🔒 Security hardening

## Verification Checklist
- [ ] Conforms to **Protocol V2** wire specification (`v: 2`, `seq`, `ts`, `id`, `payload`)
- [ ] Run `./scripts/check_docs.sh` (100% protocol parity verified)
- [ ] Unit & Integration tests passing:
  - [ ] Python: `pytest pc/airmouse_py`
  - [ ] Go: `go test ./...` in `pc/airmouse_go_new`
  - [ ] Android: `./gradlew testDebugUnitTest` in `android/`
- [ ] No large binary files added (pre-commit hook verified `< 5MB`)
- [ ] Zero placeholder policy observed (`no TODOs or stub methods`)
- [ ] Tested on target hardware (Desktop / Android / iOS)

## Related Issues
<!-- Closes #123, Fixes #456 -->
