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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import mod.gottsch.forge.eechelonsapi.core.config.EchelonConfigsHolder;
import mod.gottsch.forge.eechelonsapi.core.config.EchelonExclusions;
import mod.gottsch.forge.eechelonsapi.core.config.NameConfigsHolder;
import net.minecraft.resources.ResourceLocation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Strict decoders for the datapack json files.
 * Keys are the snake_case forms of the EchelonConfigsHolder / NameConfigsHolder fields.
 * An unknown key or a malformed value throws a JsonParseException naming the key; nothing is silently defaulted.
 * @author Mark Gottschling on 10/29/2026
 */
public final class EchelonDataCodecs {
    public static final int DEFAULT_LOAD_ORDER = 99;

    private static final Set<String> WILDCARDS = Set.of(".", ".:.", "*", "*:*");

    private static final Set<String> CONFIG_KEYS = Set.of("load_order", "dimensions", "replace",
            "hp_factor", "max_hp", "damage_factor", "max_damage",
            "knockback_increment", "max_knockback", "knockback_resist_increment", "max_knockback_resist",
            "armor_factor", "max_armor", "armor_toughness_factor", "max_armor_toughness",
            "speed_factor", "max_speed", "flying_speed_factor", "max_flying_speed", "xp_factor", "max_xp",
            "mod_whitelist", "mod_blacklist", "mob_whitelist", "mob_blacklist", "echelons");
    private static final Set<String> ECHELON_KEYS = Set.of("id", "min", "max", "histogram");
    private static final Set<String> HISTOGRAM_KEYS = Set.of("difficulty", "weight");
    private static final Set<String> NAME_CONFIG_KEYS = Set.of("load_order",
            "mod_whitelist", "mod_blacklist", "mob_whitelist", "mob_blacklist", "difficulty_names");
    private static final Set<String> DIFFICULTY_NAME_KEYS = Set.of("difficulty", "name");
    private static final Set<String> EXCLUSION_KEYS = Set.of("mods", "mobs");

    private EchelonDataCodecs() {}

    /**
     * decodes data/[namespace]/echelons/[name].json
     * @param id the file's resource location, used as the config id
     * @param json
     * @return
     */
    public static EchelonConfigsHolder.Config decodeEchelonConfig(ResourceLocation id, JsonElement json) {
        JsonObject object = object(json, "root", CONFIG_KEYS);
        EchelonConfigsHolder.Config config = new EchelonConfigsHolder.Config();
        config.setId(id.toString());
        config.setLoadOrder(optInt(object, "load_order", DEFAULT_LOAD_ORDER));
        config.setDimensions(stringList(object, "dimensions", EchelonDataCodecs::checkDimension));
        if (object.has("replace")) {
            config.setReplace(bool(object.get("replace"), "replace"));
        }

        // factors keep the class default (0.0) when absent; caps stay null when absent.
        optDouble(object, "hp_factor", config::setHpFactor);
        optDouble(object, "max_hp", config::setMaxHp);
        optDouble(object, "damage_factor", config::setDamageFactor);
        optDouble(object, "max_damage", config::setMaxDamage);
        optDouble(object, "knockback_increment", config::setKnockbackIncrement);
        optDouble(object, "max_knockback", config::setMaxKnockback);
        optDouble(object, "knockback_resist_increment", config::setKnockbackResistIncrement);
        optDouble(object, "max_knockback_resist", config::setMaxKnockbackResist);
        optDouble(object, "armor_factor", config::setArmorFactor);
        optDouble(object, "max_armor", config::setMaxArmor);
        optDouble(object, "armor_toughness_factor", config::setArmorToughnessFactor);
        optDouble(object, "max_armor_toughness", config::setMaxArmorToughness);
        optDouble(object, "speed_factor", config::setSpeedFactor);
        optDouble(object, "max_speed", config::setMaxSpeed);
        optDouble(object, "flying_speed_factor", config::setFlyingSpeedFactor);
        optDouble(object, "max_flying_speed", config::setMaxFlyingSpeed);
        optDouble(object, "xp_factor", config::setXpFactor);
        optDouble(object, "max_xp", config::setMaxXp);

        config.setModWhitelist(stringList(object, "mod_whitelist", EchelonDataCodecs::checkMod));
        config.setModBlacklist(stringList(object, "mod_blacklist", EchelonDataCodecs::checkMod));
        config.setMobWhitelist(stringList(object, "mob_whitelist", EchelonDataCodecs::checkMobOrWildcard));
        config.setMobBlacklist(stringList(object, "mob_blacklist", EchelonDataCodecs::checkMobOrWildcard));

        config.setEchelons(list(required(object, "echelons"), "echelons", true, EchelonDataCodecs::decodeEchelon));
        return config;
    }

