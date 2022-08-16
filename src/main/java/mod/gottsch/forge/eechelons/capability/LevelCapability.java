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
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

/**
 * 
 * @author Mark Gottschling on Jul 24, 2022
 *
 */
public class LevelCapability implements ICapabilitySerializable<CompoundNBT> {
	public static final ResourceLocation ID = new ResourceLocation(EEchelons.MODID, "level");
	
	// holder of the handler/data
	private final LazyOptional<ILevelHandler> handler = LazyOptional
			.of(EEchelonsCapabilities.LEVEL_CAPABILITY::getDefaultInstance);

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == EEchelonsCapabilities.LEVEL_CAPABILITY) {
			return handler.cast();
		}
		return LazyOptional.empty();
	}

	@Override
	public CompoundNBT serializeNBT() {
		return (CompoundNBT)EEchelonsCapabilities.LEVEL_CAPABILITY.getStorage().writeNBT(EEchelonsCapabilities.LEVEL_CAPABILITY,
				handler.orElseThrow(() -> new IllegalArgumentException("at serialize")), null);
	}

	@Override
	public void deserializeNBT(CompoundNBT nbt) {
		EEchelonsCapabilities.LEVEL_CAPABILITY.getStorage().readNBT(EEchelonsCapabilities.LEVEL_CAPABILITY,
				handler.orElseThrow(() -> new IllegalArgumentException("at deserialize")), null, nbt);

	}

	/**
	 * 
	 */
	public static void register() {
		CapabilityManager.INSTANCE.register(ILevelHandler.class, new IStorage<ILevelHandler>() {

			@Override
			public INBT writeNBT(Capability<ILevelHandler> capability, ILevelHandler instance, Direction side) {
				CompoundNBT tag = new CompoundNBT();
				tag.putInt("level", instance.getLevel());
				return tag;
			}

			@Override
			public void readNBT(Capability<ILevelHandler> capability, ILevelHandler instance, Direction side,
					INBT nbt) {
				if (nbt instanceof CompoundNBT) {
					CompoundNBT tag = (CompoundNBT)nbt;
					if (tag.contains("level")) {
						instance.setLevel(tag.getInt("level"));
					}
					else {
						instance.setLevel(-1);
					}
				}
			}
		}, LevelHandler::new);
	}
}
