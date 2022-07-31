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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import mod.gottsch.forge.eechelons.EEchelons;
import mod.gottsch.forge.eechelons.capability.EEchelonsCapabilities;
import mod.gottsch.forge.eechelons.config.Config;
import mod.gottsch.forge.eechelons.event.HudEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * This class was derived from Champions by TheIllusiveC4
 * @see <a href="https://github.com/TheIllusiveC4/Champions">Champions</a>
 *
 */
public class HudUtil {

	private static final ResourceLocation GUI_BAR_TEXTURES = new ResourceLocation("textures/gui/bars.png");
	private static final ResourceLocation HUD_BG = new ResourceLocation(EEchelons.MODID, "textures/gui/echelon_hud_bg.png");
	
	/**
	 * 
	 * @param matrixStack
	 * @param livingEntity
	 * @return
	 */
	public static boolean renderLevelBar(PoseStack matrixStack, final LivingEntity livingEntity) {
		
		// TODO change this so if no cap is found, then level = 0
		// so blacklisted mobs like creeper will display as level 0 instead of a no render
		if (!livingEntity.getCapability(EEchelonsCapabilities.LEVEL_CAPABILITY).isPresent()) {
			EEchelons.LOGGER.info("cap no present - no render");
			return false;
		}
		
		livingEntity.getCapability(EEchelonsCapabilities.LEVEL_CAPABILITY).ifPresent(cap -> {
			int level = cap.getLevel();

			if (level > -1) {
				Minecraft client = Minecraft.getInstance();
				int i = client.getWindow().getGuiScaledWidth();
				int k = i / 2 - 32; //91; // magic numbers - probably half the width of the HUD
				int j = 12; //21; // magic numbers - probably the height of the HUD
				int xOffset = Config.SERVER.hudXOffset.get();
				int yOffset = Config.SERVER.hudYOffset.get();

				// don't have a color for the level - just white or whatever base color OR could pull from a color map based on level - configurable
				//				          int color = rank.getB();
				//				          float r = (float) ((color >> 16) & 0xFF) / 255f;
				//				          float g = (float) ((color >> 8) & 0xFF) / 255f;
				//				          float b = (float) ((color) & 0xFF) / 255f;

				RenderSystem.defaultBlendFunc();
				//				          RenderSystem.setShaderColor(r, g, b, 1.0F);
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				RenderSystem.enableBlend();
				RenderSystem.setShader(GameRenderer::getPositionTexShader);
				RenderSystem.setShaderTexture(0, HUD_BG); // GUI_BAR_TEXTURES);
				// update static variable in the event handler - why not have those values here?
				HudEventHandler.startX = xOffset + k;
				HudEventHandler.startY = yOffset + 1;

				// 0 = x start
				// 60 = y start
				// 182 = width of the bar texture
				// 5 = height of the bar texture
				// 256 = ?
				GuiComponent.blit(matrixStack, xOffset + k, yOffset + j, 0, 0, 64, 24, 64, 24/*60, 182, 5, 256, 256*/);

				// display the level text
				String text = "Level " + level;
		          client.font.drawShadow(matrixStack, text,
		                  xOffset + (float) (i / 2 - client.font.width(text) / 2),
		                  yOffset + (float) (j  + client.font.lineHeight/*- 9*/), Color.WHITE.getRGB());
		              RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		              
				RenderSystem.disableBlend();
			}
		});
		return true;
	}
}
