# Участие в проекте

Спасибо за интерес к проекту!

## Перед началом

1. Прочитайте [README.md](README.md)
2. Скачайте ядро: `.\scripts\download-libs.ps1`
3. Соберите: `.\gradlew assembleStableDebug`

## Pull Request

1. Fork → ветка `feature/…`
2. Один логический набор изменений на PR
3. Новые строки UI — в `values/strings.xml` и при необходимости в `values-en/`
4. В описании PR укажите, что изменилось и как проверить

## Не коммитить

- `libv2ray.aar`, keystore, `local.properties`, секреты

## Сборки

| Flavor | Команда |
|--------|---------|
| stable | `assembleStableDebug` |
| dev | `assembleDevDebug` |

Подробнее: [docs/RELEASE.md](docs/RELEASE.md).

## Вопросы

GitHub Issues.
