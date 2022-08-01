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
package mod.gottsch.forge.eechelons.capability;


import mod.gottsch.forge.eechelons.EEchelons;
import mod.gottsch.forge.eechelons.echelon.EchelonManager;
import mod.gottsch.forge.eechelons.integration.ChampionsIntegration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * 
 * @author Mark Gottschling on Jul 24, 2022
 *
 */
@Mod.EventBusSubscriber(modid = EEchelons.MODID, bus = Bus.MOD)
public class EEchelonsCapabilities {

	public static Capability<ILevelHandler> LEVEL_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {	});

	/**
	 * 
	 */
	@SubscribeEvent
	public static void register(final RegisterCapabilitiesEvent event) {
		LevelCapability.register(event);
	}

	/**
	 * Forge Bus Event Subscriber class
	 */
	@Mod.EventBusSubscriber(modid = EEchelons.MODID, bus = EventBusSubscriber.Bus.FORGE)
	public static class ForgeBusSubscriber {
		/*
		 * NOTE called before entity is spawned in world
		 */
		@SubscribeEvent
		public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
			Entity entity = event.getObject();
			if (EchelonManager.isValidEntity(entity)) {
				if (ChampionsIntegration.isEnabled() && ChampionsIntegration.hasCapability(event)) {
					return;
				}
				event.addCapability(LevelCapability.ID, new LevelCapability());
			}
		}
	}
}