    private static EchelonConfigsHolder.Echelon decodeEchelon(JsonElement json, String path) {
        JsonObject object = object(json, path, ECHELON_KEYS);
        EchelonConfigsHolder.Echelon echelon = new EchelonConfigsHolder.Echelon();
        if (object.has("id")) {
            echelon.setId(string(object.get("id"), path + ".id"));
        }
        echelon.setMin(integer(required(object, "min", path), path + ".min"));
        echelon.setMax(integer(required(object, "max", path), path + ".max"));
        if (echelon.getMin() > echelon.getMax()) {
            throw new JsonParseException(path + ": min (" + echelon.getMin() + ") is greater than max (" + echelon.getMax() + ")");
        }
        echelon.setHistogram(list(required(object, "histogram", path), path + ".histogram", true, EchelonDataCodecs::decodeDifficultyEntry));
        return echelon;
    }

    private static EchelonConfigsHolder.DifficultyEntry decodeDifficultyEntry(JsonElement json, String path) {
        JsonObject object = object(json, path, HISTOGRAM_KEYS);
        EchelonConfigsHolder.DifficultyEntry entry = new EchelonConfigsHolder.DifficultyEntry();
        entry.setDifficulty(integer(required(object, "difficulty", path), path + ".difficulty"));
        double weight = number(required(object, "weight", path), path + ".weight");
        if (weight <= 0.0) {
            throw new JsonParseException(path + ".weight: must be greater than 0, got " + weight);
        }
        entry.setWeight(weight);
        return entry;
    }

    /**
     * decodes data/[namespace]/difficulty_names/[name].json
     * @param id the file's resource location, used as the config id
     * @param json
     * @return
     */
    public static NameConfigsHolder.NameConfig decodeNameConfig(ResourceLocation id, JsonElement json) {
        JsonObject object = object(json, "root", NAME_CONFIG_KEYS);
        NameConfigsHolder.NameConfig config = new NameConfigsHolder.NameConfig();
        config.setId(id.toString());
        config.setLoadOrder(optInt(object, "load_order", DEFAULT_LOAD_ORDER));
        config.setModWhitelist(stringList(object, "mod_whitelist", EchelonDataCodecs::checkMod));
        config.setModBlacklist(stringList(object, "mod_blacklist", EchelonDataCodecs::checkMod));
        config.setMobWhitelist(stringList(object, "mob_whitelist", EchelonDataCodecs::checkMobOrWildcard));
        config.setMobBlacklist(stringList(object, "mob_blacklist", EchelonDataCodecs::checkMobOrWildcard));
        config.setDifficultyNames(list(required(object, "difficulty_names"), "difficulty_names", true, EchelonDataCodecs::decodeDifficultyName));
        return config;
    }

    private static NameConfigsHolder.DifficultyName decodeDifficultyName(JsonElement json, String path) {
        JsonObject object = object(json, path, DIFFICULTY_NAME_KEYS);
        Integer difficulty = integer(required(object, "difficulty", path), path + ".difficulty");
        String name = string(required(object, "name", path), path + ".name");
        if (name.isBlank()) {
            throw new JsonParseException(path + ".name: must not be blank");
        }
        return new NameConfigsHolder.DifficultyName(difficulty, name);
    }

    /**
     * decodes data/[namespace]/echelons/exclusions/[name].json
     * a mob wildcard, ex. "mymod:*", is the same as adding "mymod" to mods.
     * @param json
     * @return
     */
    public static EchelonExclusions decodeExclusions(JsonElement json) {
        JsonObject object = object(json, "root", EXCLUSION_KEYS);
        List<String> mods = stringList(object, "mods", EchelonDataCodecs::checkNamespace);
        List<ResourceLocation> mobs = new ArrayList<>();
        stringList(object, "mobs", EchelonDataCodecs::checkMobOrWildcard).forEach(mob -> {
            if (mob.endsWith(":*")) {
                mods.add(mob.substring(0, mob.indexOf(":")));
            } else {
                mobs.add(new ResourceLocation(mob));
            }
        });
        return new EchelonExclusions(mods, mobs);
    }

