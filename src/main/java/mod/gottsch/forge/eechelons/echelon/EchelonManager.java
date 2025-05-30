/*
 * This file is part of  Enemy Echelons.
 * Copyright (c) 2022 Mark Gottschling (gottsch)
 * 
 * All rights reserved.
 *
 * Enemy Echelons is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Enemy Echelons is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Enemy Echelons.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.eechelons.echelon;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import mod.gottsch.forge.eechelons.EEchelons;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Maps;

import mod.gottsch.forge.eechelons.bst.Interval;
import mod.gottsch.forge.eechelons.bst.IntervalTree;
import mod.gottsch.forge.eechelons.capability.EEchelonsCapabilities;
import mod.gottsch.forge.eechelons.config.Config;
import mod.gottsch.forge.eechelons.config.EchelonsHolder.Echelon;
import mod.gottsch.forge.gottschcore.random.WeightedCollection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier; // Added
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import java.util.UUID; // Added
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper.UnableToAccessFieldException;

/**
 * 
 * @author Mark Gottschling on Jul 26, 2022
 *
 */
public class EchelonManager {
	// Define UUIDs for attribute modifiers
	// Health: "0b8a7a82-325a-4785-9798-c28060aa55a3"
	// Damage: "5a1a9a4c-6f4f-4c62-9a68-09b969dc68ac"
	// Armor: "c3e0f5a0-5c9e-4f72-978d-5c6d7c9a8b7f"
	// Armor Toughness: "d4f1e8a0-6b8d-4c3a-9a6b-1d2c3e4f5a6b"
	// Movement Speed: "a5b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d"
	// Attack Knockback: "b6c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e"
	// Knockback Resistance: "c7d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6f7a"
	private static final UUID ECHELON_MAX_HEALTH_MODIFIER_ID = UUID.fromString("0b8a7a82-325a-4785-9798-c28060aa55a3");
	private static final UUID ECHELON_ATTACK_DAMAGE_MODIFIER_ID = UUID.fromString("5a1a9a4c-6f4f-4c62-9a68-09b969dc68ac");
	private static final UUID ECHELON_ARMOR_MODIFIER_ID = UUID.fromString("c3e0f5a0-5c9e-4f72-978d-5c6d7c9a8b7f");
	private static final UUID ECHELON_ARMOR_TOUGHNESS_MODIFIER_ID = UUID.fromString("d4f1e8a0-6b8d-4c3a-9a6b-1d2c3e4f5a6b");
	private static final UUID ECHELON_MOVEMENT_SPEED_MODIFIER_ID = UUID.fromString("a5b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");
	private static final UUID ECHELON_ATTACK_KNOCKBACK_MODIFIER_ID = UUID.fromString("b6c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e");
	private static final UUID ECHELON_KNOCKBACK_RESISTANCE_MODIFIER_ID = UUID.fromString("c7d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6f7a");

	// f_21364_ => xpReward
	private static final String XP_REWARD_FIELDNAME = "f_21364_";
	private static final ResourceLocation ALL_DIMENSION = new ResourceLocation(".", ".");

	/*
	 * map of echelons by id
	 * currently not implemented in any meaningful way.
	 */
	private static final Map<String, Echelon> ECHELONS_BY_ID = Maps.newHashMap();

	/*
	 * map of echelons by dimension
	 */
	private static final Multimap<ResourceLocation, Echelon> ECHELONS = ArrayListMultimap.create();

	/*
	 * map of echelons by dimension-mod (namespace) pair
	 */
	private static final Map<Pair<ResourceLocation, String>, Echelon> ECHELONS_BY_MOD = Maps.newHashMap();

	/*
	 * map of echelons by dimension-mob pair.
	 * this is for white-list mobs.
	 */
	private static final Map<Pair<ResourceLocation, ResourceLocation>, Echelon> ECHELONS_BY_MOB = Maps.newHashMap();

