# ModSync

ModSync is a universal Forge 1.20.1 mod that lets a server distribute mods and resource files to connecting clients while Minecraft is running.

## Requirements

- Java 17
- Gradle 8.x or the Gradle wrapper
- Minecraft Forge 1.20.1 / Forge 47.x

## Build

```bash
gradle build
```

The built universal jar is created under `build/libs/`.

## Server Repository Layout

Server mods are synchronized directly from the active server `mods/` folder.

Client-only and non-mod content stays in `sync_repo/`:

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

The mod scans `mods/` plus `sync_repo/` and writes a generated manifest to `config/modsync-manifest.json`.

## Runtime Flow

1. Client joins the server and sends a handshake.
2. Client scans installed files and sends metadata to the server.
3. Server generates the manifest and returns it to the client.
4. Client compares local files with the manifest.
5. Missing or outdated files download in background threads over the embedded HTTP server.
6. Files previously installed by ModSync are removed if they no longer exist in that server's manifest.
7. If restart-required files were changed, the client sees a restart prompt.

## Configuration

An example configuration file is included at `config/modsync.toml`.

Important options:

- `server_http_port`
- `server_http_bind`
- `public_http_base_url`
- `optional_client_target`
- `download_threads`
- `retry_count`
- `verify_hash_after_download`
- `skip_file_extensions`

## Reverse Proxy

If you do not want to expose `server_http_port` directly, put the embedded HTTP server behind a reverse proxy and set:

```toml
public_http_base_url = "https://your-domain.example/modsync"
```

The server will publish download URLs using that base URL.

The proxy must forward these paths to the internal ModSync HTTP server:

- `/manifest`
- `/files/...`

If `public_http_base_url` is empty, ModSync falls back to direct links using `server_http_port`.

## Notes

- Files are downloaded through temporary files before replacement when enabled.
- Hashes are verified with SHA-256.
- Path traversal is blocked on both manifest generation and HTTP serving.
- The mod logs operational events to `logs/modsync.log` when enabled.

## Documentation

- `docs/README.md`
- `docs/Server_Setup_RU.md`
- `docs/Player_Guide_RU.md`
- `docs/Discord_Templates_RU.md`
- `docs/ModSync_FAQ_Discord.md`
