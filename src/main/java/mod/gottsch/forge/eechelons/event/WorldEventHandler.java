/*
 * This file is part of  Enemy Echelons.
 * Copyright (c) 2022 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.eechelons.event;

import mod.gottsch.forge.eechelons.EEchelons;
import mod.gottsch.forge.eechelons.capability.EEchelonsCapabilities;
import mod.gottsch.forge.eechelons.echelon.EchelonManager;
import mod.gottsch.forge.eechelons.network.EEchelonsNetwork;
import mod.gottsch.forge.eechelons.network.LevelRequestToServer;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity; // Added
import net.minecraft.world.entity.Entity; // Added
import java.util.Optional; // Added
import mod.gottsch.forge.eechelons.capability.EEchelonsCapabilities; // Added
import mod.gottsch.forge.eechelons.config.EchelonsHolder; // Added
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent; // Added
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * 
 * @author Mark Gottschling on Jul 31, 2022
 *
 */
public class WorldEventHandler {

	/**
	 * Forge Bus Event Subscriber class
	 */
	@Mod.EventBusSubscriber(modid = EEchelons.MODID, bus = EventBusSubscriber.Bus.FORGE)
	public static class ForgeBusSubscriber {

		/**
		 * 
		 * @param event
		 */
		@SubscribeEvent
		public static void onJoin(EntityJoinLevelEvent event) {

			Entity entity = event.getEntity();

			if (EchelonManager.isValidEntity(entity)) {
//				EEchelons.LOGGER.debug("entity joining world -> {} : {}", entity.getName().getString(), entity.getId());
				/*
				 * if on the client, request an update from the server
				 */
				if (WorldInfo.isClientSide(event.getEntity().level())) {
					// get cap, ensure that level hasn't already been set.
					if (entity.getCapability(EEchelonsCapabilities.LEVEL_CAPABILITY).map(cap -> cap.getLevel() == -1).orElse(false)) {
						LevelRequestToServer message = new LevelRequestToServer(entity.getId(), entity.level().dimension().location().toString(),
								entity.level().dimension().location().toString());
						EEchelonsNetwork.CHANNEL.sendToServer(message);
					}
				}
				else {
					Mob mob = (Mob)entity;
					EchelonManager.applyModications(mob);
				}
			}
		}
		
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

			Optional<mod.gottsch.forge.eechelons.config.EchelonsHolder.Echelon> echelonOpt = EchelonManager.getEchelon(mob);
			if (echelonOpt.isEmpty()) {
				return;
			}

			mod.gottsch.forge.eechelons.config.EchelonsHolder.Echelon echelon = echelonOpt.get();

			if (echelon.hasXpFactor()) {
				// Get the echelon level from the capability
				mob.getCapability(EEchelonsCapabilities.LEVEL_CAPABILITY).ifPresent(cap -> {
					int echelonLevel = cap.getLevel();
					if (echelonLevel < 0) {
						// Level not yet calculated, or calculation failed.
						// For now, we'll skip XP modification.
						// Alternatively, could trigger EchelonManager.applyModifications(mob) here
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
