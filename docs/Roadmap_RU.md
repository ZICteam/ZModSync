# SyncBridge Roadmap

Текущая версия мода: `1.0.91`

Этот документ фиксирует практическую дорожную карту развития SyncBridge.
Фокус: стабильная синхронизация модов и клиентских файлов для одного сервера без усложнения через профили модов.

## Цели

- сделать синхронизацию предсказуемой и безопасной для реального сервера
- сократить число сбоев при скачивании и обновлении файлов
- упростить эксплуатацию мода для администратора и игроков
- держать документацию и версионирование в актуальном состоянии

## Приоритет P0

- выровнять все пути выдачи ссылок на скачивание, чтобы `public_http_base_url` работал одинаково в HTTP-манифесте и сетевом handshake
  Статус: выполнено в `1.0.3`
- проверить и при необходимости упростить сценарий первого входа игрока на сервер
  Статус: выполнено в `1.0.4`
- убедиться, что cleanup не удаляет лишние файлы и не ломает клиент после смены содержимого сервера
  Статус: выполнено в `1.0.5`
- добавить smoke-checklist для ручной проверки после каждого важного изменения
  Статус: выполнено в `1.0.6`

## Приоритет P1

- улучшить обработку сетевых ошибок и сделать причины отказов понятнее в логах и интерфейсе
  Статус: базовое улучшение выполнено в `1.0.7`
- привести `gradlew` и базовую структуру репозитория в более удобное состояние для локальной сборки и CI
  Статус: базовое улучшение выполнено в `1.0.8`
- добавить минимальные тесты на manifest generation, path safety и file comparison
  Статус: базовое покрытие добавлено в `1.0.9`
- проверить работу больших сборок и поведение при большом числе файлов
  Статус: базовая оптимизация сканирования добавлена в `1.0.10`

## Приоритет P2

- улучшить UX экрана синхронизации, чтобы игроку было понятнее, что происходит и нужен ли перезапуск
  Статус: базовое улучшение выполнено в `1.0.11`
- расширить документацию примерами типовой серверной структуры и сценариев reverse proxy
  Статус: базовое расширение выполнено в `1.0.12`
- подготовить пакет эксплуатационных инструкций для администраторов: выпуск обновления, откат, диагностика проблем
  Статус: базовый пакет подготовлен в `1.0.13`

## Что не в фокусе

- профили модов на стороне сервера
- сложные сценарии переключения между разными серверными сборками
- лишняя автоматизация до стабилизации основного потока синхронизации

## Критерий готовности ближайшего этапа

Ближайший этап можно считать успешным, если:

- клиент стабильно получает манифест и скачивает нужные файлы
- ссылки на загрузку корректно работают как напрямую, так и через `public_http_base_url`
- обновление серверной сборки не приводит к случайной поломке клиента
- администратор может опираться на актуальную документацию без чтения исходников

## Следующий этап

- улучшить CI и выпуск артефактов
  Статус: базовая выгрузка build artifact добавлена в `1.0.14`, GitHub Releases workflow добавлен в `1.0.17`, CI warning-clean verification и upload test report добавлены в `1.0.37`, concurrency/timeouts hardening добавлены в `1.0.38`, release version consistency check добавлен в `1.0.39`, checksum publication добавлена в `1.0.45`
- постепенно убрать предупреждения совместимости с будущим Gradle 9
  Статус: базовая модернизация build script выполнена в `1.0.18`, warning перепроверен в `1.0.21`, в `1.0.34` локально не воспроизводился стабильно, в `1.0.35` была уточнена JUnit-конфигурация, а в `1.0.36` warning про automatic test framework implementation loading убран через явный `junit-platform-launcher`
- расширить тестовое покрытие сценариями cleanup и download flow
  Статус: cleanup decision coverage добавлено в `1.0.15`, download-flow messaging coverage добавлено в `1.0.16`, sync progress state coverage добавлено в `1.0.22`, sync comparison edge-case coverage добавлено в `1.0.23`, packet chunking coverage добавлено в `1.0.24`, manifest URL resolution coverage добавлено в `1.0.25`, MOTD metadata coverage добавлено в `1.0.26`, restart-state coverage добавлено в `1.0.27`, managed-state naming coverage добавлено в `1.0.28`, server sync status cache coverage добавлено в `1.0.29`, category mapping coverage добавлено в `1.0.30`, file path filtering coverage добавлено в `1.0.31`, protected-file detection coverage добавлено в `1.0.32`, manifest entry coverage добавлено в `1.0.33`, file hash cache coverage добавлено в `1.0.48`, full managed-state persistence coverage добавлено в `1.0.49`, direct hash utility coverage добавлено в `1.0.50`, client/runtime state-holder coverage добавлено в `1.0.51`, direct client file scanner coverage добавлено в `1.0.52`, direct manifest generator coverage добавлено в `1.0.53`, mini sync-pipeline integration coverage добавлено в `1.0.54`, cleanup integration coverage добавлено в `1.0.55`, direct download task coverage добавлено в `1.0.56`, post-download finalize coverage добавлено в `1.0.57`, local-HTTP download queue integration coverage добавлено в `1.0.58`, mixed download queue coverage добавлено в `1.0.59`, а invalid-hash download queue coverage добавлено в `1.0.60`
