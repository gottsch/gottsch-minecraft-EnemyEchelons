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
package mod.gottsch.forge.eechelons.bst;

import mod.gottsch.forge.eechelons.EEchelons;

/**
 * 
 * @author Mark Gottschling on Jul 26, 2022
 *
 * @param <D>
 */
public class Interval<D> implements Comparable<Interval<D>> {
	private static final String LEFT_KEY = "left";
	private static final String RIGHT_KEY = "right";
	private static final String MIN_KEY = "min";
	private static final String MAX_KEY = "max";
	private static final String DATA_KEY = "data";
	
	public static final Interval<?> EMPTY = new Interval<>(-255, 320, null);
	
	private Integer start;
	private Integer end;
	private Integer min;
	private Integer max;
	private Interval<D> left;
	private Interval<D> right;

	// extra mod specific data
	private D data;

	/**
	 * Empty constructor
	 */
	public Interval() {
		start = EMPTY.getStart();
		end = EMPTY.getEnd();
	}
	
	/**
	 * 
	 * @param start
	 * @param end
	 */
	public Interval(Integer start, Integer end) {
		this.start = start;
		this.end = end;
		this.min = start < end ? start : end;
		this.max = start > end ? start : end;
	}

	/**
	 * 
	 * @param coords1
	 * @param coords2
	 * @param data
	 */
	public Interval(Integer start, Integer end, D data) {
		this(start, end);
		this.data = data;
	}

	
	@Override
	public int compareTo(Interval<D> interval) {
		if (getStart() < interval.getStart()) {
			return -1;
		} else if (getStart() == interval.getStart()) {
			if (getEnd() == interval.getEnd()) {
				return 0;
			}
			EEchelons.LOGGER.debug("this.end -> {}, interval.end -> {}", this.getEnd(), interval.getEnd());
			return this.getEnd() < interval.getEnd() ? -1 : 1;
		} else {
			EEchelons.LOGGER.debug("this.end -> {}, interval.end -> {}", this.getEnd(), interval.getEnd());
			return 1;
		}
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
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

	public Interval<D> getLeft() {
		return left;
	}

	public void setLeft(Interval<D> left) {
		this.left = left;
	}

	public Interval<D> getRight() {
		return right;
	}

	public void setRight(Interval<D> right) {
		this.right = right;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((start == null) ? 0 : start.hashCode());
		result = prime * result + ((end == null) ? 0 : end.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Interval<?> other = (Interval<?>) obj;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		if (end == null) {
			if (other.end != null)
				return false;
		} else if (!end.equals(other.end))
			return false;
		return true;
	}

	public D getData() {
		return data;
	}

	public void setData(D data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "Interval [start=" + start + ", end=" + end + ", min=" + min + ", max=" + max + ", left=" + left
				+ ", right=" + right + ", data=" + data + "]";
	}
	
}
