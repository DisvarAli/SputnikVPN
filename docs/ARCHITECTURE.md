# Architecture — Sputnik VPN 2.1.0

## Layers

| Layer | Package | Role |
|-------|---------|------|
| UI | `ui/` | Compose screens, `MainViewModel`, `MainUiState` |
| Domain | `domain/` | `ConnectVpnUseCase` |
| Data | `data/` | Parsers, `ConfigRepository`, DataStore, file cache |
| VPN | `vpn/` | `MyVpnService`, Xray, core holders |
| Util | `util/` | Mirrors, filters, locale, audit |
| Worker | `worker/` | WorkManager |
| Widget | `widget/` | Home screen toggle |

## Connection flow

1. `MainViewModel` → `ConnectVpnUseCase` / `VpnController.connect`
2. `MyVpnService` establishes TUN
3. `VpnCoreManager.startLoop` with JSON from `V2RayConfigBuilder`
4. `TunnelConnectivityChecker` verifies SOCKS/HTTP

## Config storage

- Full list: `files/config_cache/configs.json`
- Metadata: DataStore (`vpn_configs`)
- Per-server notes storage (legacy): `PerServerBypassStorage`

## Limits

- Ping sample: up to hundreds of configs; proxy test after TCP on top survivors
- Post-test filter: hide high ping / N/A when strict list is on

## Localization

- `values/` / `values-en/` string resources
- Help: `assets/help_ru.json`, `assets/help_en.json`

## Build

- **stable** / **dev** product flavors
- Native core: `app/libs/libv2ray.aar` (download script, not in Git)
