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
package mod.gottsch.forge.eechelonsapi.core.data;

import com.google.gson.JsonElement;
import mod.gottsch.forge.eechelonsapi.EEchelonsApiMod;
import mod.gottsch.forge.eechelonsapi.core.config.EchelonConfigsHolder;
import mod.gottsch.forge.eechelonsapi.core.config.EchelonExclusions;
import mod.gottsch.forge.eechelonsapi.core.config.NameConfigsHolder;
import mod.gottsch.forge.eechelonsapi.core.registry.DifficultyNameRegistry;
import mod.gottsch.forge.eechelonsapi.core.registry.DifficultyNameRegistryEntry;
import mod.gottsch.forge.eechelonsapi.core.registry.EchelonRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

/**
 * Decodes the datapack json files and hands the results to the registries.
 * A file that fails to decode is logged (naming the file) and skipped; the remaining files still load.
 * @author Mark Gottschling on 10/29/2026
 */
public final class EchelonDataLoader {
    public static final String ECHELONS_DIRECTORY = "echelons";
    public static final String EXCLUSIONS_DIRECTORY = "echelons/exclusions";
    public static final String DIFFICULTY_NAMES_DIRECTORY = "difficulty_names";

    // the echelons listener also sees the files of the exclusions sub-folder
    private static final String EXCLUSIONS_PREFIX = "exclusions/";

    private EchelonDataLoader() {}

    /**
     * the decoded values and the errors of the files that were skipped.
     * @param <T>
     */
    public static class Result<T> {
        private final List<T> values = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        public List<T> getValues() {
            return values;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    public static Result<EchelonConfigsHolder.Config> loadEchelonConfigs(Map<ResourceLocation, JsonElement> files) {
        Map<ResourceLocation, JsonElement> configs = new TreeMap<>();
        files.forEach((id, json) -> {
            if (!id.getPath().startsWith(EXCLUSIONS_PREFIX)) {
                configs.put(id, json);
            }
        });
        return load(ECHELONS_DIRECTORY, configs, EchelonDataCodecs::decodeEchelonConfig);
    }

    public static Result<NameConfigsHolder.NameConfig> loadNameConfigs(Map<ResourceLocation, JsonElement> files) {
        return load(DIFFICULTY_NAMES_DIRECTORY, files, EchelonDataCodecs::decodeNameConfig);
    }

    public static Result<EchelonExclusions> loadExclusions(Map<ResourceLocation, JsonElement> files) {
        return load(EXCLUSIONS_DIRECTORY, files, (id, json) -> EchelonDataCodecs.decodeExclusions(json));
    }

    private static <T> Result<T> load(String directory, Map<ResourceLocation, JsonElement> files, BiFunction<ResourceLocation, JsonElement, T> decoder) {
        Result<T> result = new Result<>();
        // sorted, so that the load (and the log) is the same every time
        new TreeMap<>(files).forEach((id, json) -> {
            try {
                result.getValues().add(decoder.apply(id, json));
            } catch (RuntimeException e) {
                result.getErrors().add("Skipping " + filePath(directory, id) + ": " + e.getMessage());
            }
        });
        return result;
    }

    /**
     * @param directory
     * @param id
     * @return the path of the file within a datapack, ex. data/mymod/echelons/deep.json
     */
    public static String filePath(String directory, ResourceLocation id) {
        return "data/" + id.getNamespace() + "/" + directory + "/" + id.getPath() + ".json";
    }

    /*
     * apply to registries
     */

    public static void applyEchelonConfigs(EchelonRegistry registry, Map<ResourceLocation, JsonElement> files) {
        Result<EchelonConfigsHolder.Config> result = loadEchelonConfigs(files);
        result.getErrors().forEach(EEchelonsApiMod.LOGGER::error);
        registry.setDataConfigs(result.getValues());
        EEchelonsApiMod.LOGGER.info("loaded {} echelon config(s) from data", result.getValues().size());
    }

    public static void applyExclusions(EchelonRegistry registry, Map<ResourceLocation, JsonElement> files) {
        Result<EchelonExclusions> result = loadExclusions(files);
        result.getErrors().forEach(EEchelonsApiMod.LOGGER::error);
        EchelonExclusions exclusions = EchelonExclusions.merge(result.getValues());
        registry.setExclusions(exclusions);
        EEchelonsApiMod.LOGGER.info("loaded echelon exclusions from data -> {}", exclusions);
    }

    public static void applyNameConfigs(Map<ResourceLocation, JsonElement> files) {
        Result<NameConfigsHolder.NameConfig> result = loadNameConfigs(files);
        result.getErrors().forEach(EEchelonsApiMod.LOGGER::error);
        DifficultyNameRegistry.setDataEntries(result.getValues().stream().map(DifficultyNameRegistryEntry::new).toList());
        EEchelonsApiMod.LOGGER.info("loaded {} difficulty name config(s) from data", result.getValues().size());
    }
}
