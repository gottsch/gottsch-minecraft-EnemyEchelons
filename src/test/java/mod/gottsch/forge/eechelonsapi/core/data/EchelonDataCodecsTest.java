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

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.toml.TomlParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import mod.gottsch.forge.eechelonsapi.core.config.EchelonConfigsHolder;
import mod.gottsch.forge.eechelonsapi.core.config.EchelonExclusions;
import mod.gottsch.forge.eechelonsapi.core.config.NameConfigsHolder;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The datapack json decodes into the same values as the equivalent toml, and bad files are rejected by name.
 * @author Mark Gottschling on 10/29/2026
 */
public class EchelonDataCodecsTest {
    private static final ResourceLocation DEEP_DARK = new ResourceLocation("eechelonstest", "deep_dark");
    private static final ResourceLocation BRUTES = new ResourceLocation("eechelonstest", "brutes");

    static JsonElement json(String resource) {
        try (Reader reader = reader(resource)) {
            return JsonParser.parseReader(reader);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static JsonElement parse(String text) {
        return JsonParser.parseString(text);
    }

    static CommentedConfig toml(String resource) {
        try (Reader reader = reader(resource)) {
            return new TomlParser().parse(reader);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Reader reader(String resource) {
        InputStream stream = EchelonDataCodecsTest.class.getResourceAsStream(resource);
        assertNotNull(stream, "missing test resource " + resource);
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }

    /*
     * the same steps as Config.transformEchelonConfigs(), without touching the forge config spec
     */
    static List<EchelonConfigsHolder.Config> tomlEchelonConfigs(String resource) {
        EchelonConfigsHolder holder = new ObjectConverter().toObject(toml(resource), EchelonConfigsHolder::new);
        holder.applyLoadOrder();
        return holder.getConfigs();
    }

    @Test
    public void echelonConfigJsonMatchesToml() {
        EchelonConfigsHolder.Config fromJson = EchelonDataCodecs.decodeEchelonConfig(DEEP_DARK, json("/data/eechelonstest/echelons/deep_dark.json"));
        List<EchelonConfigsHolder.Config> tomlConfigs = tomlEchelonConfigs("/toml/deep_dark.toml");
        assertEquals(1, tomlConfigs.size());
        EchelonConfigsHolder.Config fromToml = tomlConfigs.get(0);

        assertEquals(fromToml.getId(), fromJson.getId());
        assertEquals(5, fromJson.getLoadOrder());
        assertEquals(fromToml.getLoadOrder(), fromJson.getLoadOrder());
        assertEquals(fromToml.getDimensions(), fromJson.getDimensions());
        assertEquals(fromToml.getHpFactor(), fromJson.getHpFactor());
        assertEquals(fromToml.getMaxHp(), fromJson.getMaxHp());
        assertEquals(fromToml.getDamageFactor(), fromJson.getDamageFactor());
        assertEquals(fromToml.getMaxDamage(), fromJson.getMaxDamage());
        assertEquals(fromToml.getKnockbackIncrement(), fromJson.getKnockbackIncrement());
        assertEquals(fromToml.getMaxKnockback(), fromJson.getMaxKnockback());
        assertEquals(fromToml.getKnockbackResistIncrement(), fromJson.getKnockbackResistIncrement());
        assertEquals(fromToml.getMaxKnockbackResist(), fromJson.getMaxKnockbackResist());
        assertEquals(fromToml.getArmorFactor(), fromJson.getArmorFactor());
        assertEquals(fromToml.getMaxArmor(), fromJson.getMaxArmor());
        assertEquals(fromToml.getArmorToughnessFactor(), fromJson.getArmorToughnessFactor());
        assertEquals(fromToml.getMaxArmorToughness(), fromJson.getMaxArmorToughness());
        assertEquals(fromToml.getSpeedFactor(), fromJson.getSpeedFactor());
        assertEquals(fromToml.getMaxSpeed(), fromJson.getMaxSpeed());
        assertEquals(fromToml.getFlyingSpeedFactor(), fromJson.getFlyingSpeedFactor());
        assertEquals(fromToml.getMaxFlyingSpeed(), fromJson.getMaxFlyingSpeed());
        assertEquals(fromToml.getXpFactor(), fromJson.getXpFactor());
        assertEquals(fromToml.getMaxXp(), fromJson.getMaxXp());
        assertEquals(fromToml.getReplace(), fromJson.getReplace());
        assertEquals(fromToml.getModWhitelist(), fromJson.getModWhitelist());
        assertEquals(fromToml.getModBlacklist(), fromJson.getModBlacklist());
        assertEquals(fromToml.getMobWhitelist(), fromJson.getMobWhitelist());
        assertEquals(fromToml.getMobBlacklist(), fromJson.getMobBlacklist());

        assertEquals(fromToml.getEchelons().size(), fromJson.getEchelons().size());
        for (int i = 0; i < fromToml.getEchelons().size(); i++) {
            EchelonConfigsHolder.Echelon expected = fromToml.getEchelons().get(i);
            EchelonConfigsHolder.Echelon actual = fromJson.getEchelons().get(i);
            assertEquals(expected.getId(), actual.getId());
            assertEquals(expected.getMin(), actual.getMin());
            assertEquals(expected.getMax(), actual.getMax());
            assertEquals(expected.getHistogram().size(), actual.getHistogram().size());
            for (int j = 0; j < expected.getHistogram().size(); j++) {
                assertEquals(expected.getHistogram().get(j).getDifficulty(), actual.getHistogram().get(j).getDifficulty());
                assertEquals(expected.getHistogram().get(j).getWeight(), actual.getHistogram().get(j).getWeight());
            }
        }
    }

    @Test
    public void nameConfigJsonMatchesToml() {
        NameConfigsHolder.NameConfig fromJson = EchelonDataCodecs.decodeNameConfig(BRUTES, json("/data/eechelonstest/difficulty_names/brutes.json"));
        NameConfigsHolder holder = new ObjectConverter().toObject(toml("/toml/brutes.toml"), NameConfigsHolder::new);
        assertEquals(1, holder.getNameConfigs().size());
        NameConfigsHolder.NameConfig fromToml = holder.getNameConfigs().get(0);

        assertEquals(fromToml.getId(), fromJson.getId());
        assertEquals(10, fromJson.getLoadOrder());
        assertEquals(fromToml.getLoadOrder(), fromJson.getLoadOrder());
        assertEquals(fromToml.getModWhitelist(), fromJson.getModWhitelist());
        assertEquals(fromToml.getModBlacklist(), fromJson.getModBlacklist());
        assertEquals(fromToml.getMobWhitelist(), fromJson.getMobWhitelist());
        assertEquals(fromToml.getMobBlacklist(), fromJson.getMobBlacklist());
        assertEquals(fromToml.getDifficultyNames().size(), fromJson.getDifficultyNames().size());
        for (int i = 0; i < fromToml.getDifficultyNames().size(); i++) {
            assertEquals(fromToml.getDifficultyNames().get(i).getDifficulty(), fromJson.getDifficultyNames().get(i).getDifficulty());
            assertEquals(fromToml.getDifficultyNames().get(i).getName(), fromJson.getDifficultyNames().get(i).getName());
        }
    }

    @Test
    public void exclusionsDecode() {
        EchelonExclusions exclusions = EchelonDataCodecs.decodeExclusions(json("/data/eechelonstest/echelons/exclusions/eechelonstest.json"));
        // the mob wildcard moves to the mods
        assertEquals(Set.of("dungeons2", "othermod"), exclusions.getMods());
        assertEquals(Set.of(new ResourceLocation("minecraft", "warden")), exclusions.getMobs());
    }

    @Test
    public void loadOrderDefaultsTo99() {
        EchelonConfigsHolder.Config config = EchelonDataCodecs.decodeEchelonConfig(DEEP_DARK, parse(
                "{\"echelons\": [{\"min\": 0, \"max\": 10, \"histogram\": [{\"difficulty\": 1, \"weight\": 1.0}]}]}"));
        assertEquals(99, config.getLoadOrder());
        // an absent factor keeps the class default and an absent cap stays unset
        assertEquals(0.0, config.getHpFactor());
        assertNull(config.getMaxHp());
    }

    @Test
    public void unknownKeyIsAnErrorNamingTheFile() {
        ResourceLocation bad = new ResourceLocation("eechelonstest", "bad_key");
        Result result = load(Map.of(
                DEEP_DARK, json("/data/eechelonstest/echelons/deep_dark.json"),
                bad, parse("{\"hp_factr\": 0.2, \"echelons\": [{\"min\": 0, \"max\": 10, \"histogram\": [{\"difficulty\": 1, \"weight\": 1.0}]}]}")));

        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).contains("data/eechelonstest/echelons/bad_key.json"), result.errors().get(0));
        assertTrue(result.errors().get(0).contains("hp_factr"), result.errors().get(0));
        // the good file still loads, the bad file is skipped
        assertEquals(List.of(DEEP_DARK.toString()), result.ids());
    }

    @Test
    public void nestedUnknownKeyIsAnError() {
        ResourceLocation bad = new ResourceLocation("eechelonstest", "nested");
        Result result = load(Map.of(bad, parse(
                "{\"echelons\": [{\"min\": 0, \"max\": 10, \"histogram\": [{\"difficulty\": 1, \"wieght\": 1.0}]}]}")));
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).contains("data/eechelonstest/echelons/nested.json"), result.errors().get(0));
        assertTrue(result.errors().get(0).contains("echelons[0].histogram[0].wieght"), result.errors().get(0));
        assertTrue(result.ids().isEmpty());
    }

