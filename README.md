# Enemy Echelons
Enemy Echelons is a mod that assigns levels and attribute modifications to mobs based on the world height range that they spawn at. This is a Vanilla+ mob progression mod in the same vein as Champions and Sliding Health. 

My goal was to create a mod in which the mobs progressively get more difficult the farther you travel down in the Overworld. It can be customized to be more difficult the farther you travel up as well, or both. I believe it works best in dimensions where there is a clear progression/change in the terrain, but it can be customized to any dimension.

## Echelons
An echelon is made of all the mob attribute modifiers for a custom defined set of dimensions and mobs. Each echelon defintion has a list of stratum defined by a  min and max y-value. Each strata has a list of level-weight entries (histogram). These define the likelyhood of a level being assigned to a mob upon spawn.

For more info and examples, see the [Echelons wiki page](https://github.com/gottsch/gottsch-minecraft-EnemyEchelons/wiki).

## Levels
A level is assigned to a mob at spawn and this level determines that amount of modification to its attributes (health, damage, speed, etc).

## Echelon Configuration
Enemy Echelons is very configurable, allowing you to add as few or as many levels as you wish and to setup their respective attribute modifiers.

See the [Configurations wiki page](https://github.com/gottsch/gottsch-minecraft-EnemyEchelons/wiki).