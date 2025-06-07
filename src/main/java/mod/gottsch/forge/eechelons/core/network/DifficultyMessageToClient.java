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
package mod.gottsch.forge.eechelons.core.network;

import java.util.function.Supplier;

import mod.gottsch.forge.eechelons.EEchelons;
import mod.gottsch.forge.eechelons.core.capability.EEchelonsCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * 
 * @author Mark Gottschling on Jul 28, 2022
 *
 */
public class DifficultyMessageToClient {
	private final int entityId;
	private final int level;
	private final String name;

	public DifficultyMessageToClient(int entityId, int level, String name) {
		this.entityId = entityId;
		this.level = level;
		this.name = name;
	}

	public static void encode(DifficultyMessageToClient msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.entityId);
		buf.writeInt(msg.level);
		buf.writeUtf(msg.name);
	}

	public static DifficultyMessageToClient decode(FriendlyByteBuf buf) {
		int entityId = buf.readInt();
		int level = buf.readInt();
		String name = buf.readUtf();
		return new DifficultyMessageToClient(entityId, level, name);
	}

	public static void handle(DifficultyMessageToClient msg, Supplier<NetworkEvent.Context> context) {
//		EEchelons.LOGGER.debug("received message on client -> {}", msg);
		NetworkEvent.Context ctx = context.get();
		LogicalSide sideReceived = ctx.getDirection().getReceptionSide();

		if (sideReceived != LogicalSide.CLIENT) {
			EEchelons.LOGGER.warn("DifficultyMessageToClient received on wrong side -> {}", ctx.getDirection().getReceptionSide());
			return;
		}

		context.get().enqueueWork(() ->
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> processMessage(ctx, msg))
				);
		context.get().setPacketHandled(true);

	}

	private static void processMessage(Context ctx, DifficultyMessageToClient msg) {
		ClientLevel world = Minecraft.getInstance().level;
		if (world != null) {
			Entity entity = world.getEntity(msg.entityId);
//			EEchelons.LOGGER.debug("handling client message to entity -> {} for level -> {}", entity.getName().getString(), msg.level);
			entity.getCapability(EEchelonsCapabilities.DIFFICULTY_CAPABILITY).ifPresent(cap -> {
//				EEchelons.LOGGER.debug("setting the level on the client entity");
				cap.setDifficulty(msg.level);
				cap.setName(msg.name);
			});
		}
	}

	@Override
	public String toString() {
		return "DifficultyMessageToClient [entityId=" + entityId + ", level=" + level + "]";
	}


}
