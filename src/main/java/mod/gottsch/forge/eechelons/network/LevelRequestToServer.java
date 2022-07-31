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
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * 
 * @author Mark Gottschling on Jul 30, 2022
 *
 */
public class LevelRequestToServer {
	private final int entityId;
	private final String registryName;
	private final String location;
	
	public LevelRequestToServer(int entityId, String registryName, String location) {
		this.entityId = entityId;
		this.registryName = registryName;
		this.location = location;
	}
	
	public static void encode(LevelRequestToServer msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.entityId);
		buf.writeUtf(msg.registryName);
		buf.writeUtf(msg.location);
	}
	
	public static LevelRequestToServer decode(FriendlyByteBuf buf) {
		int entityId = buf.readInt();
		String registryName = buf.readUtf();
		String location = buf.readUtf();
	    return new LevelRequestToServer(entityId, registryName, location);
	}
	
	public static void handle(LevelRequestToServer msg, Supplier<NetworkEvent.Context> context) {
		EEchelons.LOGGER.info("received request message -> {}", msg);
		NetworkEvent.Context ctx = context.get();
		LogicalSide sideReceived = ctx.getDirection().getReceptionSide();

		if (sideReceived != LogicalSide.SERVER) {
			EEchelons.LOGGER.warn("LevelRequestToServer received on wrong side -> {}", ctx.getDirection().getReceptionSide());
			return;
		}

		ctx.enqueueWork(() -> {
			ResourceKey<Level> dimension = ResourceKey.create(ResourceKey.createRegistryKey(new ResourceLocation(msg.registryName)), new ResourceLocation(msg.location));
			ServerLevel world = ctx.getSender().server.getLevel(dimension);
//			Entity entity = null;
//			for (ServerLevel world :ctx.getSender().server.getAllLevels()) {
//				entity = world.getEntity(msg.entityId);
//				if (entity != null) break;
//			}
			if (world != null) {
				Entity entity = world.getEntity(msg.entityId);
				EEchelons.LOGGER.info("handling client message to entity -> {} : {}", entity.getDisplayName().getString(), entity.getId());
				entity.getCapability(EEchelonsCapabilities.LEVEL_CAPABILITY).ifPresent(cap -> {
					EEchelons.LOGGER.info("entity {} has cap", entity.getId());
					// TODO send the level back to the client
					LevelMessageToClient message = new LevelMessageToClient(entity.getId(), cap.getLevel());
					EEchelonsNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
				});
			}
		});
		context.get().setPacketHandled(true);
	}

	@Override
	public String toString() {
		return "LevelRequestToServer [entityId=" + entityId + ", registryName=" + registryName + ", location="
				+ location + "]";
	}

}
