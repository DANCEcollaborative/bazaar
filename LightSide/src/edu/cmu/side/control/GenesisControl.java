package edu.cmu.side.control;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import edu.cmu.side.Workbench;
import edu.cmu.side.model.OrderedPluginMap;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.PredictionResult;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.plugin.FeaturePlugin;
import edu.cmu.side.plugin.SIDEPlugin;
import edu.cmu.side.view.util.CheckBoxListEntry;
import edu.cmu.side.view.util.RecipeCellRenderer;
import edu.cmu.side.view.util.Refreshable;
import edu.stanford.nlp.util.StringUtils;

public abstract class GenesisControl
{

	public static Map<Object, Collection<Refreshable>> listenerMap;
	public static Map<Object, Boolean> currentlyUpdatingMap;

	static
	{
		listenerMap = new HashMap<Object, Collection<Refreshable>>();
		currentlyUpdatingMap = new HashMap<Object, Boolean>();
	}

	public static void setCurrentlyUpdating(Object source, boolean val)
	{
		currentlyUpdatingMap.put(source, val);
	}

	public static boolean isCurrentlyUpdating(Object source)
	{
		if (!listenerMap.containsKey(source))
		{
			listenerMap.put(source, new ArrayList<Refreshable>());
			currentlyUpdatingMap.put(source, false);
		}
		return currentlyUpdatingMap.get(source);
	}

	public static void addListenerToMap(Object source, Refreshable child)
	{
		// System.out.println("GC 57: "+child.getClass().getName()+
		// " is listening to "+source);
		if (!listenerMap.containsKey(source))
		{
			listenerMap.put(source, new ArrayList<Refreshable>());
			currentlyUpdatingMap.put(source, false);
		}
		listenerMap.get(source).add(child);
	}

	public static Collection<Refreshable> getListeners(Object source)
	{
		if (!listenerMap.containsKey(source))
		{
			listenerMap.put(source, new ArrayList<Refreshable>());
			currentlyUpdatingMap.put(source, false);
		}
		return listenerMap.get(source);
	}

	public static class EvalCheckboxListener implements ItemListener
	{

		Map<? extends SIDEPlugin, Map<String, Boolean>> plugins;
		StatusUpdater updater;
		Refreshable source;

		public EvalCheckboxListener(Refreshable s, Map<? extends SIDEPlugin, Map<String, Boolean>> p, StatusUpdater u)
		{
			source = s;
			plugins = p;
			updater = u;
		}

		@Override
		public void itemStateChanged(ItemEvent ie)
		{
			CheckBoxListEntry entry = (CheckBoxListEntry) ie.getSource();
			boolean state = entry.isSelected();
			String eval = entry.getValue().toString();
			for (SIDEPlugin plug : plugins.keySet())
			{
				if (plugins.get(plug).containsKey(eval))
				{
					boolean flip = state;// !plugins.get(plug).get(eval);
					plugins.get(plug).put(eval, flip);
					Workbench.update(source);
				}
			}
		}
	}

	public static class PluginCheckboxListener<E> implements ItemListener
	{

		Refreshable source;
		Map<E, Boolean> plugins;

		public PluginCheckboxListener(Refreshable s, Map<E, Boolean> p)
		{
			source = s;
			plugins = p;
		}

		@Override
		public void itemStateChanged(ItemEvent ie)
		{
			CheckBoxListEntry check = ((CheckBoxListEntry) ie.getSource());
			E plug = (E) check.getValue();
			plugins.put(plug, check.isSelected());
			Workbench.update(source);
		}
	}

	public static Collection<Recipe> getDocumentLists()
	{
		return Workbench.getRecipesByPane(RecipeManager.Stage.DOCUMENT_LIST);
	}

	public static Collection<Recipe> getFeatureTables()
	{
		return Workbench.getRecipesByPane(RecipeManager.Stage.FEATURE_TABLE);
	}

	public static Collection<Recipe> getFilterTables()
	{
		return Workbench.getRecipesByPane(RecipeManager.Stage.MODIFIED_TABLE);
	}

