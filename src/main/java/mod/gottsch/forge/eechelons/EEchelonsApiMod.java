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
package mod.gottsch.forge.eechelons;

import mod.gottsch.forge.eechelons.core.config.Config;
import mod.gottsch.forge.eechelons.core.setup.CommonSetup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Mark Gottschling on Jul 24, 2022
 *
 */
@Mod(EEchelonsApiMod.MOD_ID)
public class EEchelonsApiMod {
	public static final Logger LOGGER = LogManager.getLogger(EEchelonsApiMod.MOD_ID);

	public static final String MOD_ID = "eechelonsapi";

	/**
	 *
	 */
	public EEchelonsApiMod() {
		// register the server config
		ModLoadingContext.get().registerConfig(Type.COMMON, Config.COMMON_SPEC);

		// register the setup method for mod loading
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		// register 'ModSetup::init' to be called at mod setup time (server and client)
		modEventBus.addListener(CommonSetup::init);
	}
}
