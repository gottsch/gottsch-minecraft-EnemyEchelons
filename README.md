# Enemy Echelons
Enemy Echelons is a mod that assigns levels/ranks and attribute modifications to mobs based on the world height range that they spawn at. This is a Vanilla+ mob progression mod in the same vein as Champions and Sliding Health. 

My goal was to create a mod in which the mobs progressively get more difficult the farther you travel down in the Overworld. It can be customized to be more difficult the farther you travel up as well, or both. I believe it works best in dimensions where there is a clear progression/change in the terrain, but it can be customized to any dimension.

## Echelons
An echelon is made of all the mob attribute modifiers for a custom defined set of dimensions and mobs. Each echelon defintion has a list of stratum defined by a  min and max y-value. Each strata has a list of level-weight entries (histogram). These define the likelihood of a level being assigned to a mob upon spawn.

For more info and examples, see the [Echelons wiki page](https://github.com/gottsch/gottsch-minecraft-EnemyEchelons/wiki).

## Levels
A level is assigned to a mob at spawn and this level determines that amount of modification to its attributes (health, damage, speed, etc). A mob with `Level 0` indicates a vanilla mob that has been unchanged. For every level, a mob will have its attributes adjusted by the `factor` or `increment` settings in the echelons configuration.

## Echelon Configuration
Enemy Echelons is very configurable, allowing you to add as few or as many levels as you wish and to setup their respective attribute modifiers.

See the [Configurations wiki page](https://github.com/gottsch/gottsch-minecraft-EnemyEchelons/wiki).

## Datapack Configs (1.1.0+)
Echelon configs, difficulty-name configs and exclusions can also be shipped as datapack JSON, including inside a mod's own jar (`src/main/resources/data/...`). They load on server start and are re-read on `/reload`. The TOML configs keep working unchanged, and datapack configs are added alongside them.

| Path | Contents |
|---|---|
| `data/<namespace>/echelons/<name>.json` | one echelon config |
| `data/<namespace>/difficulty_names/<name>.json` | one difficulty-name config |
| `data/<namespace>/echelons/exclusions/<name>.json` | mods/mobs that no echelon config may apply to |

- **Id:** the id of a config is its file's resource location, ex. `data/mymod/echelons/deep.json` -> `mymod:deep`. There is no `id` key.
- **Keys** are the snake_case forms of the TOML keys: `hp_factor`/`max_hp`, `damage_factor`/`max_damage`, `knockback_increment`/`max_knockback`, `knockback_resist_increment`/`max_knockback_resist`, `armor_factor`/`max_armor`, `armor_toughness_factor`/`max_armor_toughness`, `speed_factor`/`max_speed`, `flying_speed_factor`/`max_flying_speed`, `xp_factor`/`max_xp`, `dimensions`, `replace`, `mod_whitelist`, `mod_blacklist`, `mob_whitelist`, `mob_blacklist`, `echelons` (`id` optional, `min`, `max`, `histogram` of `difficulty`/`weight`). Difficulty-name configs take the four lists plus `difficulty_names` (`difficulty`/`name`).
- **`load_order`** is optional (default 99, as the TOML `loadOrder`). When datapack configs are present, the registry holds the TOML configs plus the datapack configs, sorted by (`load_order`, id). A lower number takes precedence.
- **Strict:** an unknown key or a malformed value (wrong type, a fractional difficulty, `min` > `max`, a weight <= 0, an empty `echelons`/`histogram`/`difficulty_names`) is logged as an error naming the file, and that file is skipped.
- **Caps:** the `max_*` values are read and stored, but are not enforced yet (except `max_xp`), same as for TOML.

### Echelon config: `data/mymod/echelons/deep.json`
```json
{
  "load_order": 10,
  "dimensions": ["minecraft:overworld"],
  "hp_factor": 0.2,
  "damage_factor": 0.2,
  "max_damage": 10.0,
  "xp_factor": 0.2,
  "mod_whitelist": ["minecraft"],
  "mob_blacklist": ["minecraft:creeper"],
  "echelons": [
    { "min": 0, "max": 319, "histogram": [ { "difficulty": 0, "weight": 200.0 }, { "difficulty": 1, "weight": 100.0 } ] },
    { "min": -64, "max": -1, "histogram": [ { "difficulty": 2, "weight": 100.0 }, { "difficulty": 3, "weight": 25.0 } ] }
  ]
}
```

### Difficulty names: `data/mymod/difficulty_names/brutes.json`
```json
{
  "mob_whitelist": ["minecraft:zombie", "minecraft:skeleton"],
  "difficulty_names": [
    { "difficulty": 1, "name": "Brute" },
    { "difficulty": 2, "name": "Captain" }
  ]
}
```
A mob wildcard (`mymod:*`) in a mob list is the same as adding `mymod` to the matching mod list.

### Exclusions: `data/mymod/echelons/exclusions/mymod.json`
```json
{
  "mods": ["mymod"],
  "mobs": ["othermod:boss"]
}
```
- Every exclusion file is merged, and the result applies to every echelon config in the global registry, from both TOML and datapacks. This is how a mod keeps its mobs out of other mods' configs, ex. a catch-all.
- **An exclusion is absolute:** it wins over `mod_whitelist` and `mob_whitelist`. A mod can opt out even of a config that whitelists it. (Within a single config, `mod_whitelist` still supersedes `mod_blacklist`, as before.)
- `"othermod:*"` in `mobs` is the same as `"othermod"` in `mods`.
- An excluded mob is left untouched by the global registry: its difficulty stays unset rather than being set to 0. A mod's own custom registry (`EnemyEchelonsApi.customRegistry()`) has no exclusions, so it can still apply its own echelons to that mob.
- Exclusions don't affect difficulty names.
- To undo an exclusion, a higher-priority datapack can override the file at the same path with empty lists.
