# ModSync: настройка сервера

## Что нужно

- Minecraft `1.20.1`
- Forge `47.x`
- Java `17`
- мод `ModSync` на сервере
- мод `ModSync` на клиентах

## Куда ставить мод

Файл `ModSync` кладётся в:

```text
mods/
```

## Структура файлов

```text
server/
  mods/
  config/
  sync_repo/
    resourcepacks/
    shaderpacks/
    configs/
    optional_client/
```

## Что откуда синхронизируется

- `mods/` -> серверные моды
- `sync_repo/resourcepacks/` -> resourcepacks
- `sync_repo/shaderpacks/` -> shaderpacks
- `sync_repo/configs/` -> configs
- `sync_repo/optional_client/` -> optional client контент

## Первый запуск

После первого запуска проверь:

- создался `config/modsync.toml`
- создался `config/modsync-manifest.json`
- ModSync не пишет ошибок в `logs/modsync.log`

## Важные настройки

Файл:

```text
config/modsync.toml
```

Минимально проверь:

```toml
enable_http_server = true
server_http_port = 8080
server_http_bind = "0.0.0.0"
public_http_base_url = ""

enable_mods_sync = true
enable_resourcepacks_sync = true
enable_shaderpacks_sync = false
enable_config_sync = true
enable_optional_client_sync = false

download_threads = 4
retry_count = 3
verify_hash_after_download = true
delete_invalid_files = true
use_temp_files = true
```

## Если используешь reverse proxy

Пример:

```toml
public_http_base_url = "https://example.com/modsync"
```

Proxy должен пропускать:

- `/manifest`
- `/files/...`

## Команды

Перечитать конфиг и пересобрать runtime:

```text
/modsync reload
```

Принудительно обновить manifest:

```text
/modsync manifest refresh
```

Показать статус:

```text
/modsync status
```

## После изменения сборки

1. Обнови файлы в `mods/` или `sync_repo/`
2. Выполни:

```text
/modsync manifest refresh
```

3. Проверь:

```text
/modsync status
```

4. Протестируй вход клиентом с Forge + ModSync

## Если игрок не может войти

Проверь:

- установлен ли ModSync у игрока
- доступен ли `server_http_port`
- открывается ли `/manifest`
- нет ли `Invalid SHA-256` в логах
- не блокирует ли proxy раздачу `/files/...`

## Если игрок без ModSync

Сервер должен отклонить такого игрока с сообщением:

```text
Missing required client sync mod. Install Forge and ModSync to join this server.
```
