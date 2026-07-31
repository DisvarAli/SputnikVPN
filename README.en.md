<p align="center">
  <img src="docs/assets/sputnik-mark.svg" alt="Sputnik VPN" width="96">
</p>

<h1 align="center">Sputnik VPN</h1>

<p align="center">
  <b>Open-source Android VPN client for Russia</b><br/>
  Kotlin · Jetpack Compose · Xray
</p>

<p align="center">
  <a href="https://github.com/DisvarAli/SputnikVPN/releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/DisvarAli/SputnikVPN?style=flat-square&color=0d7377&labelColor=0b2a2e"></a>
  <a href="https://github.com/DisvarAli/SputnikVPN/stargazers"><img alt="Stars" src="https://img.shields.io/github/stars/DisvarAli/SputnikVPN?style=flat-square&color=4DD0E1&labelColor=0b2a2e"></a>
  <img alt="Downloads" src="https://img.shields.io/github/downloads/DisvarAli/SputnikVPN/total?style=flat-square&color=2DB87A&labelColor=0b2a2e">
  <img alt="License" src="https://img.shields.io/badge/license-MIT-0d7377?style=flat-square&labelColor=0b2a2e">
  <img alt="Version" src="https://img.shields.io/badge/2.1.0-4DD0E1?style=flat-square&labelColor=0b2a2e">
</p>

<p align="center">
  <a href="README.md">Home</a> ·
  <a href="README.ru.md">Русский</a> ·
  <a href="README.en.md"><b>English</b></a>
</p>

---

Full feature tables, mirrors, protocols, and **live repo stats** (stars, forks, downloads, size, languages) are in the main [README.md](README.md).

<p align="center">
  <a href="https://github.com/DisvarAli/SputnikVPN/releases/latest"><b>⬇ Download 2.1.0</b></a>
</p>

## Highlights

- Config picker + config file import  
- 8-step mirror cascade when GitHub is blocked  
- VLESS / VMess / Trojan / Shadowsocks / Hysteria2  
- Fast TCP checks + optional Xray real ping  

> Not commercial. Not on Google Play. Unrelated to [@igareck](https://github.com/igareck/vpn-configs-for-russia).

## Build

```bash
git clone https://github.com/DisvarAli/SputnikVPN.git
cd SputnikVPN
./scripts/download-libs.sh
./gradlew assembleStableDebug
```

## License

[MIT](LICENSE)
