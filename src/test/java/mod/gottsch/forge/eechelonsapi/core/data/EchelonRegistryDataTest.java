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

import com.google.gson.JsonParser;
import mod.gottsch.forge.eechelonsapi.core.config.EchelonConfigsHolder;
import mod.gottsch.forge.eechelonsapi.core.config.EchelonExclusions;
import mod.gottsch.forge.eechelonsapi.core.config.NameConfigsHolder;
import mod.gottsch.forge.eechelonsapi.core.registry.DifficultyNameRegistry;
import mod.gottsch.forge.eechelonsapi.core.registry.DifficultyNameRegistryEntry;
import mod.gottsch.forge.eechelonsapi.core.registry.EchelonRegistry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Datapack configs are merged with the toml configs, sorted by (loadOrder, id), and exclusions win over whitelists.
 * @author Mark Gottschling on 10/29/2026
 */
public class EchelonRegistryDataTest {
    private static final ResourceLocation OVERWORLD = new ResourceLocation("minecraft", "overworld");
    private static final ResourceLocation ZOMBIE = new ResourceLocation("minecraft", "zombie");
    private static final ResourceLocation SKELETON = new ResourceLocation("dungeons2", "skeleton");
    private static final ResourceLocation BOSS = new ResourceLocation("dungeons2", "boss");

    /*
     * a toml (ie. code) config, built the way ObjectConverter builds it: plain setters, no derived structures.
     */
    private static EchelonConfigsHolder.Config tomlConfig(String id, Integer loadOrder, List<String> dimensions, List<String> modWhitelist, List<String> mobWhitelist) {
        EchelonConfigsHolder.Config config = new EchelonConfigsHolder.Config();
        config.setId(id);
        config.setLoadOrder(loadOrder);
        config.setDimensions(new ArrayList<>(dimensions));
        config.setModWhitelist(new ArrayList<>(modWhitelist));
        config.setMobWhitelist(new ArrayList<>(mobWhitelist));
        config.setHpFactor(0.1);
        EchelonConfigsHolder.DifficultyEntry entry = new EchelonConfigsHolder.DifficultyEntry();
        entry.setDifficulty(0);
        entry.setWeight(1.0);
        EchelonConfigsHolder.Echelon echelon = new EchelonConfigsHolder.Echelon();
        echelon.setMin(-64);
        echelon.setMax(319);
        echelon.setHistogram(new ArrayList<>(List.of(entry)));
        config.setEchelons(new ArrayList<>(List.of(echelon)));
        return config;
    }

    private static EchelonConfigsHolder.Config dataConfig(String id, String json) {
        return EchelonDataCodecs.decodeEchelonConfig(new ResourceLocation(id), JsonParser.parseString(json));
    }

    private static String echelons(int difficulty) {
        return "\"echelons\": [{\"min\": -64, \"max\": 319, \"histogram\": [{\"difficulty\": " + difficulty + ", \"weight\": 1.0}]}]";
    }

    private static List<String> ids(EchelonRegistry registry) {
        return registry.getConfigs().stream().map(EchelonConfigsHolder.Config::getId).toList();
    }

    @Test
    public void registryIsSortedByLoadOrderThenIdAndKeepsTomlAcrossReloads() {
        EchelonRegistry registry = new EchelonRegistry();
        registry.register(tomlConfig("toml:a", null, List.of("minecraft:overworld"), List.of("minecraft"), List.of()));
        registry.register(List.of(tomlConfig("toml:b", 1, List.of("."), List.of(), List.of())));

        // without data, toml configs keep their registration order (as in 1.0.x)
        assertEquals(List.of("toml:a", "toml:b"), ids(registry));

        registry.setDataConfigs(List.of(
                dataConfig("eechelonstest:z", "{\"load_order\": 0, " + echelons(1) + "}"),
                dataConfig("eechelonstest:y", "{" + echelons(2) + "}"),
                dataConfig("eechelonstest:x", "{\"load_order\": 1, " + echelons(3) + "}")));
        assertEquals(List.of("eechelonstest:z", "eechelonstest:x", "toml:b", "eechelonstest:y", "toml:a"), ids(registry));

        // a reload with different data keeps the toml configs
        registry.setDataConfigs(List.of(dataConfig("eechelonstest:w", "{\"load_order\": 50, " + echelons(1) + "}")));
        assertEquals(List.of("toml:b", "eechelonstest:w", "toml:a"), ids(registry));

        // a code registration while data is loaded is merged into the sorted order
        registry.register(tomlConfig("toml:c", 0, List.of("."), List.of(), List.of()));
        assertEquals(List.of("toml:c", "toml:b", "eechelonstest:w", "toml:a"), ids(registry));

        // a reload with no data leaves only the toml configs, in registration order
        registry.setDataConfigs(List.of());
        assertEquals(List.of("toml:a", "toml:b", "toml:c"), ids(registry));
        // toml lookups still work after all the rebuilds
        assertEquals("toml:a", registry.getEchelonConfig(OVERWORLD, ZOMBIE).map(EchelonConfigsHolder.Config::getId).orElse(null));

        // clear() removes the registered configs, but not the data
        registry.setDataConfigs(List.of(dataConfig("eechelonstest:w", "{" + echelons(1) + "}")));
        registry.clear();
        assertEquals(List.of("eechelonstest:w"), ids(registry));
    }

