# ModSync: выпуск релиза

Текущая версия мода: `1.0.45`

Этот документ описывает, как выпускать релизы ModSync через git-теги и GitHub Releases.

## Что уже настроено

В репозитории есть два workflow:

- `Build`
  Запускает общий smoke helper, который проверяет `test` и `build --warning-mode all`, сохраняет jar и HTML-отчёт тестов как artifacts, отменяет устаревшие ранны на той же ref и имеет timeout
- `Release`
  Запускается по тегу `v*`, использует тот же smoke helper с тегом для проверки версии, `test` и `build --warning-mode all`, рендерит release notes из changelog и затем публикует GitHub Release с jar и `CHANGELOG.md`, с защитой от дублирующихся прогонов и timeout

## Когда использовать

- когда изменения уже проверены и готовы к публикации
- когда нужно получить официальный GitHub Release с прикреплённым jar
- когда нужно зафиксировать конкретную версию для раздачи игрокам и администраторам

## Перед выпуском

Проверь:

- версия в `gradle.properties` совпадает с тем, что ты собираешься публиковать
- в `CHANGELOG.md` есть запись для этой версии
- тег `vX.Y.Z` будет совпадать с `mod_version` и верхней записью в changelog
- документация обновлена, если поведение мода менялось
- `./gradlew test`
- `./gradlew build --warning-mode all`
- `./scripts/run-release-smoke.sh vX.Y.Z` как быстрый объединённый прогон перед тегом
- проверка, что собрался ожидаемый `build/libs/modsync-X.Y.Z.jar`
- проверка, что версия внутри `META-INF/mods.toml` и `META-INF/MANIFEST.MF` совпадает с релизной
- генерация `build/libs/modsync-X.Y.Z.jar.sha256`

## Базовый порядок выпуска

1. Закоммить изменения.
2. Прогони `./scripts/run-release-smoke.sh vX.Y.Z`.
3. Запушь ветку в GitHub.
4. Создай тег в формате `vX.Y.Z`.
5. Запушь тег.
6. Дождись workflow `Release`.
7. Проверь созданный GitHub Release и приложенные файлы.

## Пример команд

Для версии `1.0.45`:

```bash
./scripts/run-release-smoke.sh v1.0.45
git tag v1.0.45
git push origin v1.0.45
```

## Что делает workflow Release

После push тега workflow:

1. забирает код
2. проверяет совпадение версии в теге, `gradle.properties` и верхней записи `CHANGELOG.md`
3. настраивает Java 17
4. запускает `./gradlew test`
5. запускает `./gradlew build --warning-mode all`
6. рендерит release notes из верхней подходящей записи `CHANGELOG.md`
7. создаёт GitHub Release
8. прикладывает:
   - `build/libs/*.jar`
   - `build/libs/*.jar.sha256`
   - `CHANGELOG.md`

## Как проверить релиз

После завершения workflow проверь:

- release создан с правильным тегом
- jar действительно приложен к релизу
- размер jar выглядит адекватно
- в названии jar версия совпадает с `gradle.properties`
- в описании релиза есть ожидаемый текст из changelog
- `.sha256` файл приложен и соответствует jar
- `CHANGELOG.md` приложен

## Если релиз не создался

Проверь:

- тег начинается с `v`
- версия тега совпадает с `mod_version` и верхней записью в `CHANGELOG.md`
- workflow `Release` не выключен в GitHub Actions
- у workflow есть права на `contents: write`
- `./gradlew build --warning-mode all` проходит локально
- в Actions нет ошибок на шаге `Create GitHub release`

## Если нужно перевыпустить релиз

Если проблема только в описании или артефакте:

1. Удали неудачный Release в GitHub.
2. При необходимости удали тег в GitHub и локально.
3. Исправь проблему в коде или документации.
4. Создай тег заново на правильном коммите.

### Пример удаления тега

```bash
git tag -d v1.0.39
git push origin :refs/tags/v1.0.39
```

### Затем снова выпусти

```bash
git tag v1.0.39
git push origin v1.0.39
```

## Рекомендуемый минимальный релизный чеклист

- версия обновлена
- changelog обновлён
- тег совпадает с версией в проекте
- тесты зелёные
- build зелёный без Gradle deprecation warning
- объединённый smoke script проходит без ошибок
- ожидаемый versioned jar действительно собран
- версия внутри jar совпадает с релизной
- checksum-файл для jar создан
- ветка запушена
- тег создан и запушен
- release появился в GitHub
- jar скачивается и открывается без сюрпризов