	/**
	 * 
	 */
	public static void build() {
		ECHELONS_BY_ID.clear();
		ECHELONS.clear();

		ECHELONS_BY_MOD.clear();

		ECHELONS_BY_MOB.clear();

		List<Echelon> echelons = Config.echelons;
		if (ObjectUtils.isEmpty(echelons)) {
			return;
		}
		echelons.forEach(echelon -> {
			if (ObjectUtils.isEmpty(echelon.getStratum())) {
				return;
			}

			// add to map
			if (!StringUtils.isNotBlank(echelon.getId())) {
				ECHELONS_BY_ID.put(echelon.getId(), echelon);
			}

			// scan the mob white/black list to see if there are any wildcards and move to mod lists.
			Predicate<String> isWildcard = mobId -> mobId.contains(":*");
			List<String> modsToAddForWhitelist = new java.util.ArrayList<>();
			List<String> modsToAddForBlacklist = new java.util.ArrayList<>();

			for (String mobId : echelon.getMobWhitelist()) {
				if (isWildcard.test(mobId)) {
					modsToAddForWhitelist.add(mobId.substring(0, mobId.indexOf(":")));
				}
			}
			for (String mobId : echelon.getMobBlacklist()) {
				if (isWildcard.test(mobId)) {
					modsToAddForBlacklist.add(mobId.substring(0, mobId.indexOf(":")));
				}
			}

			if (!modsToAddForWhitelist.isEmpty()) {
				echelon.getModWhitelist().addAll(modsToAddForWhitelist);
			}
			if (!modsToAddForBlacklist.isEmpty()) {
				echelon.getModBlacklist().addAll(modsToAddForBlacklist);
			}

			echelon.getMobWhitelist().removeIf(isWildcard);
			echelon.getMobBlacklist().removeIf(isWildcard);

			/*
			 *  build BST
			 */
			// create a new tree
			IntervalTree<WeightedCollection<Double, Integer>> tree = new IntervalTree<>();
			// process each strata in the stratum
			echelon.getStratum().forEach(strata -> {
				// build weighted collection from histogram
				WeightedCollection<Double, Integer> collection = new WeightedCollection<>();
				strata.getHistogram().forEach(entry -> {
					collection.add(entry.getWeight(), entry.getLevel());
				});
				// create new interval
				Interval<WeightedCollection<Double, Integer>> interval = new Interval<>(strata.getMin(), strata.getMax(), collection);
				// add interval to tree
				tree.insert(interval);
			});

			// add histogram to echelon
			echelon.setHistogram(tree);

			// TODO refactor to not duplicate code
			// TODO can simplify by checking if dimension is empty, then add "." to
			// TODO the dimension list and then process the list like normal.
			if (ObjectUtils.isEmpty(echelon.getDimensions())) {
				echelon.getDimensions().add(".");
			}

			// For each dimension this Echelon applies to, categorize it.
			// The categorization establishes a precedence for Echelon lookup:
			// 1. Mod-specific whitelist (ECHELONS_BY_MOD): If the Echelon has mod-specific rules.
			// 2. Mob-specific whitelist (ECHELONS_BY_MOB): If no mod rules, but has mob-specific rules.
			// 3. General dimension Echelon (ECHELONS): If neither mod nor mob specific rules.
			// Note: The actual lookup in getEchelon() prioritizes mob-specific entries first over any dimension,
			// then falls back to searching general ECHELONS for the dimension (which includes those originally mod-specific).
			echelon.getDimensions().forEach(dimension -> {
				ResourceLocation dimensionKey;
				if (dimension.equals(".") || dimension.equals("*") || dimension.equals("*:*")) {
					dimensionKey = ALL_DIMENSION;
				} else {
					dimensionKey = new ResourceLocation(dimension);
				}

				// If Echelon has a mod whitelist, it's primarily classified by mod.
				if (!echelon.getModWhitelist().isEmpty()) {
					echelon.getModWhitelist().forEach(mod -> {
						Pair<ResourceLocation, String> keyPair = new ImmutablePair<>(dimensionKey, mod);
						// Only add if this specific dimension-mod pair isn't already defined
						// (first Echelon definition encountered for this pair takes precedence).
						if (!ECHELONS_BY_MOD.containsKey(keyPair)) {
							ECHELONS_BY_MOD.put(keyPair, echelon);
						}
					});
				}
				// Else if Echelon has a mob whitelist, it's classified by mob.
				else if (!echelon.getMobWhitelist().isEmpty()) {
					echelon.getMobWhitelist().forEach(mob -> {
						Pair<ResourceLocation, ResourceLocation> keyPair = new ImmutablePair<>(dimensionKey, new ResourceLocation(mob));
						// Only add if this specific dimension-mob pair isn't already defined.
						if (!ECHELONS_BY_MOB.containsKey(keyPair)) {
							ECHELONS_BY_MOB.put(keyPair, echelon);
						}
					});
				}
				// Else, it's a general Echelon for the dimension.
				else {
					ECHELONS.put(dimensionKey, echelon);
				}
			});
		});
	}

