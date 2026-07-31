<p align="center">
  <img src="docs/assets/sputnik-banner.svg" alt="Sputnik VPN" width="100%">
</p>

<p align="center">
  <a href="https://github.com/DisvarAli/SputnikVPN/releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/DisvarAli/SputnikVPN?style=for-the-badge&color=0d7377&labelColor=0b2a2e"></a>
  <a href="https://github.com/DisvarAli/SputnikVPN/stargazers"><img alt="Stars" src="https://img.shields.io/github/stars/DisvarAli/SputnikVPN?style=for-the-badge&color=4DD0E1&labelColor=0b2a2e"></a>
  <a href="https://github.com/DisvarAli/SputnikVPN/network/members"><img alt="Forks" src="https://img.shields.io/github/forks/DisvarAli/SputnikVPN?style=for-the-badge&color=76BEBD&labelColor=0b2a2e"></a>
  <a href="https://github.com/DisvarAli/SputnikVPN/watchers"><img alt="Watchers" src="https://img.shields.io/github/watchers/DisvarAli/SputnikVPN?style=for-the-badge&color=2EE6D6&labelColor=0b2a2e"></a>
</p>

<p align="center">
  <img alt="Downloads" src="https://img.shields.io/github/downloads/DisvarAli/SputnikVPN/total?style=for-the-badge&color=2DB87A&labelColor=0b2a2e&label=downloads">
  <img alt="Issues" src="https://img.shields.io/github/issues/DisvarAli/SputnikVPN?style=for-the-badge&color=E85D5D&labelColor=0b2a2e">
  <img alt="License" src="https://img.shields.io/badge/license-MIT-0d7377?style=for-the-badge&labelColor=0b2a2e">
  <img alt="Last commit" src="https://img.shields.io/github/last-commit/DisvarAli/SputnikVPN?style=for-the-badge&color=8BA09A&labelColor=0b2a2e">
</p>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B%20→%2016-76BEBD?style=flat-square&labelColor=0b2a2e&logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.x-7F52FF?style=flat-square&labelColor=0b2a2e&logo=kotlin&logoColor=white">
  <img alt="Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?style=flat-square&labelColor=0b2a2e&logo=jetpackcompose&logoColor=white">
  <img alt="Xray" src="https://img.shields.io/badge/Core-Xray%20%2F%20libv2ray-2EE6D6?style=flat-square&labelColor=0b2a2e">
  <img alt="Version" src="https://img.shields.io/badge/app-2.1.0%20(21)-4DD0E1?style=flat-square&labelColor=0b2a2e">
</p>

<p align="center">
  <a href="README.ru.md"><b>Русский</b></a>
  ·
  <a href="README.en.md"><b>English</b></a>
  ·
  <a href="https://github.com/DisvarAli/SputnikVPN/releases/latest"><b>⬇ APK</b></a>
  ·
  <a href="CHANGELOG.md"><b>Changelog</b></a>
</p>

---

<p align="center">
  <b>Sputnik VPN</b> — открытый Android-клиент для России.<br/>
  Подписки через зеркала · быстрая проверка серверов · Xray через системный VPN.
</p>

