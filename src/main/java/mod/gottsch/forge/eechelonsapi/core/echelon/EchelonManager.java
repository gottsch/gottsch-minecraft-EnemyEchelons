/*
 * This file is part of  Enemy Echelons API.
 * Copyright (c) 2022 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.eechelonsapi.core.echelon;

import mod.gottsch.forge.eechelonsapi.core.capability.ModCapabilities;
import mod.gottsch.forge.eechelonsapi.core.capability.IDifficultyHandler;
import mod.gottsch.forge.eechelonsapi.core.config.EchelonConfigsHolder;
import mod.gottsch.forge.eechelonsapi.core.config.EchelonConfigsHolder.Config;
import mod.gottsch.forge.eechelonsapi.core.registry.DifficultyNameRegistry;
import mod.gottsch.forge.eechelonsapi.core.registry.EchelonRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Optional;
import java.util.UUID;

/**
 * TODO if separate Manager from Registry, the Registry would be instance while the Manager could be singleton.
 * @author Mark Gottschling on Jul 26, 2022
 */
public class EchelonManager {
    // f_21364_ => xpReward
//    private static final String XP_REWARD_FIELDNAME = "f_21364_";
//    private static final ResourceLocation ALL_DIMENSION = new ResourceLocation(".", ".");
    private static final int DIFFICULTY_NOT_SET = -1;

    private static final UUID ECHELON_MAX_HEALTH_MODIFIER_ID = UUID.fromString("0b8a7a82-325a-4785-9798-c28060aa55a3");
    private static final UUID ECHELON_ATTACK_DAMAGE_MODIFIER_ID = UUID.fromString("5a1a9a4c-6f4f-4c62-9a68-09b969dc68ac");
    private static final UUID ECHELON_ARMOR_MODIFIER_ID = UUID.fromString("c3e0f5a0-5c9e-4f72-978d-5c6d7c9a8b7f");
    private static final UUID ECHELON_ARMOR_TOUGHNESS_MODIFIER_ID = UUID.fromString("d4f1e8a0-6b8d-4c3a-9a6b-1d2c3e4f5a6b");
    private static final UUID ECHELON_MOVEMENT_SPEED_MODIFIER_ID = UUID.fromString("a5b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");
    private static final UUID ECHELON_FLYING_SPEED_MODIFIER_ID = UUID.fromString("aa4a2349-1cc5-4cec-810a-157ee204d3dd");
    private static final UUID ECHELON_ATTACK_KNOCKBACK_MODIFIER_ID = UUID.fromString("b6c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e");
    private static final UUID ECHELON_KNOCKBACK_RESISTANCE_MODIFIER_ID = UUID.fromString("c7d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6f7a");

    public static final EchelonRegistry REGISTRY = new EchelonRegistry();

    /**
     * @param entity
     * @return
     */
    public static boolean isValidEntity(final Entity entity) {
        return entity instanceof Mob;
    }

    /*
     * default behaviour. uses the internal mod registry
     */
    public static void applyModifications(Mob mob) {
        applyModifications(REGISTRY, mob, DIFFICULTY_NOT_SET);
    }

    /*
     * for custom registries
     */
    public static void applyModifications(EchelonRegistry registry, Mob mob) {
        applyModifications(registry, mob, DIFFICULTY_NOT_SET);
    }

    /*
     * this method is intended for other mods to make use of, if they have a non-echelon mod registry or would
     * like to directly select an echelon to apply to a programmatically spawned mob.
     */
    public static void applyModifications(EchelonRegistry registry, Mob mob, ResourceLocation echelonId, Integer selectedDifficulty) {
        // get the cap
        LazyOptional<IDifficultyHandler> handler = mob.getCapability(ModCapabilities.DIFFICULTY_CAPABILITY);

        // check if mob has capability
        if (!handler.isPresent()) {
            return;
        }
        // extract handler from capability
        IDifficultyHandler difficultyHandler = handler.map(c -> c).orElseThrow(IllegalStateException::new);
        // check if mob capability values have already been set
        if (difficultyHandler.getDifficulty() > DIFFICULTY_NOT_SET) {
            return;
        }

        // get the config by mob
        Optional<Config> echelonConfig = registry.getEchelonConfig(mob);

        if (echelonConfig.isEmpty()) {
            difficultyHandler.setDifficulty(0);
            return;
        }

        // select the difficulty from the config if not provided (default behavior)
        if (selectedDifficulty == DIFFICULTY_NOT_SET) {
            for (EchelonConfigsHolder.Echelon echelon : echelonConfig.get().getEchelons()) {
                if (echelonId.toString().equals(echelon.getId())) {
                    // get the next weighted random integer
                    selectedDifficulty = echelon.getWeightedDifficulties().next();
                    break;
                }
            }
        }

        // if setting the difficulty was unsuccessful
        if (selectedDifficulty == DIFFICULTY_NOT_SET) {
            return;
        }

        applyModifications(echelonConfig.get(), mob, selectedDifficulty);
    }