	/**
	 *
	 * @param mob
	 * @return
	 */
	public static Optional<Echelon> getEchelon(Mob mob) {
		Pair<ResourceLocation, ResourceLocation> keyPair = new ImmutablePair<>(mob.level().dimension().location(), EntityType.getKey(mob.getType()));
		if (ECHELONS_BY_MOB.containsKey(keyPair)) {
			return Optional.of(ECHELONS_BY_MOB.get(keyPair));
		}
		else {
			keyPair = new ImmutablePair<>(ALL_DIMENSION, EntityType.getKey(mob.getType()));
			if (ECHELONS_BY_MOB.containsKey(keyPair)) {
				return Optional.of(ECHELONS_BY_MOB.get(keyPair));
			}
			else {
				Optional<Echelon> echelon = searchEchelonsForMob(mob.level().dimension().location(), mob);
				if (echelon.isEmpty()) {
					echelon = searchEchelonsForMob(ALL_DIMENSION, mob);
				}
				return echelon;
			}
		}
	}

	public static Optional<Echelon> searchEchelonsForMob(ResourceLocation dimension, Mob entity) {
		Optional<Echelon> echelon = Optional.empty();
		ResourceLocation mob = EntityType.getKey(entity.getType());
		// for each echelon in a given dimension
		for (Echelon e : ECHELONS.get(dimension)) {
			// find the first valid echelon - ie not in the blacklist
			// NOTE it is assumed that the whitelisted-mob lists have been interrogated already
			if (!e.getModBlacklist().contains(mob.getNamespace())) {
				if (!e.getMobBlacklist().contains(mob.toString())) {
					return Optional.of(e);
				}
			}
		}
		return echelon;
	}

	/**
	 * 
	 * @param entity
	 * @return
	 */
	public static boolean isValidEntity(final Entity entity) {
		return entity instanceof Mob;
	}

	/**
	 * 
	 * @param mob
	 */
	public static void applyModications(Mob mob) {
		mob.getCapability(EEchelonsCapabilities.LEVEL_CAPABILITY).ifPresent(cap -> {

			if (cap.getLevel() < 0) {
				// determine the altitute (y-value)
				int y = mob.getBlockY();

				/*
				 *  apply the attribute modifications
				 */
				Optional<Echelon> echelon = getEchelon(mob);

				if (echelon.isEmpty()) {
					cap.setLevel(0);
					return;
				}

				Integer echelonLevel = echelon.get().getLevel(y);
//				EEchelons.LOGGER.debug("selected level -> {} for dimension -> {} @ y -> {}", echelonLevel, dimension, y);

				// health
				modifyHealth(mob, echelonLevel, echelon.get());

				// damage
				modifyDamage(mob, echelonLevel, echelon.get());

				// armor
				modifyArmor(mob, echelonLevel, echelon.get());

				// armor
				modifyArmorToughness(mob, echelonLevel, echelon.get());

				// knockback
				modifyKnockback(mob, echelonLevel, echelon.get());

				// knockback resist
				modifyKnockbackResist(mob, echelonLevel, echelon.get());

				// speed
				modifySpeed(mob, echelonLevel, echelon.get());

				// update the capability
				cap.setLevel(echelonLevel);
			}
		});
	}

