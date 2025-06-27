/*
 * This file is part of  Enemy Echelons API.
 * Copyright (c) 2025 Mark Gottschling (gottsch)
 *
 * All rights reserved.
 *
 * Enemy Echelons API is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Enemy Echelons API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Enemy Echelons API.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.eechelonsapi.core.registry;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import mod.gottsch.forge.eechelonsapi.core.config.EchelonConfigsHolder;
import mod.gottsch.forge.gottschcore.bst.Interval;
import mod.gottsch.forge.gottschcore.bst.IntervalTree;
import mod.gottsch.forge.gottschcore.random.WeightedCollection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author by Mark Gottschling on 6/2/2025
 */
public class EchelonRegistry {
    private static final ResourceLocation ALL_DIMENSION = new ResourceLocation(".", ".");

    /*
     * map of echelons by id
     * currently not implemented in any meaningful way.
     */
    private final Map<String, EchelonConfigsHolder.Config> CONFIGS_BY_ID = Maps.newHashMap();

    /*
     * map of echelons by dimension
     */
    private final Multimap<ResourceLocation, EchelonConfigsHolder.Config> CONFIGS = ArrayListMultimap.create();

    /*
     * map of echelons by dimension-mod (namespace) pair
     */
    private final Map<Pair<ResourceLocation, String>, EchelonConfigsHolder.Config> CONFIGS_BY_MOD = Maps.newHashMap();

    /*
     * map of echelons by dimension-mob pair.
     * this is for white-list mobs.
     */
    private final Map<Pair<ResourceLocation, ResourceLocation>, EchelonConfigsHolder.Config> CONFIGS_BY_MOB = Maps.newHashMap();

    public void clear() {
        CONFIGS_BY_ID.clear();
        CONFIGS.clear();
        CONFIGS_BY_MOD.clear();
        CONFIGS_BY_MOB.clear();
    }

    public void register(EchelonConfigsHolder.Config echelonConfig) {
        if (ObjectUtils.isEmpty(echelonConfig.getEchelons())) {
            return;
        }

        // TODO need to check if an entry exists already and if the replace property is set.
        // add to map
        if (StringUtils.isNotBlank(echelonConfig.getId())) {
            CONFIGS_BY_ID.put(echelonConfig.getId(), echelonConfig);
        }

        // scan the mob white/black list to see if there are any wildcards and move to mod lists.
        Predicate<String> isWildcard = mob -> mob.contains(":*");
        echelonConfig.getMobWhitelist().stream().filter(isWildcard)
                .forEach(mob -> {
                    echelonConfig.getModWhitelist().add(mob.substring(0, mob.indexOf(":")));
                });
        echelonConfig.getMobBlacklist().stream().filter(isWildcard)
                .forEach(mob -> {
                    echelonConfig.getModBlacklist().add(mob.substring(0, mob.indexOf(":")));
                });
        echelonConfig.getMobWhitelist().removeIf(isWildcard);
        echelonConfig.getMobBlacklist().removeIf(isWildcard);

        /*
         *  build BST
         */
        // create a new tree
        IntervalTree<WeightedCollection<Double, Integer>> tree = new IntervalTree<>();
        // process each strata in the stratum
        echelonConfig.getEchelons().forEach(echelon -> {
            // build weighted collection from histogram
            WeightedCollection<Double, Integer> collection = new WeightedCollection<>();
            echelon.getHistogram().forEach(entry -> {
                collection.add(entry.getWeight(), entry.getDifficulty());
            });
            // update echelon with the collection
            echelon.setWeightedDifficulties(collection);
            // create new interval
            Interval<WeightedCollection<Double, Integer>> interval = new Interval<>(echelon.getMin(), echelon.getMax(), collection);
            // add interval to tree
            tree.insert(interval);
        });

        // add histogram to echelon
        echelonConfig.setHistogram(tree);

        if (ObjectUtils.isEmpty(echelonConfig.getDimensions())) {
            echelonConfig.getDimensions().add(".");
        }

        // build
        echelonConfig.getDimensions().forEach(dimension -> {
            ResourceLocation dimensionKey;
            if (dimension.equals(".") || dimension.equals("*") || dimension.equals("*:*")) {
                dimensionKey = ALL_DIMENSION;
            } else {
                dimensionKey = new ResourceLocation(dimension);
            }

            if (!echelonConfig.getModWhitelist().isEmpty()) {
                echelonConfig.getModWhitelist().forEach(mod -> {
                    // create a key pair
                    Pair<ResourceLocation, String> keyPair = new ImmutablePair<>(dimensionKey, mod);
                    if (!CONFIGS_BY_MOD.containsKey(keyPair)) {
                        CONFIGS_BY_MOD.put(keyPair, echelonConfig);
                    }
                });
            }

            if (!echelonConfig.getMobWhitelist().isEmpty()) {
                echelonConfig.getMobWhitelist().forEach(mob -> {
                    // create a key pair
                    Pair<ResourceLocation, ResourceLocation> keyPair = new ImmutablePair<>(dimensionKey, new ResourceLocation(mob));
                    if (!CONFIGS_BY_MOB.containsKey(keyPair)) {
                        CONFIGS_BY_MOB.put(keyPair, echelonConfig);
                    }
                });
            }

            // register ALL configs in the CONFIGS registry regardless if categorized and registered elsewhere
            CONFIGS.put(dimensionKey, echelonConfig);
        });
    }