    /**
     * @param mob
     */
    public static void applyModifications(EchelonRegistry registry, Mob mob, Integer selectedDifficulty) {

        // get the cap
        LazyOptional<IDifficultyHandler> handler = mob.getCapability(ModCapabilities.DIFFICULTY_CAPABILITY);

        // check if mob has capability
        if (!handler.isPresent()) {
            return;
        }
        // extract handler from capability
        IDifficultyHandler difficultyHandler = handler.map(c -> c).orElseThrow(IllegalStateException::new);
        // check if mob capability values have already been set
        if (difficultyHandler.getDifficulty() > DIFFICULTY_NOT_SET) {
            return;
        }

        // TODO now don't need to be within the lambda
//        mob.getCapability(EEchelonsCapabilities.DIFFICULTY_CAPABILITY).ifPresent(cap -> {
//
//            if (cap.getDifficulty() > DIFFICULTY_NOT_SET) {
//                return;
//            }

        // determine the altitude (y-value)
        int y = mob.getBlockY();

        /*
         *  apply the attribute modifications
         */
        Optional<Config> echelonConfig = registry.getEchelonConfig(mob);

        if (echelonConfig.isEmpty()) {
            difficultyHandler.setDifficulty(0);
            return;
        }

        // select the difficulty from the config if not provided (default behavior)
        if (selectedDifficulty == DIFFICULTY_NOT_SET) {
            selectedDifficulty = echelonConfig.get().getDifficulty(y);
//				EEchelons.LOGGER.debug("selected difficulty -> {} for dimension -> {} @ y -> {}", echelonLevel, dimension, y);
        }

        applyModifications(echelonConfig.get(), mob, selectedDifficulty);
    }

        /*
         * this method does the actual applying of modifications
         */
        private static void applyModifications(EchelonConfigsHolder.Config config, Mob mob, Integer selectedDifficulty) {

            mob.getCapability(ModCapabilities.DIFFICULTY_CAPABILITY).ifPresent(difficultyHandler -> {

                // health
                modifyHealth(mob, selectedDifficulty, config);

                // damage
                modifyDamage(mob, selectedDifficulty, config);

                // armor
                modifyArmor(mob, selectedDifficulty, config);

                // armor
                modifyArmorToughness(mob, selectedDifficulty, config);

                // knockback
                modifyKnockback(mob, selectedDifficulty, config);

                // knockback resist
                modifyKnockbackResist(mob, selectedDifficulty, config);

                // speed
                modifySpeed(mob, selectedDifficulty, config);

                // fly speed
                modifyFlyingSpeed(mob, selectedDifficulty, config);

                // experience
                // NOTE this is handled by the LivingExperienceDropEvent (Forge)
    //				modifyXp(mob, difficulty, echelonConfig.get());

                // update the capability
                difficultyHandler.setDifficulty(selectedDifficulty);

                Optional<String> difficultyName = DifficultyNameRegistry.getDifficultyName(mob, selectedDifficulty);
                difficultyName.ifPresent(difficultyHandler::setName);
            });
    }

