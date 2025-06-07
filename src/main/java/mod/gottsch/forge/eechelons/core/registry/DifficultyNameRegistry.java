package mod.gottsch.forge.eechelons.core.registry;

import com.google.common.collect.Maps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * NOTE this is a registry of DifficultyNameRegistryEntrys.
 * @author by Mark Gottschling on 5/28/2025
 */
public class DifficultyNameRegistry {
    private static final ResourceLocation ALL_MOBS = new ResourceLocation(".", ".");

    // ie Map<ID, Entry>
    private static Map<ResourceLocation, DifficultyNameRegistryEntry> REGISTRY = Maps.newHashMap();

    private static Map<ResourceLocation, DifficultyNameRegistryEntry> BY_MOB = Maps.newHashMap();

    private static Map<String, DifficultyNameRegistryEntry> BY_MOD = Maps.newHashMap();

    public static void clear() {

    }

    public static void register(List<DifficultyNameRegistryEntry> entries) {
        if (ObjectUtils.isEmpty(entries)) {
            return;
        }

        entries.forEach(entry -> {

            // register all entries by id
            REGISTRY.put(entry.getId(), entry);

            // scan the mob white/black list to see if there are any wildcards and move to mod lists.
            Predicate<ResourceLocation> isWildcard = mob -> mob.getPath().equals("*");
            entry.getMobWhitelist().stream().filter(isWildcard)
                    .forEach(mob -> {
                        entry.getModWhitelist().add(mob.getNamespace());
                    });
            entry.getMobBlacklist().stream().filter(isWildcard)
                    .forEach(mob -> {
                        entry.getModBlacklist().add(mob.getNamespace());
                    });
            entry.getMobWhitelist().removeIf(isWildcard);
            entry.getMobBlacklist().removeIf(isWildcard);


            // by MOD
            if (!entry.getModWhitelist().isEmpty()) {
                entry.getModWhitelist().forEach(modNamespace -> {
                    if (modNamespace.equals(".") || modNamespace.equals(".:.")
                    || modNamespace.equals("*") || modNamespace.equals("*:*")) {
                        if (!BY_MOD.containsKey("*:*")) {
                            BY_MOD.put(ALL_MOBS.toString(), entry);
                        }
                    }
                    // if not already registered, then register it
                    else if (!BY_MOD.containsKey(modNamespace.toLowerCase())) {
                        BY_MOD.put(modNamespace.toLowerCase(), entry);
                    }
                });
            }

            // by MOB
            if (!entry.getMobWhitelist().isEmpty()) {
                entry.getMobWhitelist().forEach(mobId -> {
                    // create a key pair
                    if (!BY_MOB.containsKey(mobId)) {
                        BY_MOB.put(mobId, entry);
                    }
                });
            }

        });

    }

    public static Optional<DifficultyNameRegistryEntry> getDifficultyEntry(Mob mob) {
        ResourceLocation mobId =  EntityType.getKey(mob.getType());
        if (BY_MOB.containsKey(mobId)) {
            return Optional.of(BY_MOB.get(mobId));
        }
        else {
            // search all remaining configs
            return searchForMob(mob.level().dimension().location(), mob);
        }
    }

    public static Optional<DifficultyNameRegistryEntry> searchForMob(ResourceLocation dimension, Mob entity) {
        ResourceLocation mob = EntityType.getKey(entity.getType());

        for (DifficultyNameRegistryEntry e : REGISTRY.values()) {
            // find the first valid config - ie in the mod whitelist and not in the blacklists

            // mod whitelist contains a wildcard
            if(!e.getModWhitelist().isEmpty()
                    && (e.getModWhitelist().contains(ALL_MOBS.toString()) || e.getModWhitelist().contains(mob.getNamespace()))) {

                if (e.getModBlacklist().contains(mob.getNamespace())) {
                    return Optional.empty();
                }
                if (e.getMobBlacklist().contains(mob)) {
                    return Optional.empty();
                }

                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    public static Optional<DifficultyNameRegistryEntry> register(ResourceLocation key, DifficultyNameRegistryEntry entry) {
        return Optional.ofNullable(REGISTRY.put(key, entry));
    }

    public static Optional<DifficultyNameRegistryEntry> getById(ResourceLocation key) {
        return Optional.ofNullable(REGISTRY.get(key));
    }

    public static Optional<String> getDifficultyName(Mob mob, Integer difficulty) {
        Optional<DifficultyNameRegistryEntry> entry = getDifficultyEntry(mob);
        if (entry.isPresent()) {
            return entry.get().getDifficultyName(difficulty);
        }
        return Optional.empty();
    }
}
