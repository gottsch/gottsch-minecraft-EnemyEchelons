/*
 * This file is part of  Enemy Echelons.
 * Copyright (c) 2022, Mark Gottschling (gottsch)
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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Maps;
import com.someguyssoftware.gottschcore.random.WeightedCollection;

import mod.gottsch.forge.eechelons.EEchelons;
import mod.gottsch.forge.eechelons.bst.Interval;
import mod.gottsch.forge.eechelons.bst.IntervalTree;
import mod.gottsch.forge.eechelons.capability.EEchelonsCapabilities;
import mod.gottsch.forge.eechelons.config.Config;
import mod.gottsch.forge.eechelons.config.EchelonsHolder.Echelon;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.monster.IMob;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/**
 * 
 * @author Mark Gottschling on Jul 26, 2022
 *
 */
public class EchelonManager {
	// field_70728_aV => xpReward (1.16.5)
	// f_21364_ => xpReward (1.18.2)
	private static final String XP_REWARD_FIELDNAME = "field_70728_aV";
	private static final ResourceLocation ALL_DIMENSION = new ResourceLocation(".", ".");

	/*
	 * map of echelons by dimension
	 */
	private static final Map<ResourceLocation, Echelon> ECHELONS = Maps.newHashMap();
	/*
	 * map of level-histogram interval tree (bst) by dimension
	 */
	private static final Map<ResourceLocation, IntervalTree<WeightedCollection<Double, Integer>>> HISTOGRAM_TREES = Maps.newHashMap();

	/*
	 * map of echelons by dimension-mob pair
	 */
	private static final Map<Pair<ResourceLocation, ResourceLocation>, Echelon> ECHELONS_BY_MOB = Maps.newHashMap();
	/*
	 * map of level-histogram interval tree (bst) by dimension-mob pair
	 */
	private static final Map<Pair<ResourceLocation, ResourceLocation>, IntervalTree<WeightedCollection<Double, Integer>>> HISTOGRAM_TREES_BY_MOB = Maps.newHashMap();

	/**
	 * 
	 */
	public static void build() {
		ECHELONS.clear();
		HISTOGRAM_TREES.clear();

		ECHELONS_BY_MOB.clear();
		HISTOGRAM_TREES_BY_MOB.clear();

		List<Echelon> echelons = Config.echelons;
		//		EEchelons.LOGGER.info("config echelons -> {}", echelons);
		if (echelons == null || echelons.isEmpty()) {
			return;
		}
		echelons.forEach(echelon -> {
			if (echelon.getStratum() == null || echelon.getStratum().isEmpty()) {
				return;
			}
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

			// TODO refactor to not duplicate code
			if (echelon.getDimensions() == null || echelon.getDimensions().isEmpty()) {
				if (!echelon.getMobWhitelist().isEmpty()) {
					echelon.getMobWhitelist().forEach(mob -> {
						// create a key pair
						Pair<ResourceLocation, ResourceLocation> keyPair = new ImmutablePair<>(ALL_DIMENSION, new ResourceLocation(mob));
						ECHELONS_BY_MOB.put(keyPair, echelon);
						HISTOGRAM_TREES_BY_MOB.put(keyPair, tree);
					});

				}
				else {
					ECHELONS.put(ALL_DIMENSION, echelon);
					HISTOGRAM_TREES.put(ALL_DIMENSION, tree);
				}
			}
			else {
				// build
				echelon.getDimensions().forEach(dimension -> {
					ResourceLocation dimensionKey;
					if (dimension.equals(".") || dimension.equals("*") || dimension.equals("*:*")) {
						dimensionKey = ALL_DIMENSION;
					}else {
						dimensionKey = new ResourceLocation(dimension);
					}

					if (!echelon.getMobWhitelist().isEmpty()) {
						echelon.getMobWhitelist().forEach(mob -> {
							// create a key pair
							Pair<ResourceLocation, ResourceLocation> keyPair = new ImmutablePair<>(dimensionKey, new ResourceLocation(mob));
							ECHELONS_BY_MOB.put(keyPair, echelon);
							HISTOGRAM_TREES_BY_MOB.put(keyPair, tree);
						});
					}
					else {
						ECHELONS.put(dimensionKey, echelon);
						HISTOGRAM_TREES.put(dimensionKey, tree);
					}
				});
			}
		});
	}

