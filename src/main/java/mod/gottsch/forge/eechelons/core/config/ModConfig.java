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
package mod.gottsch.forge.eechelons.core.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import mod.gottsch.forge.eechelons.EEchelons;
import mod.gottsch.forge.eechelonsapi.core.config.EchelonConfigsHolder;
import mod.gottsch.forge.eechelonsapi.core.config.NameConfigsHolder;
import mod.gottsch.forge.gottschcore.config.AbstractConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Mark Gottschling on Jul 25, 2022
 *
 */
// TODO need to extends the API config
@EventBusSubscriber(modid = EEchelons.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModConfig extends AbstractConfig {
	public static final String CATEGORY_DIV = "##############################";
	public static final String UNDERLINE_DIV = "------------------------------";

	public static final ForgeConfigSpec SERVER_SPEC;
	public static final ServerConfig SERVER;

	public static final ForgeConfigSpec COMMON_SPEC;
	public static final CommonConfig COMMON;
	
	public static final ForgeConfigSpec CLIENT_SPEC;
	public static final ClientConfig CLIENT;

	public static ModConfig instance = new ModConfig();
	
	static {
		final Pair<ServerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder()
				.configure(ServerConfig::new);
		SERVER_SPEC = specPair.getRight();
		SERVER = specPair.getLeft();
		
		final Pair<CommonConfig, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder()
				.configure(CommonConfig::new);
		COMMON_SPEC = commonSpecPair.getRight();
		COMMON = commonSpecPair.getLeft();

		final Pair<ClientConfig, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder()
				.configure(ClientConfig::new);
		CLIENT_SPEC = clientSpecPair.getRight();
		CLIENT = clientSpecPair.getLeft();
	}

	public static class CommonConfig {
		public final Logging logging;
		public CommonConfig(ForgeConfigSpec.Builder builder) {
			logging = new Logging(builder);
		}
	}
	
	public static class ClientConfig {
		public final BooleanValue showHud;
		public final BooleanValue showLevel0Hud;
		public final IntValue hudXOffset;
		public final IntValue hudYOffset;
		public final BooleanValue useDarkHud;
		
		public final ForgeConfigSpec.BooleanValue enableWailaIntegration;
		public final ForgeConfigSpec.BooleanValue enableChampionsIntegration;

		public ClientConfig(ForgeConfigSpec.Builder builder) {
			builder.push("hud");

			// show the hud, but only if server.show=true. ie, if server hud=false, client cannot override and display hud.
		  showHud = builder
							.comment(" Enable HUD display.")
							.define("showHud", true);

		  showLevel0Hud = builder
				  .comment(" Enable HUD display when mob is at Level 0")
				  .define("showLevel0Hud", true);

				hudXOffset = builder
					.comment(" The HUD x-offset.")
					.defineInRange("hudXOffset", 0, -1000, 1000);

			hudYOffset = builder
					.comment(" The HUD y-offset.")
					.defineInRange("hudYOffset", 0, -1000, 1000);

			useDarkHud = builder
					.comment(" Use dark theme HUD.")
					.define("useDarkThemeHud", true);
			
			enableWailaIntegration =
					builder.comment(" Moves the Enemy Echelons HUD beside (to the left) of the WAILA HUD.",
							" This setting is ignored if hudXOffset or hudYOffset are set (not 0).")
					.define("enableWailaIntegration", true);

			enableChampionsIntegration =
					builder.comment(" Moves the Enemy Echelons HUD beside (to the left) of the Champions HUD.",
							" This setting will supercede enableWailaIntegration.",
							" This setting is ignored if hudXOffset or hudYOffset are set (not 0).")
					.define("enableChampionsIntegration", true);
			
			builder.pop();
		}
	}

	/**
	 * For server mod config options
	 *
	 */
	public static class ServerConfig {
		public final BooleanValue showHud;
		public final BooleanValue hudRangeEnabled;
		public final IntValue hudRange;

		public ServerConfig(ForgeConfigSpec.Builder builder) {
			builder.push("general");
			builder.pop();

			builder.push("hud");

			// showHud remains in server config so server admin can determine if users are able to see the level or not.
			showHud = builder
					.comment(" Enable HUD display.")
					.define("showHud", true);

			hudRangeEnabled = builder
					.comment(" Enable custom HUD range.",
							" NOTE enabling is more computationally expensive on the client side.")
					.define("hudRangeEnabled", false);

			hudRange = builder
					.comment(" The distance that the HUD can be seen from (in blocks).",
							" Vanilla default = 3.")
					.defineInRange("hudRange", 3, 0, 100);

			builder.pop();
		}
	}

	@Override
	public String getLogsFolder() {
		return COMMON.logging.folder.get();
	}
	
	public void setLogsFolder(String folder) {
		COMMON.logging.folder.set(folder);
	}
	
	@Override
	public String getLoggingLevel() {
		return COMMON.logging.level.get();
	}
}
