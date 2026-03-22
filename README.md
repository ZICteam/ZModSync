# ModSync

ModSync is a universal Forge 1.20.1 mod that lets a server distribute mods and resource files to connecting clients while Minecraft is running.

Current mod version: `1.0.16`

## Requirements

- Java 17
- Gradle 8.x or the Gradle wrapper
- Minecraft Forge 1.20.1 / Forge 47.x

## Build

```bash
./gradlew build
```

The built universal jar is created under `build/libs/`.

## CI

The repository includes a GitHub Actions workflow that runs `./gradlew build` on pushes and pull requests.
Minimal automated tests are available through `./gradlew test`.
Successful CI runs also upload the built jar from `build/libs/` as a workflow artifact.
Test coverage now includes cleanup decision cases and download-flow error messaging in addition to path safety, manifest JSON, and file comparison.

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
6. When sync was started from the connect flow, the client continues into the server automatically after pre-join sync completes.
7. Files previously installed by ModSync are removed if they no longer exist in that server's manifest.
8. If restart-required files were changed, the client sees a restart prompt.

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
When `public_http_base_url` is set, ModSync now uses it consistently for generated download links in both manifest delivery paths.
Detailed deployment examples are documented in `docs/Server_Setup_RU.md`.

## Notes

- Files are downloaded through temporary files before replacement when enabled.
- Hashes are verified with SHA-256.
- Path traversal is blocked on both manifest generation and HTTP serving.
- Cleanup only removes files that still match the last ModSync-managed version, which reduces the risk of deleting locally replaced files.
- The mod logs operational events to `logs/modsync.log` when enabled.
- Network and download failures are surfaced both in the log panel and directly on the sync progress screen.
- Client and server scans avoid some duplicate filesystem metadata reads to behave a bit better on large file sets.
- The sync progress screen now shows a clearer high-level state so players can tell whether ModSync is checking, downloading, finished, failed, or waiting for restart.

## Documentation

- `docs/README.md`
- `docs/Server_Setup_RU.md`
- `docs/Player_Guide_RU.md`
- `docs/Discord_Templates_RU.md`
- `docs/ModSync_FAQ_Discord.md`
- `docs/Roadmap_RU.md`
- `docs/Smoke_Checklist_RU.md`
- `docs/Operations_Guide_RU.md`
- `CHANGELOG.md`

## Release Process

For every change in this repository:

- bump `mod_version` in `gradle.properties`
- add an entry to `CHANGELOG.md` with the new version and a short summary of what changed
- update documentation in `README.md` and/or `docs/` when behavior, setup, UI, or workflows changed