> Не коммерческий сервис. Не в Google Play. Публичные узлы не контролируются автором — используйте на свой риск.<br/>
> Не связан с [@igareck](https://github.com/igareck/vpn-configs-for-russia): списки — внешний источник данных.

---

## Статистика репозитория

| Метрика | Значение |
|:--------|:---------|
| **Версия приложения** | **2.1.0** (`versionCode` **21**) |
| **Платформа** | Android **8.0+** (API 26) → Android **16** (API 36) |
| **Язык / UI** | Kotlin · Jetpack Compose · Material 3 |
| **VPN-ядро** | Xray (`libv2ray.aar`) |
| **Лицензия** | [MIT](LICENSE) |
| **Релизы** | [GitHub Releases](https://github.com/DisvarAli/SputnikVPN/releases) |
| **Issues / Stars / Forks** | см. бейджи выше (обновляются автоматически) |
| **Скачивания APK** | суммарно по всем релизам (бейдж Downloads) |

<p align="center">
  <img alt="Repo size" src="https://img.shields.io/github/repo-size/DisvarAli/SputnikVPN?style=flat-square&color=0d7377&labelColor=0b2a2e">
  <img alt="Code size" src="https://img.shields.io/github/languages/code-size/DisvarAli/SputnikVPN?style=flat-square&color=76BEBD&labelColor=0b2a2e">
  <img alt="Top language" src="https://img.shields.io/github/languages/top/DisvarAli/SputnikVPN?style=flat-square&labelColor=0b2a2e">
  <img alt="Contributors" src="https://img.shields.io/github/contributors/DisvarAli/SputnikVPN?style=flat-square&color=2EE6D6&labelColor=0b2a2e">
</p>

---

## Возможности 2.1

| | Подробности |
|:--|:--|
| **Выбор сервера** | Отдельный экран: поиск по стране / имени / IP, фильтр протокола |
| **Файл конфигов** | Импорт `.txt` / подписки с устройства прямо из выбора сервера |
| **Зеркала** | 8 ступеней, если GitHub raw недоступен |
| **Протоколы** | VLESS (+ Reality), VMess, Trojan, Shadowsocks, Hysteria2 |
| **Проверка** | Быстрый TCP; полный режим — VPN-тест через Xray у лучших серверов |
| **Подписки** | 8 встроенных списков + свои URL / файл / QR |
| **Сборки** | `stable` (`com.my.vpn`) и `dev` (`com.my.vpn.dev`, суффикс `-dev`) |

---

## Зеркала подписок

Порядок перебора при загрузке:

| # | Источник | Роль |
|:-:|:---------|:-----|
| 1 | **GitLab** | Основное git-зеркало |
| 2 | **GitHack** | RAW-прокси |
| 3 | **Codeberg** | FOSS-зеркало |
| 4 | **Gitea** | FOSS-зеркало |
| 5 | **SourceHut** | FOSS-зеркало |
| 6 | **Bitbucket** | Git-зеркало |
| 7 | **Яндекс + Bitbucket** | Путь через белые списки |
| 8 | **GitHub** | Оригинал (в конце цепочки) |

> jsDelivr не используется (устаревший CDN-кэш).

Источник списков: [igareck/vpn-configs-for-russia](https://github.com/igareck/vpn-configs-for-russia).

---

## Протоколы

| Протокол | Подключение | Примечание |
|:---------|:-----------:|:-----------|
| **VLESS** | ✅ | в т.ч. Reality |
| **VMess** | ✅ | |
| **Trojan** | ✅ | |
| **Shadowsocks** | ✅ | |
| **Hysteria2** | ✅ | UDP/QUIC — без TCP-скрининга |

---

## Скачать

| | |
|:--|:--|
| **APK** | [Последний релиз](https://github.com/DisvarAli/SputnikVPN/releases/latest) |
| **Имя сборки** | `app-stable-debug.apk` → `SputnikVPN-2.1.0.apk` |
| **Установка** | Разрешите установку из неизвестных источников |

### Как пользоваться

1. Установите APK  
2. На главном экране **↻** — подтянуть списки через зеркала  
3. **Выбрать конфиг** — сервер из списка или **файл с конфигами**  
4. **Подключиться**  

---

## Сборка из исходников

```bash
git clone https://github.com/DisvarAli/SputnikVPN.git
cd SputnikVPN

# ядро libv2ray (не в git)
powershell -File scripts/download-libs.ps1   # Windows
./scripts/download-libs.sh                   # Linux / macOS

./gradlew assembleStableDebug
```

| Flavor | Package | Версия | Команда |
|:-------|:--------|:-------|:--------|
| `stable` | `com.my.vpn` | `2.1.0` | `assembleStableDebug` |
| `dev` | `com.my.vpn.dev` | `2.1.0-dev` | `assembleDevDebug` |

APK: `app/build/outputs/apk/stable/debug/app-stable-debug.apk`

---

## Структура проекта

| Путь | Назначение |
|:-----|:-----------|
| `app/src/main/java/com/my/vpn/` | Клиент (UI, VPN, подписки) |
| `app/src/main/assets/` | Help, changelog |
| `docs/` | Архитектура, релиз, баннеры |
| `scripts/` | libs, publish, cleanup |

---

## Документация

| Документ | |
|:---------|:--|
| [Архитектура](docs/ARCHITECTURE.md) | Слои и потоки данных |
| [Разработка](docs/DEVELOPMENT.md) | Локальная сборка |
| [Релиз](docs/RELEASE.md) | Публикация |
| [Заметки 2.1.0](docs/release-notes-v2.1.0.md) | Что вошло в релиз |
| [CHANGELOG](CHANGELOG.md) | История |
| [CONTRIBUTING](CONTRIBUTING.md) | Как участвовать |

---

## Лицензия и авторы

Код — **[MIT](LICENSE)**. Автор репозитория: [@DisvarAli](https://github.com/DisvarAli).

Конфиги для РФ публикует сообщество [igareck/vpn-configs-for-russia](https://github.com/igareck/vpn-configs-for-russia) — Sputnik VPN лишь клиент.