    private static void modifySpeed(Mob mob, Integer difficulty, Config echelon) {
        if (echelon.hasSpeedFactor()) {
            AttributeInstance attribute = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attribute != null) {
                attribute.removeModifier(ECHELON_MOVEMENT_SPEED_MODIFIER_ID);

                double speedMultiplier = echelon.getSpeedFactor() * difficulty;

                if (Math.abs(speedMultiplier) > 1.0E-7) {
                    AttributeModifier speedModifier = new AttributeModifier(
                            ECHELON_MOVEMENT_SPEED_MODIFIER_ID,
                            "echelonMovementSpeed",
                            speedMultiplier,
                            AttributeModifier.Operation.MULTIPLY_BASE
                    );
                    attribute.addPermanentModifier(speedModifier);
                }
                // EEchelons.LOGGER.debug("mob new speed -> {}", mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            }
        }
    }

    private static void modifyFlyingSpeed(Mob mob, Integer difficulty, Config echelon) {
        if (echelon.hasSpeedFactor()) {
            AttributeInstance attribute = mob.getAttribute(Attributes.FLYING_SPEED);
            if (attribute != null) {
                attribute.removeModifier(ECHELON_FLYING_SPEED_MODIFIER_ID);

                double speedMultiplier = echelon.getSpeedFactor() * difficulty;

                if (Math.abs(speedMultiplier) > 1.0E-7) {
                    AttributeModifier speedModifier = new AttributeModifier(
                            ECHELON_MOVEMENT_SPEED_MODIFIER_ID,
                            "echelonFlyingSpeed",
                            speedMultiplier,
                            AttributeModifier.Operation.MULTIPLY_BASE
                    );
                    attribute.addPermanentModifier(speedModifier);
                }
                // EEchelons.LOGGER.debug("mob new speed -> {}", mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            }
        }
    }

//    private static void modifyXp(Mob mob, Integer difficulty, Config echelon) {
//        if (echelon.hasXpFactor()) {
//            double xp = 1.0 + (echelon.getXpFactor() * difficulty);
//            try {
//                int xpReward = (int) ObfuscationReflectionHelper.getPrivateValue(Mob.class, mob, XP_REWARD_FIELDNAME);
//                double newXpReward = xpReward * xp;
//                if (echelon.getMaxXp() != null) {
//                    newXpReward = Math.min(newXpReward, echelon.getMaxXp());
//                }
//                ObfuscationReflectionHelper.setPrivateValue(Mob.class, mob, (int) newXpReward, XP_REWARD_FIELDNAME);
//            } catch (UnableToAccessFieldException e) {
//                return;
//            }
//        }
//    }

    private static void modifyHealth(Mob mob, int difficulty, Config config) {
        if (config.hasHpFactor()) {
            AttributeInstance attribute = mob.getAttribute(Attributes.MAX_HEALTH);
            if (attribute != null) {
                // remove any existing modifier from this mod
                attribute.removeModifier(ECHELON_MAX_HEALTH_MODIFIER_ID);

                double healthMultiplier = (config.getHpFactor() * difficulty);

                // Apply new modifier only if it has a significant effect
                if (Math.abs(healthMultiplier) > 1.0E-7) { // Check if modifierAmount is not effectively zero
                    AttributeModifier healthModifier = new AttributeModifier(
                            ECHELON_MAX_HEALTH_MODIFIER_ID,
                            "echelonMaxHealth",
                            healthMultiplier,
                            AttributeModifier.Operation.MULTIPLY_BASE
                    );
                    attribute.addPermanentModifier(healthModifier);
//				EEchelons.LOGGER.debug("mob new health -> {}", mob.getMaxHealth());
                }
                // heal the mob to its new maximum health
                mob.setHealth(mob.getMaxHealth());
            }
        }
    }

    private static void modifyDamage(Mob mob, int difficulty, Config echelon) {
        if (echelon.hasDamageFactor()) {
            AttributeInstance attribute = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attribute != null) {
                // Remove any existing modifier from this mod
                attribute.removeModifier(ECHELON_ATTACK_DAMAGE_MODIFIER_ID);

                double damageMultiplier = (echelon.getDamageFactor() * difficulty);

                // Apply new modifier only if it has a significant effect
                if (Math.abs(damageMultiplier) > 1.0E-7) { // Check if modifierAmount is not effectively zero
                    AttributeModifier damageModifier = new AttributeModifier(
                            ECHELON_ATTACK_DAMAGE_MODIFIER_ID,
                            "echelonAttackDamage",
                            damageMultiplier,
                            AttributeModifier.Operation.MULTIPLY_BASE
                    );
                    attribute.addPermanentModifier(damageModifier);
                }
//				EEchelons.LOGGER.debug("mob new damage -> {}", mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
            }
        }
    }

