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
package mod.gottsch.forge.eechelons.integration;

import mod.gottsch.forge.eechelons.config.Config;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.ModList;
//import top.theillusivec4.champions.common.capability.ChampionCapability;

/**
 * 
 * @author Mark Gottschling on Jul 31, 2022
 *
 */
public class ChampionsIntegration {
	private static boolean championsLoaded = false;
	
	public static void init() {
		ModList modList = ModList.get();

		if (modList.isLoaded("champions")) {
			championsLoaded = true;
		}
	}

	public static boolean isEnabled() {
		return Config.CLIENT.enableChampionsIntegration.get() && championsLoaded;
	}

	public static boolean hasCapability(AttachCapabilitiesEvent<Entity> event) {
		return false;
//		return event.getCapabilities().get(ChampionCapability.ID) != null;
	}

}