    @Test
    public void dataConfigsGetTheDerivedStructures() {
        EchelonRegistry registry = new EchelonRegistry();
        registry.setDataConfigs(List.of(dataConfig("eechelonstest:deep", "{\"dimensions\": [\"minecraft:overworld\"], "
                + "\"mob_whitelist\": [\"minecraft:zombie\"], \"mod_whitelist\": [\"dungeons2\"], "
                + "\"echelons\": [{\"min\": 0, \"max\": 319, \"histogram\": [{\"difficulty\": 0, \"weight\": 1.0}]}, "
                + "{\"min\": -64, \"max\": -1, \"histogram\": [{\"difficulty\": 4, \"weight\": 1.0}]}]}")));

        // found through CONFIGS_BY_MOB and CONFIGS_BY_MOD
        Optional<EchelonConfigsHolder.Config> byMob = registry.getEchelonConfig(OVERWORLD, ZOMBIE);
        Optional<EchelonConfigsHolder.Config> byMod = registry.getEchelonConfig(OVERWORLD, SKELETON);
        assertTrue(byMob.isPresent());
        assertTrue(byMod.isPresent());
        EchelonConfigsHolder.Config config = byMob.get();
        assertSame(config, byMod.get());

        // weighted collections and the histogram tree are built
        assertNotNull(config.getHistogram());
        config.getEchelons().forEach(e -> assertNotNull(e.getWeightedDifficulties()));
        assertEquals(0, config.getDifficulty(100));
        assertEquals(4, config.getDifficulty(-30));
    }

    @Test
    public void exclusionKeepsAMobOutOfAConfigThatWouldTakeIt() {
        EchelonRegistry registry = new EchelonRegistry();
        // toml catch-all, ie. like stronger mobs below's catch_all
        registry.register(tomlConfig("toml:catch_all", null, List.of("."), List.of(), List.of()));
        // a config that whitelists the mod and one that whitelists a mob of it, both sorted before the catch-all
        registry.setDataConfigs(List.of(
                dataConfig("eechelonstest:by_mod", "{\"load_order\": 0, \"mod_whitelist\": [\"dungeons2\"], " + echelons(1) + "}"),
                dataConfig("eechelonstest:by_mob", "{\"load_order\": 0, \"mob_whitelist\": [\"dungeons2:boss\"], " + echelons(1) + "}")));

        assertEquals("eechelonstest:by_mod", registry.getEchelonConfig(OVERWORLD, SKELETON).map(EchelonConfigsHolder.Config::getId).orElse(null));
        assertEquals("eechelonstest:by_mob", registry.getEchelonConfig(OVERWORLD, BOSS).map(EchelonConfigsHolder.Config::getId).orElse(null));
        assertEquals("toml:catch_all", registry.getEchelonConfig(OVERWORLD, ZOMBIE).map(EchelonConfigsHolder.Config::getId).orElse(null));

        EchelonDataLoader.Result<EchelonExclusions> result = EchelonDataLoader.loadExclusions(Map.of(
                new ResourceLocation("dungeons2", "dungeons2"), JsonParser.parseString("{\"mods\": [\"dungeons2\"]}")));
        assertTrue(result.getErrors().isEmpty(), result.getErrors().toString());
        registry.setExclusions(EchelonExclusions.merge(result.getValues()));

        // excluded from every config, whitelists included
        assertTrue(registry.isExcluded(SKELETON));
        assertTrue(registry.getEchelonConfig(OVERWORLD, SKELETON).isEmpty());
        assertTrue(registry.getEchelonConfig(OVERWORLD, BOSS).isEmpty());
        assertTrue(registry.searchEchelonsForMob(OVERWORLD, SKELETON).isEmpty());
        // other mobs are unaffected
        assertEquals("toml:catch_all", registry.getEchelonConfig(OVERWORLD, ZOMBIE).map(EchelonConfigsHolder.Config::getId).orElse(null));
        // a custom registry has no exclusions
        EchelonRegistry custom = new EchelonRegistry();
        custom.register(tomlConfig("custom:dungeon", null, List.of("."), List.of("dungeons2"), List.of()));
        assertEquals("custom:dungeon", custom.getEchelonConfig(OVERWORLD, SKELETON).map(EchelonConfigsHolder.Config::getId).orElse(null));

        // a single mob can be excluded too
        registry.setExclusions(new EchelonExclusions(Set.of(), Set.of(ZOMBIE)));
        assertTrue(registry.getEchelonConfig(OVERWORLD, ZOMBIE).isEmpty());
        assertTrue(registry.getEchelonConfig(OVERWORLD, SKELETON).isPresent());
    }

