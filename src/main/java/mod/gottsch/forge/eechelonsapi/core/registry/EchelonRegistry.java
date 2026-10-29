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
import mod.gottsch.forge.eechelonsapi.core.config.EchelonExclusions;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
     * the lookup indexes. a rebuild fills a new instance and swaps it in with one (volatile) write,
     * so a lookup on another thread never sees a half built registry.
     */
    private static class Indexes {
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

        /*
         * the configs in the order they were indexed
         */
        private final List<EchelonConfigsHolder.Config> INDEXED = new ArrayList<>();
    }

    private volatile Indexes indexes = new Indexes();

    /*
     * the two sources of configs are kept apart so that a datapack reload never wipes the configs
     * registered through register() (ie. toml or code), and vice versa.
     */
    private final List<EchelonConfigsHolder.Config> registeredConfigs = new ArrayList<>();
    private final List<EchelonConfigsHolder.Config> dataConfigs = new ArrayList<>();

    /*
     * mods/mobs that no config in this registry applies to.
     */
    private volatile EchelonExclusions exclusions = EchelonExclusions.EMPTY;

    private static final Comparator<EchelonConfigsHolder.Config> LOAD_ORDER = Comparator
            .comparingInt((EchelonConfigsHolder.Config c) -> c.getLoadOrder())
            .thenComparing(c -> StringUtils.defaultString(c.getId()));

    /**
     * clears all configs registered through register().
     * datapack configs (if any) are owned by the reload listener and remain registered.
     */
    public synchronized void clear() {
        registeredConfigs.clear();
        rebuild();
    }

    public synchronized void register(EchelonConfigsHolder.Config echelonConfig) {
        registeredConfigs.add(echelonConfig);
        // a replacement has to remove the earlier config from the indexes, so rebuild
        if (dataConfigs.isEmpty() && !isReplacement(echelonConfig)) {
            index(indexes, echelonConfig);
        } else {
            rebuild();
        }
    }

    public synchronized void register(List<EchelonConfigsHolder.Config> configs) {
        if (ObjectUtils.isEmpty(configs)) {
            return;
        }
        registeredConfigs.addAll(configs);
        if (dataConfigs.isEmpty() && configs.stream().noneMatch(EchelonRegistry::isReplacement)) {
            Indexes current = indexes;
            configs.forEach(config -> index(current, config));
        } else {
            rebuild();
        }
    }

    private static boolean isReplacement(EchelonConfigsHolder.Config config) {
        return config != null && Boolean.TRUE.equals(config.getReplace()) && StringUtils.isNotBlank(config.getId());
    }

    /*
     * a config with replace = true takes the place of the earlier configs (in this order) with the same id,
     * so it also takes their precedence. without replace, configs with the same id are all kept (as in 1.0.x).
     */
    private static List<EchelonConfigsHolder.Config> resolveReplacements(List<EchelonConfigsHolder.Config> configs) {
        List<EchelonConfigsHolder.Config> result = new ArrayList<>();
        for (EchelonConfigsHolder.Config config : configs) {
            // a config without echelons is never indexed, so it can't replace anything either
            if (ObjectUtils.isEmpty(config.getEchelons())) {
                continue;
            }
            int place = -1;
            if (isReplacement(config)) {
                for (int i = result.size() - 1; i >= 0; i--) {
                    if (config.getId().equals(result.get(i).getId())) {
                        result.remove(i);
                        place = i;
                    }
                }
            }
            if (place >= 0) {
                result.add(place, config);
            } else {
                result.add(config);
            }
        }
        return result;
    }

    /**
     * replaces the datapack configs and rebuilds the registry.
     * if there are no datapack configs before or after, nothing is touched.
     * @param configs
     */
    public synchronized void setDataConfigs(List<EchelonConfigsHolder.Config> configs) {
        boolean hadData = !dataConfigs.isEmpty();
        dataConfigs.clear();
        if (configs != null) {
            dataConfigs.addAll(configs);
        }
        if (hadData || !dataConfigs.isEmpty()) {
            rebuild();
        }
    }

    public synchronized List<EchelonConfigsHolder.Config> getDataConfigs() {
        return List.copyOf(dataConfigs);
    }

    /**
     * @return the configs registered through register(), ie. toml or code, in registration order.
     */
    public synchronized List<EchelonConfigsHolder.Config> getRegisteredConfigs() {
        return Collections.unmodifiableList(new ArrayList<>(registeredConfigs));
    }

    /**
     * rebuilds the indexes from both sources.
     * without datapack configs, the registered configs are indexed in registration order (as in 1.0.x).
     * with datapack configs, all configs are indexed by (loadOrder, id).
     * then replacements are applied.
     */
    private void rebuild() {
        List<EchelonConfigsHolder.Config> configs = new ArrayList<>(registeredConfigs);
        if (!dataConfigs.isEmpty()) {
            configs.addAll(dataConfigs);
            // stable sort, so equal keys keep their registration order
            configs.sort(LOAD_ORDER);
        }
        Indexes next = new Indexes();
        resolveReplacements(configs).forEach(config -> index(next, config));
        indexes = next;
    }

    /**
     * @return the indexed configs, in the order they were indexed. ie. first is highest precedence.
     */
    public synchronized List<EchelonConfigsHolder.Config> getConfigs() {
        return Collections.unmodifiableList(new ArrayList<>(indexes.INDEXED));
    }

    public EchelonExclusions getExclusions() {
        return exclusions;
    }

    public void setExclusions(EchelonExclusions exclusions) {
        this.exclusions = exclusions == null ? EchelonExclusions.EMPTY : exclusions;
    }

    public boolean isExcluded(ResourceLocation mobId) {
        return exclusions.isExcluded(mobId);
    }

    public boolean isExcluded(Mob mob) {
        return isExcluded(EntityType.getKey(mob.getType()));
    }

    /*
     * builds the derived structures (weighted collections, histogram tree) and adds the config to the indexes.
     * NOTE re-indexing a config is safe: the wildcards are already moved, and the tree is rebuilt the same.
     */
    private void index(Indexes idx, EchelonConfigsHolder.Config echelonConfig) {
        if (ObjectUtils.isEmpty(echelonConfig.getEchelons())) {
            return;
        }
        idx.INDEXED.add(echelonConfig);

        // add to map. NOTE replace is applied before indexing, see resolveReplacements()
        if (StringUtils.isNotBlank(echelonConfig.getId())) {
            idx.CONFIGS_BY_ID.put(echelonConfig.getId(), echelonConfig);
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
                    if (!idx.CONFIGS_BY_MOD.containsKey(keyPair)) {
                        idx.CONFIGS_BY_MOD.put(keyPair, echelonConfig);
                    }
                });
            }

            if (!echelonConfig.getMobWhitelist().isEmpty()) {
                echelonConfig.getMobWhitelist().forEach(mob -> {
                    // create a key pair
                    Pair<ResourceLocation, ResourceLocation> keyPair = new ImmutablePair<>(dimensionKey, new ResourceLocation(mob));
                    if (!idx.CONFIGS_BY_MOB.containsKey(keyPair)) {
                        idx.CONFIGS_BY_MOB.put(keyPair, echelonConfig);
                    }
                });
            }

            // register ALL configs in the CONFIGS registry regardless if categorized and registered elsewhere
            idx.CONFIGS.put(dimensionKey, echelonConfig);
        });
    }

    /**
     * @param mob
     * @return
     */
    public Optional<EchelonConfigsHolder.Config> getEchelonConfig(Mob mob) {
        return getEchelonConfig(mob.level().dimension().location(), EntityType.getKey(mob.getType()));
    }

    /**
     * @param dimension
     * @param mobId
     * @return
     */
    public Optional<EchelonConfigsHolder.Config> getEchelonConfig(ResourceLocation dimension, ResourceLocation mobId) {
        // an excluded mob is blacklisted from every config, regardless of whitelists
        if (isExcluded(mobId)) {
            return Optional.empty();
        }
        // read the indexes once, so the whole lookup sees one version of the registry
        Indexes idx = indexes;
        Pair<ResourceLocation, ResourceLocation> keyPair = new ImmutablePair<>(dimension, mobId);
        if (idx.CONFIGS_BY_MOB.containsKey(keyPair)) {
            return Optional.of(idx.CONFIGS_BY_MOB.get(keyPair));
        } else {
            keyPair = new ImmutablePair<>(ALL_DIMENSION, mobId);
            if (idx.CONFIGS_BY_MOB.containsKey(keyPair)) {
                return Optional.of(idx.CONFIGS_BY_MOB.get(keyPair));
            } else {
                // create a key pair for the mod
                Pair<ResourceLocation, String> modKeyPair = new ImmutablePair<>(dimension, keyPair.getValue().getNamespace());
                // check by mod (ie whitelisted mods)
                if (idx.CONFIGS_BY_MOD.containsKey(modKeyPair)) {
                    EchelonConfigsHolder.Config config = idx.CONFIGS_BY_MOD.get(modKeyPair);
                    if (config.getMobBlacklist().contains(mobId.toString())) {
                        return Optional.empty();
                    }
                    return Optional.of(config);
                } else {
                    modKeyPair = new ImmutablePair<>(ALL_DIMENSION, keyPair.getValue().getNamespace());
                    if (idx.CONFIGS_BY_MOD.containsKey(modKeyPair)) {
                        EchelonConfigsHolder.Config config = idx.CONFIGS_BY_MOD.get(modKeyPair);
                        if (config.getMobBlacklist().contains(mobId.toString())) {
                            return Optional.empty();
                        }
                        return Optional.of(config);
                    }
                }

                // search all remaining configs
                Optional<EchelonConfigsHolder.Config> echelon = searchEchelonsForMob(idx, dimension, mobId);
                if (echelon.isEmpty()) {
                    echelon = searchEchelonsForMob(idx, ALL_DIMENSION, mobId);
                }
                return echelon;
            }
        }
    }

    public Optional<EchelonConfigsHolder.Config> searchEchelonsForMob(ResourceLocation dimension, Mob entity) {
        return searchEchelonsForMob(dimension, EntityType.getKey(entity.getType()));
    }

    public Optional<EchelonConfigsHolder.Config> searchEchelonsForMob(ResourceLocation dimension, ResourceLocation mob) {
        return searchEchelonsForMob(indexes, dimension, mob);
    }

    private Optional<EchelonConfigsHolder.Config> searchEchelonsForMob(Indexes idx, ResourceLocation dimension, ResourceLocation mob) {
        Optional<EchelonConfigsHolder.Config> echelon = Optional.empty();
        if (isExcluded(mob)) {
            return echelon;
        }

        // for each config in a given dimension
        for (EchelonConfigsHolder.Config e : idx.CONFIGS.get(dimension)) {
            // find the first valid echelon - ie in the mod whitelist and not in the blacklists
            if (!e.getModWhitelist().isEmpty()) {
                if (e.getModWhitelist().contains(mob.getNamespace())) {
                    if (e.getMobBlacklist().contains(mob.toString())) {
                        return Optional.empty();
                    }
                }
                // if whitelist is not empty, and the mob's namespace is not included, this config doesn't apply - try the next
                else {
                    continue;
                }
            }
            // a config with only a mob whitelist applies to those mobs only (found by CONFIGS_BY_MOB), not to every other mob
            else if (!e.getMobWhitelist().isEmpty()) {
                continue;
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