    @Test
    public void badValueIsAnErrorNamingTheFile() {
        ResourceLocation bad = new ResourceLocation("eechelonstest", "bad_value");
        Result result = load(Map.of(bad, parse(
                "{\"hp_factor\": \"lots\", \"echelons\": [{\"min\": 0, \"max\": 10, \"histogram\": [{\"difficulty\": 1, \"weight\": 1.0}]}]}")));
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).contains("data/eechelonstest/echelons/bad_value.json"), result.errors().get(0));
        assertTrue(result.errors().get(0).contains("hp_factor"), result.errors().get(0));
        assertTrue(result.ids().isEmpty());
    }

    @Test
    public void malformedValuesAreRejected() {
        String echelons = "\"echelons\": [{\"min\": 0, \"max\": 10, \"histogram\": [{\"difficulty\": 1, \"weight\": 1.0}]}]";
        List<String> bad = List.of(
                // a fractional difficulty
                "{\"echelons\": [{\"min\": 0, \"max\": 10, \"histogram\": [{\"difficulty\": 1.5, \"weight\": 1.0}]}]}",
                // min > max
                "{\"echelons\": [{\"min\": 10, \"max\": 0, \"histogram\": [{\"difficulty\": 1, \"weight\": 1.0}]}]}",
                // no histogram
                "{\"echelons\": [{\"min\": 0, \"max\": 10}]}",
                // no echelons
                "{\"hp_factor\": 0.2}",
                // empty echelons
                "{\"echelons\": []}",
                // zero weight
                "{\"echelons\": [{\"min\": 0, \"max\": 10, \"histogram\": [{\"difficulty\": 1, \"weight\": 0.0}]}]}",
                // not a list
                "{\"dimensions\": \"minecraft:overworld\", " + echelons + "}",
                // not a mob id
                "{\"mob_blacklist\": [\"Not A Mob\"], " + echelons + "}",
                // not a boolean
                "{\"replace\": \"yes\", " + echelons + "}",
                // the id comes from the file name
                "{\"id\": \"eechelonstest:other\", " + echelons + "}",
                // not an object
                "[]");
        for (String text : bad) {
            Result result = load(Map.of(DEEP_DARK, parse(text)));
            assertEquals(1, result.errors().size(), text);
            assertTrue(result.errors().get(0).contains("data/eechelonstest/echelons/deep_dark.json"), result.errors().get(0));
        }
    }

    @Test
    public void badNameConfigAndExclusionAreErrorsNamingTheFile() {
        EchelonDataLoader.Result<NameConfigsHolder.NameConfig> names = EchelonDataLoader.loadNameConfigs(Map.of(BRUTES, parse(
                "{\"difficulty_names\": [{\"difficulty\": 1, \"title\": \"Brute\"}]}")));
        assertEquals(1, names.getErrors().size());
        assertTrue(names.getErrors().get(0).contains("data/eechelonstest/difficulty_names/brutes.json"), names.getErrors().get(0));
        assertTrue(names.getValues().isEmpty());

        ResourceLocation exclusionId = new ResourceLocation("eechelonstest", "bad");
        EchelonDataLoader.Result<EchelonExclusions> exclusions = EchelonDataLoader.loadExclusions(Map.of(exclusionId, parse(
                "{\"mods\": [\"dungeons2\"], \"entities\": []}")));
        assertEquals(1, exclusions.getErrors().size());
        assertTrue(exclusions.getErrors().get(0).contains("data/eechelonstest/echelons/exclusions/bad.json"), exclusions.getErrors().get(0));
        assertTrue(exclusions.getErrors().get(0).contains("entities"), exclusions.getErrors().get(0));
    }

    @Test
    public void echelonsListenerSkipsTheExclusionsFolder() {
        ResourceLocation exclusion = new ResourceLocation("eechelonstest", "exclusions/eechelonstest");
        Result result = load(Map.of(
                DEEP_DARK, json("/data/eechelonstest/echelons/deep_dark.json"),
                exclusion, json("/data/eechelonstest/echelons/exclusions/eechelonstest.json")));
        assertTrue(result.errors().isEmpty(), result.errors().toString());
        assertEquals(List.of(DEEP_DARK.toString()), result.ids());
    }

    private record Result(List<String> ids, List<String> errors) {}

    private static Result load(Map<ResourceLocation, JsonElement> files) {
        EchelonDataLoader.Result<EchelonConfigsHolder.Config> result = EchelonDataLoader.loadEchelonConfigs(files);
        return new Result(result.getValues().stream().map(EchelonConfigsHolder.Config::getId).toList(), result.getErrors());
    }
}
