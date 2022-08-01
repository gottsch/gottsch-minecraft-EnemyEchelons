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
package mod.gottsch.forge.eechelons.setup;

import mod.gottsch.forge.eechelons.EEchelons;
import mod.gottsch.forge.eechelons.integration.ChampionsIntegration;
import mod.gottsch.forge.eechelons.integration.WailaIntegration;
import mod.gottsch.forge.eechelons.network.EEchelonsNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 
 * @author Mark Gottschling on Jul 24, 2022
 *
 */
@Mod.EventBusSubscriber(modid = EEchelons.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonSetup {

	public static void init(final FMLCommonSetupEvent event) {
		EEchelonsNetwork.register();
		ChampionsIntegration.init();
		WailaIntegration.init();
	}	

}
