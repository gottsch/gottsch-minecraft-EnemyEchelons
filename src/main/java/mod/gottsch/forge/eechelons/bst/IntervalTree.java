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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import mod.gottsch.forge.eechelons.EEchelons;

/**
 * 
 * @author Mark Gottschling on Jul 26, 2022
 *
 * @param <D>
 */
public class IntervalTree<D> {
	private Interval<D> root;
	
	public synchronized Interval<D> insert(Interval<D> interval) {
		root = insert(root, interval);
		return root;
	}
	
	/**
	 * 
	 * @param interval
	 * @param newInterval
	 * @return
	 */
	private Interval<D> insert(Interval<D> interval, Interval<D> newInterval) {
		if (interval == null) {
			interval = newInterval;
			return interval;
		}

		if (interval.getMax() == null ||  newInterval.getEnd() > interval.getMax()) {
			interval.setMax(newInterval.getEnd());
		}
        if (interval.getMin() == null || newInterval.getStart() < interval.getMin()) {
        	interval.setMin(newInterval.getStart());
        }
        
		if (interval.compareTo(newInterval) <= 0) {

			if (interval.getRight() == null) {
				interval.setRight(newInterval);
			}
			else {
				insert(interval.getRight(), newInterval);
			}
		}
		else {
			if (interval.getLeft() == null) {
				interval.setLeft(newInterval);
			}
			else {
				insert(interval.getLeft(), newInterval);
			}
		}
		return interval;
	}
	
	/**
	 * Requires and extact match of intervals.
	 * @param target
	 * @return
	 */
	public synchronized Interval<D> delete(Interval<D> target) {
		root = delete(root, target);
		EEchelons.LOGGER.debug("root is now -> {}", root);
		EEchelons.LOGGER.debug("all intervals now -> {}", toStringList(root));
		return root;
	}
	
	/**
	 * 
	 * @param interval
	 * @param target
	 * @return
	 */
	private Interval<D> delete(Interval<D> interval, Interval<D> target) {
		EEchelons.LOGGER.debug("delete interval -> {}, target -> {}", interval, target);
		if (interval == null) {
			return interval;
		}

		if (interval.compareTo(target) < 0) {
			interval.setRight(delete(interval.getRight(), target));
		}
		else if (interval.compareTo(target) > 0) {
			interval.setLeft(delete(interval.getLeft(), target));
		}
		else {
			// node with no leaf nodes
			if (interval.getLeft() == null && interval.getRight() == null) {
				return null;
			}
			else if (interval.getLeft() == null) {
				return interval.getRight();
			}
			else if (interval.getRight() == null) {
				return interval.getLeft();
			}
			else {
				// insert right tree into left tree
				insert(interval.getLeft(), interval.getRight());
				// return the left tree
				return interval.getLeft();
			}
		}
		return interval;
	}
	
	public List<Interval<D>> getOverlapping(Interval<D> interval, Interval<D> testInterval, boolean findFast) {
		return getOverlapping(interval, testInterval, true, true);
	}	
	
	/**
	 * public wrapper to ensure that the return value is non-null
	 * @param interval
	 * @param testInterval
	 */
	public synchronized List<Interval<D>> getOverlapping(Interval<D> interval, Interval<D> testInterval, boolean findFast, boolean includeBorder) {
		List<Interval<D>> results = new ArrayList<>();
		if (includeBorder) {
			checkOverlap(interval, testInterval, results, findFast);
		}
		else {
			checkOverlapNoBorder(interval, testInterval, results, findFast);
		}
		return results;
	}
	
