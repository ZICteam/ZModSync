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

## Рекомендуемая production-структура

Пример сервера, где ModSync уже встроен в обычную админскую схему:

```text
server/
  mods/
    modsync-1.0.12.jar
    your-server-mods.jar
  config/
    modsync.toml
    modsync-manifest.json
  logs/
    modsync.log
  sync_repo/
    resourcepacks/
      main-pack.zip
    shaderpacks/
    configs/
      journeymap/
      xaerominimap.txt
    optional_client/
      voice-chat-client.jar
```

Практический смысл такой:

- обязательные серверные и клиентские моды лежат в `mods/`
- клиентские ресурсы и дополнительные файлы лежат в `sync_repo/`
- generated-файлы ModSync живут в `config/` и `logs/`

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

## Типовой сценарий без reverse proxy

Если сервер может напрямую отдавать HTTP:

```toml
enable_http_server = true
server_http_port = 8080
server_http_bind = "0.0.0.0"
public_http_base_url = ""
```

Что проверить:

- порт `8080` открыт снаружи
- `http://YOUR_HOST:8080/manifest` открывается
- `http://YOUR_HOST:8080/files/...` не режется firewall'ом или хостингом

## Типовой сценарий с reverse proxy

Если прямой HTTP-порт светить не хочется, используй внешний URL:

```toml
enable_http_server = true
server_http_port = 8080
server_http_bind = "127.0.0.1"
public_http_base_url = "https://example.com/modsync"
```

В этом режиме:

- ModSync слушает локально на `127.0.0.1:8080`
- наружу публикуется только `https://example.com/modsync`
- клиенты получают ссылки уже через `public_http_base_url`

## Если используешь reverse proxy

Пример:

```toml
public_http_base_url = "https://example.com/modsync"
```

Proxy должен пропускать:

- `/manifest`
- `/files/...`

### Пример для Nginx

```nginx
location /modsync/manifest {
    proxy_pass http://127.0.0.1:8080/manifest;
    proxy_set_header Host $host;
}

location /modsync/files/ {
    proxy_pass http://127.0.0.1:8080/files/;
    proxy_set_header Host $host;
}
```

Для такого примера в `modsync.toml` нужно:

```toml
public_http_base_url = "https://example.com/modsync"
```

### Что важно не сломать на proxy

- не убирать часть пути `/modsync`
- не блокировать длинные загрузки и крупные файлы
- не подменять `/files/...` на другой внутренний путь
- не забыть HTTPS-сертификат, если наружу используется `https://`

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

## Минимальная проверка после настройки

1. Запусти сервер.
2. Убедись, что создался `config/modsync-manifest.json`.
3. Проверь `logs/modsync.log` на старте.
4. Открой `/manifest` напрямую или через proxy.
5. Подключись тестовым клиентом без части нужных файлов.
6. Убедись, что ModSync скачивает файлы и доводит вход до конца.

## Если игрок не может войти

Проверь:

- установлен ли ModSync у игрока
- доступен ли `server_http_port`
- открывается ли `/manifest`
- нет ли `Invalid SHA-256` в логах
- не блокирует ли proxy раздачу `/files/...`
- совпадает ли `public_http_base_url` с реальным внешним адресом

## Если игрок без ModSync

Сервер должен отклонить такого игрока с сообщением:

```text
Missing required client sync mod. Install Forge and ModSync to join this server.
```