- усилить manifest/pre-join HTTP этап
  Статус: manifest HTTP local-server coverage добавлено в `1.0.61`, public manifest transformation coverage добавлено в `1.0.62`, full discovered-port/configured-port fetch path coverage добавлено в `1.0.63`, path-encoding fix для имён с пробелами и URL-sensitive символами добавлен в `1.0.67`, embedded file-server resolution coverage для encoded paths добавлено в `1.0.68`, network-tolerance hardening для manifest/file download timeout path добавлено в `1.0.85`, а client-side fallback с битого `public_http_base_url` на адрес выбранного сервера добавлен в `1.0.86`
- усилить packet-based handshake path
  Статус: direct `NetworkHandler` coverage добавлено в `1.0.69`, включая encoded download URLs и выбор итогового download-list через cached manifest comparison, `start-download` decision coverage добавлено в `1.0.70`, server-side `client file list -> outbound manifest + required downloads` coverage добавлено в `1.0.71`, fallback coverage для отсутствующего cached manifest добавлено в `1.0.72`, mini packet-handshake integration coverage добавлено в `1.0.73`, chunked packet-handshake integration coverage для длинных encoded путей добавлено в `1.0.74`, empty-manifest-cache integration coverage добавлено в `1.0.75`, stale `serverSuggested` chunked integration coverage добавлено в `1.0.76`, invalid chunk-order/count hardening добавлено в `1.0.77`, а filtering для undownloadable `serverSuggested` entries добавлено в `1.0.82`
- усилить manifest parsing robustness
  Статус: normalization для `null` manifest и `entries: null` добавлена в `1.0.78`, фильтрация `null` entries внутри payload добавлена в `1.0.79`, фильтрация malformed entries без category/relativePath добавлена в `1.0.80`, фильтрация entries без usable SHA-256 добавлена в `1.0.81`, defensive container hardening для `ManifestData.entries` добавлено в `1.0.83`, а defensive filtering для malformed comparison entries в `SyncComparator` добавлено в `1.0.84`
- усилить pre-join cache path
  Статус: local scan cache reuse/expiry/invalidation coverage добавлено в `1.0.64`
- усилить pre-join orchestration path
  Статус: decision coverage для `PreJoinSyncManager` добавлено в `1.0.65`, включая already-synced, download-required и auto-continue сценарии
- усилить post-login handshake orchestration path
  Статус: coverage для `ClientBootstrap` добавлено в `1.0.66`, включая local-session skip, duplicate-handshake suppression и обычный multiplayer login path, ложный server-side handshake requirement для integrated/local session устранён в `1.0.85`, dedicated post-login acknowledgement после успешного pre-join sync добавлен в `1.0.87`, а гонка между ранним client hello и поздней pending registration на dedicated сервере устранена в `1.0.89`
- добавить безопасное staged self-update поведение для самого `modsync.jar`
  Статус: базовый post-exit self-update для клиентского `modsync` добавлен в `1.0.88`
- переименовать публичный бренд проекта для CurseForge и релизных страниц
  Статус: в `1.0.91` публичное имя проекта изменено на `SyncBridge`, а технический `modsync` mod id сохранён ради совместимости
- при необходимости собрать отдельный release-процесс под GitHub Releases
  Статус: базовый release workflow добавлен в `1.0.17`, release guide добавлен в `1.0.20`, release version consistency check добавлен в `1.0.39`, локальный release smoke helper добавлен в `1.0.40`, CI/release переведены на тот же smoke helper в `1.0.41`, release notes rendering из changelog добавлен в `1.0.42`, artifact verification добавлена в `1.0.43`, jar metadata verification добавлена в `1.0.44`, checksum generation/publication добавлена в `1.0.45`

## Правило сопровождения

После каждого изменения в проекте:

- поднять версию мода
- обновить `CHANGELOG.md`
- обновить этот roadmap, если изменились приоритеты или направление работ
- обновить остальную документацию, если изменилось поведение мода