	/**
	 * 
	 * @param interval
	 * @param testInterval
	 * @param results
	 * @param findFirst find first occurrence only
	 * @return whether an overlap was found in this subtree
	 */
	private boolean checkOverlap(Interval<D> interval, Interval<D> testInterval, List<Interval<D>> results, boolean findFast) {
		if (interval == null) {
			return false;
		}

		// short-circuit
        if(testInterval.getStart() > interval.getMax() || testInterval.getEnd() < interval.getMin()) {
        	return false;
        }

		if (!((interval.getStart() > testInterval.getEnd()) || (interval.getEnd() < testInterval.getStart()))) {
			results.add(interval);
			if (findFast) {
				return true;
			}				
		}

		// walk the left branch
		if ((interval.getLeft() != null) && (interval.getLeft().getMax() >= testInterval.getStart())) {
			if (this.checkOverlap(interval.getLeft(), testInterval, results, findFast) && findFast) {
				return true;
			}
		}

		// walk the right branch
		if (this.checkOverlap(interval.getRight(), testInterval, results, findFast) && findFast) {
			return true;
		}		
		return false;
	}
	
	/**
	 * 
	 * @param interval
	 * @param testInterval
	 * @param results
	 * @param findFirst find first occurrence only
	 * @return whether an overlap was found in this subtree
	 */
	private boolean checkOverlapNoBorder(Interval<D> interval, Interval<D> testInterval, List<Interval<D>> results, boolean findFast) {
		if (interval == null) {
			return false;
		}

		// short-circuit
        if(testInterval.getStart() > interval.getMax() || testInterval.getEnd() < interval.getMin()) {
        	return false;
        }

		if (!((interval.getStart() >= testInterval.getEnd()) || (interval.getEnd() <= testInterval.getStart()))) {
			results.add(interval);
			if (findFast) {
				return true;
			}				
		}

		// walk the left branch
		if ((interval.getLeft() != null) && (interval.getLeft().getMax() > testInterval.getStart())) { // TESTING replaced >= with >
			if (this.checkOverlapNoBorder(interval.getLeft(), testInterval, results, findFast) && findFast) {
				return true;
			}
		}

		// walk the right branch
		if (this.checkOverlapNoBorder(interval.getRight(), testInterval, results, findFast) && findFast) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param interval
	 * @param predicate
	 * @param intervals
	 */
	public synchronized void find(Interval<D> interval, Predicate<Interval<D>> predicate, List<Interval<D>> intervals) {
		find(interval, predicate, intervals, true);
	}
	
	/**
	 * Finds an interval based on a predicate. ie doesn't need to meet the interval range criteria.
	 * @param interval
	 * @param predicate
	 * @param intervals
	 * @param findFirst find first occurrence only
	 * @return whether an overlap was found in this subtree
	 */
	public synchronized boolean find(Interval<D> interval, Predicate<Interval<D>> predicate, List<Interval<D>> intervals, boolean findFirst) {
		boolean isFound = false;
		
		if (interval == null) {
			return false;
		}

		// check first to optimize findFirst search
		// add the interval to list
		if (predicate.test(interval)) {
			intervals.add(interval);
			if (findFirst) {
				return true;
			}
		}
		
		if (interval.getLeft() != null) {
			isFound = find(interval.getLeft(), predicate, intervals, findFirst);
			if (isFound && findFirst) {
				return true;
			}
		}

		if (interval.getRight() != null) {
			isFound = find(interval.getRight(), predicate, intervals, findFirst);
			if (isFound && findFirst) {
				return true;
			}
		}
		return isFound;
	}
	
	/**
	 * 
	 * @param interval
	 */
	public List<String> toStringList(Interval<D> interval) {
		List<Interval<D>> list = new ArrayList<>();
		list(interval, list);
		
		List<String> display = new ArrayList<>();
		list.forEach(element -> {
			display.add(String.format("[%s] -> [%s]: data -> %s", element.getStart(), element.getEnd(), element.getData()));
		});
		
		return display;
	}
	
	/**
	 * 
	 * @param interval
	 * @param intervals
	 */
	public synchronized void list(Interval<D> interval, List<Interval<D>> intervals) {
		if (interval == null) {
			return;
		}

		if (interval.getLeft() != null) {
			list(interval.getLeft(), intervals);
		}

		intervals.add(interval);
		
		if (interval.getRight() != null) {
			list(interval.getRight(), intervals);
		}
	}

	public void clear() {
		setRoot(null);
	}
	
	public synchronized Interval<D> getRoot() {
		return root;
	}

	public synchronized void setRoot(Interval<D> root) {
		this.root = root;
	}
}
