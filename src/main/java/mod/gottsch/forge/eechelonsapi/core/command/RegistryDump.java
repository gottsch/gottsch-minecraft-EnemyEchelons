/*
 * This file is part of  Enemy Echelons API.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.eechelonsapi.core.command;

import mod.gottsch.forge.eechelonsapi.core.config.EchelonConfigsHolder;
import mod.gottsch.forge.eechelonsapi.core.config.EchelonExclusions;
import mod.gottsch.forge.eechelonsapi.core.registry.DifficultyNameRegistryEntry;
import mod.gottsch.forge.eechelonsapi.core.registry.EchelonRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Describes the state of the registries, one line per item, for the dump command and the log.
 * ie. the configs in order of precedence with their source, the configs that were left out, the exclusions and the difficulty names.
 * @author Mark Gottschling on 10/29/2026
 */
public final class RegistryDump {
    // longer lists are cut short, ex. the default mob blacklist
    private static final int MAX_LIST = 6;

    private RegistryDump() {}

    public static List<String> echelons(EchelonRegistry registry) {
        List<String> lines = new ArrayList<>();
        Set<EchelonConfigsHolder.Config> data = identitySet(registry.getDataConfigs());
        List<EchelonConfigsHolder.Config> indexed = registry.getConfigs();
        Set<EchelonConfigsHolder.Config> indexedSet = identitySet(indexed);

        lines.add("Echelon configs, in order of precedence (" + indexed.size() + "):");
        for (int i = 0; i < indexed.size(); i++) {
            EchelonConfigsHolder.Config c = indexed.get(i);
            lines.add("  " + (i + 1) + ". " + c.getId() + " [" + source(c, data) + "]"
                    + " load_order=" + c.getLoadOrder()
                    + (Boolean.TRUE.equals(c.getReplace()) ? " replace" : "")
                    + " dimensions=" + list(c.getDimensions())
                    + " mod_whitelist=" + list(c.getModWhitelist())
                    + " mob_whitelist=" + list(c.getMobWhitelist())
                    + " mod_blacklist=" + list(c.getModBlacklist())
                    + " mob_blacklist=" + list(c.getMobBlacklist())
                    + " echelons=" + c.getEchelons().size());
        }

        // registered or loaded, but not in the registry
        List<EchelonConfigsHolder.Config> all = new ArrayList<>(registry.getRegisteredConfigs());
        all.addAll(registry.getDataConfigs());
        List<String> notIndexed = new ArrayList<>();
        for (EchelonConfigsHolder.Config c : all) {
            if (c == null || indexedSet.contains(c)) {
                continue;
            }
            String reason = indexed.stream()
                    .filter(i -> i.getId() != null && i.getId().equals(c.getId()) && Boolean.TRUE.equals(i.getReplace()))
                    .findFirst()
                    .map(i -> "replaced by " + i.getId() + " [" + source(i, data) + "]")
                    .orElse(c.getEchelons() == null || c.getEchelons().isEmpty() ? "no echelons" : "not indexed");
            notIndexed.add("  - " + c.getId() + " [" + source(c, data) + "] " + reason);
        }
        if (!notIndexed.isEmpty()) {
            lines.add("Left out (" + notIndexed.size() + "):");
            lines.addAll(notIndexed);
        }

        EchelonExclusions exclusions = registry.getExclusions();
        lines.add("Exclusions: mods=" + list(exclusions.getMods()) + " mobs=" + list(exclusions.getMobs()));
        return lines;
    }

    public static List<String> names(List<DifficultyNameRegistryEntry> entries, List<DifficultyNameRegistryEntry> dataEntries) {
        List<String> lines = new ArrayList<>();
        Set<DifficultyNameRegistryEntry> data = identitySet(dataEntries);
        lines.add("Difficulty names, in order of precedence (" + entries.size() + "):");
        for (int i = 0; i < entries.size(); i++) {
            DifficultyNameRegistryEntry e = entries.get(i);
            lines.add("  " + (i + 1) + ". " + e.getId() + " [" + (data.contains(e) ? "data" : "toml") + "]"
                    + " load_order=" + e.getLoadOrder()
                    + " mod_whitelist=" + list(e.getModWhitelist())
                    + " mob_whitelist=" + list(e.getMobWhitelist())
                    + " mod_blacklist=" + list(e.getModBlacklist())
                    + " mob_blacklist=" + list(e.getMobBlacklist())
                    + " names=" + new TreeMap<>(e.getDifficultyNames()));
        }
        return lines;
    }

    private static String source(EchelonConfigsHolder.Config config, Set<EchelonConfigsHolder.Config> data) {
        return data.contains(config) ? "data" : "toml";
    }

    private static String list(Collection<?> values) {
        if (values.size() <= MAX_LIST) {
            return values.toString();
        }
        return values.stream().limit(MAX_LIST).map(String::valueOf)
                .collect(Collectors.joining(", ", "[", ", ... +" + (values.size() - MAX_LIST) + "]"));
    }

    private static <T> Set<T> identitySet(Collection<T> values) {
        Set<T> set = Collections.newSetFromMap(new IdentityHashMap<>());
        set.addAll(values);
        return set;
    }
}
