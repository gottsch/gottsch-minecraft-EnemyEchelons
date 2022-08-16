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
package mod.gottsch.forge.eechelons.client;

import java.awt.Color;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import mod.gottsch.forge.eechelons.EEchelons;
import mod.gottsch.forge.eechelons.capability.EEchelonsCapabilities;
import mod.gottsch.forge.eechelons.config.Config;
import mod.gottsch.forge.eechelons.event.HudEventHandler;
import mod.gottsch.forge.eechelons.integration.ChampionsIntegration;
import mod.gottsch.forge.eechelons.integration.WailaIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;

/**
 * This class was derived from Champions by TheIllusiveC4
 * @see <a href="https://github.com/TheIllusiveC4/Champions">Champions</a>
 *
 */
public class HudUtil {
	private static final int WAILA_INTEGRATION_XOFFSET = -80;
	private static final int CHAMPIONS_INTEGRATION_XOFFSET = -124;
	
	private static final int HUD_OFFSET_WIDTH = 32;
	private static final int HUD_OFFSET_HEIGHT = 4;
	public static final ResourceLocation HUD_BG = new ResourceLocation(EEchelons.MODID, "textures/gui/echelon_hud_bg.png");
	public static final ResourceLocation HUD_DARK_BG = new ResourceLocation(EEchelons.MODID, "textures/gui/echelon_hud_dark_bg.png");
	
	/**
	 * 
	 * @param matrixStack
	 * @param livingEntity
	 * @return
	 */
	public static boolean renderLevelBar(MatrixStack matrixStack, final LivingEntity livingEntity) {

		int level = livingEntity.getCapability(EEchelonsCapabilities.LEVEL_CAPABILITY).map(cap -> cap.getLevel()).orElse(0);

		if (level > -1) {
			Minecraft client = Minecraft.getInstance();
			int i = client.getWindow().getGuiScaledWidth();
			// middle of the screen
			int k = i / 2 - HUD_OFFSET_WIDTH;
			int j = HUD_OFFSET_HEIGHT;
			
			int xOffset = Config.CLIENT.hudXOffset.get();
			int yOffset = Config.CLIENT.hudYOffset.get();

			RenderSystem.defaultBlendFunc();
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.enableBlend();
			client.getTextureManager().bind(Config.CLIENT.useDarkHud.get() ? HUD_DARK_BG : HUD_BG);

			/*
			 * only recalc offsets for integration if the config offsets are still default values
			 */
			int integrationXOffset = 0;
			int integrationYOffset = 0;
			if (xOffset == 0 && yOffset == 0) {
				if (ChampionsIntegration.isEnabled()) {
					integrationXOffset = CHAMPIONS_INTEGRATION_XOFFSET;
				}
				else if (WailaIntegration.isEnabled()) {
					integrationXOffset = WAILA_INTEGRATION_XOFFSET;
				}
			}

			// update static variable in the event handler
			HudEventHandler.startX = xOffset + k + integrationXOffset;
			HudEventHandler.startY = yOffset + 1 + integrationYOffset;

			// 0 = startx, 0 = starty, 64 = endx, 20 = endy, 64 = width of image, 20 = height of image
			AbstractGui.blit(matrixStack, xOffset + k + integrationXOffset, yOffset + j + integrationYOffset, 0, 0, 64, 20, 64, 20);

			// display the level text
			String text = "Level " + level;
			client.font.drawShadow(matrixStack, text,
					xOffset + (float) (i / 2 - client.font.width(text) / 2) + integrationXOffset,
					yOffset + (float) (j  + client.font.lineHeight - 3) + integrationYOffset, Color.WHITE.getRGB());
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

			RenderSystem.disableBlend();
		}

		return true;
	}
}
