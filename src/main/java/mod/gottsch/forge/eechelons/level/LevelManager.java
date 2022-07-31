package mod.gottsch.forge.eechelons.level;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.someguyssoftware.gottschcore.random.WeightedCollection;

import mod.gottsch.forge.eechelons.bst.DataIntervalTree;
import mod.gottsch.forge.eechelons.capability.LevelHandler;

@Deprecated
public class LevelManager {

	private WeightedCollection<Double, Integer> shallowLevelMap = new WeightedCollection<>();
	
	// TEMP data structure - use IntervalTree (binary search tree)
	private List<WeightedCollection<Double, Integer>> levelMaps = new LinkedList<>();
	
	// TEMP BST
	private DataIntervalTree<LevelHandler> tree = new DataIntervalTree<>(LevelHandler::new);

	// TODO create a Map<ResourceLocation, MevelIntervalTree<LevelHandler>>
	
	public LevelManager() {
//		FileConfig conf = FileConfig.of("");
		CommentedFileConfig config = CommentedFileConfig.builder("E:\\Development\\workspace\\Sandbox\\src\\resources\\eechelons.toml").autosave().build();
		config.load();
		
		
		/*
		 *  setup overworld probability map
		 */
		
		// 1st section is top of world to underground: 319 -> 30
		levelMaps.add(new WeightedCollection<Double, Integer>());
		levelMaps.get(0)
		.add(200D, 0)
		.add(150D, 1)
		.add(100D, 2)
		.add(60D, 3)
		.add(45D, 4)
		.add(35D, 5)
		.add(25D, 6)
		.add(15D, 7)
		.add(10D, 8)
		.add(5D, 9)
		.add(1D, 10);
		
		// 2nd section: 29 -> -10
		levelMaps.add(new WeightedCollection<Double, Integer>());
		levelMaps.get(1)
		.add(100D, 0)
		.add(120D, 1)
		.add(200D, 2)
		.add(120D, 3)
		.add(85D, 4)
		.add(75D, 5)
		.add(65D, 6)
		.add(50D, 7)
		.add(30D, 8)
		.add(20D, 9)
		.add(10D, 10);
		
		// 3rd section: -11 -> -64
		levelMaps.add(new WeightedCollection<Double, Integer>());
		levelMaps.get(2)
		.add(60D, 0)
		.add(80D, 1)
		.add(80D, 2)
		.add(100D, 3)
		.add(100D, 4)
		.add(90D, 5)
		.add(80D, 6)
		.add(50D, 7)
		.add(30D, 8)
		.add(20D, 9)
		.add(10D, 10);
	}
}
