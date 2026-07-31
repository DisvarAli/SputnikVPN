# Developer guide — Sputnik VPN 2.1.0

## Requirements

- JDK 17+
- Android SDK 36
- `libv2ray.aar` via `scripts/download-libs.ps1` or `.sh`

## Build

```powershell
.\scripts\download-libs.ps1
.\gradlew assembleStableDebug    # com.my.vpn — release APK for GitHub
.\gradlew assembleDevDebug       # com.my.vpn.dev — side-by-side testing
```

Emulator x86_64: `.\gradlew assembleStableDebug -PdevAbi=x86_64`

## Tests

```powershell
.\gradlew testStableDebugUnitTest
.\gradlew connectedStableDebugAndroidTest   # device/emulator
```

## Project conventions

- VPN logic in `vpn/`, not in Composables
- Settings in `AppSettingsStorage` (DataStore `app_settings`)
- VPN option holders are simplified; core starts with a neutral bypass profile.
- Version constant: `AppConstants.APP_VERSION_NAME` + `build.gradle.kts`

## Adding strings

1. Add to `res/values/strings.xml`
2. Mirror in `res/values-en/strings.xml`
3. Use `stringResource(R.string.*)` in UI (no hardcoded user-visible Russian in Composables)

## Language change

User picks language in **Appearance** → `AppSettingsStorage.setAppLanguage` → activity `recreate()` via `MainViewModel.requestActivityRecreate`.

## Release

See [RELEASE.md](RELEASE.md).

## Do not commit

- `app/libs/*.aar`
- `local.properties`, keystores, secrets