    /*
     * primitives
     */

    private static JsonObject object(JsonElement json, String path, Set<String> allowedKeys) {
        if (json == null || !json.isJsonObject()) {
            throw new JsonParseException(path + ": expected an object, got " + json);
        }
        JsonObject object = json.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!allowedKeys.contains(entry.getKey())) {
                String key = "root".equals(path) ? entry.getKey() : path + "." + entry.getKey();
                throw new JsonParseException(key + ": unknown key");
            }
        }
        return object;
    }

    private static JsonElement required(JsonObject object, String key) {
        return required(object, key, null);
    }

    private static JsonElement required(JsonObject object, String key, String path) {
        if (!object.has(key)) {
            throw new JsonParseException((path == null ? "" : path + ".") + key + ": missing required key");
        }
        return object.get(key);
    }

    private static Integer optInt(JsonObject object, String key, int defaultValue) {
        return object.has(key) ? integer(object.get(key), key) : defaultValue;
    }

    private static void optDouble(JsonObject object, String key, Consumer<Double> setter) {
        if (object.has(key)) {
            setter.accept(number(object.get(key), key));
        }
    }

    private static double number(JsonElement json, String path) {
        if (json == null || !json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(path + ": expected a number, got " + json);
        }
        double value = json.getAsDouble();
        if (!Double.isFinite(value)) {
            throw new JsonParseException(path + ": expected a finite number, got " + json);
        }
        return value;
    }

    private static Integer integer(JsonElement json, String path) {
        if (json == null || !json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(path + ": expected an integer, got " + json);
        }
        try {
            return new BigDecimal(json.getAsString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new JsonParseException(path + ": expected an integer, got " + json);
        }
    }

    private static boolean bool(JsonElement json, String path) {
        if (json == null || !json.isJsonPrimitive() || !json.getAsJsonPrimitive().isBoolean()) {
            throw new JsonParseException(path + ": expected true or false, got " + json);
        }
        return json.getAsBoolean();
    }

    private static String string(JsonElement json, String path) {
        if (json == null || !json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) {
            throw new JsonParseException(path + ": expected a string, got " + json);
        }
        return json.getAsString();
    }

    private static <T> List<T> list(JsonElement json, String path, boolean nonEmpty, ElementDecoder<T> decoder) {
        if (json == null || !json.isJsonArray()) {
            throw new JsonParseException(path + ": expected a list, got " + json);
        }
        JsonArray array = json.getAsJsonArray();
        if (nonEmpty && array.size() == 0) {
            throw new JsonParseException(path + ": must not be empty");
        }
        List<T> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            result.add(decoder.apply(array.get(i), path + "[" + i + "]"));
        }
        return result;
    }

    /*
     * an optional list of strings, each checked by the validator. absent -> empty (mutable) list.
     */
    private static List<String> stringList(JsonObject object, String key, Function<String, String> validator) {
        if (!object.has(key)) {
            return new ArrayList<>();
        }
        return list(object.get(key), key, false, (element, path) -> {
            String value = string(element, path);
            String error = validator.apply(value);
            if (error != null) {
                throw new JsonParseException(path + ": " + error + ", got \"" + value + "\"");
            }
            return value;
        });
    }

    /*
     * validators return null when valid, else a description of what was expected.
     */

    private static String checkDimension(String value) {
        return WILDCARDS.contains(value) || ResourceLocation.isValidResourceLocation(value)
                ? null : "expected a dimension id or one of . * *:*";
    }

    private static String checkMod(String value) {
        return WILDCARDS.contains(value) || checkNamespace(value) == null
                ? null : "expected a mod id or one of . * *:*";
    }

    private static String checkNamespace(String value) {
        return !value.isEmpty() && ResourceLocation.isValidNamespace(value) ? null : "expected a mod id";
    }

    private static String checkMob(String value) {
        return value.contains(":") && ResourceLocation.isValidResourceLocation(value) ? null : "expected a mob id, ex. minecraft:zombie";
    }

    private static String checkMobOrWildcard(String value) {
        if (value.endsWith(":*")) {
            return checkNamespace(value.substring(0, value.length() - 2)) == null ? null : "expected a mob id or mod wildcard, ex. minecraft:*";
        }
        return checkMob(value) == null ? null : "expected a mob id or mod wildcard, ex. minecraft:*";
    }

    @FunctionalInterface
    private interface ElementDecoder<T> {
        T apply(JsonElement json, String path);
    }
}
