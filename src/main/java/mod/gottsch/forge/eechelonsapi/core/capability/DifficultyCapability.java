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
package mod.gottsch.forge.eechelonsapi.core.capability;

import mod.gottsch.forge.eechelonsapi.EEchelonsApiMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;

/**
 * 
 * @author Mark Gottschling on Jul 24, 2022
 *
 */
public class DifficultyCapability implements ICapabilitySerializable<CompoundTag> {
	public static final ResourceLocation ID = new ResourceLocation(EEchelonsApiMod.MOD_ID, "level");
	
	// reference of handler/data for easy access
	private final DifficultyHandler handler = new DifficultyHandler();
	// holder of the handler/data
	private final LazyOptional<IDifficultyHandler> optional = LazyOptional
			.of(() -> handler);
	
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == ModCapabilities.DIFFICULTY_CAPABILITY) {
			return optional.cast();
		}
		return LazyOptional.empty();
	}

	@Override
	public CompoundTag serializeNBT() {
		return (CompoundTag)handler.serializeNBT();
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		handler.deserializeNBT(tag);
	}

	// invalidate
	public void invalidate() {
		optional.invalidate();
	}

	/**
	 * 
	 * @param event
	 */
	public static void register(RegisterCapabilitiesEvent event) {
		event.register(IDifficultyHandler.class);
	}

}