    private static void modifyArmor(Mob mob, Integer difficulty, Config echelon) {
        if (echelon.hasArmorFactor()) {
            AttributeInstance attribute = mob.getAttribute(Attributes.ARMOR);
            if (attribute != null) {
                attribute.removeModifier(ECHELON_ARMOR_MODIFIER_ID);

                double armorMultiplier = (echelon.getArmorFactor() * difficulty);

                if (Math.abs(armorMultiplier) > 1.0E-7) {
                    AttributeModifier armorModifier = new AttributeModifier(
                            ECHELON_ARMOR_MODIFIER_ID,
                            "echelonArmor",
                            armorMultiplier,
                            AttributeModifier.Operation.MULTIPLY_BASE
                    );
                    attribute.addPermanentModifier(armorModifier);
                }
                //		EEchelons.LOGGER.debug("mob new armor -> {}", mob.getAttributeValue(Attributes.ARMOR));
            }
        }
    }

    private static void modifyArmorToughness(Mob mob, Integer difficulty, Config echelon) {
        if (echelon.hasArmorToughnessFactor()) {
            AttributeInstance attribute = mob.getAttribute(Attributes.ARMOR_TOUGHNESS);
            if (attribute != null) {
                attribute.removeModifier(ECHELON_ARMOR_TOUGHNESS_MODIFIER_ID);

                double armorToughnessMultiplier = echelon.getArmorToughnessFactor() * difficulty;

                if (Math.abs(armorToughnessMultiplier) > 1.0E-7) {
                    AttributeModifier armorToughnessModifier = new AttributeModifier(
                            ECHELON_ARMOR_TOUGHNESS_MODIFIER_ID,
                            "echelonArmorToughness",
                            armorToughnessMultiplier,
                            AttributeModifier.Operation.MULTIPLY_BASE
                    );
                    attribute.addPermanentModifier(armorToughnessModifier);
                }
                //		EEchelons.LOGGER.debug("mob new armor toughness -> {}", mob.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            }
        }
    }

    private static void modifyKnockback(Mob mob, int difficulty, Config echelon) {
        if (echelon.hasKnockbackIncrement()) {
            AttributeInstance attribute = mob.getAttribute(Attributes.ATTACK_KNOCKBACK);
            if (attribute != null) {
                attribute.removeModifier(ECHELON_ATTACK_KNOCKBACK_MODIFIER_ID);

                double modifierAmount = echelon.getKnockbackIncrement() * difficulty;

                if (Math.abs(modifierAmount) > 1.0E-7) {
                    AttributeModifier knockbackModifier = new AttributeModifier(
                            ECHELON_ATTACK_KNOCKBACK_MODIFIER_ID,
                            "echelonAttackKnockback",
                            modifierAmount,
                            AttributeModifier.Operation.ADDITION
                    );
                    attribute.addPermanentModifier(knockbackModifier);
                }
                //			EEchelons.LOGGER.debug("mob new knockback -> {}", mob.getAttributeValue(Attributes.ATTACK_KNOCKBACK));
            }
        }
    }

    private static void modifyKnockbackResist(Mob mob, int difficulty, Config echelon) {
        if (echelon.hasKnockbackResistIncrement()) {
            AttributeInstance attribute = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (attribute != null) {
                attribute.removeModifier(ECHELON_KNOCKBACK_RESISTANCE_MODIFIER_ID);

                double modifierAmount = echelon.getKnockbackResistIncrement() * difficulty;

                if (Math.abs(modifierAmount) > 1.0E-7) {
                    AttributeModifier knockbackResistModifier = new AttributeModifier(
                            ECHELON_KNOCKBACK_RESISTANCE_MODIFIER_ID,
                            "echelonKnockbackResistance",
                            modifierAmount,
                            AttributeModifier.Operation.ADDITION
                    );
                    attribute.addPermanentModifier(knockbackResistModifier);
                }
                //			EEchelons.LOGGER.debug("mob new knockback resist -> {}", mob.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            }
        }
    }
}
