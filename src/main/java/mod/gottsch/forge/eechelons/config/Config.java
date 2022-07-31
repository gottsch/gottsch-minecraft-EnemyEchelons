/*
 * This file is part of  Mevels.
 * Copyright (c) 2022, Mark Gottschling (gottsch)
 * 
 * All rights reserved.
 *
 * Mevels is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mevels is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Mevels.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.eechelons.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;

import mod.gottsch.forge.eechelons.EEchelons;
import mod.gottsch.forge.eechelons.config.EchelonsHolder.Echelon;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * 
 * @author Mark Gottschling on Jul 25, 2022
 *
 */
@EventBusSubscriber(modid = EEchelons.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Config {
	public static final String CATEGORY_DIV = "##############################";
	public static final String UNDERLINE_DIV = "------------------------------";

	public static final ForgeConfigSpec SERVER_SPEC;
	public static final ServerConfig SERVER;

	static {
		final Pair<ServerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder()
				.configure(ServerConfig::new);
		SERVER_SPEC = specPair.getRight();
		SERVER = specPair.getLeft();
	}

	/**
	 * For general mod config options
	 *
	 */
	// TODO this probably could just be ClientConfig or CommonConfig
	public static class ServerConfig {
		public final BooleanValue showHud;
		public final IntValue hudXOffset;
	    public final IntValue hudYOffset;
	    public final IntValue hudRange;
	    
		public ServerConfig(ForgeConfigSpec.Builder builder) {
			builder.push("general");
			builder.pop();

			builder.push("hud");
			
			// TODO showHud remains in server config so server admin can determine if users are able to see the level or not.
			showHud = builder
					.comment("Enable HUD display.")
//			        .translation("showHud")
			        .define("showHud", true);
			
		      hudXOffset = builder
		    		  .comment("The HUD x-offset.")
//		    	      .translation("hudXOffset")
		    	      .defineInRange("hudXOffset", 0, -1000, 1000);

    	      hudYOffset = builder
    	    		  .comment("The HUD y-offset.")
//		    	    		  .translation("hudYOffset")
    	    		  .defineInRange("hudYOffset", 0, -1000, 1000);

    	      hudRange = builder
    	    		  .comment("The distance that the HUD can be seen from (in blocks).")
//		    	    		  .translation(CONFIG_PREFIX + "hudRange")
    	    		  .defineInRange("hudRange", 50, 0, 100);
		    	      
			builder.pop();
		}
	}

	/**
	 * Echelons Config
	 */
	public static final ForgeConfigSpec ECHELONS_SPEC;
	public static final EchelonsConfig ECHELONS_CONFIG;
	/*
	 * list of echelon configurations
	 */
	public static List<Echelon> echelons;
	
	static {
		final Pair<EchelonsConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder()
				.configure(EchelonsConfig::new);
		ECHELONS_SPEC = specPair.getRight();
		ECHELONS_CONFIG = specPair.getLeft();
	}
	
	public static class EchelonsConfig {
		public EchelonsHolder echelonsHolder;
		public EchelonsConfig(ForgeConfigSpec.Builder builder) {
			builder.comment(" list of echelons").define("echelons", new ArrayList<>());
			builder.build();
		}
	}

	/**
	 * 
	 * @param configData
	 */
	public static void transformEchelons(CommentedConfig configData) {
		// convert the data to an object and set the holder in the _CONFIG
	    ECHELONS_CONFIG.echelonsHolder = new ObjectConverter().toObject(configData, EchelonsHolder::new);
//	    Mevels.LOGGER.info("config.echelons -> {}", ECHELONS_CONFIG.echelonsHolder.echelons);
	    // get the list from the holder and set the config property
	    echelons = ECHELONS_CONFIG.echelonsHolder.echelons;
	}
}