    public void register(List<EchelonConfigsHolder.Config> configs) {
        if (ObjectUtils.isEmpty(configs)) {
            return;
        }
        configs.forEach(this::register);
    }

    /**
     * @param mob
     * @return
     */
    public Optional<EchelonConfigsHolder.Config> getEchelonConfig(Mob mob) {
        ResourceLocation mobId = EntityType.getKey(mob.getType());
        Pair<ResourceLocation, ResourceLocation> keyPair = new ImmutablePair<>(mob.level().dimension().location(), mobId);
        if (CONFIGS_BY_MOB.containsKey(keyPair)) {
            return Optional.of(CONFIGS_BY_MOB.get(keyPair));
        } else {
            keyPair = new ImmutablePair<>(ALL_DIMENSION, mobId);
            if (CONFIGS_BY_MOB.containsKey(keyPair)) {
                return Optional.of(CONFIGS_BY_MOB.get(keyPair));
            } else {
                // create a key pair for the mod
                Pair<ResourceLocation, String> modKeyPair = new ImmutablePair<>(mob.level().dimension().location(), keyPair.getValue().getNamespace());
                // check by mod (ie whitelisted mods)
                if (CONFIGS_BY_MOD.containsKey(modKeyPair)) {
                    EchelonConfigsHolder.Config config = CONFIGS_BY_MOD.get(modKeyPair);
                    if (config.getMobBlacklist().contains(mobId.toString())) {
                        return Optional.empty();
                    }
                    return Optional.of(config);
                } else {
                    modKeyPair = new ImmutablePair<>(ALL_DIMENSION, keyPair.getValue().getNamespace());
                    if (CONFIGS_BY_MOD.containsKey(modKeyPair)) {
                        EchelonConfigsHolder.Config config = CONFIGS_BY_MOD.get(modKeyPair);
                        if (config.getMobBlacklist().contains(mobId.toString())) {
                            return Optional.empty();
                        }
                        return Optional.of(config);
                    }
                }

                // search all remaining configs
                Optional<EchelonConfigsHolder.Config> echelon = searchEchelonsForMob(mob.level().dimension().location(), mob);
                if (echelon.isEmpty()) {
                    echelon = searchEchelonsForMob(ALL_DIMENSION, mob);
                }
                return echelon;
            }
        }
    }

    public Optional<EchelonConfigsHolder.Config> searchEchelonsForMob(ResourceLocation dimension, Mob entity) {
        Optional<EchelonConfigsHolder.Config> echelon = Optional.empty();
        ResourceLocation mob = EntityType.getKey(entity.getType());

        // for each config in a given dimension
        for (EchelonConfigsHolder.Config e : CONFIGS.get(dimension)) {
            // find the first valid echelon - ie in the mod whitelist and not in the blacklists
            if (!e.getModWhitelist().isEmpty()) {
                if (e.getModWhitelist().contains(mob.getNamespace())) {
                    if (e.getMobBlacklist().contains(mob.toString())) {
                        return Optional.empty();
                    }
                }
                // if whitelist is not empty, and the mob's namespace is not included
                else {
                    return Optional.empty();
                }
            }

            if (e.getModWhitelist().isEmpty()
                    && e.getModBlacklist().contains(mob.getNamespace())) {
                return Optional.empty();
            }

            // if not in the MOB blacklist
            if (!e.getMobBlacklist().contains(mob.toString())) {
                return Optional.of(e);
            }
        }
        return echelon;
    }
}
