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
package mod.gottsch.forge.eechelons.core.network;

import java.util.function.Supplier;

import mod.gottsch.forge.eechelons.EEchelonsApiMod;
import mod.gottsch.forge.eechelons.core.capability.ModCapabilities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.PacketDistributor;

/**
 * 
 * @author Mark Gottschling on Jul 30, 2022
 *
 */
public class DifficultyRequestToServer {
	private final int entityId;
	private final String registryName;
	private final String location;

	public DifficultyRequestToServer(int entityId, String registryName, String location) {
		this.entityId = entityId;
		this.registryName = registryName;
		this.location = location;
	}

	public static void encode(DifficultyRequestToServer msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.entityId);
		buf.writeUtf(msg.registryName);
		buf.writeUtf(msg.location);
	}

	public static DifficultyRequestToServer decode(FriendlyByteBuf buf) {
		int entityId = buf.readInt();
		String registryName = buf.readUtf();
		String location = buf.readUtf();
		return new DifficultyRequestToServer(entityId, registryName, location);
	}

	public static void handle(DifficultyRequestToServer msg, Supplier<NetworkEvent.Context> context) {
//		EEchelons.LOGGER.debug("received request message -> {}", msg);
		NetworkEvent.Context ctx = context.get();
		LogicalSide sideReceived = ctx.getDirection().getReceptionSide();

		if (sideReceived != LogicalSide.SERVER) {
			EEchelonsApiMod.LOGGER.warn("DifficultyRequestToServer received on wrong side -> {}", ctx.getDirection().getReceptionSide());
			return;
		}

		ctx.enqueueWork(() -> {
			processMessage(ctx, msg);
		});

		context.get().setPacketHandled(true);

	}

	private static void processMessage(Context ctx, DifficultyRequestToServer msg) {
		Level world = ctx.getSender().level();

//		EEchelons.LOGGER.debug("processing request message -> {}", msg);
		if (world != null) {
			Entity entity = world.getEntity(msg.entityId);
			if (entity != null) {
//				EEchelons.LOGGER.debug("handling server message to entity -> {} : {}", entity.getName().getString(), entity.getId());
				entity.getCapability(ModCapabilities.DIFFICULTY_CAPABILITY).ifPresent(cap -> {
//					EEchelons.LOGGER.debug("entity {} has cap", entity.getId());
					// send the level back to the client
					DifficultyMessageToClient message = new DifficultyMessageToClient(entity.getId(), cap.getDifficulty(), cap.getName());
					ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
				});
			}
		}
	}
	
	@Override
	public String toString() {
		return "DifficultyRequestToServer [entityId=" + entityId + ", registryName=" + registryName + ", location="
				+ location + "]";
	}

}