    @Test
    public void searchSkipsWhitelistingConfigsThatDontMatch() {
        EchelonRegistry registry = new EchelonRegistry();
        // in the same dimension: a mod whitelist, a mob whitelist, then a config for everything else
        registry.register(tomlConfig("toml:dungeons2", null, List.of("minecraft:overworld"), List.of("dungeons2"), List.of()));
        registry.register(tomlConfig("toml:creeper", null, List.of("minecraft:overworld"), List.of(), List.of("minecraft:creeper")));
        registry.register(tomlConfig("toml:overworld", null, List.of("minecraft:overworld"), List.of(), List.of()));
        registry.register(tomlConfig("toml:catch_all", null, List.of("."), List.of(), List.of()));

        // the whitelisting configs don't apply to a zombie, so the search goes on to the overworld config
        assertEquals("toml:overworld", registry.searchEchelonsForMob(OVERWORLD, ZOMBIE).map(EchelonConfigsHolder.Config::getId).orElse(null));
        assertEquals("toml:overworld", registry.getEchelonConfig(OVERWORLD, ZOMBIE).map(EchelonConfigsHolder.Config::getId).orElse(null));
        // and in another dimension, on to the catch-all
        ResourceLocation nether = new ResourceLocation("minecraft", "the_nether");
        assertEquals("toml:catch_all", registry.getEchelonConfig(nether, ZOMBIE).map(EchelonConfigsHolder.Config::getId).orElse(null));
        // the whitelisted mod and mob still get their own configs
        assertEquals("toml:dungeons2", registry.getEchelonConfig(OVERWORLD, SKELETON).map(EchelonConfigsHolder.Config::getId).orElse(null));
        assertEquals("toml:creeper", registry.getEchelonConfig(OVERWORLD, new ResourceLocation("minecraft", "creeper")).map(EchelonConfigsHolder.Config::getId).orElse(null));

        // a mob whitelist only config is never picked for other mobs, even when it comes first
        EchelonRegistry mobFirst = new EchelonRegistry();
        mobFirst.register(tomlConfig("toml:creeper", null, List.of("minecraft:overworld"), List.of(), List.of("minecraft:creeper")));
        mobFirst.register(tomlConfig("toml:catch_all", null, List.of("."), List.of(), List.of()));
        assertEquals("toml:catch_all", mobFirst.getEchelonConfig(OVERWORLD, ZOMBIE).map(EchelonConfigsHolder.Config::getId).orElse(null));

        // a mob blacklisted by the config that whitelists its mod still gets nothing (unchanged)
        EchelonRegistry blacklisted = new EchelonRegistry();
        EchelonConfigsHolder.Config vanilla = tomlConfig("toml:vanilla", null, List.of("minecraft:overworld"), List.of("minecraft"), List.of());
        vanilla.setMobBlacklist(new ArrayList<>(List.of("minecraft:cow")));
        blacklisted.register(vanilla);
        blacklisted.register(tomlConfig("toml:catch_all", null, List.of("."), List.of(), List.of()));
        assertTrue(blacklisted.getEchelonConfig(OVERWORLD, new ResourceLocation("minecraft", "cow")).isEmpty());
    }

    @Test
    public void difficultyNamesAreSortedAndKeepToml() {
        DifficultyNameRegistryEntry toml = new DifficultyNameRegistryEntry();
        toml.setId(new ResourceLocation("toml", "names"));
        DifficultyNameRegistry.register(List.of(toml));

        EchelonDataLoader.Result<NameConfigsHolder.NameConfig> result = EchelonDataLoader.loadNameConfigs(Map.of(
                new ResourceLocation("eechelonstest", "b"), JsonParser.parseString("{\"load_order\": 5, \"difficulty_names\": [{\"difficulty\": 1, \"name\": \"B\"}]}"),
                new ResourceLocation("eechelonstest", "a"), JsonParser.parseString("{\"load_order\": 5, \"difficulty_names\": [{\"difficulty\": 1, \"name\": \"A\"}]}"),
                new ResourceLocation("eechelonstest", "c"), JsonParser.parseString("{\"load_order\": 0, \"difficulty_names\": [{\"difficulty\": 1, \"name\": \"C\"}]}")));
        assertTrue(result.getErrors().isEmpty(), result.getErrors().toString());
        DifficultyNameRegistry.setDataEntries(result.getValues().stream().map(DifficultyNameRegistryEntry::new).toList());

        List<String> ids = DifficultyNameRegistry.getEntries().stream().map(e -> e.getId().toString())
                .filter(id -> id.startsWith("eechelonstest:") || id.equals("toml:names")).toList();
        assertEquals(List.of("eechelonstest:c", "eechelonstest:a", "eechelonstest:b", "toml:names"), ids);
        assertEquals(Optional.of("A"), DifficultyNameRegistry.getById(new ResourceLocation("eechelonstest", "a")).flatMap(e -> e.getDifficultyName(1)));

        // a reload without data keeps the toml entry
        DifficultyNameRegistry.setDataEntries(List.of());
        assertTrue(DifficultyNameRegistry.getById(new ResourceLocation("toml", "names")).isPresent());
        assertTrue(DifficultyNameRegistry.getById(new ResourceLocation("eechelonstest", "a")).isEmpty());
    }
}
