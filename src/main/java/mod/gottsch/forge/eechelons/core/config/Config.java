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
package mod.gottsch.forge.eechelons.core.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import mod.gottsch.forge.eechelons.EEchelonsApiMod;
import mod.gottsch.forge.gottschcore.config.AbstractConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Mark Gottschling on Jul 25, 2022
 *
 */
@EventBusSubscriber(modid = EEchelonsApiMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class Config extends AbstractConfig {
	public static final String CATEGORY_DIV = "##############################";
	public static final String UNDERLINE_DIV = "------------------------------";

	public static final ForgeConfigSpec COMMON_SPEC;
	public static final CommonConfig COMMON;

	public static Config instance = new Config();
	
	static {
		final Pair<CommonConfig, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder()
				.configure(CommonConfig::new);
		COMMON_SPEC = commonSpecPair.getRight();
		COMMON = commonSpecPair.getLeft();
	}

	public static class CommonConfig {
		public final Logging logging;
		public CommonConfig(ForgeConfigSpec.Builder builder) {
			logging = new Logging(builder);
		}
	}

	/**
	 * Echelons Config
	 */
	public static final ForgeConfigSpec ECHELONS_SPEC;
	public static final EchelonsFileConfig ECHELONS_CONFIG;
	/*
	 * list of echelon configurations
	 */
	public static List<EchelonConfigsHolder.Config> configs;

	static {
		final Pair<EchelonsFileConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder()
				.configure(EchelonsFileConfig::new);
		ECHELONS_SPEC = specPair.getRight();
		ECHELONS_CONFIG = specPair.getLeft();
	}

	/**
	 * class for the echelons_config_xxx_vX.toml config file.
	 */
	public static class EchelonsFileConfig {
		public EchelonConfigsHolder echelonConfigsHolder;

		public EchelonsFileConfig(ForgeConfigSpec.Builder builder) {
			// NOTE this define() name must match the wrapper property in the toml file.
			builder.define("loadOrder", 99);
			builder.define("configs", new ArrayList<>());
			builder.build();
		}
	}

	/**
	 * @param configData
	 * @return
	 */
	public static List<EchelonConfigsHolder.Config> transformEchelonConfigs(CommentedConfig configData) {
		// TODO separate out the conversion from the setting of Config properties so that the conversion process can be used elsewhere

		// convert the data to an object and set the holder in the _CONFIG
		// NOTE this field name MUST match the defined name in EchelonsFileConfig.
		// TODO deprecated setting of the holder to property in config
		ECHELONS_CONFIG.echelonConfigsHolder = new ObjectConverter().toObject(configData, EchelonConfigsHolder::new);
		// get the list from the holder and set the config property
		configs = ECHELONS_CONFIG.echelonConfigsHolder.configs;
		return configs;
	}

	/**
	 * Difficulty Naming Config
	 */
	public static final ForgeConfigSpec DIFFICULTY_SPEC;
	static {
		final Pair<DifficultyNamingFileConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder()
				.configure(DifficultyNamingFileConfig::new);
		DIFFICULTY_SPEC = specPair.getRight();
	}

	/**
	 * class for the echelons_difficulty_naming_config_xxx_vX.toml config file.
	 */
	public static class DifficultyNamingFileConfig {
		public NameConfigsHolder nameConfigsHolder;

		public DifficultyNamingFileConfig(ForgeConfigSpec.Builder builder) {
			// NOTE this define() name must match the wrapper property in the toml file.
			builder.define("nameConfigs", new ArrayList<>());
			builder.build();
		}
	}

	public static List<NameConfigsHolder.NameConfig> transformNameConfigs(CommentedConfig configData) {
		// convert the data to an object and set the holder in the _CONFIG
		NameConfigsHolder holder = new ObjectConverter().toObject(configData, NameConfigsHolder::new);
		return holder.nameConfigs;
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
