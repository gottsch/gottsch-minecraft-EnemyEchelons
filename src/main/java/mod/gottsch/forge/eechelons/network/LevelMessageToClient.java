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
package mod.gottsch.forge.eechelons.network;

import java.util.function.Supplier;

import mod.gottsch.forge.eechelons.EEchelons;
import mod.gottsch.forge.eechelons.capability.EEchelonsCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

/**
 * 
 * @author Mark Gottschling on Jul 28, 2022
 *
 */
public class LevelMessageToClient {
	private final int entityId;
	private final int level;
	
	public LevelMessageToClient(int entityId, int level) {
		this.entityId = entityId;
		this.level = level;
	}
	
	public static void encode(LevelMessageToClient msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.entityId);
		buf.writeInt(msg.level);
	}
	
	public static LevelMessageToClient decode(FriendlyByteBuf buf) {
		int entityId = buf.readInt();
		int level = buf.readInt();
	    return new LevelMessageToClient(entityId, level);
	}
	
	public static void handle(LevelMessageToClient msg, Supplier<NetworkEvent.Context> context) {
//		EEchelons.LOGGER.debug("received message -> {}", msg);
		NetworkEvent.Context ctx = context.get();
		LogicalSide sideReceived = ctx.getDirection().getReceptionSide();

		if (sideReceived != LogicalSide.CLIENT) {
			EEchelons.LOGGER.warn("LevelMessageToClient received on wrong side -> {}", ctx.getDirection().getReceptionSide());
			return;
		}
		
		context.get().enqueueWork(() -> {
			ClientLevel world = Minecraft.getInstance().level;
			if (world != null) {
				Entity entity = world.getEntity(msg.entityId);
//				EEchelons.LOGGER.debug("handling client message to entity -> {} for level -> {}", entity.getName().getString(), msg.level);
				entity.getCapability(EEchelonsCapabilities.LEVEL_CAPABILITY).ifPresent(cap -> {
//					EEchelons.LOGGER.debug("setting the level on the client entity");
					cap.setLevel(msg.level);
				});
			}
		});
		context.get().setPacketHandled(true);
	}

	@Override
	public String toString() {
		return "LevelMessageToClient [entityId=" + entityId + ", level=" + level + "]";
	}

}