	public static Collection<Recipe> getTrainingTables()
	{
		return Workbench.getRecipesByPane(RecipeManager.Stage.MODIFIED_TABLE, RecipeManager.Stage.FEATURE_TABLE);
	}

	public static Collection<Recipe> getTrainedModels()
	{
		return Workbench.getRecipesByPane(RecipeManager.Stage.TRAINED_MODEL);
	}

	public static int numFeatureTables()
	{
		return getFeatureTables().size();
	}

	public static int numFilterTables()
	{
		return getFilterTables().size();
	}

	public static int numDocumentLists()
	{
		return getDocumentLists().size();
	}

	public static int numTrainedModels()
	{
		return getTrainedModels().size();
	}

	public static JTree getRecipeTree(Recipe r)
	{
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(r.getStage());
		if (r.getDocumentList() != null)
		{
			top.add(getDocumentsNode(r.getDocumentList()));
		}
		if (r.getExtractors().size() > 0)
		{
			top.add(getExtractorNodes(r));
		}
		if (r.getFeatureTable() != null)
		{
			top.add(getTableNode(r.getFeatureTable(), "Feature Table:"));
		}
		if (r.getFilters().size() > 0)
		{
			top.add(getFilterNodes(r));
		}
		if (r.getFilteredTable() != null)
		{
			top.add(getTableNode(r.getFilteredTable(), "Restructured Table:"));
		}
		if (r.getLearner() != null)
		{
			top.add(getPluginNode("Learning Plugin: ", r.getLearner(), r.getLearnerSettings()));
		}

		if (r.getWrappers().size() > 0)
		{
			top.add(getWrapperNodes(r));
		}

		if (r.getValidationSettings() != null)
		{
			top.add(getValidationNode(r.getValidationSettings()));
		}

		if (r.getTrainingResult() != null)
		{
			top.add(getModelNode(r.getTrainingResult()));
		}
		if (r.getPredictionResult() != null)
		{
			top.add(getPredictionNode(r.getPredictionResult()));
		}

		JTree recipeComponent = new JTree(top);
		recipeComponent.setCellRenderer(new RecipeCellRenderer());

		// ScrollablePanel panel = new ScrollablePanel(new BorderLayout());
		// panel.add(recipeComponent, BorderLayout.CENTER);
		// panel.setScrollableWidth(ScrollableSizeHint.STRETCH);
		// panel.setScrollableHeight(ScrollableSizeHint.STRETCH);

		return recipeComponent;
	}

	private static MutableTreeNode getValidationNode(Map<String, Serializable> validationSettings)
	{
		String type = (String) validationSettings.get("type");
		String test = (String) validationSettings.get("test");
		String testSet = ((DocumentList) validationSettings.get("testSet")).getName();

		DefaultMutableTreeNode node = new DefaultMutableTreeNode("Validation: " + (test.equals("false") ? "None" : (type.equals("SUPPLY") ? testSet : "CV")));

		// TODO: add nodes for test recipe in supplied test set case.
		if (type.equals("CV")) for (String key : validationSettings.keySet())
		{
			if (key.equals("type") || key.startsWith("test"))
			{

			}
			else
			{
				String value = validationSettings.get(key).toString();

				if (value.length() > 30) value = value.substring(0, 30) + "...";

				DefaultMutableTreeNode setting = new DefaultMutableTreeNode(key + ": " + value);
				node.add(setting);
			}
		}

		return node;
	}

