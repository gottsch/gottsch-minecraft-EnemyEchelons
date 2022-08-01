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
package mod.gottsch.forge.eechelons.event;

import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;

import mod.gottsch.forge.eechelons.EEchelons;
import mod.gottsch.forge.eechelons.client.HudUtil;
import mod.gottsch.forge.eechelons.client.MouseUtil;
import mod.gottsch.forge.eechelons.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * This class was derived from Champions by TheIllusiveC4
 * @see <a href="https://github.com/TheIllusiveC4/Champions">Champions</a>
 *
 */
public class HudEventHandler {

	public static boolean isRendering = false;
	
	// NOTE these are only used to check where the offset is set to, so other integrations can move if they overlap
	// these are NOT used in the actual echelons rendering of background and level text 
	public static int startX = 0;
	public static int startY = 0;

	/**
	 * Forge Bus Event Subscriber class
	 */
	@Mod.EventBusSubscriber(modid = EEchelons.MODID, bus = EventBusSubscriber.Bus.FORGE)
	public static class ForgeBusSubscriber {
		
		@SubscribeEvent
		public static void renderHealthHud(final RenderGameOverlayEvent.BossInfo.Pre evt) {
			if (Config.SERVER.showHud.get()) {
				Minecraft mc = Minecraft.getInstance();
				Optional<LivingEntity> livingEntity = MouseUtil.getMouseOverEchelonMob(mc, evt.getPartialTicks());
				livingEntity.ifPresent(entity -> {
					PoseStack matrixStack = evt.getMatrixStack();

					if (HudUtil.renderLevelBar(matrixStack, entity)) {
						isRendering = true;

						if (evt.getType() == ElementType.BOSSINFO) {
							evt.setCanceled(true);
							ForgeHooksClient.renderBossEventPost(matrixStack, mc.getWindow());
						}
					} else {
						isRendering = false;
					}
				});

				if (livingEntity.isEmpty()) {
					isRendering = false;
				}
			}
		}
	}
}
