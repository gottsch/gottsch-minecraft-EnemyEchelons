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
public class LevelCapability implements ICapabilitySerializable<CompoundTag> {
	public static final ResourceLocation ID = new ResourceLocation(EEchelons.MODID, "level");
	
	// reference of handler/data for easy access
	private final LevelHandler handler = new LevelHandler();
	// holder of the handler/data
	private final LazyOptional<ILevelHandler> optional = LazyOptional
			.of(() -> handler);
	
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == EEchelonsCapabilities.LEVEL_CAPABILITY) {
			return optional.cast();
		}
		return LazyOptional.empty();
	}

	@Override
	public CompoundTag serializeNBT() {
		return (CompoundTag)handler.serializeNBT();
//		CompoundTag tag = new CompoundTag();
//		tag.putInt("level", handler.getLevel());
//		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		handler.deserializeNBT(tag);
//		if (tag.contains("level")) {
//			handler.setLevel(tag.getInt("level"));
//		}		
	}

	/**
	 * 
	 * @param event
	 */
	public static void register(RegisterCapabilitiesEvent event) {
		event.register(ILevelHandler.class);
	}

}
