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
package mod.gottsch.forge.eechelonsapi.core.config;

import java.util.ArrayList;
import java.util.List;

/**
 * @author by Mark Gottschling on 5/29/2025
 */
public class NameConfigsHolder {
    public List<NameConfig> nameConfigs;

    public List<NameConfig> getNameConfigs() {
        return nameConfigs;
    }

    public void setNameConfigs(List<NameConfig> nameConfigs) {
        this.nameConfigs = nameConfigs;
    }

    public static class NameConfig {
        private String id;
        private List<String> modWhitelist;
        private List<String> modBlacklist;

        private List<String> mobWhitelist;
        private List<String> mobBlacklist;

        private List<DifficultyName> difficultyNames;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<DifficultyName> getDifficultyNames() {
            return difficultyNames;
        }

        public void setDifficultyNames(List<DifficultyName> difficultyNames) {
            this.difficultyNames = difficultyNames;
        }

        public List<String> getMobBlacklist() {
            if (mobBlacklist == null) {
                mobBlacklist = new ArrayList<>();
            }
            return mobBlacklist;
        }

        public void setMobBlacklist(List<String> mobBlacklist) {
            this.mobBlacklist = mobBlacklist;
        }

        public List<String> getMobWhitelist() {
            if (mobWhitelist == null) {
                mobWhitelist = new ArrayList<>();
            }
            return mobWhitelist;
        }

        public void setMobWhitelist(List<String> mobWhitelist) {
            this.mobWhitelist = mobWhitelist;
        }

        public List<String> getModBlacklist() {
            if (modBlacklist == null) {
                modBlacklist = new ArrayList<>();
            }
            return modBlacklist;
        }

        public void setModBlacklist(List<String> modBlacklist) {
            this.modBlacklist = modBlacklist;
        }

        public List<String> getModWhitelist() {
            if (modWhitelist == null) {
                modWhitelist = new ArrayList<>();
            }
            return modWhitelist;
        }

        public void setModWhitelist(List<String> modWhitelist) {
            this.modWhitelist = modWhitelist;
        }
    }

    /*
     *
     */
    public static class DifficultyName {
        private Integer difficulty;
        private String name;

        public DifficultyName() {}

        public DifficultyName(Integer difficulty, String name) {
            this.difficulty = difficulty;
            this.name = name;
        }

        public Integer getDifficulty() {
            return difficulty == null ? 0 : difficulty;
        }

        public void setDifficulty(Integer difficulty) {
            this.difficulty = difficulty == null ? 0 : difficulty;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
