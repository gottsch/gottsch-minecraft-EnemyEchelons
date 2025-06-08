package mod.gottsch.forge.eechelons.core.config;

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
