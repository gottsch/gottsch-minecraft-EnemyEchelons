/*
 * This file is part of Enemy Echelons.
 * Copyright (c) 2025 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.eechelons.api;

import mod.gottsch.forge.eechelons.core.config.EchelonConfigsHolder;
import mod.gottsch.forge.eechelons.core.echelon.EchelonManager;
import mod.gottsch.forge.eechelons.core.registry.EchelonRegistry;
import net.minecraft.world.entity.Mob;

import java.util.List;

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

    public static EchelonRegistry customRegistry() {
        return new EchelonRegistry();
    }

    // TOD make call to get mob by desired level
    public static void apply(Mob mob) {
        EchelonManager.applyModifications(mob);
    }

    public static void apply(EchelonRegistry registry, Mob mob) {
        EchelonManager.applyModifications(registry, mob);
    }

    public static void apply(EchelonRegistry registry, Mob mob, int difficulty) {
        EchelonManager.applyModifications(registry, mob, difficulty);
    }
}
