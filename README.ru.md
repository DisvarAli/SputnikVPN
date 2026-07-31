<p align="center">
  <img src="docs/assets/sputnik-mark.svg" alt="Sputnik VPN" width="96">
</p>

<h1 align="center">Sputnik VPN</h1>

<p align="center">
  <b>Открытый Android VPN-клиент для России</b><br/>
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
  <a href="README.ru.md"><b>Русский</b></a> ·
  <a href="README.en.md">English</a>
</p>

---

Полное описание, таблицы зеркал, протоколов и **статистика репозитория** (stars, forks, downloads, размер, языки) — в главном [README.md](README.md).

<p align="center">
  <a href="https://github.com/DisvarAli/SputnikVPN/releases/latest"><b>⬇ Скачать 2.1.0</b></a>
</p>

## Кратко

- Выбор сервера + импорт файла конфигов  
- 8 зеркал при блокировке GitHub  
- VLESS / VMess / Trojan / Shadowsocks / Hysteria2  
- Быстрый TCP и опциональный VPN-тест  

> Не коммерческий продукт. Не в Google Play. Не связан с [@igareck](https://github.com/igareck/vpn-configs-for-russia).

## Сборка

```bash
git clone https://github.com/DisvarAli/SputnikVPN.git
cd SputnikVPN
powershell -File scripts/download-libs.ps1
./gradlew assembleStableDebug
```

## Лицензия

[MIT](LICENSE)
