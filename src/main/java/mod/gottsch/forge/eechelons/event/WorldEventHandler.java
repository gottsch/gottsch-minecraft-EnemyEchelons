/*
 * This file is part of  Enemy Echelons.
 * Copyright (c) 2022, Mark Gottschling (gottsch)
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
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
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
		public static void onJoin(EntityJoinWorldEvent event) {

			Entity entity = event.getEntity();

			if (EchelonManager.isValidEntity(entity)) {
//				EEchelons.LOGGER.info("entity joining world -> {} : {}", entity.getName().getString(), entity.getId());
				/*
				 * if on the client, request an update from the server
				 */
				if (event.getWorld().isClientSide) {
					// get cap, ensure that level hasn't already been set.
					if (entity.getCapability(EEchelonsCapabilities.LEVEL_CAPABILITY).map(cap -> cap.getLevel() == -1).orElse(false)) {
						LevelRequestToServer message = new LevelRequestToServer(entity.getId(), entity.level.dimension().getRegistryName().toString(),
								entity.level.dimension().location().toString());
						EEchelonsNetwork.CHANNEL.sendToServer(message);
					}
				}
				else {
					MobEntity mob = (MobEntity)entity;
					EchelonManager.applyModications(mob);
				}
			}
		}
	}

}
