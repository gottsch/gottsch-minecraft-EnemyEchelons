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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper.UnableToAccessFieldException;

/**
 * 
 * @author Mark Gottschling on Jul 26, 2022
 *
 */
public class EchelonManager {
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
			Predicate<String> isWildcard = mob -> mob.contains(":*");
			echelon.getMobWhitelist().stream().filter(isWildcard)
					.forEach(mob -> {
						echelon.getModWhitelist().add(mob.substring(0, mob.indexOf(":")));
					});
			echelon.getMobBlacklist().stream().filter(isWildcard)
					.forEach(mob -> {
						echelon.getModBlacklist().add(mob.substring(0, mob.indexOf(":")));
					});
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

//			if (ObjectUtils.isEmpty(echelon.getDimensions())) {
//				if (!echelon.getModWhitelist().isEmpty()) {
//					echelon.getModWhitelist().forEach(mod -> {
//						// create a key pair
//						Pair<ResourceLocation, String> keyPair = new ImmutablePair<>(ALL_DIMENSION, mod);
//						if (!ECHELONS_BY_MOD.containsKey(keyPair)) {
//							ECHELONS_BY_MOD.put(keyPair, echelon);
////							HISTOGRAM_TREES_BY_MOD.put(keyPair, tree);
//						}
//					});
//				}
//				else if (!echelon.getMobWhitelist().isEmpty()) {
//					echelon.getMobWhitelist().forEach(mob -> {
//						// create a key pair
//						Pair<ResourceLocation, ResourceLocation> keyPair = new ImmutablePair<>(ALL_DIMENSION, new ResourceLocation(mob));
//						if (!ECHELONS_BY_MOB.containsKey(keyPair)) {
//							ECHELONS_BY_MOB.put(keyPair, echelon);
////							HISTOGRAM_TREES_BY_MOB.put(keyPair, tree);
//						}
//					});
//
//				}
//				else {
//					ECHELONS.put(ALL_DIMENSION, echelon);
////					HISTOGRAM_TREES.put(ALL_DIMENSION, tree);
//				}
//			}
//			else {
				// build
				echelon.getDimensions().forEach(dimension -> {
					ResourceLocation dimensionKey;
					if (dimension.equals(".") || dimension.equals("*") || dimension.equals("*:*")) {
						dimensionKey = ALL_DIMENSION;
					} else {
						dimensionKey = new ResourceLocation(dimension);
					}

					if (!echelon.getModWhitelist().isEmpty()) {
						echelon.getModWhitelist().forEach(mod -> {
							// create a key pair
							Pair<ResourceLocation, String> keyPair = new ImmutablePair<>(dimensionKey, mod);
							if (!ECHELONS_BY_MOD.containsKey(keyPair)) {
								ECHELONS_BY_MOD.put(keyPair, echelon);
//								HISTOGRAM_TREES_BY_MOD.put(keyPair, tree);
							}
						});
					}
					else if (!echelon.getMobWhitelist().isEmpty()) {
						echelon.getMobWhitelist().forEach(mob -> {
							// create a key pair
							Pair<ResourceLocation, ResourceLocation> keyPair = new ImmutablePair<>(dimensionKey, new ResourceLocation(mob));
							if (!ECHELONS_BY_MOB.containsKey(keyPair)) {
								ECHELONS_BY_MOB.put(keyPair, echelon);
//								HISTOGRAM_TREES_BY_MOB.put(keyPair, tree);
							}
						});
					}
					else {
						ECHELONS.put(dimensionKey, echelon);
//						HISTOGRAM_TREES.put(dimensionKey, tree);
					}
				});
//			}
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

				// experience
				modifyXp(mob, echelonLevel, echelon.get());

