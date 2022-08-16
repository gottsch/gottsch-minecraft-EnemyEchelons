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

import java.util.Optional;

import mod.gottsch.forge.eechelons.capability.EEchelonsCapabilities;
import mod.gottsch.forge.eechelons.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

/**
 * This class was derived from Champions by TheIllusiveC4
 * @see <a href="https://github.com/TheIllusiveC4/Champions">Champions</a>
 *
 */
public class MouseUtil {
	public static Optional<LivingEntity> getMouseOverEchelonMob(Minecraft mc, float partialTicks) {
		Entity entity = mc.getCameraEntity();
		if (entity != null) {
			if (mc.level != null) {
				double range = Config.CLIENT.hudRange.get();
				RayTraceResult rayTraceResult = entity.pick(range, partialTicks, false);
				Vector3d vec3d = entity.getEyePosition(partialTicks);
				double distance = rayTraceResult.getLocation().distanceToSqr(vec3d);
				Vector3d viewVector = entity.getViewVector(1.0F);
				Vector3d vec3d2 = vec3d.add(viewVector.x * range, viewVector.y * range, viewVector.z * range);
				AxisAlignedBB aabb = entity.getBoundingBox().expandTowards(viewVector.scale(range)).inflate(1.0D, 1.0D, 1.0D);
				EntityRayTraceResult entityRayTraceResult =
						ProjectileHelper.getEntityHitResult(entity, vec3d, vec3d2, aabb, (e) -> !e.isSpectator() && e.isPickable(), distance);

				if (entityRayTraceResult != null) {
					Entity hoverEntity = entityRayTraceResult.getEntity();
					if (hoverEntity.getCapability(EEchelonsCapabilities.LEVEL_CAPABILITY).isPresent()) {
						return Optional.of((LivingEntity)hoverEntity);
					}
				}
			}
		}
		return Optional.empty();
	}
}
