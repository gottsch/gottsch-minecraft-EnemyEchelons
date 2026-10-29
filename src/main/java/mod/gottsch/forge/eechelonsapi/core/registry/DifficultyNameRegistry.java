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

import com.google.common.collect.Maps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * NOTE this is a registry of DifficultyNameRegistryEntrys.
 * @author by Mark Gottschling on 5/28/2025
 */
public class DifficultyNameRegistry {
    private static final ResourceLocation ALL_MOBS = new ResourceLocation(".", ".");

    // ie Map<ID, Entry>. linked so that searches follow registration order.
    private static Map<ResourceLocation, DifficultyNameRegistryEntry> REGISTRY = Maps.newLinkedHashMap();

    private static Map<ResourceLocation, DifficultyNameRegistryEntry> BY_MOB = Maps.newHashMap();

    private static Map<String, DifficultyNameRegistryEntry> BY_MOD = Maps.newHashMap();

    /*
     * the two sources of entries are kept apart so that a datapack reload never wipes the entries
     * registered through register() (ie. toml or code), and vice versa.
     */
    private static final List<DifficultyNameRegistryEntry> REGISTERED = new ArrayList<>();
    private static final Map<ResourceLocation, DifficultyNameRegistryEntry> REGISTERED_BY_KEY = Maps.newLinkedHashMap();
    private static final List<DifficultyNameRegistryEntry> DATA_ENTRIES = new ArrayList<>();

    private static final Comparator<DifficultyNameRegistryEntry> LOAD_ORDER = Comparator
            .comparingInt((DifficultyNameRegistryEntry e) -> e.getLoadOrder())
            .thenComparing(e -> e.getId() == null ? "" : e.getId().toString());

    // NOTE left as a no-op, as in 1.0.x
    public static void clear() {

    }

    public static synchronized void register(List<DifficultyNameRegistryEntry> entries) {
        if (ObjectUtils.isEmpty(entries)) {
            return;
        }
        REGISTERED.addAll(entries);
        if (DATA_ENTRIES.isEmpty()) {
            index(entries);
        } else {
            rebuild();
        }
    }

    /**
     * replaces the datapack entries and rebuilds the registry.
     * if there are no datapack entries before or after, nothing is touched.
     * @param entries
     */
    public static synchronized void setDataEntries(List<DifficultyNameRegistryEntry> entries) {
        boolean hadData = !DATA_ENTRIES.isEmpty();
        DATA_ENTRIES.clear();
        if (entries != null) {
            DATA_ENTRIES.addAll(entries);
        }
        if (hadData || !DATA_ENTRIES.isEmpty()) {
            rebuild();
        }
    }

    /**
     * @return the registered entries, in registry order. ie. first is highest precedence.
     */
    public static synchronized List<DifficultyNameRegistryEntry> getEntries() {
        return List.copyOf(REGISTRY.values());
    }

    /*
     * without datapack entries, the registered entries are indexed in registration order (as in 1.0.x).
     * with datapack entries, all entries are indexed by (loadOrder, id).
     */
    private static void rebuild() {
        REGISTRY.clear();
        BY_MOB.clear();
        BY_MOD.clear();

        List<DifficultyNameRegistryEntry> entries = new ArrayList<>(REGISTERED);
        if (!DATA_ENTRIES.isEmpty()) {
            entries.addAll(DATA_ENTRIES);
            // stable sort, so equal keys keep their registration order
            entries.sort(LOAD_ORDER);
        }
        index(entries);
        REGISTERED_BY_KEY.forEach(REGISTRY::put);
    }

    private static void index(List<DifficultyNameRegistryEntry> entries) {
        entries.forEach(entry -> {

            // register all entries by id
            REGISTRY.put(entry.getId(), entry);

            // scan the mob white/black list to see if there are any wildcards and move to mod lists.
            Predicate<ResourceLocation> isWildcard = mob -> mob.getPath().equals("*");
            entry.getMobWhitelist().stream().filter(isWildcard)
                    .forEach(mob -> {
                        entry.getModWhitelist().add(mob.getNamespace());
                    });
            entry.getMobBlacklist().stream().filter(isWildcard)
                    .forEach(mob -> {
                        entry.getModBlacklist().add(mob.getNamespace());
                    });
            entry.getMobWhitelist().removeIf(isWildcard);
            entry.getMobBlacklist().removeIf(isWildcard);


            // by MOD
            if (!entry.getModWhitelist().isEmpty()) {
                entry.getModWhitelist().forEach(modNamespace -> {
                    if (modNamespace.equals(".") || modNamespace.equals(".:.")
                    || modNamespace.equals("*") || modNamespace.equals("*:*")) {
                        if (!BY_MOD.containsKey("*:*")) {
                            BY_MOD.put(ALL_MOBS.toString(), entry);
                        }
                    }
                    // if not already registered, then register it
                    else if (!BY_MOD.containsKey(modNamespace.toLowerCase())) {
                        BY_MOD.put(modNamespace.toLowerCase(), entry);
                    }
                });
            }

            // by MOB
            if (!entry.getMobWhitelist().isEmpty()) {
                entry.getMobWhitelist().forEach(mobId -> {
                    // create a key pair
                    if (!BY_MOB.containsKey(mobId)) {
                        BY_MOB.put(mobId, entry);
                    }
                });
            }

        });

    }

    public static Optional<DifficultyNameRegistryEntry> getDifficultyEntry(Mob mob) {
        ResourceLocation mobId =  EntityType.getKey(mob.getType());
        if (BY_MOB.containsKey(mobId)) {
            return Optional.of(BY_MOB.get(mobId));
        }
        else {
            // search all remaining configs
            return searchForMob(mob.level().dimension().location(), mob);
        }
    }

    public static Optional<DifficultyNameRegistryEntry> searchForMob(ResourceLocation dimension, Mob entity) {
        ResourceLocation mob = EntityType.getKey(entity.getType());

        for (DifficultyNameRegistryEntry e : REGISTRY.values()) {
            // find the first valid config - ie in the mod whitelist and not in the blacklists

            // mod whitelist contains a wildcard
            if(!e.getModWhitelist().isEmpty()
                    && (e.getModWhitelist().contains(ALL_MOBS.toString()) || e.getModWhitelist().contains(mob.getNamespace()))) {

                if (e.getModBlacklist().contains(mob.getNamespace())) {
                    return Optional.empty();
                }
                if (e.getMobBlacklist().contains(mob)) {
                    return Optional.empty();
                }

                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    public static synchronized Optional<DifficultyNameRegistryEntry> register(ResourceLocation key, DifficultyNameRegistryEntry entry) {
        REGISTERED_BY_KEY.put(key, entry);
        return Optional.ofNullable(REGISTRY.put(key, entry));
    }

    public static Optional<DifficultyNameRegistryEntry> getById(ResourceLocation key) {
        return Optional.ofNullable(REGISTRY.get(key));
    }

    public static Optional<String> getDifficultyName(Mob mob, Integer difficulty) {
        Optional<DifficultyNameRegistryEntry> entry = getDifficultyEntry(mob);
        if (entry.isPresent()) {
            return entry.get().getDifficultyName(difficulty);
        }
        return Optional.empty();
    }
}
