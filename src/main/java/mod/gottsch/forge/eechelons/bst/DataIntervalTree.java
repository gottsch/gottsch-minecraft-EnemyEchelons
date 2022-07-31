package mod.gottsch.forge.eechelons.bst;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import mod.gottsch.forge.eechelons.EEchelons;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * TODO move to GottschCore
 * @author Mark Gottschling on Jul 25, 2022
 *
 * @param <D>
 */
public class DataIntervalTree<D extends INBTSerializable<Tag>> {
	private DataInterval<D> root;
	private Supplier<D> dataSupplier;
	
	public DataIntervalTree(Supplier<D> supplier) {
		dataSupplier = supplier;
	}
	
	public synchronized DataInterval<D> insert(DataInterval<D> interval) {
		root = insert(root, interval);
		return root;
	}
	
	/**
	 * 
	 * @param interval
	 * @param newInterval
	 * @return
	 */
	private DataInterval<D> insert(DataInterval<D> interval, DataInterval<D> newInterval) {
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
	public synchronized DataInterval<D> delete(DataInterval<D> target) {
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
	private DataInterval<D> delete(DataInterval<D> interval, DataInterval<D> target) {
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
	
	/**
	 * 
	 * @param interval
	 * @param predicate
	 * @param intervals
	 */
	public synchronized void find(DataInterval<D> interval, Predicate<DataInterval<D>> predicate, List<DataInterval<D>> intervals) {
		find(interval, predicate, intervals, true);
	}
	
	/**
	 * 
	 * @param interval
	 * @param predicate
	 * @param intervals
	 * @param findFirst find first occurrence only
	 * @return whether an overlap was found in this subtree
	 */
	public synchronized boolean find(DataInterval<D> interval, Predicate<DataInterval<D>> predicate, List<DataInterval<D>> intervals, boolean findFirst) {
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
	public List<String> toStringList(DataInterval<D> interval) {
		List<DataInterval<D>> list = new ArrayList<>();
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
	public synchronized void list(DataInterval<D> interval, List<DataInterval<D>> intervals) {
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
	
	/**
	 * 
	 * @param nbt
	 * @param interval
	 * @return
	 */
	public synchronized CompoundTag save(CompoundTag nbt) {
		if (getRoot() == null) {
			return nbt;
		}
		getRoot().save(nbt);	        	        
		return nbt;
	}
	
	/**
	 * 
	 * @param nbt
	 */
	public synchronized void load(CompoundTag nbt) {
		DataInterval<D> root = new DataInterval<>(dataSupplier);
		root.load(nbt);
		if (!root.equals(DataInterval.EMPTY)) {
			setRoot(root);
		}
	}

	public void clear() {
		setRoot(null);
	}
	
	public synchronized DataInterval<D> getRoot() {
		return root;
	}

	public synchronized void setRoot(DataInterval<D> root) {
		this.root = root;
	}
}
