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
package mod.gottsch.forge.eechelons.core.event;

import mod.gottsch.forge.eechelons.EEchelonsApiMod;
import mod.gottsch.forge.eechelons.core.capability.ModCapabilities;
import mod.gottsch.forge.eechelons.core.config.EchelonConfigsHolder;
import mod.gottsch.forge.eechelons.core.echelon.EchelonManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.Optional;

/**
 * 
 * @author Mark Gottschling on Jul 31, 2022
 *
 */
public class WorldEventHandler {

	/**
	 * Forge Bus Event Subscriber class
	 */
	@Mod.EventBusSubscriber(modid = EEchelonsApiMod.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
	public static class ForgeBusSubscriber {

		/**
		 * Handles the LivingExperienceDropEvent to modify XP based on Echelon.
		 * @param event The event.
		 */
		@SubscribeEvent
		public static void onExperienceDrop(LivingExperienceDropEvent event) {
			Entity entity = event.getEntity();
			if (!(entity instanceof Mob mob)) {
				return;
			}

			Optional<EchelonConfigsHolder.Config> echelonOpt = EchelonManager.REGISTRY.getEchelonConfig(mob);
			if (echelonOpt.isEmpty()) {
				return;
			}

			EchelonConfigsHolder.Config echelon = echelonOpt.get();

			if (echelon.hasXpFactor()) {
				// Get the echelon level from the capability
				mob.getCapability(ModCapabilities.DIFFICULTY_CAPABILITY).ifPresent(cap -> {
					int echelonLevel = cap.getDifficulty();
					if (echelonLevel < 0) {
						// difficulty level not yet calculated, or calculation failed.
						// for now, we'll skip XP modification.
						// alternatively, could trigger EchelonManager.applyModifications(mob) here
						// or use a default level.
						return;
					}

					double xpFactor = 1.0 + (echelon.getXpFactor() * echelonLevel);
					double newXpReward = event.getOriginalExperience() * xpFactor;

					if (echelon.getMaxXp() != null) {
						newXpReward = Math.min(newXpReward, echelon.getMaxXp());
					}

					event.setDroppedExperience((int) newXpReward);
				});
			}
		}
	}

}