	/**
	 * 
	 * @param dimension
	 * @param entity
	 * @return
	 */
	public static Echelon getEchelon(MobEntity mob) {
		Pair<ResourceLocation, ResourceLocation> keyPair = new ImmutablePair<>(mob.level.dimension().location(), mob.getType().getRegistryName());
		if (ECHELONS_BY_MOB.containsKey(keyPair)) {
			return ECHELONS_BY_MOB.get(keyPair);
		}
		else {
			keyPair = new ImmutablePair<>(ALL_DIMENSION, mob.getType().getRegistryName());
			if (ECHELONS_BY_MOB.containsKey(keyPair)) {
				return ECHELONS_BY_MOB.get(keyPair);
			}
			else {
				if (isValidEntity(mob.level.dimension().location(), mob)) {
					return ECHELONS.get(mob.level.dimension().location());
				}
				else if (isValidEntity(ALL_DIMENSION, mob)) {
					return ECHELONS.get(ALL_DIMENSION);
				}
			}
		}
		return null;
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	@Deprecated
	public static Echelon getEchelon(ResourceLocation key) {
		if (ECHELONS.containsKey(key)) {
			return ECHELONS.get(key);
		}
		return null;
	}

	public static Integer getLevel(MobEntity mob, Integer searchValue) {
		Integer result = 0;

		IntervalTree<WeightedCollection<Double, Integer>> tree = null;

		// first check the histograms by mob map
		Pair<ResourceLocation, ResourceLocation> keyPair = new ImmutablePair<>(mob.level.dimension().location(), mob.getType().getRegistryName());
		if (HISTOGRAM_TREES_BY_MOB.containsKey(keyPair)) {
			tree = HISTOGRAM_TREES_BY_MOB.get(keyPair);
			result = getLevel(tree, searchValue);
		}
		else {
			keyPair = new ImmutablePair<>(ALL_DIMENSION, mob.getType().getRegistryName());
			if (HISTOGRAM_TREES_BY_MOB.containsKey(keyPair)) {
				tree = HISTOGRAM_TREES_BY_MOB.get(keyPair);
				result = getLevel(tree, searchValue);
			}
			else {
				result = getLevel(mob.level.dimension().location(), searchValue);
			}
		}

		return result;
	}

	/**
	 * 
	 * @param key
	 * @param searchValue
	 * @return
	 */
	public static Integer getLevel(ResourceLocation key, Integer searchValue) {
		Integer result = 0;

		// use default key is not found in the histogram tree
		if (!HISTOGRAM_TREES.containsKey(key)) {
			key = ALL_DIMENSION;
		}

		if (HISTOGRAM_TREES.containsKey(key)) {
			IntervalTree<WeightedCollection<Double, Integer>> tree = HISTOGRAM_TREES.get(key);
			result = getLevel(tree, searchValue);
		}
		return result;
	}

	private static Integer getLevel(IntervalTree<WeightedCollection<Double, Integer>> tree, Integer searchValue) {
		Integer result = 0;

		List<Interval<WeightedCollection<Double, Integer>>> stratum = tree
				.getOverlapping(tree.getRoot(), new Interval<>(searchValue, searchValue), false);

		if (stratum == null || stratum.isEmpty()) {
			return 0;
		}

		// get the first element/strata - there should only be one.
		WeightedCollection<Double, Integer> col = stratum.get(0).getData();
		if (col == null) {
			return 0;
		}
		// get the next weighted random integer
		result = col.next();			

		return result;
	}

	/**
	 * 
	 * @param entity
	 * @return
	 */
	public static boolean isValidEntity(final Entity entity) {
		return entity instanceof LivingEntity && entity instanceof IMob;
	}

	/**
	 * 
	 * @param dimension
	 * @param entity
	 * @return
	 */
	public static boolean isValidEntity(ResourceLocation dimension, Entity entity) {
		boolean result = false;
		if (ECHELONS.containsKey(dimension)) {
			if (!ECHELONS.get(dimension).getMobBlacklist().contains(entity.getType().getRegistryName().toString())) {
				result = true;
			}
		}
		return result;
	}

	/**
	 * 
	 * @param mob
	 */
	public static void applyModications(MobEntity mob) {
		mob.getCapability(EEchelonsCapabilities.LEVEL_CAPABILITY).ifPresent(cap -> {

			if (cap.getLevel() < 0) {
				// determine dimension
				ResourceLocation dimension = mob.level.dimension().location();
				// determine the altitute (y-value)
				int y = mob.blockPosition().getY();

				Integer echelonLevel = EchelonManager.getLevel(mob, y);

				//				EEchelons.LOGGER.info("selected level -> {} for dimension -> {} @ y -> {}", echelonLevel, dimension, y);

				/*
				 *  apply the attribute modifications
				 */
				Echelon echelon = getEchelon(mob);
				//				EEchelons.LOGGER.info("selected echelon -> {}", echelon);
				if (echelon == null) {
					cap.setLevel(0);
					return;
				}

				// health
				modifyHealth(mob, echelonLevel, echelon);

				// damage
				modifyDamage(mob, echelonLevel, echelon);

				// armor
				modifyArmor(mob, echelonLevel, echelon);

				// armor
				modifyArmorToughness(mob, echelonLevel, echelon);

				// knockback
				modifyKnockback(mob, echelonLevel, echelon);

				// knockback resist
				modifyKnockbackResist(mob, echelonLevel, echelon);

				// speed
				modifySpeed(mob, echelonLevel, echelon);

				// experience
				modifyXp(mob, echelonLevel, echelon);

				// update the capability
				cap.setLevel(echelonLevel);
			}
		});
	}

	private static void modifySpeed(MobEntity mob, Integer level, Echelon echelon) {
		if (echelon.hasSpeedFactor()) {
			ModifiableAttributeInstance attribute = mob.getAttribute(Attributes.MOVEMENT_SPEED);
			if (attribute != null) {			
				double speed = 1.0 + (echelon.getSpeedFactor() * level);
				double newSpeed = mob.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() * speed;
				if (echelon.getMaxDamage() != null) {
					// TODO what if max speed <= 0
					newSpeed = Math.min(newSpeed, echelon.getMaxSpeed());
				}
				attribute.setBaseValue(newSpeed);
				//			EEchelons.LOGGER.info("mob new speed -> {}", mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
			}
		}
	}

	private static void modifyXp(MobEntity mob, Integer level, Echelon echelon) {
		if (echelon.hasXpFactor()) {
			double xp = 1.0 + (echelon.getXpFactor() * level);
			try {
				int xpReward = (int)ObfuscationReflectionHelper.getPrivateValue(MobEntity.class, mob, XP_REWARD_FIELDNAME);
				double newXpReward = xpReward * xp;
				if (echelon.getMaxXp() != null) {
					newXpReward = Math.min(newXpReward, echelon.getMaxXp());
				}
				ObfuscationReflectionHelper.setPrivateValue(MobEntity.class, mob, (int)newXpReward, XP_REWARD_FIELDNAME);
			}
			catch(Exception e	) {
				return;
			}
		}
	}

	private static void modifyHealth(MobEntity mob, int level, Echelon echelon) {
		if (echelon.hasHpFactor()) {
			ModifiableAttributeInstance attribute = mob.getAttribute(Attributes.MAX_HEALTH);
			if (attribute != null) {
				double health = 1.0 + (echelon.getHpFactor() * level);
				double newHealth = mob.getMaxHealth() * health;
				if (echelon.getMaxHp() != null && echelon.getMaxHp() > 0.0) {
					newHealth = Math.min(newHealth, echelon.getMaxHp());
				}
				attribute.setBaseValue(newHealth);
				mob.setHealth(mob.getMaxHealth());
				//			EEchelons.LOGGER.info("mob new health -> {}", mob.getMaxHealth());
			}
		}
	}

	private static void modifyDamage(MobEntity mob, int level, Echelon echelon) {		
		if (echelon.hasDamageFactor()) {
			ModifiableAttributeInstance attribute = mob.getAttribute(Attributes.ATTACK_DAMAGE);
			if (attribute != null) {
				double damage = 1.0 + (echelon.getDamageFactor() * level);
				double newDamage = attribute.getBaseValue() * damage;
				if (echelon.getMaxDamage() != null) {
					newDamage = Math.min(newDamage, echelon.getMaxDamage());
				}
				attribute.setBaseValue(newDamage);
//				EEchelons.LOGGER.info("mob new damage -> {}", mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
			}
		}
	}

	private static void modifyArmor(MobEntity mob, Integer level, Echelon echelon) {
		if (echelon.hasArmorFactor()) {
			ModifiableAttributeInstance attribute = mob.getAttribute(Attributes.ARMOR);
			if (attribute != null) {
				double armor = 1.0 + (echelon.getArmorFactor() * level);
				double newArmor = mob.getAttribute(Attributes.ARMOR).getBaseValue() * armor;
				if (echelon.getMaxArmor() != null) {
					newArmor = Math.min(newArmor, echelon.getMaxArmor());
				}
				attribute.setBaseValue(newArmor);
				//		EEchelons.LOGGER.info("mob new armor -> {}", mob.getAttributeValue(Attributes.ARMOR));
			}
		}
	}

	private static void modifyArmorToughness(MobEntity mob, Integer level, Echelon echelon) {
		if (echelon.hasArmorToughnessFactor()) {
			ModifiableAttributeInstance attribute = mob.getAttribute(Attributes.ARMOR_TOUGHNESS);
			if (attribute != null) {
				double armor = 1.0 + (echelon.getArmorToughnessFactor() * level);
				double newArmor = mob.getAttribute(Attributes.ARMOR_TOUGHNESS).getBaseValue() * armor;
				if (echelon.getMaxArmorToughness() != null) {
					newArmor = Math.min(newArmor, echelon.getMaxArmorToughness());
				}
				attribute.setBaseValue(newArmor);
				//		EEchelons.LOGGER.info("mob new armor toughness -> {}", mob.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
			}
		}
	}

	private static void modifyKnockback(MobEntity mob, int level, Echelon echelon) {
		if (echelon.hasKnockbackIncrement()) {
			ModifiableAttributeInstance attribute = mob.getAttribute(Attributes.ATTACK_KNOCKBACK);
			if (attribute != null) {
				double knockback = echelon.getKnockbackIncrement() * level;
				double newKnockback = mob.getAttribute(Attributes.ATTACK_KNOCKBACK).getBaseValue() + knockback;
				if (echelon.getMaxKnockback() != null) {
					newKnockback = Math.min(newKnockback, echelon.getMaxKnockback());
				}
				attribute.setBaseValue(newKnockback);
				//			EEchelons.LOGGER.info("mob new knockback -> {}", mob.getAttributeValue(Attributes.ATTACK_KNOCKBACK));
			}
		}
	}

	private static void modifyKnockbackResist(MobEntity mob, int level, Echelon echelon) {
		if (echelon.hasKnockbackResistIncrement()) {
			ModifiableAttributeInstance attribute = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
			if (attribute != null) {
				double knockback = echelon.getKnockbackResistIncrement() * level;
				double newKnockback = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getBaseValue() + knockback;
				if (echelon.getMaxKnockbackResist() != null) {
					newKnockback = Math.min(newKnockback, echelon.getMaxKnockbackResist());
				}
				attribute.setBaseValue(newKnockback);
				//			EEchelons.LOGGER.info("mob new knockback resist -> {}", mob.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
			}
		}
	}
}
