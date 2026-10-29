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
import mod.gottsch.forge.eechelonsapi.core.command.RegistryDump;
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
import java.util.concurrent.atomic.AtomicBoolean;

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

    private static EchelonConfigsHolder.Config replacing(EchelonConfigsHolder.Config config) {
        config.setReplace(true);
        return config;
    }

    @Test
    public void replaceTakesThePlaceOfTheEarlierConfigWithTheSameId() {
        EchelonRegistry registry = new EchelonRegistry();
        EchelonConfigsHolder.Config first = tomlConfig("toml:x", null, List.of("minecraft:overworld"), List.of("minecraft"), List.of());
        registry.register(first);
        registry.register(tomlConfig("toml:y", null, List.of("."), List.of(), List.of()));

        EchelonConfigsHolder.Config second = replacing(tomlConfig("toml:x", null, List.of("minecraft:overworld"), List.of("minecraft"), List.of()));
        registry.register(second);
        // the replacement takes the earlier config's place, and so its precedence
        assertEquals(List.of("toml:x", "toml:y"), ids(registry));
        assertSame(second, registry.getConfigs().get(0));
        assertSame(second, registry.getEchelonConfig(OVERWORLD, ZOMBIE).orElse(null));

        // without replace, configs with the same id are all kept, and the first wins the lookups (as in 1.0.x)
        EchelonConfigsHolder.Config third = tomlConfig("toml:x", null, List.of("minecraft:overworld"), List.of("minecraft"), List.of());
        registry.register(List.of(third));
        assertEquals(List.of("toml:x", "toml:y", "toml:x"), ids(registry));
        assertSame(second, registry.getEchelonConfig(OVERWORLD, ZOMBIE).orElse(null));

        // a replacement replaces every earlier config with its id, ie. re-registering a toml file without clear()
        EchelonConfigsHolder.Config fourth = replacing(tomlConfig("toml:x", null, List.of("minecraft:overworld"), List.of("minecraft"), List.of()));
        registry.register(List.of(fourth));
        assertEquals(List.of("toml:x", "toml:y"), ids(registry));
        assertSame(fourth, registry.getEchelonConfig(OVERWORLD, ZOMBIE).orElse(null));
    }

    @Test
    public void dataConfigCanReplaceATomlConfig() {
        EchelonRegistry registry = new EchelonRegistry();
        EchelonConfigsHolder.Config toml = replacing(tomlConfig("eechelons:default", 0, List.of("minecraft:overworld"), List.of("minecraft"), List.of()));
        registry.register(toml);
        registry.register(tomlConfig("eechelons:catch_all", 0, List.of("."), List.of(), List.of()));

        // same (load_order, id) sorts the data config after the toml one, so it is the later one and replaces it
        EchelonConfigsHolder.Config data = dataConfig("eechelons:default",
                "{\"load_order\": 0, \"replace\": true, \"dimensions\": [\"minecraft:overworld\"], \"mod_whitelist\": [\"minecraft\"], " + echelons(2) + "}");
        registry.setDataConfigs(List.of(data));
        assertEquals(List.of("eechelons:catch_all", "eechelons:default"), ids(registry));
        assertSame(data, registry.getEchelonConfig(OVERWORLD, ZOMBIE).orElse(null));

        List<String> dump = RegistryDump.echelons(registry);
        assertTrue(dump.stream().anyMatch(l -> l.contains("eechelons:default [data]") && l.contains("load_order=0") && l.contains("replace")), dump.toString());
        assertTrue(dump.stream().anyMatch(l -> l.contains("- eechelons:default [toml] replaced by eechelons:default [data]")), dump.toString());

        // a data config that sorts first is the earlier one, so the toml config (replace = true) replaces it
        registry.setDataConfigs(List.of(dataConfig("eechelons:default", "{\"load_order\": -1, " + echelons(2) + "}")));
        assertSame(toml, registry.getEchelonConfig(OVERWORLD, ZOMBIE).orElse(null));

        // and without the data, the toml config is back
        registry.setDataConfigs(List.of());
        assertSame(toml, registry.getEchelonConfig(OVERWORLD, ZOMBIE).orElse(null));
        assertEquals(List.of("eechelons:default", "eechelons:catch_all"), ids(registry));
    }

    @Test
    public void dumpListsConfigsInOrderWithSourcesAndExclusions() {
        EchelonRegistry registry = new EchelonRegistry();
        EchelonConfigsHolder.Config toml = tomlConfig("toml:catch_all", null, List.of("."), List.of(), List.of());
        toml.setMobBlacklist(new ArrayList<>(List.of("minecraft:a", "minecraft:b", "minecraft:c", "minecraft:d", "minecraft:e", "minecraft:f", "minecraft:g", "minecraft:h")));
        registry.register(toml);
        registry.setDataConfigs(List.of(dataConfig("eechelonstest:deep", "{\"load_order\": 1, " + echelons(1) + "}")));
        registry.setExclusions(new EchelonExclusions(Set.of("dungeons2"), Set.of()));

        List<String> dump = RegistryDump.echelons(registry);
        assertEquals("Echelon configs, in order of precedence (2):", dump.get(0));
        assertTrue(dump.get(1).startsWith("  1. eechelonstest:deep [data] load_order=1"), dump.get(1));
        assertTrue(dump.get(2).startsWith("  2. toml:catch_all [toml] load_order=99"), dump.get(2));
        // long lists are cut short
        assertTrue(dump.get(2).contains("mob_blacklist=[minecraft:a, minecraft:b, minecraft:c, minecraft:d, minecraft:e, minecraft:f, ... +2]"), dump.get(2));
        assertEquals("Exclusions: mods=[dungeons2] mobs=[]", dump.get(dump.size() - 1));

        DifficultyNameRegistryEntry names = new DifficultyNameRegistryEntry();
        names.setId(new ResourceLocation("toml", "dump_names"));
        names.registerDifficultyName(2, "Captain");
        names.registerDifficultyName(1, "Brute");
        List<String> nameDump = RegistryDump.names(List.of(names), List.of());
        assertTrue(nameDump.get(1).startsWith("  1. toml:dump_names [toml] load_order=99"), nameDump.get(1));
        assertTrue(nameDump.get(1).endsWith("names={1=Brute, 2=Captain}"), nameDump.get(1));
    }

    @Test
    public void lookupsNeverSeeAHalfBuiltRegistry() throws InterruptedException {
        EchelonRegistry registry = new EchelonRegistry();
        registry.register(tomlConfig("toml:catch_all", null, List.of("."), List.of(), List.of()));
        List<EchelonConfigsHolder.Config> data = List.of(dataConfig("eechelonstest:other", "{\"mod_whitelist\": [\"othermod\"], " + echelons(1) + "}"));

        // rebuild over and over on another thread, ie. a reload or a toml re-registration
        AtomicBoolean done = new AtomicBoolean();
        Thread reloader = new Thread(() -> {
            while (!done.get()) {
                registry.setDataConfigs(data);
                registry.setDataConfigs(List.of());
            }
        });
        reloader.start();
        try {
            for (int i = 0; i < 20000; i++) {
                assertTrue(registry.getEchelonConfig(OVERWORLD, ZOMBIE).isPresent(), "lookup " + i + " saw an empty registry");
            }
        } finally {
            done.set(true);
            reloader.join();
        }
    }

    @Test
    public void nameConfigMobWildcardsMoveToTheModLists() {
        NameConfigsHolder.NameConfig config = new NameConfigsHolder.NameConfig();
        config.setId("eechelonstest:wildcards");
        config.setMobWhitelist(new ArrayList<>(List.of("minecraft:*", "minecraft:zombie", "Not A Mob")));
        config.setMobBlacklist(new ArrayList<>(List.of("othermod:*")));
        config.setDifficultyNames(new ArrayList<>(List.of(new NameConfigsHolder.DifficultyName(1, "Brute"))));

        // used to throw on "minecraft:*"
        DifficultyNameRegistryEntry entry = new DifficultyNameRegistryEntry(config);
        assertEquals(List.of("minecraft"), entry.getModWhitelist());
        assertEquals(List.of("othermod"), entry.getModBlacklist());
        // the invalid id is skipped
        assertEquals(List.of(ZOMBIE), entry.getMobWhitelist());
        assertTrue(entry.getMobBlacklist().isEmpty());

        // and the datapack form accepts them too
        EchelonDataLoader.Result<NameConfigsHolder.NameConfig> result = EchelonDataLoader.loadNameConfigs(Map.of(
                new ResourceLocation("eechelonstest", "wildcards"), JsonParser.parseString(
                        "{\"mob_whitelist\": [\"minecraft:*\"], \"difficulty_names\": [{\"difficulty\": 1, \"name\": \"Brute\"}]}")));
        assertTrue(result.getErrors().isEmpty(), result.getErrors().toString());
        assertEquals(List.of("minecraft"), new DifficultyNameRegistryEntry(result.getValues().get(0)).getModWhitelist());
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
