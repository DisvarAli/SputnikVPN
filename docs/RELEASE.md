# GitHub release — Sputnik VPN 2.1.0

## Checklist

1. Confirm `versionCode` / `versionName` = 21 / `2.1.0`.
2. Update `CHANGELOG.md`, `README*.md`, `assets/CHANGELOG.md`.
3. Build APK: `./gradlew assembleStableDebug`
4. Optionally delete other GitHub releases/tags: `scripts/cleanup-old-github-releases.ps1 -KeepTag v2.1.0`
5. Publish tag `v2.1.0` and upload APK.

```bash
git tag -a v2.1.0 -m "Sputnik VPN 2.1.0"
git push origin v2.1.0
```
