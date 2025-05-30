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

/*
 * Synchronization Strategy:
 * All public methods that access or modify the tree structure (e.g., insert, delete, getRoot, setRoot)
 * or perform traversals (e.g., getOverlapping, find, list) are synchronized on the instance.
 * This provides thread safety with a coarse-grained lock.
 *
 * Rationale:
 * - Guarantees thread safety in concurrent environments.
 * - Simpler to implement and verify than more fine-grained locking mechanisms.
 *
 * Potential Considerations for Future Performance Optimization:
 * - If profiling reveals these synchronized methods as a significant bottleneck due to contention,
 *   especially in read-heavy scenarios, migrating to a java.util.concurrent.locks.ReadWriteLock
 *   could be considered to allow for concurrent read access.
 * - For specific use cases where a tree instance is effectively immutable after its initial construction
 *   and safe publication, the synchronization on read-only methods might be overly cautious but
 *   is retained for general-purpose safety.
 *
 * Current Recommendation:
 * Retain current synchronized approach unless specific performance issues are demonstrated.
 */
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
	private Interval<D> delete(Interval<D> current, Interval<D> target) {
		EEchelons.LOGGER.debug("delete current -> {}, target -> {}", current, target);
		if (current == null) {
			return null;
		}

		int comparison = current.compareTo(target);

		if (comparison > 0) { // target is smaller, go left
			current.setLeft(delete(current.getLeft(), target));
		} else if (comparison < 0) { // target is larger, go right
			current.setRight(delete(current.getRight(), target));
		} else { // current is the node to be deleted
			// Node with no children or only one child
			if (current.getLeft() == null && current.getRight() == null) {
				EEchelons.LOGGER.debug("deleting node with no children: {}", current);
				return null; // No children
			}
			if (current.getLeft() == null) {
				EEchelons.LOGGER.debug("deleting node with only right child: {}", current);
				return current.getRight(); // Only right child
			}
			if (current.getRight() == null) {
				EEchelons.LOGGER.debug("deleting node with only left child: {}", current);
				return current.getLeft(); // Only left child
			}

			// Node with two children: Get the inorder successor (smallest in the right subtree)
			EEchelons.LOGGER.debug("deleting node with two children: {}", current);
			Interval<D> successor = findMin(current.getRight());
			EEchelons.LOGGER.debug("successor found: {}", successor);

			// Copy the inorder successor's content to this node
			// NOTE: This requires Interval to have setStart, setEnd methods.
			current.setStart(successor.getStart());
			current.setEnd(successor.getEnd());
			current.setData(successor.getData());
			EEchelons.LOGGER.debug("current node after copying successor data: {}", current);

			// Delete the inorder successor from the right subtree
			// Create a target interval that exactly matches the successor for deletion
			Interval<D> successorTarget = new Interval<>(successor.getStart(), successor.getEnd(), successor.getData());
			current.setRight(delete(current.getRight(), successorTarget));
		}

		// Update min/max properties of the current node after potential child changes
		updateNodeProperties(current);
		EEchelons.LOGGER.debug("current node after updateNodeProperties: {}", current);
		return current;
	}

	/**
	 * Finds the node with the smallest 'start' value in the subtree rooted at node.
	 * Assumes node is not null.
	 * @param node The root of the subtree to search.
	 * @return The node with the smallest value.
	 */
	private Interval<D> findMin(Interval<D> node) {
		while (node.getLeft() != null) {
			node = node.getLeft();
		}
		return node;
	}

	/**
	 * Updates the min and max properties of a node based on its own interval's start/end
	 * and the min/max properties of its children.
	 * @param node The node to update.
	 */
	private void updateNodeProperties(Interval<D> node) {
		if (node == null) {
			return;
		}

		Integer currentMin = node.getStart();
		Integer currentMax = node.getEnd();

		if (node.getLeft() != null) {
			// The children's min/max should already be correct due to recursive calls to delete
			// and subsequent updateNodeProperties calls on them.
			if (node.getLeft().getMin() != null) { // Child's min could be null if it was invalid before (though less likely with this fix)
				currentMin = Math.min(currentMin, node.getLeft().getMin());
			} else { // if child's min is null, use child's start
                                currentMin = Math.min(currentMin, node.getLeft().getStart());
                        }
			if (node.getLeft().getMax() != null) {
				currentMax = Math.max(currentMax, node.getLeft().getMax());
			} else { // if child's max is null, use child's end
                                currentMax = Math.max(currentMax, node.getLeft().getEnd());
                        }
		}
		if (node.getRight() != null) {
			if (node.getRight().getMin() != null) {
				currentMin = Math.min(currentMin, node.getRight().getMin());
			} else {
                                currentMin = Math.min(currentMin, node.getRight().getStart());
                        }
			if (node.getRight().getMax() != null) {
				currentMax = Math.max(currentMax, node.getRight().getMax());
			} else {
                                currentMax = Math.max(currentMax, node.getRight().getEnd());
                        }
		}
		node.setMin(currentMin);
		node.setMax(currentMax);
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
		// For a strict overlap (no borders), an interval in the left subtree (I_L)
		// must satisfy I_L.end > testInterval.start.
		// Thus, the maximum end point in the entire left subtree (interval.getLeft().getMax())
		// must be strictly greater than testInterval.start to warrant searching there.
		// If interval.getLeft().getMax() == testInterval.start, any interval providing that max
		// would only touch testInterval's border, which is excluded by "no border".
		// This contrasts with border-inclusive searches where '>=' would be used.
		if ((interval.getLeft() != null) && (interval.getLeft().getMax() > testInterval.getStart())) {
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