	private static void modifySpeed(Mob mob, Integer level, Echelon echelon) {
		if (echelon.hasSpeedFactor()) {
			AttributeInstance attribute = mob.getAttribute(Attributes.MOVEMENT_SPEED);
			if (attribute != null) {
				attribute.removeModifier(ECHELON_MOVEMENT_SPEED_MODIFIER_ID);

				double speedMultiplier = 1.0 + (echelon.getSpeedFactor() * level);
				double modifierAmount = speedMultiplier - 1.0;
				
				if (Math.abs(modifierAmount) > 1.0E-7) {
					AttributeModifier speedModifier = new AttributeModifier(
							ECHELON_MOVEMENT_SPEED_MODIFIER_ID,
							"EchelonMovementSpeed",
							modifierAmount,
							AttributeModifier.Operation.MULTIPLY_BASE
					);
					attribute.addPermanentModifier(speedModifier);
				}
				// EEchelons.LOGGER.debug("mob new speed -> {}", mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
			}
		}
	}

	private static void modifyHealth(Mob mob, int level, Echelon echelon) {
		if (echelon.hasHpFactor()) {
			AttributeInstance attribute = mob.getAttribute(Attributes.MAX_HEALTH);
			if (attribute != null) {
				// Remove any existing modifier from this mod
				attribute.removeModifier(ECHELON_MAX_HEALTH_MODIFIER_ID);

				double healthMultiplier = 1.0 + (echelon.getHpFactor() * level);
				double modifierAmount = healthMultiplier - 1.0;

				// Apply new modifier only if it has a significant effect
				if (Math.abs(modifierAmount) > 1.0E-7) { // Check if modifierAmount is not effectively zero
					AttributeModifier healthModifier = new AttributeModifier(
							ECHELON_MAX_HEALTH_MODIFIER_ID,
							"EchelonMaxHealth", // Name for debugging/identification
							modifierAmount,
							AttributeModifier.Operation.MULTIPLY_BASE
					);
					attribute.addPermanentModifier(healthModifier);
				}
				
				// Heal the mob to its new maximum health
				mob.setHealth(mob.getMaxHealth());
//				EEchelons.LOGGER.debug("mob new health -> {}", mob.getMaxHealth());
			}
		}
	}

	private static void modifyDamage(Mob mob, int level, Echelon echelon) {		
		if (echelon.hasDamageFactor()) {
			AttributeInstance attribute = mob.getAttribute(Attributes.ATTACK_DAMAGE);
			if (attribute != null) {
				// Remove any existing modifier from this mod
				attribute.removeModifier(ECHELON_ATTACK_DAMAGE_MODIFIER_ID);
				
				double damageMultiplier = 1.0 + (echelon.getDamageFactor() * level);
				double modifierAmount = damageMultiplier - 1.0;

				// Apply new modifier only if it has a significant effect
				if (Math.abs(modifierAmount) > 1.0E-7) { // Check if modifierAmount is not effectively zero
					AttributeModifier damageModifier = new AttributeModifier(
							ECHELON_ATTACK_DAMAGE_MODIFIER_ID,
							"EchelonAttackDamage", // Name for debugging/identification
							modifierAmount,
							AttributeModifier.Operation.MULTIPLY_BASE
					);
					attribute.addPermanentModifier(damageModifier);
				}
//				EEchelons.LOGGER.debug("mob new damage -> {}", mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
			}
		}
	}