	public static MutableTreeNode getDocumentsNode(DocumentList docs)
	{
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("Documents: " + docs.getName());

		DefaultMutableTreeNode size = new DefaultMutableTreeNode("Instances: " + docs.getSize());
		Set<String> textColumns = docs.getTextColumns();
		DefaultMutableTreeNode text = new DefaultMutableTreeNode("Text Column"
						+ (textColumns.size() == 1 ? 
						": " + (textColumns.iterator().next()) : 
						"s: " + (textColumns.size())));

		if (textColumns.size() > 1) for (String s : textColumns)
		{
			DefaultMutableTreeNode textName = new DefaultMutableTreeNode(s);
			text.add(textName);
		}
		Set<String> filenames = docs.getFilenames();
		DefaultMutableTreeNode files = new DefaultMutableTreeNode("Files" + (filenames.size() > 1 ? "s" : "") + ": ");
		for (String s : filenames)
		{
			DefaultMutableTreeNode fileName = new DefaultMutableTreeNode(s.substring(s.lastIndexOf(java.io.File.separator) + 1));
			files.add(fileName);
		}
		node.add(files);
		node.add(size);
		node.add(text);
		if (textColumns.size() > 1) node.add(new DefaultMutableTreeNode("Differentiate Text Fields: " + docs.getTextColumnsAreDifferentiated()));
		return node;
	}

	public static MutableTreeNode getFilterNodes(Recipe r)
	{
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("Restructure Plugins:");
		OrderedPluginMap filters = r.getFilters();
		String label = "Restructure Plugins:";
		for (SIDEPlugin plug : filters.keySet())
		{
			node.add(getPluginNode("", plug, filters.get(plug)));
			label += " " + plug.getOutputName();
		}
		node.setUserObject(label);
		return node;
	}

	public static MutableTreeNode getWrapperNodes(Recipe r)
	{
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("Wrapper Plugins:");
		String label = "Wrapper Plugins:";
		OrderedPluginMap wrappers = r.getWrappers();
		for (SIDEPlugin plug : wrappers.keySet())
		{
			node.add(getPluginNode("", plug, wrappers.get(plug)));
			label += " " + plug.getOutputName();
		}
		node.setUserObject(label);
		return node;
	}

	public static MutableTreeNode getExtractorNodes(Recipe r)
	{
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("Feature Plugins:");
		OrderedPluginMap extractors = r.getExtractors();
		String label = "Feature Plugins:";
		for (SIDEPlugin plug : extractors.keySet())
		{
			node.add(getPluginNode("", plug, extractors.get(plug)));
			label += " " + plug.getOutputName();
		}
		node.setUserObject(label);
		return node;
	}

	public static MutableTreeNode getPluginNode(String label, SIDEPlugin plug, Map<String, String> keys)
	{
		DefaultMutableTreeNode plugin = new DefaultMutableTreeNode(label + (label.length() > 0 ? " " : "") + plug.toString());
		// Map<String, String> keys = plug.generateConfigurationSettings();
		for (String key : keys.keySet())
		{
			String value = keys.get(key);

			if (!value.equals("false"))
			{
				if (value.length() > 20) value = value.substring(0, 20) + "...";

				DefaultMutableTreeNode setting = new DefaultMutableTreeNode(key + ": " + value);
				plugin.add(setting);
			}
		}
		return plugin;
	}

	public static MutableTreeNode getTableNode(FeatureTable features, String key)
	{
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(key + " " + features.getName());
		DefaultMutableTreeNode size = new DefaultMutableTreeNode(features.getFeatureSet().size() + " features");
		DefaultMutableTreeNode annot = new DefaultMutableTreeNode("Class: " + features.getAnnotation());
		DefaultMutableTreeNode type = new DefaultMutableTreeNode("Type: " + features.getClassValueType().toString().toLowerCase());
		node.add(size);
		node.add(annot);
		node.add(type);
		return node;
	}

	public static MutableTreeNode getModelNode(TrainingResult model)
	{
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("Trained Model: " + model.getName());
		Map<String, String> evaluations = model.getCachedEvaluations();
		for (String key : evaluations.keySet())
		{
			String value = evaluations.get(key);
			try
			{
				double d = Double.parseDouble(value);
				value = String.format("%.3f", d);
			}
			catch (NumberFormatException e)
			{
			}
			node.add(new DefaultMutableTreeNode(key + ": " + value));
		}

		return node;
	}

	public static MutableTreeNode getPredictionNode(PredictionResult model)
	{
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("Prediction: " + model.getName());
		return node;
	}

}
