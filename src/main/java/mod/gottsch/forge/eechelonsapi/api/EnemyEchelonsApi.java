/*
 * This file is part of  Enemy Echelons API.
 * Copyright (c) 2025 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.eechelonsapi.api;

import mod.gottsch.forge.eechelonsapi.core.capability.IDifficultyHandler;
import mod.gottsch.forge.eechelonsapi.core.capability.ModCapabilities;
import mod.gottsch.forge.eechelonsapi.core.config.EchelonConfigsHolder;
import mod.gottsch.forge.eechelonsapi.core.echelon.EchelonManager;
import mod.gottsch.forge.eechelonsapi.core.registry.EchelonRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.Mod;

import java.security.DrbgParameters;
import java.util.List;
import java.util.Optional;

/**
 * @author by Mark Gottschling on 6/3/2025
 */
public class EnemyEchelonsApi {
    public static void register(List<EchelonConfigsHolder.Config> configs) {
        EchelonManager.REGISTRY.register(configs);
    }

    public static void register(EchelonConfigsHolder.Config config) {
        EchelonManager.REGISTRY.register(config);
    }

    public static Optional<EchelonConfigsHolder.Config> getEchelonConfig(Mob mob) {
        return EchelonManager.REGISTRY.getEchelonConfig(mob);
    }

    // generate a new custom registry
    public static EchelonRegistry customRegistry() {
        return new EchelonRegistry();
    }

    // TODO make call to get mob by desired level
    public static void apply(Mob mob) {
        EchelonManager.applyModifications(mob);
    }

    public static void apply(EchelonRegistry registry, Mob mob) {
        EchelonManager.applyModifications(registry, mob);
    }

    public static void apply(EchelonRegistry registry, Mob mob, int difficulty) {
        EchelonManager.applyModifications(registry, mob, difficulty);
    }

    public static boolean isValidEntity(Entity entity) {
        return EchelonManager.isValidEntity(entity);
    }

    public static boolean hasDifficultyCapability(Entity entity) {
        return entity.getCapability(ModCapabilities.DIFFICULTY_CAPABILITY).isPresent();
    }

    public static Optional<IDifficultyHandler> getDifficultyCapability(Entity entity) {
        return entity.getCapability(ModCapabilities.DIFFICULTY_CAPABILITY).map(cap -> cap);
    }

    public static int getDifficulty(Entity entity) {
        return entity.getCapability(ModCapabilities.DIFFICULTY_CAPABILITY).map(IDifficultyHandler::getDifficulty).orElseThrow(IllegalStateException::new);
    }
}