	private static void modifyArmor(Mob mob, Integer level, Echelon echelon) {
		if (echelon.hasArmorFactor()) {
			AttributeInstance attribute = mob.getAttribute(Attributes.ARMOR);
			if (attribute != null) {
				attribute.removeModifier(ECHELON_ARMOR_MODIFIER_ID);

				double armorMultiplier = 1.0 + (echelon.getArmorFactor() * level);
				double modifierAmount = armorMultiplier - 1.0;

				if (Math.abs(modifierAmount) > 1.0E-7) {
					AttributeModifier armorModifier = new AttributeModifier(
							ECHELON_ARMOR_MODIFIER_ID,
							"EchelonArmor",
							modifierAmount,
							AttributeModifier.Operation.MULTIPLY_BASE
					);
					attribute.addPermanentModifier(armorModifier);
				}
				// EEchelons.LOGGER.debug("mob new armor -> {}", mob.getAttributeValue(Attributes.ARMOR));
			}
		}
	}

	private static void modifyArmorToughness(Mob mob, Integer level, Echelon echelon) {
		if (echelon.hasArmorToughnessFactor()) {
			AttributeInstance attribute = mob.getAttribute(Attributes.ARMOR_TOUGHNESS);
			if (attribute != null) {
				attribute.removeModifier(ECHELON_ARMOR_TOUGHNESS_MODIFIER_ID);

				double armorToughnessMultiplier = 1.0 + (echelon.getArmorToughnessFactor() * level);
				double modifierAmount = armorToughnessMultiplier - 1.0;

				if (Math.abs(modifierAmount) > 1.0E-7) {
					AttributeModifier armorToughnessModifier = new AttributeModifier(
							ECHELON_ARMOR_TOUGHNESS_MODIFIER_ID,
							"EchelonArmorToughness",
							modifierAmount,
							AttributeModifier.Operation.MULTIPLY_BASE
					);
					attribute.addPermanentModifier(armorToughnessModifier);
				}
				// EEchelons.LOGGER.debug("mob new armor toughness -> {}", mob.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
			}
		}
	}

	private static void modifyKnockback(Mob mob, int level, Echelon echelon) {
		if (echelon.hasKnockbackIncrement()) {
			AttributeInstance attribute = mob.getAttribute(Attributes.ATTACK_KNOCKBACK);
			if (attribute != null) {
				attribute.removeModifier(ECHELON_ATTACK_KNOCKBACK_MODIFIER_ID);

				double modifierAmount = echelon.getKnockbackIncrement() * level;

				if (Math.abs(modifierAmount) > 1.0E-7) {
					AttributeModifier knockbackModifier = new AttributeModifier(
							ECHELON_ATTACK_KNOCKBACK_MODIFIER_ID,
							"EchelonAttackKnockback",
							modifierAmount,
							AttributeModifier.Operation.ADDITION
					);
					attribute.addPermanentModifier(knockbackModifier);
				}
				// EEchelons.LOGGER.debug("mob new knockback -> {}", mob.getAttributeValue(Attributes.ATTACK_KNOCKBACK));
			}
		}
	}

	private static void modifyKnockbackResist(Mob mob, int level, Echelon echelon) {
		if (echelon.hasKnockbackResistIncrement()) {
			AttributeInstance attribute = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
			if (attribute != null) {
				attribute.removeModifier(ECHELON_KNOCKBACK_RESISTANCE_MODIFIER_ID);
				
				double modifierAmount = echelon.getKnockbackResistIncrement() * level;
				
				if (Math.abs(modifierAmount) > 1.0E-7) {
					AttributeModifier knockbackResistModifier = new AttributeModifier(
							ECHELON_KNOCKBACK_RESISTANCE_MODIFIER_ID,
							"EchelonKnockbackResistance",
							modifierAmount,
							AttributeModifier.Operation.ADDITION
					);
					attribute.addPermanentModifier(knockbackResistModifier);
				}
				// EEchelons.LOGGER.debug("mob new knockback resist -> {}", mob.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
			}
		}
	}
}
