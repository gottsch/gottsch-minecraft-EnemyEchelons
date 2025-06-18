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
package mod.gottsch.forge.eechelons.core.client;

import com.mojang.blaze3d.systems.RenderSystem;
import mod.gottsch.forge.eechelons.EEchelons;
import mod.gottsch.forge.eechelons.core.config.Config;
import mod.gottsch.forge.eechelons.core.event.HudEventHandler;
import mod.gottsch.forge.eechelons.core.integration.ChampionsIntegration;
import mod.gottsch.forge.eechelons.core.integration.WailaIntegration;
import mod.gottsch.forge.eechelonsapi.core.capability.IDifficultyHandler;
import mod.gottsch.forge.eechelonsapi.core.capability.ModCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.awt.*;

/**
 * This class was derived from Champions by TheIllusiveC4
 * @see <a href="https://github.com/TheIllusiveC4/Champions">Champions</a>
 *
 */
public class HudUtil {
	private static final int WAILA_INTEGRATION_XOFFSET = -80;
	private static final int CHAMPIONS_INTEGRATION_XOFFSET = -124;
	
	private static final int HUD_OFFSET_WIDTH = 32;
	private static final int MEDIUM_OFFSET_WIDTH = 64;
	private static final int HUD_OFFSET_HEIGHT = 4;
	private static final ResourceLocation HUD_BG = new ResourceLocation(EEchelons.MOD_ID, "textures/gui/echelon_hud_bg.png");
	private static final ResourceLocation HUD_DARK_BG = new ResourceLocation(EEchelons.MOD_ID, "textures/gui/echelon_hud_dark_bg2.png");
	private static final ResourceLocation MEDIUM_DARK_BG = new ResourceLocation(EEchelons.MOD_ID, "textures/gui/medium_echelon_hud_dark_bg.png");

	/**
	 * 
	 * @param matrixStack
	 * @param livingEntity
	 * @return
	 */
	public static boolean renderLevelBar(GuiGraphics matrixStack, final LivingEntity livingEntity) {

		int difficulty = livingEntity.getCapability(ModCapabilities.DIFFICULTY_CAPABILITY).map(IDifficultyHandler::getDifficulty).orElse(0);
		String name = livingEntity.getCapability(ModCapabilities.DIFFICULTY_CAPABILITY).map(IDifficultyHandler::getName).orElse("");

		// do not display is client doesn't want to show Level 0 hud.
		if (difficulty == 0 && !Config.CLIENT.showLevel0Hud.get()) {
			return false;
		}

		if (difficulty > -1) {
			Minecraft client = Minecraft.getInstance();

			// get the screen width
			int i = client.getWindow().getGuiScaledWidth();

			// calculate the text and width
			String text = !name.isEmpty() ? name : "Level " + difficulty;
			int textWidth = client.font.width(text);

			// determine what bg size to use
			ResourceLocation hudBg = textWidth > 55 ? MEDIUM_DARK_BG : HUD_DARK_BG;

			// adjust offset calculations
			// middle of the screen
			int k = i / 2 - (textWidth > 55 ? MEDIUM_OFFSET_WIDTH : HUD_OFFSET_WIDTH);
			int j = HUD_OFFSET_HEIGHT;
			
			int xOffset = Config.CLIENT.hudXOffset.get();
			int yOffset = Config.CLIENT.hudYOffset.get();

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

			// ----- IMPORTANT: Enable blending for transparency -----
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
//			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
//			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//			RenderSystem.setShader(GameRenderer::getPositionTexShader);
//			RenderSystem.setShaderTexture(0, hudBg);

			// 0 = startx, 0 = starty, 64 = endx, 20 = endy, 64 = width of image, 20 = height of image
			matrixStack.blit(hudBg, xOffset + k + integrationXOffset, yOffset + j + integrationYOffset, 0, 0, 64, 20, 64, 20);

			RenderSystem.disableBlend();

			// display the level text
			matrixStack.drawString(Minecraft.getInstance().font, text,
					(int)(xOffset + (float) (i / 2 - textWidth / 2) + integrationXOffset),
					(int)(yOffset + (float) (j  + client.font.lineHeight - 3) + integrationYOffset),
					Color.WHITE.getRGB());
		}

		return true;
	}
}
