/*
 * This file is part of  Enemy Echelons API.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.eechelonsapi.core.config;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The merged set of mods and mobs that no echelon config in a registry may apply to.
 * Loaded from data/[namespace]/echelons/exclusions/[name].json.
 * An exclusion is absolute: it wins over every whitelist (mod and mob) of every config in the registry,
 * so a mod can keep its mobs out even of a config that names it.
 * @author Mark Gottschling on 10/29/2026
 */
public class EchelonExclusions {
    public static final EchelonExclusions EMPTY = new EchelonExclusions(Collections.emptySet(), Collections.emptySet());

    // mod ids (namespaces)
    private final Set<String> mods;
    // mob ids
    private final Set<ResourceLocation> mobs;

    public EchelonExclusions(Collection<String> mods, Collection<ResourceLocation> mobs) {
        this.mods = Collections.unmodifiableSet(new LinkedHashSet<>(mods));
        this.mobs = Collections.unmodifiableSet(new LinkedHashSet<>(mobs));
    }

    /**
     * merges a set of exclusions into one.
     * @param exclusions
     * @return
     */
    public static EchelonExclusions merge(Collection<EchelonExclusions> exclusions) {
        Set<String> mods = new LinkedHashSet<>();
        Set<ResourceLocation> mobs = new LinkedHashSet<>();
        exclusions.forEach(e -> {
            mods.addAll(e.getMods());
            mobs.addAll(e.getMobs());
        });
        return mods.isEmpty() && mobs.isEmpty() ? EMPTY : new EchelonExclusions(mods, mobs);
    }

    public boolean isExcluded(ResourceLocation mobId) {
        return mods.contains(mobId.getNamespace()) || mobs.contains(mobId);
    }

    public boolean isEmpty() {
        return mods.isEmpty() && mobs.isEmpty();
    }

    public Set<String> getMods() {
        return mods;
    }

    public Set<ResourceLocation> getMobs() {
        return mobs;
    }

    @Override
    public String toString() {
        return "EchelonExclusions{mods=" + mods + ", mobs=" + mobs + '}';
    }
}
