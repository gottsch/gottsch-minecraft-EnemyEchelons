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

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.collect.Maps;
import com.someguyssoftware.gottschcore.random.WeightedCollection;

import mod.gottsch.forge.eechelons.EEchelons;
import mod.gottsch.forge.eechelons.bst.Interval;
import mod.gottsch.forge.eechelons.bst.IntervalTree;
import mod.gottsch.forge.eechelons.capability.EEchelonsCapabilities;
import mod.gottsch.forge.eechelons.config.Config;
import mod.gottsch.forge.eechelons.config.EchelonsHolder.Echelon;
import mod.gottsch.forge.eechelons.network.EEchelonsNetwork;
import mod.gottsch.forge.eechelons.network.LevelMessageToClient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.PacketDistributor;

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
	 * map of echelons by dimension
	 */
	private static final Map<ResourceLocation, Echelon> ECHELONS = Maps.newHashMap();
	/*
	 * map of level-histogram interval tree (bst) by dimension
	 */
	private static final Map<ResourceLocation, IntervalTree<WeightedCollection<Double, Integer>>> HISTOGRAM_TREES = Maps.newHashMap();
	
	/**
	 * 
	 */
	public static void build() {
		ECHELONS.clear();
		HISTOGRAM_TREES.clear();
		
		List<Echelon> echelons = Config.echelons;
		if (ObjectUtils.isEmpty(echelons)) {
			return;
		}
		echelons.forEach(echelon -> {
			if (ObjectUtils.isEmpty(echelon.getStratum())) {
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
			
			if (ObjectUtils.isEmpty(echelon.getDimensions())) {
				ECHELONS.put(ALL_DIMENSION, echelon);
				HISTOGRAM_TREES.put(ALL_DIMENSION, tree);
			}
			else {
				// build
				echelon.getDimensions().forEach(dimension -> {
					ResourceLocation key;
					if (dimension.equals(".")) {
						key = ALL_DIMENSION;
					}else {
						key = new ResourceLocation(dimension);
					}
					ECHELONS.put(key, echelon);
					HISTOGRAM_TREES.put(key, tree);
				});
			}
		});
	}
	
	public static Echelon getEchelon(ResourceLocation key) {
		if (ECHELONS.containsKey(key)) {
			return ECHELONS.get(key);
		}
		return null;
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

			List<Interval<WeightedCollection<Double, Integer>>> stratum = tree
					.getOverlapping(tree.getRoot(), new Interval<>(searchValue, searchValue), false);
			
			if (ObjectUtils.isEmpty(stratum)) {
				return 0;
			}
			
			// get the first element/strata - there should only be one.
			WeightedCollection<Double, Integer> col = stratum.get(0).getData();
			if (ObjectUtils.isEmpty(col)) {
				return 0;
			}
			// get the next weighted random integer
			result = col.next();			
		}
		return result;
	}

	/**
	 * 
	 * @param entity
	 * @return
	 */
	public static boolean isValidEntity(final Entity entity) {
		return entity instanceof LivingEntity && entity instanceof Enemy
				&& isValidEntity(entity.getLevel().dimension().location(), entity);
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
	public static void applyModications(Mob mob) {
		mob.getCapability(EEchelonsCapabilities.LEVEL_CAPABILITY).ifPresent(cap -> {
			
//			EEchelons.LOGGER.info("mob max health -> {}", mob.getMaxHealth());
//			EEchelons.LOGGER.info("mob current health -> {}", mob.getHealth());
//			EEchelons.LOGGER.info("mob current level -> {}", cap.getLevel());
			
			if (cap.getLevel() < 0) {
				// determine dimension
				ResourceLocation dimension = mob.getLevel().dimension().location();
				// determine the altitute (y-value)
				int y = mob.getBlockY();

				Integer echelonLevel = EchelonManager.getLevel(dimension, y);

				EEchelons.LOGGER.info("selected level -> {} for dimension -> {} @ y -> {}", echelonLevel, dimension, y);

				/*
				 *  apply the attribute modifications
				 */
				Echelon echelon = getEchelon(dimension);
				if (echelon == null) {
					cap.setLevel(0);
					return;
				}
				
				// health
				modifyHealth(mob, echelonLevel, echelon);

				// damage
				modifyDamage(mob, echelonLevel, echelon);
				
				// speed
				modifySpeed(mob, echelonLevel, echelon);
				
				// experience
				modifyXp(mob, echelonLevel, echelon);
				
				// update the capability
				cap.setLevel(echelonLevel);
			}
		});
	}
	
	private static void modifySpeed(Mob mob, Integer level, Echelon echelon) {
		double speed = 1.0 + (echelon.getSpeedFactor() * level);
		double newSpeed = mob.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() * speed;
		if (echelon.getMaxDamage() != null) {
			newSpeed = Math.min(newSpeed, echelon.getMaxSpeed());
		}
		mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(newSpeed);
	}

	private static void modifyXp(Mob mob, Integer level, Echelon echelon) {
		double xp = 1.0 + (echelon.getXpFactor() * level);
		int xpReward = (int)ObfuscationReflectionHelper.getPrivateValue(Mob.class, mob, XP_REWARD_FIELDNAME);
		double newXpReward = xpReward * xp;
		if (echelon.getMaxXp() != null) {
			newXpReward = Math.min(newXpReward, echelon.getMaxXp());
		}
		ObfuscationReflectionHelper.setPrivateValue(Mob.class, mob, (int)newXpReward, XP_REWARD_FIELDNAME);
	}

	private static void modifyHealth(Mob mob, int level, Echelon echelon) {
		double health = 1.0 + (echelon.getHpFactor() * level);
		double newHealth = mob.getMaxHealth() * health;
		if (echelon.getMaxHp() != null) {
			newHealth = Math.min(newHealth, echelon.getMaxHp());
		}
		mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newHealth);
		mob.setHealth(mob.getMaxHealth());
	}
	
	private static void modifyDamage(Mob mob, int level, Echelon echelon) {
		double damage = 1.0 + (echelon.getDamageFactor() * level);
		double newDamage = mob.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * damage;
		if (echelon.getMaxDamage() != null) {
			newDamage = Math.min(newDamage, echelon.getMaxDamage());
		}
		mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(newDamage);
	}
}