				// update the capability
				cap.setLevel(echelonLevel);
			}
		});
	}

	private static void modifySpeed(Mob mob, Integer level, Echelon echelon) {		
		if (echelon.hasSpeedFactor()) {
			AttributeInstance attribute = mob.getAttribute(Attributes.MOVEMENT_SPEED);
			if (attribute != null) {
				double speed = 1.0 + (echelon.getSpeedFactor() * level);
				double newSpeed = attribute.getBaseValue() * speed;
				if (echelon.getMaxDamage() != null) {
					// TODO what if max speed <= 0
					newSpeed = Math.min(newSpeed, echelon.getMaxSpeed());
				}
				attribute.setBaseValue(newSpeed);
				//			EEchelons.LOGGER.debug("mob new speed -> {}", mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
			}	
		}
	}

	private static void modifyXp(Mob mob, Integer level, Echelon echelon) {
		if (echelon.hasXpFactor()) {
			double xp = 1.0 + (echelon.getXpFactor() * level);
			try {
				int xpReward = (int)ObfuscationReflectionHelper.getPrivateValue(Mob.class, mob, XP_REWARD_FIELDNAME);
				double newXpReward = xpReward * xp;
				if (echelon.getMaxXp() != null) {
					newXpReward = Math.min(newXpReward, echelon.getMaxXp());
				}
				ObfuscationReflectionHelper.setPrivateValue(Mob.class, mob, (int)newXpReward, XP_REWARD_FIELDNAME);
			} catch(UnableToAccessFieldException e	) {
				return;
			}
		}
	}

	private static void modifyHealth(Mob mob, int level, Echelon echelon) {
		if (echelon.hasHpFactor()) {
			AttributeInstance attribute = mob.getAttribute(Attributes.MAX_HEALTH);
			if (attribute != null) {
				double health = 1.0 + (echelon.getHpFactor() * level);
				double newHealth = mob.getMaxHealth() * health;
				if (echelon.getMaxHp() != null && echelon.getMaxHp() > 0.0) {
					newHealth = Math.min(newHealth, echelon.getMaxHp());
				}
				attribute.setBaseValue(newHealth);
				mob.setHealth(mob.getMaxHealth());
//				EEchelons.LOGGER.debug("mob new health -> {}", mob.getMaxHealth());
			}
		}
	}

	private static void modifyDamage(Mob mob, int level, Echelon echelon) {		
		if (echelon.hasDamageFactor()) {
			AttributeInstance attribute = mob.getAttribute(Attributes.ATTACK_DAMAGE);
			if (attribute != null) {
				double damage = 1.0 + (echelon.getDamageFactor() * level);
				double newDamage = attribute.getBaseValue() * damage;
				if (echelon.getMaxDamage() != null) {
					newDamage = Math.min(newDamage, echelon.getMaxDamage());
				}
				attribute.setBaseValue(newDamage);
//				EEchelons.LOGGER.debug("mob new damage -> {}", mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
			}
		}
	}

	private static void modifyArmor(Mob mob, Integer level, Echelon echelon) {
		if (echelon.hasArmorFactor()) {
			AttributeInstance attribute = mob.getAttribute(Attributes.ARMOR);
			if (attribute != null) {
				double armor = 1.0 + (echelon.getArmorFactor() * level);
				double newArmor = attribute.getBaseValue() * armor;
				if (echelon.getMaxArmor() != null) {
					newArmor = Math.min(newArmor, echelon.getMaxArmor());
				}
				attribute.setBaseValue(newArmor);
				//		EEchelons.LOGGER.debug("mob new armor -> {}", mob.getAttributeValue(Attributes.ARMOR));
			}
		}
	}

	private static void modifyArmorToughness(Mob mob, Integer level, Echelon echelon) {
		if (echelon.hasArmorToughnessFactor()) {
			AttributeInstance attribute = mob.getAttribute(Attributes.ARMOR_TOUGHNESS);
			if (attribute != null) {
				double armor = 1.0 + (echelon.getArmorToughnessFactor() * level);
				double newArmor = attribute.getBaseValue() * armor;
				if (echelon.getMaxArmorToughness() != null) {
					newArmor = Math.min(newArmor, echelon.getMaxArmorToughness());
				}
				attribute.setBaseValue(newArmor);
				//		EEchelons.LOGGER.debug("mob new armor toughness -> {}", mob.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
			}
		}
	}

	private static void modifyKnockback(Mob mob, int level, Echelon echelon) {
		if (echelon.hasKnockbackIncrement()) {
			AttributeInstance attribute = mob.getAttribute(Attributes.ATTACK_KNOCKBACK);
			if (attribute != null) {
				double knockback = echelon.getKnockbackIncrement() * level;
				double newKnockback = attribute.getBaseValue() + knockback;
				if (echelon.getMaxKnockback() != null) {
					newKnockback = Math.min(newKnockback, echelon.getMaxKnockback());
				}
				attribute.setBaseValue(newKnockback);
				//			EEchelons.LOGGER.debug("mob new knockback -> {}", mob.getAttributeValue(Attributes.ATTACK_KNOCKBACK));
			}
		}
	}

	private static void modifyKnockbackResist(Mob mob, int level, Echelon echelon) {
		if (echelon.hasKnockbackResistIncrement()) {
			AttributeInstance attribute = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
			if (attribute != null) {
				double knockback = echelon.getKnockbackResistIncrement() * level;
				double newKnockback = attribute.getBaseValue() + knockback;
				if (echelon.getMaxKnockbackResist() != null) {
					newKnockback = Math.min(newKnockback, echelon.getMaxKnockbackResist());
				}
				attribute.setBaseValue(newKnockback);
				//			EEchelons.LOGGER.debug("mob new knockback resist -> {}", mob.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
			}
		}
	}
}
