/*
 * This file is part of  Enemy Echelons.
 * Copyright (c) 2022 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.eechelons;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import mod.gottsch.forge.eechelons.core.config.Config;

import mod.gottsch.forge.eechelons.core.setup.ClientSetup;
import mod.gottsch.forge.eechelons.core.setup.CommonSetup;
import mod.gottsch.forge.eechelons.core.setup.Registration;
import mod.gottsch.forge.eechelonsapi.core.config.EchelonConfigsHolder;
import mod.gottsch.forge.eechelonsapi.core.config.NameConfigsHolder;
import mod.gottsch.forge.eechelonsapi.core.echelon.EchelonManager;
import mod.gottsch.forge.eechelonsapi.core.registry.DifficultyNameRegistry;
import mod.gottsch.forge.eechelonsapi.core.registry.DifficultyNameRegistryEntry;
import mod.gottsch.forge.gottschcore.GottschCore;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Many thanks go out to TheIllusiveC4 as the EchelonConfig 
 * loading code was derived from Champions.
 * @see <a href="https://github.com/TheIllusiveC4/Champions">Champions</a>
 *
 * @author Mark Gottschling on Jul 24, 2022
 *
 */
@Mod(EEchelons.MOD_ID)
public class EEchelons {
	public static final Logger LOGGER = LogManager.getLogger(EEchelons.MOD_ID);

	public static final String MOD_ID = "eechelons";

	private static final String ECHELONS_CONFIG_VERSION = "1.20.1_v4";
	private static final String DEFAULT_CONFIG_FOLDER = "defaultconfigs";
	private static final String EE_SUBFOLDER = "enemy_echelons";

	private static int configsLoaded = 0;

