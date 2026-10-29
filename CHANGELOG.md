# Changelog for Enemy Echelons API 1.20.1

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-10-29

### Added
- Echelon configs, difficulty-name configs and exclusions can be loaded from datapacks (including a mod's own jar), alongside the existing TOML configs. They are re-read on `/reload`.
  - `data/<namespace>/echelons/<name>.json` - one echelon config. The id is the file's resource location, ex. `mymod:deep`.
  - `data/<namespace>/difficulty_names/<name>.json` - one difficulty-name config.
  - `data/<namespace>/echelons/exclusions/<name>.json` - `{ "mods": [...], "mobs": [...] }`. Every echelon config in the global registry (TOML and datapack) treats these as blacklisted, and an exclusion wins over every whitelist. Excluded mobs are left untouched (difficulty unset), so a mod's own custom registry can still apply to them.
- Keys are the snake_case forms of the TOML keys, plus an optional `load_order` (default 99).
- Datapack files are strict: an unknown key or a malformed value is logged as an error naming the file, and that file is skipped.
- With datapack configs present, the registry is rebuilt from the TOML configs plus the datapack configs, sorted by (load_order, id). TOML configs are never removed by a reload.
- Mobs that have already spawned keep their difficulty; a reload only affects new spawns.

### Changed
- None of the existing public API changed. Without datapack configs, the registries behave exactly as in 1.0.1.

## [1.0.1] - 2025-11-20

### Changed
- Fixed update.json not being parseable.

## [1.0.0] - 2025-06-27

Initial Release.