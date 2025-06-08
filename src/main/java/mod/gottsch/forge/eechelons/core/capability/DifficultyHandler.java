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
package mod.gottsch.forge.eechelons.core.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * 
 * @author Mark Gottschling on Jul 24, 2022
 *
 */
public class DifficultyHandler implements IDifficultyHandler, INBTSerializable<Tag> {
	private int difficulty = -1;
	private String name = "";

	@Override
	public int getDifficulty() {
		return difficulty;
	}

	@Override
	public void setDifficulty(int level) {
		this.difficulty = level;
	}

	@Override
	public String getName() {
		if (name == null) name = "";
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Tag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("level", getDifficulty());
		if (getName() != null) {
			tag.putString("name", getName());
		}
		return tag;
	}

	@Override
	public void deserializeNBT(Tag tag) {
		if (tag instanceof CompoundTag) {
			CompoundTag ctag = (CompoundTag)tag;
			if (ctag.contains("level")) {
				setDifficulty(ctag.getInt("level"));
			}
			else {
				setDifficulty(-1);
			}
			if (ctag.contains("name")) {
				setName(ctag.getString("name"));
			}
		}

	}

}
