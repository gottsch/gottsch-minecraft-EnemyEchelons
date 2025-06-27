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
import mod.gottsch.forge.eechelonsapi.EEchelonsApiMod;
import mod.gottsch.forge.eechelonsapi.core.config.NameConfigsHolder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Very similar to the NameConfigsHolder.Config class
 * @author by Mark Gottschling on 5/29/2025
 */
public class DifficultyNameRegistryEntry {
    private ResourceLocation id;
    // these are only the namespace of the mod, so they are Strings and not ResourceLocations
    private List<String> modWhitelist;
    private List<String> modBlacklist;

    private List<ResourceLocation> mobWhitelist;
    private List<ResourceLocation> mobBlacklist;

    // ie Map<Difficulty, Name>
    private Map<Integer, String> difficultyNames;

    public DifficultyNameRegistryEntry() {

    }

    public DifficultyNameRegistryEntry(NameConfigsHolder.NameConfig config) {
        if (config.getId() == null) {
            EEchelonsApiMod.LOGGER.warn("Skipping a Difficulty Naming Config is missing or has an invalid id -> {}", config.getId());
            return;
        }

        this.id = new ResourceLocation(config.getId());
        config.getMobBlacklist().forEach(s -> this.getMobBlacklist().add(new ResourceLocation(s)));
        config.getMobWhitelist().forEach(s -> this.getMobWhitelist().add(new ResourceLocation(s)));
        if (!config.getModBlacklist().isEmpty()) {
            getModBlacklist().addAll(config.getModBlacklist());
        }
        if (!config.getModWhitelist().isEmpty()) {
            getModWhitelist().addAll(config.getModWhitelist());
        }
        config.getDifficultyNames().forEach(n -> {
            this.getDifficultyNames().put(n.getDifficulty(), n.getName());
        });

    }

    public Optional<String> getDifficultyName(Integer difficulty) {
        return Optional.ofNullable(difficultyNames.get(difficulty));
    }

    public void registerDifficultyName(Integer difficulty, String name) {
        this.difficultyNames.put(difficulty, name);
    }

    public Map<Integer, String> getDifficultyNames() {
        if (difficultyNames == null) {
            difficultyNames = Maps.newHashMap();
        }
        return difficultyNames;
    }

    public void setDifficultyNames(Map<Integer, String> difficultyNames) {
        this.difficultyNames = difficultyNames;
    }

    public ResourceLocation getId() {
        return id;
    }

    public void setId(ResourceLocation id) {
        this.id = id;
    }

    public List<ResourceLocation> getMobBlacklist() {
        if (mobBlacklist == null) {
            mobBlacklist = new ArrayList<>();
        }
        return mobBlacklist;
    }

    public void setMobBlacklist(List<ResourceLocation> mobBlacklist) {
        this.mobBlacklist = mobBlacklist;
    }

    public List<ResourceLocation> getMobWhitelist() {
        if (mobWhitelist == null) {
            mobWhitelist = new ArrayList<>();
        }
        return mobWhitelist;
    }

    public void setMobWhitelist(List<ResourceLocation> mobWhitelist) {
        this.mobWhitelist = mobWhitelist;
    }

    public List<String> getModBlacklist() {
        if (modBlacklist == null) {
            modBlacklist = new ArrayList<>();
        }
        return modBlacklist;
    }

    public void setModBlacklist(List<String> modBlacklist) {
        this.modBlacklist = modBlacklist;
    }

    public List<String> getModWhitelist() {
        if (modWhitelist == null) {
            modWhitelist = new ArrayList<>();
        }
        return modWhitelist;
    }

    public void setModWhitelist(List<String> modWhitelist) {
        this.modWhitelist = modWhitelist;
    }
}
