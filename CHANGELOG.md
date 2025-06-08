# Changelog for Enemy Echelons 1.20.1

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2025-05-26

### Added
- echelons_difficulty_naming_config.toml file(s).
- display name in the GUI HUD
- shudHud config to Client. note - this cannot override server showHud = false setting.
- showLevel0Hud config to Client. this setting prevent showing the hud if the mob level = 0 (which is vanilla level).
- implemented Fly Speed modification.

### Changed
- renamed terms in the echelon config.  ie rename [[echelons]] to [[configs]].  rename [[level]] to [[difficulty]] or [[rank]], rename [[stratum]] to [[echelon]].
- renamed **eechelons-echelons.toml** to **echelons_config.toml**.
- create a config sub-folder **enemy_echelons**
- create a default echelons_config.toml for vanilla only mobs. also create an empty echelons_custom_config.toml
- mod should read and register ALL .toml files in the **enemey_echelons** subfolder
- add API to allow other mods to register a config file(s).
- GUI HUD grows in width dependent on text length
- tweaked the GUI HUD texture.
- modified all attribute modifiers using permanent attribute modifiers
- moved calculation of XP reward to event instead of using reflection in the EchelonManager.
- remove unnecessary rendering code.
- refactored package names
- TODO removed option to change GUI HUD texture.

## [1.4.0]  - 2025-02-13

### Added
- config option to enable/disable custom HUD range. using a custom HUD range is more computationally expensive on the client side.

### Changed
- added condition to check for the Custom HUD range config. If =false, then use vanilla.
- don't show HUD when MC HUD option = false


## [1.3.0] - 2024-09-07

### Changed
- moved hudRange config option to Server-side. Can give unfair advantages.
- fixed mob level determination when mobs are blacklisted from all echelons.
- fixed update url to point to the correct file.
- reworked internal storage and references to echelons and histograms.
  -- works for a wider range of echelon configurations now.
- fixed pack.mcmeta
- changed isValidEntity() to check against Mob instead of Enemy.

## [1.2.0] - 2024-01-31

### Changed

- Port from 1.19.3
- Using Changelod instead of update.json.
- Reduced default HUD range to 10.
- Updated echelons toml file to v2.