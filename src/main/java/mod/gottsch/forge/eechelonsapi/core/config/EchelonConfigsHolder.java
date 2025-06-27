/*
 * This file is part of  Enemy Echelons API.
 * Copyright (c) 2022 Mark Gottschling (gottsch)
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

import mod.gottsch.forge.gottschcore.bst.IInterval;
import mod.gottsch.forge.gottschcore.bst.IIntervalTree;
import mod.gottsch.forge.gottschcore.bst.Interval;
import mod.gottsch.forge.gottschcore.random.WeightedCollection;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A holder class for the Echelon Config list.
 * A holder is required so the toml can be converted into an object properly.
 * ie. The holder is at the file level, and the list is a property in the file.
 * @author Mark Gottschling on Jul 26, 2022
 *
 */
public class EchelonConfigsHolder {
	// if no loadOrder is provided, defaults to 99 (low precedence)
	public Integer loadOrder = 99;

	public List<Config> configs;

	public Integer getLoadOrder() {
		return loadOrder == null ? 99 : loadOrder;
	}

	public void setLoadOrder(Integer loadOrder) {
		this.loadOrder = loadOrder == null ? 99 : loadOrder;
	}

	public List<Config> getConfigs() {
		return configs;
	}

	public void setConfigs(List<Config> configs) {
		this.configs = configs;
	}

	public static class Config {
		private String id;
		private List<String> dimensions;
		private Double hpFactor = 0.0;
		private Double maxHp;
		private Double damageFactor = 0.0;
		private Double maxDamage;
		private Double knockbackIncrement = 0.0;
		private Double maxKnockback;
		private Double knockbackResistIncrement = 0.0;
		private Double maxKnockbackResist;
		private Double armorFactor = 0.0;
		private Double maxArmor;
		private Double armorToughnessFactor = 0.0;
		private Double maxArmorToughness;
		private Double speedFactor = 0.0;
		private Double maxSpeed;
		private Double flyingSpeedFactor = 0.0;
		private Double maxFlyingSpeed;
		
		private Double xpFactor = 0.0;
		private Double maxXp;

		// TODO
		private Boolean replace;
		/*
		 * these are lists for mod wildcards. these lists only contain the modid.
		 * ex ["minecraft", "ddenizens"]
		 * if a wildcard is used in the mob lists, they need to be added to these lists.
		 * ex. "ddenizens:*" in mob list -> "ddenizens" in mod list.
		 * these lists supercede the mob lists.
		 */
		private List<String> modWhitelist;
		private List<String> modBlacklist;

		private List<String> mobWhitelist;
		private List<String> mobBlacklist;

		private List<Echelon> echelons;

		// internal: not set by config, but set by the manager
		private IIntervalTree<WeightedCollection<Double, Integer>> histogram;

		/**
		 *
		 * @param y
		 * @return
		 */
		public Integer getDifficulty(Integer y) {
			Integer result = 0;

			List<IInterval<WeightedCollection<Double, Integer>>> difficulties = histogram
					.getOverlapping(histogram.getRoot(), new Interval<>(y, y), true, true);

			if (ObjectUtils.isEmpty(difficulties)) {
				return 0;
			}

			// get the first element/echelon - there should only be one.
			WeightedCollection<Double, Integer> col = difficulties.get(0).getData();
			if (ObjectUtils.isEmpty(col)) {
				return 0;
			}
			// get the next weighted random integer
			result = col.next();

			return result;
		}

