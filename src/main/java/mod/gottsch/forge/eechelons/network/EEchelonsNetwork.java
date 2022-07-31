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

import java.util.Optional;

import mod.gottsch.forge.eechelons.EEchelons;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 
 * @author Mark Gottschling on Jul 28, 2022
 *
 */
public class EEchelonsNetwork {
	public static final String PROTOCOL_VERSION = "1.0";
	public static SimpleChannel CHANNEL;
	public static int TO_CLIENT_ID = 0;
	public static int TO_SERVER_ID = 1;
	
	public static void register() {
	    CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(EEchelons.MODID, "eechelons_channel"))
	        .networkProtocolVersion(() -> PROTOCOL_VERSION).clientAcceptedVersions(PROTOCOL_VERSION::equals)
	        .serverAcceptedVersions(PROTOCOL_VERSION::equals).simpleChannel();

	    CHANNEL.registerMessage(TO_CLIENT_ID, LevelMessageToClient.class, LevelMessageToClient::encode, LevelMessageToClient::decode,
	        LevelMessageToClient::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
	    
	    CHANNEL.registerMessage(TO_SERVER_ID, LevelRequestToServer.class, LevelRequestToServer::encode, LevelRequestToServer::decode,
	    		LevelRequestToServer::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
	  }
}
