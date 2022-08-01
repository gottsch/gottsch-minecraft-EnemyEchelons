/*
 * This file is part of  Enemy Echelons.
 * Copyright (c) 2022, Mark Gottschling (gottsch)
 * 
 * All rights reserved.
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
package mod.gottsch.forge.eechelons.config;

import java.util.ArrayList;
import java.util.List;

/**
 * A holder class for the Echelon list.
 * A holder is required so the toml can be converted into an object properly.
 * ie. The holder is at the file level, and the list is a property in the file.
 * @author Mark Gottschling on Jul 26, 2022
 *
 */
public class EchelonsHolder {
	public List<Echelon> echelons;
	
	public static class Echelon {
		private List<String> dimensions;
		private Double hpFactor;
		private Double maxHp;
		private Double damageFactor;
		private Double maxDamage;
		private Double knockbackIncrement;
		private Double maxKnockback;
		private Double knockbackResistIncrement;
		private Double maxKnockbackResist;
		private Double armorFactor;
		private Double maxArmor;
		private Double armorToughnessFactor;
		private Double maxArmorToughness;
		private Double speedFactor;
		private Double maxSpeed;
		private Double flySpeedFactor;
		private Double maxFlySpeed;
		
		private Double xpFactor;
		private Double maxXp;
		
		private List<String> mobBlacklist;

		private List<Strata> stratum;
		
		public List<String> getDimensions() {
			return dimensions;
		}

		public void setDimensions(List<String> dimensions) {
			this.dimensions = dimensions;
		}

		public Double getHpFactor() {
			return hpFactor;
		}

		public void setHpFactor(Double hpFactor) {
			this.hpFactor = hpFactor;
		}

		public Double getDamageFactor() {
			return damageFactor;
		}

		public void setDamageFactor(Double damageFactor) {
			this.damageFactor = damageFactor;
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

		public List<Strata> getStratum() {
			return stratum;
		}

		public void setStratum(List<Strata> stratum) {
			this.stratum = stratum;
		}

		@Override
		public String toString() {
			return "Echelon [dimensions=" + dimensions + ", hpFactor=" + hpFactor + ", maxHp=" + maxHp
					+ ", damageFactor=" + damageFactor + ", maxDamage=" + maxDamage + ", mobBlacklist=" + mobBlacklist
					+ ", stratum=" + stratum + "]";
		}

		public Double getXpFactor() {
			return xpFactor;
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
			return speedFactor;
		}

		public void setSpeedFactor(Double speedFactor) {
			this.speedFactor = speedFactor;
		}

		public Double getMaxSpeed() {
			return maxSpeed;
		}

		public void setMaxSpeed(Double maxSpeed) {
			this.maxSpeed = maxSpeed;
		}

		public Double getKnockbackIncrement() {
			return knockbackIncrement;
		}

		public void setKnockbackIncrement(Double knockbackIncrement) {
			this.knockbackIncrement = knockbackIncrement;
		}

		public Double getMaxKnockback() {
			return maxKnockback;
		}

		public void setMaxKnockback(Double maxKnockback) {
			this.maxKnockback = maxKnockback;
		}

		public Double getKnockbackResistIncrement() {
			return knockbackResistIncrement;
		}

		public void setKnockbackResistIncrement(Double knockbackResistIncrement) {
			this.knockbackResistIncrement = knockbackResistIncrement;
		}

		public Double getMaxKnockbackResist() {
			return maxKnockbackResist;
		}

		public void setMaxKnockbackResist(Double maxKnockbackResist) {
			this.maxKnockbackResist = maxKnockbackResist;
		}

		public Double getArmorFactor() {
			return armorFactor;
		}

		public void setArmorFactor(Double armorFactor) {
			this.armorFactor = armorFactor;
		}

		public Double getMaxArmor() {
			return maxArmor;
		}

		public void setMaxArmor(Double maxArmor) {
			this.maxArmor = maxArmor;
		}

		public Double getArmorToughnessFactor() {
			return armorToughnessFactor;
		}

		public void setArmorToughnessFactor(Double armorToughnessFactor) {
			this.armorToughnessFactor = armorToughnessFactor;
		}

		public Double getMaxArmorToughness() {
			return maxArmorToughness;
		}

		public void setMaxArmorToughness(Double maxArmorToughness) {
			this.maxArmorToughness = maxArmorToughness;
		}		
	}
	
	/*
	 * 
	 */
	public static class Strata {
		private Integer min;
		private Integer max;
		private List<LevelEntry> histogram;
		
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
		public List<LevelEntry> getHistogram() {
			return histogram;
		}
		public void setHistogram(List<LevelEntry> histogram) {
			this.histogram = histogram;
		}		
		@Override
		public String toString() {
			return "Strata [min=" + min + ", max=" + max + ", histogram=" + histogram + "]";
		}
	}
	
	public static class LevelEntry {
		private Integer level;
		private Double weight;
		
		public Integer getLevel() {
			return level;
		}
		public void setLevel(Integer level) {
			this.level = level;
		}
		public Double getWeight() {
			return weight;
		}
		public void setWeight(Double weight) {
			this.weight = weight;
		}
		
		@Override
		public String toString() {
			return "LevelEntry [level=" + level + ", weight=" + weight + "]";
		}		
	}
}