		public String getId() {
			return this.id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public List<String> getDimensions() {
			if (dimensions == null) {
				dimensions = new ArrayList<>();
			}
			return dimensions;
		}

		public void setDimensions(List<String> dimensions) {
			this.dimensions = dimensions;
		}

		public Double getHpFactor() {
			return hpFactor;
		}

		public void setHpFactor(Double hpFactor) {
			if (hpFactor == null) {
				hpFactor = 0.0;
			}
			this.hpFactor = hpFactor;
		}
		
		public boolean hasHpFactor() {
			return hpFactor != null && hpFactor > 0.0;
		}

		public Double getDamageFactor() {
			if (damageFactor == null) {
				damageFactor = 0.0;
			}
			return damageFactor;
		}

		public void setDamageFactor(Double damageFactor) {
			this.damageFactor = damageFactor;
		}
		
		public boolean hasDamageFactor() {
			return damageFactor != null && damageFactor > 0.0;
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

		public void setModWhitelist(List<String> list) {
			this.modWhitelist = list;
		}

		public Double getMaxHp() {
			return maxHp;
		}

		public void setMaxHp(Double maxHp) {
			this.maxHp = maxHp;
		}

		public Double getMaxDamage() {
			return maxDamage;
		}

		public void setMaxDamage(Double maxDamage) {
			this.maxDamage = maxDamage;
		}

		public List<Echelon> getEchelons() {
			return echelons;
		}

		public void setEchelons(List<Echelon> echelons) {
			this.echelons = echelons;
		}

		@Override
		public String toString() {
			return "Echelon Config [dimensions=" + dimensions + ", hpFactor=" + hpFactor + ", maxHp=" + maxHp
					+ ", damageFactor=" + damageFactor + ", maxDamage=" + maxDamage + ", mobBlacklist=" + mobBlacklist
					+ ", echelons=" + echelons + "]";
		}

		public Double getXpFactor() {
			if (xpFactor == null) {
				xpFactor = 0.0;
			}
			return xpFactor;
		}

		public boolean hasXpFactor() {
			return xpFactor != null && xpFactor > 0.0;
		}
		
		public void setXpFactor(Double xpFactor) {
			this.xpFactor = xpFactor;
		}

		public Double getMaxXp() {
			return maxXp;
		}

		public void setMaxXp(Double maxXp) {
			this.maxXp = maxXp;
		}

		public Double getSpeedFactor() {
			if (speedFactor == null) {
				speedFactor = 0.0;
			}
			return speedFactor;
		}

		public void setSpeedFactor(Double speedFactor) {
			this.speedFactor = speedFactor;
		}

		public boolean hasSpeedFactor() {
			return speedFactor != null && speedFactor > 0.0;
		}
		
		public Double getMaxSpeed() {
			return maxSpeed;
		}

		public void setMaxSpeed(Double maxSpeed) {
			this.maxSpeed = maxSpeed;
		}

		public Double getKnockbackIncrement() {
			if (knockbackIncrement == null) {
				knockbackIncrement = 0.0;
			}
			return knockbackIncrement;
		}

		public void setKnockbackIncrement(Double knockbackIncrement) {
			this.knockbackIncrement = knockbackIncrement;
		}

		public boolean hasKnockbackIncrement() {
			return knockbackIncrement != null && knockbackIncrement > 0.0;
		}
		
		public Double getMaxKnockback() {
			return maxKnockback;
		}

		public void setMaxKnockback(Double maxKnockback) {
			this.maxKnockback = maxKnockback;
		}

		public Double getKnockbackResistIncrement() {
			if (knockbackResistIncrement == null) {
				knockbackResistIncrement = 0.0;
			}
			return knockbackResistIncrement;
		}

		public void setKnockbackResistIncrement(Double knockbackResistIncrement) {
			this.knockbackResistIncrement = knockbackResistIncrement;
		}

		public boolean hasKnockbackResistIncrement() {
			return knockbackResistIncrement != null && knockbackResistIncrement > 0.0;
		}
		
		public Double getMaxKnockbackResist() {
			return maxKnockbackResist;
		}

		public void setMaxKnockbackResist(Double maxKnockbackResist) {
			this.maxKnockbackResist = maxKnockbackResist;
		}
		
		public Double getArmorFactor() {
			if (armorFactor == null) {
				armorFactor = 0.0;
			}
			return armorFactor;
		}

		public void setArmorFactor(Double armorFactor) {
			this.armorFactor = armorFactor;
		}

		public boolean hasArmorFactor() {
			return armorFactor != null && armorFactor > 0.0;
		}
		
		public Double getMaxArmor() {
			return maxArmor;
		}

		public void setMaxArmor(Double maxArmor) {
			this.maxArmor = maxArmor;
		}

		public Double getArmorToughnessFactor() {
			if (armorToughnessFactor == null) {
				armorToughnessFactor = 0.0;
			}
			return armorToughnessFactor;
		}

		public void setArmorToughnessFactor(Double armorToughnessFactor) {
			this.armorToughnessFactor = armorToughnessFactor;
		}

		public boolean hasArmorToughnessFactor() {
			return armorToughnessFactor != null && armorToughnessFactor > 0.0;
		}
		
		public Double getMaxArmorToughness() {
			return maxArmorToughness;
		}

		public void setMaxArmorToughness(Double maxArmorToughness) {
			this.maxArmorToughness = maxArmorToughness;
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

		public IIntervalTree<WeightedCollection<Double, Integer>> getHistogram() {
			return this.histogram;
		}

		public void setHistogram(IIntervalTree<WeightedCollection<Double, Integer>> histogram) {
			this.histogram = histogram;
		}

		public Boolean getReplace() {
			return replace;
		}

		public void setReplace(Boolean replace) {
			this.replace = replace;
		}

        public Double getFlyingSpeedFactor() {
            return flyingSpeedFactor;
        }

        public void setFlyingSpeedFactor(Double flyingSpeedFactor) {
            this.flyingSpeedFactor = flyingSpeedFactor;
        }

        public Double getMaxFlyingSpeed() {
            return maxFlyingSpeed;
        }

        public void setMaxFlyingSpeed(Double maxFlyingSpeed) {
            this.maxFlyingSpeed = maxFlyingSpeed;
        }
    }
	
	/*
	 * 
	 */
	public static class Echelon {
		private String id;
		private Integer min;
		private Integer max;
		private List<DifficultyEntry> histogram;

		// internal: not set by config, but set by the manager
		private WeightedCollection<Double, Integer> weightedDifficulties;


		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
		public Integer getMin() {
			return min;
		}
		public void setMin(Integer min) {
			this.min = min;
		}
		public Integer getMax() {
			return max;
		}
		public void setMax(Integer max) {
			this.max = max;
		}
		public List<DifficultyEntry> getHistogram() {
			return histogram;
		}
		public WeightedCollection<Double, Integer> getWeightedDifficulties() {
			return weightedDifficulties;
		}
		public void setWeightedDifficulties(WeightedCollection<Double, Integer> weightedDifficulties) {
			this.weightedDifficulties = weightedDifficulties;
		}

		public void setHistogram(List<DifficultyEntry> histogram) {
			this.histogram = histogram;
		}

		@Override
		public String toString() {
			return "Echelon{" +
					"histogram=" + histogram +
					", id='" + id + '\'' +
					", min=" + min +
					", max=" + max +
					", weightedDifficulties=" + weightedDifficulties +
					'}';
		}
	}

	/*
	 *
	 */
	public static class DifficultyEntry {
		private Integer difficulty;
		private Double weight;
		
		public Integer getDifficulty() {
			return difficulty;
		}
		public void setDifficulty(Integer difficulty) {
			this.difficulty = difficulty;
		}
		public Double getWeight() {
			return weight;
		}
		public void setWeight(Double weight) {
			this.weight = weight;
		}
		
		@Override
		public String toString() {
			return "DifficultyEntry [difficulty=" + difficulty + ", weight=" + weight + "]";
		}		
	}
}