	/**
	 *
	 */
	public EEchelons() {
		// register the deferred registries
		Registration.init();
		// register the server config
		ModLoadingContext.get().registerConfig(Type.CLIENT, Config.CLIENT_SPEC);
		ModLoadingContext.get().registerConfig(Type.COMMON, Config.COMMON_SPEC);
		ModLoadingContext.get().registerConfig(Type.SERVER, Config.SERVER_SPEC);

		// create the default config
		copyToServerDefaultConfig(EEchelons.class, Config.ECHELONS_SPEC,
				getConfigFilename(ECHELONS_CONFIG_VERSION));
		copyToServerDefaultConfig(EEchelons.class, Config.ECHELONS_SPEC,
				getCustomConfigFilename(ECHELONS_CONFIG_VERSION));
		copyToConfig(EEchelons.class, Config.DIFFICULTY_SPEC,
				getDifficultyNamingConfigFilename(ECHELONS_CONFIG_VERSION));

		// register the setup method for mod loading
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		// register 'ModSetup::init' to be called at mod setup time (server and client)
		modEventBus.addListener(CommonSetup::init);
		modEventBus.addListener(this::onLoadConfig);

		// register 'ClientSetup::init' to be called at mod setup time (client only)
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modEventBus.addListener(ClientSetup::init));
	}


	// NOTE can't move to GottschCore
	public void copyToServerDefaultConfig(Class<?> sourceClass, ForgeConfigSpec spec, String resourceFilename) {

		ModLoadingContext.get().registerConfig(Type.SERVER, spec, getConfigSubfolder(resourceFilename).toString());

		// NOTE cannot have FMLPath.GAMEDIR.get() in GottschCore for some reason.
		File defaults = new File(FMLPaths.GAMEDIR.get() + "/defaultconfigs/" + getConfigSubfolder(resourceFilename));

		if (!defaults.exists()) {
			try {
				FileUtils.copyInputStreamToFile(
						Objects.requireNonNull(sourceClass.getClassLoader().getResourceAsStream(resourceFilename)),
						defaults);
			} catch (IOException e) {
				EEchelons.LOGGER.error("error copying to default config -> {}", resourceFilename);
			}
		}
	}

	public void copyToConfig(Class<?> sourceClass, ForgeConfigSpec spec, String resourceFilename) {
		ModLoadingContext.get().registerConfig(Type.COMMON, spec, getConfigSubfolder(resourceFilename).toString());

		File defaults = FMLPaths.CONFIGDIR.get().resolve(getConfigSubfolder(resourceFilename)).toFile();

		if (!defaults.exists()) {
			try {
				FileUtils.copyInputStreamToFile(
						Objects.requireNonNull(sourceClass.getClassLoader().getResourceAsStream(resourceFilename)),
						defaults);
            } catch (IOException e) {
				LOGGER.error("Error creating common config for " + resourceFilename);
			}
		}
	}

	/**
	 * On a config event.
	 * @param event
	 */
	private void onLoadConfig(final ModConfigEvent event, List<EchelonConfigsHolder.Config> configs) {
		if (event.getConfig().getModId().equals(MOD_ID)) {
			if (event.getConfig().getType() == Type.COMMON) {
				IConfigSpec<?> spec = event.getConfig().getSpec();

				if (spec == Config.DIFFICULTY_SPEC) {
					// get the toml config data
					CommentedConfig commentedConfig = event.getConfig().getConfigData();
					List<NameConfigsHolder.NameConfig> configs = Config.transformNameConfigs(commentedConfig);
					List<DifficultyNameRegistryEntry> entries = configs.stream().map(DifficultyNameRegistryEntry::new).toList();
					DifficultyNameRegistry.register(entries);
				}
			}

			if (event.getConfig().getType() == Type.SERVER) {
				IConfigSpec<?> spec = event.getConfig().getSpec();
				// get the toml config data
				CommentedConfig commentedConfig = event.getConfig().getConfigData();

				if (spec == Config.ECHELONS_SPEC) {
					// clear the EchelonManager.
					// NOTE only EEchelons should do this,
					// all other mods should only add to the manager.
					if (configsLoaded == 0) {
						EchelonManager.REGISTRY.clear();
					}

					// TODO all echelon configs should be converted to objects first, then sorted by load order, then register()/build()

					configsLoaded++;
					// transform/copy the toml into the config
					List<EchelonConfigsHolder.Config> configs = Config.transformEchelonConfigs(commentedConfig);

					// TODO make this an API call
					// TODO review loading of config file. ALL configs need to be registered separately
					// and the mobs need to be updated

					// pass the EchelonConfigs to the build
					EchelonManager.REGISTRY.register(configs);

					// TODO don't like this way of controlling when to load configs.
					if (configsLoaded == 2) {
						loadAdditionalConfigs(event);
					}
				}
			}
		}
	}

	private void loadAdditionalConfigs(final ModConfigEvent event) {
		Path folder = event.getConfig().getFullPath().getParent();

		// Get all files recursively
		Collection<File> files = FileUtils.listFiles(folder.toFile(), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		files.stream()
				.filter(File::isFile) // filter out directories, keep only files - this might be redundant with the FileUtils.
				.filter(file -> file.getName().endsWith(".toml")) // Filter by extension (.txt in this example)
				.filter(f -> !f.getName().equals(getConfigFilename(ECHELONS_CONFIG_VERSION))
						&& !f.getName().equals(getCustomConfigFilename(ECHELONS_CONFIG_VERSION)))
				.forEach(f -> {
					System.out.println(f.getAbsolutePath());
					// create a CommentedFileConfig instance
					CommentedFileConfig configData = CommentedFileConfig.builder(f)
							.sync() // Automatically synchronize changes to file
							.autosave() // Automatically save changes
							.build();
					configData.load(); // Load the file content

					EchelonConfigsHolder holder = new ObjectConverter().toObject(configData, EchelonConfigsHolder::new);
					// build/register config file
					EchelonManager.REGISTRY.register(holder.configs);
				});
	}

	private String getConfigFilename(String version) {
		return "echelons_config_" + version + ".toml";
	}

	private String getCustomConfigFilename(String version) {
		return "echelons_custom_config_" + version + ".toml";
	}

	private String getDifficultyNamingConfigFilename(String version) {
		return "echelons_difficulty_naming_config_" + version + ".toml";
	}

	private Path getConfigSubfolder(String filename) {
		return Paths.get(EE_SUBFOLDER).resolve(filename);
	}

}
